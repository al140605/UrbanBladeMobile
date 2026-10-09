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
    val route: String? = null,
    /** Botones de la notificación (p. ej. terminar / +10 / +15 min del aviso «tu servicio termina en 5 min»). */
    val actions: List<ServiceAction> = emptyList(),
    /** Código público de la cita sobre la que actúan los botones. */
    val appointmentCode: String? = null
)

/** Acción que el barbero puede hacer desde la notificación sin abrir la app. */
sealed class ServiceAction {
    /** Terminar el servicio ahora (cita → «completada»). */
    data object Finish : ServiceAction()

    /** Agregar [minutes] al servicio en curso. */
    data class Extend(val minutes: Int) : ServiceAction()
}

/** Minutos permitidos al agregar tiempo desde la notificación (los mismos del backend: config/appointments.php). */
private val NOTIFICATION_EXTEND_MINUTES = setOf(10, 15)

/**
 * Convierte el texto del backend (`terminar`, `extender_10`…) en una acción. Solo se aceptan las conocidas: un valor
 * raro (o de otra versión) simplemente no crea botón.
 */
fun parseServiceAction(raw: String?): ServiceAction? {
    val value = raw?.trim()?.lowercase() ?: return null
    if (value == "terminar") return ServiceAction.Finish
    val minutes = value.removePrefix("extender_").takeIf { value.startsWith("extender_") }?.toIntOrNull()
    return minutes?.takeIf { it in NOTIFICATION_EXTEND_MINUTES }?.let { ServiceAction.Extend(it) }
}

/** Texto de un botón de la notificación. */
fun ServiceAction.label(): String = when (this) {
    ServiceAction.Finish -> "Terminar ya"
    is ServiceAction.Extend -> "+$minutes min"
}

/** Clave estable de la acción para el intent del botón. */
fun ServiceAction.key(): String = when (this) {
    ServiceAction.Finish -> "terminar"
    is ServiceAction.Extend -> "extender_$minutes"
}

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
    val code = data["appointment_code"]?.trim()?.takeIf { it.isNotEmpty() }
    // Los botones necesitan la cita: sin código no se muestran aunque el mensaje los pida.
    val actions = if (code == null) emptyList()
    else data["acciones"].orEmpty().split(',').mapNotNull(::parseServiceAction).distinct()
    return PushContent(title, body, PushChannel.from(data["channel"]), safePushRoute(data["route"]), actions, code)
}
