package com.urbanblade.mobile.ui.screens

import com.urbanblade.mobile.data.model.SystemServiceStatus
import com.urbanblade.mobile.data.model.SystemStatusResponse

/** Gravedad de un punto del sistema, de menor a mayor. */
enum class HealthLevel { OK, WARNING, DOWN }

data class HealthIssue(val level: HealthLevel, val title: String, val detail: String?)

data class SystemHealth(val level: HealthLevel, val headline: String, val issues: List<HealthIssue>)

/** Más de esto ya se siente en la app: conviene revisarlo aunque el servicio esté arriba. */
const val SLOW_LATENCY_MS = 1000

/**
 * Resume `GET admin/system/status` en una frase y la lista de lo que conviene revisar,
 * ordenada por gravedad. El backend manda "up"/"down" por servicio y "success"/"failed"
 * por tarea programada.
 */
fun systemHealth(status: SystemStatusResponse): SystemHealth {
    val issues = buildList {
        service("La base de datos", status.database)?.let(::add)
        service("Redis (caché, sesiones y colas)", status.redis)?.let(::add)
        status.queue.failed?.takeIf { it > 0 }?.let { failed ->
            add(
                HealthIssue(
                    HealthLevel.WARNING,
                    if (failed == 1) "1 trabajo fallido en la cola" else "$failed trabajos fallidos en la cola",
                    "Correos, notificaciones o PDF que no se generaron."
                )
            )
        }
        status.scheduledTasks.filter { it.status.equals("failed", ignoreCase = true) }.forEach { task ->
            add(HealthIssue(HealthLevel.WARNING, "Falló la tarea ${task.name ?: "programada"}", task.error))
        }
    }.sortedByDescending { it.level.ordinal }

    val level = issues.maxOfOrNull { it.level } ?: HealthLevel.OK
    val headline = when (level) {
        HealthLevel.OK -> "Todo funciona con normalidad"
        HealthLevel.WARNING -> if (issues.size == 1) "Funciona, con 1 punto por revisar" else "Funciona, con ${issues.size} puntos por revisar"
        HealthLevel.DOWN -> "Hay servicios caídos"
    }
    return SystemHealth(level, headline, issues)
}

/** El backend lo manda cuando nada de la configuración usa ese servicio (p. ej. Redis en staging). */
const val SERVICE_NOT_USED = "no_usado"

private fun service(name: String, s: SystemServiceStatus): HealthIssue? = when {
    s.status.equals(SERVICE_NOT_USED, ignoreCase = true) -> null
    s.status.equals("down", ignoreCase = true) -> HealthIssue(HealthLevel.DOWN, "$name no responde", s.error)
    !s.status.equals("up", ignoreCase = true) -> HealthIssue(HealthLevel.WARNING, "No se pudo verificar ${name.replaceFirstChar { it.lowercase() }}", s.error)
    (s.latencyMs ?: 0) > SLOW_LATENCY_MS -> HealthIssue(HealthLevel.WARNING, "$name responde lento", "${s.latencyMs} ms")
    else -> null
}
