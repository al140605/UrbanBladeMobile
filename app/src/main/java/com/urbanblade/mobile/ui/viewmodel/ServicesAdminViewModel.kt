package com.urbanblade.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.urbanblade.mobile.core.network.AppContainer
import com.urbanblade.mobile.data.model.ServiceAdminItem
import com.urbanblade.mobile.data.model.ServiceUpsertRequest
import com.urbanblade.mobile.data.repository.UrbanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

/** Filtro de estado de la lista: todos, solo activos o solo inactivos. */
enum class ServiceStatusFilter(val label: String, val param: String?) {
    Todos("Todos", null),
    Activos("Activos", "1"),
    Inactivos("Inactivos", "0")
}

data class ServicesAdminState(
    val items: List<ServiceAdminItem> = emptyList(),
    val categories: List<String> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val lastPage: Int = 1,
    val query: String = "",
    val status: ServiceStatusFilter = ServiceStatusFilter.Todos,
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val notice: String? = null
) {
    val hasMore: Boolean get() = page < lastPage
}

/** Catálogo de servicios del administrador: buscar, filtrar, crear, editar, activar y eliminar. */
class ServicesAdminViewModel @JvmOverloads constructor(
    private val repo: UrbanRepository = AppContainer.urbanRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ServicesAdminState())
    val state: StateFlow<ServicesAdminState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val s = _state.value
                val response = repo.servicesAdmin(1, s.query.trim().ifEmpty { null }, s.status.param, null)
                _state.value = _state.value.copy(
                    items = response.data,
                    categories = response.categories,
                    total = response.meta.total,
                    page = response.meta.currentPage,
                    lastPage = response.meta.lastPage,
                    loading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.toFriendlyMessage("No se pudieron cargar los servicios."))
            }
        }
    }

    fun loadMore() {
        val s = _state.value
        if (s.loadingMore || s.loading || !s.hasMore) return
        viewModelScope.launch {
            _state.value = _state.value.copy(loadingMore = true)
            try {
                val response = repo.servicesAdmin(s.page + 1, s.query.trim().ifEmpty { null }, s.status.param, null)
                val known = _state.value.items.map { it.id }.toSet()
                _state.value = _state.value.copy(
                    items = _state.value.items + response.data.filter { it.id !in known },
                    page = response.meta.currentPage,
                    lastPage = response.meta.lastPage,
                    loadingMore = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(loadingMore = false, error = e.toFriendlyMessage("No se pudieron cargar más servicios."))
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

    /** Crea (slug nulo) o edita un servicio; [onDone] solo se llama si el servidor lo aceptó. */
    fun save(slug: String?, body: ServiceUpsertRequest, onDone: () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, error = null)
            try {
                if (slug == null) repo.createService(body) else repo.updateService(slug, body)
                _state.value = _state.value.copy(saving = false, notice = if (slug == null) "Servicio creado." else "Cambios guardados.")
                onDone()
                load()
            } catch (e: HttpException) {
                _state.value = _state.value.copy(
                    saving = false,
                    error = if (e.code() == 422) e.serverMessage() ?: "Revisa los datos del servicio." else e.toFriendlyMessage("No se pudo guardar el servicio.")
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(saving = false, error = e.toFriendlyMessage("No se pudo guardar el servicio."))
            }
        }
    }

    /** Activa o desactiva un servicio sin tocar el resto de sus datos. */
    fun toggleActive(item: ServiceAdminItem) {
        val body = ServiceUpsertRequest(
            item.nombre, item.categoria, item.precio, item.duracionMin, item.descripcion, !item.activo
        )
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, error = null)
            try {
                repo.updateService(item.slug, body)
                _state.value = _state.value.copy(
                    saving = false,
                    notice = if (item.activo) "\"${item.nombre}\" ya no se ofrece." else "\"${item.nombre}\" vuelve a estar disponible."
                )
                load()
            } catch (e: Exception) {
                _state.value = _state.value.copy(saving = false, error = e.toFriendlyMessage("No se pudo cambiar el estado del servicio."))
            }
        }
    }

    fun delete(item: ServiceAdminItem) {
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, error = null)
            try {
                repo.deleteService(item.slug)
                _state.value = _state.value.copy(saving = false, notice = "\"${item.nombre}\" eliminado.")
                load()
            } catch (e: Exception) {
                _state.value = _state.value.copy(saving = false, error = e.toFriendlyMessage("No se pudo eliminar el servicio."))
            }
        }
    }
}
