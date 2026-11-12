package com.urbanblade.mobile.ui.screens

import com.urbanblade.mobile.data.model.AppointmentRow
import java.time.LocalDateTime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Qué citas se presentan como «próximas» en Mis citas del cliente. */
class IsStillUpcomingTest {
    private val ahora = LocalDateTime.of(2026, 10, 30, 14, 0)

    private fun cita(fecha: String, inicio: String, estado: String, fin: String? = null) =
        AppointmentRow(id = "1", fecha = fecha, horaInicio = inicio, horaFin = fin, estado = estado)

    @Test
    fun `una cita de otro dia futuro es proxima`() {
        assertTrue(isStillUpcoming(cita("2026-10-31", "09:00:00", "pendiente"), ahora))
    }

    @Test
    fun `una cita de un dia pasado ya no es proxima`() {
        assertFalse(isStillUpcoming(cita("2026-10-29", "16:00:00", "confirmada"), ahora))
    }

    @Test
    fun `una pendiente de hoy que ya termino pasa al historial`() {
        assertFalse(isStillUpcoming(cita("2026-10-30", "09:00:00", "pendiente", fin = "09:30:00"), ahora))
    }

    @Test
    fun `sin hora de fin se usa la de inicio`() {
        assertFalse(isStillUpcoming(cita("2026-10-30", "09:00:00", "confirmada"), ahora))
        assertTrue(isStillUpcoming(cita("2026-10-30", "16:00:00", "confirmada"), ahora))
    }

    @Test
    fun `una cita de hoy que sigue en curso se conserva`() {
        assertTrue(isStillUpcoming(cita("2026-10-30", "13:30:00", "confirmada", fin = "14:30:00"), ahora))
    }

    @Test
    fun `una cita en proceso se conserva aunque haya pasado su hora`() {
        assertTrue(isStillUpcoming(cita("2026-10-30", "12:00:00", "en_proceso", fin = "12:30:00"), ahora))
    }

    @Test
    fun `canceladas y completadas nunca son proximas`() {
        assertFalse(isStillUpcoming(cita("2026-10-31", "09:00:00", "cancelada"), ahora))
        assertFalse(isStillUpcoming(cita("2026-10-31", "09:00:00", "completada"), ahora))
    }
}
