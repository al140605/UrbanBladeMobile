package com.urbanblade.mobile.ui.screens

import com.urbanblade.mobile.data.model.BarberItem
import com.urbanblade.mobile.data.model.BarberUser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun `el barbero del muro se traduce del id de usuario al de su perfil`() {
        val barbers = listOf(
            BarberItem(id = "perfil-1", slug = "luis-gonzalez", user = BarberUser("usuario-1", "Luis")),
            BarberItem(id = "perfil-2", user = BarberUser("usuario-2", "Ana"))
        )
        assertEquals("perfil-1", resolveBarberId(barbers, "usuario-1"))
        assertEquals("perfil-2", resolveBarberId(barbers, "perfil-2"))
        assertEquals("perfil-1", resolveBarberId(barbers, "luis-gonzalez"))
        assertNull(resolveBarberId(barbers, "desconocido"))
        assertNull(resolveBarberId(barbers, ""))
    }

    @Test
    fun `un nombre desconocido cae en corte`() {
        assertEquals(ServiceKind.CORTE, serviceKind("Servicio especial"))
        assertEquals(ServiceKind.TRATAMIENTO, serviceKind("Tratamiento capilar"))
    }
}
