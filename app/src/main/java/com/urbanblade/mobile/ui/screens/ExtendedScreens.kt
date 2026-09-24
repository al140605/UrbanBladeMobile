package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.urbanblade.mobile.data.model.AuthUser
import com.urbanblade.mobile.data.model.OrderRow
import com.urbanblade.mobile.data.model.ProductItem
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreScreen(user: AuthUser, onOrders: () -> Unit, vm: StoreViewModel = viewModel()) {
    val products by vm.products.collectAsState()
    val cart by vm.cart.collectAsState()
    val busy by vm.busy.collectAsState()
    val error by vm.error.collectAsState()
    val message by vm.message.collectAsState()
    var query by remember { mutableStateOf("") }
    val isClient = user.roles.contains("cliente")
    val total = products.sumOf { it.precioVenta * (cart[it.id] ?: 0) }

    LaunchedEffect(Unit) { vm.load() }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            if (cart.isNotEmpty() && isClient) {
                Surface(
                    color = UrbanColors.Surface,
                    shadowElevation = 18.dp,
                    border = BorderStroke(1.dp, UrbanColors.Line),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("${cart.values.sum()} artículos", style = MaterialTheme.typography.labelMedium, color = UrbanColors.Muted)
                            Text("\$${"%.2f".format(total)} MXN", style = MaterialTheme.typography.titleLarge, color = UrbanColors.Gold)
                        }
                        UrbanPrimaryButton(
                            text = "Pedir",
                            onClick = { vm.checkout(onOrders) },
                            loading = busy,
                            icon = Icons.Default.ShoppingBag
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                UrbanPageHeader(
                    title = "Tienda",
                    subtitle = "Productos seleccionados para mantener tu estilo.",
                    eyebrow = "UrbanBlade Shop",
                    trailing = {
                        IconButton(onClick = onOrders) {
                            BadgedBox(badge = { if (cart.isNotEmpty()) Badge { Text(cart.values.sum().toString()) } }) {
                                Icon(Icons.Default.ReceiptLong, "Mis pedidos", tint = UrbanColors.Gold)
                            }
                        }
                    }
                )
            }
            item {
                OutlinedTextField(
                    query,
                    { query = it },
                    placeholder = { Text("Buscar productos") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        IconButton(onClick = { vm.load(query.ifBlank { null }) }) { Icon(Icons.Default.Tune, "Buscar") }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
            }
            if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = UrbanColors.Gold) }
            if (error != null && products.isEmpty() && !busy) {
                item { UrbanMascotState(UrbanStateKind.ERROR, "No pudimos cargar la tienda", error, "Reintentar") { vm.load(query.ifBlank { null }) } }
            } else {
                error?.let { item { UrbanErrorBanner(it) } }
            }
            message?.let { item { UrbanInfoBanner(it, Icons.Default.CheckCircle) } }
            if (!isClient) item { UrbanInfoBanner("Puedes explorar la tienda; el checkout está disponible para cuentas cliente.", Icons.Default.Visibility) }
            item { UrbanSectionTitle("Productos", "${products.size} disponibles") }
            items(products, key = { it.id }) { product ->
                ProductCard(
                    item = product,
                    qty = cart[product.id] ?: 0,
                    add = { if (isClient) vm.add(product.id) },
                    remove = { if (isClient) vm.remove(product.id) },
                    canShop = isClient
                )
            }
            if (products.isEmpty() && !busy && error == null) item {
                UrbanMascotState(UrbanStateKind.EMPTY, "No encontramos productos", "Prueba con otra búsqueda.")
            }
            item { Spacer(Modifier.height(if (cart.isNotEmpty()) 80.dp else 8.dp)) }
        }
    }
}

