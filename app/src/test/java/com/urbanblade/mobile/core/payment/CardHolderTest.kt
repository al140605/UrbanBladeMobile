package com.urbanblade.mobile.core.payment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Nombre del titular y vista previa de la tarjeta que se llena mientras se escribe. */
class CardHolderTest {

    @Test
    fun `el nombre se escribe en mayusculas y sin simbolos ni numeros`() {
        assertEquals("LUIS GONZÁLEZ", normalizeHolderName("luis gonzález"))
        assertEquals("JOHN O'NEIL-SMITH JR.", normalizeHolderName("john o'neil-smith jr."))
        assertEquals("ANA LOPEZ", normalizeHolderName("ana123 *lopez#"))
    }

    @Test
    fun `espacios repetidos o iniciales se limpian`() {
        assertEquals("ANA LOPEZ", normalizeHolderName("   ana    lopez"))
    }

    @Test
    fun `el espacio final se conserva para poder escribir el apellido`() {
        assertEquals("ANA ", normalizeHolderName("ana "))
        assertEquals("ANA ", normalizeHolderName("ana   "))
        assertEquals("", normalizeHolderName("   "))
    }

    @Test
    fun `el nombre no pasa del largo de una tarjeta`() {
        assertEquals(HOLDER_MAX_LENGTH, normalizeHolderName("a".repeat(60)).length)
    }

    @Test
    fun `se pide nombre y apellido`() {
        assertTrue(isValidHolderName("LUIS GONZALEZ"))
        assertTrue(isValidHolderName("  Ana Lopez  "))
        assertFalse(isValidHolderName("LUIS"))
        assertFalse(isValidHolderName(""))
        assertFalse(isValidHolderName("A B"))
        assertFalse(isValidHolderName("..- -.."))
        assertNull(holderNameProblem("ANA LOPEZ"))
        assertNotNull(holderNameProblem("ANA"))
    }

    @Test
    fun `el numero se agrupa y se completa con puntos`() {
        assertEquals("•••• •••• •••• ••••", cardNumberPreview("", "unknown"))
        assertEquals("4242 42•• •••• ••••", cardNumberPreview("424242", "visa"))
        assertEquals("4242 4242 4242 4242", cardNumberPreview("4242424242424242", "visa"))
    }

    @Test
    fun `amex se agrupa 4-6-5`() {
        assertEquals("3782 822463 10005", cardNumberPreview("378282246310005", "amex"))
        assertEquals("3782 82•••• •••••", cardNumberPreview("378282", "AMEX"))
    }

    @Test
    fun `solo cuentan los digitos del numero`() {
        assertEquals("4242 4242 4242 4242", cardNumberPreview("4242-4242 4242.4242abc", "visa"))
        assertEquals("1234567890123456789", cardDigits("1234567890123456789012"))
    }

    @Test
    fun `el vencimiento se muestra como MM barra AA`() {
        assertEquals("", expiryPreview(""))
        assertEquals("1", expiryPreview("1"))
        assertEquals("12", expiryPreview("12"))
        assertEquals("12/3", expiryPreview("123"))
        assertEquals("12/34", expiryPreview("12/34"))
        assertEquals("12/34", expiryPreview("123456"))
    }

    @Test
    fun `la marca se detecta con los primeros digitos`() {
        assertEquals("visa", detectCardBrand("4242"))
        assertEquals("visa", detectCardBrand("4"))
        assertEquals("mastercard", detectCardBrand("5555"))
        assertEquals("mastercard", detectCardBrand("2221"))
        assertEquals("amex", detectCardBrand("3782"))
        assertEquals("amex", detectCardBrand("34"))
        assertEquals("discover", detectCardBrand("6011"))
        assertEquals("jcb", detectCardBrand("3530"))
        assertEquals("diners", detectCardBrand("3056"))
        assertEquals("unionpay", detectCardBrand("6200"))
    }

    @Test
    fun `sin digitos suficientes la marca es desconocida`() {
        assertEquals("unknown", detectCardBrand(""))
        assertEquals("unknown", detectCardBrand("5"))
        assertEquals("unknown", detectCardBrand("9999"))
    }
}
