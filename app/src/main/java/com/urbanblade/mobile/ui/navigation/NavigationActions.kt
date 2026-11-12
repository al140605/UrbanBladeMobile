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
