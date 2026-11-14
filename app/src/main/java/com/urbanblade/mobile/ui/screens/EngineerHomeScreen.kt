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
import com.urbanblade.mobile.ui.theme.UrbanSpace

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
    val logsLoading by logsVm.busy.collectAsState()
    val logsError by logsVm.error.collectAsState()
    val logsLoaded by logsVm.hasLoaded.collectAsState()
    LaunchedEffect(Unit) {
        systemVm.load()
        logsVm.load(null, event = null)
    }

    val health = status?.let(::systemHealth)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = UrbanSpace.Gutter, end = UrbanSpace.Gutter, top = UrbanSpace.Md, bottom = com.urbanblade.mobile.ui.components.BladebotClearance),
        verticalArrangement = Arrangement.spacedBy(UrbanSpace.Md)
    ) {
        item {
            HomeHeader("INGENIERO", user, loading || logsLoading, onNavigate) { systemVm.load(); logsVm.load(null, event = null) }
        }

        if (error != null && status != null) item { UrbanStaleContentNotice(error.orEmpty()) { systemVm.load() } }

        when {
            error == null && status == null -> item { UrbanSkeletonList(1) }
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
                    UrbanHeroSummary(
                        label = "Estado del sistema",
                        headline = health.headline,
                        large = false,
                        onClick = { onNavigate("system") },
                        stats = listOf(
                            UrbanHeroStatSpec(
                                "Base de datos",
                                status?.database?.latencyMs?.let { "$it ms" } ?: "—",
                                Icons.Default.Storage,
                                serviceTone(status?.database?.status)
                            ),
                            UrbanHeroStatSpec(
                                "Fallidos en cola",
                                status?.queue?.failed?.toString() ?: "—",
                                Icons.Default.Dns,
                                if ((status?.queue?.failed ?: 0) > 0) UrbanColors.Warning else UrbanColors.Gold
                            )
                        )
                    )
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

        item { UrbanSectionTitle("Actividad registrada", "Hoy y acumulado histórico de la bitácora.") }
        if (logsError != null) item {
            if (logsLoaded) UrbanStaleContentNotice(logsError.orEmpty()) { logsVm.load() }
            else UrbanMascotState(UrbanStateKind.ERROR, "No pudimos consultar la actividad", logsError, "Reintentar") { logsVm.load() }
        }
        if (!logsLoaded && logsError == null) item { UrbanSkeletonList(1) }
        if (logsLoaded) item {
            UrbanStatStrip(
                listOf(
                    Triple("Hoy", logs.stats.hoy.toString(), UrbanColors.Gold),
                    Triple("Creados en total", logs.stats.creates.toString(), UrbanColors.Success),
                    Triple("Eliminados en total", logs.stats.deletes.toString(), UrbanColors.Danger)
                ),
                onClick = { onNavigate("logs") }
            )
        }

        item { UrbanSectionTitle("Tus módulos", "Análisis del negocio y del servidor.") }
        item {
            UrbanModuleGrid(
                tiles = listOf(
                    Triple("Reportes", "Ingresos, citas e inventario", Icons.Default.Assessment),
                    Triple("Bitácora", "Quién hizo qué y cuándo", Icons.Default.Terminal),
                    Triple("Métricas", "KPIs del negocio", Icons.Default.MonitorHeart),
                    Triple("Insights IA", "Tendencias y alertas", Icons.Default.AutoAwesome),
                    Triple("Analítica", "Operación y clientes", Icons.Default.QueryStats),
                    Triple("Estado del sistema", "Servicios y tareas", Icons.Default.Dns),
                ),
                routes = listOf("reports", "logs", "admin_metrics", "insights", "analytics", "system"),
                onNavigate = onNavigate
            )
        }
    }
}

internal fun serviceTone(status: String?): Color = when {
    status.equals("up", ignoreCase = true) -> UrbanColors.Success
    status.equals(SERVICE_NOT_USED, ignoreCase = true) -> UrbanColors.Muted
    status.equals("down", ignoreCase = true) -> UrbanColors.Danger
    else -> UrbanColors.Warning
}
