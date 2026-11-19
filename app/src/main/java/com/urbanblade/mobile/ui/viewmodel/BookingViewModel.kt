package com.urbanblade.mobile.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.urbanblade.mobile.core.network.AppContainer
import com.urbanblade.mobile.data.model.*
import com.urbanblade.mobile.data.repository.UrbanRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.time.LocalDate


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
    private val _slotsLoading = MutableStateFlow(false)
    val slotsLoading = _slotsLoading.asStateFlow()
    private val _slotsError = MutableStateFlow<String?>(null)
    val slotsError = _slotsError.asStateFlow()
    private val _slotsFor = MutableStateFlow<String?>(null)
    val slotsFor = _slotsFor.asStateFlow()
    private var slotsRequest: kotlinx.coroutines.Job? = null
    private val _catalogLoading = MutableStateFlow(false)
    val catalogLoading = _catalogLoading.asStateFlow()
    private val _catalogError = MutableStateFlow<String?>(null)
    val catalogError = _catalogError.asStateFlow()
    /** Productos con existencias para el paso "Extras" (mismo catálogo que la tienda). */
    private val _products = MutableStateFlow<List<ProductItem>>(emptyList())
    val products = _products.asStateFlow()
    /** Qué pasó con los productos de la visita, para el mensaje final. */
    private val _productsNote = MutableStateFlow<String?>(null)
    val productsNote = _productsNote.asStateFlow()
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

    /** Flujo finalizado con cita creada; no significa que el backend haya confirmado el pago. */
    private val _reservationCompleted = MutableStateFlow(false)
    val reservationCompleted = _reservationCompleted.asStateFlow()

    private var pendingDone: ((String?) -> Unit)? = null
    private var pendingId: String? = null

    /** Datos bancarios y tarjetas guardadas; si fallan, el pago sigue disponible (sin CLABE o sin tarjetas guardadas). */
    private val _transferInfoError = MutableStateFlow<String?>(null)
    val transferInfoError = _transferInfoError.asStateFlow()
    private var transferRequest: Job? = null
    fun loadTransferQuote(serviceId: String, propina: Double) {
        transferRequest?.cancel()
        _transferInfo.value = null
        _transferInfoError.value = null
        transferRequest = viewModelScope.launch {
            try {
                val info = repo.transferInfo(propina = propina, serviceId = serviceId)
                ensureActive()
                _transferInfo.value = info
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _transferInfoError.value = e.toFriendlyMessage("No se pudo consultar el importe de transferencia.") }
        }
    }

    fun loadPaymentOptions() = viewModelScope.launch {
        _savedCards.value = try { repo.savedCards() } catch (_: Exception) { emptyList() }
    }

    fun loadCatalog() = viewModelScope.launch {
        if (_catalogLoading.value) return@launch
        _catalogLoading.value = true
        _catalogError.value = null
        // Los productos son opcionales: si fallan, la reserva sigue sin el paso de extras con productos.
        launch { _products.value = runCatching { repo.products() }.getOrDefault(emptyList()).filter { it.stockActual > 0 } }
        try {
            val services = repo.services()
            val barbers = repo.barbers()
            _services.value = services
            _barbers.value = barbers
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            _catalogError.value = e.toFriendlyMessage("No se pudo cargar el catálogo.")
        } finally {
            _catalogLoading.value = false
        }
    }

    fun loadSlots(barberId: String, serviceId: String, date: String) {
        slotsRequest?.cancel()
        _slotsFor.value = null
        _slots.value = emptyList()
        slotsRequest = viewModelScope.launch {
            _slots.value = emptyList(); _slotsError.value = null
            if (barberId.isBlank() || serviceId.isBlank() || date.isBlank()) return@launch
            _slotsLoading.value = true
            try {
                _slots.value = repo.slots(barberId, serviceId, date)
                _slotsFor.value = "$barberId|$serviceId|$date"
            }
            catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { _slotsError.value = e.toFriendlyMessage("No pudimos consultar los horarios.") }
            finally { _slotsLoading.value = false }
        }
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
        productos: List<OrderItemRequest> = emptyList(),
        aceptaCargo: Boolean = false,
        onDone: (String?) -> Unit
    ) {
        // El bloqueo se toma antes de lanzar: dos eventos en el mismo frame no crean dos citas.
        if (_busy.value || _reservationCompleted.value) return
        if (barberId.isBlank() || serviceId.isBlank() || date.isBlank() || time.isBlank()) {
            _error.value = "Selecciona barbero, servicio, fecha y horario."
            return
        }
        _busy.value = true
        viewModelScope.launch {
            _error.value = null; _message.value = null; _paymentNote.value = null
            val res = try {
                // Igual que la web: "pagar ahora" (tarjeta, o transferencia con comprobante) le pide a barber
                // que fije el cobro completo con el descuento real del cliente; se cobra por el endpoint de depósito.
                val payNow = method == BookingPayMethod.TARJETA || (method == BookingPayMethod.TRANSFERENCIA && receiptUri != null)
                repo.createAppointment(
                    AppointmentRequest(
                        barberId, serviceId, date, time, notes.ifBlank { null },
                        pagarAhora = payNow.takeIf { it },
                        propinaSugerida = propina.takeIf { payNow && it > 0 },
                        aceptaCargoInasistencia = aceptaCargo.takeIf { it },
                        productos = productos.ifEmpty { null }
                    )
                )
            } catch (e: Exception) {
                // Un 422 trae el motivo real (una cita por día, horario ocupado): se muestra tal cual.
                _error.value = (e as? HttpException)?.takeIf { it.code() == 422 }?.serverMessage()
                    ?: e.toFriendlyMessage("No se pudo reservar la cita.")
                _busy.value = false
                return@launch
            }
            val id = res.data?.id
            val code = res.data?.code
            _productsNote.value = when {
                productos.isEmpty() -> null
                res.productosError != null -> "Tu cita quedó reservada, pero no pudimos agregar los productos: ${res.productosError}"
                else -> "Tus productos te esperan en el salón y se pagan al recogerlos."
            }
            when (method) {
                BookingPayMethod.EFECTIVO -> {
                    _paymentNote.value = "Pagas en el salón el día de tu cita."
                    finishReservation(id, onDone)
                }
                BookingPayMethod.TRANSFERENCIA -> {
                    // El comprobante es opcional al reservar: si no lo trae, lo sube después desde Mis citas.
                    _paymentNote.value = if (receiptUri == null) {
                        "Transfiere a la CLABE que te mostramos y sube tu comprobante desde Mis citas cuando el barbero confirme tu cita."
                    } else try {
                        if (context == null || code == null) error("sin datos")
                        repo.uploadDepositReceipt(context, code, receiptUri)
                        "Recibimos tu comprobante. Te avisaremos cuando se verifique."
                    } catch (e: Exception) {
                        "Tu cita quedó reservada, pero no se pudo subir el comprobante. Súbelo desde Mis citas."
                    }
                    finishReservation(id, onDone)
                }
                BookingPayMethod.TARJETA -> {
                    try {
                        if (code == null) error("sin código")
                        pendingDone = onDone; pendingId = id
                        val secret = repo.depositStripeIntent(
                            code,
                            DepositIntentRequest(
                                guardarTarjeta = (saveCard && savedCardId == null).takeIf { it },
                                tarjetaGuardada = (savedCardId != null).takeIf { it }
                            )
                        ).clientSecret
                        _cardConfirm.value = CardConfirm(secret, savedCardId)
                    } catch (e: Exception) {
                        pendingDone = null; pendingId = null
                        _paymentNote.value = "Tu cita quedó reservada, pero no pudimos iniciar el pago con tarjeta. Págala desde Mis citas."
                        finishReservation(id, onDone)
                    }
                }
            }
        }
    }

    /** Resultado de confirmar la tarjeta con Stripe. El cobro real lo confirma el webhook de barber. */
    @Suppress("UNUSED_PARAMETER")
    fun onCardResult(completed: Boolean, canceled: Boolean, reason: String?) {
        // El launcher puede volver a entregar un resultado; solo se consume el pendiente actual.
        val done = pendingDone ?: return
        val id = pendingId
        pendingDone = null; pendingId = null
        _cardConfirm.value = null
        _paymentNote.value = when {
            completed -> "Pago con tarjeta enviado. UrbanBlade lo está confirmando."
            canceled -> "Cancelaste el pago. Tu cita quedó reservada y puedes pagarla desde Mis citas."
            else -> "El pago con tarjeta no se completó. Tu cita quedó reservada; págala desde Mis citas."
        }
        finishReservation(id, done)
    }

    private fun finishReservation(id: String?, onDone: (String?) -> Unit) {
        _reservationCompleted.value = true
        _busy.value = false
        onDone(id)
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
