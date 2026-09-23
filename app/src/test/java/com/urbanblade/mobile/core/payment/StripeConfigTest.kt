package com.urbanblade.mobile.core.payment

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** T144 (HU-17): sin publishable key real la app no debe ofrecer el pago con tarjeta. */
class StripeConfigTest {

    @Test
    fun `una publishable key de prueba real habilita la tarjeta`() {
        assertTrue(isStripeConfigured("pk_test_51AbCdEf"))
    }

    @Test
    fun `el valor por defecto sin configurar deshabilita la tarjeta`() {
        assertFalse(isStripeConfigured("pk_test_PENDIENTE_CONFIGURAR"))
    }

    @Test
    fun `una clave vacia o secreta no se acepta`() {
        assertFalse(isStripeConfigured(""))
        assertFalse(isStripeConfigured("sk_test_123"))
    }
}
