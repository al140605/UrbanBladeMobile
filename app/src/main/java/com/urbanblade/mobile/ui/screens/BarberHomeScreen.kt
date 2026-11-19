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
import androidx.compose.material.icons.filled.HowToReg
import com.urbanblade.mobile.core.appointments.canCheckIn
import com.urbanblade.mobile.ui.components.UrbanActionRow
import com.urbanblade.mobile.ui.components.UrbanHeroCard
import com.urbanblade.mobile.ui.components.UrbanHeroLabel
import com.urbanblade.mobile.ui.components.UrbanOutlineButton
import com.urbanblade.mobile.ui.components.UrbanPrimaryButton
import com.urbanblade.mobile.ui.components.UrbanRowGroup
import com.urbanblade.mobile.ui.components.UrbanStatStrip
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urbanblade.mobile.data.model.AuthUser
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.BarberAgendaViewModel
import com.urbanblade.mobile.ui.viewmodel.DashboardViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import com.urbanblade.mobile.ui.theme.UrbanSpace

/**
 * Inicio del barbero: "Mi día". Qué cita sigue, qué falta confirmar y sus accesos. Antes veía
 * el tablero genérico de "todo el negocio", con datos que su rol no gestiona.
 */
@Composable
fun BarberHomeScreen(
    user: AuthUser,
    onNavigate: (String) -> Unit,
    vm: BarberAgendaViewModel = viewModel(),
    dashboardVm: DashboardViewModel = viewModel()
) {
    val agenda by vm.agenda.collectAsState()
    val busy by vm.busy.collectAsState()
    val error by vm.error.collectAsState()
    LaunchedEffect(Unit) { vm.load("day", null, 0) }
    // Gráficas de su semana: GET /dashboard (rol barbero). Si falla, simplemente no aparecen.
    val dashboard by dashboardVm.data.collectAsState()
    LaunchedEffect(Unit) { dashboardVm.load() }

    val now = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
    val day = barberDay(agenda.data, now)
    val loadedOnce by vm.hasLoaded.collectAsState()
    // Lo que sigue hoy: se calcula al cambiar la agenda, no en cada recomposición.
    val upcoming = remember(agenda.data, day) {
        agenda.data
            .filter { (it.estado == "pendiente" || it.estado == "confirmada") && it.id != (day.inProcess ?: day.next)?.id }
            .sortedBy { it.horaInicio }
            .take(3)
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = UrbanSpace.Gutter, end = UrbanSpace.Gutter, top = UrbanSpace.Md, bottom = com.urbanblade.mobile.ui.components.BladebotClearance),
        verticalArrangement = Arrangement.spacedBy(UrbanSpace.Md)
    ) {
        item {
            HomeHeader("BARBERO", user, busy, onNavigate) { vm.load("day", null, 0); dashboardVm.load() }
        }

        if (error != null && loadedOnce) item { UrbanStaleContentNotice(error.orEmpty()) { vm.load("day", null, 0) } }

        when {
            error != null && !loadedOnce -> item {
                UrbanMascotState(UrbanStateKind.ERROR, "No pudimos cargar tu día", error, "Reintentar") { vm.load("day", null, 0) }
            }
            !loadedOnce -> item { UrbanSkeletonList(1) }
            else -> {
                // Tres números del día en una franja, antes de la cita de ahora.
                item {
                    UrbanStatStrip(
                        listOf(
                            Triple("Citas hoy", agenda.stats.totalPeriod.toString(), UrbanColors.Ink),
                            Triple("Terminadas", day.done.toString(), UrbanColors.Success),
                            Triple("Tus ingresos", UrbanFormat.moneyShort(agenda.stats.incomeTotal), UrbanColors.Ink)
                        )
                    )
                }
                item {
                    val focus = day.inProcess ?: day.next
                    UrbanHeroCard {
                        UrbanHeroLabel(if (day.inProcess != null) "Atendiendo ahora" else "Tu siguiente cita")
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (focus != null) UrbanFormat.time(focus.horaInicio) else "Día libre por ahora",
                            style = MaterialTheme.typography.headlineMedium,
                            color = UrbanColors.Ink
                        )
                        if (focus == null) {
                            Text("Cuando entre una cita nueva, aparece aquí.", style = MaterialTheme.typography.bodyMedium, color = UrbanColors.Muted)
                        }
                        if (focus != null) {
                            Text(
                                listOfNotNull(focus.service?.nombre, focus.client?.user?.name).joinToString(" · ").ifBlank { "Cita" },
                                style = MaterialTheme.typography.titleMedium,
                                color = UrbanColors.Ink
                            )
                            // La acción de ahora a un toque, sin abrir la agenda: Llegó y el siguiente paso de la cita.
                            val code = focus.code
                            val nextState = nextStatesFor(focus.estado).firstOrNull()
                            val canArrive = code != null && canCheckIn(focus.estado, focus.fecha)
                            val blocked = focus.estado == "confirmada" && focus.puedeIniciar == false
                            if (code != null && (canArrive || (nextState != null && !isDestructiveStatus(nextState)))) {
                                Spacer(Modifier.height(12.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (canArrive) {
                                        UrbanOutlineButton("Llegó", { vm.checkIn(code) }, Modifier.weight(1f), Icons.Default.HowToReg)
                                    }
                                    if (nextState != null && !isDestructiveStatus(nextState)) {
                                        UrbanPrimaryButton(
                                            text = if (focus.estado == "pendiente") "Aprobar" else statusActionLabel(nextState),
                                            onClick = { vm.updateStatus(code, nextState, "day", null, 0) },
                                            enabled = !blocked,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                            if (focus.estado == "confirmada" && focus.puedeIniciar == false && !focus.motivoNoIniciar.isNullOrBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(focus.motivoNoIniciar, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Warning)
                            }
                        }
                    }
                }
                // Lo que sigue hoy, sin entrar a la agenda.
                if (upcoming.isNotEmpty()) {
                    item { UrbanSectionTitle("Tu día", null, "Ver agenda") { onNavigate("appointments") } }
                    item {
                        UrbanRowGroup(upcoming) { appt ->
                            UrbanActionRow(
                                title = UrbanFormat.time(appt.horaInicio) + " · " + (appt.client?.user?.name ?: "Cliente"),
                                subtitle = (appt.service?.nombre ?: "Cita") + " · " + statusLabel(appt.estado),
                                icon = Icons.Default.Schedule
                            ) { onNavigate("appointments") }
                        }
                    }
                }
                if (day.toConfirm > 0) {
                    item { UrbanSectionTitle("Requiere tu atención") }
                    item {
                        UrbanAttentionRow(
                            Icons.Default.HourglassTop,
                            UrbanFormat.count(day.toConfirm, "cita por confirmar", "citas por confirmar"),
                            "Confírmalas para que el cliente reciba su aviso.",
                            UrbanColors.Warning,
                            onClick = { onNavigate("appointments") }
                        )
                    }
                }
            }
        }

        // La agenda y Mi perfil (perfil, horario y portafolio) ya están en la barra inferior; aquí, el resto.
        item { UrbanSectionTitle("Tu trabajo") }
        item {
            UrbanModuleGrid(
                tiles = listOf(
                    Triple("Muro de Inspiración", "Trabajos del equipo", Icons.Default.Groups),
                    Triple("Catálogo", "Servicios y barberos", Icons.Default.ContentCut),
                ),
                routes = listOf("social", "catalog"),
                onNavigate = onNavigate
            )
        }

        dashboard?.data.chartSeries("performanceChart")?.let { week ->
            item { UrbanDashboardChart("Tu semana", "Citas de los últimos 7 días", week) }
        }
        dashboard?.data.chartSeries("servicesChart")?.let { services ->
            item { UrbanDashboardChart("Tus servicios más pedidos", "Último año", services, categories = true) }
        }

    }
}
