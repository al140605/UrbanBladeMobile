package com.urbanblade.mobile.core.push

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.urbanblade.mobile.core.network.AppContainer
import kotlinx.coroutines.launch

/**
 * Tras iniciar sesión (T142): en Android 13+ pide el permiso de notificaciones
 * y registra el token FCM del dispositivo en barber. Se ejecuta una vez por
 * usuario; si Firebase no está configurado en el build, no hace nada.
 */
@Composable
fun PushRegistrationEffect(userId: String?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val register: () -> Unit = {
        scope.launch { PushNotifications.registerCurrentToken(context, AppContainer.urbanRepository) }
    }
    // El token se registra aunque el permiso se niegue: si el usuario lo activa
    // después en Ajustes, los recordatorios empiezan a llegar sin volver a iniciar sesión.
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { register() }

    LaunchedEffect(userId) {
        if (!PushNotifications.isAvailable(context)) return@LaunchedEffect
        PushNotifications.ensureChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !PushNotifications.canNotify(context)) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            register()
        }
    }
}
