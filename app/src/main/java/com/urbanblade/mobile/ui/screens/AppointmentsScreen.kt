package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.urbanblade.mobile.data.model.SlotItem
import com.urbanblade.mobile.data.model.WaitlistEntry
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.AppointmentsViewModel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentsScreen(user: AuthUser, onBook: () -> Unit, vm: AppointmentsViewModel = viewModel()) {
    val response by vm.data.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val waitlistEntries by vm.waitlistEntries.collectAsState()
    var confirmCancel by remember { mutableStateOf<AppointmentRow?>(null) }
    var rescheduling by remember { mutableStateOf<AppointmentRow?>(null) }
    val canBook = user.roles.any { it in listOf("cliente", "administrador", "recepcionista") }
    val isClient = user.roles.contains("cliente")

    LaunchedEffect(Unit) {
        vm.load()
        if (isClient) vm.loadWaitlist()
    }

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
                val manageable = appt.estado in listOf("pendiente", "confirmada") && appt.code != null
                AppointmentCard(
                    appt = appt,
                    onCancel = if (manageable) { { confirmCancel = appt } } else null,
                    onReschedule = if (manageable && isClient) { { rescheduling = appt } } else null
                )
            }

            if (isClient && waitlistEntries.isNotEmpty()) {
                item { UrbanSectionTitle("Mi lista de espera", "Te avisamos si se libera un horario") }
                items(waitlistEntries, key = { it.id }) { entry ->
                    WaitlistCard(entry, onLeave = { vm.leaveWaitlist(entry.id) })
                }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }

    rescheduling?.let { appt ->
        RescheduleSheet(
            appt = appt,
            vm = vm,
            onDismiss = { rescheduling = null }
        )
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
private fun AppointmentCard(appt: AppointmentRow, onCancel: (() -> Unit)?, onReschedule: (() -> Unit)? = null) {
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
                if (onCancel != null || onReschedule != null) {
                    Spacer(Modifier.height(4.dp))
                    Row {
                        if (onReschedule != null) {
                            TextButton(onClick = onReschedule, contentPadding = PaddingValues(0.dp)) {
                                Icon(Icons.Default.EditCalendar, null, modifier = Modifier.size(17.dp), tint = UrbanColors.Gold)
                                Spacer(Modifier.width(5.dp))
                                Text("Reagendar", color = UrbanColors.Gold)
                            }
                            Spacer(Modifier.width(16.dp))
                        }
                        if (onCancel != null) {
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
    }
}

@Composable
private fun WaitlistCard(entry: WaitlistEntry, onLeave: () -> Unit) {
    UrbanCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(entry.service?.nombre ?: "Servicio", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${entry.barber?.name ?: "Cualquier barbero"} · ${entry.fecha ?: "—"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = UrbanColors.Muted
                )
            }
            UrbanStatusPill(entry.estado)
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onLeave) { Icon(Icons.Default.Close, "Salir de la lista de espera", tint = UrbanColors.Danger) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RescheduleSheet(appt: AppointmentRow, vm: AppointmentsViewModel, onDismiss: () -> Unit) {
    val barbers by vm.barbers.collectAsState()
    val slots by vm.rescheduleSlots.collectAsState()
    val busy by vm.rescheduling.collectAsState()
    val error by vm.error.collectAsState()

    var barberId by remember { mutableStateOf(appt.barber?.id.orEmpty()) }
    var date by remember { mutableStateOf(appt.fecha) }
    var time by remember { mutableStateOf("") }
    val serviceId = appt.service?.id.orEmpty()

    LaunchedEffect(Unit) { vm.loadBarbers() }
    LaunchedEffect(barberId, date) {
        time = ""
        if (barberId.isNotBlank() && date.isNotBlank()) vm.loadRescheduleSlots(barberId, serviceId, date)
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = UrbanColors.Card) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)) {
            UrbanSectionTitle("Reagendar cita", appt.service?.nombre)
            Spacer(Modifier.height(14.dp))

            Text("Profesional", style = MaterialTheme.typography.labelMedium, color = UrbanColors.Muted)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(barbers, key = { it.id }) { barber ->
                    val selected = barber.id == barberId
                    Surface(
                        onClick = { barberId = barber.id },
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) UrbanColors.Gold else UrbanColors.Background,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) UrbanColors.Gold else UrbanColors.Line)
                    ) {
                        Text(
                            barber.user?.name ?: "Barbero",
                            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) Color(0xFF080808) else UrbanColors.Ink
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Text("Fecha", style = MaterialTheme.typography.labelMedium, color = UrbanColors.Muted)
            Spacer(Modifier.height(8.dp))
            val days = remember { (0..29).map { LocalDate.now().plusDays(it.toLong()) } }
            val selectedDate = remember(date) { runCatching { LocalDate.parse(date) }.getOrNull() }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(days) { day ->
                    val selected = day == selectedDate
                    Surface(
                        onClick = { date = day.toString() },
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) UrbanColors.Gold else UrbanColors.Background,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) UrbanColors.Gold else UrbanColors.Line),
                        modifier = Modifier.width(54.dp)
                    ) {
                        Column(Modifier.padding(vertical = 8.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("es", "MX")).replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) Color(0xFF080808) else UrbanColors.Muted
                            )
                            Text(day.dayOfMonth.toString(), style = MaterialTheme.typography.titleMedium, color = if (selected) Color(0xFF080808) else UrbanColors.Ink)
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Text("Horario", style = MaterialTheme.typography.labelMedium, color = UrbanColors.Muted)
            Spacer(Modifier.height(8.dp))
            if (slots.isEmpty()) {
                Text("Sin horarios para este día.", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 180.dp)
                ) {
                    items(slots, key = { it.time }) { slot: SlotItem ->
                        val selected = slot.time == time
                        Surface(
                            onClick = { time = slot.time },
                            shape = MaterialTheme.shapes.medium,
                            color = if (selected) UrbanColors.Gold else UrbanColors.Background,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) UrbanColors.Gold else UrbanColors.Line)
                        ) {
                            Box(Modifier.padding(vertical = 10.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text(slot.label, style = MaterialTheme.typography.labelLarge, color = if (selected) Color(0xFF080808) else UrbanColors.Ink)
                            }
                        }
                    }
                }
            }

            error?.let { Spacer(Modifier.height(12.dp)); UrbanErrorBanner(it) }

            Spacer(Modifier.height(18.dp))
            UrbanPrimaryButton(
                text = "Confirmar reagendado",
                onClick = {
                    appt.code?.let { code -> vm.reschedule(code, barberId, serviceId, date, time, appt.notas, onDismiss) }
                },
                enabled = barberId.isNotBlank() && date.isNotBlank() && time.isNotBlank(),
                loading = busy,
                icon = Icons.Default.EditCalendar,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
