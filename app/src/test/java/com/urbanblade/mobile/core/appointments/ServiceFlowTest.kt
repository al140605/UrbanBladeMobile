package com.urbanblade.mobile.core.appointments

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.OffsetDateTime

class ServiceFlowTest {
    private val now = OffsetDateTime.parse("2026-10-08T10:00:00Z").toInstant().toEpochMilli()

    @Test
    fun `minutesLeft redondea hacia arriba y tolera datos vacios`() {
        assertEquals(13, minutesLeft("2026-10-08T10:12:10Z", now))
        assertEquals(-3, minutesLeft("2026-10-08T09:57:00Z", now))
        assertEquals(5, minutesLeft("2026-10-08T05:05:00-05:00", now))
        assertNull(minutesLeft(null, now))
        assertNull(minutesLeft("", now))
        assertNull(minutesLeft("no-es-fecha", now))
    }

    @Test
    fun `remainingLabel cubre cada caso`() {
        assertEquals("Quedan 12 min", remainingLabel(12))
        assertEquals("Termina en 1 min", remainingLabel(1))
        assertEquals("Termina ahora", remainingLabel(0))
        assertEquals("Se pasó 3 min", remainingLabel(-3))
        assertEquals("Sin hora de fin", remainingLabel(null))
    }

    @Test
    fun `remainingTone marca por terminar y pasado`() {
        assertEquals(RemainingTone.OK, remainingTone(20))
        assertEquals(RemainingTone.OK, remainingTone(null))
        assertEquals(RemainingTone.SOON, remainingTone(5))
        assertEquals(RemainingTone.SOON, remainingTone(0))
        assertEquals(RemainingTone.OVER, remainingTone(-1))
    }

    @Test
    fun `canForceFromBody lee puede_forzar del 422`() {
        assertTrue(canForceFromBody("""{"message":"Choca","puede_forzar":true,"choca_con":{"hora_inicio":"10:05"}}"""))
        assertFalse(canForceFromBody("""{"message":"Solo se puede agregar tiempo a un servicio en proceso."}"""))
        assertFalse(canForceFromBody("""{"puede_forzar":false}"""))
        assertFalse(canForceFromBody(null))
        assertFalse(canForceFromBody("no es json"))
    }

    @Test
    fun `las opciones de tiempo son las del backend`() {
        assertEquals(listOf(10, 15), EXTEND_OPTIONS)
    }
}
