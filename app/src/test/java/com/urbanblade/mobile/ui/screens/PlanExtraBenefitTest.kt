package com.urbanblade.mobile.ui.screens

import com.urbanblade.mobile.data.model.MembershipPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** La descripción del plan no debe repetir el descuento en Beneficios; solo lo que agrega. */
class PlanExtraBenefitTest {

    private fun plan(pct: Double, descripcion: String?) = MembershipPlan(id = "p", nombre = "Plan", descripcion = descripcion, descuentoPct = pct)

    @Test
    fun `solo el descuento no agrega nada`() {
        assertNull(planExtraBenefit(plan(10.0, "10% de descuento en todos los servicios.")))
        assertNull(planExtraBenefit(plan(10.0, null)))
    }

    @Test
    fun `se queda con lo que agrega al descuento`() {
        assertEquals("Prioridad de agenda", planExtraBenefit(plan(15.0, "15% de descuento y prioridad de agenda.")))
        assertEquals("Beneficios VIP", planExtraBenefit(plan(20.0, "20% de descuento y beneficios VIP.")))
    }

    @Test
    fun `una descripcion distinta se muestra completa`() {
        assertEquals("Corte gratis el mes de tu cumpleaños", planExtraBenefit(plan(10.0, "Corte gratis el mes de tu cumpleaños")))
    }
}
