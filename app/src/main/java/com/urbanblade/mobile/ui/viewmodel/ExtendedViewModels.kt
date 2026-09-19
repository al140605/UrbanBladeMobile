package com.urbanblade.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import com.urbanblade.mobile.core.network.AppContainer
import com.urbanblade.mobile.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StoreViewModel : ViewModel() {
    private val repo = AppContainer.urbanRepository
    private val _products = MutableStateFlow<List<ProductItem>>(emptyList())
    val products = _products.asStateFlow()
    private val _cart = MutableStateFlow<Map<String, Int>>(emptyMap())
    val cart = _cart.asStateFlow()
    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _message = MutableStateFlow<String?>(null); val message = _message.asStateFlow()
    private val _error = MutableStateFlow<String?>(null); val error = _error.asStateFlow()

    fun load(q: String? = null) = viewModelScope.launch {
        _busy.value = true; _error.value = null
        try { _products.value = repo.products(q) }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo cargar la tienda.") }
        finally { _busy.value = false }
    }
    fun add(id: String) { _cart.value = _cart.value.toMutableMap().apply { this[id] = (this[id] ?: 0) + 1 } }
    fun remove(id: String) { _cart.value = _cart.value.toMutableMap().apply { val n=(this[id]?:0)-1; if(n<=0) remove(id) else this[id]=n } }
    fun clear() { _cart.value = emptyMap() }
    fun checkout(onDone: () -> Unit) = viewModelScope.launch {
        if (_cart.value.isEmpty()) { _error.value = "Tu carrito está vacío."; return@launch }
        _busy.value = true; _error.value = null
        try {
            val body = OrderRequest(_cart.value.map { OrderItemRequest(it.key, it.value) })
            val res = repo.createOrder(body)
            _message.value = res.message ?: "Pedido creado."
            clear(); onDone()
        } catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo crear el pedido.") }
        finally { _busy.value = false }
    }
}

class OrdersViewModel : ViewModel() {
    private val repo = AppContainer.urbanRepository
    private val _data = MutableStateFlow(OrdersResponse()); val data = _data.asStateFlow()
    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null); val error = _error.asStateFlow()
    fun load() = viewModelScope.launch { _busy.value=true; try { _data.value=repo.orders() } catch(e:Exception){_error.value=e.toFriendlyMessage("No se pudieron cargar los pedidos.")} finally{_busy.value=false} }
    fun cancel(id:String)= viewModelScope.launch { try { repo.cancelOrder(id); load() } catch(e:Exception){_error.value=e.toFriendlyMessage("No se pudo cancelar.")} }
    fun deliver(id:String, method:String)= viewModelScope.launch { try { repo.deliverOrder(id,method); load() } catch(e:Exception){_error.value=e.toFriendlyMessage("No se pudo entregar.")} }
}

class PaymentsViewModel : ViewModel() {
    private val repo = AppContainer.urbanRepository
    private val _payments = MutableStateFlow(PaymentsResponse()); val payments = _payments.asStateFlow()
    private val _pending = MutableStateFlow(PendingPaymentsResponse()); val pending = _pending.asStateFlow()
    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null); val error = _error.asStateFlow()
    fun load(staff:Boolean)=viewModelScope.launch { _busy.value=true; _error.value=null; try { _payments.value=repo.payments(); if(staff)_pending.value=repo.pendingPayments() } catch(e:Exception){_error.value=e.toFriendlyMessage("No se pudieron cargar los pagos.")} finally{_busy.value=false} }
    fun approve(id:String,staff:Boolean)=viewModelScope.launch{ try{repo.approvePayment(id);load(staff)}catch(e:Exception){_error.value=e.toFriendlyMessage("No se pudo aprobar.")} }
    fun reject(id:String,reason:String,staff:Boolean)=viewModelScope.launch{try{repo.rejectPayment(id,reason);load(staff)}catch(e:Exception){_error.value=e.toFriendlyMessage("No se pudo rechazar.")}}
}

class NotificationsViewModel : ViewModel() {
    private val repo = AppContainer.urbanRepository
    private val _data = MutableStateFlow<JsonObject?>(null); val data = _data.asStateFlow()
    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null); val error = _error.asStateFlow()
    fun load()=viewModelScope.launch{_busy.value=true;try{_data.value=repo.notifications()}catch(e:Exception){_error.value=e.toFriendlyMessage("No se pudieron cargar las notificaciones.")}finally{_busy.value=false}}
    fun readAll()=viewModelScope.launch{try{repo.markAllNotificationsRead();load()}catch(e:Exception){_error.value=e.toFriendlyMessage("No se pudieron marcar.")}}
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
            _schedules.value = repo.updateBarberSchedule(schedules)
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

