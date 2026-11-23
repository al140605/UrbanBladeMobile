package com.urbanblade.mobile.ui.navigation

import com.urbanblade.mobile.core.push.routeForNotificationType
import com.urbanblade.mobile.core.push.safePushRoute
import com.urbanblade.mobile.data.model.ModuleFeatures
import org.junit.Assert.assertEquals
import org.junit.Test

/** Una ruta de un módulo apagado no se abre, ni desde un atajo ni desde un aviso viejo: lleva al Inicio. */
class ModuleRoutesTest {
    private val off = ModuleFeatures()

    @Test fun `las rutas de modulos apagados llevan al inicio`() {
        listOf("social", "moderation", "blocked_authors", "campaigns", "raffles", "gift_cards").forEach {
            assertEquals("$it debería llevar al Inicio", "home", allowedRoute(it, off))
        }
    }

    @Test fun `las demas rutas se conservan aunque todo este apagado`() {
        listOf("home", "wallet", "appointments", "payments", "orders", "offers_admin", "reviews", "reports").forEach {
            assertEquals(it, allowedRoute(it, off))
        }
    }

    @Test fun `con el modulo encendido su ruta se abre`() {
        assertEquals("social", allowedRoute("social", ModuleFeatures(social = true)))
        assertEquals("gift_cards", allowedRoute("gift_cards", ModuleFeatures(giftCards = true)))
        assertEquals("home", allowedRoute("raffles", ModuleFeatures(social = true, campaigns = true)))
    }

    @Test fun `un aviso viejo hacia el muro termina en el inicio cuando el muro esta apagado`() {
        // El push de «reporte resuelto» manda a social; la bandeja usa el mismo destino.
        val route = routeForNotificationType("report_resolved")
        assertEquals("social", route)
        assertEquals("home", allowedRoute(canonicalRoute(route!!, barberOnly = false), off))
        // Sigue siendo una ruta conocida de push: quien la frena es la compuerta de navegación.
        assertEquals("social", safePushRoute("social"))
        assertEquals("home", allowedRoute(canonicalRoute(safePushRoute("social")!!, barberOnly = false), off))
        // El premio de un sorteo ya ganado lleva a Beneficios, que sigue existiendo (membresía y puntos).
        assertEquals("wallet", allowedRoute(routeForNotificationType("raffle_win")!!, off))
    }
}
