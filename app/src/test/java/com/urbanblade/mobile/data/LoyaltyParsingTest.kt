package com.urbanblade.mobile.data

import com.google.gson.Gson
import com.urbanblade.mobile.data.model.ClientLoyalty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** La Wallet lee la lealtad embebida en GET dashboard (DashboardController::clientPayload en barber). */
class LoyaltyParsingTest {

    @Test
    fun `lee la escalera de niveles, las reglas de puntos y el premio del sorteo`() {
        val json = """
            {"nivel":"nuevo","nivelLabel":"Caballero","puntos":20,"discountPct":0,"nextNivel":"regular",
             "nextNivelLabel":"Regular","citasFaltan":4,"progressPct":20,
             "recentTransactions":[{"descripcion":"Cita completada","puntos":10}],
             "nextDiscountPct":5,
             "levels":[{"nivel":"nuevo","label":"Caballero","citas":0,"discountPct":0},{"nivel":"regular","label":"Regular","citas":5,"discountPct":5}],
             "earnRules":[{"descripcion":"Cita completada","puntos":10},{"descripcion":"Amigo referido","puntos":30}],
             "maxRedeemPct":50,
             "wonRaffle":{"mes":"Septiembre","premio":"Corte gratis","isExpired":false,"venceEn":"30/10/2026"}}
        """.trimIndent()

        val loyalty = Gson().fromJson(json, ClientLoyalty::class.java)

        assertEquals(5, loyalty.nextDiscountPct)
        assertEquals(listOf("Caballero", "Regular"), loyalty.levels.map { it.label })
        assertEquals(5, loyalty.levels[1].citas)
        assertEquals(30, loyalty.earnRules[1].puntos)
        assertEquals(50, loyalty.maxRedeemPct)
        assertEquals("Corte gratis", loyalty.wonRaffle?.premio)
    }

    @Test
    fun `sin los campos nuevos (backend anterior) la wallet no falla`() {
        val loyalty = Gson().fromJson("""{"nivel":"nuevo","nivelLabel":"Caballero","puntos":0}""", ClientLoyalty::class.java)

        assertNull(loyalty.nextDiscountPct)
        assertTrue(loyalty.levels.isEmpty())
        assertTrue(loyalty.earnRules.isEmpty())
        assertNull(loyalty.maxRedeemPct)
        assertNull(loyalty.wonRaffle)
    }
}
