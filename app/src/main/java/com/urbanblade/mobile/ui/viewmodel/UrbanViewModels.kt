package com.urbanblade.mobile.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.urbanblade.mobile.core.network.AppContainer
import com.urbanblade.mobile.data.model.*
import com.urbanblade.mobile.data.repository.UrbanRepository
import com.google.gson.JsonObject
import kotlinx.coroutines.delay
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

    // Checkout de cita: crear el intent de Stripe tiene su propio busy;
    // subir comprobante de transferencia también, para no bloquear el resto
    // de la hoja mientras cualquiera de los dos está en curso.
    private val _checkoutBusy = MutableStateFlow(false)
    val checkoutBusy = _checkoutBusy.asStateFlow()
    private val _uploadingReceipt = MutableStateFlow(false)
    val uploadingReceipt = _uploadingReceipt.asStateFlow()
    private val _stripeClientSecret = MutableStateFlow<String?>(null)
    val stripeClientSecret = _stripeClientSecret.asStateFlow()

    // La confirmación real del pago con tarjeta la hace el webhook de
    // Stripe en barber, no la respuesta del intent -- tras un
    // PaymentSheetResult.Completed hay una ventana real donde la cita
    // todavía no trae hasPayment=true. Este flag solo controla el mensaje
    // "confirmando tu pago" mientras se reintenta un refresco corto.
    private val _confirmingPayment = MutableStateFlow(false)
    val confirmingPayment = _confirmingPayment.asStateFlow()

    // Aviso informativo del checkout con tarjeta (cancelación o confirmación
    // demorada). Separado de _error porque no es un fallo: la cita solo queda
    // pagada cuando el webhook de barber recibe payment_intent.succeeded.
    private val _paymentNotice = MutableStateFlow<String?>(null)
    val paymentNotice = _paymentNotice.asStateFlow()

    fun startStripeCheckout(appointmentId: String, puntosCanjeados: Int, codigoGiftCard: String?, propina: Double) =
        viewModelScope.launch {
            _checkoutBusy.value = true; _error.value = null; _paymentNotice.value = null
            try {
                val data = repo.stripeIntent(
                    StripeIntentRequest(appointmentId, puntosCanjeados, codigoGiftCard?.takeIf { it.isNotBlank() }, propina)
                )
                _stripeClientSecret.value = data.clientSecret
            } catch (e: HttpException) {
                _error.value = if (e.code() == 422) e.serverMessage() ?: e.toFriendlyMessage("No se pudo iniciar el pago.")
                else e.toFriendlyMessage("No se pudo iniciar el pago.")
            } catch (e: Exception) {
                _error.value = e.toFriendlyMessage("No se pudo iniciar el pago.")
            } finally { _checkoutBusy.value = false }
        }

    fun clearStripeClientSecret() { _stripeClientSecret.value = null }

    /** El cliente cerró el PaymentSheet sin pagar: Stripe no hizo ningún cargo y la cita sigue pendiente de pago. */
    fun onStripeCanceled() {
        _stripeClientSecret.value = null
        _paymentNotice.value = "Cancelaste el pago con tarjeta. No se hizo ningún cargo y tu cita sigue pendiente de pago."
    }

    /** Stripe rechazó o no pudo procesar la tarjeta. La cita no se marca como pagada (solo el webhook de barber puede hacerlo). */
    fun onStripeFailed(reason: String?) {
        _stripeClientSecret.value = null
        _paymentNotice.value = null
        val detail = reason?.trim()?.takeIf { it.isNotEmpty() }?.let { ": $it" } ?: "."
        _error.value = "El pago con tarjeta no se completó$detail Intenta con otra tarjeta o paga por transferencia."
    }

    /** Reintenta cargar las citas hasta ~10s esperando a que el webhook confirme el pago. */
    fun confirmAppointmentPayment(appointmentId: String) = viewModelScope.launch {
        _confirmingPayment.value = true
        _paymentNotice.value = null
        repeat(5) {
            delay(2000)
            try {
                val fresh = repo.appointments()
                _data.value = fresh
                if (fresh.data.any { it.id == appointmentId && it.hasPayment }) return@launch
            } catch (_: Exception) { /* se reintenta en el próximo ciclo */ }
        }
        // Stripe ya aceptó el pago pero el webhook aún no llega: avisar para que
        // el cliente no intente pagar otra vez mientras se confirma.
        _paymentNotice.value = "Stripe recibió tu pago y UrbanBlade lo está confirmando. Aparecerá como pagado en unos minutos; no es necesario pagar de nuevo."
    }.also { it.invokeOnCompletion { _confirmingPayment.value = false } }

    fun uploadPaymentReceipt(context: Context, appointmentCode: String, propina: Double, receiptUri: Uri, onDone: () -> Unit) =
        viewModelScope.launch {
            _uploadingReceipt.value = true; _error.value = null
            try {
                repo.uploadPaymentReceipt(context, appointmentCode, propina, receiptUri)
                load()
                onDone()
            } catch (e: HttpException) {
                _error.value = if (e.code() == 422) e.serverMessage() ?: e.toFriendlyMessage("No se pudo subir el comprobante.")
                else e.toFriendlyMessage("No se pudo subir el comprobante.")
            } catch (e: Exception) {
                _error.value = e.toFriendlyMessage("No se pudo subir el comprobante.")
            } finally { _uploadingReceipt.value = false }
        }

    fun loadBarbers() = viewModelScope.launch {
        try { _barbers.value = repo.barbers() } catch (_: Exception) { }
    }

    fun loadRescheduleSlots(barberId: String, serviceId: String, date: String) = viewModelScope.launch {
        if (barberId.isBlank() || serviceId.isBlank() || date.isBlank()) { _rescheduleSlots.value = emptyList(); return@launch }
        try { _rescheduleSlots.value = repo.slots(barberId, serviceId, date) }
        catch (_: Exception) { _rescheduleSlots.value = emptyList() }
    }

    private val _hasMore = MutableStateFlow(false)
    /** Hay más citas (más antiguas) por traer del servidor. */
    val hasMore = _hasMore.asStateFlow()
    private val _loadingMore = MutableStateFlow(false)
    val loadingMore = _loadingMore.asStateFlow()
    private var page = 1

    private val _month = MutableStateFlow<List<AppointmentRow>>(emptyList())
    /** Citas del mes que muestra el calendario. */
    val month = _month.asStateFlow()
    private val _monthLoading = MutableStateFlow(false)
    val monthLoading = _monthLoading.asStateFlow()
    private var currentMonth: java.time.YearMonth? = null

    fun load() = viewModelScope.launch {
        _loading.value = true; _error.value = null
        try {
            val response = repo.appointments(page = 1, perPage = PAGE_SIZE)
            page = 1
            _data.value = response
            // Un servidor sin paginación no manda `meta`: se trata como lista completa.
            _hasMore.value = response.meta?.hasMore == true
        }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudieron cargar las citas.") }
        finally { _loading.value = false }
        currentMonth?.let { loadMonth(it) }
    }

    /** Trae la página siguiente (citas más antiguas) y la agrega al final de la lista. */
    fun loadMore() = viewModelScope.launch {
        if (_loadingMore.value || !_hasMore.value) return@launch
        _loadingMore.value = true
        try {
            val response = repo.appointments(page = page + 1, perPage = PAGE_SIZE)
            page += 1
            val known = _data.value.data.map { it.id }.toSet()
            _data.value = _data.value.copy(data = _data.value.data + response.data.filter { it.id !in known })
            _hasMore.value = response.meta?.hasMore == true
        } catch (e: Exception) {
            _error.value = e.toFriendlyMessage("No se pudieron cargar más citas.")
        } finally { _loadingMore.value = false }
    }

    /** Carga todas las citas de un mes para el calendario (varias páginas si hace falta). */
    fun loadMonth(month: java.time.YearMonth) = viewModelScope.launch {
        currentMonth = month
        _monthLoading.value = true
        try {
            val desde = month.atDay(1).toString()
            val hasta = month.atEndOfMonth().toString()
            val rows = mutableListOf<AppointmentRow>()
            var p = 1
            while (p <= MAX_MONTH_PAGES) {
                val response = repo.appointments(page = p, perPage = 50, desde = desde, hasta = hasta)
                rows += response.data
                if (response.meta?.hasMore != true) break
                p += 1
            }
            // Si el servidor ignora el rango, este filtro deja solo el mes pedido.
            if (currentMonth == month) _month.value = rows.filter { it.fecha.startsWith(month.toString()) }
        } catch (e: Exception) {
            _error.value = e.toFriendlyMessage("No se pudo cargar el calendario.")
        } finally { _monthLoading.value = false }
    }

    private companion object {
        const val PAGE_SIZE = 30
        const val MAX_MONTH_PAGES = 6
    }

    fun loadWaitlist() = viewModelScope.launch {
        try { _waitlistEntries.value = repo.waitlist() }
        catch (_: Exception) { /* sección secundaria, no bloquea la pantalla */ }
    }

    fun leaveWaitlist(id: String) = viewModelScope.launch {
        try { repo.leaveWaitlist(id); loadWaitlist() }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo salir de la lista de espera.") }
    }

    private val _actionBusy = MutableStateFlow(false)
    /** Ocupado mientras el personal cambia el estado de una cita o registra un cobro. */
    val actionBusy = _actionBusy.asStateFlow()
    private val _notice = MutableStateFlow<String?>(null)
    val notice = _notice.asStateFlow()

    fun clearNotice() {
        _notice.value = null
    }

    /** Confirmar, iniciar, completar o marcar inasistencia; la máquina de estados la valida el servidor. */
    fun changeStatus(item: AppointmentRow, estado: String) = viewModelScope.launch {
        val code = item.code ?: return@launch
        _actionBusy.value = true; _error.value = null
        try {
            repo.updateAppointmentStatus(code, estado)
            load()
        } catch (e: HttpException) {
            _error.value = if (e.code() == 422) e.serverMessage() ?: "No se puede cambiar el estado de esta cita."
            else e.toFriendlyMessage("No se pudo cambiar el estado de la cita.")
        } catch (e: Exception) {
            _error.value = e.toFriendlyMessage("No se pudo cambiar el estado de la cita.")
        } finally { _actionBusy.value = false }
    }

    /** Registra el cobro de una cita (efectivo o transferencia); el monto real lo fija el servidor. */
    fun charge(item: AppointmentRow, metodo: String, propina: Double, onDone: () -> Unit) = viewModelScope.launch {
        _actionBusy.value = true; _error.value = null
        try {
            val informativo = item.precioCobrado ?: item.service?.precio ?: 0.0
            repo.createPayment(CreatePaymentRequest(item.id, informativo, metodo, propina))
            _notice.value = "Cobro registrado."
            onDone()
            load()
        } catch (e: HttpException) {
            _error.value = if (e.code() == 422) e.serverMessage() ?: "No se pudo registrar el cobro."
            else e.toFriendlyMessage("No se pudo registrar el cobro.")
        } catch (e: Exception) {
            _error.value = e.toFriendlyMessage("No se pudo registrar el cobro.")
        } finally { _actionBusy.value = false }
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

/** Cómo paga el cliente al reservar: en el salón, por transferencia o con tarjeta ahora. */
enum class BookingPayMethod { EFECTIVO, TRANSFERENCIA, TARJETA }

/** PaymentIntent listo para confirmar; [paymentMethodId] es la tarjeta guardada elegida (null = tarjeta nueva). */
data class CardConfirm(val clientSecret: String, val paymentMethodId: String?)

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

    /**
     * Pago con tarjeta pendiente de confirmar en la pantalla: con [CardConfirm.paymentMethodId]
     * se paga con una tarjeta guardada; sin él, con lo que el cliente escribió en el formulario.
     */
    private val _cardConfirm = MutableStateFlow<CardConfirm?>(null)
    val cardConfirm = _cardConfirm.asStateFlow()

    /** Datos bancarios para transferir; null mientras cargan. */
    private val _transferInfo = MutableStateFlow<TransferInfo?>(null)
    val transferInfo = _transferInfo.asStateFlow()

    private val _savedCards = MutableStateFlow<List<SavedCard>>(emptyList())
    val savedCards = _savedCards.asStateFlow()

    /** Aviso sobre el pago tras reservar (comprobante recibido, pago pendiente, etc.). */
    private val _paymentNote = MutableStateFlow<String?>(null)
    val paymentNote = _paymentNote.asStateFlow()

    private var pendingDone: ((String?) -> Unit)? = null
    private var pendingId: String? = null

    /** Datos bancarios y tarjetas guardadas; si fallan, el pago sigue disponible (sin CLABE o sin tarjetas guardadas). */
    fun loadPaymentOptions() = viewModelScope.launch {
        _transferInfo.value = try { repo.transferInfo() } catch (_: Exception) { TransferInfo() }
        _savedCards.value = try { repo.savedCards() } catch (_: Exception) { emptyList() }
    }

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

    fun create(barberId: String, serviceId: String, date: String, time: String, notes: String, onDone: (String?) -> Unit) {
        if (barberId.isBlank() || serviceId.isBlank() || date.isBlank() || time.isBlank()) {
            _error.value = "Selecciona barbero, servicio, fecha y horario."
            return
        }
        viewModelScope.launch {
            _busy.value = true; _error.value = null; _message.value = null
            try {
                val res = repo.createAppointment(AppointmentRequest(barberId, serviceId, date, time, notes.ifBlank { null }))
                _message.value = res.message ?: "Cita reservada correctamente."
                onDone(res.data?.id)
            } catch (e: Exception) {
                _error.value = e.toFriendlyMessage("No se pudo reservar la cita.")
            } finally { _busy.value = false }
        }
    }

    /**
     * Reserva y, según el método, paga en el mismo paso (igual que la web): efectivo no cobra nada
     * (se paga en el salón), transferencia sube el comprobante y tarjeta pide el intent a barber.
     * La cita se crea primero; si el pago falla la cita queda reservada y se avisa. El monto real
     * siempre lo calcula barber, aquí solo viaja la propina.
     */
    fun reserve(
        context: Context?,
        barberId: String, serviceId: String, date: String, time: String, notes: String,
        method: BookingPayMethod, propina: Double, receiptUri: Uri?,
        savedCardId: String? = null, saveCard: Boolean = false,
        onDone: (String?) -> Unit
    ) {
        if (barberId.isBlank() || serviceId.isBlank() || date.isBlank() || time.isBlank()) {
            _error.value = "Selecciona barbero, servicio, fecha y horario."
            return
        }
        viewModelScope.launch {
            _busy.value = true; _error.value = null; _message.value = null; _paymentNote.value = null
            val res = try {
                repo.createAppointment(AppointmentRequest(barberId, serviceId, date, time, notes.ifBlank { null }))
            } catch (e: Exception) {
                _error.value = e.toFriendlyMessage("No se pudo reservar la cita.")
                _busy.value = false
                return@launch
            }
            val id = res.data?.id
            val code = res.data?.code
            when (method) {
                BookingPayMethod.EFECTIVO -> {
                    _paymentNote.value = "Pagas en el salón el día de tu cita."
                    _busy.value = false; onDone(id)
                }
                BookingPayMethod.TRANSFERENCIA -> {
                    // El comprobante es opcional al reservar: si no lo trae, lo sube después desde Mis citas.
                    _paymentNote.value = if (receiptUri == null) {
                        "Transfiere a la CLABE que te mostramos y sube tu comprobante desde Mis citas."
                    } else try {
                        if (context == null || code == null) error("sin datos")
                        repo.uploadPaymentReceipt(context, code, propina, receiptUri)
                        "Recibimos tu comprobante. Te avisaremos cuando se verifique."
                    } catch (e: Exception) {
                        "Tu cita quedó reservada, pero no se pudo subir el comprobante. Súbelo desde Mis citas."
                    }
                    _busy.value = false; onDone(id)
                }
                BookingPayMethod.TARJETA -> {
                    try {
                        if (id == null) error("sin id")
                        pendingDone = onDone; pendingId = id
                        val secret = repo.stripeIntent(
                            StripeIntentRequest(
                                id,
                                propina = propina,
                                guardarTarjeta = (saveCard && savedCardId == null).takeIf { it },
                                tarjetaGuardada = (savedCardId != null).takeIf { it }
                            )
                        ).clientSecret
                        _cardConfirm.value = CardConfirm(secret, savedCardId)
                    } catch (e: Exception) {
                        pendingDone = null
                        _paymentNote.value = "Tu cita quedó reservada, pero no pudimos iniciar el pago con tarjeta. Págala desde Mis citas."
                        _busy.value = false; onDone(id)
                    }
                }
            }
        }
    }

    /** Resultado de confirmar la tarjeta con Stripe. El cobro real lo confirma el webhook de barber. */
    fun onCardResult(completed: Boolean, canceled: Boolean, reason: String?) {
        _cardConfirm.value = null
        _paymentNote.value = when {
            completed -> "Pago con tarjeta enviado. UrbanBlade lo está confirmando."
            canceled -> "Cancelaste el pago. Tu cita quedó reservada y puedes pagarla desde Mis citas."
            else -> "El pago con tarjeta no se completó${reason?.takeIf { it.isNotBlank() }?.let { ": $it" } ?: ""}. Tu cita quedó reservada; págala desde Mis citas."
        }
        _busy.value = false
        pendingDone?.invoke(pendingId)
        pendingDone = null
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
