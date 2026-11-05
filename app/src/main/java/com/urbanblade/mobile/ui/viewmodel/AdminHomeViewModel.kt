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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job

data class AdminHomeState(
    val kpis: JsonObject? = null,
    /** `data` completo de GET /dashboard (series de las gráficas). */
    val data: JsonObject? = null,
    val pendingPayments: Int? = null,
    val pendingPaymentsError: String? = null,
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
    private var loadRequest: Job? = null

    fun load() {
        if (loadRequest?.isActive == true) return
        _state.value = _state.value.copy(loading = true, error = null)
        loadRequest = viewModelScope.launch {
            try {
                val dashboard = repo.dashboard()
                val kpis = dashboard.data.getAsJsonObject("kpis") ?: dashboard.data
                // Si falla la consulta de pagos por verificar, el resto del resumen se muestra igual.
                var pendingError: String? = null
                val pending = try { repo.pendingPayments().data.size }
                catch (e: CancellationException) { throw e }
                catch (_: Exception) {
                    pendingError = "No pudimos consultar los pagos por verificar. Abre Cobros para revisarlos."
                    null
                }
                val occupancy = try {
                    repo.adminStats()
                        .getAsJsonObject("stats")
                        ?.get("occupancyRate")
                        ?.asInt
                } catch (e: CancellationException) { throw e }
                catch (_: Exception) { null }
                _state.value = AdminHomeState(
                    kpis = kpis,
                    data = dashboard.data,
                    pendingPayments = pending,
                    pendingPaymentsError = pendingError,
                    occupancyRate = occupancy
                )
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = e.toFriendlyMessage("No se pudo cargar el resumen del negocio.")
                )
            }
        }
    }
}
