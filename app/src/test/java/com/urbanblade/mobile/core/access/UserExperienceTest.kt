package com.urbanblade.mobile.core.access

import org.junit.Assert.*
import org.junit.Test

class UserExperienceTest {
    @Test fun `cada rol mantiene su experiencia y personal gana al cliente sin depender del orden`() {
        UserExperience.priority.forEach { role ->
            assertEquals(role, UserExperience.primaryRole(listOf("cliente", role)))
            assertEquals(role, UserExperience.primaryRole(listOf(role, "cliente")))
        }
        assertEquals("recepcionista", UserExperience.primaryRole(listOf("barbero", "recepcionista")))
        assertEquals("guest", UserExperience.primaryRole(listOf("desconocido")))
        assertFalse(UserExperience.canBook(emptyList()))
    }

    @Test fun `el rol ingeniero retirado no cuenta como rol conocido`() {
        // Cuenta vieja que solo conserva «ingeniero»: sin rol vigente, ni cliente ni con reserva.
        assertEquals("guest", UserExperience.primaryRole(listOf("ingeniero")))
        assertFalse(UserExperience.hasKnownRole(listOf("ingeniero")))
        assertFalse(UserExperience.hasKnownRole(emptyList()))
        assertFalse(UserExperience.isClient(listOf("ingeniero")))
        assertFalse(UserExperience.canBook(listOf("ingeniero")))
        assertEquals("Cuenta", UserExperience.label(listOf("ingeniero")))
        // Con otro rol vigente, el ingeniero sobrante no estorba.
        assertEquals("barbero", UserExperience.primaryRole(listOf("ingeniero", "barbero")))
        assertEquals("Barbero", UserExperience.label(listOf("ingeniero", "barbero")))
        assertTrue(UserExperience.hasKnownRole(listOf("ingeniero", "cliente")))
        assertTrue(UserExperience.isClient(listOf("cliente", "ingeniero")))
    }
}
