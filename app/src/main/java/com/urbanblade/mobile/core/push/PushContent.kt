package com.urbanblade.mobile.core.push

/** Canales de notificación de Android que el backend nombra en el campo "channel" (ver PushRouting.php). */
enum class PushChannel(val id: String, val label: String, val description: String, val important: Boolean) {
    CITAS("citas", "Citas", "Recordatorios y cambios de tus citas en UrbanBlade", true),
    PAGOS("pagos", "Pagos", "Comprobantes y estado de tus pagos", true),
    PEDIDOS("pedidos", "Pedidos", "Avisos de tus pedidos de la tienda", false),
    FIDELIDAD("fidelidad", "Beneficios", "Niveles, puntos, sorteos y cumpleaños", false),
    PROMOCIONES("promociones", "Promociones", "Ofertas y novedades de la barbería", false),
    OPERACION("operacion", "Operación", "Avisos internos para el personal", true);

    companion object {
        /** Un canal desconocido o ausente cae en Citas, igual que en el backend. */
        fun from(id: String?): PushChannel = entries.firstOrNull { it.id == id } ?: CITAS
    }
}

/** Título, texto, canal y pantalla destino de la notificación del sistema. */
data class PushContent(
    val title: String,
    val body: String,
    val channel: PushChannel = PushChannel.CITAS,
    val route: String? = null
)

/** Pantallas a las que una notificación puede llevar. Lo demás se ignora (la app no abre rutas arbitrarias). */
val PUSH_ROUTES = setOf(
    "home", "appointments", "payments", "orders", "wallet", "notifications", "inventory",
    // Añadidas para los enlaces de los correos (urbanblade://open?route=...).
    "catalog", "barber_agenda", "profile",
)

fun safePushRoute(route: String?): String? = route?.trim()?.takeIf { it in PUSH_ROUTES }

/** Pantalla pedida por un enlace `urbanblade://open?route=<pantalla>`; null si no es de la app o no está permitida. */
fun routeFromDeepLink(scheme: String?, host: String?, route: String?): String? =
    if (scheme == "urbanblade" && host == "open") safePushRoute(route) else null

/**
 * Pantalla a la que lleva una notificación de la bandeja (GET /notifications) según su `type`. Son los
 * mismos tipos que clasifica el backend en PushRouting.php; un tipo desconocido no navega a ningún lado.
 */
fun routeForNotificationType(type: String?): String? = when (type) {
    "appointment", "review_request", "service_overrun" -> "appointments"
    "payment", "transfer_receipt", "membership_invoice" -> "payments"
    "order_delivered", "order_expired" -> "orders"
    "loyalty_level_up", "loyalty_level_downgraded", "loyalty_points_expired", "raffle_win", "client_birthday" -> "wallet"
    "promotion" -> "catalog"
    "inventory_low_stock" -> "inventory"
    else -> null
}

/**
 * Arma el contenido de una notificación push (T142) a partir de lo que llega de
 * Firebase: primero el bloque "notification"; si el backend manda solo "data",
 * se usan sus claves title/body (o message). Sin texto no se muestra nada.
 * "channel" y "route" salen de data. Es Kotlin puro para poder probarlo sin Android
 * (ver PushContentTest).
 */
fun pushContent(notificationTitle: String?, notificationBody: String?, data: Map<String, String>): PushContent? {
    val body = listOf(notificationBody, data["body"], data["message"])
        .firstOrNull { !it.isNullOrBlank() }?.trim() ?: return null
    val title = listOf(notificationTitle, data["title"])
        .firstOrNull { !it.isNullOrBlank() }?.trim() ?: "UrbanBlade"
    return PushContent(title, body, PushChannel.from(data["channel"]), safePushRoute(data["route"]))
}
