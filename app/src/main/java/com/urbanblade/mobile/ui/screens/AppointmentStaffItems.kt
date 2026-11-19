package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.urbanblade.mobile.core.appointments.arrivalTime
import com.urbanblade.mobile.data.model.AppointmentRow
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.components.UrbanFormat
import com.urbanblade.mobile.ui.theme.UrbanColors
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime


@Composable
internal fun PrioritySectionHeader(title: String, subtitle: String, count: Int, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier.size(34.dp),
            shape = CircleShape,
            color = UrbanColors.Gold.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, UrbanColors.Gold.copy(alpha = 0.5f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = UrbanColors.Gold, modifier = Modifier.size(19.dp))
            }
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(count.toString(), style = MaterialTheme.typography.labelLarge, color = UrbanColors.Muted)
            }
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
        }
    }
}

@Composable
internal fun StaffPriorityAppointment(
    appt: AppointmentRow,
    onConfirm: () -> Unit,
    onCancel: (() -> Unit)?,
    onCharge: (() -> Unit)?
) {
    var stamped by remember { mutableStateOf(false) }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    LaunchedEffect(stamped) {
        if (stamped) {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            kotlinx.coroutines.delay(650)
            onConfirm()
        }
    }

    Box(contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.fillMaxWidth().alpha(if (stamped) 0.5f else 1f),
            shape = RoundedCornerShape(20.dp),
            color = UrbanColors.Card.copy(alpha = 0.86f),
            border = BorderStroke(1.dp, UrbanColors.Gold.copy(alpha = 0.72f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                AppointmentDateRail(appt, emphasized = true)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            appt.service?.nombre ?: "Servicio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        UrbanStatusPill(appt.estado)
                        StaffAppointmentMenu(onCancel = onCancel, onCharge = onCharge)
                    }
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            PersonLine(
                                label = appt.client?.user?.name ?: "Cliente por confirmar",
                                name = appt.client?.user?.name ?: "Cliente",
                                imageUrl = appt.client?.user?.avatarUrl,
                                color = UrbanColors.Ink
                            )
                            PersonLine(
                                label = appt.barber?.user?.name ?: "Barbero por confirmar",
                                name = appt.barber?.user?.name ?: "Barbero",
                                imageUrl = appt.barber?.fotoUrl,
                                color = UrbanColors.Muted
                            )
                            ReminderLine(appt)
                        }
                        Button(
                            onClick = { stamped = true },
                            modifier = Modifier.heightIn(min = 48.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = UrbanColors.Gold,
                                contentColor = UrbanColors.OnGold
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Confirmar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        if (stamped) {
            com.urbanblade.mobile.ui.components.UrbanStampAnimation("CONFIRMADA")
        }
    }
}

@Composable
internal fun StaffTimelineAppointment(
    appt: AppointmentRow,
    onStatus: ((String) -> Unit)?,
    onCancel: (() -> Unit)?,
    onCharge: (() -> Unit)?,
    /** Hora ISO de llegada si ya se registró en esta sesión; [onArrived] es null cuando la cita no admite el check-in. */
    arrivedAt: String? = null,
    onArrived: (() -> Unit)? = null
) {
    var stampedAction by remember { mutableStateOf<String?>(null) }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    LaunchedEffect(stampedAction) {
        if (stampedAction != null) {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            kotlinx.coroutines.delay(650)
            onStatus?.invoke(stampedAction!!)
            stampedAction = null
        }
    }

    Box(contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.alpha(if (stampedAction != null) 0.5f else 1f)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                AppointmentDateRail(appt, emphasized = false)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            appt.service?.nombre ?: "Servicio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        UrbanStatusPill(appt.estado)
                        StaffAppointmentMenu(
                            onStatus = if (onStatus != null) { { estado -> stampedAction = estado } } else null,
                            currentStatus = appt.estado,
                            startBlocked = appt.estado == "confirmada" && appt.puedeIniciar == false,
                            startBlockReason = appt.motivoNoIniciar,
                            onCancel = onCancel,
                            onCharge = onCharge,
                            onArrived = onArrived
                        )
                    }
                    Text(appt.client?.user?.name ?: "Cliente por confirmar", style = MaterialTheme.typography.bodyMedium)
                    Text(appt.barber?.user?.name ?: "Barbero por confirmar", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                    arrivalTime(arrivedAt)?.let {
                        Text("Llegó a las $it", style = MaterialTheme.typography.labelLarge, color = UrbanColors.Success)
                    }
                    ReminderLine(appt)
                }
            }
            HorizontalDivider(color = UrbanColors.Line.copy(alpha = 0.65f))
        }

        when (stampedAction) {
            "completada" -> com.urbanblade.mobile.ui.components.UrbanStampAnimation("COMPLETADA", color = UrbanColors.Success)
            "no_asistio" -> com.urbanblade.mobile.ui.components.UrbanStampAnimation("NO ASISTIÓ", color = UrbanColors.Danger)
            "en_proceso" -> com.urbanblade.mobile.ui.components.UrbanStampAnimation("INICIADA", color = UrbanColors.Gold)
        }
    }
}

