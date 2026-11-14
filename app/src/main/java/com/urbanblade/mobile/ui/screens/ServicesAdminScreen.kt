package com.urbanblade.mobile.ui.screens

import com.urbanblade.mobile.ui.components.UrbanSwitch
import com.urbanblade.mobile.ui.components.UrbanSwitchRow
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sell
import com.urbanblade.mobile.ui.components.UrbanFormChoice
import com.urbanblade.mobile.ui.components.UrbanFormDangerZone
import com.urbanblade.mobile.ui.components.UrbanFormRow
import com.urbanblade.mobile.ui.components.UrbanFormSection
import com.urbanblade.mobile.ui.components.UrbanFormSheet
import com.urbanblade.mobile.ui.components.UrbanFormSwitch
import com.urbanblade.mobile.ui.components.formBlockerText
import com.urbanblade.mobile.ui.components.rememberUrbanFormState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urbanblade.mobile.data.model.ServiceAdminItem
import com.urbanblade.mobile.data.model.ServiceUpsertRequest
import com.urbanblade.mobile.ui.components.UrbanCard
import com.urbanblade.mobile.ui.components.UrbanEmptyState
import com.urbanblade.mobile.ui.components.UrbanErrorBanner
import com.urbanblade.mobile.ui.components.UrbanFormat
import com.urbanblade.mobile.ui.components.UrbanInfoBanner
import com.urbanblade.mobile.ui.components.UrbanOutlineButton
import com.urbanblade.mobile.ui.components.UrbanPrimaryButton
import com.urbanblade.mobile.ui.components.UrbanSectionTitle
import com.urbanblade.mobile.ui.components.UrbanTextField
import com.urbanblade.mobile.ui.components.urbanFilterChipColors
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.ServiceStatusFilter
import com.urbanblade.mobile.ui.viewmodel.ServicesAdminViewModel
import com.urbanblade.mobile.ui.components.UrbanModuleScreen
import com.urbanblade.mobile.ui.components.UrbanFab
import com.urbanblade.mobile.ui.components.UrbanTextButton
import com.urbanblade.mobile.ui.components.isConfirmedEmpty
import com.urbanblade.mobile.ui.components.urbanListFeedback

