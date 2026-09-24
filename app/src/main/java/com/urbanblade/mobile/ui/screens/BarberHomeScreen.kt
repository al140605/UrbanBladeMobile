package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urbanblade.mobile.data.model.AuthUser
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.BarberAgendaViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Inicio del barbero: "Mi día". Qué cita sigue, qué falta confirmar y sus accesos. Antes veía
 * el tablero genérico de "todo el negocio", con datos que su rol no gestiona.
 */
@Composable
fun BarberHomeScreen(
    user: AuthUser,
    onNavigate: (String) -> Unit,
    vm: BarberAgendaViewModel = viewModel()
) {
    val agenda by vm.agenda.collectAsState()
    val busy by vm.busy.collectAsState()
    val error by vm.error.collectAsState()
    LaunchedEffect(Unit) { vm.load("day", null, 0) }

    val now = remember { LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) }
    val day = barberDay(agenda.data, now)
    val loadedOnce = agenda.range.label != null

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            UrbanPageHeader(
                title = "Hola, ${user.name.substringBefore(' ')}",
                subtitle = UrbanFormat.date(LocalDate.now().toString()),
                eyebrow = "BARBERO",
                trailing = { UrbanAvatar(user.name, imageUrl = user.avatarUrl) }
            )
        }

        when {
            error != null && !loadedOnce -> item {
                UrbanMascotState(UrbanStateKind.ERROR, "No pudimos cargar tu día", error, "Reintentar") { vm.load("day", null, 0) }
            }
            busy && !loadedOnce -> item { UrbanSkeletonList(1) }
            else -> {
                item {
                    UrbanHeroCard(Modifier.clickable { onNavigate("barber_agenda") }) {
                        UrbanHeroLabel(if (day.inProcess != null) "Atendiendo ahora" else "Tu siguiente cita")
                        Spacer(Modifier.height(6.dp))
                        val focus = day.inProcess ?: day.next
                        if (focus != null) {
                            Text(UrbanFormat.time(focus.horaInicio), style = MaterialTheme.typography.displaySmall, color = UrbanColors.Ink)
                            Text(
                                listOfNotNull(focus.service?.nombre, focus.client?.user?.name).joinToString(" · ").ifBlank { "Cita" },
                                style = MaterialTheme.typography.titleMedium,
                                color = UrbanColors.Ink
                            )
                        } else {
                            Text("Sin citas pendientes hoy", style = MaterialTheme.typography.headlineSmall, color = UrbanColors.Ink)
                            Text("Revisa tu semana en la agenda.", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                        }
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = UrbanColors.Ink.copy(alpha = 0.22f))
                        Spacer(Modifier.height(14.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            UrbanHeroStat("Por atender", day.remaining.toString(), Icons.Default.Event, Modifier.weight(1f))
                            UrbanHeroStat("Terminadas", day.done.toString(), Icons.Default.CheckCircle, Modifier.weight(1f), tone = UrbanColors.Success)
                        }
                    }
                }
                if (day.toConfirm > 0) {
                    item { UrbanSectionTitle("Requiere tu atención", "Tus clientes esperan respuesta.") }
                    item {
                        UrbanAttentionRow(
                            Icons.Default.HourglassTop,
                            UrbanFormat.count(day.toConfirm, "cita por confirmar", "citas por confirmar"),
                            "Confírmalas para que el cliente reciba su aviso.",
                            UrbanColors.Warning,
                            onClick = { onNavigate("barber_agenda") }
                        )
                    }
                }
            }
        }

        item { UrbanSectionTitle("Tu trabajo", "Agenda, portafolio y horario.") }
        item {
            UrbanModuleGrid(
                tiles = listOf(
                    Triple("Mi agenda", "Confirma e inicia tus citas", Icons.Default.CalendarMonth),
                    Triple("Portafolio", "Muestra tus mejores cortes", Icons.Default.PhotoLibrary),
                    Triple("Mi horario", "Días y horas que atiendes", Icons.Default.Schedule),
                    Triple("Muro social", "Trabajos de la comunidad", Icons.Default.Groups),
                ),
                routes = listOf("barber_agenda", "barber_portfolio", "barber_schedule", "social"),
                onNavigate = onNavigate
            )
        }
    }
}
