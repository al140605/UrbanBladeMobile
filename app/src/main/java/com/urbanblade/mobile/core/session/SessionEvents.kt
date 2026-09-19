package com.urbanblade.mobile.core.session

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Avisos de sesión que nacen en la capa de red y consume la de interfaz. El cliente HTTP no puede
 * navegar ni tocar el estado de la app: solo avisa aquí cuando el servidor rechaza el token.
 */
object SessionEvents {
    private val _expired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val expired: SharedFlow<Unit> = _expired.asSharedFlow()

    fun notifyExpired() {
        _expired.tryEmit(Unit)
    }
}
