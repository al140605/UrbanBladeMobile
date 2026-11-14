package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import com.urbanblade.mobile.ui.components.urbanFilterChipColors
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urbanblade.mobile.core.push.routeForNotificationType
import com.urbanblade.mobile.ui.components.UrbanErrorBanner
import com.urbanblade.mobile.ui.components.UrbanFormat
import com.urbanblade.mobile.ui.components.UrbanMascotState
import com.urbanblade.mobile.ui.components.UrbanModuleScreen
import com.urbanblade.mobile.ui.components.UrbanOutlineButton
import com.urbanblade.mobile.ui.components.UrbanPillTabs
import com.urbanblade.mobile.ui.components.UrbanSectionTitle
import com.urbanblade.mobile.ui.components.UrbanStateKind
import com.urbanblade.mobile.ui.components.UrbanTextButton
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.NotificationsViewModel
import java.time.ZonedDateTime

internal val NOTIFICATION_CATEGORIES = listOf("Citas", "Pagos", "Pedidos", "Sistema")

/** Tipo de aviso para filtrar la bandeja; lo que no encaja en ninguno cae en «Otros» y no tiene filtro propio. */
internal fun notificationCategory(type: String?): String {
    val t = type?.lowercase().orEmpty()
    return when {
        t in setOf("appointment", "cita", "reminder", "service_ending", "service_overrun", "review_request") -> "Citas"
        t in setOf("payment", "pago", "deposit", "transfer_receipt", "membership_invoice") -> "Pagos"
        t.startsWith("order") || t == "pedido" -> "Pedidos"
        t in setOf("inventory_low_stock", "daily_summary", "barber_performance_report", "barber_review_flagged", "report_resolved") -> "Sistema"
        else -> "Otros"
    }
}

/**
 * Bandeja de avisos al estilo de las redes sociales: «Nuevas» arriba y lo leído por antigüedad, filtro
 * Todas / No leídas, una fila por aviso con su ícono, tiempo relativo y punto si no se ha leído, y un menú
 * por aviso (marcar como leído, eliminar). Tocar un aviso lo marca como leído y abre lo que anuncia.
 */
