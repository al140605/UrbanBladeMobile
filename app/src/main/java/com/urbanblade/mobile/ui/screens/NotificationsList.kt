package com.urbanblade.mobile.ui.screens

import com.google.gson.JsonElement
import com.google.gson.JsonObject

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
