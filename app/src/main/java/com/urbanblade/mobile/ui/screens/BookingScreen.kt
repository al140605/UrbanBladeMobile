package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import coil.compose.AsyncImage
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urbanblade.mobile.data.model.BarberItem
import com.urbanblade.mobile.data.model.ServiceItem
import com.urbanblade.mobile.data.model.SlotItem
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.BookingViewModel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private enum class BookingStep(val label: String) {
    SERVICE("Servicio"), BARBER("Profesional"), CALENDAR("Calendario"), REVIEW("Revisión")
}

/**
 * Wizard visual de reserva: un paso a la vez (servicio -> profesional ->
 * calendario con horarios reales del servidor -> revisión), reemplaza el
 * diseño anterior de dropdowns + fecha de texto libre. `initialServiceId`/
 * `initialBarberId` llegan de un tap en el catálogo (autenticado, por
 * argumento de ruta) o de PendingBooking (invitado que acaba de iniciar
 * sesión) -- ver UrbanBladeRoot.kt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(
    initialServiceId: String? = null,
    initialBarberId: String? = null,
    onBack: () -> Unit,
    onCreated: () -> Unit,
    vm: BookingViewModel = viewModel()
) {
    val services by vm.services.collectAsState()
    val barbers by vm.barbers.collectAsState()
    val slots by vm.slots.collectAsState()
    val busy by vm.busy.collectAsState()
    val error by vm.error.collectAsState()
    val waitlistJoined by vm.waitlistJoined.collectAsState()

    var step by remember { mutableStateOf(BookingStep.SERVICE) }
    var serviceId by remember { mutableStateOf(initialServiceId.orEmpty()) }
    var barberId by remember { mutableStateOf(initialBarberId.orEmpty()) }
    var date by remember { mutableStateOf(vm.defaultDate()) }
    var time by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var slotsRequested by remember { mutableStateOf(false) }
    var confirmed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.loadCatalog() }
    LaunchedEffect(barberId, serviceId, date) {
        time = ""
        slotsRequested = false
        if (barberId.isNotBlank() && serviceId.isNotBlank() && date.isNotBlank()) {
            vm.loadSlots(barberId, serviceId, date)
            slotsRequested = true
        }
    }

    val selectedService = services.firstOrNull { it.id == serviceId }
    val selectedBarber = barbers.firstOrNull { it.id == barberId }

    val canAdvance = when (step) {
        BookingStep.SERVICE -> serviceId.isNotBlank()
        BookingStep.BARBER -> barberId.isNotBlank()
        BookingStep.CALENDAR -> time.isNotBlank()
        BookingStep.REVIEW -> true
    }

    // Momento de éxito antes de salir del flujo (antes se saltaba de pantalla sin confirmar nada).
    if (confirmed) {
        UrbanSuccessScreen(
            title = "¡Cita reservada!",
            message = "Te avisaremos cuando el barbero la confirme.",
            details = listOfNotNull(
                selectedService?.let { "Servicio" to it.nombre },
                selectedBarber?.user?.name?.let { "Barbero" to it },
                "Fecha" to UrbanFormat.date(date),
                "Hora" to UrbanFormat.time(time)
            ),
            primaryText = "Ver mis citas",
            onPrimary = onCreated
        )
        return
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = { UrbanTopBar(title = "Reservar cita", onBack = onBack) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            BookingProgress(step)
            Spacer(Modifier.height(4.dp))

            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
            ) {
                when (step) {
                    BookingStep.SERVICE -> ServiceStep(services, serviceId) { serviceId = it }
                    BookingStep.BARBER -> BarberStep(barbers, barberId) { barberId = it }
                    BookingStep.CALENDAR -> CalendarStep(
                        date = date,
                        onDateChange = { date = it },
                        slots = slots,
                        time = time,
                        onTimeChange = { time = it },
                        showEmptyState = slotsRequested && slots.isEmpty(),
                        waitlistJoined = waitlistJoined,
                        onJoinWaitlist = { vm.joinWaitlist(barberId, serviceId, date) }
                    )
                    BookingStep.REVIEW -> ReviewStep(
                        service = selectedService,
                        barber = selectedBarber,
                        date = date,
                        time = time,
                        notes = notes,
                        onNotesChange = { notes = it }
                    )
                }
            }

            error?.let {
                Box(Modifier.padding(horizontal = 18.dp)) { UrbanErrorBanner(it) }
                Spacer(Modifier.height(8.dp))
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (step != BookingStep.SERVICE) {
                    UrbanOutlineButton(
                        text = "Atrás",
                        onClick = { step = BookingStep.entries[step.ordinal - 1] },
                        icon = Icons.Default.ArrowBack,
                        modifier = Modifier.weight(1f)
                    )
                }
                UrbanPrimaryButton(
                    text = if (step == BookingStep.REVIEW) "Confirmar cita" else "Siguiente",
                    onClick = {
                        if (step == BookingStep.REVIEW) {
                            vm.create(barberId, serviceId, date, time, notes) { confirmed = true }
                        } else {
                            step = BookingStep.entries[step.ordinal + 1]
                        }
                    },
                    enabled = canAdvance,
                    loading = busy && step == BookingStep.REVIEW,
                    icon = if (step == BookingStep.REVIEW) Icons.Default.CheckCircle else Icons.Default.ArrowForward,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BookingProgress(step: BookingStep) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            BookingStep.entries.forEach { s ->
                val active = s.ordinal <= step.ordinal
                Box(
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(if (active) UrbanColors.Gold else UrbanColors.Line, RoundedCornerShape(2.dp))
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Paso ${step.ordinal + 1} de ${BookingStep.entries.size} · ${step.label}",
            style = MaterialTheme.typography.labelMedium,
            color = UrbanColors.Muted
        )
    }
}

@Composable
private fun ServiceStep(services: List<ServiceItem>, selectedId: String, onSelect: (String) -> Unit) {
    Column {
        UrbanSectionTitle("Elige tu servicio", "Precio y duración confirmados por UrbanBlade")
        Spacer(Modifier.height(10.dp))
        if (services.isEmpty()) {
            UrbanEmptyState("Cargando servicios…", null, Icons.Default.ContentCut)
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(services, key = { it.id }) { service ->
                    SelectableRow(
                        selected = service.id == selectedId,
                        onClick = { onSelect(service.id) },
                        title = service.nombre,
                        subtitle = "${service.duracionMin} min",
                        trailing = "\$${"%.0f".format(service.precio)}",
                        imageUrl = service.imagen
                    )
                }
            }
        }
    }
}

@Composable
private fun BarberStep(barbers: List<BarberItem>, selectedId: String, onSelect: (String) -> Unit) {
    Column {
        UrbanSectionTitle("Elige tu profesional", "Cada barbero confirma su propia disponibilidad")
        Spacer(Modifier.height(10.dp))
        if (barbers.isEmpty()) {
            UrbanEmptyState("Cargando profesionales…", null, Icons.Default.Groups)
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(barbers, key = { it.id }) { barber ->
                    SelectableRow(
                        selected = barber.id == selectedId,
                        onClick = { onSelect(barber.id) },
                        title = barber.user?.name ?: "Barbero",
                        subtitle = listOfNotNull(
                            barber.avgRating?.takeIf { barber.totalReviews > 0 }?.let { "★ %.1f (%d)".format(java.util.Locale.US, it, barber.totalReviews) },
                            barber.especialidades?.takeIf { it.isNotBlank() }
                        ).joinToString(" · ").ifBlank { "Aún sin reseñas" },
                        imageUrl = barber.foto,
                        avatarName = barber.user?.name ?: "Barbero"
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectableRow(
    selected: Boolean,
    onClick: () -> Unit,
    title: String,
    subtitle: String?,
    trailing: String? = null,
    imageUrl: String? = null,
    avatarName: String? = null
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) UrbanColors.Gold.copy(alpha = 0.13f) else UrbanColors.Card,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) UrbanColors.Gold else UrbanColors.Line),
        modifier = Modifier
            .fillMaxWidth()
            // Lectores de pantalla: "botón de opción, seleccionado".
            .semantics(mergeDescendants = true) {
                role = Role.RadioButton
                this.selected = selected
            }
    ) {
        Row(
            Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                null,
                tint = if (selected) UrbanColors.Gold else UrbanColors.Muted
            )
            Spacer(Modifier.width(12.dp))
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(if (avatarName != null) CircleShape else RoundedCornerShape(12.dp))
                        .background(UrbanColors.CardAlt)
                )
                Spacer(Modifier.width(12.dp))
            } else if (avatarName != null) {
                UrbanAvatar(avatarName, Modifier.size(52.dp))
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                subtitle?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                }
            }
            trailing?.let {
                Text(it, style = MaterialTheme.typography.titleMedium, color = UrbanColors.Gold)
            }
        }
    }
}

@Composable
private fun CalendarStep(
    date: String,
    onDateChange: (String) -> Unit,
    slots: List<SlotItem>,
    time: String,
    onTimeChange: (String) -> Unit,
    showEmptyState: Boolean,
    waitlistJoined: Boolean,
    onJoinWaitlist: () -> Unit
) {
    val days = remember { (0..29).map { LocalDate.now().plusDays(it.toLong()) } }
    val selectedDate = remember(date) { runCatching { LocalDate.parse(date) }.getOrNull() }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        UrbanSectionTitle("Elige el día", "Horarios reales, consultados al servidor")
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(days) { day ->
                val selected = day == selectedDate
                Surface(
                    onClick = { onDateChange(day.toString()) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) UrbanColors.Gold else UrbanColors.Card,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) UrbanColors.Gold else UrbanColors.Line),
                    modifier = Modifier.width(58.dp)
                ) {
                    Column(
                        Modifier.padding(vertical = 10.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("es", "MX")).replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) UrbanColors.OnGold else UrbanColors.Muted
                        )
                        Text(
                            day.dayOfMonth.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (selected) UrbanColors.OnGold else UrbanColors.Ink
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        UrbanSectionTitle("Elige la hora", null)
        Spacer(Modifier.height(10.dp))
        when {
            slots.isNotEmpty() -> {
                // Agrupados por franja (como en la web): mañana < 12:00, tarde 12:00-17:59, noche desde 18:00.
                val groups = listOf(
                    Triple("Mañana", Icons.Default.WbSunny, slots.filter { (it.time.take(2).toIntOrNull() ?: 0) < 12 }),
                    Triple("Tarde", Icons.Default.WbTwilight, slots.filter { (it.time.take(2).toIntOrNull() ?: 0) in 12..17 }),
                    Triple("Noche", Icons.Default.NightsStay, slots.filter { (it.time.take(2).toIntOrNull() ?: 0) >= 18 })
                ).filter { it.third.isNotEmpty() }
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    groups.forEach { (label, icon, group) ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(icon, null, tint = UrbanColors.Gold, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(label, style = MaterialTheme.typography.labelLarge, color = UrbanColors.Muted)
                            }
                            group.chunked(3).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    row.forEach { slot ->
                                        SlotChip(slot, selected = slot.time == time, onClick = { onTimeChange(slot.time) }, modifier = Modifier.weight(1f))
                                    }
                                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        }
                    }
                }
            }
            showEmptyState -> {
                UrbanPremiumCard(Modifier.fillMaxWidth()) {
                    UrbanEmptyState(
                        title = "Sin horarios este día",
                        subtitle = "Prueba otro día o únete a la lista de espera y te avisamos si se libera un horario.",
                        icon = Icons.Default.EventBusy,
                        actionLabel = if (!waitlistJoined) "Unirme a la lista de espera" else null,
                        onAction = if (!waitlistJoined) onJoinWaitlist else null
                    )
                    if (waitlistJoined) {
                        Spacer(Modifier.height(8.dp))
                        UrbanInfoBanner("Ya te anotamos en la lista de espera para este día.", Icons.Default.NotificationsActive)
                    }
                }
            }
            else -> LinearProgressIndicator(Modifier.fillMaxWidth(), color = UrbanColors.Gold)
        }
    }
}

@Composable
private fun SlotChip(slot: SlotItem, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) UrbanColors.Gold else UrbanColors.Card,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) UrbanColors.Gold else UrbanColors.Line),
        modifier = modifier.semantics { role = Role.RadioButton; this.selected = selected }
    ) {
        Box(Modifier.padding(vertical = 12.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                slot.label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) UrbanColors.OnGold else UrbanColors.Ink
            )
        }
    }
}

@Composable
private fun ReviewStep(
    service: ServiceItem?,
    barber: BarberItem?,
    date: String,
    time: String,
    notes: String,
    onNotesChange: (String) -> Unit
) {
    Column {
        UrbanSectionTitle("Revisa tu cita", "Confirma los detalles antes de reservar")
        Spacer(Modifier.height(10.dp))
        UrbanPremiumCard(Modifier.fillMaxWidth()) {
            ReviewRow(Icons.Default.ContentCut, service?.nombre ?: "—", service?.let { "${it.duracionMin} min · \$${"%.0f".format(it.precio)}" })
            Spacer(Modifier.height(12.dp))
            ReviewRow(Icons.Default.Person, barber?.user?.name ?: "—", null)
            Spacer(Modifier.height(12.dp))
            ReviewRow(Icons.Default.CalendarMonth, UrbanFormat.date(date), null)
            Spacer(Modifier.height(12.dp))
            ReviewRow(Icons.Default.Schedule, time.takeIf { it.isNotBlank() }?.let { UrbanFormat.time(it) } ?: "—", null)
        }
        Spacer(Modifier.height(14.dp))
        UrbanFieldLabel("Notas (opcional)")
        OutlinedTextField(
            notes,
            onNotesChange,
            placeholder = { Text("Ej. degradado bajo, barba corta…") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            shape = MaterialTheme.shapes.medium
        )
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun ReviewRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = UrbanColors.Gold, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted) }
        }
    }
}
