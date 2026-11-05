package com.urbanblade.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.urbanblade.mobile.core.network.AppContainer
import com.urbanblade.mobile.data.model.BarberAdminItem
import com.urbanblade.mobile.data.model.BarberPerformance
import com.urbanblade.mobile.data.model.BarberUpsertRequest
import com.urbanblade.mobile.data.repository.UrbanRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class BarbersAdminState(
    val items: List<BarberAdminItem> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val lastPage: Int = 1,
    val query: String = "",
    val status: ServiceStatusFilter = ServiceStatusFilter.Todos,
    /** True cuando la primera consulta terminó bien; hasta entonces un vacío no está confirmado. */
    val loaded: Boolean = false,
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val saving: Boolean = false,
    /** Error de guardar, activar, editar o eliminar; la consulta de la lista usa [listError]. */
    val error: String? = null,
    /** Error de la consulta de la lista: no se mezcla con los de las acciones. */
    val listError: String? = null,
    val notice: String? = null,
    val performance: BarberPerformance? = null,
    val performanceLoading: Boolean = false
) {
    val hasMore: Boolean get() = page < lastPage
}

/** Equipo de barberos del administrador: buscar, activar, editar perfil y comisión, ver rendimiento. */
class BarbersAdminViewModel @JvmOverloads constructor(
    private val repo: UrbanRepository = AppContainer.urbanRepository
) : ViewModel() {
    private val _state = MutableStateFlow(BarbersAdminState())
    val state: StateFlow<BarbersAdminState> = _state.asStateFlow()

    private var loadRequest: Job? = null
    private var moreRequest: Job? = null

    /** Cada consulta cancela la anterior (y la paginación en curso): una respuesta tardía no pisa la última. */
    fun load() {
        loadRequest?.cancel()
        moreRequest?.cancel()
        _state.value = _state.value.copy(loading = true, loadingMore = false, error = null, listError = null)
        loadRequest = viewModelScope.launch {
            try {
                val s = _state.value
                val response = repo.barbersAdmin(1, s.query.trim().ifEmpty { null }, s.status.param)
                coroutineContext.ensureActive()
                _state.value = _state.value.copy(
                    items = response.data,
                    total = response.meta.total,
                    page = response.meta.currentPage,
                    lastPage = response.meta.lastPage,
                    loaded = true,
                    loading = false
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                coroutineContext.ensureActive()
                _state.value = _state.value.copy(loading = false, listError = e.toFriendlyMessage("No se pudo cargar el equipo."))
            }
        }
    }

    fun loadMore() {
        val s = _state.value
        if (s.loadingMore || s.loading || !s.hasMore) return
        moreRequest = viewModelScope.launch {
            _state.value = _state.value.copy(loadingMore = true, listError = null)
            try {
                val response = repo.barbersAdmin(s.page + 1, s.query.trim().ifEmpty { null }, s.status.param)
                coroutineContext.ensureActive()
                val known = _state.value.items.map { it.id }.toSet()
                _state.value = _state.value.copy(
                    items = _state.value.items + response.data.filter { it.id !in known },
                    page = response.meta.currentPage,
                    lastPage = response.meta.lastPage,
                    loadingMore = false
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                coroutineContext.ensureActive()
                _state.value = _state.value.copy(loadingMore = false, listError = e.toFriendlyMessage("No se pudieron cargar más barberos."))
            }
        }
    }

    fun setQuery(value: String) {
        _state.value = _state.value.copy(query = value)
    }

    fun setStatus(filter: ServiceStatusFilter) {
        if (filter == _state.value.status) return
        _state.value = _state.value.copy(status = filter)
        load()
    }

    fun clearMessages() {
        _state.value = _state.value.copy(error = null, notice = null)
    }

    /** Estadísticas de un barbero para la hoja de detalle; un fallo aquí no bloquea la edición. */
    fun loadPerformance(barber: BarberAdminItem) {
        viewModelScope.launch {
            _state.value = _state.value.copy(performance = null, performanceLoading = true)
            val result = runCatching { repo.barberPerformance(barber.slug) }.getOrNull()
            _state.value = _state.value.copy(performance = result, performanceLoading = false)
        }
    }

    fun save(barber: BarberAdminItem, body: BarberUpsertRequest, onDone: () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, error = null)
            try {
                repo.updateBarber(barber.slug, body)
                _state.value = _state.value.copy(saving = false, notice = "Cambios guardados.")
                onDone()
                load()
            } catch (e: HttpException) {
                _state.value = _state.value.copy(
                    saving = false,
                    error = if (e.code() == 422) e.serverMessage() ?: "Revisa los datos del barbero." else e.toFriendlyMessage("No se pudo guardar el barbero.")
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(saving = false, error = e.toFriendlyMessage("No se pudo guardar el barbero."))
            }
        }
    }

    /** Activa o desactiva a un barbero conservando el resto de su perfil tal cual. */
    fun toggleActive(barber: BarberAdminItem) {
        val body = BarberUpsertRequest(
            barber.user.name, barber.user.email, barber.especialidades, barber.descripcion,
            barber.foto, !barber.activo, barber.comisionPct
        )
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, error = null)
            try {
                repo.updateBarber(barber.slug, body)
                _state.value = _state.value.copy(
                    saving = false,
                    notice = if (barber.activo) "${barber.user.name} ya no recibe citas." else "${barber.user.name} vuelve a recibir citas."
                )
                load()
            } catch (e: Exception) {
                _state.value = _state.value.copy(saving = false, error = e.toFriendlyMessage("No se pudo cambiar el estado del barbero."))
            }
        }
    }
}
