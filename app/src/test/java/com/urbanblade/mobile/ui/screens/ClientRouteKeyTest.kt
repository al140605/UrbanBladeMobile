package com.urbanblade.mobile.ui.screens

import com.urbanblade.mobile.data.model.ClientRow
import org.junit.Assert.assertEquals
import org.junit.Test

/** El servidor resuelve `admin/clients/{client}` por slug: con el id el detalle respondía 404. */
class ClientRouteKeyTest {

    @Test
    fun `la clave de ruta del cliente es su slug`() {
        assertEquals("carlos-cliente", ClientRow(id = "6ae24578", slug = "carlos-cliente").routeKey())
    }

    @Test
    fun `sin slug se conserva el id`() {
        assertEquals("6ae24578", ClientRow(id = "6ae24578", slug = null).routeKey())
    }

    @Test
    fun `un slug vacio tampoco sirve de clave`() {
        assertEquals("6ae24578", ClientRow(id = "6ae24578", slug = "  ").routeKey())
    }
}