/** Catálogo de servicios del administrador: buscar, filtrar, crear, editar, activar y eliminar. */
@Composable
fun ServicesAdminScreen(onBack: () -> Unit, vm: ServicesAdminViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    var editing by remember { mutableStateOf<ServiceAdminItem?>(null) }
    var creating by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<ServiceAdminItem?>(null) }

    LaunchedEffect(Unit) { vm.load() }

    val firstTime = state.query.isBlank() && state.status == ServiceStatusFilter.Todos

    UrbanModuleScreen(
        eyebrow = "ADMINISTRACIÓN",
        title = "Servicios",
        subtitle = "Precio, duración y lo que ven tus clientes al reservar.",
        onBack = onBack,
        onRefresh = { vm.load() },
        refreshing = state.loading || state.loadingMore || state.saving,
        floatingActionButton = { expanded ->
            UrbanFab("Nuevo servicio", Icons.Default.Add, expanded) { vm.clearMessages(); creating = true }
        },
        // Sin servicios y sin filtros el vacío trae «Nuevo servicio»: el flotante sobra. Con un filtro sin resultados se queda.
        hideFloatingActionButton = firstTime && isConfirmedEmpty(state.loaded, state.loading, state.items.isNotEmpty(), state.listError)
    ) {
        item {
            UrbanTextField(
                value = state.query,
                onValueChange = vm::setQuery,
                label = "Buscar servicio",
                leadingIcon = Icons.Default.Search,
                imeAction = ImeAction.Search,
                onImeAction = { vm.load() },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ServiceStatusFilter.entries.forEach { filter ->
                    FilterChip(selected = state.status == filter, onClick = { vm.setStatus(filter) }, label = { Text(filter.label) }, colors = urbanFilterChipColors())
                }
            }
        }
        if (state.saving) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = UrbanColors.Gold) }
        state.notice?.let { item { UrbanInfoBanner(it, Icons.Default.CheckCircle) } }
        // Con el formulario abierto el error de guardar se muestra dentro de la hoja, no aquí.
        if (editing == null && !creating) state.error?.let { item { UrbanErrorBanner(it) } }

        urbanListFeedback(
            state.loaded, state.loading, state.items.isNotEmpty(), state.listError,
            "No pudimos cargar los servicios",
            if (firstTime) "Aún no hay servicios" else "Sin resultados",
            if (firstTime) "Crea el primero para que tus clientes puedan reservar." else "Prueba con otra búsqueda o filtro.",
            emptyActionLabel = if (firstTime) "Nuevo servicio" else null,
            emptyActionIcon = Icons.Default.Add,
            onEmptyAction = if (firstTime) { { creating = true } } else null
        ) { vm.load() }

        if (state.items.isNotEmpty()) {
            item { UrbanSectionTitle("Catálogo", UrbanFormat.count(state.total, "servicio", "servicios")) }
        }
        items(state.items, key = { it.id }) { service ->
            ServiceAdminCard(
                service = service,
                onOpen = { vm.clearMessages(); editing = service },
                onToggle = { vm.toggleActive(service) }
            )
        }
        if (state.hasMore) {
            item {
                UrbanOutlineButton(
                    text = if (state.loadingMore) "Cargando…" else "Cargar más servicios",
                    onClick = { vm.loadMore() },
                    icon = Icons.Default.ExpandMore,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (creating || editing != null) {
        ServiceFormSheet(
            service = editing,
            categories = state.categories,
            saving = state.saving,
            error = state.error,
            onDismiss = { creating = false; editing = null },
            onSave = { body -> vm.save(editing?.slug, body) { creating = false; editing = null } },
            onDelete = editing?.let { current -> { vm.delete(current); editing = null } }
        )
    }

}

@Composable
private fun ServiceAdminCard(service: ServiceAdminItem, onOpen: () -> Unit, onToggle: () -> Unit) {
    UrbanCard(Modifier.fillMaxWidth(), onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    service.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (service.activo) UrbanColors.Ink else UrbanColors.Muted
                )
                Text(
                    "${service.categoria} · ${service.duracionMin} min" + if (service.activo) "" else " · Inactivo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = UrbanColors.Muted
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    UrbanFormat.moneyShort(service.precio),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (service.activo) UrbanColors.Gold else UrbanColors.Muted
                )
                UrbanSwitch(
                    checked = service.activo,
                    onCheckedChange = { onToggle() }
                )
            }
        }
    }
}

/** Errores por campo del formulario de servicio, en orden de aparición (vacío = todo bien). */
internal fun validateServiceForm(nombre: String, categoria: String, precio: String, duracion: String): LinkedHashMap<String, String> {
    val errors = LinkedHashMap<String, String>()
    if (nombre.isBlank()) errors["nombre"] = "Escribe el nombre del servicio."
    if (categoria.isBlank()) errors["categoria"] = "Elige o escribe una categoría."
    val p = precio.replace(',', '.').toDoubleOrNull()
    if (p == null || p < 0) errors["precio"] = "Escribe un precio válido, por ejemplo 180."
    val d = duracion.toIntOrNull()
    if (d == null || d !in 5..600) errors["duracion"] = "Entre 5 y 600 minutos."
    return errors
}

private val SERVICE_FIELD_LABEL = mapOf("nombre" to "Nombre", "categoria" to "Categoría", "precio" to "Precio", "duracion" to "Duración")