class RafflesViewModel : ViewModel() {
    private val repo = AppContainer.urbanRepository
    private val _raffles = MutableStateFlow(RaffleResponse()); val raffles = _raffles.asStateFlow()
    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null); val error = _error.asStateFlow()
    fun load() = viewModelScope.launch { _busy.value = true; _error.value = null; try { _raffles.value = repo.raffles() } catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudieron cargar los sorteos.") } finally { _busy.value = false } }
}

class ClientsViewModel : ViewModel() {
    private val repo = AppContainer.urbanRepository
    private val _clients = MutableStateFlow(ClientsResponse()); val clients = _clients.asStateFlow()
    private val _detail = MutableStateFlow<ClientDetail?>(null); val detail = _detail.asStateFlow()
    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _saving = MutableStateFlow(false); val saving = _saving.asStateFlow()
    private val _message = MutableStateFlow<String?>(null); val message = _message.asStateFlow()
    private val _error = MutableStateFlow<String?>(null); val error = _error.asStateFlow()

    private val _hasMore = MutableStateFlow(false); val hasMore = _hasMore.asStateFlow()
    private val _loadingMore = MutableStateFlow(false); val loadingMore = _loadingMore.asStateFlow()
    private var page = 1
    private var lastSearch: String? = null
    private var lastSegment: String? = null

    fun load(search: String? = null, segment: String? = null) = viewModelScope.launch {
        _busy.value = true; _error.value = null
        lastSearch = search; lastSegment = segment
        try {
            val response = repo.clients(search, segment, 1)
            page = 1
            _clients.value = response
            _hasMore.value = (response.currentPage ?: 1) < (response.lastPage ?: 1)
        }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudieron cargar los clientes.") }
        finally { _busy.value = false }
    }

    /** El servidor entrega 15 clientes por página: esto trae la siguiente y la agrega al final. */
    fun loadMore() = viewModelScope.launch {
        if (_loadingMore.value || !_hasMore.value) return@launch
        _loadingMore.value = true
        try {
            val response = repo.clients(lastSearch, lastSegment, page + 1)
            page += 1
            val known = _clients.value.data.map { it.id }.toSet()
            _clients.value = _clients.value.copy(data = _clients.value.data + response.data.filter { it.id !in known })
            _hasMore.value = (response.currentPage ?: page) < (response.lastPage ?: page)
        }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudieron cargar más clientes.") }
        finally { _loadingMore.value = false }
    }

    fun loadDetail(id: String) = viewModelScope.launch {
        _busy.value = true; _error.value = null; _detail.value = null
        try { _detail.value = repo.clientDetail(id) }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo cargar el cliente.") }
        finally { _busy.value = false }
    }

    fun create(name: String, email: String, telefono: String?, password: String, onDone: () -> Unit) = viewModelScope.launch {
        _saving.value = true; _error.value = null
        try {
            repo.createClient(name, email, telefono, password)
            _message.value = "Cliente creado."
            onDone()
        } catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo crear el cliente.") }
        finally { _saving.value = false }
    }

    fun update(id: String, name: String?, email: String?, telefono: String?, notas: String?) = viewModelScope.launch {
        _saving.value = true; _error.value = null
        try {
            repo.updateClient(id, name, email, telefono, notas)
            _message.value = "Cliente actualizado."
            loadDetail(id)
        } catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo actualizar el cliente.") }
        finally { _saving.value = false }
    }

    fun delete(id: String, onDone: () -> Unit) = viewModelScope.launch {
        _saving.value = true; _error.value = null
        try {
            repo.deleteClient(id)
            onDone()
        } catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo eliminar el cliente.") }
        finally { _saving.value = false }
    }
}

class InventoryViewModel : ViewModel() {
    private val repo = AppContainer.urbanRepository
    private val _products = MutableStateFlow(InventoryResponse()); val products = _products.asStateFlow()
    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _saving = MutableStateFlow(false); val saving = _saving.asStateFlow()
    private val _message = MutableStateFlow<String?>(null); val message = _message.asStateFlow()
    private val _error = MutableStateFlow<String?>(null); val error = _error.asStateFlow()

