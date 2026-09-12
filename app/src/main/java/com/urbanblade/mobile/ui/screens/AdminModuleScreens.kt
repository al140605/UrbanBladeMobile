package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urbanblade.mobile.data.model.BarberScheduleDay
import com.urbanblade.mobile.data.model.SystemServiceStatus
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.*

// ── Métricas de admin ────────────────────────────────────────────────────

@Composable
fun AdminMetricsScreen(onBack: () -> Unit, vm: AdminMetricsViewModel = viewModel()) {
    val metrics by vm.metrics.collectAsState()
    val busy by vm.busy.collectAsState()
    val error by vm.error.collectAsState()
    LaunchedEffect(Unit) { vm.load() }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = { UrbanTopBar("Métricas del negocio", onBack) { IconButton(onClick = { vm.load() }) { Icon(Icons.Default.Refresh, "Actualizar") } } }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = UrbanColors.Gold) }
            error?.let { item { UrbanErrorBanner(it) } }

            item { UrbanMetricCard("Clientes totales", metrics.totalClients.toString(), Icons.Default.Groups, Modifier.fillMaxWidth()) }
            item { UrbanMetricCard("Barberos activos", metrics.activeBarbers.toString(), Icons.Default.ContentCut, Modifier.fillMaxWidth()) }
            item { UrbanMetricCard("Tasa de cancelación", "${"%.1f".format(metrics.cancellationRate)}%", Icons.Default.EventBusy, Modifier.fillMaxWidth()) }
            item { UrbanMetricCard("Ingreso promedio por cita", "\$${"%.2f".format(metrics.averageRevenuePerAppointment)}", Icons.Default.Payments, Modifier.fillMaxWidth()) }
            item { UrbanMetricCard("Ingreso total", "\$${"%.2f".format(metrics.totalRevenue)}", Icons.Default.AccountBalanceWallet, Modifier.fillMaxWidth()) }
        }
    }
}

// ── Estado del sistema ───────────────────────────────────────────────────

@Composable
fun SystemStatusScreen(onBack: () -> Unit, vm: SystemStatusViewModel = viewModel()) {
    val status by vm.status.collectAsState()
    val busy by vm.busy.collectAsState()
    val error by vm.error.collectAsState()
    LaunchedEffect(Unit) { vm.load() }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = { UrbanTopBar("Estado del sistema", onBack) { IconButton(onClick = { vm.load() }) { Icon(Icons.Default.Refresh, "Actualizar") } } }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = UrbanColors.Gold) }
            error?.let { item { UrbanErrorBanner(it) } }

            status?.let { s ->
                item {
                    UrbanCard(Modifier.fillMaxWidth()) {
                        Text(s.app.name ?: "UrbanBlade", style = MaterialTheme.typography.titleMedium, color = UrbanColors.Gold)
                        Spacer(Modifier.height(8.dp))
                        UrbanKeyValue("Entorno", s.app.env ?: "—")
                        UrbanKeyValue("Laravel", s.app.laravelVersion ?: "—", Modifier.padding(top = 6.dp))
                        UrbanKeyValue("PHP", s.app.phpVersion ?: "—", Modifier.padding(top = 6.dp))
                    }
                }
                item { ServiceStatusCard("Base de datos (MongoDB)", s.database) }
                item { ServiceStatusCard("Redis", s.redis) }
                item {
                    UrbanCard(Modifier.fillMaxWidth()) {
                        Text("Cola de trabajos", style = MaterialTheme.typography.titleMedium, color = UrbanColors.Gold)
                        Spacer(Modifier.height(8.dp))
                        UrbanKeyValue("Conexión", s.queue.connection ?: "—")
                        UrbanKeyValue("Pendientes", s.queue.pending?.toString() ?: "—", Modifier.padding(top = 6.dp))
                        UrbanKeyValue("Fallidos", s.queue.failed?.toString() ?: "—", Modifier.padding(top = 6.dp))
                    }
                }
                if (s.scheduledTasks.isNotEmpty()) {
                    item { UrbanSectionTitle("Tareas programadas", "${s.scheduledTasks.size} tareas") }
                    items(s.scheduledTasks) { task ->
                        UrbanCard(Modifier.fillMaxWidth()) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(task.name ?: "—", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    task.expression?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted) }
                                }
                                Spacer(Modifier.width(8.dp))
                                task.status?.let { SimpleStatusPill(it) }
                            }
                            task.ranAt?.let {
                                Spacer(Modifier.height(6.dp))
                                UrbanKeyValue("Última corrida", it)
                            }
                            task.error?.let {
                                Spacer(Modifier.height(6.dp))
                                Text(it, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Danger)
                            }
                        }
                    }
                }
            }
            if (status == null && !busy && error == null) {
                item { UrbanEmptyState("Sin información", "No se pudo obtener el estado del sistema.", Icons.Default.MonitorHeart) }
            }
        }
    }
}

