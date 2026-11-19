package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import coil.compose.AsyncImage
import androidx.compose.ui.unit.dp
import com.urbanblade.mobile.data.model.ServiceItem
import com.urbanblade.mobile.data.model.SlotItem
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.components.UrbanFormat
import com.urbanblade.mobile.ui.theme.UrbanColors
import androidx.compose.foundation.horizontalScroll
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.input.ImeAction
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import com.urbanblade.mobile.ui.theme.UrbanSpace


/** Encabezado del paso con el patrón de la app: eyebrow con el paso, título serif, avance y resumen. */
@Composable
internal fun BookingHeader(step: BookingStep, summary: String?) {
    Column(Modifier.fillMaxWidth().padding(horizontal = UrbanSpace.Gutter)) {
        UrbanPageHeader(
            title = step.title,
            subtitle = step.hint,
            eyebrow = "Reserva",
            compact = true
        )
        Spacer(Modifier.height(12.dp))
        UrbanStepProgress(BookingStep.entries.map { it.label }, step.ordinal)
        summary?.let {
            Spacer(Modifier.height(10.dp))
            Surface(shape = MaterialTheme.shapes.medium, color = UrbanColors.Card,
                border = androidx.compose.foundation.BorderStroke(1.dp, UrbanColors.Line), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = UrbanColors.Gold, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Ink)
                }
            }
        }
    }
}

@Composable
internal fun ServiceStep(services: List<ServiceItem>, selectedId: String, onSelect: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf<ServiceFilter?>(null) }
    val filters = remember(services) { ServiceFilter.entries.filter { f -> services.any { serviceKind(it.nombre) in f.kinds } } }
    val visible = remember(services, query, filter) { filterServices(services, query, filter) }
    Column {
        if (services.isEmpty()) {
            UrbanSkeletonList(4)
        } else {
            UrbanTextField(
                value = query,
                onValueChange = { query = it },
                label = "Buscar servicio",
                placeholder = "Fade, barba, keratina…",
                leadingIcon = Icons.Default.Search,
                imeAction = ImeAction.Done
            )
            Spacer(Modifier.height(10.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text("Todos") }, colors = bookingChipColors())
                filters.forEach { f ->
                    FilterChip(selected = filter == f, onClick = { filter = if (filter == f) null else f }, label = { Text(f.label) }, colors = bookingChipColors())
                }
            }
            Spacer(Modifier.height(10.dp))
            if (visible.isEmpty()) {
                UrbanMascotState(
                    UrbanStateKind.EMPTY,
                    "No encontramos ese servicio",
                    "Prueba con otra palabra o quita el filtro.",
                    actionLabel = "Limpiar búsqueda",
                    actionIcon = Icons.Default.Close,
                    onAction = { query = ""; filter = null }
                )
            }
            Column(Modifier.fillMaxWidth()) {
                visible.forEach { service -> key(service.id) {
                    CompactServiceRow(
                        selected = service.id == selectedId,
                        onClick = { onSelect(service.id) },
                        title = service.nombre,
                        subtitle = "${service.duracionMin} min",
                        trailing = "${UrbanFormat.moneyShort(service.precio)}",
                        imageUrl = service.imagen,
                        fallbackIcon = serviceIcon(service.nombre)
                    )
                }
                }
            }
        }
    }
}

@Composable
private fun CompactServiceRow(
    selected: Boolean,
    onClick: () -> Unit,
    title: String,
    subtitle: String?,
    trailing: String? = null,
    imageUrl: String? = null,
    avatarName: String? = null,
    /** Sin foto del servicio, un ícono según su tipo (el mismo que en Explorar). */
    fallbackIcon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(if (selected) UrbanColors.Gold.copy(alpha = 0.08f) else Color.Transparent)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                role = Role.RadioButton
                this.selected = selected
            }
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(if (avatarName != null) CircleShape else RoundedCornerShape(8.dp))
                        .background(UrbanColors.CardAlt)
                )
                Spacer(Modifier.width(12.dp))
            } else if (avatarName != null) {
                UrbanAvatar(avatarName, Modifier.size(44.dp))
                Spacer(Modifier.width(12.dp))
            } else if (fallbackIcon != null) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(UrbanColors.Gold.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(fallbackIcon, null, tint = UrbanColors.Gold, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
            }
            
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selected) UrbanColors.Gold else UrbanColors.Ink,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                )
                subtitle?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                }
            }
            
            trailing?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selected) UrbanColors.Gold else UrbanColors.Ink
                )
            }
            
            Spacer(Modifier.width(12.dp))
            
            if (selected) {
                Icon(Icons.Default.CheckCircle, null, tint = UrbanColors.Gold, modifier = Modifier.size(22.dp))
            } else {
                Box(Modifier.size(22.dp))
            }
        }
        HorizontalDivider(color = UrbanColors.Line.copy(alpha = 0.4f), modifier = Modifier.padding(horizontal = 12.dp))
    }
}

@Composable
internal fun CalendarStep(
    header: @Composable () -> Unit = {},
    date: String,
    onDateChange: (String) -> Unit,
    slots: List<SlotItem>,
    time: String,
    onTimeChange: (String) -> Unit,
    showEmptyState: Boolean,
    loading: Boolean,
    waitlistJoined: Boolean,
    onJoinWaitlist: () -> Unit
) {
    val days = remember { (0..29).map { LocalDate.now().plusDays(it.toLong()) } }
    val selectedDate = remember(date) { runCatching { LocalDate.parse(date) }.getOrNull() }

    Column {
        header()
        UrbanFieldLabel("Día")
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(days, key = { it.toString() }) { day ->
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
        UrbanFieldLabel("Hora")
        Spacer(Modifier.height(8.dp))
        when {
            slots.isNotEmpty() -> {
                // Agrupados por franja (como en la web): mañana < 12:00, tarde 12:00-17:59, noche desde 18:00.
                val groups = remember(slots) {
                    listOf(
                        Triple("Mañana", Icons.Default.WbSunny, slots.filter { (it.time.take(2).toIntOrNull() ?: 0) < 12 }),
                        Triple("Tarde", Icons.Default.WbTwilight, slots.filter { (it.time.take(2).toIntOrNull() ?: 0) in 12..17 }),
                        Triple("Noche", Icons.Default.NightsStay, slots.filter { (it.time.take(2).toIntOrNull() ?: 0) >= 18 })
                    ).filter { it.third.isNotEmpty() }
                }
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
                UrbanMascotState(
                    UrbanStateKind.EMPTY,
                    "Sin horarios este día",
                    "Prueba otro día o únete a la lista de espera y te avisamos si se libera un horario.",
                    actionLabel = if (!waitlistJoined) "Unirme a la lista de espera" else null,
                    actionIcon = Icons.Default.NotificationsActive,
                    onAction = if (!waitlistJoined) onJoinWaitlist else null
                )
                if (waitlistJoined) {
                    UrbanInfoBanner("Ya te anotamos en la lista de espera para este día.", Icons.Default.NotificationsActive)
                }
            }
            loading -> UrbanSkeletonList(2)
            else -> Unit
        }
    }
}
