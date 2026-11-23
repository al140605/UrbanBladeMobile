package com.urbanblade.mobile.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination

/** Los atajos hacia una pestaña usan la misma pila y restauración que la barra inferior. */
internal fun NavController.openDestination(
    route: String,
    rootRoutes: List<String>,
    singleTop: Boolean = false
) {
    navigate(route) {
        launchSingleTop = singleTop
        if (route in rootRoutes) {
            val start = graph.findStartDestination()
            popUpTo(start.id) { saveState = true }
            launchSingleTop = true
            // Inicio permanece en la pila. Restaurarlo puede reabrir un módulo secundario
            // guardado desde él (por ejemplo, Notificaciones al entrar al Muro).
            restoreState = route != start.route
        }
    }
}

/**
 * Ruta que se abre de verdad dentro de la sesión. Para el barbero que no administra ni atiende recepción, su
 * agenda es la pestaña «Agenda» (`appointments`): `barber_agenda`, que siguen mandando los push, los correos
 * (`urbanblade://open?route=…`) y la bandeja, se redirige ahí para no abrir la misma pantalla dos veces.
 * Para quien además administra o atiende recepción, `barber_agenda` sigue siendo su agenda personal, distinta
 * de la del negocio, y no se toca.
 */
internal fun canonicalRoute(route: String, barberOnly: Boolean): String =
    if (barberOnly && route == "barber_agenda") "appointments" else route
