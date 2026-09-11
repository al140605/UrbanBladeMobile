package com.urbanblade.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.urbanblade.mobile.core.network.AppContainer
import com.urbanblade.mobile.data.model.AuthUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

sealed interface AuthState {
    data object Loading : AuthState
    data object Guest : AuthState
    data class Authenticated(val user: AuthUser) : AuthState
}

class AuthViewModel : ViewModel() {
    private val repository = AppContainer.authRepository

    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init { bootstrap() }

    fun bootstrap() {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            val user = repository.restoreSession()
            _state.value = user?.let { AuthState.Authenticated(it) } ?: AuthState.Guest
        }
    }

    fun login(email: String, password: String) {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            _error.value = null
            try {
                val user = repository.login(email.trim(), password)
                _state.value = AuthState.Authenticated(user)
            } catch (e: Exception) {
                _error.value = e.toFriendlyMessage("No se pudo iniciar sesión.")
            } finally { _busy.value = false }
        }
    }

    fun register(name: String, email: String, password: String, confirmation: String) {
        if (_busy.value) return
        if (password != confirmation) {
            _error.value = "Las contraseñas no coinciden."
            return
        }
        viewModelScope.launch {
            _busy.value = true
            _error.value = null
            try {
                val user = repository.register(name.trim(), email.trim(), password)
                _state.value = AuthState.Authenticated(user)
            } catch (e: Exception) {
                _error.value = e.toFriendlyMessage("No se pudo crear la cuenta.")
            } finally { _busy.value = false }
        }
    }

    fun forgotPassword(email: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            _error.value = null
            try { onResult(repository.forgotPassword(email.trim())) }
            catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo enviar el correo.") }
            finally { _busy.value = false }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _state.value = AuthState.Guest
        }
    }
}

internal fun Exception.toFriendlyMessage(fallback: String): String {
    return when (this) {
        is HttpException -> when (code()) {
            401 -> "Credenciales incorrectas o sesión vencida."
            422 -> "Revisa los datos capturados."
            429 -> "Demasiados intentos. Intenta de nuevo más tarde."
            503 -> "UrbanBlade está en mantenimiento."
            else -> fallback
        }
        else -> message?.takeIf { it.isNotBlank() } ?: fallback
    }
}
