package com.urbanblade.mobile.ui.screens

import com.urbanblade.mobile.ui.components.formBlockerText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UnifiedFormsTest {
    @Test
    fun `el botón explica qué falta`() {
        assertNull(formBlockerText(emptyList()))
        assertEquals("Falta: nombre", formBlockerText(listOf("Nombre")))
        assertEquals("Falta: nombre y 2 más", formBlockerText(listOf("Nombre", "Correo", "Contraseña")))
    }

    @Test
    fun `cliente nuevo exige nombre, correo válido y contraseña de 8`() {
        val vacio = validateClientForm("", "", "")
        assertEquals(listOf("Nombre", "Correo", "Contraseña"), vacio.missing)
        assertEquals("Escribe un correo válido, como nombre@correo.com.", validateClientForm("Ana", "ana@", "12345678").email)
        assertEquals("Usa al menos 8 caracteres.", validateClientForm("Ana", "ana@correo.com", "1234567").password)
        assertTrue(validateClientForm("Ana", "ana@correo.com", "12345678").missing.isEmpty())
    }

    @Test
    fun `cliente existente no pide contraseña`() {
        assertTrue(validateClientForm("Ana", "ana@correo.com", null).missing.isEmpty())
    }

    @Test
    fun `usuario nuevo pide contraseña y el existente no`() {
        assertEquals(listOf("Nombre", "Correo", "Rol", "Contraseña"), validateUserForm("", "x", "", "", "", false).missing)
        assertTrue(validateUserForm("Ana", "ana@correo.com", "recepcionista", "", "", true).missing.isEmpty())
        assertEquals("Las contraseñas no coinciden.", validateUserForm("Ana", "ana@correo.com", "barbero", "12345678", "1234567x", false).password)
    }

    @Test
    fun `servicio valida precio y duración`() {
        assertTrue(validateServiceForm("Corte", "Corte", "180", "45").isEmpty())
        assertEquals(listOf("nombre", "categoria", "precio", "duracion"), validateServiceForm("", "", "x", "4").keys.toList())
    }

    @Test
    fun `barbero valida correo y comisión`() {
        assertTrue(validateBarberForm("Diego", "diego@urbanblade.mx", "").isEmpty())
        assertEquals(listOf("nombre", "email", "comision"), validateBarberForm("", "diego", "120").keys.toList())
    }

    @Test
    fun `plan y paquete validan sus límites`() {
        assertTrue(validatePlanForm("Plan", "499", "10").isEmpty())
        assertEquals(listOf("nombre", "precio", "descuento"), validatePlanForm("", "0", "0").keys.toList())
        assertTrue(validatePackageForm("5 cortes", "s1", "5", "800", "").isEmpty())
        assertEquals(listOf("nombre", "servicio", "usos", "precio", "vigencia"), validatePackageForm("", "", "1", "0", "900").keys.toList())
    }

    @Test
    fun `producto valida precios y existencias`() {
        assertTrue(validateProductForm("Cera", "Cabello", "85.5", "140", "51", "10").isEmpty())
        val errores = validateProductForm("", "", "x", "-1", "", "2.5")
        assertEquals(listOf("nombre", "categoria", "precioCompra", "precioVenta", "stockActual", "stockMinimo"), errores.keys.toList())
        assertEquals("No puede ser negativo.", errores["precioVenta"])
    }
}
