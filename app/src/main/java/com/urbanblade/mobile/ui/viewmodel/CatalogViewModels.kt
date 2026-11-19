package com.urbanblade.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.urbanblade.mobile.core.network.AppContainer
import com.urbanblade.mobile.data.model.*
import com.urbanblade.mobile.data.repository.UrbanRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException


class CatalogViewModel @JvmOverloads constructor(
    private val repo: UrbanRepository = AppContainer.urbanRepository
) : ViewModel() {
    private val _services = MutableStateFlow<List<ServiceItem>>(emptyList())
    val services = _services.asStateFlow()
    private val _barbers = MutableStateFlow<List<BarberItem>>(emptyList())
    val barbers = _barbers.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _favoriteBusy = MutableStateFlow(false)
    val favoriteBusy = _favoriteBusy.asStateFlow()

    fun favorite(barber: BarberItem) {
        if (_favoriteBusy.value) return
        _favoriteBusy.value = true
        viewModelScope.launch {
            _error.value = null
            try {
                val saved = repo.favoriteBarber(if (barber.esFavorito) null else barber.id)
                _barbers.value = _barbers.value.map { it.copy(esFavorito = it.id == saved.barberId) }
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { _error.value = e.toFriendlyMessage("No pudimos guardar tu barbero favorito.") }
            finally { _favoriteBusy.value = false }
        }
    }

    fun load() = viewModelScope.launch {
        _loading.value = true; _error.value = null
        try {
            _services.value = repo.services()
            _barbers.value = repo.barbers()
        } catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo cargar el catálogo.") }
        finally { _loading.value = false }
    }
}

/** Ficha del barbero para el cliente: datos, portafolio, reseñas y calificar por servicio. */
class BarberProfileViewModel : ViewModel() {
    private val repo = AppContainer.urbanRepository
    private val _profile = MutableStateFlow<BarberProfileResponse?>(null)
    val profile = _profile.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    private val _sending = MutableStateFlow(false)
    val sending = _sending.asStateFlow()
    private val _reviewError = MutableStateFlow<String?>(null)
    val reviewError = _reviewError.asStateFlow()
    private val _reviewSent = MutableStateFlow<String?>(null)
    val reviewSent = _reviewSent.asStateFlow()

    fun load(slug: String) = viewModelScope.launch {
        _loading.value = true; _error.value = null
        try { _profile.value = repo.barberProfile(slug) }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo cargar el perfil del barbero.") }
        finally { _loading.value = false }
    }

    fun review(slug: String, rating: Int, comment: String, serviceId: String?) = viewModelScope.launch {
        _sending.value = true; _reviewError.value = null
        try {
            val res = repo.reviewBarber(slug, BarberReviewRequest(rating, comment.trim().ifBlank { null }, serviceId))
            _reviewSent.value = res.message ?: "¡Gracias por tu reseña!"
            com.urbanblade.mobile.core.analytics.UrbanAnalytics.log("resena_enviada", "calificacion" to rating, "con_servicio" to (serviceId != null), "con_comentario" to comment.isNotBlank())
            _profile.value = repo.barberProfile(slug)
        } catch (e: Exception) {
            // 422 trae el motivo real (ya la reseñaste, servicio no recibido...).
            _reviewError.value = (e as? HttpException)?.takeIf { it.code() == 422 }?.serverMessage()
                ?: e.toFriendlyMessage("No se pudo enviar tu reseña.")
        } finally { _sending.value = false }
    }
}
