package com.urbanblade.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.urbanblade.mobile.core.network.AppContainer
import com.urbanblade.mobile.data.model.ClientLoyalty
import com.urbanblade.mobile.data.repository.UrbanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Nivel y puntos del cliente para el Inicio (la Wallet completa ya no es pestaña). Si falla,
 * la tarjeta simplemente no se muestra: no es algo que bloquee el Inicio.
 */
class LoyaltySummaryViewModel @JvmOverloads constructor(
    private val repo: UrbanRepository = AppContainer.urbanRepository
) : ViewModel() {
    private val _loyalty = MutableStateFlow<ClientLoyalty?>(null)
    val loyalty: StateFlow<ClientLoyalty?> = _loyalty.asStateFlow()

    fun load() = viewModelScope.launch {
        _loyalty.value = runCatching { repo.clientLoyalty() }.getOrNull()
    }
}
