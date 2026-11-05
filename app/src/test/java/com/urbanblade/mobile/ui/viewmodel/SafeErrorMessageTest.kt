package com.urbanblade.mobile.ui.viewmodel

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Test

class SafeErrorMessageTest {
    @Test fun `unexpected exceptions never expose raw diagnostics`() {
        val diagnostics = listOf("Internal URL with token=synthetic", "SQLSTATE internal query", "Internal filesystem path")
        diagnostics.forEach { detail ->
            assertEquals("No se pudo completar la operación.", IllegalStateException(detail).toFriendlyMessage("No se pudo completar la operación."))
        }
    }

    @Test fun `network failures retain their actionable message`() {
        assertEquals("Sin conexión. Revisa tu red e inténtalo de nuevo.", IOException("internal host").toFriendlyMessage("Error"))
    }
}
