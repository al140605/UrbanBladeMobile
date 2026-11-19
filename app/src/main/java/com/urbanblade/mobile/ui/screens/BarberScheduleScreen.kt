package com.urbanblade.mobile.ui.screens

import com.urbanblade.mobile.ui.components.UrbanSwitch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.width
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urbanblade.mobile.data.model.BarberScheduleDay
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.*
import com.urbanblade.mobile.ui.components.UrbanModuleScreen


private val WEEK_DAYS = listOf(
    "monday" to "Lunes",
    "tuesday" to "Martes",
    "wednesday" to "Miércoles",
    "thursday" to "Jueves",
    "friday" to "Viernes",
    "saturday" to "Sábado",
    "sunday" to "Domingo",
)

@Composable
fun BarberScheduleScreen(onBack: () -> Unit, vm: BarberScheduleViewModel = viewModel()) {
    val loaded by vm.schedules.collectAsState()
    val busy by vm.busy.collectAsState()
    val saving by vm.saving.collectAsState()
    val message by vm.message.collectAsState()
    val error by vm.error.collectAsState()
    LaunchedEffect(Unit) { vm.load() }

    // Estado editable local: siempre 7 días -- si el servidor todavía no
    // tiene un horario guardado para alguno, se rellena con un default
    // razonable (09:00-18:00, inactivo) para no dejar días sin fila.
    var days by remember { mutableStateOf<List<BarberScheduleDay>>(emptyList()) }
    LaunchedEffect(loaded) {
        days = WEEK_DAYS.map { (key, _) ->
            loaded.find { it.dayOfWeek == key }?.let { it.copy(startTime = normalizeTime(it.startTime), endTime = normalizeTime(it.endTime)) }
                ?: BarberScheduleDay(key, "09:00", "18:00", isActive = false)
        }
    }

    val activeDays = days.filter { it.isActive }
    val weeklyMinutes = activeDays.sumOf { minutesBetween(it.startTime, it.endTime) ?: 0 }

    UrbanModuleScreen(
        eyebrow = "BARBERO",
        title = "Mi horario",
        subtitle = "Los días y horas en que aceptas citas.",
        onBack = onBack,
        onRefresh = { vm.load() },
        refreshing = busy
    ) {
        if (error != null && loaded.isEmpty()) {
            item { UrbanMascotState(UrbanStateKind.ERROR, "No pudimos cargar tu horario", error, "Reintentar") { vm.load() } }
            return@UrbanModuleScreen
        }
        error?.let { item { UrbanErrorBanner(it) } }
        message?.let { item { UrbanInfoBanner(it, Icons.Default.CheckCircle) } }

        item {
            // Tres números en una franja en lugar de una tarjeta alta con foto.
            UrbanStatStrip(
                listOf(
                    Triple("Días activos", activeDays.size.toString(), UrbanColors.Ink),
                    Triple("Horas a la semana", String.format("%.0f", weeklyMinutes / 60.0), UrbanColors.Ink),
                    Triple("Días libres", (7 - activeDays.size).toString(), UrbanColors.Ink)
                )
            )
        }
        if (activeDays.isEmpty()) item {
            Text("Activa al menos un día para recibir reservas.", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
        }

        item {
            // Los siete días en una sola tarjeta: interruptor, y las horas se eligen con el reloj, no se escriben.
            UrbanCard(Modifier.fillMaxWidth()) {
                days.forEachIndexed { index, day ->
                    val label = WEEK_DAYS.first { it.first == day.dayOfWeek }.second
                    if (index > 0) HorizontalDivider(color = UrbanColors.Line, modifier = Modifier.padding(vertical = 8.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.width(92.dp))
                        if (day.isActive) {
                            ScheduleTimeChip(day.startTime, !isValidTime(day.startTime)) { v ->
                                days = days.toMutableList().apply { this[index] = day.copy(startTime = v) }
                            }
                            Text("a", color = UrbanColors.Muted)
                            ScheduleTimeChip(day.endTime, !isValidTime(day.endTime) || minutesBetween(day.startTime, day.endTime) == null) { v ->
                                days = days.toMutableList().apply { this[index] = day.copy(endTime = v) }
                            }
                        } else {
                            Text("Descanso", style = MaterialTheme.typography.bodyMedium, color = UrbanColors.Muted)
                        }
                        Spacer(Modifier.weight(1f))
                        UrbanSwitch(
                            checked = day.isActive,
                            onCheckedChange = { checked -> days = days.toMutableList().apply { this[index] = day.copy(isActive = checked) } }
                        )
                    }
                }
            }
        }

        item {
            UrbanOutlineButton(
                text = "Copiar las horas del lunes a todos los días",
                onClick = {
                    val monday = days.firstOrNull()
                    if (monday != null) days = days.map { it.copy(startTime = monday.startTime, endTime = monday.endTime) }
                },
                icon = Icons.Default.ContentCopy,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            val valid = remember(days) {
                days.all { !it.isActive || (isValidTime(it.startTime) && isValidTime(it.endTime) && minutesBetween(it.startTime, it.endTime) != null) }
            }
            if (!valid) {
                Text(
                    "Usa el formato HH:mm (ej. 09:00) y una salida posterior a la entrada en los días activos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = UrbanColors.Danger,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            UrbanPrimaryButton(
                text = "Guardar horario",
                onClick = { vm.save(days) },
                enabled = valid,
                loading = saving,
                icon = Icons.Default.Save,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun isValidTime(value: String): Boolean = Regex("^([01]\\d|2[0-3]):[0-5]\\d$").matches(value)

// ── Corte de caja ────────────────────────────────────────────────────────


/** Hora de entrada o salida: un botón con la hora que abre el reloj, en lugar de escribirla a mano. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleTimeChip(value: String, invalid: Boolean, onChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val parts = value.split(":")
    OutlinedButton(
        onClick = { open = true },
        border = androidx.compose.foundation.BorderStroke(1.dp, if (invalid) UrbanColors.Danger else UrbanColors.Line),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    ) { Text(value, color = if (invalid) UrbanColors.Danger else UrbanColors.Ink) }
    if (open) {
        val state = androidx.compose.material3.rememberTimePickerState(
            initialHour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 9,
            initialMinute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0,
            is24Hour = true
        )
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { open = false },
            containerColor = UrbanColors.Card,
            text = { androidx.compose.material3.TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = { onChange(String.format("%02d:%02d", state.hour, state.minute)); open = false }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancelar") } }
        )
    }
}
