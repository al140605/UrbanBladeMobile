package com.urbanblade.mobile.ui.screens

import com.urbanblade.mobile.data.model.AppointmentRow

private val ACTIVE = setOf("pendiente", "confirmada", "en_proceso")

/** Resumen del día del barbero a partir de su agenda de hoy. */
data class BarberDay(
    /** La cita que está atendiendo ahora, si hay una en proceso. */
    val inProcess: AppointmentRow?,
    /** La siguiente cita por atender (la primera desde la hora actual; si todas quedaron atrás, la más temprana). */
    val next: AppointmentRow?,
    /** Citas que todavía requieren algo hoy: pendientes, confirmadas o en proceso. */
    val remaining: Int,
    val done: Int,
    /** Citas que el cliente pidió y el barbero aún no confirma. */
    val toConfirm: Int
)

/** [now] en formato HH:mm; las horas del backend vienen como HH:mm:ss. */
fun barberDay(rows: List<AppointmentRow>, now: String): BarberDay {
    val active = rows.filter { it.estado in ACTIVE }
    val upcoming = active.filter { it.estado != "en_proceso" }.sortedBy { it.horaInicio }
    return BarberDay(
        inProcess = active.firstOrNull { it.estado == "en_proceso" },
        next = upcoming.firstOrNull { it.horaInicio.take(5) >= now } ?: upcoming.firstOrNull(),
        remaining = active.size,
        done = rows.count { it.estado == "completada" },
        toConfirm = rows.count { it.estado == "pendiente" }
    )
}

/** Texto del botón para pasar una cita a [estado], como lo diría el barbero. */
fun statusActionLabel(estado: String): String = when (estado) {
    "confirmada" -> "Confirmar"
    "en_proceso" -> "Iniciar servicio"
    "completada" -> "Terminar"
    "cancelada" -> "Cancelar"
    "no_asistio" -> "No asistió"
    else -> estado.replace('_', ' ').replaceFirstChar { it.uppercase() }
}

/** Nombre de un estado para filtros y etiquetas. */
fun statusLabel(estado: String): String = when (estado) {
    "pendiente" -> "Por confirmar"
    "confirmada" -> "Confirmadas"
    "en_proceso" -> "En proceso"
    "completada" -> "Completadas"
    "cancelada" -> "Canceladas"
    "no_asistio" -> "No asistió"
    else -> estado.replace('_', ' ').replaceFirstChar { it.uppercase() }
}

/** Cancelar o marcar inasistencia no se puede deshacer desde la agenda: se confirma antes. */
fun isDestructiveStatus(estado: String) = estado == "cancelada" || estado == "no_asistio"

/**
 * El backend devuelve el horario como HH:mm:ss pero al guardarlo exige HH:mm (date_format:H:i).
 * Sin recortarlo, la pantalla marcaba todo en rojo y no dejaba guardar.
 */
fun normalizeTime(value: String): String =
    if (Regex("^[0-9]{2}:[0-9]{2}:[0-9]{2}$").matches(value)) value.take(5) else value

/** Minutos entre dos horas HH:mm; null si alguna no es válida o el cierre no es posterior. */
fun minutesBetween(start: String, end: String): Int? {
    fun parse(v: String): Int? {
        val m = Regex("^([01]\\d|2[0-3]):([0-5]\\d)$").find(v) ?: return null
        return m.groupValues[1].toInt() * 60 + m.groupValues[2].toInt()
    }
    val s = parse(start) ?: return null
    val e = parse(end) ?: return null
    return (e - s).takeIf { it > 0 }
}