/** Formulario de servicio: el mismo para crear (service null) y para editar. */
@Composable
private fun ServiceFormSheet(
    service: ServiceAdminItem?,
    categories: List<String>,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (ServiceUpsertRequest) -> Unit,
    onDelete: (() -> Unit)?
) {
    var nombre by remember { mutableStateOf(service?.nombre.orEmpty()) }
    var categoria by remember { mutableStateOf(service?.categoria.orEmpty()) }
    var precio by remember { mutableStateOf(service?.let { "%.0f".format(it.precio) }.orEmpty()) }
    var duracion by remember { mutableStateOf(service?.duracionMin?.toString().orEmpty()) }
    var descripcion by remember { mutableStateOf(service?.descripcion.orEmpty()) }
    var activo by remember { mutableStateOf(service?.activo ?: true) }
    val form = rememberUrbanFormState()
    val errors = validateServiceForm(nombre, categoria, precio, duracion)

    UrbanFormSheet(
        eyebrow = "Servicios",
        title = if (service == null) "Nuevo servicio" else "Editar servicio",
        primaryLabel = if (service == null) "Crear servicio" else "Guardar cambios",
        onSubmit = {
            onSave(
                ServiceUpsertRequest(
                    nombre.trim(), categoria.trim(), precio.replace(',', '.').toDouble(), duracion.toInt(),
                    descripcion.trim().ifEmpty { null }, activo
                )
            )
        },
        onDismiss = onDismiss,
        saving = saving,
        blocker = formBlockerText(errors.keys.map { SERVICE_FIELD_LABEL.getValue(it) }),
        error = error
    ) {
        UrbanFormSection("Servicio", icon = Icons.Default.ContentCut) {
            UrbanTextField(
                nombre, { nombre = it }, "Nombre", Modifier.fillMaxWidth(),
                leadingIcon = Icons.Default.ContentCut, placeholder = "Ej. Corte clásico",
                error = form.error("nombre", errors["nombre"]), onBlur = { form.touch("nombre") },
                capitalization = KeyboardCapitalization.Sentences
            )
            UrbanTextField(
                categoria, { categoria = it }, "Categoría", Modifier.fillMaxWidth(),
                leadingIcon = Icons.Default.Sell, placeholder = "Ej. Corte, barba, combo",
                error = form.error("categoria", errors["categoria"]), onBlur = { form.touch("categoria") },
                capitalization = KeyboardCapitalization.Sentences
            )
            if (categories.isNotEmpty()) {
                UrbanFormChoice(label = "Categorías existentes", options = categories, selected = categoria.takeIf { it in categories }, onSelect = { categoria = it }, optionLabel = { it })
            }
            UrbanTextField(
                descripcion, { descripcion = it }, "Descripción (opcional)", Modifier.fillMaxWidth(),
                leadingIcon = Icons.Default.Notes, placeholder = "Lo que incluye el servicio",
                capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Default, minLines = 3
            )
        }
        UrbanFormSection("Precio y duración", icon = Icons.Default.Schedule) {
            UrbanFormRow {
                UrbanTextField(
                    precio, { precio = it.filter { c -> c.isDigit() || c == '.' || c == ',' } }, "Precio (MXN)", Modifier.weight(1f),
                    leadingIcon = Icons.Default.AttachMoney, placeholder = "0.00",
                    error = form.error("precio", errors["precio"]), onBlur = { form.touch("precio") },
                    keyboardType = KeyboardType.Decimal
                )
                UrbanTextField(
                    duracion, { duracion = it.filter(Char::isDigit) }, "Duración (min)", Modifier.weight(1f),
                    leadingIcon = Icons.Default.Schedule, placeholder = "30",
                    error = form.error("duracion", errors["duracion"]), onBlur = { form.touch("duracion") },
                    keyboardType = KeyboardType.Number
                )
            }
        }
        UrbanFormSection("Disponibilidad", icon = Icons.Default.PowerSettingsNew) {
            UrbanFormSwitch(
                "Disponible para reservar", "Si lo apagas, los clientes ya no lo ven.", activo, { activo = it },
                icon = Icons.Default.PowerSettingsNew
            )
        }
        onDelete?.let { UrbanFormDangerZone("servicio", it, enabled = !saving, note = "Si solo quieres dejar de ofrecerlo, desactívalo: así conserva su historial.") }
    }
}
