package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
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
// pendiente desde la agenda). El primero es la acción principal.
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
    var confirmChange by remember { mutableStateOf<Pair<AppointmentRow, String>?>(null) }
    val reload = { vm.load(period, estadoFilter, offset) }

    LaunchedEffect(period, estadoFilter, offset) { reload() }

    val change = { appt: AppointmentRow, estado: String ->
        if (isDestructiveStatus(estado)) confirmChange = appt to estado
        else appt.code?.let { vm.updateStatus(it, estado, period, estadoFilter, offset) }
    }

    UrbanModuleScreen(
        eyebrow = "BARBERO",
        title = "Mi agenda",
        subtitle = "Confirma, inicia y termina tus citas.",
        onBack = onBack,
        onRefresh = { reload() },
        refreshing = busy
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = period == "day", onClick = { period = "day"; offset = 0 }, label = { Text("Día") })
                    FilterChip(selected = period == "week", onClick = { period = "week"; offset = 0 }, label = { Text("Semana") })
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { offset -= 1 }) { Icon(Icons.Default.ChevronLeft, "Anterior") }
                    Text(
                        agenda.range.label ?: "—",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { offset += 1 }) { Icon(Icons.Default.ChevronRight, "Siguiente") }
                }
            }
        }

        item {
            UrbanHeroCard {
                UrbanHeroLabel(if (period == "day") "Tu día" else "Tu semana")
                Spacer(Modifier.height(6.dp))
                Text(
                    UrbanFormat.count(agenda.stats.totalPeriod, "cita", "citas"),
                    style = MaterialTheme.typography.displaySmall,
                    color = UrbanColors.Ink
                )
                Text("Productividad ${agenda.stats.productivity}%", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = UrbanColors.Ink.copy(alpha = 0.22f))
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    UrbanHeroStat("Terminadas", agenda.stats.completedPeriod.toString(), Icons.Default.CheckCircle, Modifier.weight(1f), tone = UrbanColors.Success)
                    UrbanHeroStat("Tus ingresos", "\$" + "%,.0f".format(agenda.stats.incomeTotal), Icons.Default.Payments, Modifier.weight(1f))
                }
            }
        }

        item {
            // Siete filtros no caben en una fila: se desplazan de lado.
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = estadoFilter == null, onClick = { estadoFilter = null }, label = { Text("Todas") })
                ESTADOS.forEach { e ->
                    FilterChip(
                        selected = estadoFilter == e,
                        onClick = { estadoFilter = if (estadoFilter == e) null else e },
                        label = { Text(statusLabel(e)) }
                    )
                }
            }
        }

        when {
            error != null && agenda.data.isEmpty() -> item {
                UrbanMascotState(UrbanStateKind.ERROR, "No pudimos cargar tu agenda", error, "Reintentar") { reload() }
            }
            agenda.data.isEmpty() && !busy -> item {
                UrbanMascotState(
                    UrbanStateKind.EMPTY,
                    if (estadoFilter == null) "Sin citas en este periodo" else "Sin citas con este estado",
                    "Cambia de día o de semana con las flechas."
                )
            }
            else -> {
                error?.let { item { UrbanErrorBanner(it) } }
                items(agenda.data, key = { it.id }) { appt ->
                    AgendaCard(appt, updating == appt.code, showDate = period == "week") { estado -> change(appt, estado) }
                }
            }
        }
    }

    confirmChange?.let { (appt, estado) ->
        AlertDialog(
            onDismissRequest = { confirmChange = null },
            title = { Text(if (estado == "cancelada") "¿Cancelar esta cita?" else "¿Marcar que no asistió?") },
            text = {
                Text(
                    "${appt.client?.user?.name ?: "El cliente"} · ${UrbanFormat.time(appt.horaInicio)}. No se puede deshacer desde la agenda."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmChange = null
                    appt.code?.let { vm.updateStatus(it, estado, period, estadoFilter, offset) }
                }) { Text(statusActionLabel(estado), color = UrbanColors.Danger) }
            },
            dismissButton = { TextButton(onClick = { confirmChange = null }) { Text("Volver") } }
        )
    }
}

@Composable
private fun AgendaCard(appt: AppointmentRow, updating: Boolean, showDate: Boolean, onChangeStatus: (String) -> Unit) {
    val next = nextStatesFor(appt.estado)

    UrbanCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(UrbanColors.Gold.copy(alpha = 0.14f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(UrbanFormat.time(appt.horaInicio), style = MaterialTheme.typography.titleSmall, color = UrbanColors.Gold)
                if (showDate) Text(UrbanFormat.date(appt.fecha).substringBefore(' '), style = MaterialTheme.typography.labelSmall, color = UrbanColors.Muted)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(appt.service?.nombre ?: "—", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(appt.client?.user?.name ?: "Cliente", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            SimpleStatusPill(statusLabel(appt.estado), statusTone(appt.estado))
        }
        appt.notas?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(8.dp))
            Text("“$it”", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (next.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            UrbanPrimaryButton(
                text = statusActionLabel(next.first()),
                onClick = { onChangeStatus(next.first()) },
                loading = updating,
                modifier = Modifier.fillMaxWidth()
            )
            if (next.size > 1) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    next.drop(1).forEach { estado ->
                        TextButton(onClick = { onChangeStatus(estado) }, enabled = !updating) {
                            Text(statusActionLabel(estado), color = if (isDestructiveStatus(estado)) UrbanColors.Danger else UrbanColors.Gold)
                        }
                    }
                }
            }
        }
    }
}

private fun statusTone(estado: String) = when (estado) {
    "pendiente" -> UrbanColors.Warning
    "confirmada" -> UrbanColors.Info
    "en_proceso" -> UrbanColors.Gold
    "completada" -> UrbanColors.Success
    "cancelada", "no_asistio" -> UrbanColors.Danger
    else -> UrbanColors.Muted
}
