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
import com.urbanblade.mobile.data.model.AuthUser
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.ClientsViewModel

private val SEGMENT_LABEL = mapOf(
    "vip" to "VIP",
    "new" to "Nuevo",
    "active" to "Activo",
    "inactive" to "Inactivo",
)

@Composable
fun ClientsListScreen(user: AuthUser, onClientClick: (String) -> Unit, onBack: () -> Unit, vm: ClientsViewModel = viewModel()) {
    val clients by vm.clients.collectAsState()
    val busy by vm.busy.collectAsState()
    val error by vm.error.collectAsState()
    var search by remember { mutableStateOf("") }
    var segment by remember { mutableStateOf<String?>(null) }
    var showCreate by remember { mutableStateOf(false) }

    LaunchedEffect(segment) { vm.load(search.takeIf { it.isNotBlank() }, segment) }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            UrbanTopBar("Clientes", onBack) {
                IconButton(onClick = { showCreate = true }) { Icon(Icons.Default.PersonAdd, "Agregar cliente") }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Buscar por nombre, correo o teléfono…") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { vm.load(search.takeIf { it.isNotBlank() }, segment) }),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search)
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = segment == null, onClick = { segment = null }, label = { Text("Todos") })
                    SEGMENT_LABEL.forEach { (key, label) ->
                        FilterChip(selected = segment == key, onClick = { segment = if (segment == key) null else key }, label = { Text(label) })
                    }
                }
            }

            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = UrbanColors.Gold) }
                error?.let { item { UrbanErrorBanner(it) } }
                if (clients.data.isEmpty() && !busy) {
                    item { UrbanEmptyState("Sin clientes", "No hay clientes que coincidan con la búsqueda.", Icons.Default.Groups) }
                }
                items(clients.data) { client ->
                    UrbanCard(Modifier.fillMaxWidth(), onClick = { onClientClick(client.id) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            UrbanAvatar(client.name ?: "?", Modifier.size(48.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(client.name ?: "Sin nombre", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(client.email ?: client.telefono ?: "—", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                client.segment?.let { SimpleStatusPill(SEGMENT_LABEL[it] ?: it, color = if (it == "vip") UrbanColors.Gold else null) }
                                Spacer(Modifier.height(4.dp))
                                Text("\$${"%.2f".format(client.totalSpent)}", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        CreateClientDialog(
            onDismiss = { showCreate = false },
            onCreate = { name, email, telefono, password ->
                vm.create(name, email, telefono, password) {
                    showCreate = false
                    vm.load(search.takeIf { it.isNotBlank() }, segment)
                }
            },
            saving = vm.saving.collectAsState().value
        )
    }
}

@Composable
private fun CreateClientDialog(onDismiss: () -> Unit, onCreate: (String, String, String?, String) -> Unit, saving: Boolean) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo cliente") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(email, { email = it }, label = { Text("Correo") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(telefono, { telefono = it }, label = { Text("Teléfono (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(password, { password = it }, label = { Text("Contraseña") }, singleLine = true, visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving && name.isNotBlank() && email.isNotBlank() && password.length >= 8,
                onClick = { onCreate(name, email, telefono.takeIf { it.isNotBlank() }, password) }
            ) { Text(if (saving) "Guardando…" else "Crear") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun ClientDetailScreen(user: AuthUser, clientId: String, onBack: () -> Unit, vm: ClientsViewModel = viewModel()) {
    val detail by vm.detail.collectAsState()
    val busy by vm.busy.collectAsState()
    val saving by vm.saving.collectAsState()
    val message by vm.message.collectAsState()
    val error by vm.error.collectAsState()
    var editing by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var notas by remember { mutableStateOf("") }

    LaunchedEffect(clientId) { vm.loadDetail(clientId) }
    LaunchedEffect(detail) {
        detail?.let {
            name = it.name.orEmpty()
            email = it.email.orEmpty()
            telefono = it.telefono.orEmpty()
            notas = it.notas.orEmpty()
        }
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            UrbanTopBar(detail?.name ?: "Cliente", onBack) {
                if (user.roles.contains("administrador")) {
                    IconButton(onClick = { showDeleteConfirm = true }) { Icon(Icons.Default.Delete, "Eliminar cliente") }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = UrbanColors.Gold) }
            error?.let { item { UrbanErrorBanner(it) } }
            message?.let { item { UrbanInfoBanner(it, Icons.Default.CheckCircle) } }

            detail?.let { d ->
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        UrbanMetricCard("Nivel", d.nivel?.replaceFirstChar { it.uppercase() } ?: "—", Icons.Default.Star, Modifier.weight(1f))
                        UrbanMetricCard("Puntos", d.puntos.toString(), Icons.Default.Toll, Modifier.weight(1f))
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        UrbanMetricCard("Citas totales", d.totalAppointments.toString(), Icons.Default.CalendarMonth, Modifier.weight(1f))
                        UrbanMetricCard("Total gastado", "\$${"%.2f".format(d.totalSpent)}", Icons.Default.Payments, Modifier.weight(1f))
                    }
                }
                item {
                    UrbanCard(Modifier.fillMaxWidth()) {
                        UrbanKeyValue("Barbero preferido", d.preferredBarber ?: "N/A")
                        UrbanKeyValue("Última cita", d.lastAppointment ?: "—", Modifier.padding(top = 6.dp))
                        UrbanKeyValue("Cliente desde", d.joinedAt ?: "—", Modifier.padding(top = 6.dp))
                    }
                }

                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        UrbanSectionTitle("Datos de contacto")
                        TextButton(onClick = { editing = !editing }) { Text(if (editing) "Cancelar" else "Editar") }
                    }
                }
                item {
                    UrbanCard(Modifier.fillMaxWidth()) {
                        if (editing) {
                            OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(email, { email = it }, label = { Text("Correo") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(telefono, { telefono = it }, label = { Text("Teléfono") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(notas, { notas = it }, label = { Text("Notas") }, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(12.dp))
                            UrbanPrimaryButton(
                                text = "Guardar cambios",
                                onClick = {
                                    vm.update(clientId, name.takeIf { it.isNotBlank() }, email.takeIf { it.isNotBlank() }, telefono.takeIf { it.isNotBlank() }, notas)
                                    editing = false
                                },
                                loading = saving,
                                icon = Icons.Default.Save,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            UrbanKeyValue("Correo", d.email ?: "—")
                            UrbanKeyValue("Teléfono", d.telefono ?: "—", Modifier.padding(top = 6.dp))
                            if (!d.notas.isNullOrBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(d.notas, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                            }
                        }
                    }
                }

                if (d.appointments.isNotEmpty()) {
                    item { UrbanSectionTitle("Historial de citas", "${d.appointments.size} citas") }
                    items(d.appointments) { appt ->
                        UrbanCard(Modifier.fillMaxWidth()) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(appt.service ?: "—", style = MaterialTheme.typography.titleMedium)
                                    Text("${appt.fecha ?: "—"} · ${appt.barber ?: "—"}", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                                }
                                appt.estado?.let { SimpleStatusPill(it) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("¿Eliminar cliente?") },
            text = { Text("Esta acción no se puede deshacer. Un cliente con citas registradas no se puede eliminar.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    vm.delete(clientId, onBack)
                }) { Text("Sí, eliminar") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar") } }
        )
    }
}
