package com.urbanblade.mobile.core.appointments

import com.google.gson.JsonParser
import java.time.OffsetDateTime
import kotlin.math.ceil

/*
 * Flujo de citas V2 en la app: tiempo restante del servicio en curso y lectura del 422 de «agregar tiempo». Funciones
 * puras (sin Android ni red) para poder probarlas con JUnit.
 */

/** Minutos que el barbero puede agregar de una vez (coinciden con config/appointments.php del backend). */
val EXTEND_OPTIONS = listOf(10, 15)

enum class RemainingTone { OK, SOON, OVER }

/** Minutos que faltan para [finIso] (negativo si ya se pasó); null si no hay un fin estimado válido. */
fun minutesLeft(finIso: String?, nowMillis: Long = System.currentTimeMillis()): Int? {
    if (finIso.isNullOrBlank()) return null
    val end = runCatching { OffsetDateTime.parse(finIso).toInstant().toEpochMilli() }.getOrNull() ?: return null
    return ceil((end - nowMillis) / 60_000.0).toInt()
}

/** Texto corto de la tarjeta del servicio en curso: «Quedan 12 min», «Termina en 1 min», «Se pasó 3 min». */
fun remainingLabel(minutes: Int?): String = when {
    minutes == null -> "Sin hora de fin"
    minutes > 1 -> "Quedan $minutes min"
    minutes == 1 -> "Termina en 1 min"
    minutes == 0 -> "Termina ahora"
    else -> "Se pasó ${-minutes} min"
}

/** Normal, por terminar (≤ 5 min) o pasado. */
fun remainingTone(minutes: Int?): RemainingTone = when {
    minutes == null || minutes > 5 -> RemainingTone.OK
    minutes >= 0 -> RemainingTone.SOON
    else -> RemainingTone.OVER
}

/** ¿El cuerpo del 422 de «agregar tiempo» permite confirmar y extender de todos modos (choca con la siguiente cita)? */
fun canForceFromBody(body: String?): Boolean = runCatching {
    JsonParser.parseString(body).asJsonObject.get("puede_forzar")?.asBoolean == true
}.getOrDefault(false)
