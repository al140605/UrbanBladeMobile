package com.urbanblade.mobile.ui.screens

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationsListTest {

    private fun parse(json: String) = parseNotifications(JsonParser.parseString(json).asJsonObject)

    @Test
    fun `lee titulo, mensaje, tipo y si ya se leyo`() {
        val view = parse(
            """{"data":[
                {"id":"n1","type":"App\\Notifications\\X","data":{"type":"appointment","title":"Cita confirmada","message":"Te esperamos a las 3:30"},"read_at":null,"created_at":"2026-09-24T08:00:00-06:00"},
                {"id":"n2","data":{"title":"Pago recibido","message":"Gracias"},"read_at":"2026-09-24T09:00:00-06:00","created_at":"2026-09-24T08:30:00-06:00"}
            ],"meta":{"unread":1}}"""
        )
        assertEquals(2, view.items.size)
        assertEquals("Cita confirmada", view.items[0].title)
        assertEquals("Te esperamos a las 3:30", view.items[0].message)
        assertEquals("appointment", view.items[0].type)
        assertFalse(view.items[0].read)
        assertTrue(view.items[1].read)
        assertEquals(1, view.unread)
    }

    @Test
    fun `sin titulo usa subject o un titulo generico`() {
        val view = parse("""{"data":[{"id":"a","data":{"subject":"Recordatorio","body":"Mañana"}},{"id":"b","data":{}}]}""")
        assertEquals("Recordatorio", view.items[0].title)
        assertEquals("Mañana", view.items[0].message)
        assertEquals("Aviso de UrbanBlade", view.items[1].title)
        assertNull(view.items[1].message)
        assertEquals(2, view.unread) // sin meta, se cuentan las no leídas
    }

    @Test
    fun `respuesta vacia o sin data no truena`() {
        assertTrue(parseNotifications(null).items.isEmpty())
        assertTrue(parse("""{"message":"x"}""").items.isEmpty())
    }
}
