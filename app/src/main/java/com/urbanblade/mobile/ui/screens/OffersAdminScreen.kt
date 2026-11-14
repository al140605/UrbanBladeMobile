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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Tag
import com.urbanblade.mobile.ui.components.UrbanFormChoice
import com.urbanblade.mobile.ui.components.UrbanFormRow
import com.urbanblade.mobile.ui.components.UrbanFormSection
import com.urbanblade.mobile.ui.components.UrbanFormSheet
import com.urbanblade.mobile.ui.components.UrbanFormSwitch
import com.urbanblade.mobile.ui.components.formBlockerText
import com.urbanblade.mobile.ui.components.rememberUrbanFormState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urbanblade.mobile.data.model.MembershipPlanItem
import com.urbanblade.mobile.data.model.MembershipPlanRequest
import com.urbanblade.mobile.data.model.ServiceItem
import com.urbanblade.mobile.data.model.ServicePackageItem
import com.urbanblade.mobile.data.model.ServicePackageRequest
import com.urbanblade.mobile.ui.components.UrbanCard
import com.urbanblade.mobile.ui.components.UrbanEmptyState
import com.urbanblade.mobile.ui.components.UrbanErrorBanner
import com.urbanblade.mobile.ui.components.UrbanFieldLabel
import com.urbanblade.mobile.ui.components.UrbanFormat
import com.urbanblade.mobile.ui.components.UrbanInfoBanner
import com.urbanblade.mobile.ui.components.UrbanPrimaryButton
import com.urbanblade.mobile.ui.components.UrbanSectionTitle
import com.urbanblade.mobile.ui.components.UrbanTextField
import com.urbanblade.mobile.ui.components.urbanFilterChipColors
import com.urbanblade.mobile.ui.components.UrbanPillTabs
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.OffersAdminViewModel
import com.urbanblade.mobile.ui.components.UrbanModuleScreen
import com.urbanblade.mobile.ui.components.UrbanFab
import com.urbanblade.mobile.ui.components.isConfirmedEmpty
import com.urbanblade.mobile.ui.components.urbanListFeedback

private enum class OffersTab(val label: String) { Membresias("Membresías"), Paquetes("Paquetes") }

private fun money(value: Double) = UrbanFormat.moneyShort(value)

