package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.urbanblade.mobile.data.model.PaymentRow
import com.urbanblade.mobile.ui.components.UrbanBarChart
import com.urbanblade.mobile.ui.components.UrbanCard
import com.urbanblade.mobile.ui.components.UrbanFormat
import com.urbanblade.mobile.ui.components.UrbanHBars
import com.urbanblade.mobile.ui.components.UrbanPremiumCard
import com.urbanblade.mobile.ui.components.UrbanSectionTitle
import com.urbanblade.mobile.ui.theme.UrbanColors

private fun money(value: Double) = "$" + String.format(java.util.Locale("es", "MX"), "%,.2f", value)
private fun moneyShort(value: Double) = "$" + String.format(java.util.Locale("es", "MX"), "%,.0f", value)

/** Resumen visual de los pagos cargados: total, ticket promedio, últimos días y desglose por método. */
@Composable
internal fun PaymentsSummary(payments: List<PaymentRow>, staff: Boolean) {
    val stats = remember(payments) { computePaymentStats(payments) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        UrbanPremiumCard(Modifier.fillMaxWidth()) {
            Text(
                if (staff) "Cobrado" else "Total pagado",
                style = MaterialTheme.typography.labelLarge,
                color = UrbanColors.Gold
            )
            Text(
                money(stats.total),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = UrbanColors.Ink
            )
            Text(
                UrbanFormat.count(stats.count, "pago", "pagos") + " en el historial",
                style = MaterialTheme.typography.bodySmall,
                color = UrbanColors.Muted
            )
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Stat("Ticket promedio", moneyShort(stats.average), Modifier.weight(1f))
                Stat("Propinas", moneyShort(stats.tips), Modifier.weight(1f))
            }
        }

        if (stats.byDay.any { it.second > 0.0 }) {
            UrbanCard(Modifier.fillMaxWidth()) {
                UrbanSectionTitle("Últimos 14 días", "Toca una barra para ver el día")
                Spacer(Modifier.height(12.dp))
                UrbanBarChart(
                    labels = stats.byDay.map { it.first },
                    values = stats.byDay.map { it.second },
                    format = ::moneyShort
                )
            }
        }

        if (stats.byMethod.size > 1) {
            UrbanCard(Modifier.fillMaxWidth()) {
                UrbanSectionTitle("Por método de pago")
                Spacer(Modifier.height(12.dp))
                UrbanHBars(
                    labels = stats.byMethod.map { it.first },
                    values = stats.byMethod.map { it.second },
                    format = ::moneyShort
                )
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = UrbanColors.Muted)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = UrbanColors.Ink)
    }
}
