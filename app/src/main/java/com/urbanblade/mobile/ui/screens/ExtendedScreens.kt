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
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.urbanblade.mobile.ui.components.UrbanFormat
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.*
import com.urbanblade.mobile.ui.theme.UrbanSpace

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreScreen(user: AuthUser, onOrders: () -> Unit, vm: StoreViewModel = viewModel(), onBack: (() -> Unit)? = null) {
    val products by vm.products.collectAsState()
    val cart by vm.cart.collectAsState()
    val knownProducts by vm.knownProducts.collectAsState()
    val busy by vm.busy.collectAsState()
    val error by vm.error.collectAsState()
    val message by vm.message.collectAsState()
    var query by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    val isClient = com.urbanblade.mobile.core.access.UserExperience.isClient(user.roles)
    val total = remember(cart, knownProducts) { vm.estimatedTotal }

    LaunchedEffect(Unit) { vm.load(query.trim().ifBlank { null }) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = { onBack?.let { UrbanTopBar("", it) } },
        bottomBar = {
            if (cart.isNotEmpty() && isClient) {
                Surface(
                    color = UrbanColors.Surface,
                    shadowElevation = 18.dp,
                    border = BorderStroke(1.dp, UrbanColors.Line),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = UrbanSpace.Gutter, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Total estimado · ${UrbanFormat.count(cart.values.sum(), "artículo", "artículos")}", style = MaterialTheme.typography.labelMedium, color = UrbanColors.Muted)
                        Text("${UrbanFormat.money(total)} MXN", style = MaterialTheme.typography.titleLarge, color = UrbanColors.GoldText)
                        Text("Paga y recoge en sucursal.", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                        UrbanPrimaryButton(
                            text = "Crear pedido",
                            onClick = { vm.checkout(onOrders) },
                            loading = busy,
                            enabled = !busy,
                            icon = Icons.Default.ShoppingBag,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = UrbanSpace.Gutter, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                UrbanPageHeader(
                    title = "Tienda",
                    subtitle = "Productos seleccionados para mantener tu estilo.",
                    eyebrow = "UrbanBlade Shop",
                    trailing = {
                        if (isClient || user.roles.any { it in listOf("administrador", "recepcionista") }) IconButton(onClick = onOrders) {
                            BadgedBox(badge = { if (cart.isNotEmpty()) Badge { Text(cart.values.sum().toString()) } }) {
                                Icon(Icons.Default.ReceiptLong, if (isClient) "Mis pedidos" else "Pedidos", tint = UrbanColors.Gold)
                            }
                        }
                    }
                )
            }
            item {
                UrbanTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = "Buscar productos",
                    leadingIcon = Icons.Default.Search,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Search,
                    onImeAction = { vm.load(query.trim().ifBlank { null }) },
                    modifier = Modifier.fillMaxWidth()
                )
                UrbanOutlineButton("Buscar", { vm.load(query.trim().ifBlank { null }) }, enabled = !busy,
                    modifier = Modifier.fillMaxWidth(), icon = Icons.Default.Search)
            }
            if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = UrbanColors.Gold) }
            if (error != null && products.isEmpty() && !busy) {
                item { UrbanMascotState(UrbanStateKind.ERROR, "No pudimos cargar la tienda", error, "Reintentar") { vm.load(query.ifBlank { null }) } }
            } else {
                error?.let { item { UrbanErrorBanner(it) } }
            }
            message?.let { item { UrbanInfoBanner(it, Icons.Default.CheckCircle) } }
            if (!isClient) item { UrbanInfoBanner("Consulta productos y precios. Los pedidos se crean desde una cuenta de cliente.", Icons.Default.Visibility) }
            item { UrbanSectionTitle("Productos", if (busy) "Consultando catálogo…" else UrbanFormat.count(products.size, "resultado", "resultados")) }
            items(products, key = { it.id }) { product ->
                ProductCard(
                    item = product,
                    qty = cart[product.id] ?: 0,
                    add = { if (isClient) vm.add(product.id) },
                    remove = { if (isClient) vm.remove(product.id) },
                    canShop = isClient && !busy
                )
            }
            if (products.isEmpty() && !busy && error == null) item {
                UrbanMascotState(UrbanStateKind.EMPTY, "No encontramos productos", "Prueba con otra búsqueda.")
            }
            item { Spacer(Modifier.height(if (cart.isNotEmpty()) 80.dp else 8.dp)) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
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
                    Modifier.size(70.dp).clip(RoundedCornerShape(16.dp)).background(UrbanColors.CardAlt),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Inventory2, null, tint = UrbanColors.Gold, modifier = Modifier.size(29.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                item.categoria?.let { Text(it.uppercase(), style = MaterialTheme.typography.labelMedium, color = UrbanColors.Muted) }
                Text(item.nombre, style = MaterialTheme.typography.titleMedium)
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
        }
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = UrbanColors.Line)
        Spacer(Modifier.height(10.dp))
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(UrbanFormat.moneyShort(item.precioVenta), style = MaterialTheme.typography.titleLarge, color = UrbanColors.Ink)
            if (canShop) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (qty > 0) {
                        SmallFloatingActionButton(onClick = remove, containerColor = UrbanColors.CardAlt, contentColor = UrbanColors.Ink) {
                            Icon(Icons.Default.Remove, "Quitar ${item.nombre} del carrito")
                        }
                        Text(qty.toString(), Modifier.padding(horizontal = 9.dp), style = MaterialTheme.typography.titleMedium)
                    }
                    if (item.stockActual > qty) {
                        SmallFloatingActionButton(onClick = add, containerColor = UrbanColors.Gold, contentColor = UrbanColors.OnGold) {
                            Icon(Icons.Default.Add, "Agregar ${item.nombre} al carrito")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentsScreen(user: AuthUser, onBack: () -> Unit) {
    val staff = user.roles.any { it == "administrador" || it == "recepcionista" }
    // El personal tiene su propia pantalla (filtros, estadísticas del servidor, revisión de comprobantes);
    // el cliente ve "Mis facturas": comprobantes de sus citas y de sus compras de productos.
    if (staff) PaymentsStaffScreen(onBack) else InvoicesScreen(onBack)
}

@Composable
fun MoreScreen(user: AuthUser, onNavigate: (String) -> Unit) {
    val roles = user.roles.toSet()
    val barber = "barbero" in roles
    val admin = "administrador" in roles
    val engineer = com.urbanblade.mobile.core.access.UserExperience.isEngineer(user.roles)
    // Cada grupo junta pantallas que antes eran módulos sueltos (ver Hubs.kt): menos puertas de entrada.
    val hubs = hubEntries(user)
    val quick = buildList {
        if (barber) add(ModuleItem("barber_agenda", "Mi agenda", "Tu día de trabajo", Icons.Default.Event, ""))
        if (!engineer) {
            add(ModuleItem("catalog", "Servicios y barberos", "Explora el catálogo", Icons.Default.ContentCut, ""))
            add(ModuleItem("social", "Muro de Inspiración", "Trabajos del equipo", Icons.Default.Groups, ""))
            if (!admin) add(ModuleItem("blocked_authors", "Autores bloqueados", "Deshacer un bloqueo del muro", Icons.Default.Block, ""))
        }
        add(ModuleItem("notifications", "Notificaciones", "Novedades de tu cuenta", Icons.Default.Notifications, ""))
        add(ModuleItem("chatbot", "Bladebot", "Asistente UrbanBlade", Icons.Default.SmartToy, ""))
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = UrbanSpace.Gutter, end = UrbanSpace.Gutter, top = 18.dp, bottom = com.urbanblade.mobile.ui.components.BladebotClearance),
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
        if (hubs.isNotEmpty()) {
            item { UrbanSectionTitle("Herramientas") }
            items(hubs.chunked(2)) { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { hub -> HubTile(hub, Modifier.weight(1f)) { onNavigate(hub.route) } }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        item { UrbanSectionTitle("Accesos") }
        item {
            UrbanRowGroup(quick) { module ->
                UrbanActionRow(
                    title = module.title,
                    subtitle = module.subtitle,
                    icon = module.icon,
                    onClick = { onNavigate(module.route) }
                )
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

/** Tarjeta de un grupo de Más: ícono, nombre, qué incluye y cuántas secciones tiene. */
@Composable
private fun HubTile(hub: HubEntry, modifier: Modifier, onClick: () -> Unit) {
    UrbanCard(modifier.heightIn(min = 124.dp), onClick = onClick) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(UrbanColors.Gold.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) { Icon(hub.icon, null, tint = UrbanColors.Gold) }
            Column {
                Text(hub.title, style = MaterialTheme.typography.titleMedium, color = UrbanColors.Ink)
                Text(hub.subtitle, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
            }
            if (hub.tabs > 1) Text("${hub.tabs} secciones", style = MaterialTheme.typography.labelSmall, color = UrbanColors.Gold)
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