    fun load() = viewModelScope.launch {
        _busy.value = true; _error.value = null
        try { _products.value = repo.inventoryProducts() }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo cargar el inventario.") }
        finally { _busy.value = false }
    }

    fun createProduct(context: android.content.Context, body: CreateProductRequest, imageUri: android.net.Uri?, onDone: () -> Unit) = viewModelScope.launch {
        _saving.value = true; _error.value = null
        try { repo.createProduct(context, body, imageUri); _message.value = "Producto creado."; onDone(); load() }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo crear el producto.") }
        finally { _saving.value = false }
    }

    fun updateProduct(context: android.content.Context, id: String, body: CreateProductRequest, imageUri: android.net.Uri?, onDone: () -> Unit) = viewModelScope.launch {
        _saving.value = true; _error.value = null
        try { repo.updateProduct(context, id, body, imageUri); _message.value = "Producto actualizado."; onDone(); load() }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo actualizar el producto.") }
        finally { _saving.value = false }
    }

    fun deleteProduct(id: String) = viewModelScope.launch {
        _saving.value = true; _error.value = null
        try { repo.deleteProduct(id); load() }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo eliminar el producto.") }
        finally { _saving.value = false }
    }

    fun registerMovement(productId: String, tipo: String, cantidad: Int, motivo: String?, onDone: () -> Unit) = viewModelScope.launch {
        _saving.value = true; _error.value = null
        try { repo.registerMovement(productId, tipo, cantidad, motivo); _message.value = "Movimiento registrado."; onDone(); load() }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo registrar el movimiento.") }
        finally { _saving.value = false }
    }
}

class PortfolioViewModel : ViewModel() {
    private val repo = AppContainer.urbanRepository
    private val _portfolio = MutableStateFlow(PortfolioResponse()); val portfolio = _portfolio.asStateFlow()
    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _uploading = MutableStateFlow(false); val uploading = _uploading.asStateFlow()
    private val _message = MutableStateFlow<String?>(null); val message = _message.asStateFlow()
    private val _error = MutableStateFlow<String?>(null); val error = _error.asStateFlow()

    fun load() = viewModelScope.launch {
        _busy.value = true; _error.value = null
        try { _portfolio.value = repo.portfolio() }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo cargar tu portafolio.") }
        finally { _busy.value = false }
    }

    fun upload(context: android.content.Context, title: String, description: String?, media: List<android.net.Uri>, onDone: () -> Unit) = viewModelScope.launch {
        _uploading.value = true; _error.value = null
        try {
            val res = repo.createWork(context, title, description, media)
            _message.value = res.message
            onDone()
            load()
        } catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo subir el trabajo.") }
        finally { _uploading.value = false }
    }

    fun delete(id: String) = viewModelScope.launch {
        _busy.value = true; _error.value = null
        try { repo.deleteWork(id); load() }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo eliminar el trabajo.") }
        finally { _busy.value = false }
    }
}

class CampaignsViewModel : ViewModel() {
    private val repo = AppContainer.urbanRepository
    private val _campaigns = MutableStateFlow(CampaignsResponse()); val campaigns = _campaigns.asStateFlow()
    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _sending = MutableStateFlow(false); val sending = _sending.asStateFlow()
    private val _message = MutableStateFlow<String?>(null); val message = _message.asStateFlow()
    private val _error = MutableStateFlow<String?>(null); val error = _error.asStateFlow()

    fun load() = viewModelScope.launch {
        _busy.value = true; _error.value = null
        try { _campaigns.value = repo.campaigns() }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudieron cargar las campañas.") }
        finally { _busy.value = false }
    }

    fun create(body: CreateCampaignRequest, onDone: () -> Unit) = viewModelScope.launch {
        _sending.value = true; _error.value = null
        try {
            val res = repo.createCampaign(body)
            _message.value = res.message
            onDone()
            load()
        } catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo crear la campaña.") }
        finally { _sending.value = false }
    }
}

class BarberAgendaViewModel : ViewModel() {
    private val repo = AppContainer.urbanRepository
    private val _agenda = MutableStateFlow(BarberAgendaResponse()); val agenda = _agenda.asStateFlow()
    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _updating = MutableStateFlow<String?>(null); val updating = _updating.asStateFlow()
    private val _error = MutableStateFlow<String?>(null); val error = _error.asStateFlow()

