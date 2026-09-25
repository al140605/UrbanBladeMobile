package com.urbanblade.mobile.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/** Checklist CN-100: estados de cita consistentes y comprensibles en toda la app. */
class UrbanStatusTest {

    private val appointmentStates = listOf("pendiente", "confirmada", "en_proceso", "completada", "cancelada", "no_asistio")

    @Test
    fun `cada estado de cita tiene etiqueta en espanol con acentos e icono propio`() {
        assertEquals(
            listOf("Pendiente", "Confirmada", "En proceso", "Completada", "Cancelada", "No asistió"),
            appointmentStates.map { statusStyle(it).label }
        )
        appointmentStates.forEach { assertNotNull(it, statusStyle(it).icon) }
        assertEquals(appointmentStates.size, appointmentStates.map { statusStyle(it).icon }.toSet().size)
    }

    @Test
    fun `confirmada y completada ya no comparten color`() {
        assertNotEquals(statusStyle("confirmada").tone, statusStyle("completada").tone)
    }

    @Test
    fun `el estado se reconoce sin importar mayusculas ni espacios`() {
        assertEquals("No asistió", statusStyle(" NO_ASISTIO ").label)
    }

    @Test
    fun `los estados de pedidos y pagos conservan su color de siempre`() {
        assertEquals(StatusTone.SUCCESS, statusStyle("verificado").tone)
        assertEquals(StatusTone.WARNING, statusStyle("en_revision").tone)
        assertEquals("En revision", statusStyle("en_revision").label)
    }

    @Test
    fun `los estados de pago se leen en espanol`() {
        assertEquals("Reembolsado", statusStyle("reembolsado").label)
        assertEquals("Por verificar", statusStyle("pendiente_verificacion").label)
        assertEquals(StatusTone.WARNING, statusStyle("pendiente_verificacion").tone)
    }
}
