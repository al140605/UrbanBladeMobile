package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.JsonObject
import com.urbanblade.mobile.data.model.AuthUser
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(
    user: AuthUser,
    onAppointments: () -> Unit,
    onBook: () -> Unit,
    vm: DashboardViewModel = viewModel()
) {
    val dashboard by vm.data.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    LaunchedEffect(Unit) { vm.load() }

    val firstName = user.name.substringBefore(' ')
    val primaryRole = dashboard?.role ?: user.roles.firstOrNull().orEmpty()
    val canBook = user.roles.any { it in listOf("cliente", "administrador", "recepcionista") }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            UrbanPageHeader(
                title = "Hola, $firstName",
                subtitle = "Tu jornada UrbanBlade está lista.",
                eyebrow = primaryRole.ifBlank { "UrbanBlade" },
                trailing = { UrbanAvatar(user.name) }
            )
        }

        item {
            UrbanPremiumCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text("SASTRERÍA NOCTURNA", style = MaterialTheme.typography.labelMedium, color = UrbanColors.Gold)
                        Spacer(Modifier.height(6.dp))
                        Text("Todo el negocio,\nen tu bolsillo.", style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Agenda, clientes, pagos y operación conectados al mismo backend.",
                            style = MaterialTheme.typography.bodySmall,
                            color = UrbanColors.Muted
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Box(Modifier.size(74.dp)) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = MaterialTheme.shapes.large,
                            color = UrbanColors.Gold.copy(alpha = 0.12f)
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                                Icon(Icons.Default.ContentCut, null, tint = UrbanColors.Gold, modifier = Modifier.size(34.dp))
                            }
                        }
                    }
                }
            }
        }

        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = UrbanColors.Gold) }
        error?.let { item { UrbanErrorBanner(it) } }

        dashboard?.let { response ->
            val kpis = extractKpis(response.data)
            if (kpis.isNotEmpty()) {
                item {
                    UrbanSectionTitle(
                        title = "Resumen",
                        subtitle = "Indicadores principales de ${primaryRole.replace('_', ' ')}"
                    )
                }
                items(kpis.chunked(2)) { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { (label, value) ->
                            UrbanMetricCard(
                                label = label,
                                value = value,
                                icon = iconForKpi(label),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            UrbanSectionTitle(
                title = "Acciones rápidas",
                subtitle = "Lo que más usas, a un toque."
            )
        }
        item {
            UrbanQuickAction(
                title = "Mis citas",
                subtitle = "Consulta próximas y anteriores",
                icon = Icons.Default.CalendarMonth,
                onClick = onAppointments,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (canBook) {
            item {
                UrbanQuickAction(
                    title = "Reservar cita",
                    subtitle = "Solo horarios realmente disponibles",
                    icon = Icons.Default.ContentCut,
                    onClick = onBook,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item {
            UrbanInfoBanner(
                "La app adapta cada módulo a tus roles, pero Laravel sigue siendo la autoridad de permisos y datos.",
                Icons.Default.VerifiedUser
            )
        }
        item { Spacer(Modifier.height(4.dp)) }
    }
}

private fun iconForKpi(label: String): ImageVector = when {
    label.contains("ingreso", ignoreCase = true) -> Icons.Default.Payments
    label.contains("cliente", ignoreCase = true) -> Icons.Default.People
    label.contains("cita", ignoreCase = true) -> Icons.Default.EventAvailable
    else -> Icons.Default.Insights
}

private fun extractKpis(data: JsonObject): List<Pair<String, String>> {
    val source = data.getAsJsonObject("kpis") ?: data
    val friendly = mapOf(
        "appointments_today" to "Citas hoy",
        "appointments_week" to "Citas esta semana",
        "revenue_today" to "Ingresos hoy",
        "revenue_month" to "Ingresos del mes",
        "clients_total" to "Clientes",
        "total_clients" to "Clientes",
        "pending_appointments" to "Citas pendientes"
    )
    val result = mutableListOf<Pair<String, String>>()
    for ((key, value) in source.entrySet()) {
        if (!value.isJsonPrimitive) continue
        val primitive = value.asJsonPrimitive
        if (!primitive.isNumber && !primitive.isString) continue
        val label = friendly[key] ?: key.replace('_', ' ').replaceFirstChar { it.uppercase() }
        result += label to primitive.asString
        if (result.size == 4) break
    }
    return result
}
