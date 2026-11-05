package com.urbanblade.mobile.ui.screens

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset
import java.time.ZonedDateTime

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

    // «Ahora» fijo: 5 de noviembre de 2026, 12:00 (UTC-6).
    private val now = ZonedDateTime.of(2026, 11, 5, 12, 0, 0, 0, ZoneOffset.ofHours(-6))

    private fun item(id: String, read: Boolean, createdAt: String?) =
        NotificationItem(id = id, title = "t$id", message = null, type = "appointment", read = read, createdAt = createdAt)

    @Test
    fun `lo no leido va siempre en nuevas aunque sea viejo`() {
        assertEquals(NotificationGroup.NEW, notificationGroup(item("a", false, "2026-01-01T09:00:00-06:00"), now))
    }

    @Test
    fun `lo leido se reparte por dias`() {
        assertEquals(NotificationGroup.TODAY, notificationGroup(item("a", true, "2026-11-05T08:00:00-06:00"), now))
        assertEquals(NotificationGroup.YESTERDAY, notificationGroup(item("b", true, "2026-11-04T23:30:00-06:00"), now))
        assertEquals(NotificationGroup.WEEK, notificationGroup(item("c", true, "2026-11-01T10:00:00-06:00"), now))
        assertEquals(NotificationGroup.OLDER, notificationGroup(item("d", true, "2026-10-20T10:00:00-06:00"), now))
        assertEquals(NotificationGroup.OLDER, notificationGroup(item("e", true, null), now))
        assertEquals(NotificationGroup.OLDER, notificationGroup(item("f", true, "no es fecha"), now))
    }

    @Test
    fun `las secciones salen en orden, sin vacias y conservando el orden del servidor`() {
        val groups = groupNotifications(
            listOf(
                item("viejo", true, "2026-09-01T10:00:00-06:00"),
                item("nuevo1", false, "2026-11-05T11:00:00-06:00"),
                item("hoy", true, "2026-11-05T09:00:00-06:00"),
                item("nuevo2", false, "2026-11-04T11:00:00-06:00")
            ),
            now
        )
        assertEquals(listOf(NotificationGroup.NEW, NotificationGroup.TODAY, NotificationGroup.OLDER), groups.map { it.first })
        assertEquals(listOf("nuevo1", "nuevo2"), groups[0].second.map { it.id })
    }

    @Test
    fun `tiempo relativo en espanol`() {
        assertEquals("Ahora", relativeNotificationTime("2026-11-05T11:59:40-06:00", now))
        assertEquals("Hace 5 min", relativeNotificationTime("2026-11-05T11:55:00-06:00", now))
        assertEquals("Hace 3 h", relativeNotificationTime("2026-11-05T09:00:00-06:00", now))
        assertEquals("Ayer", relativeNotificationTime("2026-11-04T08:00:00-06:00", now))
        assertEquals("Hace 3 d", relativeNotificationTime("2026-11-02T08:00:00-06:00", now))
        assertEquals("20 oct", relativeNotificationTime("2026-10-20T08:00:00-06:00", now))
        assertNull(relativeNotificationTime(null, now))
        assertNull(relativeNotificationTime("ayer", now))
    }

    @Test
    fun `la zona del servidor no cambia el dia en que se muestra`() {
        // 23:30 UTC del 4 de nov son las 17:30 del 4 de nov en UTC-6: «Ayer».
        assertEquals("Ayer", relativeNotificationTime("2026-11-04T23:30:00Z", now))
    }
}
