package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urbanblade.mobile.data.model.SystemUserRow
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.SystemUsersViewModel

@Composable
fun UsersScreen(onBack: () -> Unit, vm: SystemUsersViewModel = viewModel()) {
    val response by vm.users.collectAsState()
    val busy by vm.busy.collectAsState()
    val saving by vm.saving.collectAsState()
    val message by vm.message.collectAsState()
    val error by vm.error.collectAsState()

    var search by remember { mutableStateOf("") }
    var roleFilter by remember { mutableStateOf<String?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var showEdit by remember { mutableStateOf<SystemUserRow?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<SystemUserRow?>(null) }

    LaunchedEffect(roleFilter) { vm.load(search.takeIf { it.isNotBlank() }, roleFilter) }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            UrbanTopBar("Usuarios", onBack) {
                IconButton(onClick = { showCreate = true }) { Icon(Icons.Default.PersonAdd, "Agregar usuario") }
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = UrbanColors.Gold) }
            error?.let { item { UrbanErrorBanner(it) } }
            message?.let { item { UrbanInfoBanner(it, Icons.Default.CheckCircle) } }

            item {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Buscar por nombre o correo…") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { vm.load(search.takeIf { it.isNotBlank() }, roleFilter) }),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search)
                )
            }

            if (response.roles.isNotEmpty()) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = roleFilter == null, onClick = { roleFilter = null }, label = { Text("Todos") })
                        response.roles.forEach { role ->
                            FilterChip(selected = roleFilter == role, onClick = { roleFilter = if (roleFilter == role) null else role }, label = { Text(role.replaceFirstChar { it.uppercase() }) })
                        }
                    }
                }
            }

            if (response.data.isEmpty() && !busy) {
                item { UrbanEmptyState("Sin usuarios", "No hay usuarios que coincidan con la búsqueda.", Icons.Default.Person) }
            }

            items(response.data) { user ->
                UrbanCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        UrbanAvatar(user.name, Modifier.size(44.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(user.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(user.email, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                user.roles.forEach { UrbanRolePill(it) }
                            }
                        }
                        IconButton(onClick = { showEdit = user }) { Icon(Icons.Default.Edit, "Editar") }
                        IconButton(onClick = { showDeleteConfirm = user }) { Icon(Icons.Default.Delete, "Eliminar", tint = UrbanColors.Danger) }
                    }
                }
            }
        }
    }

    if (showCreate) {
        UserFormDialog(
            title = "Nuevo usuario",
            initial = null,
            roles = response.roles,
            saving = saving,
            onDismiss = { showCreate = false },
            onSubmit = { name, email, password, role -> vm.create(name, email, password.orEmpty(), role) { showCreate = false } }
        )
    }

    showEdit?.let { user ->
        UserFormDialog(
            title = "Editar usuario",
            initial = user,
            roles = response.roles,
            saving = saving,
            onDismiss = { showEdit = null },
            onSubmit = { name, email, password, role -> vm.update(user.id, name, email, password, role) { showEdit = null } }
        )
    }

    showDeleteConfirm?.let { user ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("¿Eliminar a ${user.name}?") },
            text = { Text("Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = { vm.delete(user.id); showDeleteConfirm = null }) { Text("Sí, eliminar") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun UserFormDialog(
    title: String,
    initial: SystemUserRow?,
    roles: List<String>,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String?, String) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var email by remember { mutableStateOf(initial?.email.orEmpty()) }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(initial?.roles?.firstOrNull() ?: roles.firstOrNull().orEmpty()) }

    val isEdit = initial != null
    val valid = name.isNotBlank() && email.isNotBlank() && role.isNotBlank() && (isEdit || password.length >= 8)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            UrbanFormScroll(
                modifier = Modifier.heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(email, { email = it }, label = { Text("Correo") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    password, { password = it },
                    label = { Text(if (isEdit) "Nueva contraseña (opcional)" else "Contraseña") },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    roles.forEach { r ->
                        FilterChip(selected = role == r, onClick = { role = r }, label = { Text(r.replaceFirstChar { it.uppercase() }) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving && valid,
                onClick = { onSubmit(name, email, password.takeIf { it.isNotBlank() }, role) }
            ) { Text(if (saving) "Guardando…" else "Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
