package com.urbanblade.mobile.core.push

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Enlaces de los correos: urbanblade://open?route=<pantalla> (la web los envía desde /abrir en un celular Android). */
class EmailDeepLinkTest {
    @Test
    fun `abre la pantalla pedida si esta permitida`() {
        assertEquals("payments", routeFromDeepLink("urbanblade", "open", "payments"))
        assertEquals("catalog", routeFromDeepLink("urbanblade", "open", "catalog"))
        assertEquals("barber_agenda", routeFromDeepLink("urbanblade", "open", "barber_agenda"))
    }

    @Test
    fun `ignora pantallas que no estan en la lista cerrada`() {
        assertNull(routeFromDeepLink("urbanblade", "open", "admin_metrics"))
        assertNull(routeFromDeepLink("urbanblade", "open", "https://malicioso.example"))
        assertNull(routeFromDeepLink("urbanblade", "open", null))
    }

    @Test
    fun `ignora enlaces que no son de la app`() {
        assertNull(routeFromDeepLink("https", "open", "payments"))
        assertNull(routeFromDeepLink("urbanblade", "otro", "payments"))
        assertNull(routeFromDeepLink(null, null, "payments"))
    }

    @Test
    fun `las pantallas de los correos estan permitidas`() {
        assertTrue(PUSH_ROUTES.containsAll(listOf("appointments", "payments", "orders", "catalog", "barber_agenda", "inventory", "notifications", "home")))
    }
}