/** Membresías mensuales y paquetes prepagados: crear, editar y activar o desactivar. */
@Composable
fun OffersAdminScreen(onBack: () -> Unit, vm: OffersAdminViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    var tab by remember { mutableStateOf(OffersTab.Membresias) }
    var editingPlan by remember { mutableStateOf<MembershipPlanItem?>(null) }
    var creatingPlan by remember { mutableStateOf(false) }
    var editingPackage by remember { mutableStateOf<ServicePackageItem?>(null) }
    var creatingPackage by remember { mutableStateOf(false) }
    val formOpen = editingPlan != null || creatingPlan || editingPackage != null || creatingPackage

    LaunchedEffect(Unit) { vm.load() }

    UrbanModuleScreen(
        eyebrow = "ADMINISTRACIÓN",
        title = "Membresías y paquetes",
        subtitle = "Planes mensuales con descuento y usos pagados por adelantado.",
        onBack = onBack,
        onRefresh = { vm.load() },
        refreshing = state.loading || state.saving,
        floatingActionButton = { expanded ->
            UrbanFab(if (tab == OffersTab.Membresias) "Nuevo plan" else "Nuevo paquete", Icons.Default.Add, expanded) {
                vm.clearMessages()
                if (tab == OffersTab.Membresias) creatingPlan = true else creatingPackage = true
            }
        },
        // Con la pestaña vacía su estado ya trae el botón («Nuevo plan» / «Nuevo paquete»): el flotante sobra.
        hideFloatingActionButton = isConfirmedEmpty(
            state.loaded, state.loading,
            if (tab == OffersTab.Membresias) state.plans.isNotEmpty() else state.packages.isNotEmpty(),
            state.listError
        )
    ) {
        item {
            UrbanPillTabs(
                listOf(OffersTab.Membresias.label to Icons.Default.CardMembership, OffersTab.Paquetes.label to Icons.Default.Inventory2),
                tab.ordinal
            ) { tab = OffersTab.entries[it] }
        }
        if (state.saving) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = UrbanColors.Gold) }
        state.notice?.let { item { UrbanInfoBanner(it, Icons.Default.CheckCircle) } }
        // Con el formulario abierto el error de guardar se muestra dentro de la hoja, no aquí.
        if (!formOpen) state.error?.let { item { UrbanErrorBanner(it) } }

        if (tab == OffersTab.Membresias) {
            urbanListFeedback(
                state.loaded, state.loading, state.plans.isNotEmpty(), state.listError,
                "No pudimos cargar los beneficios", "Aún no hay planes", "Crea un plan mensual con descuento para tus clientes frecuentes.",
                emptyActionLabel = "Nuevo plan", emptyActionIcon = Icons.Default.Add, onEmptyAction = { creatingPlan = true }
            ) { vm.load() }
            if (state.plans.isNotEmpty()) item { UrbanSectionTitle("Planes mensuales", UrbanFormat.count(state.plans.size, "plan", "planes")) }
            items(state.plans, key = { it.id }) { plan ->
                OfferCard(
                    title = plan.nombre,
                    subtitle = "${plan.descuentoPct} % de descuento en servicios",
                    price = money(plan.precioMensual) + " / mes",
                    active = plan.activo,
                    onOpen = { vm.clearMessages(); editingPlan = plan },
                    onToggle = { vm.togglePlan(plan) }
                )
            }
        } else {
            urbanListFeedback(
                state.loaded, state.loading, state.packages.isNotEmpty(), state.listError,
                "No pudimos cargar los beneficios", "Aún no hay paquetes", "Vende varios usos de un servicio por adelantado.",
                emptyActionLabel = "Nuevo paquete", emptyActionIcon = Icons.Default.Add, onEmptyAction = { creatingPackage = true }
            ) { vm.load() }
            if (state.packages.isNotEmpty()) item { UrbanSectionTitle("Paquetes de usos", UrbanFormat.count(state.packages.size, "paquete", "paquetes")) }
            items(state.packages, key = { it.id }) { pack ->
                OfferCard(
                    title = pack.nombre,
                    subtitle = "${pack.cantidadUsos} × ${pack.service.nombre ?: "servicio"}" +
                        (pack.vigenciaDias?.let { " · vigencia $it días" } ?: ""),
                    price = money(pack.precio),
                    active = pack.activo,
                    onOpen = { vm.clearMessages(); editingPackage = pack },
                    onToggle = { vm.togglePackage(pack) }
                )
            }
        }
    }

    if (creatingPlan || editingPlan != null) {
        PlanFormSheet(
            plan = editingPlan, saving = state.saving, error = state.error,
            onDismiss = { creatingPlan = false; editingPlan = null },
            onSave = { body -> vm.savePlan(editingPlan?.id, body) { creatingPlan = false; editingPlan = null } }
        )
    }
    if (creatingPackage || editingPackage != null) {
        PackageFormSheet(
            pack = editingPackage, services = state.services, saving = state.saving, error = state.error,
            onDismiss = { creatingPackage = false; editingPackage = null },
            onSave = { body -> vm.savePackage(editingPackage?.id, body) { creatingPackage = false; editingPackage = null } }
        )
    }
}

@Composable
private fun OfferCard(title: String, subtitle: String, price: String, active: Boolean, onOpen: () -> Unit, onToggle: () -> Unit) {
    UrbanCard(Modifier.fillMaxWidth(), onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    color = if (active) UrbanColors.Ink else UrbanColors.Muted
                )
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = UrbanColors.Muted)
                Text(
                    price + if (active) "" else " · Inactivo",
                    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                    color = if (active) UrbanColors.Gold else UrbanColors.Muted
                )
            }
            UrbanSwitch(
                checked = active, onCheckedChange = { onToggle() }
            )
        }
    }
}

/** Errores por campo del plan de membresía, en orden de aparición (vacío = todo bien). */
internal fun validatePlanForm(nombre: String, precio: String, descuento: String): LinkedHashMap<String, String> {
    val errors = LinkedHashMap<String, String>()
    if (nombre.isBlank()) errors["nombre"] = "Escribe el nombre del plan."
    val p = precio.replace(',', '.').toDoubleOrNull()
    if (p == null || p < 1) errors["precio"] = "El precio mínimo es 1 peso."
    val d = descuento.toIntOrNull()
    if (d == null || d !in 1..100) errors["descuento"] = "Entre 1 y 100 %."
    return errors
}

private val PLAN_FIELD_LABEL = mapOf("nombre" to "Nombre", "precio" to "Precio mensual", "descuento" to "Descuento")

