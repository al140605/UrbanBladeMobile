package com.urbanblade.mobile.ui.screens

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceptionDashboardTest {

    private fun parse(json: String) = parseReceptionDashboard(JsonParser.parseString(json).asJsonObject)

    @Test
    fun `lee indicadores, citas por confirmar y pedidos`() {
        val d = parse(
            """{"todayLabel":"jueves 24","kpis":{"appointments_today":7,"pending_payments":2,"new_clients_today":1,
               "low_stock_count":6,"pending_orders":3,"collected_today":1250.5},
               "nextAppointments":[{"id":"a1","hora_inicio":"15:30:00","cliente":"Luis","servicio":"Arreglo de Barba","barbero":"Kike"}],
               "pendingOrders":[{"id":"o1","folio":"UB-0012","cliente":"Ana","itemsCount":2,"total":350}]}"""
        )!!
        assertEquals(7, d.appointmentsToday)
        assertEquals(2, d.pendingPayments)
        assertEquals(6, d.lowStock)
        assertEquals(1250.5, d.collectedToday, 0.001)
        assertEquals("Arreglo de Barba", d.toConfirm.single().servicio)
        assertEquals("UB-0012", d.orders.single().folio)
        assertEquals(350.0, d.orders.single().total, 0.001)
    }

    @Test
    fun `sin kpis no es un tablero de recepcion`() {
        assertNull(parse("""{"stats":{}}"""))
        assertNull(parseReceptionDashboard(null))
    }

    @Test
    fun `listas ausentes o incompletas no truenan`() {
        val d = parse("""{"kpis":{"appointments_today":0},"nextAppointments":[{"hora_inicio":"10:00:00"}]}""")!!
        assertTrue(d.toConfirm.isEmpty()) // sin id se descarta
        assertTrue(d.orders.isEmpty())
        assertEquals(0.0, d.collectedToday, 0.001)
    }
}