@Composable
internal fun AppointmentDateRail(appt: AppointmentRow, emphasized: Boolean) {
    Column(
        modifier = Modifier.width(54.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        if (emphasized) {
            Text(appt.fecha.take(10).takeLast(2), style = MaterialTheme.typography.headlineMedium, color = UrbanColors.GoldText, fontWeight = FontWeight.Bold)
            Text(UrbanFormat.dateShort(appt.fecha).substringAfter(" ").uppercase(), style = MaterialTheme.typography.labelSmall, color = UrbanColors.Muted)
            HorizontalDivider(Modifier.padding(vertical = 3.dp), color = UrbanColors.Line)
        }
        Text(
            appt.horaInicio.take(5),
            style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (emphasized) UrbanColors.Ink else UrbanColors.Gold
        )
        if (!emphasized) {
            Text(
                UrbanFormat.dateShort(appt.fecha).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = UrbanColors.Muted
            )
        }
    }
}

@Composable
private fun ReminderLine(appt: AppointmentRow) {
    val (label, color, icon) = remember(appt.reminder24hSent, appt.reminder2hSent, appt.fecha, appt.horaInicio) {
        when {
            appt.reminder2hSent -> Triple("Recordatorio 2 h enviado", UrbanColors.Muted, Icons.Default.NotificationsActive)
            appt.reminder24hSent -> Triple("Recordatorio 24 h enviado", UrbanColors.Muted, Icons.Default.NotificationsActive)
            appointmentIsFuture(appt) -> Triple("Recordatorio por enviar", UrbanColors.Muted, Icons.Default.Schedule)
            else -> Triple("Sin recordatorio enviado", UrbanColors.Danger, Icons.Default.NotificationsOff)
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = color)
    }
}

private fun appointmentIsFuture(appt: AppointmentRow): Boolean = runCatching {
    val date = LocalDate.parse(appt.fecha.take(10))
    val time = LocalTime.parse(appt.horaInicio.take(5))
    LocalDateTime.of(date, time).isAfter(LocalDateTime.now())
}.getOrDefault(false)

@Composable
internal fun StaffAppointmentMenu(
    onStatus: ((String) -> Unit)? = null,
    currentStatus: String? = null,
    startBlocked: Boolean = false,
    startBlockReason: String? = null,
    onCancel: (() -> Unit)?,
    onCharge: (() -> Unit)?,
    onArrived: (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val next = when (currentStatus) {
        "confirmada" -> "en_proceso" to "Iniciar servicio"
        "en_proceso" -> "completada" to "Completar servicio"
        else -> null
    }
    if (next != null || onCancel != null || onCharge != null || onArrived != null) {
        Box {
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Más acciones")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                if (next != null && onStatus != null) {
                    DropdownMenuItem(
                        text = { Text(if (startBlocked) "${next.second} (no disponible)" else next.second) },
                        enabled = !startBlocked,
                        leadingIcon = { Icon(Icons.Default.PlayArrow, null) },
                        onClick = { expanded = false; onStatus(next.first) }
                    )
                }
                if (startBlocked && !startBlockReason.isNullOrBlank()) {
                    DropdownMenuItem(
                        text = { Text(startBlockReason) },
                        enabled = false,
                        onClick = {}
                    )
                }
                if (onArrived != null) {
                    DropdownMenuItem(
                        text = { Text("Llegó") },
                        leadingIcon = { Icon(Icons.Default.HowToReg, null) },
                        onClick = { expanded = false; onArrived() }
                    )
                }
                if (onCharge != null) {
                    DropdownMenuItem(
                        text = { Text("Cobrar") },
                        leadingIcon = { Icon(Icons.Default.Payments, null) },
                        onClick = { expanded = false; onCharge() }
                    )
                }
                if (onCancel != null) {
                    DropdownMenuItem(
                        text = { Text("Cancelar cita", color = UrbanColors.Danger) },
                        leadingIcon = { Icon(Icons.Default.Close, null, tint = UrbanColors.Danger) },
                        onClick = { expanded = false; onCancel() }
                    )
                }
            }
        }
    }
}


/** Una persona de la cita (cliente o barbero) con su foto, o iniciales si no tiene. */
@Composable
internal fun PersonLine(label: String, name: String, imageUrl: String?, color: Color) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        UrbanAvatar(name, Modifier.size(30.dp), imageUrl = imageUrl)
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            modifier = Modifier.weight(1f)
        )
    }
}
