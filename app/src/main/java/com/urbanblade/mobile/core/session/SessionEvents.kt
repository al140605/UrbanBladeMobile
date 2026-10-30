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

    private val _reachable = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    /** El servidor contestó bien a una llamada: ya hay conexión, aunque se haya entrado con datos guardados. */
    val reachable: SharedFlow<Unit> = _reachable.asSharedFlow()

    fun notifyReachable() {
        _reachable.tryEmit(Unit)
    }
}
