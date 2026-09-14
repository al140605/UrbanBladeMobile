package com.urbanblade.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.urbanblade.mobile.core.network.AppContainer
import com.urbanblade.mobile.data.model.*
import com.urbanblade.mobile.data.repository.UrbanRepository
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
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

class AppointmentsViewModel @JvmOverloads constructor(
    private val repo: UrbanRepository = AppContainer.urbanRepository
) : ViewModel() {
    private val _data = MutableStateFlow(AppointmentsResponse())
    val data = _data.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    // Acción de reagendar tiene su propio busy -- no debe deshabilitar toda
    // la lista mientras el usuario edita en la hoja de reagendado.
    private val _rescheduling = MutableStateFlow(false)
    val rescheduling = _rescheduling.asStateFlow()

    private val _waitlistEntries = MutableStateFlow<List<WaitlistEntry>>(emptyList())
    val waitlistEntries = _waitlistEntries.asStateFlow()

    // Solo para la hoja de reagendado (mantiene el servicio de la cita,
    // permite cambiar barbero/fecha/hora) -- estado independiente del
    // wizard de nueva reserva en BookingViewModel.
    private val _barbers = MutableStateFlow<List<BarberItem>>(emptyList())
    val barbers = _barbers.asStateFlow()
    private val _rescheduleSlots = MutableStateFlow<List<SlotItem>>(emptyList())
    val rescheduleSlots = _rescheduleSlots.asStateFlow()

    fun loadBarbers() = viewModelScope.launch {
        try { _barbers.value = repo.barbers() } catch (_: Exception) { }
    }

    fun loadRescheduleSlots(barberId: String, serviceId: String, date: String) = viewModelScope.launch {
        if (barberId.isBlank() || serviceId.isBlank() || date.isBlank()) { _rescheduleSlots.value = emptyList(); return@launch }
        try { _rescheduleSlots.value = repo.slots(barberId, serviceId, date) }
        catch (_: Exception) { _rescheduleSlots.value = emptyList() }
    }

    fun load() = viewModelScope.launch {
        _loading.value = true; _error.value = null
        try { _data.value = repo.appointments() }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudieron cargar las citas.") }
        finally { _loading.value = false }
    }

    fun loadWaitlist() = viewModelScope.launch {
        try { _waitlistEntries.value = repo.waitlist() }
        catch (_: Exception) { /* sección secundaria, no bloquea la pantalla */ }
    }

    fun leaveWaitlist(id: String) = viewModelScope.launch {
        try { repo.leaveWaitlist(id); loadWaitlist() }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo salir de la lista de espera.") }
    }

    fun cancel(item: AppointmentRow) = viewModelScope.launch {
        val code = item.code ?: return@launch
        try { repo.cancelAppointment(code); load() }
        catch (e: HttpException) {
            // El 422 aquí es la política de cancelación (N horas de
            // anticipación) o "ya no se puede cancelar" -- mostrar el mensaje
            // real del servidor, no el genérico de toFriendlyMessage().
            _error.value = if (e.code() == 422) e.serverMessage() ?: "Esta cita ya no se puede cancelar."
            else e.toFriendlyMessage("No se pudo cancelar la cita.")
        }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo cancelar la cita.") }
    }

    /**
     * Reagenda una cita propia -- PUT appointments/{code}, que en barber
     * detecta el rol cliente y delega a rescheduleAsClient() (mismo
     * AppointmentRequest que crear una cita, sin política de horas mínima,
     * solo exige que la cita siga pendiente/confirmada y futura).
     */
    fun reschedule(
        code: String,
        barberId: String,
        serviceId: String,
        fecha: String,
        horaInicio: String,
        notas: String?,
        onDone: () -> Unit
    ) {
        if (code.isBlank() || barberId.isBlank() || serviceId.isBlank() || fecha.isBlank() || horaInicio.isBlank()) {
            _error.value = "Selecciona barbero, servicio, fecha y horario."
            return
        }
        viewModelScope.launch {
            _rescheduling.value = true; _error.value = null
            try {
                repo.updateAppointment(code, AppointmentRequest(barberId, serviceId, fecha, horaInicio, notas))
                load()
                onDone()
            } catch (e: HttpException) {
                _error.value = if (e.code() == 422) e.serverMessage() ?: "Esta cita ya no se puede reagendar."
                else e.toFriendlyMessage("No se pudo reagendar la cita.")
            } catch (e: Exception) {
                _error.value = e.toFriendlyMessage("No se pudo reagendar la cita.")
            } finally { _rescheduling.value = false }
        }
    }
}

class BookingViewModel @JvmOverloads constructor(
    private val repo: UrbanRepository = AppContainer.urbanRepository
) : ViewModel() {

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
    private val _waitlistJoined = MutableStateFlow(false)
    val waitlistJoined = _waitlistJoined.asStateFlow()

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

    fun joinWaitlist(barberId: String, serviceId: String, date: String) = viewModelScope.launch {
        if (barberId.isBlank() || serviceId.isBlank() || date.isBlank()) return@launch
        try {
            val res = repo.joinWaitlist(barberId, serviceId, date)
            _message.value = res.message ?: "Te anotamos en la lista de espera."
            _waitlistJoined.value = true
        } catch (e: Exception) {
            _error.value = e.toFriendlyMessage("No se pudo unir a la lista de espera.")
        }
    }
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

data class WalletData(
    val loyalty: ClientLoyalty? = null,
    val membership: MyMembership? = null,
    val packages: List<MyPackage> = emptyList(),
    val giftCards: List<GiftCard> = emptyList(),
    val referrals: ReferralInfo? = null
)

/**
 * Solo lectura esta ronda: carga las cinco fuentes de autoservicio en
 * paralelo (memberships/mine, packages, gift-cards/mine, referrals/mine,
 * más lealtad embebida en dashboard). Comprar paquetes/membresías/gift
 * cards y suscribirse queda para la ronda de checkout con Stripe.
 */
class WalletViewModel @JvmOverloads constructor(
    private val repo: UrbanRepository = AppContainer.urbanRepository
) : ViewModel() {
    private val _data = MutableStateFlow(WalletData())
    val data = _data.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun load() = viewModelScope.launch {
        _loading.value = true; _error.value = null
        try {
            _data.value = WalletData(
                loyalty = runCatching { repo.clientLoyalty() }.getOrNull(),
                membership = runCatching { repo.myMembership() }.getOrNull(),
                packages = runCatching { repo.myPackages() }.getOrDefault(emptyList()),
                giftCards = runCatching { repo.myGiftCards() }.getOrDefault(emptyList()),
                referrals = runCatching { repo.myReferrals() }.getOrNull()
            )
        } catch (e: Exception) {
            _error.value = e.toFriendlyMessage("No se pudo cargar tu wallet.")
        } finally { _loading.value = false }
    }

    fun cancelMembership() = viewModelScope.launch {
        _busy.value = true; _error.value = null
        try {
            val res = repo.cancelMembership()
            _message.value = res.message ?: "Tu membresía se cancelará al finalizar el periodo actual."
            load()
        } catch (e: Exception) {
            _error.value = e.toFriendlyMessage("No se pudo cancelar la membresía.")
        } finally { _busy.value = false }
    }
}
