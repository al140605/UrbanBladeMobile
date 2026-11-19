package com.urbanblade.mobile.ui.screens

import com.urbanblade.mobile.data.model.AppointmentRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppointmentPayableTest {

    private fun cita(
        estado: String = "confirmada",
        chargeable: Boolean = true,
        hasPayment: Boolean = false,
        status: String? = null,
        code: String? = "abc123"
    ) = AppointmentRow(
        id = "1", code = code, fecha = "2026-11-20", horaInicio = "10:00", estado = estado,
        hasPayment = hasPayment, isChargeable = chargeable, paymentStatus = status
    )

    @Test fun `una cita confirmada en efectivo sin pagos se puede pagar`() {
        assertTrue(cita(status = "pendiente_en_recepcion").isPayable())
    }

    @Test fun `contra un servidor anterior sin payment_status se mantiene la regla de siempre`() {
        assertTrue(cita(status = null).isPayable())
        assertFalse(cita(status = null, hasPayment = true).isPayable())
        assertFalse(cita(status = null, chargeable = false).isPayable())
    }

    @Test fun `transferencia en revision y pago hecho no ofrecen pagar`() {
        assertFalse(cita(status = "transferencia_en_revision", hasPayment = true).isPayable())
        assertFalse(cita(status = "pagado", hasPayment = true).isPayable())
    }

    @Test fun `sin codigo o sin ser cobrable no se paga`() {
        assertFalse(cita(code = null, status = "pendiente_en_recepcion").isPayable())
        assertFalse(cita(estado = "pendiente", chargeable = false, status = "sin_pago").isPayable())
    }

    @Test fun `las etiquetas de estado de pago estan en espanol`() {
        assertEquals("Pagado", paymentStatusLabel("pagado"))
        assertEquals("Pago en recepción", paymentStatusLabel("pendiente_en_recepcion"))
        assertEquals("Transferencia en revisión", paymentStatusLabel("transferencia_en_revision"))
        assertEquals("Sin pago", paymentStatusLabel("sin_pago"))
        assertEquals(null, paymentStatusLabel(null))
    }
}
