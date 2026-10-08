package com.urbanblade.mobile.core.push

import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Pantalla pendiente de abrir cuando el usuario toca una notificación. MainActivity la deja aquí
 * (al abrir la app o con la app ya abierta) y la navegación autenticada la consume una sola vez.
 * Si el usuario aún no inicia sesión, se queda esperando hasta que lo haga.
 */
object PushDeepLink {
    const val EXTRA_ROUTE = "route"

    private val _pending = MutableStateFlow<String?>(null)
    val pending: StateFlow<String?> = _pending

    /** Lee la ruta del intent (extra propio o dato que Firebase copia al tocar la notificación). */
    fun handle(intent: Intent?) {
        val route = safePushRoute(intent?.getStringExtra(EXTRA_ROUTE))
            ?: routeFromDeepLink(intent?.data?.scheme, intent?.data?.host, intent?.data?.getQueryParameter("route"))
        route?.let { _pending.value = it }
    }

    fun consume() {
        _pending.value = null
    }
}
