package com.urbanblade.mobile.core.config

import com.urbanblade.mobile.data.model.ModuleFeatures
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Estado de los interruptores de módulos (`GET barbershop` → `features`), visible para toda la app.
 *
 * Nace con todo apagado y solo se enciende con una respuesta válida del servidor: mientras la petición no ha
 * llegado, o si falla, ningún módulo opcional se muestra (nunca ofrecer algo que el backend no sirve). Se vuelve
 * a pedir al arrancar y cada vez que la app regresa a primer plano, no en cada recomposición.
 */
object ModuleFeaturesStore {
    private val _features = MutableStateFlow(ModuleFeatures())
    val features: StateFlow<ModuleFeatures> = _features.asStateFlow()

    /** Lectura puntual para ViewModels y lógica sin Compose. */
    val current: ModuleFeatures get() = _features.value

    /**
     * Pide los interruptores con [fetch] y los publica. Un fallo (sin red, 5xx, JSON raro) los deja todos
     * apagados; la cancelación se propaga.
     */
    suspend fun refresh(fetch: suspend () -> ModuleFeatures?) {
        _features.value = try {
            fetch() ?: ModuleFeatures()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ModuleFeatures()
        }
    }

    /** Apaga todo (al cerrar sesión no hace falta, pero las pruebas lo usan para partir de cero). */
    fun reset() {
        _features.value = ModuleFeatures()
    }
}
