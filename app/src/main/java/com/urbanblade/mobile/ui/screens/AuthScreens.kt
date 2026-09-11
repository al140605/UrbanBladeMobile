package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.AuthViewModel

@Composable
fun LoginScreen(authViewModel: AuthViewModel, onRegister: () -> Unit, onForgot: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    val busy by authViewModel.busy.collectAsState()
    val error by authViewModel.error.collectAsState()

    AuthShell(
        title = "Tu estilo. Tu tiempo.",
        subtitle = "Todo UrbanBlade en la palma de tu mano.",
        helper = "Nava cuida la entrada. Tú solo trae tus credenciales."
    ) {
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
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(Modifier.weight(1f), color = UrbanColors.Line)
            Text("  ¿Eres nuevo?  ", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
            HorizontalDivider(Modifier.weight(1f), color = UrbanColors.Line)
        }
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
    val busy by authViewModel.busy.collectAsState()
    val error by authViewModel.error.collectAsState()

    AuthShell(
        title = "Crea tu cuenta",
        subtitle = "Reserva, compra y administra tu experiencia UrbanBlade.",
        helper = "Tu cuenta se conecta al mismo ecosistema de la versión web.",
        onBack = onBack
    ) {
        UrbanFieldLabel("Nombre")
        OutlinedTextField(name, { name = it }, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Person, null) }, singleLine = true, shape = MaterialTheme.shapes.medium)
        Spacer(Modifier.height(12.dp))
        UrbanFieldLabel("Correo")
        OutlinedTextField(email, { email = it }, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.AlternateEmail, null) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), shape = MaterialTheme.shapes.medium)
        Spacer(Modifier.height(12.dp))
        UrbanFieldLabel("Contraseña")
        OutlinedTextField(password, { password = it }, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Lock, null) }, visualTransformation = PasswordVisualTransformation(), singleLine = true, shape = MaterialTheme.shapes.medium)
        Spacer(Modifier.height(12.dp))
        UrbanFieldLabel("Confirmar contraseña")
        OutlinedTextField(confirmation, { confirmation = it }, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.VerifiedUser, null) }, visualTransformation = PasswordVisualTransformation(), singleLine = true, shape = MaterialTheme.shapes.medium)
        Spacer(Modifier.height(14.dp))
        error?.let { UrbanErrorBanner(it); Spacer(Modifier.height(12.dp)) }
        UrbanPrimaryButton(
            text = "Crear cuenta",
            onClick = { authViewModel.register(name, email, password, confirmation) },
            enabled = name.isNotBlank() && email.isNotBlank() && password.isNotBlank() && confirmation.isNotBlank(),
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
private fun AuthShell(
    title: String,
    subtitle: String,
    helper: String,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
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

            UrbanPremiumCard(Modifier.fillMaxWidth()) {
                content()
            }

            Spacer(Modifier.height(16.dp))
            UrbanInfoBanner(helper, Icons.Default.ContentCut)
            Spacer(Modifier.weight(0.65f))
            Text(
                "SASTRERÍA NOCTURNA · ANDROID",
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 18.dp),
                style = MaterialTheme.typography.labelMedium,
                color = UrbanColors.Muted
            )
        }
    }
}
