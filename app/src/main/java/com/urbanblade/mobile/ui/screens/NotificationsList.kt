package com.urbanblade.mobile.ui.screens

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/** Una notificación lista para mostrarse (GET /notifications). */
data class NotificationItem(
    val id: String,
    val title: String,
    val message: String?,
    val type: String?,
    val read: Boolean,
    val createdAt: String?
)

data class NotificationsView(val items: List<NotificationItem>, val unread: Int)

private fun JsonObject.str(key: String): String? =
    get(key)?.takeIf { it.isJsonPrimitive }?.asString?.takeIf { it.isNotBlank() }

/**
 * Traduce la respuesta del servidor a una lista legible. Antes la pantalla pintaba el JSON tal cual
 * (llaves como "data", "meta" o "unread_count"). Cada clase de notificación de barber guarda su
 * propio arreglo en `data`; casi todas traen `title` y `message`, y algunas solo `body` o `subject`.
 */
fun parseNotifications(json: JsonObject?): NotificationsView {
    if (json == null) return NotificationsView(emptyList(), 0)
    val rows = json.get("data")?.takeIf(JsonElement::isJsonArray)?.asJsonArray ?: return NotificationsView(emptyList(), 0)
    val items = rows.mapNotNull { element ->
        val row = element.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
        val payload = row.get("data")?.takeIf { it.isJsonObject }?.asJsonObject ?: JsonObject()
        NotificationItem(
            id = row.str("id") ?: return@mapNotNull null,
            title = payload.str("title") ?: payload.str("subject") ?: "Aviso de UrbanBlade",
            message = payload.str("message") ?: payload.str("body"),
            type = payload.str("type"),
            read = row.str("read_at") != null,
            createdAt = row.str("created_at")
        )
    }
    val unread = json.get("meta")?.takeIf { it.isJsonObject }?.asJsonObject
        ?.get("unread")?.takeIf { it.isJsonPrimitive }?.asInt
        ?: items.count { !it.read }
    return NotificationsView(items, unread)
}

/** Secciones de la bandeja: lo no leído arriba y lo leído por antigüedad. */
enum class NotificationGroup(val label: String) {
    NEW("Nuevas"),
    TODAY("Hoy"),
    YESTERDAY("Ayer"),
    WEEK("Esta semana"),
    OLDER("Anteriores")
}

private fun NotificationItem.moment(zone: java.time.ZoneId): ZonedDateTime? =
    createdAt?.let { runCatching { OffsetDateTime.parse(it).atZoneSameInstant(zone) }.getOrNull() }

/** Lo no leído es siempre «Nuevas»; lo leído se reparte por días desde [now]. Sin fecha válida cae en «Anteriores». */
fun notificationGroup(item: NotificationItem, now: ZonedDateTime): NotificationGroup {
    if (!item.read) return NotificationGroup.NEW
    val moment = item.moment(now.zone) ?: return NotificationGroup.OLDER
    val days = ChronoUnit.DAYS.between(moment.toLocalDate(), now.toLocalDate())
    return when {
        days <= 0 -> NotificationGroup.TODAY
        days == 1L -> NotificationGroup.YESTERDAY
        days < 7 -> NotificationGroup.WEEK
        else -> NotificationGroup.OLDER
    }
}

/** Secciones en su orden de siempre, sin las vacías; dentro de cada una se conserva el orden del servidor (lo más nuevo primero). */
fun groupNotifications(items: List<NotificationItem>, now: ZonedDateTime): List<Pair<NotificationGroup, List<NotificationItem>>> =
    items.groupBy { notificationGroup(it, now) }
        .toSortedMap(compareBy { it.ordinal })
        .map { (group, rows) -> group to rows }

private val SHORT_MONTHS = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

/** «Ahora», «Hace 5 min», «Hace 2 h», «Ayer», «Hace 3 d» o «12 oct»; null si la fecha no se puede leer. */
fun relativeNotificationTime(createdAt: String?, now: ZonedDateTime): String? {
    val moment = createdAt?.let { runCatching { OffsetDateTime.parse(it).atZoneSameInstant(now.zone) }.getOrNull() } ?: return null
    val minutes = ChronoUnit.MINUTES.between(moment, now)
    if (minutes < 1) return "Ahora"
    if (minutes < 60) return "Hace $minutes min"
    val days = ChronoUnit.DAYS.between(moment.toLocalDate(), now.toLocalDate())
    if (days <= 0) return "Hace ${ChronoUnit.HOURS.between(moment, now)} h"
    if (days == 1L) return "Ayer"
    if (days < 7) return "Hace $days d"
    return "${moment.dayOfMonth} ${SHORT_MONTHS[moment.monthValue - 1]}"
}
