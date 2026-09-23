package com.urbanblade.mobile.core.push

/** Título y texto que se muestran en la notificación del sistema. */
data class PushContent(val title: String, val body: String)

/**
 * Arma el contenido de una notificación push (T142) a partir de lo que llega de
 * Firebase: primero el bloque "notification"; si el backend manda solo "data",
 * se usan sus claves title/body (o message). Sin texto no se muestra nada.
 * Es Kotlin puro para poder probarlo sin Android (ver PushContentTest).
 */
fun pushContent(notificationTitle: String?, notificationBody: String?, data: Map<String, String>): PushContent? {
    val body = listOf(notificationBody, data["body"], data["message"])
        .firstOrNull { !it.isNullOrBlank() }?.trim() ?: return null
    val title = listOf(notificationTitle, data["title"])
        .firstOrNull { !it.isNullOrBlank() }?.trim() ?: "UrbanBlade"
    return PushContent(title, body)
}