    fun load(period: String = "day", estado: String? = null, offset: Int = 0) = viewModelScope.launch {
        _busy.value = true; _error.value = null
        try { _agenda.value = repo.barberAgenda(period, estado, offset) }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo cargar tu agenda.") }
        finally { _busy.value = false }
    }

    fun updateStatus(code: String, estado: String, period: String, filtroEstado: String?, offset: Int) = viewModelScope.launch {
        _updating.value = code; _error.value = null
        try { repo.updateAppointmentStatus(code, estado); load(period, filtroEstado, offset) }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo actualizar la cita.") }
        finally { _updating.value = null }
    }
}

class ReportsViewModel : ViewModel() {
    private val repo = AppContainer.urbanRepository
    private val _manifest = MutableStateFlow(ReportManifest()); val manifest = _manifest.asStateFlow()
    private val _report = MutableStateFlow<ReportData?>(null); val report = _report.asStateFlow()
    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null); val error = _error.asStateFlow()

    fun loadManifest() = viewModelScope.launch {
        try { _manifest.value = repo.reportManifest() }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudieron cargar los tipos de reporte.") }
    }

    fun generate(type: String, startDate: String?, endDate: String?) = viewModelScope.launch {
        _busy.value = true; _error.value = null; _report.value = null
        try { _report.value = repo.exportReport(type, "json", startDate, endDate) }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo generar el reporte.") }
        finally { _busy.value = false }
    }
}

class SettingsViewModel : ViewModel() {
    private val repo = AppContainer.urbanRepository
    private val _setting = MutableStateFlow(BarbershopSetting()); val setting = _setting.asStateFlow()
    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _saving = MutableStateFlow(false); val saving = _saving.asStateFlow()
    private val _message = MutableStateFlow<String?>(null); val message = _message.asStateFlow()
    private val _error = MutableStateFlow<String?>(null); val error = _error.asStateFlow()

    fun load() = viewModelScope.launch {
        _busy.value = true; _error.value = null
        try { _setting.value = repo.settings() }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo cargar la configuración.") }
        finally { _busy.value = false }
    }

    fun save(body: UpdateSettingRequest) = viewModelScope.launch {
        _saving.value = true; _error.value = null
        try { _setting.value = repo.updateSettings(body); _message.value = "Configuración actualizada." }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo guardar la configuración.") }
        finally { _saving.value = false }
    }

    fun toggleMaintenance() = viewModelScope.launch {
        _saving.value = true; _error.value = null
        try {
            val res = repo.toggleMaintenance()
            _setting.value = _setting.value.copy(maintenanceMode = res.data.maintenanceMode)
            _message.value = res.message
        } catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo cambiar el modo mantenimiento.") }
        finally { _saving.value = false }
    }
}

class SocialFeedViewModel : ViewModel() {
    private val repo = AppContainer.urbanRepository
    private val _feed = MutableStateFlow(SocialFeedResponse()); val feed = _feed.asStateFlow()
    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null); val error = _error.asStateFlow()

    fun load() = viewModelScope.launch {
        _busy.value = true; _error.value = null
        try { _feed.value = repo.socialFeed() }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo cargar el muro social.") }
        finally { _busy.value = false }
    }

    private fun updateWork(id: String, transform: (SocialWork) -> SocialWork) {
        _feed.value = _feed.value.copy(data = _feed.value.data.map { if (it.id == id) transform(it) else it })
    }

    fun toggleReaction(id: String) = viewModelScope.launch {
        try {
            val res = repo.reactWork(id)
            updateWork(id) { it.copy(isReacted = res.status == "added", reactionsCount = res.count ?: it.reactionsCount) }
        } catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo reaccionar.") }
    }

    fun toggleSave(id: String) = viewModelScope.launch {
        try {
            val res = repo.saveWork(id)
            val added = res.status == "added"
            updateWork(id) { it.copy(isSaved = added, savedCount = it.savedCount + if (added) 1 else -1) }
        } catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo guardar.") }
    }

    fun comment(id: String, text: String, onDone: () -> Unit) = viewModelScope.launch {
        if (text.isBlank()) return@launch
        try {
            val res = repo.commentWork(id, text)
            val newComment = res.data
            updateWork(id) {
                it.copy(
                    commentsCount = it.commentsCount + 1,
                    comments = if (newComment != null) (listOf(newComment) + it.comments).take(3) else it.comments
                )
            }
            onDone()
        } catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo publicar el comentario.") }
    }
}

