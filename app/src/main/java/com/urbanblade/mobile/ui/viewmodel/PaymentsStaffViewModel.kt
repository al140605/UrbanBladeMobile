package com.urbanblade.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.urbanblade.mobile.core.network.AppContainer
import com.urbanblade.mobile.data.model.NoShowFeeItem
import com.urbanblade.mobile.data.model.PaymentRow
import com.urbanblade.mobile.data.model.PaymentsStats
import com.urbanblade.mobile.data.model.PendingPaymentRow
import com.urbanblade.mobile.data.repository.UrbanRepository
import com.urbanblade.mobile.ui.screens.ReportRange
import com.urbanblade.mobile.ui.screens.dates
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

/** Filtro por método de pago del historial. */
enum class PaymentMethodFilter(val label: String, val param: String?) {
    Todos("Todos", null),
    Efectivo("Efectivo", "efectivo"),
    Tarjeta("Tarjeta", "tarjeta"),
    Transferencia("Transferencia", "transferencia")
}

/** Motivo de rechazo: el servidor exige texto de hasta 500 caracteres. */
const val REJECT_REASON_MAX = 500

data class PaymentsStaffState(
    val items: List<PaymentRow> = emptyList(),
    val stats: PaymentsStats = PaymentsStats(),
    val total: Int = 0,
    val page: Int = 1,
    val lastPage: Int = 1,
    val pending: List<PendingPaymentRow> = emptyList(),
    /**
     * Anticipos por revisar: transferencias de "pagar ahora" al reservar y depósitos anti-no-show.
     * Antes ninguna pantalla (ni la web) los mostraba y se quedaban en revisión para siempre.
     */
    val deposits: List<PendingPaymentRow> = emptyList(),
    /** Adeudos por inasistencia pendientes: se cobran en sucursal (o admin los condona). */
    val fees: List<NoShowFeeItem> = emptyList(),
    val busyFeeId: String? = null,
    val query: String = "",
    val method: PaymentMethodFilter = PaymentMethodFilter.Todos,
    val range: ReportRange = ReportRange.All,
    /** True cuando la consulta del historial terminó bien; hasta entonces las cifras y los vacíos no son datos. */
    val loaded: Boolean = false,
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    /** Comprobante en revisión (aprobando o rechazando) para bloquear sus botones. */
    val busyPaymentId: String? = null,
    /** Error de aprobar, rechazar, cobrar, condonar o abrir un comprobante; la consulta usa [listError]. */
    val error: String? = null,
    /** Error de la consulta del historial (y de «cargar más»): no se mezcla con los de las acciones. */
    val listError: String? = null,
    /**
     * Fallo parcial: el historial cargó, pero no se pudo consultar alguna lista de revisión (comprobantes,
     * anticipos o adeudos). Se conserva lo anterior, que puede estar incompleto, y se avisa.
     */
    val reviewListsError: String? = null,
    /** True si no se pudo confirmar la lista de comprobantes o anticipos: no se puede decir «todo al día». */
    val reviewUnknown: Boolean = false,
    val notice: String? = null
) {
    val hasMore: Boolean get() = page < lastPage
}

