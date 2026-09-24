package com.urbanblade.mobile.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class ClientVisualsTest {

    @Test
    fun `los servicios reales del catalogo se clasifican por su nombre`() {
        assertEquals(ServiceKind.AFEITADO, serviceKind("Afeitado Clásico"))
        assertEquals(ServiceKind.BARBA, serviceKind("Arreglo de Barba"))
        assertEquals(ServiceKind.BARBA, serviceKind("Diseño de Barba"))
        assertEquals(ServiceKind.COMBO, serviceKind("Combo Corte + Barba"))
        assertEquals(ServiceKind.COMBO, serviceKind("Combo Premium"))
        assertEquals(ServiceKind.INFANTIL, serviceKind("Corte Infantil"))
        assertEquals(ServiceKind.CORTE, serviceKind("Corte a Navaja"))
        assertEquals(ServiceKind.CORTE, serviceKind("Corte Mohicano"))
    }

    @Test
    fun `un nombre desconocido cae en corte`() {
        assertEquals(ServiceKind.CORTE, serviceKind("Servicio especial"))
        assertEquals(ServiceKind.TRATAMIENTO, serviceKind("Tratamiento capilar"))
    }
}
