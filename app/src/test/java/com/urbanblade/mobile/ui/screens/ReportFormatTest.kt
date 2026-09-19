package com.urbanblade.mobile.ui.screens

import com.google.gson.JsonNull
import com.google.gson.JsonPrimitive
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportFormatTest {

    private val today = LocalDate.of(2026, 9, 18)

    @Test
    fun `los rangos rapidos calculan sus fechas`() {
        assertEquals("2026-09-12" to "2026-09-18", ReportRange.Last7.dates(today))
        assertEquals("2026-08-20" to "2026-09-18", ReportRange.Last30.dates(today))
        assertEquals("2026-09-01" to "2026-09-18", ReportRange.ThisMonth.dates(today))
        assertEquals("2026-08-01" to "2026-08-31", ReportRange.LastMonth.dates(today))
        assertEquals(null to null, ReportRange.All.dates(today))
    }

    @Test
    fun `el mes pasado en enero es diciembre del anio anterior`() {
        assertEquals("2025-12-01" to "2025-12-31", ReportRange.LastMonth.dates(LocalDate.of(2026, 1, 10)))
    }

    @Test
    fun `distingue series de fechas de categorias`() {
        assertTrue(labelsAreDates(listOf("2026-09-17", "2026-09-18")))
        assertFalse(labelsAreDates(listOf("pendiente", "completada")))
        assertFalse(labelsAreDates(emptyList()))
    }

    @Test
    fun `acorta fechas y deja intactas las etiquetas que no lo son`() {
        assertEquals("18/09", shortDateLabel("2026-09-18"))
        assertEquals("Efectivo", shortDateLabel("Efectivo"))
    }

    @Test
    fun `da formato a los valores segun la unidad`() {
        assertEquals("$1,234", formatChartValue(1234.4, "MXN"))
        assertEquals("12 citas", formatChartValue(12.0, "citas"))
        assertEquals("2.5", formatChartValue(2.5, null))
    }

    @Test
    fun `formatea celdas de dinero, booleanos y vacios`() {
        assertEquals("$171.00", formatCell("total", JsonPrimitive(171)))
        assertEquals("12", formatCell("visitas", JsonPrimitive(12)))
        assertEquals("Sí", formatCell("bajo_minimo", JsonPrimitive(true)))
        assertEquals("—", formatCell("cliente", JsonNull.INSTANCE))
        assertEquals("—", formatCell("cliente", null))
        assertEquals("Efectivo", formatCell("metodo_pago", JsonPrimitive("Efectivo")))
    }
}
