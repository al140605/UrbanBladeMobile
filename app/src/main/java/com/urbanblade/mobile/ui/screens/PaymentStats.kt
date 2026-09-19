package com.urbanblade.mobile.ui.screens

import com.urbanblade.mobile.data.model.PaymentRow
import java.time.LocalDate

/**
 * Resumen de los pagos que la pantalla ya tiene cargados (no consulta nada más). Cada pago cuenta
 * con su propina, igual que en el historial.
 */
data class PaymentStats(
    val total: Double,
    val tips: Double,
    val count: Int,
    val average: Double,
    /** Método de pago y monto, de mayor a menor. */
    val byMethod: List<Pair<String, Double>>,
    /** Los últimos días hasta hoy (sin huecos): etiqueta "dd" y monto cobrado ese día. */
    val byDay: List<Pair<String, Double>>
)

fun computePaymentStats(
    payments: List<PaymentRow>,
    days: Int = 14,
    today: LocalDate = LocalDate.now()
): PaymentStats {
    val total = payments.sumOf { it.monto + it.propina }
    val tips = payments.sumOf { it.propina }

    val byMethod = payments
        .groupBy { (it.metodoPago ?: "otro").trim().lowercase().ifEmpty { "otro" } }
        .map { (method, rows) -> method.replaceFirstChar { it.uppercase() } to rows.sumOf { it.monto + it.propina } }
        .sortedByDescending { it.second }

    val perDay = payments
        .mapNotNull { row ->
            val date = row.createdAt?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            date?.let { it to row.monto + row.propina }
        }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, amounts) -> amounts.sum() }
    val byDay = (days - 1 downTo 0).map { back ->
        val date = today.minusDays(back.toLong())
        date.dayOfMonth.toString().padStart(2, '0') to (perDay[date] ?: 0.0)
    }

    return PaymentStats(
        total = total,
        tips = tips,
        count = payments.size,
        average = if (payments.isEmpty()) 0.0 else total / payments.size,
        byMethod = byMethod,
        byDay = byDay
    )
}
