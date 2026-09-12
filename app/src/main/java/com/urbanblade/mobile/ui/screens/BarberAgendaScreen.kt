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
import com.urbanblade.mobile.data.model.AppointmentRow
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.BarberAgendaViewModel

private val ESTADOS = listOf("pendiente", "confirmada", "en_proceso", "completada", "cancelada", "no_asistio")

// Próximos estados razonables desde el estado actual -- no se ofrecen todos
// los 6 siempre (ej. no tiene sentido regresar una cita completada a
// pendiente desde la agenda).
private fun nextStatesFor(estado: String): List<String> = when (estado) {
    "pendiente" -> listOf("confirmada", "cancelada")
    "confirmada" -> listOf("en_proceso", "cancelada", "no_asistio")
    "en_proceso" -> listOf("completada", "cancelada")
    else -> emptyList()
}

@Composable
fun BarberAgendaScreen(onBack: () -> Unit, vm: BarberAgendaViewModel = viewModel()) {
    val agenda by vm.agenda.collectAsState()
    val busy by vm.busy.collectAsState()
    val updating by vm.updating.collectAsState()
    val error by vm.error.collectAsState()

    var period by remember { mutableStateOf("day") }
    var estadoFilter by remember { mutableStateOf<String?>(null) }
    var offset by remember { mutableIntStateOf(0) }

    LaunchedEffect(period, estadoFilter, offset) { vm.load(period, estadoFilter, offset) }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = { UrbanTopBar("Mi agenda", onBack) { IconButton(onClick = { vm.load(period, estadoFilter, offset) }) { Icon(Icons.Default.Refresh, "Actualizar") } } }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = UrbanColors.Gold) }
            error?.let { item { UrbanErrorBanner(it) } }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = period == "day", onClick = { period = "day"; offset = 0 }, label = { Text("Día") })
                    FilterChip(selected = period == "week", onClick = { period = "week"; offset = 0 }, label = { Text("Semana") })
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { offset -= 1 }) { Icon(Icons.Default.ChevronLeft, "Anterior") }
                    Text(
                        agenda.range.label ?: "—",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { offset += 1 }) { Icon(Icons.Default.ChevronRight, "Siguiente") }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    UrbanMetricCard("Citas del periodo", agenda.stats.totalPeriod.toString(), Icons.Default.CalendarMonth, Modifier.weight(1f))
                    UrbanMetricCard("Productividad", "${agenda.stats.productivity}%", Icons.Default.TrendingUp, Modifier.weight(1f))
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    UrbanMetricCard("Completadas", agenda.stats.completedPeriod.toString(), Icons.Default.CheckCircle, Modifier.weight(1f))
                    UrbanMetricCard("Ingreso acumulado", "\$${"%.0f".format(agenda.stats.incomeTotal)}", Icons.Default.Payments, Modifier.weight(1f))
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = estadoFilter == null, onClick = { estadoFilter = null }, label = { Text("Todas") })
                    ESTADOS.forEach { e ->
                        FilterChip(selected = estadoFilter == e, onClick = { estadoFilter = if (estadoFilter == e) null else e }, label = { Text(e.replace('_', ' ').replaceFirstChar { it.uppercase() }) })
                    }
                }
            }

            if (agenda.data.isEmpty() && !busy) {
                item { UrbanEmptyState("Sin citas", "No tienes citas en este rango.", Icons.Default.EventAvailable) }
            }

            items(agenda.data) { appt ->
                AgendaCard(appt, updating == appt.code) { estado ->
                    appt.code?.let { vm.updateStatus(it, estado, period, estadoFilter, offset) }
                }
            }
        }
    }
}

@Composable
private fun AgendaCard(appt: AppointmentRow, updating: Boolean, onChangeStatus: (String) -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    val next = nextStatesFor(appt.estado)

    UrbanCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(appt.service?.nombre ?: "—", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(appt.client?.user?.name ?: "Cliente", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                Text("${appt.fecha} · ${appt.horaInicio}", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
            }
            SimpleStatusPill(appt.estado)
        }
        if (next.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Box {
                UrbanOutlineButton(
                    text = if (updating) "Actualizando…" else "Cambiar estado",
                    onClick = { showMenu = true },
                    icon = Icons.Default.SwapVert,
                    modifier = Modifier.fillMaxWidth()
                )
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    next.forEach { estado ->
                        DropdownMenuItem(
                            text = { Text(estado.replace('_', ' ').replaceFirstChar { it.uppercase() }) },
                            onClick = { showMenu = false; onChangeStatus(estado) }
                        )
                    }
                }
            }
        }
    }
}
