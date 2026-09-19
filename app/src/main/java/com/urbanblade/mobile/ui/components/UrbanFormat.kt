package com.urbanblade.mobile.ui.components

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

/**
 * Formatos legibles para lo que la API manda en ISO ("2026-09-21", "10:00"). Si algo no se puede
 * interpretar, se devuelve el texto original en lugar de fallar.
 */
object UrbanFormat {
    private val es = Locale("es", "MX")

    /** "2026-09-21" -> "Lunes 21 de septiembre". */
    fun date(iso: String): String = runCatching {
        val d = LocalDate.parse(iso.take(10))
        val day = d.dayOfWeek.getDisplayName(TextStyle.FULL, es).replaceFirstChar { it.uppercase() }
        "$day ${d.dayOfMonth} de ${d.month.getDisplayName(TextStyle.FULL, es)}"
    }.getOrDefault(iso)

    /** "10:00" o "10:00:00" -> "10:00 AM" (mismo estilo que los horarios del asistente de reserva). */
    fun time(hhmm: String): String = runCatching {
        val t = LocalTime.parse(hhmm.take(5))
        val hour12 = if (t.hour % 12 == 0) 12 else t.hour % 12
        String.format(Locale.US, "%d:%02d %s", hour12, t.minute, if (t.hour < 12) "AM" else "PM")
    }.getOrDefault(hhmm)

    fun count(n: Int, one: String, many: String): String = if (n == 1) "1 $one" else "$n $many"
}
