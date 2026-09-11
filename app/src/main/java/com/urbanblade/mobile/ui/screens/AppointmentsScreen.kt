package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urbanblade.mobile.data.model.AppointmentRow
import com.urbanblade.mobile.data.model.AuthUser
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.AppointmentsViewModel

@Composable
fun AppointmentsScreen(user: AuthUser, onBook: () -> Unit, vm: AppointmentsViewModel = viewModel()) {
    val response by vm.data.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    var confirmCancel by remember { mutableStateOf<AppointmentRow?>(null) }
    val canBook = user.roles.any { it in listOf("cliente", "administrador", "recepcionista") }

    LaunchedEffect(Unit) { vm.load() }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            if (canBook) {
                ExtendedFloatingActionButton(
                    onClick = onBook,
                    containerColor = UrbanColors.Gold,
                    contentColor = Color(0xFF080808),
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("Nueva cita", fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                UrbanPageHeader(
                    title = "Citas",
                    subtitle = "Tu agenda, ordenada y siempre conectada.",
                    eyebrow = "Agenda"
                )
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    UrbanMetricCard(
                        label = "Próximas",
                        value = response.stats.proximas.toString(),
                        icon = Icons.Default.Upcoming,
                        modifier = Modifier.weight(1f)
                    )
                    UrbanMetricCard(
                        label = "Completadas",
                        value = response.stats.completadas.toString(),
                        icon = Icons.Default.TaskAlt,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = UrbanColors.Gold) }
            error?.let { item { UrbanErrorBanner(it) } }

            if (!loading && response.data.isEmpty()) {
                item {
                    UrbanPremiumCard(Modifier.fillMaxWidth()) {
                        UrbanEmptyState(
                            title = "Tu agenda está libre",
                            subtitle = "Cuando tengas una cita aparecerá aquí.",
                            icon = Icons.Default.EventAvailable,
                            actionLabel = if (canBook) "Reservar ahora" else null,
                            onAction = if (canBook) onBook else null
                        )
                    }
                }
            }

            if (response.data.isNotEmpty()) {
                item { UrbanSectionTitle("Tu agenda", "${response.stats.total} citas registradas") }
            }

            items(response.data, key = { it.id }) { appt ->
                AppointmentCard(
                    appt = appt,
                    onCancel = if (appt.estado in listOf("pendiente", "confirmada") && appt.code != null) {
                        { confirmCancel = appt }
                    } else null
                )
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }

    confirmCancel?.let { appt ->
        AlertDialog(
            onDismissRequest = { confirmCancel = null },
            containerColor = UrbanColors.Card,
            title = { Text("Cancelar cita") },
            text = {
                Text(
                    "¿Quieres cancelar la cita del ${appt.fecha} a las ${appt.horaInicio.take(5)}? Esta acción se enviará al backend.",
                    color = UrbanColors.Muted
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.cancel(appt); confirmCancel = null }) {
                    Text("Sí, cancelar", color = UrbanColors.Danger)
                }
            },
            dismissButton = { TextButton(onClick = { confirmCancel = null }) { Text("Volver") } }
        )
    }
}

@Composable
private fun AppointmentCard(appt: AppointmentRow, onCancel: (() -> Unit)?) {
    UrbanPremiumCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            Surface(
                modifier = Modifier.size(width = 66.dp, height = 72.dp),
                color = Color(0x18D4AF37),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x40D4AF37))
            ) {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(appt.fecha.takeLast(2), style = MaterialTheme.typography.headlineMedium, color = UrbanColors.Gold)
                    Text(appt.horaInicio.take(5), style = MaterialTheme.typography.labelMedium, color = UrbanColors.Ink)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(appt.service?.nombre ?: "Servicio", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    UrbanStatusPill(appt.estado)
                }
                Text(appt.barber?.user?.name ?: "Barbero por confirmar", style = MaterialTheme.typography.bodyMedium, color = UrbanColors.Muted)
                Text(appt.fecha, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                appt.notas?.takeIf { it.isNotBlank() }?.let {
                    Text("“$it”", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Ink)
                }
                if (onCancel != null) {
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = onCancel, contentPadding = PaddingValues(0.dp)) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(17.dp), tint = UrbanColors.Danger)
                        Spacer(Modifier.width(5.dp))
                        Text("Cancelar cita", color = UrbanColors.Danger)
                    }
                }
            }
        }
    }
}
