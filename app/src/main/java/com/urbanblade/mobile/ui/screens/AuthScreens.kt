package com.urbanblade.mobile.ui.screens

import android.util.Patterns
import com.urbanblade.mobile.R
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
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
import com.urbanblade.mobile.ui.components.UrbanPublicPanel
import com.urbanblade.mobile.ui.components.AuthPrimaryButton
import com.urbanblade.mobile.ui.components.LegalConsentCheckbox
import com.urbanblade.mobile.ui.components.LegalLinksText
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
import com.urbanblade.mobile.ui.theme.UrbanSpace
import com.urbanblade.mobile.ui.components.UrbanTextButton

private fun isValidEmail(value: String) = Patterns.EMAIL_ADDRESS.matcher(value.trim()).matches()

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onRegister: () -> Unit,
    onForgot: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    var email by rememberSaveable { mutableStateOf("") }
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
        title = "Qué gusto verte.",
        subtitle = "Entra para reservar, comprar y seguir tus beneficios.",
        mood = if (error != null) MascotMood.ERROR else MascotMood.DEFAULT,
        onBack = onBack,
        footer = {
            AuthPrimaryButton(
                text = "Iniciar sesión",
                onClick = ::submit,
                enabled = canSubmit,
                loading = busy,
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            // El camino alterno viaja con el botón: así el hueco de arriba se lee como aire y no
            // como un botón suelto al fondo.
            SwitchAuthRow(prompt = "¿Eres nuevo?", action = "Crea tu cuenta", onClick = onRegister)
        }
    ) {
        StaggerIn(1) {
            GoogleSignInButton(
                text = "Continuar con Google",
                loading = googleBusy || busy,
                enabled = !busy,
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
        // Continuar con Google crea la cuenta si no existe: el aviso deja claro qué se acepta.
        StaggerIn(2) { LegalLinksText("Al continuar con Google aceptas los ") }
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
                    UrbanTextButton(onClick = onForgot, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("¿Olvidaste tu contraseña?", color = UrbanColors.GoldText, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        AnimatedVisibility(visible = error != null) {
            error?.let { UrbanErrorBanner(it) }
        }
    }
}

@Composable
fun RegisterScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    onLogin: (() -> Unit)? = null
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var emailTouched by remember { mutableStateOf(false) }
    val busy by authViewModel.busy.collectAsState()
    val error by authViewModel.error.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var googleBusy by remember { mutableStateOf(false) }
    // Consentimiento obligatorio (igual que la web): Términos, Aviso de Privacidad y mayoría de edad.
    var acceptedLegal by rememberSaveable { mutableStateOf(false) }
    // Dos pasos (auditoría del 24-sep): la pantalla era demasiado larga y densa de una vez.
    var step by rememberSaveable { mutableStateOf(0) }

    // Mismo mínimo que AuthController::register() ('password' => ['min:8', 'confirmed']) --
    // validar aquí evita un viaje al servidor que de todos modos rechazaría la contraseña.
    LaunchedEffect(Unit) { authViewModel.clearError() }

    val passwordTooShort = password.isNotEmpty() && password.length < 8
    val passwordsMismatch = confirmation.isNotEmpty() && password != confirmation
    val emailValid = isValidEmail(email)
    val stepOneValid = name.isNotBlank() && emailValid && acceptedLegal
    val stepTwoValid = password.length >= 8 && confirmation == password
    val canSubmit = stepOneValid && stepTwoValid
    fun submit() {
        if (canSubmit && !busy) authViewModel.register(name.trim(), email.trim(), password, confirmation)
    }
    // La flecha de la barra vuelve al paso anterior, no fuera de la pantalla.
    val backAction: () -> Unit = if (step == 1) ({ step = 0 }) else onBack
    androidx.activity.compose.BackHandler(enabled = step == 1, onBack = backAction)

    AuthScaffold(
        eyebrow = "NUEVA CUENTA",
        title = if (step == 0) "Empieza tu historia." else "Tu contraseña",
        subtitle = if (step == 0) {
            "Tus datos, y después la contraseña."
        } else {
            "Elige una contraseña de al menos 8 caracteres."
        },
        mood = if (error != null) MascotMood.ERROR else MascotMood.DEFAULT,
        onBack = backAction,
        footer = {
            if (step == 0) {
                AuthPrimaryButton(
                    text = "Continuar",
                    onClick = { step = 1 },
                    enabled = stepOneValid,
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                AuthPrimaryButton(
                    text = "Crear cuenta",
                    onClick = ::submit,
                    enabled = canSubmit,
                    loading = busy,
                    icon = Icons.Default.HowToReg,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (step == 0 && onLogin != null) {
                Spacer(Modifier.height(8.dp))
                SwitchAuthRow(prompt = "¿Ya tienes cuenta?", action = "Inicia sesión", onClick = onLogin)
            }
        }
    ) {
        StaggerIn(0) {
            StepBar(step = step, total = 2)
        }
        if (step == 0) {
            StaggerIn(1) {
                LegalConsentCheckbox(acceptedLegal, { acceptedLegal = it })
            }
            StaggerIn(1) {
                GoogleSignInButton(
                    text = "Registrarme con Google",
                    loading = googleBusy || busy,
                    enabled = acceptedLegal && !busy,
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
        } else {
            StaggerIn(1) {
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
            StaggerIn(2) {
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
            StaggerIn(4) {
                SwitchAuthRow(prompt = "¿Algo mal en tus datos?", action = "Volver", onClick = { step = 0 })
            }
        }
        AnimatedVisibility(visible = error != null) {
            error?.let { UrbanErrorBanner(it) }
        }
    }
}

@Composable
fun ForgotPasswordScreen(authViewModel: AuthViewModel, onBack: () -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
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
        onBack = onBack,
        footer = if (sent == null) {
            {
                AuthPrimaryButton(
                    text = "Enviar instrucciones",
                    onClick = ::submit,
                    enabled = emailValid,
                    loading = busy,
                    icon = Icons.AutoMirrored.Filled.Send,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            null
        }
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
                    onBlur = { emailTouched = true }
                )
            }
            AnimatedVisibility(visible = error != null) {
                error?.let { UrbanErrorBanner(it) }
            }
            StaggerIn(3) {
                Text(
                    "Si no lo ves en unos minutos, revisa tu bandeja de spam.",
                    style = MaterialTheme.typography.bodySmall,
                    color = UrbanColors.Muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            StaggerIn(4) {
                SwitchAuthRow(prompt = "¿Ya la recordaste?", action = "Inicia sesión", onClick = onBack)
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

/** Avance del registro: un segmento por paso, el actual y los anteriores en dorado. */
@Composable
private fun StepBar(step: Int, total: Int) {
    Column(Modifier.semantics { contentDescription = "Paso ${step + 1} de $total" }) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(total) { index ->
                Box(
                    Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp))
                        .background(if (index <= step) UrbanColors.Gold else UrbanColors.Line)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("Paso ${step + 1} de $total", style = MaterialTheme.typography.labelMedium, color = UrbanColors.GoldText)
    }
}

@Composable
private fun SwitchAuthRow(prompt: String, action: String, onClick: () -> Unit) {
    if (LocalDensity.current.fontScale > 1.15f) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(prompt, style = MaterialTheme.typography.bodyMedium, color = UrbanColors.Muted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            UrbanTextButton(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(action, color = UrbanColors.GoldText, fontWeight = FontWeight.Bold)
            }
        }
        return
    }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(prompt, style = MaterialTheme.typography.bodyMedium, color = UrbanColors.Muted)
        UrbanTextButton(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(action, color = UrbanColors.GoldText, fontWeight = FontWeight.Bold)
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
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.heightIn(min = 56.dp),
        shape = RoundedCornerShape(16.dp),
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
 * Mismo patrón que la bienvenida: fotografía a pantalla completa con velo, la marca oficial arriba y
 * el título sobre la foto; el formulario va en un panel opaco abajo. Con el teclado abierto la foto y
 * el título ceden el espacio y el pie queda fijo para no perder la acción principal.
 */
@Composable
private fun AuthScaffold(
    eyebrow: String,
    title: String,
    subtitle: String,
    mood: MascotMood,
    onBack: (() -> Unit)?,
    footer: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val keyboardOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val gold = Color(0xFFF0D68C)
    Box(Modifier.fillMaxSize().background(Color(0xFF080808))) {
        Image(
            painterResource(R.drawable.public_welcome_portrait), null,
            Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alignment = Alignment.TopCenter
        )
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(
            0f to Color(0xA0000000),
            0.22f to Color(0x80000000),
            0.42f to Color(0xC0080808),
            0.60f to Color(0xE6080808),
            1f to Color(0xF2080808)
        )))
        Column(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White) }
                } else Spacer(Modifier.size(48.dp))
                Spacer(Modifier.width(4.dp))
                Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.size(44.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("URBANBLADE", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("BARBERÍA · ESTILO · CUIDADO", style = MaterialTheme.typography.labelSmall, color = gold)
                }
            }
            BoxWithConstraints(Modifier.weight(1f)) {
                val viewportHeight = maxHeight
                Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                        .heightIn(min = viewportHeight).padding(horizontal = UrbanSpace.Gutter),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    if (!keyboardOpen) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Column(Modifier.weight(1f)) {
                                Text(eyebrow, style = MaterialTheme.typography.labelMedium, color = gold)
                                Spacer(Modifier.height(6.dp))
                                Text(title, style = MaterialTheme.typography.displaySmall, color = Color.White,
                                    modifier = Modifier.semantics { heading() })
                                Spacer(Modifier.height(8.dp))
                                Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = Color(0xFFE5E0D8))
                            }
                            if (mood != MascotMood.DEFAULT) {
                                Spacer(Modifier.width(12.dp))
                                Image(painterResource(UrbanColors.current.mascot(mood)), null,
                                    modifier = Modifier.size(64.dp), contentScale = ContentScale.Fit)
                            }
                        }
                        Spacer(Modifier.height(UrbanSpace.Md))
                    }
                    UrbanPublicPanel(outlined = true, spacing = 16.dp) {
                        if (keyboardOpen) Text(title, style = MaterialTheme.typography.headlineSmall, color = UrbanColors.Ink,
                            modifier = Modifier.semantics { heading() })
                        content()
                        if (!keyboardOpen) footer?.invoke()
                    }
                    Spacer(Modifier.height(UrbanSpace.Md))
                }
            }
            if (keyboardOpen && footer != null) {
                Column(Modifier.fillMaxWidth().background(UrbanColors.Card).padding(horizontal = 24.dp, vertical = 8.dp)) {
                    footer()
                }
            }
        }
    }
}
