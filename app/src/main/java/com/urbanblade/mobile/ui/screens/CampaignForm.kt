package com.urbanblade.mobile.ui.screens

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Límites que valida el servidor (CampaignController::store). */
const val CAMPAIGN_TITLE_MAX = 150
const val CAMPAIGN_BODY_MAX = 2000
const val CAMPAIGN_CTA_LABEL_MAX = 40
const val CAMPAIGN_CTA_URL_MAX = 300

/** Borrador de campaña tal como está en el formulario. */
data class CampaignDraft(
    val titulo: String,
    val cuerpo: String,
    val ctaLabel: String,
    val ctaUrl: String,
    val schedule: Boolean,
    val scheduledAt: LocalDateTime?
)

/** Error por campo; todos nulos significa que el borrador es válido. */
data class CampaignErrors(
    val titulo: String? = null,
    val cuerpo: String? = null,
    val ctaLabel: String? = null,
    val ctaUrl: String? = null,
    val scheduledAt: String? = null,
    val segment: String? = null
) {
    val hasAny: Boolean get() = listOf(titulo, cuerpo, ctaLabel, ctaUrl, scheduledAt, segment).any { it != null }
}

/**
 * Anticipa lo que el servidor rechazaría, para no gastar un envío masivo en un error tonto. El
 * servidor sigue siendo la autoridad: esto solo evita el viaje y da el mensaje junto al campo.
 */
fun validateCampaign(draft: CampaignDraft, recipients: Int, now: LocalDateTime = LocalDateTime.now()): CampaignErrors {
    val url = draft.ctaUrl.trim()
    return CampaignErrors(
        titulo = when {
            draft.titulo.isBlank() -> "Escribe un título."
            draft.titulo.length > CAMPAIGN_TITLE_MAX -> "Máximo $CAMPAIGN_TITLE_MAX caracteres."
            else -> null
        },
        cuerpo = when {
            draft.cuerpo.isBlank() -> "Escribe el mensaje."
            draft.cuerpo.length > CAMPAIGN_BODY_MAX -> "Máximo $CAMPAIGN_BODY_MAX caracteres."
            else -> null
        },
        ctaLabel = if (draft.ctaLabel.length > CAMPAIGN_CTA_LABEL_MAX) "Máximo $CAMPAIGN_CTA_LABEL_MAX caracteres." else null,
        ctaUrl = when {
            url.isEmpty() -> null
            url.length > CAMPAIGN_CTA_URL_MAX -> "Máximo $CAMPAIGN_CTA_URL_MAX caracteres."
            !(url.startsWith("http://") || url.startsWith("https://")) || url.contains(' ') -> "Escribe un enlace completo, con https://"
            else -> null
        },
        scheduledAt = when {
            !draft.schedule -> null
            draft.scheduledAt == null -> "Elige fecha y hora."
            !draft.scheduledAt.isAfter(now) -> "Elige un momento en el futuro."
            else -> null
        },
        segment = if (recipients <= 0) "Este segmento no tiene clientes." else null
    )
}

private val SERVER_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

/** Fecha y hora en el formato que acepta el servidor. */
fun formatScheduleForServer(value: LocalDateTime): String = value.format(SERVER_FORMAT)

/** Nombre legible de un segmento; el de "inactive" explica qué significa. */
fun segmentLabel(key: String?, levels: Map<String, String>): String = when (key) {
    null, "" -> "—"
    "todos" -> "Todos los clientes"
    "inactive" -> "En riesgo (30+ días sin cita)"
    else -> levels[key] ?: key.replaceFirstChar { it.uppercase() }
}

/** Frase de la confirmación de envío, con el número de destinatarios. */
fun sendConfirmation(recipients: Int, schedule: Boolean, whenText: String?): String {
    val who = if (recipients == 1) "1 cliente" else "$recipients clientes"
    return if (schedule) {
        "Se programará para $who${whenText?.let { " el $it" } ?: ""}. Quienes desactivaron promociones no la recibirán."
    } else {
        "Se enviará ahora a $who y no se puede deshacer. Quienes desactivaron promociones no la recibirán."
    }
}
