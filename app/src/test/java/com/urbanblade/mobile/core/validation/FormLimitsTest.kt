package com.urbanblade.mobile.core.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Fija los topes que repiten las reglas `max:` del servidor (`barber`): si el backend cambia uno, esta prueba avisa. */
class FormLimitsTest {
    @Test
    fun `los topes coinciden con las reglas max del servidor`() {
        assertEquals(255, FormLimits.NAME_MAX)
        assertEquals(255, FormLimits.EMAIL_MAX)
        assertEquals(30, FormLimits.PHONE_MAX)
        assertEquals(1000, FormLimits.NOTES_MAX)
        assertEquals(500, FormLimits.COMMENT_MAX)
        assertEquals(1000, FormLimits.DESCRIPTION_MAX)
        assertEquals(1000, FormLimits.SPECIALTIES_MAX)
    }

    @Test
    fun `el contador solo aparece cerca del limite`() {
        assertFalse(FormLimits.nearLimit("a".repeat(799), FormLimits.NOTES_MAX))
        assertTrue(FormLimits.nearLimit("a".repeat(800), FormLimits.NOTES_MAX))
        assertTrue(FormLimits.nearLimit("a".repeat(1000), FormLimits.NOTES_MAX))
    }
}
