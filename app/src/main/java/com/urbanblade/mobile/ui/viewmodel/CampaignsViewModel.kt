package com.urbanblade.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.urbanblade.mobile.core.network.AppContainer
import com.urbanblade.mobile.data.model.CampaignsResponse
import com.urbanblade.mobile.data.model.CreateCampaignRequest
import com.urbanblade.mobile.data.repository.UrbanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class CampaignsState(
    val data: CampaignsResponse = CampaignsResponse(),
    val loading: Boolean = false,
    val sending: Boolean = false,
    val error: String? = null,
    val notice: String? = null
)

/** Campañas de marketing por segmento (solo administrador): historial y envío inmediato o programado. */
class CampaignsViewModel @JvmOverloads constructor(
    private val repo: UrbanRepository = AppContainer.urbanRepository
) : ViewModel() {
    private val _state = MutableStateFlow(CampaignsState())
    val state: StateFlow<CampaignsState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                _state.value = _state.value.copy(data = repo.campaigns(), loading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.toFriendlyMessage("No se pudieron cargar las campañas."))
            }
        }
    }

    fun clearMessages() {
        _state.value = _state.value.copy(error = null, notice = null)
    }

    /** Envía o programa la campaña; [onDone] solo se llama si el servidor la aceptó. */
    fun create(body: CreateCampaignRequest, onDone: () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(sending = true, error = null, notice = null)
            try {
                val response = repo.createCampaign(body)
                _state.value = _state.value.copy(sending = false, notice = response.message ?: "Campaña registrada.")
                onDone()
                load()
            } catch (e: HttpException) {
                // 422: segmento sin clientes, fecha pasada, enlace inválido… el servidor explica cuál.
                _state.value = _state.value.copy(
                    sending = false,
                    error = if (e.code() == 422) e.serverMessage() ?: "Revisa los datos de la campaña." else e.toFriendlyMessage("No se pudo crear la campaña.")
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(sending = false, error = e.toFriendlyMessage("No se pudo crear la campaña."))
            }
        }
    }
}
