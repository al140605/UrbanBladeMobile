package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.JsonObject
import com.urbanblade.mobile.data.model.AuthUser
import com.urbanblade.mobile.ui.components.UrbanAvatar
import com.urbanblade.mobile.ui.components.UrbanCard
import com.urbanblade.mobile.ui.components.UrbanErrorBanner
import com.urbanblade.mobile.ui.components.UrbanFormat
import com.urbanblade.mobile.ui.components.UrbanInfoBanner
import com.urbanblade.mobile.ui.components.UrbanMetricCard
import com.urbanblade.mobile.ui.components.UrbanOutlineButton
import com.urbanblade.mobile.ui.components.UrbanPageHeader
import com.urbanblade.mobile.ui.components.UrbanPremiumCard
import com.urbanblade.mobile.ui.components.UrbanSectionTitle
import com.urbanblade.mobile.ui.components.UrbanSkeletonList
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.AdminHomeViewModel
import java.time.LocalDate
import java.util.Locale
import kotlin.math.abs

private fun JsonObject?.number(key: String): Double? =
    this?.get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asDouble

private fun JsonObject?.text(key: String): String? =
    this?.get(key)?.takeIf { it.isJsonPrimitive }?.asString?.takeIf { it.isNotBlank() }

private fun money(value: Double) = "\$" + String.format(Locale("es", "MX"), "%,.0f", value)

/**
 * Inicio del administrador: cómo va el negocio hoy, qué requiere su atención y accesos directos a
 * sus módulos. Reemplaza al tablero genérico de cuatro indicadores de citas (sin ingresos).
 */
@Composable
fun AdminHomeScreen(
    user: AuthUser,
    onNavigate: (String) -> Unit,
    vm: AdminHomeViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    LaunchedEffect(Unit) { vm.load() }

    val kpis = state.kpis
    val firstName = user.name.substringBefore(' ')
    val lowStock = kpis.number("low_stock_count")?.toInt() ?: 0
    val topBarber = kpis.text("top_barber_name")

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            UrbanPageHeader(
                title = "Hola, $firstName",
                subtitle = UrbanFormat.date(LocalDate.now().toString()),
                eyebrow = "ADMINISTRADOR",
                trailing = { UrbanAvatar(user.name, imageUrl = user.avatarUrl) }
            )
        }

        when {
            state.loading && kpis == null -> item { UrbanSkeletonList(2) }
            state.error != null && kpis == null -> item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    UrbanErrorBanner(state.error.orEmpty())
                    UrbanOutlineButton(
                        text = "Reintentar",
                        onClick = { vm.load() },
                        icon = Icons.Default.Refresh,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            kpis != null -> {
                item { TodayCard(kpis) }

                item { UrbanSectionTitle("Requiere tu atención", "Lo que conviene revisar ahora.") }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (state.pendingPayments > 0) {
                            AttentionRow(
                                icon = Icons.Default.Payments,
                                text = UrbanFormat.count(state.pendingPayments, "pago por verificar", "pagos por verificar"),
                                onClick = { onNavigate("payments") }
                            )
                        }
                        if (lowStock > 0) {
                            AttentionRow(
                                icon = Icons.Default.Inventory2,
                                text = UrbanFormat.count(lowStock, "producto con stock bajo", "productos con stock bajo"),
                                onClick = { onNavigate("inventory") }
                            )
                        }
                        if (state.pendingPayments == 0 && lowStock == 0) {
                            UrbanInfoBanner("Todo al día: no hay pagos ni inventario por atender.", Icons.Default.CheckCircle)
                        }
                    }
                }

                item { UrbanSectionTitle("Resumen del mes", "Así va este mes.") }
                item { MonthGrid(kpis) }

                if (topBarber != null) {
                    item { TopBarberCard(topBarber, kpis.number("top_barber_total")) }
                }
            }
        }

        item { UrbanSectionTitle("Módulos", "Tu negocio, a un toque.") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HomeTile("Citas", "Agenda del negocio", Icons.Default.CalendarMonth, { onNavigate("appointments") }, Modifier.weight(1f), highlighted = true)
                    HomeTile("Pagos", "Cobros y comprobantes", Icons.Default.Payments, { onNavigate("payments") }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HomeTile("Clientes", "Atención y gestión", Icons.Default.People, { onNavigate("clients") }, Modifier.weight(1f))
                    HomeTile("Inventario", "Stock y movimientos", Icons.Default.Inventory2, { onNavigate("inventory") }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HomeTile("Reportes", "Datos del negocio", Icons.Default.Assessment, { onNavigate("reports") }, Modifier.weight(1f))
                    HomeTile("Corte de caja", "Resumen del turno", Icons.Default.PointOfSale, { onNavigate("cash") }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HomeTile("Servicios", "Catálogo y precios", Icons.Default.ContentCut, { onNavigate("services_admin") }, Modifier.weight(1f))
                    HomeTile("Barberos", "Equipo y comisiones", Icons.Default.Badge, { onNavigate("barbers_admin") }, Modifier.weight(1f))
                }
            }
        }
        item { Spacer(Modifier.height(4.dp)) }
    }
}

