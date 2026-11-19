package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import com.urbanblade.mobile.data.model.AppointmentRow
import com.urbanblade.mobile.data.model.SlotItem
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.AppointmentsViewModel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import com.urbanblade.mobile.ui.theme.UrbanSpace


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RescheduleSheet(appt: AppointmentRow, vm: AppointmentsViewModel, onDismiss: () -> Unit) {
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
        Column(Modifier.fillMaxWidth().padding(horizontal = UrbanSpace.Gutter, vertical = 8.dp)) {
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
                            color = if (selected) UrbanColors.OnGold else UrbanColors.Ink
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
                items(days, key = { it.toString() }) { day ->
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
                                color = if (selected) UrbanColors.OnGold else UrbanColors.Muted
                            )
                            Text(day.dayOfMonth.toString(), style = MaterialTheme.typography.titleMedium, color = if (selected) UrbanColors.OnGold else UrbanColors.Ink)
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
                                Text(slot.label, style = MaterialTheme.typography.labelLarge, color = if (selected) UrbanColors.OnGold else UrbanColors.Ink)
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
