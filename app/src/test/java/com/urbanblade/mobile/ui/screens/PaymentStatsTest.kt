package com.urbanblade.mobile.ui.screens

import com.urbanblade.mobile.data.model.PaymentRow
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class PaymentStatsTest {

    private val today = LocalDate.of(2026, 9, 18)

    private fun payment(monto: Double, metodo: String?, fecha: String?, propina: Double = 0.0) =
        PaymentRow(id = "$monto-$fecha", monto = monto, metodoPago = metodo, propina = propina, createdAt = fecha)

    @Test
    fun `suma monto y propina y calcula el ticket promedio`() {
        val stats = computePaymentStats(
            listOf(payment(171.0, "efectivo", "2026-09-18T10:00:00Z", propina = 20.0), payment(200.0, "tarjeta", "2026-09-17")),
            today = today
        )

        assertEquals(391.0, stats.total, 0.0)
        assertEquals(20.0, stats.tips, 0.0)
        assertEquals(2, stats.count)
        assertEquals(195.5, stats.average, 0.0)
    }

    @Test
    fun `agrupa por metodo de mayor a menor y normaliza el nombre`() {
        val stats = computePaymentStats(
            listOf(
                payment(100.0, "Efectivo", "2026-09-18"),
                payment(50.0, "efectivo", "2026-09-18"),
                payment(300.0, "tarjeta", "2026-09-18"),
                payment(10.0, null, "2026-09-18")
            ),
            today = today
        )

        assertEquals(listOf("Tarjeta" to 300.0, "Efectivo" to 150.0, "Otro" to 10.0), stats.byMethod)
    }

    @Test
    fun `los ultimos dias no tienen huecos y terminan hoy`() {
        val stats = computePaymentStats(
            listOf(payment(100.0, "efectivo", "2026-09-18"), payment(40.0, "efectivo", "2026-09-16"), payment(60.0, "efectivo", "2026-09-16")),
            days = 5,
            today = today
        )

        assertEquals(listOf("14", "15", "16", "17", "18"), stats.byDay.map { it.first })
        assertEquals(listOf(0.0, 0.0, 100.0, 0.0, 100.0), stats.byDay.map { it.second })
    }

    @Test
    fun `sin pagos todo queda en cero y una fecha ilegible no rompe el calculo`() {
        val empty = computePaymentStats(emptyList(), today = today)
        assertEquals(0.0, empty.average, 0.0)

        val broken = computePaymentStats(listOf(payment(80.0, "efectivo", "no es fecha")), days = 3, today = today)
        assertEquals(80.0, broken.total, 0.0)
        assertEquals(listOf(0.0, 0.0, 0.0), broken.byDay.map { it.second })
    }
}
