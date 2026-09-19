package com.urbanblade.mobile.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentReviewTest {

    @Test
    fun `mismo monto o dentro de la tolerancia coincide`() {
        assertEquals(TransferCheck.Match, compareTransfer(180.0, 180.0))
        assertEquals(TransferCheck.Match, compareTransfer(180.4, 180.0))
    }

    @Test
    fun `un monto distinto se marca como diferente sin ser un error`() {
        assertEquals(TransferCheck.Differs, compareTransfer(171.0, 180.0))
    }

    @Test
    fun `sin precio de lista no se puede comparar`() {
        assertEquals(TransferCheck.Unknown, compareTransfer(180.0, null))
        assertEquals(TransferCheck.Unknown, compareTransfer(180.0, 0.0))
    }

    @Test
    fun `el OCR solo discrepa cuando lee un monto distinto`() {
        assertFalse(ocrDisagrees(180.0, null))
        assertFalse(ocrDisagrees(180.0, 180.0))
        assertTrue(ocrDisagrees(180.0, 150.0))
    }
}
