package com.urbanblade.mobile.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsFormTest {

    private fun valid() = SettingsDraft(nombre = "UrbanBlade", politica = "24")

    @Test
    fun `un borrador minimo valido no tiene errores`() {
        assertTrue(validateSettings(valid()).isEmpty())
    }

    @Test
    fun `el nombre es obligatorio y tiene tope de 255`() {
        assertEquals("Escribe el nombre del negocio.", validateSettings(valid().copy(nombre = "  "))[SettingsField.NOMBRE])
        assertEquals("Máximo 255 caracteres.", validateSettings(valid().copy(nombre = "x".repeat(256)))[SettingsField.NOMBRE])
    }

    @Test
    fun `la politica de cancelacion va de 1 a 168 horas`() {
        assertNull(validateSettings(valid().copy(politica = "1"))[SettingsField.POLITICA])
        assertNull(validateSettings(valid().copy(politica = "168"))[SettingsField.POLITICA])
        assertEquals("Entre 1 y 168 horas.", validateSettings(valid().copy(politica = "0"))[SettingsField.POLITICA])
        assertEquals("Entre 1 y 168 horas.", validateSettings(valid().copy(politica = "169"))[SettingsField.POLITICA])
        assertEquals("Entre 1 y 168 horas.", validateSettings(valid().copy(politica = ""))[SettingsField.POLITICA])
    }

    @Test
    fun `el horario usa HH mm y el cierre debe ser posterior a la apertura`() {
        assertTrue(validateSettings(valid().copy(apertura = "09:00", cierre = "20:30")).isEmpty())
        assertEquals("Usa el formato HH:mm.", validateSettings(valid().copy(apertura = "9am"))[SettingsField.APERTURA])
        assertEquals("Usa el formato HH:mm.", validateSettings(valid().copy(cierre = "25:00"))[SettingsField.CIERRE])
        assertEquals(
            "El cierre debe ser después de la apertura.",
            validateSettings(valid().copy(apertura = "20:00", cierre = "09:00"))[SettingsField.CIERRE]
        )
        assertEquals(
            "El cierre debe ser después de la apertura.",
            validateSettings(valid().copy(apertura = "09:00", cierre = "09:00"))[SettingsField.CIERRE]
        )
    }

    @Test
    fun `un horario a medias no se compara y no marca error`() {
        assertTrue(validateSettings(valid().copy(apertura = "09:00")).isEmpty())
        assertTrue(validateSettings(valid().copy(cierre = "20:00")).isEmpty())
    }

    @Test
    fun `la CLABE vacia se acepta y si se escribe son 18 digitos`() {
        assertNull(validateSettings(valid().copy(clabe = ""))[SettingsField.CLABE])
        assertNull(validateSettings(valid().copy(clabe = "012345678901234567"))[SettingsField.CLABE])
        assertEquals("La CLABE tiene 18 dígitos.", validateSettings(valid().copy(clabe = "0123"))[SettingsField.CLABE])
        assertEquals("La CLABE tiene 18 dígitos.", validateSettings(valid().copy(clabe = "01234567890123456A"))[SettingsField.CLABE])
    }

    @Test
    fun `los campos de texto respetan los topes del servidor`() {
        val errors = validateSettings(
            valid().copy(
                direccion = "d".repeat(256),
                telefono = "1".repeat(31),
                banco = "b".repeat(101),
                beneficiario = "n".repeat(151),
                concepto = "c".repeat(101)
            )
        )
        assertEquals("Máximo 255 caracteres.", errors[SettingsField.DIRECCION])
        assertEquals("Máximo 30 caracteres.", errors[SettingsField.TELEFONO])
        assertEquals("Máximo 100 caracteres.", errors[SettingsField.BANCO])
        assertEquals("Máximo 150 caracteres.", errors[SettingsField.BENEFICIARIO])
        assertEquals("Máximo 100 caracteres.", errors[SettingsField.CONCEPTO])
    }
}
