package com.urbanblade.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import com.urbanblade.mobile.core.network.AppContainer
import com.urbanblade.mobile.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.ensureActive
import com.urbanblade.mobile.data.repository.UrbanRepository


class StoreViewModel @JvmOverloads constructor(private val repo: UrbanRepository = AppContainer.urbanRepository) : ViewModel() {
    private val _products = MutableStateFlow<List<ProductItem>>(emptyList())
    val products = _products.asStateFlow()
    private val _knownProducts = MutableStateFlow<Map<String, ProductItem>>(emptyMap())
    val knownProducts = _knownProducts.asStateFlow()
    private val _cart = MutableStateFlow<Map<String, Int>>(emptyMap())
    val cart = _cart.asStateFlow()
    val estimatedTotal: Double get() = _cart.value.entries.sumOf { (id, quantity) -> (_knownProducts.value[id]?.precioVenta ?: 0.0) * quantity }
    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _message = MutableStateFlow<String?>(null); val message = _message.asStateFlow()
    private val _error = MutableStateFlow<String?>(null); val error = _error.asStateFlow()
    private var loadRequest: Job? = null
    private var checkoutRequest: Job? = null

    fun load(q: String? = null) {
        if (checkoutRequest?.isActive == true) return
        loadRequest?.cancel()
        _busy.value = true; _error.value = null
        loadRequest = viewModelScope.launch {
            try {
                val loaded = repo.products(q)
                ensureActive()
                _products.value = loaded
                _knownProducts.value = _knownProducts.value + loaded.associateBy { it.id }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo cargar la tienda.") }
            finally { if (coroutineContext[Job]?.isActive == true) _busy.value = false }
        }
    }
    fun add(id: String) {
        if (_busy.value) return
        val product = _knownProducts.value[id] ?: return
        val quantity = _cart.value[id] ?: 0
        if (quantity >= product.stockActual) return
        _cart.value = _cart.value + (id to quantity + 1)
    }
    fun remove(id: String) {
        if (_busy.value) return
        val quantity = (_cart.value[id] ?: 0) - 1
        _cart.value = if (quantity <= 0) _cart.value - id else _cart.value + (id to quantity)
    }
    fun clear() { _cart.value = emptyMap() }
    fun checkout(onDone: () -> Unit) {
        if (_busy.value || checkoutRequest?.isActive == true) return
        if (_cart.value.isEmpty()) { _error.value = "Tu carrito está vacío."; return }
        val body = OrderRequest(_cart.value.map { OrderItemRequest(it.key, it.value) })
        _busy.value = true; _error.value = null
        checkoutRequest = viewModelScope.launch {
            try {
                val response = repo.createOrder(body)
                _message.value = response.message ?: "Pedido creado."
                clear(); onDone()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo crear el pedido.") }
            finally { _busy.value = false }
        }
    }
}

class NotificationsViewModel @JvmOverloads constructor(private val repo: UrbanRepository = AppContainer.urbanRepository) : ViewModel() {
    private val _data = MutableStateFlow<JsonObject?>(null); val data = _data.asStateFlow()
    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null); val error = _error.asStateFlow()
    private var loadRequest: Job? = null
    private var readRequest: Job? = null

    fun load() {
        if (readRequest?.isActive == true) return
        loadRequest?.cancel()
        _busy.value = true; _error.value = null
        loadRequest = viewModelScope.launch {
            try {
                val response = repo.notifications()
                ensureActive()
                _data.value = response
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudieron cargar las notificaciones.") }
            finally { if (coroutineContext[Job]?.isActive == true) _busy.value = false }
        }
    }

    fun readAll() = markRead(null)
    fun read(id: String) = markRead(id)
    fun delete(id: String) = markRead(id, delete = true)

    private fun markRead(id: String?, delete: Boolean = false) {
        if (_busy.value) return
        _busy.value = true; _error.value = null
        readRequest = viewModelScope.launch {
            try {
                if (delete && id != null) repo.deleteNotification(id)
                else if (id == null) repo.markAllNotificationsRead() else repo.markNotificationRead(id)
                val response = repo.notifications()
                ensureActive()
                _data.value = response
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo actualizar el estado de tus avisos. Reintenta la consulta.") }
            finally { _busy.value = false }
        }
    }
}

class BarberScheduleViewModel : ViewModel() {
    private val repo = AppContainer.urbanRepository
    private val _schedules = MutableStateFlow<List<BarberScheduleDay>>(emptyList()); val schedules = _schedules.asStateFlow()
    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _saving = MutableStateFlow(false); val saving = _saving.asStateFlow()
    private val _message = MutableStateFlow<String?>(null); val message = _message.asStateFlow()
    private val _error = MutableStateFlow<String?>(null); val error = _error.asStateFlow()

    fun load() = viewModelScope.launch {
        _busy.value = true; _error.value = null
        try { _schedules.value = repo.barberSchedule() }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo cargar tu horario.") }
        finally { _busy.value = false }
    }

    fun save(schedules: List<BarberScheduleDay>) = viewModelScope.launch {
        _saving.value = true; _error.value = null; _message.value = null
        try {
            repo.updateBarberSchedule(schedules)
            // Lo que se muestra es lo que quedó guardado: se vuelve a consultar en lugar de fiarse de la respuesta de PUT.
            _schedules.value = repo.barberSchedule()
            _message.value = "Horario actualizado."
        } catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo guardar tu horario.") }
        finally { _saving.value = false }
    }
}

class CashCloseViewModel : ViewModel() {
    private val repo = AppContainer.urbanRepository
    private val _preview = MutableStateFlow(CashClosePreview()); val preview = _preview.asStateFlow()
    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _closing = MutableStateFlow(false); val closing = _closing.asStateFlow()
    private val _message = MutableStateFlow<String?>(null); val message = _message.asStateFlow()
    private val _error = MutableStateFlow<String?>(null); val error = _error.asStateFlow()

    fun load() = viewModelScope.launch {
        _busy.value = true; _error.value = null
        try { _preview.value = repo.cashClosePreview() }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo cargar el corte de caja.") }
        finally { _busy.value = false }
    }

    fun close(efectivoContado: Double, notas: String?) = viewModelScope.launch {
        _closing.value = true; _error.value = null; _message.value = null
        try {
            repo.registerCashClose(efectivoContado, notas)
            _message.value = "Caja cerrada correctamente."
            load()
        } catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo cerrar la caja.") }
        finally { _closing.value = false }
    }
}

class AdminMetricsViewModel : ViewModel() {
    private val repo = AppContainer.urbanRepository
    private val _metrics = MutableStateFlow(AdminMetrics()); val metrics = _metrics.asStateFlow()
    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null); val error = _error.asStateFlow()
    fun load() = viewModelScope.launch { _busy.value = true; _error.value = null; try { _metrics.value = repo.adminMetrics() } catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudieron cargar las métricas.") } finally { _busy.value = false } }
}

class SystemStatusViewModel : ViewModel() {
    private val repo = AppContainer.urbanRepository
    private val _status = MutableStateFlow<SystemStatusResponse?>(null); val status = _status.asStateFlow()
    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null); val error = _error.asStateFlow()
    fun load() = viewModelScope.launch { _busy.value = true; _error.value = null; try { _status.value = repo.systemStatus() } catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo cargar el estado del sistema.") } finally { _busy.value = false } }
}

class RafflesViewModel @JvmOverloads constructor(private val repo: UrbanRepository = AppContainer.urbanRepository) : ViewModel() {
    private val _hasLoaded = MutableStateFlow(false); val hasLoaded = _hasLoaded.asStateFlow()
    private val _raffles = MutableStateFlow(RaffleResponse()); val raffles = _raffles.asStateFlow()
    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null); val error = _error.asStateFlow()
    private var loadRequest: Job? = null

    fun load() {
        loadRequest?.cancel()
        _busy.value = true
        _error.value = null
        loadRequest = viewModelScope.launch {
            try {
                val result = repo.raffles()
                coroutineContext.ensureActive()
                _raffles.value = result
                _hasLoaded.value = true
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                coroutineContext.ensureActive()
                _error.value = e.toFriendlyMessage("No se pudieron cargar los sorteos.")
            } finally { if (coroutineContext.isActive) _busy.value = false }
        }
    }
}
