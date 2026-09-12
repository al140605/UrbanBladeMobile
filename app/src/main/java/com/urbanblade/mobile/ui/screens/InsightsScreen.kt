package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.InsightsViewModel

private fun statusColor(status: String?) = when (status) {
    "positive" -> UrbanColors.Success
    "warning" -> UrbanColors.Warning
    "negative" -> UrbanColors.Danger
    else -> UrbanColors.Info
}

@Composable
fun InsightsScreen(onBack: () -> Unit, vm: InsightsViewModel = viewModel()) {
    val data by vm.data.collectAsState()
    val busy by vm.busy.collectAsState()
    val error by vm.error.collectAsState()

    LaunchedEffect(Unit) { vm.load() }

    val isEmpty = data.revenue == null && data.appointments == null && data.serviceConcentration == null

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = { UrbanTopBar("Insights IA", onBack) { IconButton(onClick = { vm.load() }, enabled = !busy) { Icon(Icons.Default.Refresh, "Actualizar") } } }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (busy) {
                item {
                    UrbanCard(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = UrbanColors.Gold)
                            Spacer(Modifier.width(12.dp))
                            Text("Calculando tendencias sobre el historial reciente…", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                        }
                    }
                }
            }
            error?.let { item { UrbanErrorBanner(it) } }

            data.revenue?.let { revenue ->
                item {
                    UrbanCard(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Ingresos", style = MaterialTheme.typography.titleMedium, color = UrbanColors.Gold)
                            Icon(Icons.Default.TrendingUp, null, tint = statusColor(revenue.status))
                        }
                        revenue.message?.let { Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp)) }
                        revenue.avgDaily?.let { UrbanKeyValue("Promedio diario", "\$${"%.2f".format(it)}", Modifier.padding(top = 8.dp)) }
                    }
                }
            }

            data.appointments?.let { appts ->
                item {
                    UrbanCard(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Citas", style = MaterialTheme.typography.titleMedium, color = UrbanColors.Gold)
                            Icon(Icons.Default.CalendarMonth, null, tint = statusColor(appts.status))
                        }
                        appts.message?.let { Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp)) }
                        appts.avgDaily?.let { UrbanKeyValue("Promedio diario", "%.1f".format(it), Modifier.padding(top = 8.dp)) }
                        appts.trend7d?.let { UrbanKeyValue("Últimos 7 días", "%.1f".format(it), Modifier.padding(top = 6.dp)) }
                    }
                }
            }

            data.serviceConcentration?.let { service ->
                item {
                    UrbanCard(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Concentración de servicios", style = MaterialTheme.typography.titleMedium, color = UrbanColors.Gold)
                            Icon(Icons.Default.PieChart, null, tint = statusColor(service.status))
                        }
                        service.message?.let { Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp)) }
                        service.topService?.let { UrbanKeyValue("Servicio top", it, Modifier.padding(top = 8.dp)) }
                        service.percentage?.let { UrbanKeyValue("Participación", "${"%.1f".format(it)}%", Modifier.padding(top = 6.dp)) }
                    }
                }
            }

            if (isEmpty && !busy) {
                item { UrbanEmptyState("Sin insights todavía", "Se necesitan más datos históricos para generar recomendaciones.", Icons.Default.Lightbulb) }
            }
        }
    }
}
