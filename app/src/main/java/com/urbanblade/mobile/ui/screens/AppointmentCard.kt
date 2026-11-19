package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.urbanblade.mobile.data.model.AppointmentRow
import com.urbanblade.mobile.data.model.WaitlistEntry
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.components.UrbanFormat
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.components.UrbanTextButton


@Composable
internal fun AppointmentCard(
    appt: AppointmentRow,
    clientName: String? = null,
    onCancel: (() -> Unit)?,
    onReschedule: (() -> Unit)? = null,
    onPay: (() -> Unit)? = null,
    onStatus: ((String) -> Unit)? = null,
    onCharge: (() -> Unit)? = null
) {
    UrbanPremiumCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            Surface(
                modifier = Modifier.size(width = 66.dp, height = 72.dp),
                color = UrbanColors.Gold.copy(alpha = 0.09f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, UrbanColors.Gold.copy(alpha = 0.25f))
            ) {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(appt.fecha.take(10).takeLast(2), style = MaterialTheme.typography.headlineMedium, color = UrbanColors.GoldText)
                    Text(appt.horaInicio.take(5), style = MaterialTheme.typography.labelMedium, color = UrbanColors.Ink)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                com.urbanblade.mobile.ui.components.UrbanAppointmentHeading(appt.service?.nombre ?: "Servicio", appt.estado) {
                    StaffAppointmentMenu(onCancel = onCancel, onCharge = null)
                }
                clientName?.let { PersonLine("Cliente: $it", it, appt.client?.user?.avatarUrl, UrbanColors.Ink) }
                PersonLine(
                    label = appt.barber?.user?.name ?: "Barbero por confirmar",
                    name = appt.barber?.user?.name ?: "Barbero",
                    imageUrl = appt.barber?.fotoUrl,
                    color = UrbanColors.Muted
                )
                Text(UrbanFormat.date(appt.fecha), style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                appt.notas?.takeIf { it.isNotBlank() }?.let {
                    Text("“$it”", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Ink)
                }
                if (onStatus != null || onCharge != null) {
                    Spacer(Modifier.height(6.dp))
                    // Siguiente paso del flujo (pendiente, confirmada, en proceso, completada) y cobro.
                    val next = when (appt.estado) {
                        "pendiente" -> "confirmada" to "Confirmar"
                        "confirmada" -> "en_proceso" to "Iniciar"
                        "en_proceso" -> "completada" to "Completar"
                        else -> null
                    }
                    // Iniciar exige que sea hoy y que el pago esté resuelto (cobrada o transferencia verificada).
                    val startBlocked = appt.estado == "confirmada" && appt.puedeIniciar == false
                    if (startBlocked && !appt.motivoNoIniciar.isNullOrBlank()) {
                        Text(appt.motivoNoIniciar, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Warning)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (next != null && onStatus != null) {
                            UrbanPrimaryButton(
                                text = next.second,
                                onClick = { onStatus(next.first) },
                                enabled = !startBlocked,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (onCharge != null) {
                            UrbanOutlineButton(
                                text = "Cobrar",
                                onClick = onCharge,
                                icon = Icons.Default.Payments,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                if (onPay != null) {
                    Spacer(Modifier.height(6.dp))
                    UrbanPrimaryButton(
                        text = "Pagar cita",
                        onClick = onPay,
                        icon = Icons.Default.Payments,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                val canNoShow = appt.estado == "confirmada" && onStatus != null
                if (onReschedule != null || canNoShow) {
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (canNoShow) {
                            UrbanTextButton(onClick = { onStatus?.invoke("no_asistio") }, contentPadding = PaddingValues(0.dp)) {
                                Text("No asistió", color = UrbanColors.Muted)
                            }
                            Spacer(Modifier.width(16.dp))
                        }
                        if (onReschedule != null) {
                            UrbanTextButton(onClick = onReschedule, contentPadding = PaddingValues(0.dp)) {
                                Icon(Icons.Default.EditCalendar, null, modifier = Modifier.size(17.dp), tint = UrbanColors.Gold)
                                Spacer(Modifier.width(5.dp))
                                Text("Reagendar", color = UrbanColors.GoldText)
                            }
                            Spacer(Modifier.width(16.dp))
                        }

                    }
                }
            }
        }
    }
}

@Composable
internal fun WaitlistCard(entry: WaitlistEntry, onLeave: () -> Unit) {
    // Salir de la lista hace perder el lugar: se confirma como las demás acciones que no se deshacen.
    var confirmLeave by remember { mutableStateOf(false) }
    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            containerColor = UrbanColors.Card,
            title = { Text("¿Salir de la lista de espera?") },
            text = { Text("Ya no te avisaremos si se libera un horario para ${entry.service?.nombre ?: "este servicio"}.", color = UrbanColors.Muted) },
            confirmButton = { TextButton(onClick = { confirmLeave = false; onLeave() }) { Text("Sí, salir", color = UrbanColors.Danger) } },
            dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text("Volver") } }
        )
    }
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
            IconButton(onClick = { confirmLeave = true }) { Icon(Icons.Default.Close, "Salir de la lista de espera", tint = UrbanColors.Danger) }
        }
    }
}
