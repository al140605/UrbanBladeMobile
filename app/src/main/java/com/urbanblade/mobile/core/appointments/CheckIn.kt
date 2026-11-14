package com.urbanblade.mobile.core.appointments

import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val HOUR_MINUTE = DateTimeFormatter.ofPattern("HH:mm")

/**
 * ¿Se puede registrar la llegada del cliente? El servidor solo lo admite en citas de HOY en estado pendiente o
 * confirmada (`AppointmentCheckInController`); aquí se evita ofrecer un botón que respondería 422.
 */
fun canCheckIn(estado: String, fecha: String, today: LocalDate = LocalDate.now()): Boolean =
    estado in setOf("pendiente", "confirmada") && fecha.take(10) == today.toString()

/** «10:02» en la hora del teléfono a partir del ISO-8601 que manda el servidor (`llegada_en`); null si no se puede leer. */
fun arrivalTime(iso: String?, zone: ZoneId = ZoneId.systemDefault()): String? {
    if (iso.isNullOrBlank()) return null
    return try {
        OffsetDateTime.parse(iso).atZoneSameInstant(zone).format(HOUR_MINUTE)
    } catch (_: Exception) {
        null
    }
}
