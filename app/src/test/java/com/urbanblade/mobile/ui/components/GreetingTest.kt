package com.urbanblade.mobile.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class GreetingTest {
    @Test fun `la manana va de las cinco a las once`() {
        assertEquals("Buenos días", greetingFor(5))
        assertEquals("Buenos días", greetingFor(11))
    }

    @Test fun `la tarde va de las doce a las seis`() {
        assertEquals("Buenas tardes", greetingFor(12))
        assertEquals("Buenas tardes", greetingFor(18))
    }

    @Test fun `la noche y la madrugada saludan de noche`() {
        assertEquals("Buenas noches", greetingFor(19))
        assertEquals("Buenas noches", greetingFor(0))
        assertEquals("Buenas noches", greetingFor(4))
    }
}
