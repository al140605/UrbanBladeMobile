package com.urbanblade.mobile.core.push

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Botones de la notificación «tu servicio termina en 5 min» (terminar ya / +10 / +15 min). */
class ServiceActionContentTest {
    @Test
    fun `parseServiceAction solo acepta las acciones conocidas`() {
        assertEquals(ServiceAction.Finish, parseServiceAction("terminar"))
        assertEquals(ServiceAction.Extend(10), parseServiceAction("extender_10"))
        assertEquals(ServiceAction.Extend(15), parseServiceAction(" Extender_15 "))
        assertNull(parseServiceAction("extender_45"))
        assertNull(parseServiceAction("extender_"))
        assertNull(parseServiceAction("borrar_todo"))
        assertNull(parseServiceAction(null))
    }

    @Test
    fun `las etiquetas y claves de los botones son estables`() {
        assertEquals("Terminar ya", ServiceAction.Finish.label())
        assertEquals("+10 min", ServiceAction.Extend(10).label())
        assertEquals("terminar", ServiceAction.Finish.key())
        assertEquals("extender_15", ServiceAction.Extend(15).key())
    }

    @Test
    fun `pushContent trae los botones y la cita desde data`() {
        val content = pushContent(
            null, null,
            mapOf(
                "title" to "Tu servicio termina en 5 min",
                "body" to "¿Lo terminas ahora o agregas más tiempo?",
                "channel" to "operacion",
                "route" to "barber_agenda",
                "appointment_code" to "ABC123",
                "acciones" to "terminar,extender_10,extender_15,raro"
            )
        )!!

        assertEquals("Tu servicio termina en 5 min", content.title)
        assertEquals(PushChannel.OPERACION, content.channel)
        assertEquals("barber_agenda", content.route)
        assertEquals("ABC123", content.appointmentCode)
        assertEquals(listOf(ServiceAction.Finish, ServiceAction.Extend(10), ServiceAction.Extend(15)), content.actions)
    }

    @Test
    fun `sin codigo de cita no hay botones`() {
        val content = pushContent(null, null, mapOf("title" to "x", "body" to "y", "acciones" to "terminar"))!!

        assertTrue(content.actions.isEmpty())
        assertNull(content.appointmentCode)
    }

    @Test
    fun `una notificacion normal no cambia`() {
        val content = pushContent("Recordatorio", "Tu cita es mañana", mapOf("channel" to "citas", "route" to "appointments"))!!

        assertTrue(content.actions.isEmpty())
        assertEquals("appointments", content.route)
    }
}