@Composable
fun NotificationsScreen(onBack: () -> Unit, onOpen: (String) -> Unit = {}, vm: NotificationsViewModel = viewModel()) {
    val data by vm.data.collectAsState()
    val busy by vm.busy.collectAsState()
    val error by vm.error.collectAsState()
    LaunchedEffect(Unit) { vm.load() }
    val view = remember(data) { parseNotifications(data) }
    // «Sin leer» y el tipo (Citas, Pagos, Pedidos, Sistema) se combinan; sin ninguno se ve todo.
    var unreadOnly by rememberSaveable { mutableStateOf(false) }
    var category by rememberSaveable { mutableStateOf("") }
    val now = remember(data) { ZonedDateTime.now() }
    val shown = remember(view, unreadOnly, category) {
        view.items.filter { (!unreadOnly || !it.read) && (category.isEmpty() || notificationCategory(it.type) == category) }
    }
    val categories = remember(view) { NOTIFICATION_CATEGORIES.filter { c -> view.items.any { notificationCategory(it.type) == c } } }
    val groups = remember(shown, now) { groupNotifications(shown, now) }

    UrbanModuleScreen(
        eyebrow = "CUENTA",
        title = "Notificaciones",
        subtitle = when {
            busy -> "Actualizando tus avisos…"
            error != null -> "La consulta necesita actualizarse."
            data == null -> "Tus avisos de UrbanBlade."
            view.unread > 0 -> UrbanFormat.count(view.unread, "aviso sin leer", "avisos sin leer")
            else -> "Estás al día."
        },
        onBack = onBack,
        onRefresh = { vm.load() },
        refreshing = busy,
        actions = {
            // «Marcar todas como leídas» vive en la barra, junto a actualizar, en lugar de ocupar una fila.
            if (view.unread > 0) IconButton(onClick = { if (!busy) vm.readAll() }) {
                Icon(Icons.Default.DoneAll, "Marcar todas como leídas", tint = UrbanColors.GoldText)
            }
        }
    ) {
        if (view.items.isNotEmpty()) item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = unreadOnly,
                    onClick = { unreadOnly = !unreadOnly },
                    label = { Text(if (view.unread > 0) "Sin leer · ${view.unread}" else "Sin leer") },
                    colors = urbanFilterChipColors()
                )
                categories.forEach { c ->
                    FilterChip(
                        selected = category == c,
                        onClick = { category = if (category == c) "" else c },
                        label = { Text(c) },
                        colors = urbanFilterChipColors()
                    )
                }
            }
        }
        if (error != null && view.items.isNotEmpty()) item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                UrbanErrorBanner(error!!)
                UrbanOutlineButton("Reintentar consulta", { vm.load() }, enabled = !busy, modifier = Modifier.fillMaxWidth())
            }
        }
        when {
            error != null && view.items.isEmpty() -> item {
                UrbanMascotState(UrbanStateKind.ERROR, "No pudimos cargar tus avisos", error, "Reintentar") { vm.load() }
            }
            view.items.isEmpty() && !busy -> item {
                UrbanMascotState(UrbanStateKind.EMPTY, "Sin notificaciones", "Aquí verás confirmaciones de citas, pagos y novedades.")
            }
            shown.isEmpty() && view.items.isNotEmpty() -> item {
                if (unreadOnly && category.isEmpty()) UrbanMascotState(UrbanStateKind.EMPTY, "Estás al día", "No tienes avisos sin leer.")
                else UrbanMascotState(UrbanStateKind.EMPTY, "Sin avisos con este filtro", "Quita un filtro para ver el resto.")
            }
            else -> groups.forEach { (group, rows) ->
                item(key = "titulo-${group.name}") { UrbanSectionTitle(group.label) }
                item(key = "grupo-${group.name}") {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = UrbanColors.Card,
                        border = BorderStroke(1.dp, UrbanColors.Line),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            rows.forEachIndexed { index, n ->
                                if (index > 0) HorizontalDivider(color = UrbanColors.Line)
                                val route = routeForNotificationType(n.type)
                                NotificationRow(
                                    n = n,
                                    now = now,
                                    hasRoute = route != null,
                                    onClick = {
                                        if (!n.read) vm.read(n.id)
                                        route?.let(onOpen)
                                    },
                                    onMarkRead = { vm.read(n.id) },
                                    onDelete = { vm.delete(n.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(
    n: NotificationItem,
    now: ZonedDateTime,
    hasRoute: Boolean,
    onClick: () -> Unit,
    onMarkRead: () -> Unit,
    onDelete: () -> Unit
) {
    val (icon, kindTone) = notificationLook(n)
    // Leídas en gris; sin leer, con el color de su tipo (cancelada en rojo, confirmada en verde...).
    val tone = if (n.read) UrbanColors.Muted else kindTone
    val time = relativeNotificationTime(n.createdAt, now)
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (n.read) Color.Transparent else UrbanColors.Gold.copy(alpha = 0.08f)),
        verticalAlignment = Alignment.Top
    ) {
        Row(
            Modifier
                .weight(1f)
                .clickable(onClickLabel = if (hasRoute) "Abrir" else "Marcar como leída", onClick = onClick)
                .semantics(mergeDescendants = true) {
                    stateDescription = if (n.read) "Leída" else "No leída"
                    customActions = buildList {
                        if (!n.read) add(CustomAccessibilityAction("Marcar como leída") { onMarkRead(); true })
                        add(CustomAccessibilityAction("Eliminar esta notificación") { onDelete(); true })
                    }
                }
                .heightIn(min = 72.dp)
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(tone.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = tone, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    n.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (n.read) FontWeight.Normal else FontWeight.Bold,
                    color = UrbanColors.Ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                n.message?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
                time?.let {
                    Spacer(Modifier.size(4.dp))
                    Text(
                        it,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (n.read) FontWeight.Normal else FontWeight.SemiBold,
                        color = if (n.read) UrbanColors.Muted else UrbanColors.GoldText
                    )
                }
            }
            if (!n.read) {
                Spacer(Modifier.width(8.dp))
                Box(Modifier.padding(top = 20.dp).size(10.dp).clip(CircleShape).background(UrbanColors.Gold))
            }
        }
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Default.MoreVert, "Más opciones del aviso", tint = UrbanColors.Muted)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                if (!n.read) DropdownMenuItem(
                    text = { Text("Marcar como leída") },
                    leadingIcon = { Icon(Icons.Default.MarkEmailRead, null) },
                    onClick = { menu = false; onMarkRead() }
                )
                DropdownMenuItem(
                    text = { Text("Eliminar esta notificación") },
                    leadingIcon = { Icon(Icons.Default.Delete, null) },
                    onClick = { menu = false; onDelete() }
                )
            }
        }
    }
}

/**
 * Ícono y color de un aviso. Todas las de citas llegan con type "appointment", así que el estado
 * (cancelada, confirmada, recordatorio...) se toma del título que manda el servidor.
 */
private fun notificationLook(n: NotificationItem): Pair<ImageVector, Color> {
    val type = n.type?.lowercase().orEmpty()
    val title = n.title.lowercase()
    return when {
        "cancel" in title || "expir" in type || "no asist" in title -> Icons.Default.EventBusy to UrbanColors.Danger
        "confirm" in title || "complet" in title || type == "order_delivered" -> Icons.Default.EventAvailable to UrbanColors.Success
        "recordatorio" in title || type == "reminder" -> Icons.Default.Alarm to UrbanColors.Info
        "reagend" in title || "cambi" in title -> Icons.Default.EditCalendar to UrbanColors.Warning
        type in setOf("service_ending", "service_overrun") -> Icons.Default.Alarm to UrbanColors.Warning
        type == "inventory_low_stock" -> Icons.Default.Inventory2 to UrbanColors.Warning
        type == "barber_review_flagged" -> Icons.Default.Flag to UrbanColors.Danger
        type in setOf("barber_performance_report", "daily_summary") -> Icons.Default.Assessment to UrbanColors.Info
        type == "report_resolved" -> Icons.Default.VerifiedUser to UrbanColors.Info
        type == "membership_invoice" -> Icons.Default.ReceiptLong to UrbanColors.Success
        type == "review_request" -> Icons.Default.RateReview to UrbanColors.Gold
        type in setOf("appointment", "cita") -> Icons.Default.CalendarMonth to UrbanColors.Gold
        type in setOf("payment", "pago", "deposit", "transfer_receipt") -> Icons.Default.Payments to UrbanColors.Success
        type.startsWith("order") || type == "pedido" -> Icons.Default.ShoppingBag to UrbanColors.Gold
        type.startsWith("loyalty") || type in setOf("points", "raffle", "raffle_win", "referral", "client_birthday") -> Icons.Default.Star to UrbanColors.Gold
        type == "promotion" -> Icons.Default.LocalOffer to UrbanColors.Gold
        else -> Icons.Default.Notifications to UrbanColors.Gold
    }
}
