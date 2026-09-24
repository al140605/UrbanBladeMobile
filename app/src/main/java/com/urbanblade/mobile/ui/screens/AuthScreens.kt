package com.urbanblade.mobile.ui.screens

import android.util.Patterns
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.urbanblade.mobile.core.auth.GoogleAuthHelper
import com.urbanblade.mobile.ui.components.AuthBackdrop
import com.urbanblade.mobile.ui.components.AuthPrimaryButton
import com.urbanblade.mobile.ui.components.PasswordStrengthMeter
import com.urbanblade.mobile.ui.components.StaggerIn
import com.urbanblade.mobile.ui.components.UrbanBrandMark
import com.urbanblade.mobile.ui.components.UrbanErrorBanner
import com.urbanblade.mobile.ui.components.UrbanSuccessCheck
import com.urbanblade.mobile.ui.components.UrbanTextField
import com.urbanblade.mobile.ui.theme.MascotMood
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.theme.mascot
import com.urbanblade.mobile.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

private fun isValidEmail(value: String) = Patterns.EMAIL_ADDRESS.matcher(value.trim()).matches()

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onRegister: () -> Unit,
    onForgot: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var emailTouched by remember { mutableStateOf(false) }
    val busy by authViewModel.busy.collectAsState()
    val error by authViewModel.error.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var googleBusy by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { authViewModel.clearError() }

    val emailValid = isValidEmail(email)
    val canSubmit = emailValid && password.isNotBlank()
    fun submit() {
        if (canSubmit && !busy) authViewModel.login(email.trim(), password)
    }

    AuthScaffold(
        eyebrow = "ACCESO",
        title = "Bienvenido de vuelta",
        subtitle = "Entra para reservar, comprar y seguir tus beneficios.",
        mood = if (error != null) MascotMood.ERROR else MascotMood.DEFAULT,
        onBack = onBack
    ) {
        StaggerIn(1) {
            GoogleSignInButton(
                text = "Continuar con Google",
                loading = googleBusy,
                onClick = {
                    scope.launch {
                        googleBusy = true
                        val result = GoogleAuthHelper.requestGoogleIdToken(context)
                        googleBusy = false
                        when (result) {
                            is GoogleAuthHelper.Result.Token -> authViewModel.loginWithGoogle(result.value)
                            GoogleAuthHelper.Result.Unavailable -> authViewModel.reportGoogleUnavailable()
                            GoogleAuthHelper.Result.Cancelled -> authViewModel.reportGoogleCancelled()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
        StaggerIn(2) { OrDivider("o con tu correo") }
        StaggerIn(3) {
            UrbanTextField(
                value = email,
                onValueChange = { email = it },
                label = "Correo electrónico",
                leadingIcon = Icons.Default.AlternateEmail,
                placeholder = "nombre@correo.com",
                keyboardType = KeyboardType.Email,
                error = if (emailTouched && email.isNotBlank() && !emailValid) "Escribe un correo válido." else null,
                onBlur = { emailTouched = true }
            )
        }
        StaggerIn(4) {
            Column {
                UrbanTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Contraseña",
                    leadingIcon = Icons.Default.Lock,
                    isPassword = true,
                    imeAction = ImeAction.Done,
                    onImeAction = ::submit
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onForgot, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("¿Olvidaste tu contraseña?", color = UrbanColors.Gold, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        AnimatedVisibility(visible = error != null) {
            error?.let { UrbanErrorBanner(it) }
        }
        StaggerIn(5) {
            AuthPrimaryButton(
                text = "Entrar a UrbanBlade",
                onClick = ::submit,
                enabled = canSubmit,
                loading = busy,
                icon = Icons.Default.ArrowForward,
                modifier = Modifier.fillMaxWidth()
            )
        }
        StaggerIn(6) {
            SwitchAuthRow(prompt = "¿Eres nuevo?", action = "Crea tu cuenta", onClick = onRegister)
        }
    }
}

@Composable
fun RegisterScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    onLogin: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var emailTouched by remember { mutableStateOf(false) }
    val busy by authViewModel.busy.collectAsState()
    val error by authViewModel.error.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var googleBusy by remember { mutableStateOf(false) }

    // Mismo mínimo que AuthController::register() ('password' => ['min:8', 'confirmed']) --
    // validar aquí evita un viaje al servidor que de todos modos rechazaría la contraseña.
    LaunchedEffect(Unit) { authViewModel.clearError() }

    val passwordTooShort = password.isNotEmpty() && password.length < 8
    val passwordsMismatch = confirmation.isNotEmpty() && password != confirmation
    val emailValid = isValidEmail(email)
    val canSubmit = name.isNotBlank() && emailValid && password.length >= 8 && confirmation == password
    fun submit() {
        if (canSubmit && !busy) authViewModel.register(name.trim(), email.trim(), password, confirmation)
    }

    AuthScaffold(
        eyebrow = "NUEVA CUENTA",
        title = "Crea tu cuenta",
        subtitle = "Reserva, compra y administra tu experiencia UrbanBlade.",
        mood = if (error != null) MascotMood.ERROR else MascotMood.DEFAULT,
        onBack = onBack
    ) {
        StaggerIn(1) {
            GoogleSignInButton(
                text = "Registrarme con Google",
                loading = googleBusy,
                onClick = {
                    scope.launch {
                        googleBusy = true
                        val result = GoogleAuthHelper.requestGoogleIdToken(context)
                        googleBusy = false
                        when (result) {
                            is GoogleAuthHelper.Result.Token -> authViewModel.loginWithGoogle(result.value)
                            GoogleAuthHelper.Result.Unavailable -> authViewModel.reportGoogleUnavailable()
                            GoogleAuthHelper.Result.Cancelled -> authViewModel.reportGoogleCancelled()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
        StaggerIn(2) { OrDivider("o con tu correo") }
        StaggerIn(3) {
            UrbanTextField(
                value = name,
                onValueChange = { name = it },
                label = "Nombre",
                leadingIcon = Icons.Default.Person,
                capitalization = KeyboardCapitalization.Words
            )
        }
        StaggerIn(4) {
            UrbanTextField(
                value = email,
                onValueChange = { email = it },
                label = "Correo electrónico",
                leadingIcon = Icons.Default.AlternateEmail,
                placeholder = "nombre@correo.com",
                keyboardType = KeyboardType.Email,
                error = if (emailTouched && email.isNotBlank() && !emailValid) "Escribe un correo válido." else null,
                onBlur = { emailTouched = true }
            )
        }
        StaggerIn(5) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                UrbanTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Contraseña",
                    leadingIcon = Icons.Default.Lock,
                    isPassword = true,
                    error = if (passwordTooShort) "Mínimo 8 caracteres." else null,
                    helper = if (password.isEmpty()) "Mínimo 8 caracteres." else null
                )
                PasswordStrengthMeter(password)
            }
        }
        StaggerIn(6) {
            UrbanTextField(
                value = confirmation,
                onValueChange = { confirmation = it },
                label = "Confirmar contraseña",
                leadingIcon = Icons.Default.VerifiedUser,
                isPassword = true,
                imeAction = ImeAction.Done,
                onImeAction = ::submit,
                error = if (passwordsMismatch) "Las contraseñas no coinciden." else null
            )
        }
        AnimatedVisibility(visible = error != null) {
            error?.let { UrbanErrorBanner(it) }
        }
        StaggerIn(7) {
            AuthPrimaryButton(
                text = "Crear cuenta",
                onClick = ::submit,
                enabled = canSubmit,
                loading = busy,
                icon = Icons.Default.HowToReg,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (onLogin != null) {
            StaggerIn(8) {
                SwitchAuthRow(prompt = "¿Ya tienes cuenta?", action = "Inicia sesión", onClick = onLogin)
            }
        }
    }
}

@Composable
fun ForgotPasswordScreen(authViewModel: AuthViewModel, onBack: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var emailTouched by remember { mutableStateOf(false) }
    val busy by authViewModel.busy.collectAsState()
    val error by authViewModel.error.collectAsState()

    LaunchedEffect(Unit) { authViewModel.clearError() }

    val emailValid = isValidEmail(email)
    fun submit() {
        if (emailValid && !busy) authViewModel.forgotPassword(email.trim()) { message = it }
    }

    val sent = message
    AuthScaffold(
        eyebrow = if (sent == null) "RECUPERAR ACCESO" else "CORREO ENVIADO",
        title = if (sent == null) "Recupera tu acceso" else "Revisa tu correo",
        subtitle = if (sent == null) {
            "Te enviaremos las instrucciones al correo asociado a tu cuenta."
        } else {
            "Si el correo está registrado, te llegará un enlace para elegir una contraseña nueva."
        },
        mood = if (sent != null) MascotMood.SUCCESS else MascotMood.DEFAULT,
        onBack = onBack
    ) {
        if (sent == null) {
            StaggerIn(1) {
                UrbanTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Correo electrónico",
                    leadingIcon = Icons.Default.MailOutline,
                    placeholder = "nombre@correo.com",
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done,
                    onImeAction = ::submit,
                    error = if (emailTouched && email.isNotBlank() && !emailValid) "Escribe un correo válido." else null,
                    onBlur = { emailTouched = true },
                    helper = "Si no lo ves, revisa también tu bandeja de spam."
                )
            }
            AnimatedVisibility(visible = error != null) {
                error?.let { UrbanErrorBanner(it) }
            }
            StaggerIn(2) {
                AuthPrimaryButton(
                    text = "Enviar instrucciones",
                    onClick = ::submit,
                    enabled = emailValid,
                    loading = busy,
                    icon = Icons.Default.Send,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                UrbanSuccessCheck(diameter = 96.dp)
                Text(
                    sent,
                    style = MaterialTheme.typography.bodyMedium,
                    color = UrbanColors.Muted,
                    textAlign = TextAlign.Center
                )
                AuthPrimaryButton(
                    text = "Volver a iniciar sesión",
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SwitchAuthRow(prompt: String, action: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(prompt, style = MaterialTheme.typography.bodyMedium, color = UrbanColors.Muted)
        TextButton(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(action, color = UrbanColors.Gold, fontWeight = FontWeight.Bold)
        }
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
 * "G" de Google dibujada con sus cuatro colores de marca: anillo abierto arriba a la derecha y la
 * barra azul horizontal. Evita depender de un asset externo para un botón que solo aparece en 2
 * pantallas.
 */
@Composable
private fun GoogleGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val stroke = size.minDimension * 0.2f
        val diameter = size.minDimension - stroke
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize = Size(diameter, diameter)
        val style = Stroke(stroke)
        val blue = Color(0xFF4285F4)
        drawArc(Color(0xFFEA4335), startAngle = 225f, sweepAngle = 90f, useCenter = false, topLeft = topLeft, size = arcSize, style = style)
        drawArc(Color(0xFFFBBC05), startAngle = 135f, sweepAngle = 90f, useCenter = false, topLeft = topLeft, size = arcSize, style = style)
        drawArc(Color(0xFF34A853), startAngle = 45f, sweepAngle = 90f, useCenter = false, topLeft = topLeft, size = arcSize, style = style)
        drawArc(blue, startAngle = 0f, sweepAngle = 45f, useCenter = false, topLeft = topLeft, size = arcSize, style = style)
        drawLine(blue, Offset(size.width / 2f, size.height / 2f), Offset(size.width, size.height / 2f), strokeWidth = stroke)
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
        modifier = modifier.heightIn(min = 56.dp),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, UrbanColors.Line),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = UrbanColors.Card,
            contentColor = UrbanColors.Ink
        )
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = UrbanColors.Gold)
        } else {
            GoogleGlyph()
            Spacer(Modifier.width(10.dp))
            Text(text, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/**
 * Estructura común: barra superior con marca, cabecera (eyebrow, título, subtítulo y la mascota en
 * la expresión que toca) y el contenido en columna con separación de 16 dp.
 */
@Composable
private fun AuthScaffold(
    eyebrow: String,
    title: String,
    subtitle: String,
    mood: MascotMood,
    onBack: (() -> Unit)?,
    content: @Composable ColumnScope.() -> Unit
) {
    AuthBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
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

            Spacer(Modifier.height(12.dp))

            StaggerIn(0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(eyebrow, style = MaterialTheme.typography.labelMedium, color = UrbanColors.Gold)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = UrbanColors.Ink,
                            modifier = Modifier.semantics { heading() }
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = UrbanColors.Muted)
                    }
                    // La mascota solo aparece cuando comunica algo (un error o un envío correcto);
                    // en reposo era tan pequeña que no se reconocía y apretaba el título.
                    if (mood != MascotMood.DEFAULT) {
                        Spacer(Modifier.width(12.dp))
                        Image(
                            painter = painterResource(UrbanColors.current.mascot(mood)),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(84.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )

            Spacer(Modifier.height(20.dp))
        }
    }
}
