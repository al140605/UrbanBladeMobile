package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urbanblade.mobile.data.model.AuthUser
import com.urbanblade.mobile.ui.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(user: AuthUser, onLogout: () -> Unit, vm: ProfileViewModel = viewModel()) {
    val profile by vm.profile.collectAsState()
    val busy by vm.busy.collectAsState()
    val message by vm.message.collectAsState()
    val error by vm.error.collectAsState()
    var name by remember { mutableStateOf(user.name) }
    var email by remember { mutableStateOf(user.email) }
    var phone by remember { mutableStateOf(user.client?.telefono.orEmpty()) }
    var birth by remember { mutableStateOf(user.client?.fechaNacimiento.orEmpty()) }
    var sex by remember { mutableStateOf(user.client?.sexo.orEmpty()) }

    LaunchedEffect(Unit) { vm.load() }
    LaunchedEffect(profile) {
        profile?.let {
            name = it.name; email = it.email; phone = it.client?.telefono.orEmpty()
            birth = it.client?.fechaNacimiento.orEmpty(); sex = it.client?.sexo.orEmpty()
        }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("Mi perfil", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text(user.roles.joinToString(" · "), color = MaterialTheme.colorScheme.primary)
        }
        if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        item { OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(email, { email = it }, label = { Text("Correo") }, modifier = Modifier.fillMaxWidth()) }
        if (user.roles.contains("cliente")) {
            item { OutlinedTextField(phone, { phone = it }, label = { Text("Teléfono") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(birth, { birth = it }, label = { Text("Fecha de nacimiento") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(sex, { sex = it }, label = { Text("Sexo") }, modifier = Modifier.fillMaxWidth(), supportingText = { Text("masculino, femenino o prefiero_no_decir") }) }
        }
        message?.let { item { Text(it, color = MaterialTheme.colorScheme.primary) } }
        error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        item { Button(onClick = { vm.save(name, email, phone, birth, sex) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Guardar cambios") } }
        item {
            OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Logout, null); Spacer(Modifier.width(8.dp)); Text("Cerrar sesión")
            }
        }
    }
}
