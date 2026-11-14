package com.urbanblade.mobile.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.urbanblade.mobile.core.media.UploadRejectedException
import com.urbanblade.mobile.core.network.AppContainer
import com.urbanblade.mobile.core.validation.FormLimits
import com.urbanblade.mobile.data.model.BarberMeResponse
import com.urbanblade.mobile.data.repository.UrbanRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Mensaje si bio o especialidades pasan el tope que el servidor aplica; null si todo cabe. */
internal fun validateBarberProfile(especialidades: String, descripcion: String): String? = when {
    especialidades.trim().length > FormLimits.SPECIALTIES_MAX ->
        "Las especialidades pueden tener hasta ${FormLimits.SPECIALTIES_MAX} caracteres."
    descripcion.trim().length > FormLimits.DESCRIPTION_MAX ->
        "La descripción puede tener hasta ${FormLimits.DESCRIPTION_MAX} caracteres."
    else -> null
}

data class BarberMeState(
    val profile: BarberMeResponse? = null,
    /** True cuando la primera consulta terminó bien. */
    val loaded: Boolean = false,
    val loading: Boolean = false,
    /** Error de cargar el perfil; el de guardar va aparte en [saveError]. */
    val loadError: String? = null,
    val saving: Boolean = false,
    val saveError: String? = null,
    val notice: String? = null
)

/** «Mi perfil» del barbero: ver su calificación y actividad, y editar especialidades, descripción y foto. */
class BarberMeViewModel @JvmOverloads constructor(
    private val repo: UrbanRepository = AppContainer.urbanRepository
) : ViewModel() {
    private val _state = MutableStateFlow(BarberMeState())
    val state: StateFlow<BarberMeState> = _state.asStateFlow()

    private var loadRequest: Job? = null

    fun load() {
        loadRequest?.cancel()
        _state.value = _state.value.copy(loading = true, loadError = null)
        loadRequest = viewModelScope.launch {
            try {
                val profile = repo.barberMe()
                coroutineContext.ensureActive()
                _state.value = _state.value.copy(profile = profile, loaded = true, loading = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                coroutineContext.ensureActive()
                _state.value = _state.value.copy(loading = false, loadError = e.toFriendlyMessage("No se pudo cargar tu perfil."))
            }
        }
    }

    fun save(context: Context, especialidades: String, descripcion: String, photo: Uri?) {
        if (_state.value.saving) return
        validateBarberProfile(especialidades, descripcion)?.let {
            _state.value = _state.value.copy(saveError = it, notice = null)
            return
        }
        _state.value = _state.value.copy(saving = true, saveError = null, notice = null)
        viewModelScope.launch {
            try {
                val res = repo.updateBarberProfile(context, especialidades, descripcion, photo)
                val current = _state.value.profile
                _state.value = _state.value.copy(
                    profile = current?.copy(
                        especialidades = res.especialidades ?: especialidades.trim(),
                        descripcion = res.descripcion ?: descripcion.trim(),
                        fotoUrl = res.fotoUrl ?: current.fotoUrl
                    ),
                    saving = false,
                    notice = res.message ?: "Perfil actualizado."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val text = (e as? UploadRejectedException)?.message
                    ?: e.toFriendlyMessage("No se pudo guardar tu perfil. Inténtalo de nuevo.")
                _state.value = _state.value.copy(saving = false, saveError = text)
            }
        }
    }

    fun clearMessages() {
        _state.value = _state.value.copy(saveError = null, notice = null)
    }
}