class InsightsViewModel : ViewModel() {
    private val repo = AppContainer.urbanRepository
    private val _data = MutableStateFlow(InsightsData()); val data = _data.asStateFlow()
    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null); val error = _error.asStateFlow()
    fun load() = viewModelScope.launch {
        _busy.value = true; _error.value = null
        try { _data.value = repo.predictionInsights() }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudieron generar los insights.") }
        finally { _busy.value = false }
    }
}

class AnalyticsViewModel : ViewModel() {
    private val repo = AppContainer.urbanRepository
    private val _data = MutableStateFlow(AnalyticsResponse()); val data = _data.asStateFlow()
    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null); val error = _error.asStateFlow()
    fun load() = viewModelScope.launch {
        _busy.value = true; _error.value = null
        try { _data.value = repo.analytics() }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo cargar la analítica.") }
        finally { _busy.value = false }
    }
}

class ReviewsViewModel : ViewModel() {
    private val repo = AppContainer.urbanRepository
    private val _reviews = MutableStateFlow(ReviewsResponse()); val reviews = _reviews.asStateFlow()
    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null); val error = _error.asStateFlow()
    fun load(barberId: String? = null, rating: Int? = null) = viewModelScope.launch {
        _busy.value = true; _error.value = null
        try { _reviews.value = repo.reviews(barberId, rating) }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudieron cargar las reseñas.") }
        finally { _busy.value = false }
    }
}

class LogsViewModel : ViewModel() {
    private val repo = AppContainer.urbanRepository
    private val _logs = MutableStateFlow(LogsResponse()); val logs = _logs.asStateFlow()
    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null); val error = _error.asStateFlow()
    fun load(search: String? = null, logName: String? = null, event: String? = null) = viewModelScope.launch {
        _busy.value = true; _error.value = null
        try { _logs.value = repo.logs(search, logName, event) }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudieron cargar los logs.") }
        finally { _busy.value = false }
    }
}

class SystemUsersViewModel : ViewModel() {
    private val repo = AppContainer.urbanRepository
    private val _users = MutableStateFlow(SystemUsersResponse()); val users = _users.asStateFlow()
    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _saving = MutableStateFlow(false); val saving = _saving.asStateFlow()
    private val _message = MutableStateFlow<String?>(null); val message = _message.asStateFlow()
    private val _error = MutableStateFlow<String?>(null); val error = _error.asStateFlow()

    fun load(search: String? = null, role: String? = null) = viewModelScope.launch {
        _busy.value = true; _error.value = null
        try { _users.value = repo.systemUsers(search, role) }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudieron cargar los usuarios.") }
        finally { _busy.value = false }
    }

    fun create(name: String, email: String, password: String, role: String, onDone: () -> Unit) = viewModelScope.launch {
        _saving.value = true; _error.value = null
        try { repo.createUser(name, email, password, role); _message.value = "Usuario creado."; onDone(); load() }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo crear el usuario.") }
        finally { _saving.value = false }
    }

    fun update(id: String, name: String, email: String, password: String?, role: String, onDone: () -> Unit) = viewModelScope.launch {
        _saving.value = true; _error.value = null
        try { repo.updateUser(id, name, email, password, role); _message.value = "Usuario actualizado."; onDone(); load() }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo actualizar el usuario.") }
        finally { _saving.value = false }
    }

    fun delete(id: String) = viewModelScope.launch {
        _saving.value = true; _error.value = null
        try { repo.deleteUser(id); load() }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo eliminar el usuario.") }
        finally { _saving.value = false }
    }
}

class ChatbotViewModel : ViewModel() {
    private val repo=AppContainer.urbanRepository
    private val _messages=MutableStateFlow<List<Pair<Boolean,String>>>(emptyList()); val messages=_messages.asStateFlow()
    private val _busy=MutableStateFlow(false); val busy=_busy.asStateFlow()
    fun send(text:String)=viewModelScope.launch{
        if(text.isBlank())return@launch
        _messages.value += true to text; _busy.value=true
        try { val r=repo.chatbot(text); val answer = sequenceOf("answer","response","message","reply").mapNotNull { k-> r.get(k)?.takeIf{!it.isJsonNull}?.asString }.firstOrNull() ?: r.toString(); _messages.value += false to answer }
        catch(e:Exception){_messages.value += false to e.toFriendlyMessage("No pude responder ahora.")}
        finally{_busy.value=false}
    }
}
