package com.urbanblade.mobile.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class StaffOperationsFormatTest {

    @Test
    fun `formatWhen junta dia corto y hora cuando hay hora`() {
        assertEquals("21 sep · 10:30", formatWhen("2026-09-21T10:30:00+00:00"))
    }

    @Test
    fun `formatWhen solo con fecha muestra el dia y vacio muestra guion`() {
        assertEquals("21 sep", formatWhen("2026-09-21"))
        assertEquals("—", formatWhen(null))
        assertEquals("—", formatWhen("  "))
    }

    @Test
    fun `los estados de la lista de espera se muestran en espanol`() {
        assertEquals("En espera", waitlistStatusLabel("activo"))
        assertEquals("Avisada", waitlistStatusLabel("notificado"))
        assertEquals("Reservó", waitlistStatusLabel("reservado"))
        assertEquals("Canceló", waitlistStatusLabel("cancelado"))
        assertEquals("Raro", waitlistStatusLabel("raro"))
    }
}
