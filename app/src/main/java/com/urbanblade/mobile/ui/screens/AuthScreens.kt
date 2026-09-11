package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.urbanblade.mobile.ui.viewmodel.AuthViewModel

@Composable
fun LoginScreen(authViewModel: AuthViewModel, onRegister: () -> Unit, onForgot: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val busy by authViewModel.busy.collectAsState()
    val error by authViewModel.error.collectAsState()

    AuthShell("Tu estilo. Tu tiempo.", "Accede a UrbanBlade desde Android.") {
        OutlinedTextField(email, { email = it }, label = { Text("Correo") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(password, { password = it }, label = { Text("Contraseña") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = { authViewModel.login(email, password) }, enabled = !busy && email.isNotBlank() && password.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
            if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text("Entrar")
        }
        TextButton(onClick = onForgot, modifier = Modifier.align(Alignment.End)) { Text("Olvidé mi contraseña") }
        HorizontalDivider()
        OutlinedButton(onClick = onRegister, modifier = Modifier.fillMaxWidth()) { Text("Crear cuenta") }
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
    AuthShell("Crear cuenta", "Reserva y administra tus citas.", onBack) {
        OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(email, { email = it }, label = { Text("Correo") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        OutlinedTextField(password, { password = it }, label = { Text("Contraseña") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation())
        OutlinedTextField(confirmation, { confirmation = it }, label = { Text("Confirmar contraseña") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = { authViewModel.register(name, email, password, confirmation) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
            Text(if (busy) "Creando…" else "Registrarme")
        }
    }
}

@Composable
fun ForgotPasswordScreen(authViewModel: AuthViewModel, onBack: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    val busy by authViewModel.busy.collectAsState()
    val error by authViewModel.error.collectAsState()
    AuthShell("Recuperar acceso", "Te enviaremos instrucciones por correo.", onBack) {
        OutlinedTextField(email, { email = it }, label = { Text("Correo") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        Button(onClick = { authViewModel.forgotPassword(email) { message = it } }, enabled = !busy && email.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
            Text(if (busy) "Enviando…" else "Enviar instrucciones")
        }
    }
}

@Composable
private fun AuthShell(title: String, subtitle: String, onBack: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 40.dp),
        verticalArrangement = Arrangement.Center
    ) {
        if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver") }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ContentCut, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
            Spacer(Modifier.width(10.dp))
            Text("URBANBLADE", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(28.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        content()
    }
}
