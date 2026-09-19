package com.urbanblade.mobile.ui.screens

import com.urbanblade.mobile.ui.components.UrbanFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ClientsHelpersTest {

    @Test
    fun `no avisa si el cliente viene con regularidad o no hay dato`() {
        assertNull(inactivityNote(null))
        assertNull(inactivityNote(0))
        assertNull(inactivityNote(44))
    }

    @Test
    fun `avisa desde los 45 dias sin venir`() {
        assertEquals("Hace 45 días que no viene. Un mensaje o llamada ahora puede recuperarlo.", inactivityNote(45))
        assertEquals("Hace 90 días que no viene. Un mensaje o llamada ahora puede recuperarlo.", inactivityNote(90))
    }

    @Test
    fun `la fecha corta usa dia y mes abreviado o devuelve el texto original`() {
        assertEquals("21 sep", UrbanFormat.dateShort("2026-09-21"))
        assertEquals("21 sep", UrbanFormat.dateShort("2026-09-21T10:00:00Z"))
        assertEquals("pronto", UrbanFormat.dateShort("pronto"))
    }
}
