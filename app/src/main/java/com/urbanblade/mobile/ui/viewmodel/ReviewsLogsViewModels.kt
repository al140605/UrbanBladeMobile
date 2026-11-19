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


class ReviewsViewModel @JvmOverloads constructor(private val repo: UrbanRepository = AppContainer.urbanRepository) : ViewModel() {
    private val _hasLoaded = MutableStateFlow(false); val hasLoaded = _hasLoaded.asStateFlow()
    private val _reviews = MutableStateFlow(ReviewsResponse()); val reviews = _reviews.asStateFlow()
    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null); val error = _error.asStateFlow()
    private var loadRequest: Job? = null

    fun load(barberId: String? = null, rating: Int? = null) {
        loadRequest?.cancel()
        _busy.value = true
        _error.value = null
        loadRequest = viewModelScope.launch {
            try {
                val result = repo.reviews(barberId, rating)
                coroutineContext.ensureActive()
                _reviews.value = result
                _hasLoaded.value = true
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                coroutineContext.ensureActive()
                _error.value = e.toFriendlyMessage("No se pudieron cargar las reseñas.")
            } finally { if (coroutineContext.isActive) _busy.value = false }
        }
    }
}

class LogsViewModel @JvmOverloads constructor(private val repo: UrbanRepository = AppContainer.urbanRepository) : ViewModel() {
    private val _hasLoaded = MutableStateFlow(false); val hasLoaded = _hasLoaded.asStateFlow()
    private val _logs = MutableStateFlow(LogsResponse()); val logs = _logs.asStateFlow()
    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null); val error = _error.asStateFlow()
    private var loadRequest: Job? = null

    fun load(search: String? = null, logName: String? = null, event: String? = null) {
        loadRequest?.cancel()
        _busy.value = true
        _error.value = null
        loadRequest = viewModelScope.launch {
            try {
                val result = repo.logs(search, logName, event)
                coroutineContext.ensureActive()
                _logs.value = result
                _hasLoaded.value = true
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                coroutineContext.ensureActive()
                _error.value = e.toFriendlyMessage("No se pudieron cargar los logs.")
            } finally { if (coroutineContext.isActive) _busy.value = false }
        }
    }
}
