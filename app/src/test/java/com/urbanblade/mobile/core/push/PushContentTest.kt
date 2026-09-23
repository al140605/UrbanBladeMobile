package com.urbanblade.mobile.core.push

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}
