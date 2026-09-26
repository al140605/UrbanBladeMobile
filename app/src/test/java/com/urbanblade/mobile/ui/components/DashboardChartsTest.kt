package com.urbanblade.mobile.ui.components

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class DashboardChartsTest {

    @Test
    fun `lee la serie del dashboard con etiquetas y valores`() {
        val data = JsonParser.parseString(
            """{"incomeChart": {"labels": ["03 Aug", "10 Aug"], "values": [11200, 12850.5]}}"""
        ).asJsonObject

        val series = data.chartSeries("incomeChart")!!

        assertEquals(listOf("03 Aug", "10 Aug"), series.labels)
        assertEquals(listOf(11200.0, 12850.5), series.values)
    }

    @Test
    fun `sin la serie o en ceros no hay grafica`() {
        val data = JsonParser.parseString("""{"flowChart": {"labels": ["9", "10"], "values": [0, 0]}}""").asJsonObject

        assertNull(data.chartSeries("incomeChart"))
        assertFalse(data.chartSeries("flowChart")!!.hasData)
        assertNull(null.chartSeries("incomeChart"))
    }

    @Test
    fun `dias y fechas en ingles de barber se muestran en espanol`() {
        assertEquals("Lun", spanishChartLabel("Mon"))
        assertEquals("Dom", spanishChartLabel("Sun"))
        assertEquals("3 ago", spanishChartLabel("03 Aug"))
        assertEquals("21 sep", spanishChartLabel("21 Sep"))
        // Lo que no es fecha pasa igual.
        assertEquals("Corte Clásico", spanishChartLabel("Corte Clásico"))
        assertEquals("10", spanishChartLabel("10"))
    }
}
