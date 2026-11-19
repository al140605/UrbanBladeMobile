package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urbanblade.mobile.core.appointments.canCheckIn
import com.urbanblade.mobile.data.model.AppointmentRow
import com.urbanblade.mobile.data.model.AppointmentStats
import com.urbanblade.mobile.data.model.AuthUser
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.components.UrbanFormat
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.AppointmentsViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import com.urbanblade.mobile.ui.components.UrbanFab
import com.urbanblade.mobile.ui.components.rememberFabExpanded
import androidx.compose.foundation.lazy.rememberLazyListState


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentsScreen(user: AuthUser, onBook: () -> Unit, vm: AppointmentsViewModel = viewModel()) {
    val response by vm.data.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val arrivals by vm.arrivals.collectAsState()
    val waitlistEntries by vm.waitlistEntries.collectAsState()
    var confirmCancel by remember { mutableStateOf<AppointmentRow?>(null) }
    var rescheduling by remember { mutableStateOf<AppointmentRow?>(null) }
    var checkingOut by remember { mutableStateOf<AppointmentRow?>(null) }
    var charging by remember { mutableStateOf<AppointmentRow?>(null) }
    val notice by vm.notice.collectAsState()
    val hasMore by vm.hasMore.collectAsState()
    val loadingMore by vm.loadingMore.collectAsState()
    val monthRows by vm.month.collectAsState()
    val monthLoading by vm.monthLoading.collectAsState()
    var calendarMode by remember { mutableStateOf(false) }
    var shownMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDay by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }
    val staff = user.roles.any { it in listOf("administrador", "recepcionista") }
    val canBook = com.urbanblade.mobile.core.access.UserExperience.canBook(user.roles)
    val isClient = com.urbanblade.mobile.core.access.UserExperience.isClient(user.roles)

    // El personal ve las citas de todo el negocio: las estadísticas del servidor son las del propio
    // usuario (0 para un administrador), así que se calculan aquí sobre la lista.
    var staffFilter by remember { mutableStateOf("atencion") }
    val today = remember { LocalDate.now().toString() }
    val active = listOf("pendiente", "confirmada", "en_proceso")
    val stats = if (!isClient) {
        AppointmentStats(
            total = response.data.size,
            proximas = response.data.count { 
                it.estado in active && (runCatching { LocalDate.parse(it.fecha.take(10)) }.getOrNull() ?: LocalDate.MIN) >= LocalDate.parse(today) 
            },
            completadas = response.data.count { it.estado == "completada" },
            canceladas = response.data.count { it.estado == "cancelada" }
        )
    } else {
        response.stats
    }
    
    fun getLocalDate(d: String) = runCatching { LocalDate.parse(d.take(10)) }.getOrNull() ?: LocalDate.MAX
    fun getLocalTime(t: String) = runCatching { java.time.LocalTime.parse(t.take(5).padStart(5, '0')) }.getOrNull() ?: java.time.LocalTime.MAX
    
    val visible = if (calendarMode) {
        monthRows.filter { it.fecha.startsWith(selectedDay?.toString() ?: "-") }.sortedBy { getLocalTime(it.horaInicio) }
    } else if (!staff) {
        response.data
    } else {
        when (staffFilter) {
            "hoy" -> response.data.filter { it.fecha.take(10) == today }.sortedBy { getLocalTime(it.horaInicio) }
            "atencion", "proximas" -> response.data.filter { 
                    it.estado in active && (runCatching { LocalDate.parse(it.fecha.take(10)) }.getOrNull() ?: LocalDate.MIN) >= LocalDate.parse(today) 
                }
                .sortedWith(compareBy<AppointmentRow> { getLocalDate(it.fecha) }.thenBy { getLocalTime(it.horaInicio) })
            else -> response.data.sortedWith(compareBy<AppointmentRow> { getLocalDate(it.fecha) }.thenBy { getLocalTime(it.horaInicio) })
        }
    }
    val pendingCount = if (staff && !calendarMode) visible.count { it.estado == "pendiente" } else 0
    // Solo la siguiente decisión ocupa la tarjeta destacada. Las demás citas,
    // incluso si siguen pendientes, permanecen en la lista compacta.
    val attention = if (staff && !calendarMode) visible.filter { it.estado == "pendiente" }.take(1) else emptyList()
    val attentionIds = attention.map { it.id }.toSet()
    val later = if (staff && !calendarMode) visible.filterNot { it.id in attentionIds } else emptyList()

    LaunchedEffect(calendarMode, shownMonth) {
        if (calendarMode) vm.loadMonth(shownMonth)
    }

    LaunchedEffect(Unit) {
        vm.load()
        if (isClient) vm.loadWaitlist()
    }

    val listState = rememberLazyListState()
    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            if (canBook) UrbanFab("Nueva cita", Icons.Default.Add, rememberFabExpanded(listState), onBook)
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            state = listState,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = com.urbanblade.mobile.ui.components.BladebotClearance),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                UrbanPageHeader(
                    title = "Citas",
                    subtitle = "Tu agenda, ordenada y siempre conectada.",
                    eyebrow = "Agenda"
                )
            }

            // Si la carga falló y no hay nada que mostrar, no se pintan contadores en 0 ni
            // "agenda libre": sería afirmar algo que no sabemos. Solo el error y "Reintentar".
            val failed = error != null && response.data.isEmpty()

            if (!failed && staff) item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text(
                        text = "${stats.proximas} próximas · ${stats.completadas} completadas",
                        style = MaterialTheme.typography.labelLarge,
                        color = UrbanColors.Muted
                    )
                }
            }

            if (loading) item { UrbanSkeletonList(3) }
            notice?.let {
                item { UrbanInfoBanner(it, Icons.Default.CheckCircle) }
            }
            error?.let { item { UrbanErrorBanner(it) } }
            if (failed && !loading) {
                item {
                    UrbanOutlineButton(
                        text = "Reintentar",
                        onClick = { vm.load() },
                        icon = Icons.Default.Refresh,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (!loading && !failed && response.data.isEmpty()) {
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
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !calendarMode, onClick = { calendarMode = false }, label = { Text("Lista") })
                        FilterChip(
                            selected = calendarMode,
                            onClick = { calendarMode = true },
                            label = { Text("Calendario") },
                            leadingIcon = { Icon(Icons.Default.CalendarMonth, null, Modifier.size(18.dp)) }
                        )
                    }
                }
                if (calendarMode) {
                    item {
                        AppointmentsCalendar(
                            month = shownMonth,
                            rows = monthRows,
                            selected = selectedDay,
                            loading = monthLoading,
                            onMonthChange = { shownMonth = it; selectedDay = null },
                            onSelect = { selectedDay = it }
                        )
                    }
                    item {
                        UrbanSectionTitle(
                            selectedDay?.let { UrbanFormat.date(it.toString()) } ?: "Elige un día",
                            if (selectedDay == null) "Toca un día del calendario" else UrbanFormat.count(visible.size, "cita", "citas")
                        )
                    }
                    if (selectedDay != null && visible.isEmpty() && !monthLoading) {
                        item { UrbanEmptyState("Sin citas este día", "Elige otro día del calendario.", Icons.Default.EventAvailable) }
                    }
                } else if (staff) {
                    item {
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("atencion" to "Requieren atención", "hoy" to "Hoy", "proximas" to "Próximas", "todas" to "Todas")
                                .forEach { (key, label) ->
                                    FilterChip(selected = staffFilter == key, onClick = { staffFilter = key }, label = { Text(label) })
                                }
                        }
                    }
                    if (visible.isEmpty()) {
                        item { UrbanEmptyState("Sin citas en este filtro", "Prueba con otro filtro.", Icons.Default.EventAvailable) }
                    }
                } else {
                    item { UrbanSectionTitle("Tu agenda", UrbanFormat.count(response.data.size, "cita cargada", "citas cargadas")) }
                }
            }

            if (staff && !calendarMode && visible.isNotEmpty()) {
                item {
                    PrioritySectionHeader(
                        title = "Requieren atención",
                        subtitle = "Citas que necesitan una decisión tuya.",
                        count = pendingCount,
                        icon = Icons.Default.PriorityHigh
                    )
                }
                items(attention, key = { "priority-${it.id}" }) { appt ->
                    StaffPriorityAppointment(
                        appt = appt,
                        onConfirm = { vm.changeStatus(appt, "confirmada") },
                        onCancel = if (appt.code != null) { { confirmCancel = appt } } else null,
                        onCharge = if (appt.isChargeable && !appt.hasPayment) { { charging = appt } } else null
                    )
                }
                if (attention.isEmpty()) {
                    item { UrbanInfoBanner("No hay citas pendientes de confirmación.", Icons.Default.TaskAlt) }
                }
                if (later.isNotEmpty()) {
                    item {
                        PrioritySectionHeader(
                            title = "Después",
                            subtitle = "Próximas citas en tu agenda.",
                            count = later.size,
                            icon = Icons.Default.Schedule
                        )
                    }
                    items(later, key = { "later-${it.id}" }) { appt ->
                        StaffTimelineAppointment(
                            appt = appt,
                            onStatus = if (appt.code != null && appt.estado in listOf("confirmada", "en_proceso")) {
                                { estado -> vm.changeStatus(appt, estado) }
                            } else null,
                            onCancel = if (appt.code != null && appt.estado in listOf("pendiente", "confirmada")) {
                                { { confirmCancel = appt } }
                            } else null,
                            onCharge = if (appt.isChargeable && !appt.hasPayment) { { charging = appt } } else null,
                            arrivedAt = appt.code?.let { arrivals[it] },
                            onArrived = if (appt.code != null && canCheckIn(appt.estado, appt.fecha)) { { vm.checkIn(appt) } } else null
                        )
                    }
                }
            }

            if (!staff || calendarMode) items(visible, key = { it.id }) { appt ->
                val manageable = appt.estado in listOf("pendiente", "confirmada") && appt.code != null
                val payable = isClient && appt.isPayable()
                AppointmentCard(
                    appt = appt,
                    clientName = if (!isClient) appt.client?.user?.name else null,
                    onCancel = if (manageable) { { confirmCancel = appt } } else null,
                    onReschedule = if (manageable && isClient) { { rescheduling = appt } } else null,
                    onPay = if (payable) { { checkingOut = appt } } else null,
                    onStatus = if (staff && appt.code != null && appt.estado in listOf("pendiente", "confirmada", "en_proceso")) {
                        { estado -> vm.changeStatus(appt, estado) }
                    } else null,
                    onCharge = if (staff && appt.isChargeable && !appt.hasPayment) { { charging = appt } } else null
                )
            }

            if (!calendarMode && hasMore) {
                item {
                    UrbanOutlineButton(
                        text = if (loadingMore) "Cargando…" else "Cargar más citas",
                        onClick = { vm.loadMore() },
                        enabled = !loading && !loadingMore,
                        icon = Icons.Default.ExpandMore,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
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

    charging?.let { appt ->
        ChargeSheet(appt = appt, vm = vm, onDismiss = { charging = null })
    }

    checkingOut?.let { appt ->
        CheckoutSheet(
            appt = appt,
            vm = vm,
            onDismiss = { checkingOut = null }
        )
    }

    confirmCancel?.let { appt ->
        AlertDialog(
            onDismissRequest = { confirmCancel = null },
            containerColor = UrbanColors.Card,
            title = { Text("Cancelar cita") },
            text = {
                Text(
                    if (staff) {
                        "¿Cancelar la cita de ${appt.client?.user?.name ?: "este cliente"} del ${UrbanFormat.date(appt.fecha)} a las " +
                            "${UrbanFormat.time(appt.horaInicio)}? Se le avisará y, si pagó al reservar, se le devolverá."
                    } else {
                        "¿Quieres cancelar tu cita del ${UrbanFormat.date(appt.fecha)} a las ${UrbanFormat.time(appt.horaInicio)}?"
                    },
                    color = UrbanColors.Muted
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.cancel(appt, asStaff = staff); confirmCancel = null }) {
                    Text("Sí, cancelar", color = UrbanColors.Danger)
                }
            },
            dismissButton = { TextButton(onClick = { confirmCancel = null }) { Text("Volver") } }
        )
    }
}
