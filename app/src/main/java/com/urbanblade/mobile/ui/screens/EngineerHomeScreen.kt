package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urbanblade.mobile.data.model.AuthUser
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.LogsViewModel
import com.urbanblade.mobile.ui.viewmodel.SystemStatusViewModel
import java.time.LocalDate

internal fun healthTone(level: HealthLevel): Color = when (level) {
    HealthLevel.OK -> UrbanColors.Success
    HealthLevel.WARNING -> UrbanColors.Warning
    HealthLevel.DOWN -> UrbanColors.Danger
}

internal fun healthIcon(level: HealthLevel) = when (level) {
    HealthLevel.OK -> Icons.Default.CheckCircle
    HealthLevel.WARNING -> Icons.Default.Warning
    HealthLevel.DOWN -> Icons.Default.Error
}

/**
 * Inicio del ingeniero (rol de solo lectura): salud del servidor, actividad de hoy y acceso a
 * sus módulos de análisis. Antes veía el tablero genérico de "agenda, clientes y pagos",
 * que su rol no puede gestionar.
 */
@Composable
fun EngineerHomeScreen(
    user: AuthUser,
    onNavigate: (String) -> Unit,
    systemVm: SystemStatusViewModel = viewModel(),
    logsVm: LogsViewModel = viewModel()
) {
    val status by systemVm.status.collectAsState()
    val loading by systemVm.busy.collectAsState()
    val error by systemVm.error.collectAsState()
    val logs by logsVm.logs.collectAsState()
    LaunchedEffect(Unit) {
        systemVm.load()
        logsVm.load(null, event = null)
    }

    val health = status?.let(::systemHealth)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            UrbanPageHeader(
                title = "Hola, ${user.name.substringBefore(' ')}",
                subtitle = UrbanFormat.date(LocalDate.now().toString()),
                eyebrow = "INGENIERO",
                trailing = { UrbanAvatar(user.name, imageUrl = user.avatarUrl) }
            )
        }

        when {
            loading && status == null -> item { UrbanSkeletonList(1) }
            error != null && status == null -> item {
                UrbanMascotState(
                    UrbanStateKind.ERROR,
                    "No pudimos revisar el servidor",
                    error,
                    actionLabel = "Reintentar",
                    onAction = { systemVm.load() }
                )
            }
            health != null -> {
                item {
                    UrbanHeroCard(Modifier.clickable { onNavigate("system") }) {
                        UrbanHeroLabel("Estado del sistema")
                        Spacer(Modifier.height(6.dp))
                        Text(health.headline, style = MaterialTheme.typography.headlineSmall, color = UrbanColors.Ink)
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = UrbanColors.Ink.copy(alpha = 0.22f))
                        Spacer(Modifier.height(14.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            UrbanHeroStat(
                                "Base de datos",
                                status?.database?.latencyMs?.let { "$it ms" } ?: "—",
                                Icons.Default.Storage,
                                Modifier.weight(1f),
                                tone = serviceTone(status?.database?.status)
                            )
                            UrbanHeroStat(
                                "Fallidos en cola",
                                status?.queue?.failed?.toString() ?: "—",
                                Icons.Default.Dns,
                                Modifier.weight(1f),
                                tone = if ((status?.queue?.failed ?: 0) > 0) UrbanColors.Warning else UrbanColors.Gold
                            )
                        }
                    }
                }
                if (health.issues.isNotEmpty()) {
                    item { UrbanSectionTitle("Requiere tu atención", "Ordenado de más a menos grave.") }
                    health.issues.forEach { issue ->
                        item {
                            UrbanAttentionRow(healthIcon(issue.level), issue.title, issue.detail, healthTone(issue.level), onClick = { onNavigate("system") })
                        }
                    }
                }
            }
        }

        item { UrbanSectionTitle("Actividad de hoy", "Movimientos registrados en la bitácora.") }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                UrbanMetricCard("Hoy", logs.stats.hoy.toString(), Icons.Default.Today, Modifier.weight(1f))
                UrbanMetricCard("Creados", logs.stats.creates.toString(), Icons.Default.AddCircle, Modifier.weight(1f))
                UrbanMetricCard("Eliminados", logs.stats.deletes.toString(), Icons.Default.RemoveCircle, Modifier.weight(1f))
            }
        }

        item { UrbanSectionTitle("Tus módulos", "Análisis del negocio y del servidor.") }
        item {
            UrbanModuleGrid(
                tiles = listOf(
                    Triple("Reportes", "Ingresos, citas e inventario", Icons.Default.Assessment),
                    Triple("Logs", "Quién hizo qué y cuándo", Icons.Default.Terminal),
                    Triple("Métricas", "KPIs del negocio", Icons.Default.MonitorHeart),
                    Triple("Insights IA", "Tendencias y alertas", Icons.Default.AutoAwesome),
                    Triple("Analítica", "Operación y clientes", Icons.Default.QueryStats),
                    Triple("Sistema", "Servicios y tareas", Icons.Default.Dns),
                ),
                routes = listOf("reports", "logs", "admin_metrics", "insights", "analytics", "system"),
                onNavigate = onNavigate
            )
        }
    }
}

internal fun serviceTone(status: String?): Color = when {
    status.equals("up", ignoreCase = true) -> UrbanColors.Success
    status.equals("down", ignoreCase = true) -> UrbanColors.Danger
    else -> UrbanColors.Warning
}
