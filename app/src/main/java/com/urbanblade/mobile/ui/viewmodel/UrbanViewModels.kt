package com.urbanblade.mobile.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.urbanblade.mobile.core.network.AppContainer
import com.urbanblade.mobile.data.model.*
import com.urbanblade.mobile.data.repository.UrbanRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException


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
    private val _hasLoaded = MutableStateFlow(false)
    val hasLoaded = _hasLoaded.asStateFlow()
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

    // Adeudo por inasistencia del cliente (mientras sea > 0 no puede reservar) y ticket del servicio terminado.
    private val _debt = MutableStateFlow(0.0)
    val debt = _debt.asStateFlow()
    private val _debtError = MutableStateFlow<String?>(null)
    val debtError = _debtError.asStateFlow()
    private var debtRequest: Job? = null
    private val _ticket = MutableStateFlow<ServiceTicket?>(null)
    val ticket = _ticket.asStateFlow()
    private val _ticketBusy = MutableStateFlow<String?>(null)
    val ticketBusy = _ticketBusy.asStateFlow()

    /** Un error de consulta nunca convierte el último adeudo confirmado en cero. */
    fun loadDebt() {
        debtRequest?.cancel()
        debtRequest = viewModelScope.launch {
            _debtError.value = null
            try {
                val total = repo.noShowFees().adeudoTotal
                coroutineContext.ensureActive()
                if (total != null) _debt.value = total
                else _debtError.value = "No pudimos comprobar tu adeudo. Actualiza para volver a consultar."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _debtError.value = "No pudimos comprobar tu adeudo. Actualiza para volver a consultar."
            }
        }
    }

    fun openTicket(code: String) = viewModelScope.launch {
        _ticketBusy.value = code
        try { _ticket.value = repo.appointmentTicket(code) }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("Esta cita todavía no tiene ticket.") }
        finally { _ticketBusy.value = null }
    }

    fun dismissTicket() { _ticket.value = null }

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

    // Tarjetas guardadas del cliente para el carrusel del checkout (vacío si no tiene o si falla).
    private val _savedCards = MutableStateFlow<List<SavedCard>>(emptyList())
    val savedCards = _savedCards.asStateFlow()

    // Pago con una tarjeta guardada: se confirma en la hoja con PaymentLauncher, sin abrir el PaymentSheet.
    private val _savedCardConfirm = MutableStateFlow<CardConfirm?>(null)
    val savedCardConfirm = _savedCardConfirm.asStateFlow()

    fun loadSavedCards() = viewModelScope.launch {
        _savedCards.value = try { repo.savedCards() } catch (_: Exception) { emptyList() }
    }

    /** Datos bancarios para transferir; null mientras cargan. Si fallan, se muestran como «sin configurar». */
    private val _transferInfo = MutableStateFlow<TransferInfo?>(null)
    val transferInfo = _transferInfo.asStateFlow()

    private val _transferInfoError = MutableStateFlow<String?>(null)
    val transferInfoError = _transferInfoError.asStateFlow()
    private var transferRequest: Job? = null
    fun loadTransferInfo(appointmentId: String, propina: Double = 0.0) {
        transferRequest?.cancel()
        _transferInfo.value = null
        _transferInfoError.value = null
        transferRequest = viewModelScope.launch {
            try {
                val info = repo.transferInfo(appointmentId, propina)
                ensureActive()
                _transferInfo.value = info
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _transferInfoError.value = e.toFriendlyMessage("No se pudo consultar el importe de transferencia.") }
        }
    }

    /** Quita un error anterior (p. ej. al cambiar de método de pago) para que no parezca del paso actual. */
    fun clearError() { _error.value = null }

    fun clearSavedCardConfirm() { _savedCardConfirm.value = null }

    /**
     * Con [savedCardId] el intent se crea para el Customer del cliente (tarjeta_guardada) y se confirma
     * con esa tarjeta; sin él, se abre el PaymentSheet de Stripe para una tarjeta nueva, como antes.
     */
    fun startStripeCheckout(appointmentId: String, puntosCanjeados: Int, codigoGiftCard: String?, propina: Double, savedCardId: String? = null) =
        viewModelScope.launch {
            _checkoutBusy.value = true; _error.value = null; _paymentNotice.value = null
            try {
                val data = repo.stripeIntent(
                    StripeIntentRequest(
                        appointmentId, puntosCanjeados, codigoGiftCard?.takeIf { it.isNotBlank() }, propina,
                        tarjetaGuardada = (savedCardId != null).takeIf { it }
                    )
                )
                if (savedCardId != null) _savedCardConfirm.value = CardConfirm(data.clientSecret, savedCardId)
                else _stripeClientSecret.value = data.clientSecret
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
    @Suppress("UNUSED_PARAMETER") // No mostrar detalles internos del SDK en la interfaz.
    fun onStripeFailed(reason: String?) {
        _stripeClientSecret.value = null
        _paymentNotice.value = null
        _error.value = "El pago con tarjeta no se completó. Intenta con otra tarjeta o paga por transferencia."
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
    private var listRequest: Job? = null
    private var pageRequest: Job? = null

    private val _month = MutableStateFlow<List<AppointmentRow>>(emptyList())
    /** Citas del mes que muestra el calendario. */
    val month = _month.asStateFlow()
    private val _monthLoading = MutableStateFlow(false)
    val monthLoading = _monthLoading.asStateFlow()
    private var currentMonth: java.time.YearMonth? = null

    fun load() {
        pageRequest?.cancel()
        _loadingMore.value = false
        listRequest?.cancel()
        listRequest = viewModelScope.launch {
            _loading.value = true; _error.value = null
            try {
                val response = repo.appointments(page = 1, perPage = PAGE_SIZE)
                coroutineContext.ensureActive()
                page = 1
                _data.value = response
                _hasLoaded.value = true
                // Un servidor sin paginación no manda `meta`: se trata como lista completa.
                _hasMore.value = response.meta?.hasMore == true
            }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudieron cargar las citas.") }
            finally { _loading.value = false }
            currentMonth?.let { loadMonth(it) }
        }
    }

    /** Trae la página siguiente (citas más antiguas) y la agrega al final de la lista. */
    fun loadMore() {
        if (_loading.value || listRequest?.isActive == true || pageRequest?.isActive == true || !_hasMore.value) return
        pageRequest = viewModelScope.launch {
            _loadingMore.value = true
            _error.value = null
            try {
                val response = repo.appointments(page = page + 1, perPage = PAGE_SIZE)
                coroutineContext.ensureActive()
                page += 1
                val known = _data.value.data.map { it.id }.toSet()
                _data.value = _data.value.copy(data = _data.value.data + response.data.filter { it.id !in known })
                _hasMore.value = response.meta?.hasMore == true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = e.toFriendlyMessage("No se pudieron cargar más citas.")
            } finally { _loadingMore.value = false }
        }
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

    /** Llegadas registradas en esta sesión (código de cita → hora ISO): el listado del servidor no trae `llegada_en`. */
    private val _arrivals = MutableStateFlow<Map<String, String>>(emptyMap())
    val arrivals = _arrivals.asStateFlow()

    /** «Llegó»: registra la llegada del cliente sin cambiar el estado de la cita; el servidor es idempotente. */
    fun checkIn(item: AppointmentRow) = viewModelScope.launch {
        val code = item.code ?: return@launch
        if (_actionBusy.value) return@launch
        _actionBusy.value = true; _error.value = null
        try {
            val res = repo.checkInAppointment(code)
            res.llegadaEn?.let { _arrivals.value = _arrivals.value + (code to it) }
            _notice.value = res.message ?: "Llegada registrada."
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: HttpException) {
            _error.value = if (e.code() == 422) e.serverMessage() ?: "No se puede registrar la llegada de esta cita."
            else e.toFriendlyMessage("No se pudo registrar la llegada.")
        } catch (e: Exception) {
            _error.value = e.toFriendlyMessage("No se pudo registrar la llegada.")
        } finally { _actionBusy.value = false }
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

    /**
     * El cliente cancela su propia cita (DELETE, con la política de horas); el personal la cancela
     * cambiando el estado (PATCH status), que es lo que el servidor le permite a recepción y además
     * avisa al cliente, devuelve lo pagado al reservar y libera el horario para la lista de espera.
     */
    fun cancel(item: AppointmentRow, asStaff: Boolean = false) = viewModelScope.launch {
        val code = item.code ?: return@launch
        if (asStaff) {
            changeStatus(item, "cancelada")
            return@launch
        }
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