@Composable
private fun ServiceStatusCard(title: String, service: SystemServiceStatus) {
    UrbanCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = UrbanColors.Gold)
            SimpleStatusPill(service.status ?: "desconocido")
        }
        Spacer(Modifier.height(8.dp))
        UrbanKeyValue("Latencia", service.latencyMs?.let { "${it} ms" } ?: "—")
        service.error?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Danger)
        }
    }
}

/**
 * UrbanStatusPill mapea colores por palabras clave en español (complet-,
 * cancel-, pend-...) que no cubren "up"/"down" ni los estados en español de
 * sorteos (vigente/reclamado/vencido) -- pill local con mapeo explícito en
 * vez de forzar esas palabras a coincidir con el heurístico existente.
 */
@Composable
internal fun SimpleStatusPill(status: String, color: androidx.compose.ui.graphics.Color? = null) {
    val resolved = color ?: when (status.lowercase()) {
        "up", "ok", "activo", "vigente" -> UrbanColors.Success
        "down", "error", "vencido" -> UrbanColors.Danger
        "reclamado" -> UrbanColors.Info
        else -> UrbanColors.Muted
    }
    Surface(
        shape = CircleShape,
        color = resolved.copy(alpha = 0.13f),
        border = BorderStroke(1.dp, resolved.copy(alpha = 0.35f))
    ) {
        Text(
            status.replaceFirstChar { it.uppercase() },
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            color = resolved,
            maxLines = 1
        )
    }
}

// ── Sorteos ──────────────────────────────────────────────────────────────

@Composable
fun RafflesScreen(onBack: () -> Unit, vm: RafflesViewModel = viewModel()) {
    val raffles by vm.raffles.collectAsState()
    val busy by vm.busy.collectAsState()
    val error by vm.error.collectAsState()
    LaunchedEffect(Unit) { vm.load() }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = { UrbanTopBar("Sorteos", onBack) { IconButton(onClick = { vm.load() }) { Icon(Icons.Default.Refresh, "Actualizar") } } }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = UrbanColors.Gold) }
            error?.let { item { UrbanErrorBanner(it) } }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    UrbanMetricCard("Total", raffles.stats.total.toString(), Icons.Default.CardGiftcard, Modifier.weight(1f))
                    UrbanMetricCard("Reclamados", raffles.stats.reclamados.toString(), Icons.Default.CheckCircle, Modifier.weight(1f))
                    UrbanMetricCard("Vigentes", raffles.stats.vigentes.toString(), Icons.Default.HourglassTop, Modifier.weight(1f))
                }
            }

            if (raffles.data.isEmpty() && !busy) {
                item { UrbanEmptyState("Sin sorteos", "Todavía no hay resultados de sorteos.", Icons.Default.CardGiftcard) }
            }

            items(raffles.data) { raffle ->
                UrbanCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text(raffle.premio ?: "—", style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(raffle.client?.user?.name ?: "Cliente sin nombre", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                        }
                        Spacer(Modifier.width(8.dp))
                        SimpleStatusPill(
                            when {
                                raffle.isClaimed -> "reclamado"
                                raffle.isExpired -> "vencido"
                                else -> "vigente"
                            }
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    UrbanKeyValue("Mes", raffle.mes ?: "—")
                    UrbanKeyValue("Nivel ganador", raffle.nivelGanador ?: "—", Modifier.padding(top = 6.dp))
                    UrbanKeyValue("Vence", raffle.venceEn ?: "—", Modifier.padding(top = 6.dp))
                }
            }
        }
    }
}

