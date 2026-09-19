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
            error?.let { item { UrbanErrorBanner(it) } }
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
            if (products.isEmpty() && !busy) item { UrbanEmptyState("No encontramos productos", "Prueba con otra búsqueda.", Icons.Default.Inventory2) }
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
fun OrdersScreen(user: AuthUser, onBack: () -> Unit, vm: OrdersViewModel = viewModel()) {
    val data by vm.data.collectAsState()
    val busy by vm.busy.collectAsState()
    val error by vm.error.collectAsState()
    val staff = user.roles.any { it == "administrador" || it == "recepcionista" }
    LaunchedEffect(Unit) { vm.load() }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            UrbanTopBar("Pedidos", onBack) {
                IconButton(onClick = { vm.load() }) { Icon(Icons.Default.Refresh, "Actualizar") }
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { UrbanPageHeader("Pedidos", if (staff) "Bandeja de pedidos y entregas." else "Sigue el estado de tus compras.", "Tienda") }
            if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = UrbanColors.Gold) }
            error?.let { item { UrbanErrorBanner(it) } }
            items(data.data, key = { it.id }) { order ->
                OrderCard(order, staff, cancel = { vm.cancel(order.id) }, deliver = { vm.deliver(order.id, it) })
            }
            if (data.data.isEmpty() && !busy) item { UrbanEmptyState("Aún no hay pedidos", "Tus compras aparecerán aquí.", Icons.Default.ShoppingBag) }
        }
    }
}

@Composable
private fun OrderCard(order: OrderRow, staff: Boolean, cancel: () -> Unit, deliver: (String) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    UrbanPremiumCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(order.folio ?: "Pedido", style = MaterialTheme.typography.titleMedium)
                order.createdAt?.let { Text(it.take(10), style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted) }
            }
            UrbanStatusPill(order.estado)
        }
        Spacer(Modifier.height(14.dp))
        order.client?.name?.let { UrbanKeyValue("Cliente", it) }
        order.items.take(4).forEach { line ->
            UrbanKeyValue("${line.cantidad} × ${line.nombre ?: "Producto"}", "\$${"%.2f".format(line.subtotal)}")
        }
        if (order.items.size > 4) Text("+${order.items.size - 4} productos", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = UrbanColors.Line)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Total", style = MaterialTheme.typography.labelLarge)
            Text("\$${"%.2f".format(order.total)} MXN", style = MaterialTheme.typography.titleLarge, color = UrbanColors.Gold)
        }
        if (order.estado == "pendiente") {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = cancel, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                if (staff) {
                    Box(Modifier.weight(1f)) {
                        Button(onClick = { menu = true }, modifier = Modifier.fillMaxWidth()) { Text("Entregar") }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            listOf("efectivo", "tarjeta", "transferencia").forEach { method ->
                                DropdownMenuItem(
                                    text = { Text(method.replaceFirstChar { it.uppercase() }) },
                                    onClick = { menu = false; deliver(method) }
                                )
                            }
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

    Scaffold(containerColor = Color.Transparent, topBar = { UrbanTopBar(if (staff) "Pagos" else "Mis pagos", onBack) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { UrbanPageHeader(if (staff) "Centro de cobro" else "Tus pagos", if (staff) "Comprobantes, historial y operación." else "Historial conectado a tus citas.", "Facturación") }
            if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = UrbanColors.Gold) }
            error?.let { item { UrbanErrorBanner(it) } }

            if (payments.data.isNotEmpty()) item { PaymentsSummary(payments.data, staff) }

            if (staff && pending.data.isNotEmpty()) {
                item { UrbanSectionTitle("Por revisar", "${pending.data.size} transferencias pendientes") }
                items(pending.data, key = { it.id }) { payment ->
                    UrbanPremiumCard(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("Transferencia", style = MaterialTheme.typography.labelMedium, color = UrbanColors.Gold)
                                Text("\$${"%.2f".format(payment.monto)} MXN", style = MaterialTheme.typography.titleLarge)
                            }
                            UrbanStatusPill("pendiente")
                        }
                        payment.ocrTexto?.takeIf { it.isNotBlank() }?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(it, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { vm.approve(payment.id, staff) }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Check, null); Spacer(Modifier.width(5.dp)); Text("Aprobar") }
                            OutlinedButton(onClick = { vm.reject(payment.id, "Comprobante no válido", staff) }, modifier = Modifier.weight(1f)) { Text("Rechazar") }
                        }
                    }
                }
            }

            item { UrbanSectionTitle("Historial", "Pagos registrados en UrbanBlade") }
            items(payments.data, key = { it.id }) { payment ->
                UrbanCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(payment.metodoPago?.replaceFirstChar { it.uppercase() } ?: "Pago", style = MaterialTheme.typography.titleMedium)
                            payment.appointment?.let { a ->
                                Text(listOfNotNull(a.service, a.barber, a.fecha).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted, maxLines = 2)
                            }
                        }
                        Text("\$${"%.2f".format(payment.monto + payment.propina)}", style = MaterialTheme.typography.titleLarge, color = UrbanColors.Gold)
                    }
                }
            }
            if (payments.data.isEmpty() && !busy) item { UrbanEmptyState("No hay pagos registrados", null, Icons.Default.ReceiptLong) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(onBack: () -> Unit, vm: NotificationsViewModel = viewModel()) {
    val data by vm.data.collectAsState()
    val busy by vm.busy.collectAsState()
    val error by vm.error.collectAsState()
    LaunchedEffect(Unit) { vm.load() }
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            UrbanTopBar("Notificaciones", onBack) {
                TextButton(onClick = { vm.readAll() }) { Text("Leer todas") }
            }
        }
    ) { padding ->
        JsonContentScreen(data, busy, error, Modifier.padding(padding), emptyIcon = Icons.Default.NotificationsNone)
    }
}

