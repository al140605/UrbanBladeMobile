package com.urbanblade.mobile.ui.screens

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CampaignFormTest {

    private val now = LocalDateTime.of(2026, 9, 18, 12, 0)

    private fun draft(
        titulo: String = "Promo",
        cuerpo: String = "20% en cortes",
        ctaLabel: String = "",
        ctaUrl: String = "",
        schedule: Boolean = false,
        at: LocalDateTime? = null
    ) = CampaignDraft(titulo, cuerpo, ctaLabel, ctaUrl, schedule, at)

    @Test
    fun `un borrador completo a un segmento con clientes es valido`() {
        assertFalse(validateCampaign(draft(), recipients = 10, now = now).hasAny)
    }

    @Test
    fun `titulo y mensaje son obligatorios y tienen tope`() {
        val empty = validateCampaign(draft(titulo = " ", cuerpo = ""), 10, now)
        assertEquals("Escribe un título.", empty.titulo)
        assertEquals("Escribe el mensaje.", empty.cuerpo)

        val long = validateCampaign(draft(titulo = "x".repeat(151), cuerpo = "y".repeat(2001)), 10, now)
        assertEquals("Máximo 150 caracteres.", long.titulo)
        assertEquals("Máximo 2000 caracteres.", long.cuerpo)
    }

    @Test
    fun `el enlace debe ser completo y sin espacios, y es opcional`() {
        assertNull(validateCampaign(draft(ctaUrl = ""), 10, now).ctaUrl)
        assertNull(validateCampaign(draft(ctaUrl = "https://urbanblade.mx/reservar"), 10, now).ctaUrl)
        assertEquals("Escribe un enlace completo, con https://", validateCampaign(draft(ctaUrl = "urbanblade.mx"), 10, now).ctaUrl)
        assertEquals("Escribe un enlace completo, con https://", validateCampaign(draft(ctaUrl = "https://a b.mx"), 10, now).ctaUrl)
    }

    @Test
    fun `programar exige una fecha futura`() {
        assertEquals("Elige fecha y hora.", validateCampaign(draft(schedule = true), 10, now).scheduledAt)
        assertEquals("Elige un momento en el futuro.", validateCampaign(draft(schedule = true, at = now.minusHours(1)), 10, now).scheduledAt)
        assertNull(validateCampaign(draft(schedule = true, at = now.plusDays(1)), 10, now).scheduledAt)
        // enviar ahora ignora la fecha
        assertNull(validateCampaign(draft(schedule = false, at = now.minusDays(9)), 10, now).scheduledAt)
    }

    @Test
    fun `un segmento sin clientes no se puede enviar`() {
        val errors = validateCampaign(draft(), recipients = 0, now = now)
        assertEquals("Este segmento no tiene clientes.", errors.segment)
        assertTrue(errors.hasAny)
    }

    @Test
    fun `la fecha se manda en el formato del servidor`() {
        assertEquals("2026-09-21 10:05", formatScheduleForServer(LocalDateTime.of(2026, 9, 21, 10, 5)))
    }

    @Test
    fun `los segmentos tienen nombres claros`() {
        val levels = mapOf("vip" to "VIP", "regular" to "Regular")
        assertEquals("Todos los clientes", segmentLabel("todos", levels))
        assertEquals("En riesgo (30+ días sin cita)", segmentLabel("inactive", levels))
        assertEquals("VIP", segmentLabel("vip", levels))
        assertEquals("Otro", segmentLabel("otro", levels))
        assertEquals("—", segmentLabel(null, levels))
    }

    @Test
    fun `la confirmacion dice a cuantos clientes llega y si se puede deshacer`() {
        assertEquals(
            "Se enviará ahora a 1 cliente y no se puede deshacer. Quienes desactivaron promociones no la recibirán.",
            sendConfirmation(1, false, null)
        )
        assertEquals(
            "Se programará para 42 clientes el 21 sep · 10:00. Quienes desactivaron promociones no la recibirán.",
            sendConfirmation(42, true, "21 sep · 10:00")
        )
    }
}