@Composable
private fun ProductCard(item: ProductItem, qty: Int, add: () -> Unit, remove: () -> Unit, canShop: Boolean) {
    UrbanPremiumCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!item.imagen.isNullOrBlank()) {
                AsyncImage(
                    model = item.imagen,
                    contentDescription = item.nombre,
                    modifier = Modifier.size(70.dp).clip(RoundedCornerShape(16.dp))
                )
            } else {
                Box(
                    Modifier.size(70.dp).clip(RoundedCornerShape(16.dp)).background(UrbanColors.Gold.copy(alpha = 0.09f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Inventory2, null, tint = UrbanColors.Gold, modifier = Modifier.size(29.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                item.categoria?.let { Text(it.uppercase(), style = MaterialTheme.typography.labelMedium, color = UrbanColors.Gold) }
                Text(item.nombre, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                // Muchas descripciones empiezan repitiendo el nombre ("Aceite — producto…"): se quita ese prefijo.
                item.descripcion
                    ?.removePrefix(item.nombre)
                    ?.trimStart(' ', '—', '–', '-', ':')
                    ?.trim()?.replaceFirstChar { c -> c.uppercase() }
                    ?.takeIf { it.isNotBlank() }
                    ?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                // El comprador no necesita el inventario exacto: solo avisar si se agota.
                when {
                    item.stockActual <= 0 -> {
                        Spacer(Modifier.height(7.dp))
                        Text("Agotado", style = MaterialTheme.typography.labelMedium, color = UrbanColors.Danger)
                    }
                    item.stockActual <= 5 -> {
                        Spacer(Modifier.height(7.dp))
                        Text(
                            if (item.stockActual == 1) "Última pieza" else "Últimas ${item.stockActual} piezas",
                            style = MaterialTheme.typography.labelMedium,
                            color = UrbanColors.Warning
                        )
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text("\$${"%.0f".format(item.precioVenta)}", style = MaterialTheme.typography.titleLarge, color = UrbanColors.Gold)
                if (canShop) {
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (qty > 0) {
                            SmallFloatingActionButton(onClick = remove, containerColor = UrbanColors.CardAlt, contentColor = UrbanColors.Ink) { Icon(Icons.Default.Remove, "Quitar del carrito") }
                            Text(qty.toString(), Modifier.padding(horizontal = 9.dp), style = MaterialTheme.typography.titleMedium)
                        }
                        if (item.stockActual > 0) {
                            SmallFloatingActionButton(
                                onClick = add,
                                containerColor = UrbanColors.Gold,
                                contentColor = UrbanColors.OnGold,
                            ) { Icon(Icons.Default.Add, "Agregar al carrito") }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentsScreen(user: AuthUser, onBack: () -> Unit, vm: PaymentsViewModel = viewModel()) {
    val staff = user.roles.any { it == "administrador" || it == "recepcionista" }
    // El personal tiene su propia pantalla (filtros, estadísticas del servidor, revisión de comprobantes).
    if (staff) {
        PaymentsStaffScreen(onBack)
        return
    }
    val payments by vm.payments.collectAsState()
    val pending by vm.pending.collectAsState()
    val busy by vm.busy.collectAsState()
    val error by vm.error.collectAsState()
    LaunchedEffect(Unit) { vm.load(staff) }

    UrbanModuleScreen(
        eyebrow = "CUENTA",
        title = "Mis pagos",
        subtitle = "Lo que has pagado, conectado a tus citas.",
        onBack = onBack,
        onRefresh = { vm.load(staff) },
        refreshing = busy
    ) {
        when {
            error != null && payments.data.isEmpty() -> item {
                UrbanMascotState(UrbanStateKind.ERROR, "No pudimos cargar tus pagos", error, "Reintentar") { vm.load(staff) }
            }
            payments.data.isEmpty() && !busy -> item {
                UrbanMascotState(UrbanStateKind.EMPTY, "Aún no hay pagos", "Cuando pagues una cita o un pedido lo verás aquí con su comprobante.")
            }
            else -> {
                error?.let { item { UrbanErrorBanner(it) } }
                item { PaymentsSummary(payments.data, staff) }
                item { UrbanSectionTitle("Historial", UrbanFormat.count(payments.data.size, "pago", "pagos")) }
                items(payments.data, key = { it.id }) { payment ->
                    val metodo = payment.metodoPago?.lowercase()
                    UrbanAttentionRow(
                        icon = when (metodo) {
                            "tarjeta" -> Icons.Default.CreditCard
                            "transferencia" -> Icons.Default.AccountBalance
                            else -> Icons.Default.Payments
                        },
                        text = payment.appointment?.service ?: payment.metodoPago?.replaceFirstChar { it.uppercase() } ?: "Pago",
                        subtitle = listOfNotNull(
                            payment.metodoPago?.replaceFirstChar { it.uppercase() },
                            payment.appointment?.barber,
                            payment.appointment?.fecha?.let { UrbanFormat.date(it) }
                        ).joinToString(" · ").ifBlank { null },
                        tone = UrbanColors.Gold,
                        trailing = {
                            Text("\$${"%,.2f".format(payment.monto + payment.propina)}", style = MaterialTheme.typography.titleMedium, color = UrbanColors.Gold)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationsScreen(onBack: () -> Unit, vm: NotificationsViewModel = viewModel()) {
    val data by vm.data.collectAsState()
    val busy by vm.busy.collectAsState()
    val error by vm.error.collectAsState()
    LaunchedEffect(Unit) { vm.load() }
    val view = parseNotifications(data)

    UrbanModuleScreen(
        eyebrow = "CUENTA",
        title = "Notificaciones",
        subtitle = if (view.unread > 0) UrbanFormat.count(view.unread, "aviso sin leer", "avisos sin leer") else "Estás al día.",
        onBack = onBack,
        onRefresh = { vm.load() },
        refreshing = busy
    ) {
        if (view.unread > 0) item {
            UrbanOutlineButton(
                text = "Marcar todas como leídas",
                onClick = { vm.readAll() },
                icon = Icons.Default.DoneAll,
                modifier = Modifier.fillMaxWidth()
            )
        }
        when {
            error != null && view.items.isEmpty() -> item {
                UrbanMascotState(UrbanStateKind.ERROR, "No pudimos cargar tus avisos", error, "Reintentar") { vm.load() }
            }
            view.items.isEmpty() && !busy -> item {
                UrbanMascotState(UrbanStateKind.EMPTY, "Sin notificaciones", "Aquí verás confirmaciones de citas, pagos y novedades.")
            }
            else -> items(view.items, key = { it.id }) { n ->
                NotificationRow(n) { if (!n.read) vm.read(n.id) }
            }
        }
    }
}

@Composable
private fun NotificationRow(n: NotificationItem, onOpen: () -> Unit) {
    val tone = if (n.read) UrbanColors.Muted else UrbanColors.Gold
    UrbanCard(Modifier.fillMaxWidth(), onClick = onOpen) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(tone.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    when (n.type?.lowercase()) {
                        "appointment", "cita", "reminder" -> Icons.Default.CalendarMonth
                        "payment", "pago", "deposit" -> Icons.Default.Payments
                        "order", "pedido" -> Icons.Default.ShoppingBag
                        "loyalty", "points", "raffle", "referral" -> Icons.Default.Star
                        else -> Icons.Default.Notifications
                    },
                    null,
                    tint = tone,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    n.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (n.read) FontWeight.Normal else FontWeight.Bold,
                    color = UrbanColors.Ink
                )
                n.message?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
                n.createdAt?.let { iso ->
                    val whenText = runCatching {
                        val date = java.time.OffsetDateTime.parse(iso)
                        UrbanFormat.date(date.toLocalDate().toString()) + " · " + UrbanFormat.time(date.toLocalTime().toString())
                    }.getOrNull()
                    whenText?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, style = MaterialTheme.typography.labelSmall, color = UrbanColors.Muted)
                    }
                }
            }
            if (!n.read) {
                Spacer(Modifier.width(8.dp))
                Box(Modifier.padding(top = 6.dp).size(9.dp).clip(CircleShape).background(UrbanColors.Gold))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatbotScreen(onBack: () -> Unit, vm: ChatbotViewModel = viewModel()) {
    val messages by vm.messages.collectAsState()
    val busy by vm.busy.collectAsState()
    var text by remember { mutableStateOf("") }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = { UrbanTopBar("Bladebot", onBack) },
        bottomBar = {
            Surface(color = UrbanColors.Surface, shadowElevation = 16.dp, border = BorderStroke(1.dp, UrbanColors.Line)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        text,
                        { text = it },
                        Modifier.weight(1f),
                        placeholder = { Text("Pregunta a Bladebot…") },
                        leadingIcon = { Icon(Icons.Default.SmartToy, null) },
                        shape = MaterialTheme.shapes.medium,
                        maxLines = 4
                    )
                    Spacer(Modifier.width(8.dp))
                    FloatingActionButton(
                        onClick = { val current = text; text = ""; vm.send(current) },
                        containerColor = UrbanColors.Gold,
                        contentColor = UrbanColors.OnGold,
                        modifier = Modifier.size(50.dp)
                    ) { Icon(Icons.Default.Send, "Enviar") }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    UrbanPremiumCard(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = UrbanColors.Gold.copy(alpha = 0.09f), modifier = Modifier.size(54.dp)) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.SmartToy, null, tint = UrbanColors.Gold) }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Hola, soy Bladebot", style = MaterialTheme.typography.titleMedium)
                                Text("Puedo ayudarte con UrbanBlade y sus módulos.", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                            }
                        }
                    }
                }
            }
            items(messages) { message ->
                val fromUser = message.first
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = if (fromUser) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        modifier = Modifier.widthIn(max = 310.dp),
                        shape = RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (fromUser) 18.dp else 5.dp,
                            bottomEnd = if (fromUser) 5.dp else 18.dp
                        ),
                        color = if (fromUser) UrbanColors.Gold else UrbanColors.Card,
                        contentColor = if (fromUser) UrbanColors.OnGold else UrbanColors.Ink,
                        border = if (fromUser) null else BorderStroke(1.dp, UrbanColors.Line)
                    ) {
                        Text(message.second, Modifier.padding(horizontal = 14.dp, vertical = 11.dp), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = UrbanColors.Gold) }
        }
    }
}

@Composable
fun MoreScreen(user: AuthUser, onNavigate: (String) -> Unit) {
    val roles = user.roles.toSet()
    val staff = roles.any { it == "administrador" || it == "recepcionista" }
    val admin = "administrador" in roles
    val engineer = "ingeniero" in roles
    val barber = "barbero" in roles
    val client = "cliente" in roles

    val modules = buildList {
        add(ModuleItem("catalog", "Servicios y barberos", "Explora el catálogo", Icons.Default.ContentCut, "GENERAL"))
        add(ModuleItem("notifications", "Notificaciones", "Novedades de tu cuenta", Icons.Default.Notifications, "GENERAL"))
        add(ModuleItem("analytics", "Analítica", "Actividad y rendimiento", Icons.Default.QueryStats, "GENERAL"))
        add(ModuleItem("social", "Muro de Inspiración", "Trabajos del equipo", Icons.Default.Groups, "GENERAL"))
        add(ModuleItem("chatbot", "Bladebot", "Asistente UrbanBlade", Icons.Default.SmartToy, "GENERAL"))
        if (staff || client) {
            add(ModuleItem("payments", "Pagos", "Historial y comprobantes", Icons.Default.Payments, "OPERACIÓN"))
            add(ModuleItem("orders", "Pedidos", "Tienda y entregas", Icons.Default.ReceiptLong, "OPERACIÓN"))
        }
        if (staff) {
            add(ModuleItem("clients", "Clientes", "Atención y gestión", Icons.Default.People, "OPERACIÓN"))
            add(ModuleItem("inventory", "Inventario", "Stock y movimientos", Icons.Default.Inventory2, "OPERACIÓN"))
            add(ModuleItem("inventory_movements", "Movimientos de inventario", "Entradas y salidas de stock", Icons.Default.SwapVert, "OPERACIÓN"))
            add(ModuleItem("waitlist_staff", "Lista de espera", "Clientes esperando un horario", Icons.Default.HourglassTop, "OPERACIÓN"))
            add(ModuleItem("gift_cards", "Gift cards", "Vender y consultar saldo", Icons.Default.CardGiftcard, "OPERACIÓN"))
            add(ModuleItem("cash", "Corte de caja", "Resumen de turno", Icons.Default.PointOfSale, "OPERACIÓN"))
        }
        if (barber) {
            add(ModuleItem("barber_agenda", "Mi agenda", "Tu día de trabajo", Icons.Default.Event, "BARBERO"))
            add(ModuleItem("barber_portfolio", "Portafolio", "Muestra tus trabajos", Icons.Default.PhotoLibrary, "BARBERO"))
            add(ModuleItem("barber_schedule", "Mi horario", "Disponibilidad semanal", Icons.Default.Schedule, "BARBERO"))
        }
        if (admin || engineer) {
            add(ModuleItem("reports", "Reportes", "Datos del negocio", Icons.Default.Assessment, "ANÁLISIS"))
            add(ModuleItem("logs", "Logs", "Trazabilidad del sistema", Icons.Default.Terminal, "ANÁLISIS"))
            add(ModuleItem("admin_metrics", "Métricas", "KPIs operativos", Icons.Default.MonitorHeart, "ANÁLISIS"))
            add(ModuleItem("insights", "Insights IA", "Señales y predicciones", Icons.Default.AutoAwesome, "ANÁLISIS"))
        }
        if (admin) {
            add(ModuleItem("offers_admin", "Membresías y paquetes", "Planes mensuales y paquetes de usos", Icons.Default.CardMembership, "ADMIN"))
            add(ModuleItem("barbers_admin", "Barberos", "Equipo, comisiones y rendimiento", Icons.Default.Badge, "ADMIN"))
            add(ModuleItem("services_admin", "Servicios", "Catálogo, precios y duración", Icons.Default.ContentCut, "ADMIN"))
            add(ModuleItem("campaigns", "Campañas", "Marketing y alcance", Icons.Default.Campaign, "ADMIN"))
            add(ModuleItem("raffles", "Sorteos", "Lealtad y promociones", Icons.Default.EmojiEvents, "ADMIN"))
            add(ModuleItem("reviews", "Reseñas", "Opiniones de clientes", Icons.Default.Star, "ADMIN"))
            add(ModuleItem("users", "Usuarios", "Roles y accesos", Icons.Default.ManageAccounts, "ADMIN"))
            add(ModuleItem("settings", "Configuración", "Ajustes del negocio", Icons.Default.Settings, "ADMIN"))
        }
        if (engineer || admin) add(ModuleItem("system", "Estado del sistema", "Servicios y salud", Icons.Default.Dns, "SISTEMA"))
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            UrbanPageHeader(
                title = "Más herramientas",
                subtitle = "Tu panel cambia según responsabilidades y permisos.",
                eyebrow = "Módulos",
                trailing = { UrbanAvatar(user.name, imageUrl = user.avatarUrl) }
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                user.roles.take(3).forEach { UrbanRolePill(it) }
            }
        }
        modules.groupBy { it.section }.forEach { (section, group) ->
            item { UrbanSectionTitle(section.replaceFirstChar { it.uppercase() }) }
            items(group) { module -> ModuleCard(module, onNavigate) }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun ModuleCard(module: ModuleItem, onNavigate: (String) -> Unit) {
    UrbanCard(Modifier.fillMaxWidth(), onClick = { onNavigate(module.route) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(UrbanColors.Gold.copy(alpha = 0.09f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(module.icon, null, tint = UrbanColors.Gold, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(module.title, style = MaterialTheme.typography.titleMedium)
                Text(module.subtitle, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
            }
            Icon(Icons.Default.ChevronRight, null, tint = UrbanColors.Gold)
        }
    }
}

private data class ModuleItem(
    val route: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val section: String
)
