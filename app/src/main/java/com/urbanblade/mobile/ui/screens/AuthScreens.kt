package com.urbanblade.mobile.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.urbanblade.mobile.core.auth.GoogleAuthHelper
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(authViewModel: AuthViewModel, onRegister: () -> Unit, onForgot: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    val busy by authViewModel.busy.collectAsState()
    val error by authViewModel.error.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var googleBusy by remember { mutableStateOf(false) }

    AuthShell(
        title = "Tu estilo. Tu tiempo.",
        subtitle = "Todo UrbanBlade en la palma de tu mano.",
        helper = "Nava cuida la entrada. Tú solo trae tus credenciales."
    ) {
        GoogleSignInButton(
            text = "Continuar con Google",
            loading = googleBusy,
            onClick = {
                scope.launch {
                    googleBusy = true
                    val idToken = GoogleAuthHelper.requestGoogleIdToken(context)
                    googleBusy = false
                    if (idToken != null) authViewModel.loginWithGoogle(idToken)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        OrDivider("O CON TU CORREO")
        Spacer(Modifier.height(16.dp))

        UrbanFieldLabel("Correo electrónico")
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = { Text("nombre@correo.com") },
            leadingIcon = { Icon(Icons.Default.AlternateEmail, null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        )
        Spacer(Modifier.height(14.dp))
        UrbanFieldLabel("Contraseña")
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = { Text("••••••••") },
            leadingIcon = { Icon(Icons.Default.Lock, null) },
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, "Mostrar contraseña")
                }
            },
            singleLine = true,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onForgot) { Text("¿Olvidaste tu contraseña?") }
        }
        error?.let {
            UrbanErrorBanner(it)
            Spacer(Modifier.height(12.dp))
        }
        UrbanPrimaryButton(
            text = "Entrar a UrbanBlade",
            onClick = { authViewModel.login(email, password) },
            enabled = email.isNotBlank() && password.isNotBlank(),
            loading = busy,
            icon = Icons.Default.ArrowForward,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(14.dp))
        OrDivider("¿Eres nuevo?")
        Spacer(Modifier.height(14.dp))
        UrbanOutlineButton(
            text = "Crear mi cuenta",
            onClick = onRegister,
            icon = Icons.Default.PersonAdd,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun RegisterScreen(authViewModel: AuthViewModel, onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmation by remember { mutableStateOf(false) }
    val busy by authViewModel.busy.collectAsState()
    val error by authViewModel.error.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var googleBusy by remember { mutableStateOf(false) }

    // Mismo mínimo que AuthController::register() ('password' => ['min:8', 'confirmed']) --
    // validar aquí evita un viaje al servidor que de todos modos rechazaría la contraseña.
    val passwordTooShort = password.isNotEmpty() && password.length < 8
    val passwordsMismatch = confirmation.isNotEmpty() && password != confirmation

    AuthShell(
        title = "Crea tu cuenta",
        subtitle = "Reserva, compra y administra tu experiencia UrbanBlade.",
        helper = "Tu cuenta se conecta al mismo ecosistema de la versión web.",
        onBack = onBack
    ) {
        GoogleSignInButton(
            text = "Continuar con Google",
            loading = googleBusy,
            onClick = {
                scope.launch {
                    googleBusy = true
                    val idToken = GoogleAuthHelper.requestGoogleIdToken(context)
                    googleBusy = false
                    if (idToken != null) authViewModel.loginWithGoogle(idToken)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        OrDivider("O CON TU CORREO")
        Spacer(Modifier.height(16.dp))

        UrbanFieldLabel("Nombre")
        OutlinedTextField(name, { name = it }, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Person, null) }, singleLine = true, shape = MaterialTheme.shapes.medium)
        Spacer(Modifier.height(12.dp))
        UrbanFieldLabel("Correo")
        OutlinedTextField(email, { email = it }, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.AlternateEmail, null) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), shape = MaterialTheme.shapes.medium)
        Spacer(Modifier.height(12.dp))
        UrbanFieldLabel("Contraseña")
        OutlinedTextField(
            password,
            { password = it },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Lock, null) },
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, "Mostrar contraseña")
                }
            },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            isError = passwordTooShort,
            supportingText = { if (passwordTooShort) Text("Mínimo 8 caracteres.") },
            shape = MaterialTheme.shapes.medium
        )
        Spacer(Modifier.height(12.dp))
        UrbanFieldLabel("Confirmar contraseña")
        OutlinedTextField(
            confirmation,
            { confirmation = it },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.VerifiedUser, null) },
            trailingIcon = {
                IconButton(onClick = { showConfirmation = !showConfirmation }) {
                    Icon(if (showConfirmation) Icons.Default.VisibilityOff else Icons.Default.Visibility, "Mostrar confirmación")
                }
            },
            visualTransformation = if (showConfirmation) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            isError = passwordsMismatch,
            supportingText = { if (passwordsMismatch) Text("Las contraseñas no coinciden.") },
            shape = MaterialTheme.shapes.medium
        )
        Spacer(Modifier.height(14.dp))
        error?.let { UrbanErrorBanner(it); Spacer(Modifier.height(12.dp)) }
        UrbanPrimaryButton(
            text = "Crear cuenta",
            onClick = { authViewModel.register(name, email, password, confirmation) },
            enabled = name.isNotBlank() && email.isNotBlank() && password.length >= 8 && confirmation == password,
            loading = busy,
            icon = Icons.Default.HowToReg,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun ForgotPasswordScreen(authViewModel: AuthViewModel, onBack: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    val busy by authViewModel.busy.collectAsState()
    val error by authViewModel.error.collectAsState()

    AuthShell(
        title = "Recupera tu acceso",
        subtitle = "Te enviaremos las instrucciones al correo asociado a tu cuenta.",
        helper = "Si no ves el correo, revisa también tu bandeja de spam.",
        onBack = onBack
    ) {
        UrbanFieldLabel("Correo electrónico")
        OutlinedTextField(
            email,
            { email = it },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.MailOutline, null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            shape = MaterialTheme.shapes.medium
        )
        Spacer(Modifier.height(14.dp))
        error?.let { UrbanErrorBanner(it); Spacer(Modifier.height(12.dp)) }
        message?.let { UrbanInfoBanner(it, Icons.Default.MarkEmailRead); Spacer(Modifier.height(12.dp)) }
        UrbanPrimaryButton(
            text = "Enviar instrucciones",
            onClick = { authViewModel.forgotPassword(email) { message = it } },
            enabled = email.isNotBlank(),
            loading = busy,
            icon = Icons.Default.Send,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun OrDivider(label: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(Modifier.weight(1f), color = UrbanColors.Line)
        Text("  $label  ", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
        HorizontalDivider(Modifier.weight(1f), color = UrbanColors.Line)
    }
}

/**
 * "G" de Google dibujado con 4 arcos de sus colores de marca -- evita
 * depender de un asset SVG externo para un botón que solo aparece en 2
 * pantallas.
 */
@Composable
private fun GoogleGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val strokeWidth = size.minDimension * 0.24f
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)
        val stroke = Stroke(strokeWidth)
        drawArc(Color(0xFF4285F4), startAngle = -90f, sweepAngle = 90f, useCenter = false, topLeft = topLeft, size = arcSize, style = stroke)
        drawArc(Color(0xFF34A853), startAngle = 0f, sweepAngle = 90f, useCenter = false, topLeft = topLeft, size = arcSize, style = stroke)
        drawArc(Color(0xFFFBBC05), startAngle = 90f, sweepAngle = 90f, useCenter = false, topLeft = topLeft, size = arcSize, style = stroke)
        drawArc(Color(0xFFEA4335), startAngle = 180f, sweepAngle = 90f, useCenter = false, topLeft = topLeft, size = arcSize, style = stroke)
    }
}

@Composable
private fun GoogleSignInButton(
    text: String,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        enabled = !loading,
        modifier = modifier.heightIn(min = 52.dp),
        shape = RoundedCornerShape(15.dp),
        border = BorderStroke(1.dp, UrbanColors.Line),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = UrbanColors.Ink)
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(19.dp), strokeWidth = 2.dp, color = UrbanColors.Gold)
        } else {
            GoogleGlyph()
            Spacer(Modifier.width(10.dp))
            Text(text, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun AuthShell(
    title: String,
    subtitle: String,
    helper: String,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    // Entrada animada de la tarjeta (una sola vez al montar la pantalla, no
    // en cada recomposición) -- fade + escala sutil, look más premium que
    // aparecer de golpe.
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    UrbanBladeBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver") }
                } else {
                    Spacer(Modifier.size(48.dp))
                }
                UrbanBrandMark(compact = true)
                Spacer(Modifier.size(48.dp))
            }

            Spacer(Modifier.weight(0.35f))

            Text("URBANBLADE", style = MaterialTheme.typography.labelMedium, color = UrbanColors.Gold)
            Spacer(Modifier.height(7.dp))
            Text(title, style = MaterialTheme.typography.displaySmall, color = UrbanColors.Ink)
            Spacer(Modifier.height(8.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = UrbanColors.Muted)
            Spacer(Modifier.height(20.dp))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(350, easing = EaseOutCubic)) +
                    scaleIn(initialScale = 0.96f, animationSpec = tween(350, easing = EaseOutCubic))
            ) {
                // Esquinas un poco más grandes que el UrbanPremiumCard por
                // defecto (24.dp) -- una sensación más de "hoja modal" al
                // estilo iOS solo en el contexto de auth, sin tocar el resto
                // de la app que usa UrbanPremiumCard con su shape por defecto.
                UrbanPremiumCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp)) {
                    content()
                }
            }

            Spacer(Modifier.height(16.dp))
            UrbanInfoBanner(helper, Icons.Default.ContentCut)
            Spacer(Modifier.weight(0.65f))
            Text(
                "${UrbanColors.current.label.uppercase()} · ANDROID",
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 18.dp),
                style = MaterialTheme.typography.labelMedium,
                color = UrbanColors.Muted
            )
        }
    }
}
