package com.urbanblade.mobile.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El botón flotante de un módulo se oculta solo con el vacío CONFIRMADO (el estado vacío ya trae su acción): nunca
 * mientras carga, si falló la consulta o si hay elementos, para no dejar al administrador sin forma de crear.
 */
class UrbanListFeedbackTest {
    @Test
    fun `vacio confirmado es cargado, sin carga, sin error y sin elementos`() {
        assertTrue(isConfirmedEmpty(loaded = true, loading = false, hasItems = false, error = null))
    }

    @Test
    fun `mientras carga por primera vez no es vacio`() {
        assertFalse(isConfirmedEmpty(loaded = false, loading = true, hasItems = false, error = null))
        assertFalse(isConfirmedEmpty(loaded = false, loading = false, hasItems = false, error = null))
    }

    @Test
    fun `si la consulta fallo no es vacio y el flotante se queda`() {
        assertFalse(isConfirmedEmpty(loaded = false, loading = false, hasItems = false, error = "Sin conexión"))
        assertFalse(isConfirmedEmpty(loaded = true, loading = false, hasItems = false, error = "Sin conexión"))
    }

    @Test
    fun `mientras actualiza no es vacio`() {
        assertFalse(isConfirmedEmpty(loaded = true, loading = true, hasItems = false, error = null))
    }

    @Test
    fun `con elementos nunca es vacio`() {
        assertFalse(isConfirmedEmpty(loaded = true, loading = false, hasItems = true, error = null))
    }

    @Test
    fun `coincide con el estado EMPTY del listado`() {
        assertEquals(ListFeedbackState.EMPTY, listFeedbackState(true, false, false, null))
        assertTrue(isConfirmedEmpty(true, false, false, null))
    }
}
