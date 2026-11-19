package com.urbanblade.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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


class ClientsViewModel @JvmOverloads constructor(private val repo: UrbanRepository = AppContainer.urbanRepository) : ViewModel() {
    private val _hasLoaded = MutableStateFlow(false); val hasLoaded = _hasLoaded.asStateFlow()
    private val _clients = MutableStateFlow(ClientsResponse()); val clients = _clients.asStateFlow()
    private val _detail = MutableStateFlow<ClientDetail?>(null); val detail = _detail.asStateFlow()
    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _saving = MutableStateFlow(false); val saving = _saving.asStateFlow()
    private val _message = MutableStateFlow<String?>(null); val message = _message.asStateFlow()
    /** Errores del detalle y de crear, editar o eliminar; la lista usa [listError]. */
    private val _error = MutableStateFlow<String?>(null); val error = _error.asStateFlow()
    /** Error de la consulta de la lista: no se mezcla con los de las acciones. */
    private val _listError = MutableStateFlow<String?>(null); val listError = _listError.asStateFlow()
    private var loadRequest: Job? = null
    private var moreRequest: Job? = null

    private val _hasMore = MutableStateFlow(false); val hasMore = _hasMore.asStateFlow()
    private val _loadingMore = MutableStateFlow(false); val loadingMore = _loadingMore.asStateFlow()
    private var page = 1
    private var lastSearch: String? = null
    private var lastSegment: String? = null

    /** Cada consulta cancela la anterior (y la paginación en curso): buscar rápido no deja pasar una respuesta vieja. */
    fun load(search: String? = null, segment: String? = null): Job {
        loadRequest?.cancel()
        moreRequest?.cancel()
        _loadingMore.value = false
        _busy.value = true; _error.value = null; _listError.value = null
        lastSearch = search; lastSegment = segment
        val request = viewModelScope.launch {
            try {
                val response = repo.clients(search, segment, 1)
                coroutineContext.ensureActive()
                page = 1
                _clients.value = response
                _hasMore.value = (response.currentPage ?: 1) < (response.lastPage ?: 1)
                _hasLoaded.value = true
            }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                coroutineContext.ensureActive()
                _listError.value = e.toFriendlyMessage("No se pudieron cargar los clientes.")
            }
            finally { if (coroutineContext.isActive) _busy.value = false }
        }
        loadRequest = request
        return request
    }

    /** El servidor entrega 15 clientes por página: esto trae la siguiente y la agrega al final. */
    fun loadMore(): Job {
        if (_loadingMore.value || !_hasMore.value) return viewModelScope.launch { }
        _loadingMore.value = true
        val request = viewModelScope.launch {
            try {
                val response = repo.clients(lastSearch, lastSegment, page + 1)
                coroutineContext.ensureActive()
                page += 1
                val known = _clients.value.data.map { it.id }.toSet()
                _clients.value = _clients.value.copy(data = _clients.value.data + response.data.filter { it.id !in known })
                _hasMore.value = (response.currentPage ?: page) < (response.lastPage ?: page)
            }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                coroutineContext.ensureActive()
                _listError.value = e.toFriendlyMessage("No se pudieron cargar más clientes.")
            }
            finally { if (coroutineContext.isActive) _loadingMore.value = false }
        }
        moreRequest = request
        return request
    }

    fun loadDetail(id: String) = viewModelScope.launch {
        _busy.value = true; _error.value = null; _detail.value = null
        try { _detail.value = repo.clientDetail(id) }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo cargar el cliente.") }
        finally { _busy.value = false }
    }

    /** Al abrir un formulario no debe verse el error de la acción anterior. */
    fun clearError() { _error.value = null }

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

class InventoryViewModel @JvmOverloads constructor(private val repo: UrbanRepository = AppContainer.urbanRepository) : ViewModel() {
    private val _hasLoaded = MutableStateFlow(false); val hasLoaded = _hasLoaded.asStateFlow()
    private val _products = MutableStateFlow(InventoryResponse()); val products = _products.asStateFlow()
    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _saving = MutableStateFlow(false); val saving = _saving.asStateFlow()
    private val _message = MutableStateFlow<String?>(null); val message = _message.asStateFlow()
    /** Errores de guardar, editar, eliminar o registrar un movimiento; la consulta usa [listError]. */
    private val _error = MutableStateFlow<String?>(null); val error = _error.asStateFlow()
    /** Error de la consulta de la lista: no se mezcla con los de las acciones. */
    private val _listError = MutableStateFlow<String?>(null); val listError = _listError.asStateFlow()
    private var loadRequest: Job? = null

    /** Cada consulta cancela la anterior: una respuesta tardía no pisa la última. */
    fun load(): Job {
        loadRequest?.cancel()
        _busy.value = true; _error.value = null; _listError.value = null
        val request = viewModelScope.launch {
            try {
                val result = repo.inventoryProducts()
                coroutineContext.ensureActive()
                _products.value = result; _hasLoaded.value = true
            }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                coroutineContext.ensureActive()
                _listError.value = e.toFriendlyMessage("No se pudo cargar el inventario.")
            }
            finally { if (coroutineContext.isActive) _busy.value = false }
        }
        loadRequest = request
        return request
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

    /** «Ya lo pedí»: el servidor silencia la alerta de ese producto; se recarga para ver «reposición pendiente». */
    fun markOrdered(id: String) = viewModelScope.launch {
        if (_saving.value) return@launch
        _saving.value = true; _error.value = null
        try { _message.value = repo.markProductOrdered(id).message ?: "Producto marcado como pedido."; load() }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo marcar el producto como pedido.") }
        finally { _saving.value = false }
    }

    fun registerMovement(productId: String, tipo: String, cantidad: Int, motivo: String?, onDone: () -> Unit) = viewModelScope.launch {
        _saving.value = true; _error.value = null
        try { repo.registerMovement(productId, tipo, cantidad, motivo); _message.value = "Movimiento registrado."; onDone(); load() }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo registrar el movimiento.") }
        finally { _saving.value = false }
    }
}

class PortfolioViewModel @JvmOverloads constructor(private val repo: UrbanRepository = AppContainer.urbanRepository) : ViewModel() {
    private val _hasLoaded = MutableStateFlow(false); val hasLoaded = _hasLoaded.asStateFlow()
    private val _portfolio = MutableStateFlow(PortfolioResponse()); val portfolio = _portfolio.asStateFlow()
    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _uploading = MutableStateFlow(false); val uploading = _uploading.asStateFlow()
    private val _message = MutableStateFlow<String?>(null); val message = _message.asStateFlow()
    private val _error = MutableStateFlow<String?>(null); val error = _error.asStateFlow()

    fun load() = viewModelScope.launch {
        _busy.value = true; _error.value = null
        try { _portfolio.value = repo.portfolio(); _hasLoaded.value = true }
        catch (e: CancellationException) { throw e }
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