/** Formulario de plan de membresía: el mismo para crear (plan null) y para editar. */
@Composable
private fun PlanFormSheet(plan: MembershipPlanItem?, saving: Boolean, error: String?, onDismiss: () -> Unit, onSave: (MembershipPlanRequest) -> Unit) {
    var nombre by remember { mutableStateOf(plan?.nombre.orEmpty()) }
    var descripcion by remember { mutableStateOf(plan?.descripcion.orEmpty()) }
    var precio by remember { mutableStateOf(plan?.let { "%.0f".format(it.precioMensual) }.orEmpty()) }
    var descuento by remember { mutableStateOf(plan?.descuentoPct?.toString().orEmpty()) }
    var activo by remember { mutableStateOf(plan?.activo ?: true) }
    val form = rememberUrbanFormState()
    val errors = validatePlanForm(nombre, precio, descuento)
    val precioValue = precio.replace(',', '.').toDoubleOrNull()
    val priceChanges = plan == null || (precioValue != null && kotlin.math.abs(precioValue - plan.precioMensual) > 0.004)

    UrbanFormSheet(
        eyebrow = "Membresías",
        title = if (plan == null) "Nuevo plan" else "Editar plan",
        primaryLabel = if (plan == null) "Crear plan" else "Guardar cambios",
        onSubmit = {
            onSave(MembershipPlanRequest(nombre.trim(), descripcion.trim().ifEmpty { null }, precio.replace(',', '.').toDouble(), descuento.toInt(), activo))
        },
        onDismiss = onDismiss,
        saving = saving,
        blocker = formBlockerText(errors.keys.map { PLAN_FIELD_LABEL.getValue(it) }),
        error = error
    ) {
        UrbanFormSection("Plan", icon = Icons.Default.CardMembership) {
            UrbanTextField(
                nombre, { nombre = it }, "Nombre", Modifier.fillMaxWidth(),
                leadingIcon = Icons.Default.CardMembership, placeholder = "Ej. Plan Barba y Corte",
                error = form.error("nombre", errors["nombre"]), onBlur = { form.touch("nombre") },
                capitalization = KeyboardCapitalization.Sentences
            )
            UrbanTextField(
                descripcion, { descripcion = it }, "Descripción (opcional)", Modifier.fillMaxWidth(),
                leadingIcon = Icons.Default.Notes, placeholder = "Qué incluye la membresía",
                capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Default, minLines = 3
            )
        }
        UrbanFormSection("Cobro", icon = Icons.Default.Payments) {
            UrbanFormRow {
                UrbanTextField(
                    precio, { precio = it.filter { c -> c.isDigit() || c == '.' || c == ',' } }, "Precio mensual (MXN)", Modifier.weight(1f),
                    leadingIcon = Icons.Default.AttachMoney, placeholder = "0.00",
                    error = form.error("precio", errors["precio"]), onBlur = { form.touch("precio") },
                    keyboardType = KeyboardType.Decimal
                )
                UrbanTextField(
                    descuento, { descuento = it.filter(Char::isDigit) }, "Descuento (%)", Modifier.weight(1f),
                    leadingIcon = Icons.Default.Percent, placeholder = "10",
                    error = form.error("descuento", errors["descuento"]), onBlur = { form.touch("descuento") },
                    keyboardType = KeyboardType.Number
                )
            }
            if (priceChanges) {
                Text(
                    "Al guardar un precio nuevo se crea un precio nuevo en Stripe; el anterior deja de usarse.",
                    style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted
                )
            }
        }
        UrbanFormSection("Disponibilidad", icon = Icons.Default.PowerSettingsNew) {
            UrbanFormSwitch(
                "Disponible para contratar", "Las membresías ya contratadas no se ven afectadas.", activo, { activo = it },
                icon = Icons.Default.PowerSettingsNew
            )
        }
    }
}

/** Errores por campo del paquete de usos, en orden de aparición (vacío = todo bien). */
internal fun validatePackageForm(nombre: String, serviceId: String, usos: String, precio: String, vigencia: String): LinkedHashMap<String, String> {
    val errors = LinkedHashMap<String, String>()
    if (nombre.isBlank()) errors["nombre"] = "Escribe el nombre del paquete."
    if (serviceId.isBlank()) errors["servicio"] = "Elige el servicio que incluye."
    val u = usos.toIntOrNull()
    if (u == null || u !in 2..100) errors["usos"] = "Entre 2 y 100 usos."
    val p = precio.replace(',', '.').toDoubleOrNull()
    if (p == null || p <= 0) errors["precio"] = "Escribe un precio válido."
    val v = if (vigencia.isBlank()) null else vigencia.toIntOrNull()
    if (vigencia.isNotBlank() && (v == null || v !in 1..730)) errors["vigencia"] = "Entre 1 y 730 días."
    return errors
}

