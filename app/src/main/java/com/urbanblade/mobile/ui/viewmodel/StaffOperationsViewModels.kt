package com.urbanblade.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.urbanblade.mobile.core.network.AppContainer
import com.urbanblade.mobile.data.model.InventoryMovementRow
import com.urbanblade.mobile.data.model.InventoryMovementStats
import com.urbanblade.mobile.data.model.WaitlistEntry
import com.urbanblade.mobile.data.repository.UrbanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Filtro por tipo de movimiento del historial de inventario. */
enum class MovementFilter(val label: String, val param: String?) {
    Todos("Todos", null),
    Entradas("Entradas", "entrada"),
    Salidas("Salidas", "salida")
}

data class InventoryMovementsState(
    val items: List<InventoryMovementRow> = emptyList(),
    val stats: InventoryMovementStats = InventoryMovementStats(),
    val total: Int = 0,
    val page: Int = 1,
    val lastPage: Int = 1,
    val query: String = "",
    val filter: MovementFilter = MovementFilter.Todos,
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null
) {
    val hasMore: Boolean get() = page < lastPage
}

/** Historial de entradas y salidas de stock, con búsqueda por producto o motivo y paginación. */
class InventoryMovementsViewModel @JvmOverloads constructor(
    private val repo: UrbanRepository = AppContainer.urbanRepository
) : ViewModel() {
    private val _state = MutableStateFlow(InventoryMovementsState())
    val state: StateFlow<InventoryMovementsState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val s = _state.value
                val response = repo.inventoryMovementsPage(1, s.filter.param, s.query.trim().ifEmpty { null })
                _state.value = _state.value.copy(
                    items = response.data,
                    stats = response.meta.stats,
                    total = response.meta.total,
                    page = response.meta.currentPage,
                    lastPage = response.meta.lastPage,
                    loading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.toFriendlyMessage("No se pudo cargar el historial."))
            }
        }
    }

    fun loadMore() {
        val s = _state.value
        if (s.loadingMore || s.loading || !s.hasMore) return
        viewModelScope.launch {
            _state.value = _state.value.copy(loadingMore = true)
            try {
                val response = repo.inventoryMovementsPage(s.page + 1, s.filter.param, s.query.trim().ifEmpty { null })
                val known = _state.value.items.map { it.id }.toSet()
                _state.value = _state.value.copy(
                    items = _state.value.items + response.data.filter { it.id !in known },
                    page = response.meta.currentPage,
                    lastPage = response.meta.lastPage,
                    loadingMore = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(loadingMore = false, error = e.toFriendlyMessage("No se pudieron cargar más movimientos."))
            }
        }
    }

    fun setQuery(value: String) {
        _state.value = _state.value.copy(query = value)
    }

    fun setFilter(filter: MovementFilter) {
        if (filter == _state.value.filter) return
        _state.value = _state.value.copy(filter = filter)
        load()
    }
}

/** Estados de la lista de espera tal como los guarda el servidor. */
enum class WaitlistFilter(val label: String, val param: String?) {
    Todas("Todas", null),
    Espera("En espera", "activo"),
    Notificadas("Avisadas", "notificado"),
    Reservadas("Reservadas", "reservado"),
    Canceladas("Canceladas", "cancelado")
}

data class WaitlistStaffState(
    val items: List<WaitlistEntry> = emptyList(),
    val filter: WaitlistFilter = WaitlistFilter.Espera,
    val loading: Boolean = false,
    val error: String? = null
)

/** Lista de espera de todo el negocio (la ve el personal); por defecto solo las que siguen esperando. */
class WaitlistStaffViewModel @JvmOverloads constructor(
    private val repo: UrbanRepository = AppContainer.urbanRepository
) : ViewModel() {
    private val _state = MutableStateFlow(WaitlistStaffState())
    val state: StateFlow<WaitlistStaffState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val items = repo.waitlistForStaff(_state.value.filter.param)
                _state.value = _state.value.copy(items = items, loading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.toFriendlyMessage("No se pudo cargar la lista de espera."))
            }
        }
    }

    fun setFilter(filter: WaitlistFilter) {
        if (filter == _state.value.filter) return
        _state.value = _state.value.copy(filter = filter)
        load()
    }
}
