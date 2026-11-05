package com.urbanblade.mobile.core.payment

import com.urbanblade.mobile.data.model.TransferQuote
import org.junit.Assert.*
import org.junit.Test

class TransferQuoteTest {
    private val quote = TransferQuote("a", "s", 270.0, 20.0, 290.0, "mxn")
    @Test fun `quote is bound to appointment and current tip`() {
        assertTrue(quote.matches(appointmentId = "a", tip = 20.0))
        assertFalse(quote.matches(appointmentId = "b", tip = 20.0))
        assertFalse(quote.matches(appointmentId = "a", tip = 10.0))
    }
    @Test fun `draft quote belongs to selected service and cannot replace appointment quote`() {
        val draft = quote.copy(appointmentId = null)
        assertTrue(draft.matches(serviceId = "s", tip = 20.0))
        assertFalse(draft.matches(serviceId = "other", tip = 20.0))
        assertFalse(draft.matches(appointmentId = "a", tip = 20.0))
        assertFalse(quote.matches(serviceId = "s", tip = 20.0))
    }
    @Test fun `missing inconsistent or unsupported amount never enables receipt upload`() {
        assertFalse(TransferQuote().matches(appointmentId = "a", tip = 0.0))
        for (invalid in listOf(quote.copy(total = 1.0), quote.copy(total = Double.NaN),
            quote.copy(moneda = "usd"), quote.copy(serviceAmount = -1.0), quote.copy(propina = -1.0))) {
            assertFalse(invalid.matches(appointmentId = "a", tip = 20.0))
        }
    }
}