// ── Horario de barbero ──────────────────────────────────────────────────

private val WEEK_DAYS = listOf(
    "monday" to "Lunes",
    "tuesday" to "Martes",
    "wednesday" to "Miércoles",
    "thursday" to "Jueves",
    "friday" to "Viernes",
    "saturday" to "Sábado",
    "sunday" to "Domingo",
)

@Composable
fun BarberScheduleScreen(onBack: () -> Unit, vm: BarberScheduleViewModel = viewModel()) {
    val loaded by vm.schedules.collectAsState()
    val busy by vm.busy.collectAsState()
    val saving by vm.saving.collectAsState()
    val message by vm.message.collectAsState()
    val error by vm.error.collectAsState()
    LaunchedEffect(Unit) { vm.load() }

    // Estado editable local: siempre 7 días -- si el servidor todavía no
    // tiene un horario guardado para alguno, se rellena con un default
    // razonable (09:00-18:00, inactivo) para no dejar días sin fila.
    var days by remember { mutableStateOf<List<BarberScheduleDay>>(emptyList()) }
    LaunchedEffect(loaded) {
        days = WEEK_DAYS.map { (key, _) ->
            loaded.find { it.dayOfWeek == key } ?: BarberScheduleDay(key, "09:00", "18:00", isActive = false)
        }
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = { UrbanTopBar("Mi horario", onBack) }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = UrbanColors.Gold) }
            error?.let { item { UrbanErrorBanner(it) } }
            message?.let { item { UrbanInfoBanner(it, Icons.Default.CheckCircle) } }

            itemsIndexed(days) { index, day ->
                val label = WEEK_DAYS.first { it.first == day.dayOfWeek }.second
                UrbanCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(label, style = MaterialTheme.typography.titleMedium)
                        Switch(
                            checked = day.isActive,
                            onCheckedChange = { checked -> days = days.toMutableList().apply { this[index] = day.copy(isActive = checked) } },
                            colors = SwitchDefaults.colors(checkedTrackColor = UrbanColors.Gold)
                        )
                    }
                    if (day.isActive) {
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = day.startTime,
                                onValueChange = { v -> days = days.toMutableList().apply { this[index] = day.copy(startTime = v) } },
                                label = { Text("Inicio") },
                                placeholder = { Text("HH:mm") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = day.endTime,
                                onValueChange = { v -> days = days.toMutableList().apply { this[index] = day.copy(endTime = v) } },
                                label = { Text("Fin") },
                                placeholder = { Text("HH:mm") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item {
                val validFormat = remember(days) { days.all { !it.isActive || (isValidTime(it.startTime) && isValidTime(it.endTime)) } }
                if (!validFormat) {
                    Text(
                        "Usa el formato HH:mm (ej. 09:00) en los días activos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = UrbanColors.Danger,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                UrbanPrimaryButton(
                    text = "Guardar horario",
                    onClick = { vm.save(days) },
                    enabled = validFormat,
                    loading = saving,
                    icon = Icons.Default.Save,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun isValidTime(value: String): Boolean = Regex("^([01]\\d|2[0-3]):[0-5]\\d$").matches(value)

// ── Corte de caja ────────────────────────────────────────────────────────

private val METODO_LABEL = mapOf(
    "efectivo" to "Efectivo",
    "tarjeta" to "Tarjeta",
    "transferencia" to "Transferencia",
    "otro" to "Otro",
    "desconocido" to "Sin método",
)

@Composable
fun CashCloseScreen(onBack: () -> Unit, vm: CashCloseViewModel = viewModel()) {
    val preview by vm.preview.collectAsState()
    val busy by vm.busy.collectAsState()
    val closing by vm.closing.collectAsState()
    val message by vm.message.collectAsState()
    val error by vm.error.collectAsState()
    LaunchedEffect(Unit) { vm.load() }

    var efectivoContado by remember { mutableStateOf("") }
    var notas by remember { mutableStateOf("") }
    var showConfirm by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = { UrbanTopBar("Corte de caja", onBack) { IconButton(onClick = { vm.load() }) { Icon(Icons.Default.Refresh, "Actualizar") } } }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = UrbanColors.Gold) }
            error?.let { item { UrbanErrorBanner(it) } }
            message?.let { item { UrbanInfoBanner(it, Icons.Default.CheckCircle) } }

            item {
                UrbanPageHeader(
                    title = preview.fecha ?: "Hoy",
                    subtitle = "Ingresos esperados según lo registrado en el sistema.",
                    eyebrow = "Corte de caja"
                )
            }

            if (preview.esperado.isNotEmpty()) {
                item { UrbanSectionTitle("Esperado por método") }
                items(preview.esperado.entries.toList()) { (metodo, monto) ->
                    UrbanCard(Modifier.fillMaxWidth()) {
                        UrbanKeyValue(METODO_LABEL[metodo] ?: metodo.replaceFirstChar { it.uppercase() }, "\$${"%.2f".format(monto)}")
                    }
                }
            }

            item {
                UrbanPremiumCard(Modifier.fillMaxWidth()) {
                    UrbanKeyValue("Total esperado", "\$${"%.2f".format(preview.esperadoTotal)}")
                    Spacer(Modifier.height(8.dp))
                    UrbanKeyValue("Efectivo esperado en caja", "\$${"%.2f".format(preview.efectivoEsperado)}")
                    Spacer(Modifier.height(8.dp))
                    UrbanKeyValue("Propinas", "\$${"%.2f".format(preview.propinas)}")
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    UrbanMetricCard("Pagos", preview.pagos.toString(), Icons.Default.Receipt, Modifier.weight(1f))
                    UrbanMetricCard("Pedidos", preview.pedidos.toString(), Icons.Default.ShoppingBag, Modifier.weight(1f))
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    UrbanMetricCard("Paquetes", preview.paquetes.toString(), Icons.Default.Inventory2, Modifier.weight(1f))
                    UrbanMetricCard("Gift cards", preview.giftCards.toString(), Icons.Default.CardGiftcard, Modifier.weight(1f))
                    UrbanMetricCard("Membresías", preview.membresias.toString(), Icons.Default.Star, Modifier.weight(1f))
                }
            }

            val cierre = preview.cierre
            if (cierre != null) {
                item {
                    UrbanCard(Modifier.fillMaxWidth()) {
                        Text("Caja ya cerrada hoy", style = MaterialTheme.typography.titleMedium, color = UrbanColors.Gold)
                        Spacer(Modifier.height(8.dp))
                        UrbanKeyValue("Efectivo contado", "\$${"%.2f".format(cierre.efectivoContado)}")
                        UrbanKeyValue("Diferencia", "\$${"%.2f".format(cierre.diferencia)}", Modifier.padding(top = 6.dp))
                        cierre.cerradoPorNombre?.let { UrbanKeyValue("Cerrado por", it, Modifier.padding(top = 6.dp)) }
                        cierre.notas?.takeIf { it.isNotBlank() }?.let {
                            Spacer(Modifier.height(6.dp))
                            Text(it, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                        }
                    }
                }
            } else {
                item { UrbanSectionTitle("Registrar corte") }
                item {
                    UrbanCard(Modifier.fillMaxWidth()) {
                        UrbanFieldLabel("Efectivo contado físicamente")
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = efectivoContado,
                            onValueChange = { efectivoContado = it },
                            placeholder = { Text("0.00") },
                            leadingIcon = { Icon(Icons.Default.Payments, null) },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                        UrbanFieldLabel("Notas (opcional)")
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = notas,
                            onValueChange = { notas = it },
                            placeholder = { Text("Ej. faltante por cambio de un billete") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                item {
                    val contado = efectivoContado.toDoubleOrNull()
                    UrbanPrimaryButton(
                        text = "Cerrar caja",
                        onClick = { showConfirm = true },
                        enabled = contado != null && contado >= 0,
                        loading = closing,
                        icon = Icons.Default.Lock,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    if (showConfirm) {
        val contado = efectivoContado.toDoubleOrNull() ?: 0.0
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("¿Cerrar la caja de hoy?") },
            text = { Text("Se registrará \$${"%.2f".format(contado)} como efectivo contado. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    vm.close(contado, notas.takeIf { it.isNotBlank() })
                }) { Text("Sí, cerrar") }
            },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("Cancelar") } }
        )
    }
}