private val PACKAGE_FIELD_LABEL = mapOf(
    "nombre" to "Nombre", "servicio" to "Servicio incluido", "usos" to "Usos", "precio" to "Precio", "vigencia" to "Vigencia"
)

/** Formulario de paquete de usos: el mismo para crear (pack null) y para editar. */
@Composable
private fun PackageFormSheet(
    pack: ServicePackageItem?, services: List<ServiceItem>, saving: Boolean, error: String?,
    onDismiss: () -> Unit, onSave: (ServicePackageRequest) -> Unit
) {
    var nombre by remember { mutableStateOf(pack?.nombre.orEmpty()) }
    var serviceId by remember { mutableStateOf(pack?.service?.id.orEmpty()) }
    var usos by remember { mutableStateOf(pack?.cantidadUsos?.toString().orEmpty()) }
    var precio by remember { mutableStateOf(pack?.let { "%.0f".format(it.precio) }.orEmpty()) }
    var vigencia by remember { mutableStateOf(pack?.vigenciaDias?.toString().orEmpty()) }
    var activo by remember { mutableStateOf(pack?.activo ?: true) }
    val form = rememberUrbanFormState()
    val errors = validatePackageForm(nombre, serviceId, usos, precio, vigencia)

    UrbanFormSheet(
        eyebrow = "Paquetes",
        title = if (pack == null) "Nuevo paquete" else "Editar paquete",
        primaryLabel = if (pack == null) "Crear paquete" else "Guardar cambios",
        onSubmit = {
            onSave(
                ServicePackageRequest(
                    nombre.trim(), serviceId, usos.toInt(), precio.replace(',', '.').toDouble(),
                    if (vigencia.isBlank()) null else vigencia.toInt(), activo
                )
            )
        },
        onDismiss = onDismiss,
        saving = saving,
        blocker = formBlockerText(errors.keys.map { PACKAGE_FIELD_LABEL.getValue(it) }),
        error = error
    ) {
        UrbanFormSection("Paquete", icon = Icons.Default.Inventory2) {
            UrbanTextField(
                nombre, { nombre = it }, "Nombre", Modifier.fillMaxWidth(),
                leadingIcon = Icons.Default.Inventory2, placeholder = "Ej. 5 cortes clásicos",
                error = form.error("nombre", errors["nombre"]), onBlur = { form.touch("nombre") },
                capitalization = KeyboardCapitalization.Sentences
            )
            if (services.isEmpty()) {
                Text("No se pudo cargar el catálogo de servicios.", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
            } else {
                UrbanFormChoice(
                    label = "Servicio incluido", options = services.map { it.id }, selected = serviceId.takeIf { it.isNotBlank() },
                    onSelect = { serviceId = it }, optionLabel = { id -> services.firstOrNull { it.id == id }?.nombre.orEmpty() },
                    icon = Icons.Default.ContentCut
                )
            }
        }
        UrbanFormSection("Cobro y vigencia", icon = Icons.Default.Payments) {
            UrbanFormRow {
                UrbanTextField(
                    usos, { usos = it.filter(Char::isDigit) }, "Usos", Modifier.weight(1f),
                    leadingIcon = Icons.Default.Tag, placeholder = "5",
                    error = form.error("usos", errors["usos"]), onBlur = { form.touch("usos") },
                    keyboardType = KeyboardType.Number
                )
                UrbanTextField(
                    precio, { precio = it.filter { c -> c.isDigit() || c == '.' || c == ',' } }, "Precio (MXN)", Modifier.weight(1f),
                    leadingIcon = Icons.Default.AttachMoney, placeholder = "0.00",
                    error = form.error("precio", errors["precio"]), onBlur = { form.touch("precio") },
                    keyboardType = KeyboardType.Decimal
                )
            }
            UrbanTextField(
                vigencia, { vigencia = it.filter(Char::isDigit) }, "Vigencia en días (opcional)", Modifier.fillMaxWidth(),
                leadingIcon = Icons.Default.CalendarMonth, placeholder = "90",
                helper = "Vacío = sin vencimiento.",
                error = form.error("vigencia", errors["vigencia"]), onBlur = { form.touch("vigencia") },
                keyboardType = KeyboardType.Number
            )
        }
        UrbanFormSection("Disponibilidad", icon = Icons.Default.PowerSettingsNew) {
            UrbanFormSwitch(
                "Disponible para venta", "Los paquetes ya vendidos siguen siendo válidos.", activo, { activo = it },
                icon = Icons.Default.PowerSettingsNew
            )
        }
    }
}
