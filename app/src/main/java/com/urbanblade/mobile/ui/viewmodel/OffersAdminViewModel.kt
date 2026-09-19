package com.urbanblade.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.urbanblade.mobile.core.network.AppContainer
import com.urbanblade.mobile.data.model.MembershipPlanItem
import com.urbanblade.mobile.data.model.MembershipPlanRequest
import com.urbanblade.mobile.data.model.ServiceItem
import com.urbanblade.mobile.data.model.ServicePackageItem
import com.urbanblade.mobile.data.model.ServicePackageRequest
import com.urbanblade.mobile.data.repository.UrbanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class OffersAdminState(
    val plans: List<MembershipPlanItem> = emptyList(),
    val packages: List<ServicePackageItem> = emptyList(),
    /** Servicios del catálogo, para elegir a cuál aplica un paquete. */
    val services: List<ServiceItem> = emptyList(),
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val notice: String? = null
)

/** Membresías mensuales y paquetes prepagados del administrador. Eliminar solo desactiva. */
class OffersAdminViewModel @JvmOverloads constructor(
    private val repo: UrbanRepository = AppContainer.urbanRepository
) : ViewModel() {
    private val _state = MutableStateFlow(OffersAdminState())
    val state: StateFlow<OffersAdminState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val plans = repo.adminMembershipPlans()
                val packages = repo.servicePackages()
                // El selector de servicios es secundario: si falla, la lista se muestra igual.
                val services = runCatching { repo.services() }.getOrDefault(_state.value.services)
                _state.value = _state.value.copy(plans = plans, packages = packages, services = services, loading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.toFriendlyMessage("No se pudieron cargar los planes y paquetes."))
            }
        }
    }

    fun clearMessages() {
        _state.value = _state.value.copy(error = null, notice = null)
    }

    fun savePlan(id: String?, body: MembershipPlanRequest, onDone: () -> Unit) = execute(
        failure = "No se pudo guardar el plan.",
        success = if (id == null) "Plan creado." else "Cambios guardados.",
        onDone = onDone
    ) { if (id == null) repo.createMembershipPlan(body) else repo.updateMembershipPlan(id, body) }

    /** Activa o desactiva un plan sin cambiar su precio, así que no genera un Price nuevo en Stripe. */
    fun togglePlan(plan: MembershipPlanItem) = execute(
        failure = "No se pudo cambiar el estado del plan.",
        success = if (plan.activo) "\"${plan.nombre}\" desactivado. Las membresías ya contratadas no cambian." else "\"${plan.nombre}\" activado."
    ) {
        repo.updateMembershipPlan(
            plan.id,
            MembershipPlanRequest(plan.nombre, plan.descripcion, plan.precioMensual, plan.descuentoPct, !plan.activo)
        )
    }

    fun savePackage(id: String?, body: ServicePackageRequest, onDone: () -> Unit) = execute(
        failure = "No se pudo guardar el paquete.",
        success = if (id == null) "Paquete creado." else "Cambios guardados.",
        onDone = onDone
    ) { if (id == null) repo.createServicePackage(body) else repo.updateServicePackage(id, body) }

    fun togglePackage(item: ServicePackageItem) = execute(
        failure = "No se pudo cambiar el estado del paquete.",
        success = if (item.activo) "\"${item.nombre}\" desactivado. Los ya vendidos siguen siendo válidos." else "\"${item.nombre}\" activado."
    ) {
        repo.updateServicePackage(
            item.id,
            ServicePackageRequest(item.nombre, item.service.id.orEmpty(), item.cantidadUsos, item.precio, item.vigenciaDias, !item.activo)
        )
    }

    private fun execute(failure: String, success: String, onDone: () -> Unit = {}, call: suspend () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, error = null)
            try {
                call()
                _state.value = _state.value.copy(saving = false, notice = success)
                onDone()
                load()
            } catch (e: HttpException) {
                _state.value = _state.value.copy(
                    saving = false,
                    error = if (e.code() == 422) e.serverMessage() ?: failure else e.toFriendlyMessage(failure)
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(saving = false, error = e.toFriendlyMessage(failure))
            }
        }
    }
}
