package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.ReportsViewModel

private val TYPE_LABEL = mapOf(
    "ingresos" to "Ingresos",
    "citas" to "Citas",
    "inventario" to "Inventario",
    "clientes" to "Clientes",
)

@Composable
fun ReportsScreen(onBack: () -> Unit, vm: ReportsViewModel = viewModel()) {
    val manifest by vm.manifest.collectAsState()
    val report by vm.report.collectAsState()
    val busy by vm.busy.collectAsState()
    val error by vm.error.collectAsState()

    var type by remember { mutableStateOf<String?>(null) }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { vm.loadManifest() }
    LaunchedEffect(manifest) { if (type == null) type = manifest.types.firstOrNull() }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = { UrbanTopBar("Reportes", onBack) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    manifest.types.forEach { t ->
                        FilterChip(selected = type == t, onClick = { type = t }, label = { Text(TYPE_LABEL[t] ?: t) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(startDate, { startDate = it }, label = { Text("Desde (AAAA-MM-DD)") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(endDate, { endDate = it }, label = { Text("Hasta (AAAA-MM-DD)") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                UrbanPrimaryButton(
                    text = "Generar reporte",
                    onClick = { type?.let { vm.generate(it, startDate.takeIf { s -> s.isNotBlank() }, endDate.takeIf { s -> s.isNotBlank() }) } },
                    enabled = type != null,
                    loading = busy,
                    icon = Icons.Default.Assessment,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Solo vista en la app (JSON). Descargar en PDF o Excel todavía no está disponible en Android.",
                    style = MaterialTheme.typography.bodySmall,
                    color = UrbanColors.Muted
                )
            }

            error?.let {
                UrbanErrorBanner(it)
                Spacer(Modifier.height(8.dp))
            }

            report?.let { r ->
                Column(Modifier.padding(horizontal = 18.dp)) {
                    Text(r.title ?: "Reporte", style = MaterialTheme.typography.titleLarge, color = UrbanColors.Gold)
                    Spacer(Modifier.height(4.dp))
                    Text("${r.rows.size} filas", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                    Spacer(Modifier.height(12.dp))
                }
                if (r.rows.isEmpty()) {
                    UrbanEmptyState("Sin resultados", "No hay datos para este reporte en el rango elegido.", Icons.Default.Assessment)
                } else {
                    Box(Modifier.weight(1f).horizontalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
                        Column {
                            Row {
                                r.headings.forEach { heading ->
                                    Text(
                                        heading,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = UrbanColors.Gold,
                                        modifier = Modifier.width(140.dp).padding(vertical = 8.dp)
                                    )
                                }
                            }
                            HorizontalDivider(color = UrbanColors.Line)
                            r.rows.forEach { row ->
                                Row {
                                    r.keys.forEach { key ->
                                        val value = row[key]
                                        val text = when {
                                            value == null || value.isJsonNull -> "—"
                                            value.isJsonPrimitive -> value.asJsonPrimitive.toString().trim('"')
                                            else -> value.toString()
                                        }
                                        Text(
                                            text,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = UrbanColors.Ink,
                                            modifier = Modifier.width(140.dp).padding(vertical = 8.dp)
                                        )
                                    }
                                }
                                HorizontalDivider(color = UrbanColors.Line.copy(alpha = 0.4f))
                            }
                        }
                    }
                }
            }
        }
    }
}
