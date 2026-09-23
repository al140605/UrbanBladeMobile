package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import com.urbanblade.mobile.ui.theme.previewColors
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urbanblade.mobile.core.network.AppContainer
import com.urbanblade.mobile.data.model.AuthUser
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.theme.UrbanTheme
import com.urbanblade.mobile.ui.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

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
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { vm.load() }
    LaunchedEffect(profile) {
        profile?.let {
            name = it.name
            email = it.email
            phone = it.client?.telefono.orEmpty()
            birth = it.client?.fechaNacimiento.orEmpty()
            sex = it.client?.sexo.orEmpty()
        }
    }

    // "Guardar cambios" solo se activa cuando algo cambió respecto a lo que devolvió el servidor.
    val dirty = name != user.name || email != user.email ||
        (user.roles.contains("cliente") && (
            phone != user.client?.telefono.orEmpty() ||
                birth != user.client?.fechaNacimiento.orEmpty() ||
                sex != user.client?.sexo.orEmpty()
            ))

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            UrbanPageHeader(
                title = "Mi perfil",
                subtitle = "Tu identidad dentro de UrbanBlade.",
                eyebrow = "Cuenta"
            )
        }

        item {
            UrbanPremiumCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UrbanAvatar(name, Modifier.size(64.dp), imageUrl = user.avatarUrl)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(name, style = MaterialTheme.typography.titleLarge)
                        Text(email, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            user.roles.take(2).forEach { UrbanRolePill(it) }
                        }
                    }
                }
            }
        }

        if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = UrbanColors.Gold) }

        item {
            UrbanSectionTitle("Apariencia", "Elige el tema visual de UrbanBlade")
        }

        item {
            UrbanCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    UrbanTheme.entries.forEach { theme ->
                        UrbanThemeSwatch(
                            theme = theme,
                            selected = theme == UrbanColors.current,
                            onClick = {
                                UrbanColors.applyTheme(theme)
                                scope.launch { AppContainer.sessionManager.saveTheme(theme.key) }
                            }
                        )
                    }
                }
            }
        }

        item {
            UrbanSectionTitle("Información personal", "Mantén tus datos al día")
        }

        item {
            UrbanCard(Modifier.fillMaxWidth()) {
                UrbanFieldLabel("Nombre")
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    name,
                    { name = it },
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
                Spacer(Modifier.height(14.dp))
                UrbanFieldLabel("Correo")
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    email,
                    { email = it },
                    leadingIcon = { Icon(Icons.Default.AlternateEmail, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                if (user.roles.contains("cliente")) {
                    Spacer(Modifier.height(14.dp))
                    UrbanFieldLabel("Teléfono")
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        phone,
                        { phone = it },
                        leadingIcon = { Icon(Icons.Default.Phone, null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium
                    )
                    Spacer(Modifier.height(14.dp))
                    UrbanFieldLabel("Fecha de nacimiento")
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        birth,
                        { birth = it },
                        leadingIcon = { Icon(Icons.Default.Cake, null) },
                        placeholder = { Text("AAAA-MM-DD") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium
                    )
                    Spacer(Modifier.height(14.dp))
                    UrbanFieldLabel("Sexo")
                    Spacer(Modifier.height(6.dp))
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("masculino" to "Masculino", "femenino" to "Femenino", "prefiero_no_decir" to "Prefiero no decir").forEach { (value, label) ->
                            FilterChip(
                                selected = sex == value,
                                onClick = { sex = if (sex == value) "" else value },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        }

        message?.let { item { UrbanInfoBanner(it, Icons.Default.CheckCircle) } }
        error?.let { item { UrbanErrorBanner(it) } }

        item {
            UrbanPrimaryButton(
                text = "Guardar cambios",
                onClick = { vm.save(name, email, phone, birth, sex) },
                enabled = dirty,
                loading = busy,
                icon = Icons.Default.Save,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            UrbanOutlineButton(
                text = "Cerrar sesión",
                onClick = onLogout,
                icon = Icons.Default.Logout,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Text(
                "Tus datos están protegidos. Puedes cerrar tu sesión en cualquier momento.",
                style = MaterialTheme.typography.bodySmall,
                color = UrbanColors.Muted,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun UrbanThemeSwatch(theme: UrbanTheme, selected: Boolean, onClick: () -> Unit) {
    // Vista previa de dos tonos: fondo del tema + acento, para distinguir un tema de otro
    // (los acentos de Noir, Salón y Libreta son tres dorados muy parecidos por sí solos).
    val (previewBackground, previewAccent) = theme.previewColors()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 4.dp)
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(previewBackground)
                .border(
                    BorderStroke(if (selected) 2.5.dp else 1.dp, if (selected) UrbanColors.Ink else UrbanColors.Line),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(previewAccent)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            theme.label,
            style = MaterialTheme.typography.bodySmall,
            color = if (selected) UrbanColors.Gold else UrbanColors.Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(64.dp)
        )
    }
}
