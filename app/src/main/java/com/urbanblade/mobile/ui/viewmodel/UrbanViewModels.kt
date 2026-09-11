package com.urbanblade.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.urbanblade.mobile.core.network.AppContainer
import com.urbanblade.mobile.data.model.*
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class DashboardViewModel : ViewModel() {
    private val repo = AppContainer.urbanRepository
    private val _data = MutableStateFlow<DashboardResponse?>(null)
    val data = _data.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun load() = viewModelScope.launch {
        _loading.value = true; _error.value = null
        try { _data.value = repo.dashboard() }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo cargar el dashboard.") }
        finally { _loading.value = false }
    }
}

class AppointmentsViewModel : ViewModel() {
    private val repo = AppContainer.urbanRepository
    private val _data = MutableStateFlow(AppointmentsResponse())
    val data = _data.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun load() = viewModelScope.launch {
        _loading.value = true; _error.value = null
        try { _data.value = repo.appointments() }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudieron cargar las citas.") }
        finally { _loading.value = false }
    }

    fun cancel(item: AppointmentRow) = viewModelScope.launch {
        val code = item.code ?: return@launch
        try { repo.cancelAppointment(code); load() }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo cancelar la cita.") }
    }
}

class BookingViewModel : ViewModel() {
    private val repo = AppContainer.urbanRepository

    private val _services = MutableStateFlow<List<ServiceItem>>(emptyList())
    val services = _services.asStateFlow()
    private val _barbers = MutableStateFlow<List<BarberItem>>(emptyList())
    val barbers = _barbers.asStateFlow()
    private val _slots = MutableStateFlow<List<SlotItem>>(emptyList())
    val slots = _slots.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun loadCatalog() = viewModelScope.launch {
        try {
            _services.value = repo.services()
            _barbers.value = repo.barbers()
        } catch (e: Exception) {
            _error.value = e.toFriendlyMessage("No se pudo cargar el catálogo.")
        }
    }

    fun loadSlots(barberId: String, serviceId: String, date: String) = viewModelScope.launch {
        if (barberId.isBlank() || serviceId.isBlank() || date.isBlank()) return@launch
        try { _slots.value = repo.slots(barberId, serviceId, date) }
        catch (_: Exception) { _slots.value = emptyList() }
    }

    fun create(barberId: String, serviceId: String, date: String, time: String, notes: String, onDone: () -> Unit) {
        if (barberId.isBlank() || serviceId.isBlank() || date.isBlank() || time.isBlank()) {
            _error.value = "Selecciona barbero, servicio, fecha y horario."
            return
        }
        viewModelScope.launch {
            _busy.value = true; _error.value = null; _message.value = null
            try {
                val res = repo.createAppointment(AppointmentRequest(barberId, serviceId, date, time, notes.ifBlank { null }))
                _message.value = res.message ?: "Cita reservada correctamente."
                onDone()
            } catch (e: Exception) {
                _error.value = e.toFriendlyMessage("No se pudo reservar la cita.")
            } finally { _busy.value = false }
        }
    }

    fun defaultDate(): String = LocalDate.now().toString()
}

class CatalogViewModel : ViewModel() {
    private val repo = AppContainer.urbanRepository
    private val _services = MutableStateFlow<List<ServiceItem>>(emptyList())
    val services = _services.asStateFlow()
    private val _barbers = MutableStateFlow<List<BarberItem>>(emptyList())
    val barbers = _barbers.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun load() = viewModelScope.launch {
        _loading.value = true; _error.value = null
        try {
            _services.value = repo.services()
            _barbers.value = repo.barbers()
        } catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo cargar el catálogo.") }
        finally { _loading.value = false }
    }
}

class ProfileViewModel : ViewModel() {
    private val repo = AppContainer.urbanRepository
    private val _profile = MutableStateFlow<ProfileUser?>(null)
    val profile = _profile.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun load() = viewModelScope.launch {
        _busy.value = true; _error.value = null
        try { _profile.value = repo.profile() }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo cargar tu perfil.") }
        finally { _busy.value = false }
    }

    fun save(name: String, email: String, phone: String, birth: String, sex: String) = viewModelScope.launch {
        _busy.value = true; _message.value = null; _error.value = null
        try {
            val res = repo.updateProfile(
                UpdateProfileRequest(
                    name = name.trim(), email = email.trim(),
                    telefono = phone.ifBlank { null },
                    fechaNacimiento = birth.ifBlank { null },
                    sexo = sex.ifBlank { null }
                )
            )
            _message.value = res.message ?: "Perfil actualizado."
            load()
        } catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo guardar el perfil.") }
        finally { _busy.value = false }
    }
}
