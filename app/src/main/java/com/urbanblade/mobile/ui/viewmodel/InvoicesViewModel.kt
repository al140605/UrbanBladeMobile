package com.urbanblade.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.urbanblade.mobile.core.network.AppContainer
import com.urbanblade.mobile.data.model.OrderRow
import com.urbanblade.mobile.data.model.PaymentRow
import com.urbanblade.mobile.data.repository.UrbanRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InvoicesState(
    val loading: Boolean = false,
    val error: String? = null,
    val payments: List<PaymentRow> = emptyList(),
    val totalPaid: Double = 0.0,
    val orders: List<OrderRow> = emptyList(),
    /** Id del comprobante que se está generando (pago u orden), para el indicador del botón. */
    val openingId: String? = null,
    val receiptError: String? = null
) {
    val collected get() = payments.filter { it.collected }
    val delivered get() = orders.filter { it.estado == "entregado" }
    val toPickUp get() = orders.filter { it.estado == "pendiente" }
    val productsPaid get() = delivered.sumOf { it.total }
}

/**
 * "Mis facturas" del cliente: comprobantes de sus citas pagadas (GET payments, PDF por
 * payments/{id}/receipt) y de sus compras de productos entregadas (orders/{id}/receipt-link).
 */
class InvoicesViewModel @JvmOverloads constructor(
    private val repo: UrbanRepository = AppContainer.urbanRepository
) : ViewModel() {
    private val _state = MutableStateFlow(InvoicesState())
    val state: StateFlow<InvoicesState> = _state.asStateFlow()

    fun load() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        try {
            val payments = async { repo.payments() }
            // Si los pedidos fallan se siguen mostrando las citas.
            val orders = async { runCatching { repo.orders().data }.getOrDefault(emptyList()) }
            val p = payments.await()
            _state.update { it.copy(payments = p.data, totalPaid = p.meta?.totalPagado ?: p.data.filter { r -> r.collected }.sumOf { r -> r.monto + r.propina }, orders = orders.await()) }
        } catch (e: Exception) {
            _state.update { it.copy(error = e.toFriendlyMessage("No pudimos cargar tus facturas.")) }
        } finally {
            _state.update { it.copy(loading = false) }
        }
    }

    fun openPaymentReceipt(id: String, onUrl: (String) -> Unit) = open(id, onUrl) { repo.paymentReceiptUrl(id) }

    fun openOrderReceipt(id: String, onUrl: (String) -> Unit) = open(id, onUrl) { repo.orderReceiptUrl(id) }

    private fun open(id: String, onUrl: (String) -> Unit, fetch: suspend () -> String?) = viewModelScope.launch {
        if (_state.value.openingId != null) return@launch
        _state.update { it.copy(openingId = id, receiptError = null) }
        try {
            val url = fetch()
            if (url == null) _state.update { it.copy(receiptError = "No se pudo generar el comprobante. Intenta de nuevo.") } else onUrl(url)
        } catch (e: Exception) {
            _state.update { it.copy(receiptError = e.toFriendlyMessage("No se pudo abrir el comprobante. Intenta de nuevo.")) }
        } finally {
            _state.update { it.copy(openingId = null) }
        }
    }
}
