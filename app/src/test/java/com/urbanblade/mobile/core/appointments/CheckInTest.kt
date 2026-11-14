package com.urbanblade.mobile.core.appointments

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckInTest {
    private val today = LocalDate.of(2026, 10, 29)

    @Test
    fun `permite check-in solo hoy y en pendiente o confirmada`() {
        assertTrue(canCheckIn("pendiente", "2026-10-29", today))
        assertTrue(canCheckIn("confirmada", "2026-10-29T10:00:00+00:00", today))
        assertFalse(canCheckIn("en_proceso", "2026-10-29", today))
        assertFalse(canCheckIn("completada", "2026-10-29", today))
        assertFalse(canCheckIn("cancelada", "2026-10-29", today))
        assertFalse(canCheckIn("confirmada", "2026-10-30", today))
    }

    @Test
    fun `arrivalTime formatea en la zona indicada`() {
        assertEquals("10:02", arrivalTime("2026-10-29T10:02:11+00:00", ZoneId.of("UTC")))
        assertEquals("04:02", arrivalTime("2026-10-29T10:02:11+00:00", ZoneId.of("America/Mexico_City")))
    }

    @Test
    fun `arrivalTime devuelve null con valores ilegibles`() {
        assertNull(arrivalTime(null))
        assertNull(arrivalTime(""))
        assertNull(arrivalTime("no es una fecha"))
    }
}
