package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urbanblade.mobile.data.model.AuthUser
import com.urbanblade.mobile.data.model.CreateProductRequest
import com.urbanblade.mobile.data.model.InventoryProductRow
import com.urbanblade.mobile.data.model.UpdateProductRequest
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.InventoryViewModel

private val TIPO_LABEL = mapOf("venta_cliente" to "Venta a cliente", "insumo_trabajo" to "Insumo de trabajo")

@Composable
fun InventoryListScreen(user: AuthUser, onBack: () -> Unit, vm: InventoryViewModel = viewModel()) {
    val response by vm.products.collectAsState()
    val busy by vm.busy.collectAsState()
    val saving by vm.saving.collectAsState()
    val message by vm.message.collectAsState()
    val error by vm.error.collectAsState()
    val isAdmin = user.roles.contains("administrador")

    var categoryFilter by remember { mutableStateOf<String?>(null) }
    var showMovement by remember { mutableStateOf<InventoryProductRow?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var showEdit by remember { mutableStateOf<InventoryProductRow?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<InventoryProductRow?>(null) }

    LaunchedEffect(Unit) { vm.load() }

    val visibleProducts = remember(response, categoryFilter) {
        response.data.filter { categoryFilter == null || it.categoria == categoryFilter }
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            UrbanTopBar("Inventario", onBack) {
                if (isAdmin) {
                    IconButton(onClick = { showCreate = true }) { Icon(Icons.Default.Add, "Agregar producto") }
                }
                IconButton(onClick = { vm.load() }) { Icon(Icons.Default.Refresh, "Actualizar") }
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = UrbanColors.Gold) }
            error?.let { item { UrbanErrorBanner(it) } }
            message?.let { item { UrbanInfoBanner(it, Icons.Default.CheckCircle) } }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    UrbanMetricCard("Productos", response.meta.stats.total.toString(), Icons.Default.Inventory2, Modifier.weight(1f))
                    UrbanMetricCard("Stock bajo", response.meta.stats.bajoStock.toString(), Icons.Default.WarningAmber, Modifier.weight(1f))
                    UrbanMetricCard("Valor total", "\$${"%.0f".format(response.meta.stats.valorTotal)}", Icons.Default.AttachMoney, Modifier.weight(1f))
                }
            }

            if (response.meta.categorias.isNotEmpty()) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = categoryFilter == null, onClick = { categoryFilter = null }, label = { Text("Todas") })
                        response.meta.categorias.forEach { cat ->
                            FilterChip(selected = categoryFilter == cat, onClick = { categoryFilter = if (categoryFilter == cat) null else cat }, label = { Text(cat) })
                        }
                    }
                }
            }

            if (visibleProducts.isEmpty() && !busy) {
                item { UrbanEmptyState("Sin productos", "No hay productos en esta categoría.", Icons.Default.Inventory2) }
            }

            items(visibleProducts) { product ->
                UrbanCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            product.categoria?.let { Text(it.uppercase(), style = MaterialTheme.typography.labelMedium, color = UrbanColors.Gold) }
                            Text(product.nombre, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Stock: ${product.stockActual} (mín. ${product.stockMinimo})", style = MaterialTheme.typography.bodySmall, color = if (product.lowStock) UrbanColors.Danger else UrbanColors.Muted)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("\$${"%.2f".format(product.precioVenta)}", style = MaterialTheme.typography.titleMedium, color = UrbanColors.Gold)
                            if (product.lowStock) SimpleStatusPill("down")
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        UrbanOutlineButton(text = "Movimiento", onClick = { showMovement = product }, icon = Icons.Default.SwapVert, modifier = Modifier.weight(1f))
                        if (isAdmin) {
                            IconButton(onClick = { showEdit = product }) { Icon(Icons.Default.Edit, "Editar") }
                            IconButton(onClick = { showDeleteConfirm = product }) { Icon(Icons.Default.Delete, "Eliminar", tint = UrbanColors.Danger) }
                        }
                    }
                }
            }
        }
    }

    showMovement?.let { product ->
        MovementDialog(
            product = product,
            allowEntrada = isAdmin,
            saving = saving,
            onDismiss = { showMovement = null },
            onConfirm = { tipo, cantidad, motivo ->
                vm.registerMovement(product.id, tipo, cantidad, motivo) { showMovement = null }
            }
        )
    }

    if (showCreate) {
        ProductFormDialog(
            title = "Nuevo producto",
            initial = null,
            saving = saving,
            onDismiss = { showCreate = false },
            onSubmit = { req -> vm.createProduct(req) { showCreate = false } }
        )
    }

    showEdit?.let { product ->
        ProductFormDialog(
            title = "Editar producto",
            initial = product,
            saving = saving,
            onDismiss = { showEdit = null },
            onSubmit = { req ->
                vm.updateProduct(
                    product.id,
                    UpdateProductRequest(req.nombre, req.categoria, req.descripcion, req.precioCompra, req.precioVenta, req.stockActual, req.stockMinimo, req.tipo, req.activo)
                ) { showEdit = null }
            }
        )
    }

    showDeleteConfirm?.let { product ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("¿Eliminar ${product.nombre}?") },
            text = { Text("Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteProduct(product.id); showDeleteConfirm = null }) { Text("Sí, eliminar") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun MovementDialog(
    product: InventoryProductRow,
    allowEntrada: Boolean,
    saving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, Int, String?) -> Unit
) {
    // La recepción solo puede registrar salidas (consumo) -- misma regla
    // que ya valida InventoryController::storeMovement() en barber, no se
    // inventa aquí, solo se refleja.
    var tipo by remember { mutableStateOf("salida") }
    var cantidad by remember { mutableStateOf("1") }
    var motivo by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Movimiento: ${product.nombre}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (allowEntrada) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = tipo == "entrada", onClick = { tipo = "entrada" }, label = { Text("Entrada") })
                        FilterChip(selected = tipo == "salida", onClick = { tipo = "salida" }, label = { Text("Salida") })
                    }
                } else {
                    Text("Tipo: Salida (consumo)", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                }
                OutlinedTextField(cantidad, { cantidad = it.filter(Char::isDigit) }, label = { Text("Cantidad") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(motivo, { motivo = it }, label = { Text("Motivo (opcional)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving && (cantidad.toIntOrNull() ?: 0) > 0,
                onClick = { onConfirm(tipo, cantidad.toInt(), motivo.takeIf { it.isNotBlank() }) }
            ) { Text(if (saving) "Guardando…" else "Registrar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun ProductFormDialog(
    title: String,
    initial: InventoryProductRow?,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (CreateProductRequest) -> Unit
) {
    var nombre by remember { mutableStateOf(initial?.nombre.orEmpty()) }
    var categoria by remember { mutableStateOf(initial?.categoria.orEmpty()) }
    var descripcion by remember { mutableStateOf(initial?.descripcion.orEmpty()) }
    var precioCompra by remember { mutableStateOf(initial?.precioCompra?.toString().orEmpty()) }
    var precioVenta by remember { mutableStateOf(initial?.precioVenta?.toString().orEmpty()) }
    var stockActual by remember { mutableStateOf(initial?.stockActual?.toString().orEmpty()) }
    var stockMinimo by remember { mutableStateOf(initial?.stockMinimo?.toString().orEmpty()) }
    var tipo by remember { mutableStateOf(initial?.tipo ?: "venta_cliente") }

    val valid = nombre.isNotBlank() && categoria.isNotBlank() &&
        precioCompra.toDoubleOrNull() != null && precioVenta.toDoubleOrNull() != null &&
        stockActual.toIntOrNull() != null && stockMinimo.toIntOrNull() != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(nombre, { nombre = it }, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(categoria, { categoria = it }, label = { Text("Categoría") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(descripcion, { descripcion = it }, label = { Text("Descripción (opcional)") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(precioCompra, { precioCompra = it }, label = { Text("Precio compra") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(precioVenta, { precioVenta = it }, label = { Text("Precio venta") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(stockActual, { stockActual = it.filter(Char::isDigit) }, label = { Text("Stock actual") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(stockMinimo, { stockMinimo = it.filter(Char::isDigit) }, label = { Text("Stock mínimo") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TIPO_LABEL.forEach { (key, label) ->
                        FilterChip(selected = tipo == key, onClick = { tipo = key }, label = { Text(label) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving && valid,
                onClick = {
                    onSubmit(
                        CreateProductRequest(
                            nombre = nombre,
                            categoria = categoria,
                            descripcion = descripcion.takeIf { it.isNotBlank() },
                            precioCompra = precioCompra.toDouble(),
                            precioVenta = precioVenta.toDouble(),
                            stockActual = stockActual.toInt(),
                            stockMinimo = stockMinimo.toInt(),
                            tipo = tipo
                        )
                    )
                }
            ) { Text(if (saving) "Guardando…" else "Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
