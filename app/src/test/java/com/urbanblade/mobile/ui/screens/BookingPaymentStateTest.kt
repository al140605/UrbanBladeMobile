package com.urbanblade.mobile.ui.screens

import com.urbanblade.mobile.data.model.SavedCard
import com.urbanblade.mobile.ui.viewmodel.BookingPayMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BookingPaymentStateTest {

    private val visa = SavedCard("pm_visa", "visa", "4242", 12, 2032)
    private val master = SavedCard("pm_master", "mastercard", "4444", 3, 2030)

    @Test
    fun `la propina se calcula sobre el precio del servicio`() {
        val state = BookingPaymentState()
        assertEquals(0.0, state.tipFor(450.0), 0.001)
        state.tipOption = BookingPaymentState.TIP_10
        assertEquals(45.0, state.tipFor(450.0), 0.001)
        state.tipOption = BookingPaymentState.TIP_15
        assertEquals(67.5, state.tipFor(450.0), 0.001)
        state.tipOption = BookingPaymentState.TIP_OTHER
        state.customTip = "80"
        assertEquals(80.0, state.tipFor(450.0), 0.001)
        state.customTip = ""
        assertEquals(0.0, state.tipFor(450.0), 0.001)
    }

    @Test
    fun `con tarjetas guardadas se usa la primera si no se eligio otra`() {
        val state = BookingPaymentState().apply { method = BookingPayMethod.TARJETA }
        assertEquals("pm_visa", state.savedCardToUse(listOf(visa, master)))
        state.selectedCardId = "pm_master"
        assertEquals("pm_master", state.savedCardToUse(listOf(visa, master)))
    }

    @Test
    fun `usar otra tarjeta o no tener guardadas paga con tarjeta nueva`() {
        val state = BookingPaymentState().apply { method = BookingPayMethod.TARJETA }
        assertNull(state.savedCardToUse(emptyList()))
        state.useNewCard = true
        assertNull(state.savedCardToUse(listOf(visa)))
    }

    @Test
    fun `una tarjeta elegida que ya no existe cae en la primera guardada`() {
        val state = BookingPaymentState().apply { method = BookingPayMethod.TARJETA; selectedCardId = "pm_borrada" }
        assertEquals("pm_visa", state.savedCardToUse(listOf(visa)))
    }

    @Test
    fun `sin metodo tarjeta nunca se usa una tarjeta guardada`() {
        val state = BookingPaymentState().apply { method = BookingPayMethod.TRANSFERENCIA }
        assertNull(state.savedCardToUse(listOf(visa)))
    }

    @Test
    fun `etiquetas de marca y vencimiento como en las apps de pago`() {
        assertEquals("Visa", cardBrandLabel("visa"))
        assertEquals("Mastercard", cardBrandLabel("mastercard"))
        assertEquals("American Express", cardBrandLabel("amex"))
        assertEquals("Tarjeta", cardBrandLabel(""))
        assertEquals("Vence 12/32", cardExpiryLabel(visa))
        assertEquals("Vence 03/30", cardExpiryLabel(master))
    }

    @Test
    fun `con el metodo en tarjeta se puede elegir la tarjeta guardada (membresia)`() {
        // La hoja de membresía arranca en TARJETA; en efectivo la tarjeta guardada nunca se usa.
        val state = BookingPaymentState()
        assertEquals(null, state.savedCardToUse(listOf(visa, master)))
        state.method = com.urbanblade.mobile.ui.viewmodel.BookingPayMethod.TARJETA
        assertEquals("pm_visa", state.savedCardToUse(listOf(visa, master)))
        state.useNewCard = false
        state.selectedCardId = "pm_master"
        assertEquals("pm_master", state.savedCardToUse(listOf(visa, master)))
    }
}
