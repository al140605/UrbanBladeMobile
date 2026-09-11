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

class GenericModuleViewModel : ViewModel() {
    private val repo = AppContainer.urbanRepository
    private val _data = MutableStateFlow<JsonObject?>(null); val data = _data.asStateFlow()
    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null); val error = _error.asStateFlow()
    fun load(endpoint:String)=viewModelScope.launch{_busy.value=true;_error.value=null;try{_data.value=repo.module(endpoint)}catch(e:Exception){_error.value=e.toFriendlyMessage("No se pudo cargar el módulo.")}finally{_busy.value=false}}
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
