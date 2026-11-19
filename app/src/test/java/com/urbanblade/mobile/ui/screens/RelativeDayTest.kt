package com.urbanblade.mobile.ui.screens

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RelativeDayTest {
    private val hoy = LocalDate.of(2026, 11, 18)

    @Test fun `hoy y manana se nombran`() {
        assertEquals("Hoy", relativeDay("2026-11-18", hoy))
        assertEquals("Mañana", relativeDay("2026-11-19", hoy))
    }

    @Test fun `dentro de la semana se cuentan los dias`() {
        assertEquals("En 2 días", relativeDay("2026-11-20", hoy))
        assertEquals("En 6 días", relativeDay("2026-11-24", hoy))
    }

    @Test fun `mas lejos o en el pasado no se rotula`() {
        assertNull(relativeDay("2026-11-25", hoy))
        assertNull(relativeDay("2026-11-17", hoy))
    }

    @Test fun `acepta fecha con hora y descarta basura`() {
        assertEquals("Mañana", relativeDay("2026-11-19T10:00:00", hoy))
        assertNull(relativeDay("no-es-fecha", hoy))
    }
}
