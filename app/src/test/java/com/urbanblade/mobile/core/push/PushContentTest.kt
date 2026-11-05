package com.urbanblade.mobile.core.push

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** T142 (HU-16): contenido de la notificación push según lo que envíe el backend. */
class PushContentTest {

    @Test
    fun `usa el bloque notification cuando viene`() {
        val content = pushContent("Recordatorio", "Tu cita es mañana a las 10:00", emptyMap())
        assertEquals(PushContent("Recordatorio", "Tu cita es mañana a las 10:00"), content)
    }

    @Test
    fun `usa title y body de data si no hay bloque notification`() {
        val content = pushContent(null, null, mapOf("title" to "Cita confirmada", "body" to "Te esperamos"))
        assertEquals(PushContent("Cita confirmada", "Te esperamos"), content)
    }

    @Test
    fun `acepta message como texto y UrbanBlade como titulo por defecto`() {
        val content = pushContent(null, "  ", mapOf("message" to "Tu cita fue reprogramada"))
        assertEquals(PushContent("UrbanBlade", "Tu cita fue reprogramada"), content)
    }

    @Test
    fun `sin texto no se muestra notificacion`() {
        assertNull(pushContent("Solo título", null, mapOf("appointment_id" to "abc")))
    }

    @Test
    fun `el canal y la pantalla salen de data`() {
        val content = pushContent("Pago recibido", "Recibimos tu pago", mapOf("channel" to "pagos", "route" to "payments"))
        assertEquals(PushChannel.PAGOS, content?.channel)
        assertEquals("payments", content?.route)
    }

    @Test
    fun `un canal desconocido o ausente cae en citas`() {
        assertEquals(PushChannel.CITAS, pushContent("a", "b", mapOf("channel" to "inventado"))?.channel)
        assertEquals(PushChannel.CITAS, pushContent("a", "b", emptyMap())?.channel)
    }

    @Test
    fun `solo se aceptan pantallas conocidas`() {
        assertEquals("orders", safePushRoute(" orders "))
        assertNull(safePushRoute("admin_metrics"))
        assertNull(safePushRoute("https://malicioso.example"))
        assertNull(safePushRoute(null))
        assertNull(pushContent("a", "b", mapOf("route" to "logs"))?.route)
    }

    @Test
    fun `todos los canales que manda el backend existen en la app`() {
        // Misma lista que App\Services\Push\PushRouting::CHANNELS.
        val backend = listOf("citas", "pagos", "pedidos", "fidelidad", "promociones", "operacion", "comunidad")
        backend.forEach { assertEquals(it, PushChannel.from(it).id) }
        assertEquals(backend.size, PushChannel.entries.size)
    }

    @Test
    fun `citas pagos y operacion son importantes y promociones no`() {
        assertTrue(PushChannel.CITAS.important && PushChannel.PAGOS.important && PushChannel.OPERACION.important)
        assertTrue(!PushChannel.PROMOCIONES.important && !PushChannel.PEDIDOS.important)
    }
}

/** La bandeja de avisos abre la misma pantalla que el push (mismos `type` que PushRouting.php). */
class NotificationRouteTest {
    @Test
    fun `cada tipo de aviso del backend lleva a una pantalla permitida`() {
        val tipos = listOf(
            "appointment", "review_request", "payment", "transfer_receipt", "membership_invoice", "order_delivered", "order_expired",
            "loyalty_level_up", "loyalty_level_downgraded", "loyalty_points_expired", "raffle_win", "client_birthday",
            "promotion", "service_overrun", "inventory_low_stock", "report_resolved"
        )
        tipos.forEach { tipo ->
            val ruta = routeForNotificationType(tipo)
            assertTrue("«$tipo» sin pantalla", ruta != null)
        }
    }

    @Test
    fun `pagos pedidos y citas abren su pantalla`() {
        assertEquals("payments", routeForNotificationType("payment"))
        assertEquals("orders", routeForNotificationType("order_delivered"))
        assertEquals("appointments", routeForNotificationType("appointment"))
        assertEquals("wallet", routeForNotificationType("raffle_win"))
    }

    @Test
    fun `la respuesta a un reporte abre el muro y su canal es comunidad`() {
        assertEquals("social", routeForNotificationType("report_resolved"))
        assertEquals("social", safePushRoute("social"))
        assertEquals(PushChannel.COMUNIDAD, PushChannel.from("comunidad"))
        assertTrue(!PushChannel.COMUNIDAD.important)
    }

    @Test
    fun `un tipo desconocido o vacio no navega`() {
        assertNull(routeForNotificationType("algo_nuevo"))
        assertNull(routeForNotificationType(null))
    }

    @Test
    fun `los tipos nuevos abren su pantalla`() {
        assertEquals("barber_agenda", routeForNotificationType("service_ending"))
        assertEquals("reviews", routeForNotificationType("barber_review_flagged"))
        assertEquals("reports", routeForNotificationType("barber_performance_report"))
        assertEquals("reviews", safePushRoute("reviews"))
        assertEquals("reports", safePushRoute("reports"))
        assertNull(routeForNotificationType("tipo_desconocido"))
    }

    @Test
    fun `si el push trae la bandeja pero el tipo se conoce, abre la pantalla del tipo`() {
        assertEquals("reviews", pushRoute("notifications", "barber_review_flagged"))
        assertEquals("reviews", pushRoute(null, "barber_review_flagged"))
        assertEquals("orders", pushRoute("orders", "barber_review_flagged")) // la ruta explicita manda
        assertEquals("notifications", pushRoute("notifications", "tipo_desconocido"))
        assertNull(pushRoute("https://malicioso.example", "tipo_desconocido"))
    }

    @Test
    fun `el contenido del push usa el tipo cuando la ruta es generica`() {
        val content = pushContent("Reseña señalada", "Revisa", mapOf("route" to "notifications", "type" to "barber_review_flagged"))
        assertEquals("reviews", content?.route)
    }
}
