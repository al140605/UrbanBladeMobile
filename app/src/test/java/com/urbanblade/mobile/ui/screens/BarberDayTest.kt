package com.urbanblade.mobile.ui.screens

import com.urbanblade.mobile.data.model.AppointmentRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BarberDayTest {

    private fun appt(id: String, hora: String, estado: String) =
        AppointmentRow(id = id, fecha = "2026-09-24", horaInicio = "$hora:00", estado = estado)

    @Test
    fun `la siguiente cita es la primera desde la hora actual`() {
        val day = barberDay(
            listOf(appt("a", "10:00", "confirmada"), appt("b", "16:00", "confirmada"), appt("c", "12:30", "pendiente")),
            now = "12:00"
        )
        assertEquals("c", day.next?.id)
        assertEquals(3, day.remaining)
        assertEquals(1, day.toConfirm)
    }

    @Test
    fun `si todas quedaron atras la siguiente es la mas temprana sin atender`() {
        val day = barberDay(listOf(appt("a", "11:00", "confirmada"), appt("b", "09:00", "pendiente")), now = "18:00")
        assertEquals("b", day.next?.id)
    }

    @Test
    fun `la cita en proceso se separa y no cuenta como siguiente`() {
        val day = barberDay(listOf(appt("a", "10:00", "en_proceso"), appt("b", "11:00", "confirmada")), now = "10:15")
        assertEquals("a", day.inProcess?.id)
        assertEquals("b", day.next?.id)
        assertEquals(2, day.remaining)
    }

    @Test
    fun `completadas y canceladas no quedan pendientes`() {
        val day = barberDay(
            listOf(appt("a", "09:00", "completada"), appt("b", "10:00", "cancelada"), appt("c", "11:00", "no_asistio")),
            now = "12:00"
        )
        assertNull(day.next)
        assertNull(day.inProcess)
        assertEquals(0, day.remaining)
        assertEquals(1, day.done)
    }

    @Test
    fun `etiquetas de accion y estados destructivos`() {
        assertEquals("Confirmar", statusActionLabel("confirmada"))
        assertEquals("Iniciar servicio", statusActionLabel("en_proceso"))
        assertEquals("Terminar", statusActionLabel("completada"))
        assertEquals("Por confirmar", statusLabel("pendiente"))
        assertTrue(isDestructiveStatus("cancelada"))
        assertTrue(isDestructiveStatus("no_asistio"))
        assertFalse(isDestructiveStatus("completada"))
    }

    @Test
    fun `minutos de una jornada`() {
        assertEquals(540, minutesBetween("09:00", "18:00"))
        assertNull(minutesBetween("18:00", "09:00"))
        assertNull(minutesBetween("9am", "18:00"))
    }
}