/** Pagos del negocio: comprobantes por revisar más el historial con filtros, estadísticas y paginación. */
class PaymentsStaffViewModel @JvmOverloads constructor(
    private val repo: UrbanRepository = AppContainer.urbanRepository
) : ViewModel() {
    private val _state = MutableStateFlow(PaymentsStaffState())
    val state: StateFlow<PaymentsStaffState> = _state.asStateFlow()

    private var loadRequest: Job? = null
    private var moreRequest: Job? = null

    /** Consulta secundaria: si falla se conserva lo anterior y se avisa, pero nunca se traga una cancelación. */
    private suspend fun <T> secundaria(anterior: T, consulta: suspend () -> T): Pair<T, Boolean> =
        try {
            consulta() to true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            anterior to false
        }

    /** Cada consulta cancela la anterior (y la paginación en curso): una respuesta tardía no pisa la última. */
    fun load() {
        loadRequest?.cancel()
        moreRequest?.cancel()
        _state.value = _state.value.copy(loading = true, loadingMore = false, error = null, listError = null)
        loadRequest = viewModelScope.launch {
            try {
                val s = _state.value
                val (desde, hasta) = s.range.dates()
                val history = repo.paymentsForStaff(1, s.query.trim().ifEmpty { null }, s.method.param, desde, hasta)
                coroutineContext.ensureActive()
                // Si falla una lista de revisión no se oculta el historial: se conserva la anterior y se avisa.
                val pending = secundaria(_state.value.pending) { repo.pendingPayments().data }
                val deposits = secundaria(_state.value.deposits) { repo.pendingDeposits().data }
                val fees = secundaria(_state.value.fees) { repo.noShowFees().data }
                coroutineContext.ensureActive()
                val fallidas = buildList {
                    if (!pending.second) add("los comprobantes por revisar")
                    if (!deposits.second) add("los anticipos por revisar")
                    if (!fees.second) add("los adeudos por inasistencia")
                }
                val meta = history.meta
                _state.value = _state.value.copy(
                    items = history.data,
                    stats = meta?.stats ?: PaymentsStats(),
                    total = meta?.total ?: history.data.size,
                    page = meta?.currentPage ?: 1,
                    lastPage = meta?.lastPage ?: 1,
                    pending = pending.first,
                    deposits = deposits.first,
                    fees = fees.first,
                    reviewListsError = if (fallidas.isEmpty()) null else
                        "No pudimos consultar ${fallidas.joinToString(", ")}. La lista puede estar incompleta.",
                    reviewUnknown = !pending.second || !deposits.second,
                    loaded = true,
                    loading = false
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                coroutineContext.ensureActive()
                _state.value = _state.value.copy(loading = false, listError = e.toFriendlyMessage("No se pudieron cargar los pagos."))
            }
        }
    }

    fun loadMore() {
        val s = _state.value
        if (s.loadingMore || s.loading || !s.hasMore) return
        moreRequest = viewModelScope.launch {
            _state.value = _state.value.copy(loadingMore = true, listError = null)
            try {
                val (desde, hasta) = s.range.dates()
                val response = repo.paymentsForStaff(s.page + 1, s.query.trim().ifEmpty { null }, s.method.param, desde, hasta)
                coroutineContext.ensureActive()
                val known = _state.value.items.map { it.id }.toSet()
                _state.value = _state.value.copy(
                    items = _state.value.items + response.data.filter { it.id !in known },
                    page = response.meta?.currentPage ?: (s.page + 1),
                    lastPage = response.meta?.lastPage ?: s.lastPage,
                    loadingMore = false
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                coroutineContext.ensureActive()
                _state.value = _state.value.copy(loadingMore = false, listError = e.toFriendlyMessage("No se pudieron cargar más pagos."))
            }
        }
    }

    fun setQuery(value: String) {
        _state.value = _state.value.copy(query = value)
    }

    fun setMethod(method: PaymentMethodFilter) {
        if (method == _state.value.method) return
        _state.value = _state.value.copy(method = method)
        load()
    }

    fun setRange(range: ReportRange) {
        if (range == _state.value.range) return
        _state.value = _state.value.copy(range = range)
        load()
    }

    fun clearMessages() {
        _state.value = _state.value.copy(error = null, notice = null)
    }

    /** Cobra en sucursal un adeudo por inasistencia (efectivo o transferencia): el cliente ya puede volver a reservar. */
    fun payFee(id: String, metodo: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(busyFeeId = id, error = null, notice = null)
            try {
                repo.payNoShowFee(id, metodo)
                _state.value = _state.value.copy(
                    fees = _state.value.fees.filterNot { it.id == id },
                    busyFeeId = null,
                    notice = "Cargo cobrado. El cliente ya puede volver a reservar."
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(busyFeeId = null, error = e.toFriendlyMessage("No se pudo registrar el cobro."))
            }
        }
    }

    /** Administración condona un adeudo; el motivo queda registrado. */
    fun waiveFee(id: String, motivo: String, onDone: () -> Unit) {
        val trimmed = motivo.trim()
        if (trimmed.length < 3) {
            _state.value = _state.value.copy(error = "Escribe el motivo para condonar el cargo.")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(busyFeeId = id, error = null, notice = null)
            try {
                repo.waiveNoShowFee(id, trimmed.take(300))
                _state.value = _state.value.copy(
                    fees = _state.value.fees.filterNot { it.id == id },
                    busyFeeId = null,
                    notice = "Cargo condonado."
                )
                onDone()
            } catch (e: Exception) {
                _state.value = _state.value.copy(busyFeeId = null, error = e.toFriendlyMessage("No se pudo condonar el cargo."))
            }
        }
    }

    /** Aprueba una transferencia: queda verificada y el pago resuelto; el barbero inicia y termina el servicio. */
    fun approve(id: String) = review(id, "No se pudo aprobar el comprobante.", "Comprobante aprobado. El pago quedó registrado; el barbero ya puede iniciar la cita.") {
        repo.approvePayment(id)
    }

    /** Rechaza un comprobante con el motivo escrito por el personal; el cliente puede subir otro. */
    fun reject(id: String, reason: String, onDone: () -> Unit) {
        val trimmed = reason.trim()
        if (trimmed.isEmpty()) {
            _state.value = _state.value.copy(error = "Escribe el motivo del rechazo.")
            return
        }
        review(id, "No se pudo rechazar el comprobante.", "Comprobante rechazado. El cliente puede subir uno nuevo.", onDone) {
            repo.rejectPayment(id, trimmed.take(REJECT_REASON_MAX))
        }
    }

    /** Aprueba un anticipo: queda verificado y se descuenta al cobrar; la cita sigue esperando al barbero. */
    fun approveDeposit(id: String) = review(
        id,
        "No se pudo aprobar el anticipo.",
        "Anticipo aprobado. Se descontará al cobrar la cita."
    ) { repo.approveDeposit(id) }

    /** Rechaza un anticipo con motivo; el cliente puede subir otro comprobante. */
    fun rejectDeposit(id: String, reason: String, onDone: () -> Unit) {
        val trimmed = reason.trim()
        if (trimmed.isEmpty()) {
            _state.value = _state.value.copy(error = "Escribe el motivo del rechazo.")
            return
        }
        review(id, "No se pudo rechazar el anticipo.", "Anticipo rechazado. El cliente puede subir otro comprobante.", onDone) {
            repo.rejectDeposit(id, trimmed.take(REJECT_REASON_MAX))
        }
    }

    /** Pide la URL firmada del comprobante de un pago y la entrega para abrirla. */
    fun receiptUrl(id: String, onUrl: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val url = repo.paymentReceiptUrl(id)
                if (url.isNullOrBlank()) {
                    _state.value = _state.value.copy(error = "Este pago no tiene comprobante disponible.")
                } else {
                    onUrl(url)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.toFriendlyMessage("No se pudo abrir el comprobante."))
            }
        }
    }

    private fun review(id: String, failure: String, success: String, onDone: () -> Unit = {}, call: suspend () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(busyPaymentId = id, error = null)
            try {
                call()
                _state.value = _state.value.copy(busyPaymentId = null, notice = success)
                onDone()
                load()
            } catch (e: HttpException) {
                _state.value = _state.value.copy(
                    busyPaymentId = null,
                    error = if (e.code() == 422) e.serverMessage() ?: failure else e.toFriendlyMessage(failure)
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(busyPaymentId = null, error = e.toFriendlyMessage(failure))
            }
        }
    }
}
