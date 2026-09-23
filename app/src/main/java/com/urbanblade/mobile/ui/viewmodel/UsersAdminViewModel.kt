package com.urbanblade.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.urbanblade.mobile.core.network.AppContainer
import com.urbanblade.mobile.data.model.SystemUserRow
import com.urbanblade.mobile.data.repository.UrbanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

/** Mínimo de caracteres que exige el servidor para una contraseña. */
const val PASSWORD_MIN = 8

/**
 * Problema de una contraseña escrita en el formulario, o nulo si está bien. En alta es obligatoria; al
 * editar puede quedar vacía (no se cambia) y, si se escribe, debe cumplir el mínimo y coincidir.
 */
fun passwordProblem(password: String, confirmation: String, required: Boolean): String? = when {
    password.isEmpty() && !required -> null
    password.length < PASSWORD_MIN -> "Mínimo $PASSWORD_MIN caracteres."
    password != confirmation -> "Las contraseñas no coinciden."
    else -> null
}

data class UsersAdminState(
    val items: List<SystemUserRow> = emptyList(),
    val roles: List<String> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val lastPage: Int = 1,
    val query: String = "",
    val roleFilter: String? = null,
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val notice: String? = null
) {
    val hasMore: Boolean get() = page < lastPage
}

/** Cuentas de acceso al sistema (solo administrador): buscar, filtrar por rol, crear, editar y eliminar. */
class UsersAdminViewModel @JvmOverloads constructor(
    private val repo: UrbanRepository = AppContainer.urbanRepository
) : ViewModel() {
    private val _state = MutableStateFlow(UsersAdminState())
    val state: StateFlow<UsersAdminState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val s = _state.value
                val response = repo.systemUsers(s.query.trim().ifEmpty { null }, s.roleFilter, 1)
                _state.value = _state.value.copy(
                    items = response.data,
                    roles = response.roles.ifEmpty { _state.value.roles },
                    total = response.meta?.total ?: response.data.size,
                    page = response.meta?.currentPage ?: 1,
                    lastPage = response.meta?.lastPage ?: 1,
                    loading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.toFriendlyMessage("No se pudieron cargar los usuarios."))
            }
        }
    }

    fun loadMore() {
        val s = _state.value
        if (s.loadingMore || s.loading || !s.hasMore) return
        viewModelScope.launch {
            _state.value = _state.value.copy(loadingMore = true)
            try {
                val response = repo.systemUsers(s.query.trim().ifEmpty { null }, s.roleFilter, s.page + 1)
                val known = _state.value.items.map { it.id }.toSet()
                _state.value = _state.value.copy(
                    items = _state.value.items + response.data.filter { it.id !in known },
                    page = response.meta?.currentPage ?: (s.page + 1),
                    lastPage = response.meta?.lastPage ?: s.lastPage,
                    loadingMore = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(loadingMore = false, error = e.toFriendlyMessage("No se pudieron cargar más usuarios."))
            }
        }
    }

    fun setQuery(value: String) {
        _state.value = _state.value.copy(query = value)
    }

    fun setRoleFilter(role: String?) {
        if (role == _state.value.roleFilter) return
        _state.value = _state.value.copy(roleFilter = role)
        load()
    }

    fun clearMessages() {
        _state.value = _state.value.copy(error = null, notice = null)
    }

    fun create(name: String, email: String, password: String, role: String, onDone: () -> Unit) =
        mutate("No se pudo crear el usuario.", "Usuario creado.", onDone) { repo.createUser(name, email, password, role) }

    fun update(id: String, name: String, email: String, password: String?, role: String, onDone: () -> Unit) =
        mutate("No se pudo actualizar el usuario.", "Usuario actualizado.", onDone) { repo.updateUser(id, name, email, password, role) }

    fun delete(id: String) = mutate("No se pudo eliminar el usuario.", "Usuario eliminado.") { repo.deleteUser(id) }

    /** Ejecuta un cambio; ante un rechazo del servidor (correo repetido, cuenta propia…) muestra su mensaje. */
    private fun mutate(failure: String, success: String, onDone: () -> Unit = {}, call: suspend () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, error = null, notice = null)
            try {
                call()
                _state.value = _state.value.copy(saving = false, notice = success)
                onDone()
                load()
            } catch (e: HttpException) {
                _state.value = _state.value.copy(
                    saving = false,
                    error = if (e.code() in 400..499 && e.code() != 401) e.serverMessage() ?: failure else e.toFriendlyMessage(failure)
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(saving = false, error = e.toFriendlyMessage(failure))
            }
        }
    }
}