@Composable
private fun JsonContentScreen(
    data: JsonObject?,
    busy: Boolean,
    error: String?,
    modifier: Modifier = Modifier,
    emptyIcon: ImageVector
) {
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = UrbanColors.Gold) }
        error?.let { item { UrbanErrorBanner(it) } }
        data?.entrySet()?.forEach { (key, value) -> item { JsonCard(key, value) } }
        if (data == null && !busy && error == null) item { UrbanEmptyState("Sin información", "Este módulo todavía no tiene datos para mostrar.", emptyIcon) }
    }
}

@Composable
private fun JsonCard(label: String, value: JsonElement) {
    UrbanPremiumCard(Modifier.fillMaxWidth()) {
        Text(label.replace('_', ' ').replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleMedium, color = UrbanColors.Gold)
        Spacer(Modifier.height(10.dp))
        when {
            value.isJsonArray -> value.asJsonArray.take(20).forEachIndexed { index, element ->
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
                    Surface(shape = CircleShape, color = UrbanColors.Gold.copy(alpha = 0.09f), modifier = Modifier.size(24.dp)) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("${index + 1}", style = MaterialTheme.typography.labelMedium, color = UrbanColors.Gold) }
                    }
                    Spacer(Modifier.width(9.dp))
                    Text(compactJson(element), style = MaterialTheme.typography.bodySmall, color = UrbanColors.Ink, modifier = Modifier.weight(1f))
                }
            }
            value.isJsonObject -> value.asJsonObject.entrySet().take(20).forEach { (key, item) ->
                UrbanKeyValue(key.replace('_', ' ').replaceFirstChar { it.uppercase() }, compactJson(item), Modifier.padding(vertical = 4.dp))
            }
            else -> Text(compactJson(value), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

private fun compactJson(element: JsonElement): String = when {
    element.isJsonNull -> "—"
    element.isJsonPrimitive -> element.asJsonPrimitive.toString().trim('"')
    element.isJsonObject -> element.asJsonObject.entrySet().take(5).joinToString(" · ") { "${it.key}: ${compactJson(it.value)}" }
    element.isJsonArray -> "${element.asJsonArray.size()} elementos"
    else -> element.toString()
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
        add(ModuleItem("social", "Muro social", "Trabajos y comunidad", Icons.Default.Groups, "GENERAL"))
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
                trailing = { UrbanAvatar(user.name) }
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
