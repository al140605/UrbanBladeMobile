package com.urbanblade.mobile.core.payment

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG
import android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

/** Each system verification authorizes one pending operation, never a reusable session. */
internal class PaymentAuthorizationGate {
    private var sequence = 0L
    private var attempt: Long? = null
    private var pending: (() -> Unit)? = null
    private var denied: (() -> Unit)? = null
    fun begin(action: () -> Unit, onDenied: () -> Unit): Long? {
        if (attempt != null) return null
        sequence += 1
        attempt = sequence
        pending = action
        denied = onDenied
        return sequence
    }
    fun finish(id: Long, authorized: Boolean) {
        if (attempt != id) return
        val callback = if (authorized) pending else denied
        attempt = null
        pending = null
        denied = null
        callback?.invoke()
    }
    fun cancel() { attempt?.let { finish(it, false) } }
}

/** System authentication only; no fingerprint, PIN or reusable authorization is stored. */
@Suppress("DEPRECATION")
@Composable
fun rememberPaymentAuthorization(onDenied: () -> Unit): ((() -> Unit) -> Unit) {
    val context = LocalContext.current
    val latestDenied by rememberUpdatedState(onDenied)
    val gate = remember { PaymentAuthorizationGate() }
    var cancellation by remember { mutableStateOf<CancellationSignal?>(null) }
    var credentialAttempt by remember { mutableStateOf<Long?>(null) }
    val credentials = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val id = credentialAttempt
        credentialAttempt = null
        if (id != null) gate.finish(id, it.resultCode == Activity.RESULT_OK)
    }
    DisposableEffect(gate) {
        onDispose {
            gate.cancel()
            cancellation?.cancel()
        }
    }
    return remember(context, gate, credentials) {
        authorize@{ action ->
            val id = gate.begin(action) { latestDenied() } ?: return@authorize
            val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            if (!keyguard.isDeviceSecure) {
                Toast.makeText(context, "Configura un PIN, patrón o contraseña de bloqueo para autorizar pagos.", Toast.LENGTH_LONG).show()
                gate.finish(id, false)
                return@authorize
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val signal = CancellationSignal()
                    cancellation = signal
                    BiometricPrompt.Builder(context)
                        .setTitle("Autorizar pago en UrbanBlade")
                        .setSubtitle("Confirma tu identidad con huella o bloqueo del teléfono")
                        .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
                        .build()
                        .authenticate(signal, context.mainExecutor, object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                gate.finish(id, true)
                            }
                            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                gate.finish(id, false)
                            }
                        })
                } else {
                    // Android 8–10: compatible system PIN/pattern/password fallback.
                    val intent = keyguard.createConfirmDeviceCredentialIntent("Autorizar pago", "Confirma el bloqueo de tu teléfono")
                    if (intent == null) gate.finish(id, false) else {
                        credentialAttempt = id
                        credentials.launch(intent)
                    }
                }
            } catch (_: Exception) {
                gate.finish(id, false)
                Toast.makeText(context, "No se pudo verificar tu identidad. Intenta de nuevo.", Toast.LENGTH_LONG).show()
            }
        }
    }
}
