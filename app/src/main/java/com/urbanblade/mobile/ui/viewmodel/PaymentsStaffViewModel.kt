package com.urbanblade.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.urbanblade.mobile.core.network.AppContainer
import com.urbanblade.mobile.data.model.PaymentRow
import com.urbanblade.mobile.data.model.PaymentsStats
import com.urbanblade.mobile.data.model.PendingPaymentRow
import com.urbanblade.mobile.data.repository.UrbanRepository
import com.urbanblade.mobile.ui.screens.ReportRange
import com.urbanblade.mobile.ui.screens.dates
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
    val query: String = "",
    val method: PaymentMethodFilter = PaymentMethodFilter.Todos,
    val range: ReportRange = ReportRange.All,
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    /** Comprobante en revisión (aprobando o rechazando) para bloquear sus botones. */
    val busyPaymentId: String? = null,
    val error: String? = null,
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

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val s = _state.value
                val (desde, hasta) = s.range.dates()
                val history = repo.paymentsForStaff(1, s.query.trim().ifEmpty { null }, s.method.param, desde, hasta)
                // Si falla la lista de pendientes no se oculta el historial: se conserva la anterior.
                val pending = runCatching { repo.pendingPayments().data }.getOrDefault(_state.value.pending)
                val meta = history.meta
                _state.value = _state.value.copy(
                    items = history.data,
                    stats = meta?.stats ?: PaymentsStats(),
                    total = meta?.total ?: history.data.size,
                    page = meta?.currentPage ?: 1,
                    lastPage = meta?.lastPage ?: 1,
                    pending = pending,
                    loading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.toFriendlyMessage("No se pudieron cargar los pagos."))
            }
        }
    }

    fun loadMore() {
        val s = _state.value
        if (s.loadingMore || s.loading || !s.hasMore) return
        viewModelScope.launch {
            _state.value = _state.value.copy(loadingMore = true)
            try {
                val (desde, hasta) = s.range.dates()
                val response = repo.paymentsForStaff(s.page + 1, s.query.trim().ifEmpty { null }, s.method.param, desde, hasta)
                val known = _state.value.items.map { it.id }.toSet()
                _state.value = _state.value.copy(
                    items = _state.value.items + response.data.filter { it.id !in known },
                    page = response.meta?.currentPage ?: (s.page + 1),
                    lastPage = response.meta?.lastPage ?: s.lastPage,
                    loadingMore = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(loadingMore = false, error = e.toFriendlyMessage("No se pudieron cargar más pagos."))
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

    /** Aprueba una transferencia: el servidor completa la cita y acredita el pago. */
    fun approve(id: String) = review(id, "No se pudo aprobar el comprobante.", "Comprobante aprobado. La cita quedó completada.") {
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
