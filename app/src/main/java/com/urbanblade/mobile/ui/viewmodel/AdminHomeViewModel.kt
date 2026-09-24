package com.urbanblade.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import com.urbanblade.mobile.core.network.AppContainer
import com.urbanblade.mobile.data.repository.UrbanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminHomeState(
    val kpis: JsonObject? = null,
    val pendingPayments: Int = 0,
    val occupancyRate: Int? = null,
    val loading: Boolean = false,
    val error: String? = null
)

/** Datos del inicio del administrador: indicadores del negocio y pagos por verificar. */
class AdminHomeViewModel @JvmOverloads constructor(
    private val repo: UrbanRepository = AppContainer.urbanRepository
) : ViewModel() {
    private val _state = MutableStateFlow(AdminHomeState())
    val state: StateFlow<AdminHomeState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val dashboard = repo.dashboard()
                val kpis = dashboard.data.getAsJsonObject("kpis") ?: dashboard.data
                // Si falla la consulta de pagos por verificar, el resto del resumen se muestra igual.
                val pending = runCatching { repo.pendingPayments().data.size }.getOrDefault(0)
                val occupancy = runCatching {
                    repo.adminStats()
                        .getAsJsonObject("stats")
                        ?.get("occupancyRate")
                        ?.asInt
                }.getOrNull()
                _state.value = AdminHomeState(
                    kpis = kpis,
                    pendingPayments = pending,
                    occupancyRate = occupancy
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = e.toFriendlyMessage("No se pudo cargar el resumen del negocio.")
                )
            }
        }
    }
}
