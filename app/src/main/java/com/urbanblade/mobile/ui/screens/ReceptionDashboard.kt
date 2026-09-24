package com.urbanblade.mobile.ui.screens

import com.google.gson.JsonElement
import com.google.gson.JsonObject

/** Cita de hoy que todavía espera confirmación (nextAppointments del tablero de recepción). */
data class ReceptionAppointment(val id: String, val hora: String, val cliente: String, val servicio: String, val barbero: String)

data class ReceptionPendingOrder(val id: String, val folio: String?, val cliente: String, val total: Double)

/** Tablero de recepción tal como lo arma DashboardController::receptionistPayload() en barber. */
data class ReceptionDashboard(
    val appointmentsToday: Int,
    val pendingPayments: Int,
    val newClientsToday: Int,
    val lowStock: Int,
    val pendingOrders: Int,
    val collectedToday: Double,
    val toConfirm: List<ReceptionAppointment>,
    val orders: List<ReceptionPendingOrder>
)

private fun JsonObject?.int(key: String): Int =
    this?.get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asInt ?: 0

private fun JsonObject?.double(key: String): Double =
    this?.get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asDouble ?: 0.0

private fun JsonObject.str(key: String): String? =
    get(key)?.takeIf { it.isJsonPrimitive }?.asString?.takeIf { it.isNotBlank() }

private fun JsonObject.objects(key: String): List<JsonObject> =
    get(key)?.takeIf(JsonElement::isJsonArray)?.asJsonArray?.mapNotNull { it.takeIf { e -> e.isJsonObject }?.asJsonObject }.orEmpty()

/** null si la respuesta no es de recepción (no trae kpis): la pantalla cae al tablero genérico. */
fun parseReceptionDashboard(data: JsonObject?): ReceptionDashboard? {
    val kpis = data?.get("kpis")?.takeIf { it.isJsonObject }?.asJsonObject ?: return null
    return ReceptionDashboard(
        appointmentsToday = kpis.int("appointments_today"),
        pendingPayments = kpis.int("pending_payments"),
        newClientsToday = kpis.int("new_clients_today"),
        lowStock = kpis.int("low_stock_count"),
        pendingOrders = kpis.int("pending_orders"),
        collectedToday = kpis.double("collected_today"),
        toConfirm = data.objects("nextAppointments").mapNotNull { a ->
            ReceptionAppointment(
                id = a.str("id") ?: return@mapNotNull null,
                hora = a.str("hora_inicio") ?: "",
                cliente = a.str("cliente") ?: "Cliente",
                servicio = a.str("servicio") ?: "—",
                barbero = a.str("barbero") ?: "—"
            )
        },
        orders = data.objects("pendingOrders").mapNotNull { o ->
            ReceptionPendingOrder(
                id = o.str("id") ?: return@mapNotNull null,
                folio = o.str("folio"),
                cliente = o.str("cliente") ?: "Cliente",
                total = o.double("total")
            )
        }
    )
}
