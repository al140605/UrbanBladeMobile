package com.urbanblade.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.urbanblade.mobile.core.network.AppContainer
import com.urbanblade.mobile.data.model.BarbershopSetting
import com.urbanblade.mobile.data.model.UpdateSettingRequest
import com.urbanblade.mobile.data.repository.UrbanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class SettingsState(
    val setting: BarbershopSetting = BarbershopSetting(),
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val notice: String? = null
)

/** Configuración del negocio (solo administrador): datos, horario, políticas, datos bancarios y mantenimiento. */
class SettingsViewModel @JvmOverloads constructor(
    private val repo: UrbanRepository = AppContainer.urbanRepository
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                _state.value = _state.value.copy(setting = repo.settings(), loading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.toFriendlyMessage("No se pudo cargar la configuración."))
            }
        }
    }

    fun clearMessages() {
        _state.value = _state.value.copy(error = null, notice = null)
    }

    fun save(body: UpdateSettingRequest) {
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, error = null, notice = null)
            try {
                _state.value = _state.value.copy(setting = repo.updateSettings(body), saving = false, notice = "Configuración actualizada.")
            } catch (e: HttpException) {
                // 422: horario incoherente, campo demasiado largo… el servidor dice cuál.
                _state.value = _state.value.copy(
                    saving = false,
                    error = if (e.code() == 422) e.serverMessage() ?: "Revisa los datos de la configuración." else e.toFriendlyMessage("No se pudo guardar la configuración.")
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(saving = false, error = e.toFriendlyMessage("No se pudo guardar la configuración."))
            }
        }
    }

    fun toggleMaintenance() {
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, error = null, notice = null)
            try {
                val response = repo.toggleMaintenance()
                _state.value = _state.value.copy(
                    setting = _state.value.setting.copy(maintenanceMode = response.data.maintenanceMode),
                    saving = false,
                    notice = response.message ?: "Modo mantenimiento actualizado."
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(saving = false, error = e.toFriendlyMessage("No se pudo cambiar el modo mantenimiento."))
            }
        }
    }
}
