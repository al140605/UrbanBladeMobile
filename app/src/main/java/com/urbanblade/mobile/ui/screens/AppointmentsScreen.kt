package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urbanblade.mobile.data.model.AppointmentRow
import com.urbanblade.mobile.data.model.AuthUser
import com.urbanblade.mobile.ui.viewmodel.AppointmentsViewModel

@Composable
fun AppointmentsScreen(user: AuthUser, onBook: () -> Unit, vm: AppointmentsViewModel = viewModel()) {
    val response by vm.data.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    var confirmCancel by remember { mutableStateOf<AppointmentRow?>(null) }
    LaunchedEffect(Unit) { vm.load() }

    Scaffold(
        floatingActionButton = {
            if (user.roles.any { it in listOf("cliente", "administrador", "recepcionista") }) {
                FloatingActionButton(onClick = onBook) { Icon(Icons.Default.Add, "Nueva cita") }
            }
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("Citas", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Text("${response.stats.proximas} próximas · ${response.stats.completadas} completadas", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            if (!loading && response.data.isEmpty()) item {
                Card { Text("Aún no hay citas para mostrar.", Modifier.padding(20.dp)) }
            }
            items(response.data, key = { it.id }) { appt ->
                ElevatedCard {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(appt.service?.nombre ?: "Servicio", fontWeight = FontWeight.Bold)
                            SuggestionChip(onClick = {}, label = { Text(appt.estado.replace('_',' ')) })
                        }
                        Text("${appt.fecha} · ${appt.horaInicio.take(5)}")
                        Text(appt.barber?.user?.name ?: "Barbero por confirmar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (appt.estado in listOf("pendiente", "confirmada") && appt.code != null) {
                            TextButton(onClick = { confirmCancel = appt }) {
                                Icon(Icons.Default.DeleteOutline, null)
                                Spacer(Modifier.width(6.dp))
                                Text("Cancelar")
                            }
                        }
                    }
                }
            }
        }
    }

    confirmCancel?.let { appt ->
        AlertDialog(
            onDismissRequest = { confirmCancel = null },
            title = { Text("Cancelar cita") },
            text = { Text("¿Quieres cancelar la cita del ${appt.fecha} a las ${appt.horaInicio.take(5)}?") },
            confirmButton = { TextButton(onClick = { vm.cancel(appt); confirmCancel = null }) { Text("Sí, cancelar") } },
            dismissButton = { TextButton(onClick = { confirmCancel = null }) { Text("Volver") } }
        )
    }
}
