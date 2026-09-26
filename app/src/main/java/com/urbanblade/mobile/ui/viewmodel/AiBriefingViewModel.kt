package com.urbanblade.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.urbanblade.mobile.core.network.AppContainer
import com.urbanblade.mobile.data.model.AiBriefing
import com.urbanblade.mobile.data.repository.UrbanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Resumen del día (GET ai/briefing). Es un extra: si falla o está vacío queda en null y la
 * tarjeta simplemente no aparece; nunca muestra error ni bloquea el inicio.
 */
class AiBriefingViewModel @JvmOverloads constructor(
    private val repo: UrbanRepository = AppContainer.urbanRepository
) : ViewModel() {
    private val _briefing = MutableStateFlow<AiBriefing?>(null)
    val briefing: StateFlow<AiBriefing?> = _briefing.asStateFlow()

    fun load() = viewModelScope.launch {
        _briefing.value = runCatching { repo.aiBriefing() }.getOrNull()?.takeIf { it.text.isNotBlank() }
    }
}