@Composable
private fun TodayCard(kpis: JsonObject) {
    val incomeToday = kpis.number("income_today") ?: 0.0
    val appointmentsToday = kpis.number("appointments_today")?.toInt() ?: 0
    val incomeWeek = kpis.number("income_week")
    val appointmentsWeek = kpis.number("appointments_week")?.toInt()

    UrbanPremiumCard(Modifier.fillMaxWidth()) {
        Text(
            "HOY",
            style = MaterialTheme.typography.labelMedium,
            color = UrbanColors.Gold,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text("Ingresos", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                Text(money(incomeToday), style = MaterialTheme.typography.displaySmall, color = UrbanColors.Ink)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Citas", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                Text(appointmentsToday.toString(), style = MaterialTheme.typography.displaySmall, color = UrbanColors.Gold)
            }
        }
        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = UrbanColors.Line)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                buildString {
                    append("Esta semana: ")
                    append(incomeWeek?.let { money(it) } ?: "—")
                    if (appointmentsWeek != null) append(" · ${UrbanFormat.count(appointmentsWeek, "cita", "citas")}")
                },
                style = MaterialTheme.typography.bodySmall,
                color = UrbanColors.Muted,
                modifier = Modifier.weight(1f)
            )
        }
        val incomeGrowth = kpis.number("income_growth")
        val appointmentGrowth = kpis.number("appointment_growth")
        if (incomeGrowth != null || appointmentGrowth != null) {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TrendPill("Ingresos", incomeGrowth)
                TrendPill("Citas", appointmentGrowth)
            }
        }
    }
}

@Composable
private fun TrendPill(label: String, percent: Double?) {
    if (percent == null) return
    val up = percent >= 0
    val tone = if (up) UrbanColors.Success else UrbanColors.Danger
    Surface(shape = RoundedCornerShape(50), color = tone.copy(alpha = 0.14f)) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (up) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                null,
                tint = tone,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "$label ${if (up) "+" else "−"}${String.format(Locale("es", "MX"), "%.1f", abs(percent))}%",
                style = MaterialTheme.typography.labelMedium,
                color = tone
            )
        }
    }
}

@Composable
private fun AttentionRow(icon: ImageVector, text: String, onClick: () -> Unit) {
    UrbanCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(UrbanColors.Warning.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = UrbanColors.Warning, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.titleSmall, color = UrbanColors.Ink, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = UrbanColors.Muted)
        }
    }
}

@Composable
private fun MonthGrid(kpis: JsonObject) {
    val cells = buildList {
        kpis.number("income_month")?.let { add(Triple("Ingresos del mes", money(it), Icons.Default.AccountBalanceWallet)) }
        kpis.number("appointments_month")?.let { add(Triple("Citas del mes", it.toInt().toString(), Icons.Default.CalendarMonth)) }
        kpis.number("new_clients")?.let { add(Triple("Clientes nuevos", it.toInt().toString(), Icons.Default.People)) }
        kpis.number("recurring_clients")?.let { add(Triple("Clientes recurrentes", it.toInt().toString(), Icons.Default.People)) }
    }
    if (cells.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        cells.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (label, value, icon) ->
                    UrbanMetricCard(label = label, value = value, icon = icon, modifier = Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TopBarberCard(name: String, total: Double?) {
    UrbanCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(UrbanColors.Gold.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.EmojiEvents, null, tint = UrbanColors.Gold, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Barbero destacado del mes", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                Text(name, style = MaterialTheme.typography.titleMedium, color = UrbanColors.Ink)
            }
            total?.takeIf { it > 0 }?.let {
                Text(money(it), style = MaterialTheme.typography.titleMedium, color = UrbanColors.Gold)
            }
        }
    }
}
