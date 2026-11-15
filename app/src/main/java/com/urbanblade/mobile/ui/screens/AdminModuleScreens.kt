package com.urbanblade.mobile.ui.screens

import com.urbanblade.mobile.ui.components.UrbanSwitch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.width
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urbanblade.mobile.data.model.BarberScheduleDay
import com.urbanblade.mobile.data.model.SystemServiceStatus
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.components.UrbanFormat
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.*
import com.urbanblade.mobile.ui.components.UrbanModuleScreen
import com.urbanblade.mobile.ui.theme.UrbanSpace
import com.urbanblade.mobile.ui.components.UrbanTonePill

// ── Métricas del negocio ─────────────────────────────────────────────────

private fun metricsMoney(value: Double) = UrbanFormat.moneyShort(value)

@Composable
fun AdminMetricsScreen(onBack: () -> Unit, vm: AdminMetricsViewModel = viewModel()) {
    val metrics by vm.metrics.collectAsState()
    val busy by vm.busy.collectAsState()
    val error by vm.error.collectAsState()
    LaunchedEffect(Unit) { vm.load() }

    UrbanModuleScreen(
        eyebrow = "ANÁLISIS",
        title = "Métricas",
        subtitle = "Lo acumulado: ingresos, clientes y cancelaciones.",
        onBack = onBack,
        onRefresh = { vm.load() },
        refreshing = busy
    ) {
        if (error != null) {
            item {
                UrbanMascotState(UrbanStateKind.ERROR, "No pudimos cargar las métricas", error, "Reintentar") { vm.load() }
            }
            return@UrbanModuleScreen
        }
        if (busy && metrics.totalRevenue == 0.0 && metrics.totalClients == 0) {
            item { UrbanSkeletonList(2) }
            return@UrbanModuleScreen
        }

        item {
            UrbanHeroCard {
                UrbanHeroLabel("Ingreso total")
                Spacer(Modifier.height(6.dp))
                Text(metricsMoney(metrics.totalRevenue), style = MaterialTheme.typography.displaySmall, color = UrbanColors.Ink)
                Text(
                    "Promedio por cita: ${metricsMoney(metrics.averageRevenuePerAppointment)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = UrbanColors.Muted
                )
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = UrbanColors.Ink.copy(alpha = 0.22f))
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    UrbanHeroStat("Clientes", metrics.totalClients.toString(), Icons.Default.Groups, Modifier.weight(1f))
                    UrbanHeroStat("Barberos activos", metrics.activeBarbers.toString(), Icons.Default.ContentCut, Modifier.weight(1f))
                }
            }
        }

        item { UrbanSectionTitle("Cancelaciones", "Cada cita cancelada es un hueco sin ingreso.") }
        item {
            val rate = metrics.cancellationRate
            // Hasta 10 % de cancelaciones es normal; más de 20 % ya pide intervenir (recordatorios, depósitos).
            val tone = when {
                rate < 10.0 -> UrbanColors.Success
                rate < 20.0 -> UrbanColors.Warning
                else -> UrbanColors.Danger
            }
            UrbanCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    UrbanRingGauge(fraction = (rate / 100.0).toFloat(), centerText = "%.1f%%".format(rate), color = tone)
                    Column(Modifier.weight(1f)) {
                        Text(
                            when {
                                rate < 10.0 -> "Saludable"
                                rate < 20.0 -> "Vigílala"
                                else -> "Alta"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = tone
                        )
                        Text(
                            when {
                                rate < 10.0 -> "Pocas citas se pierden."
                                rate < 20.0 -> "Recordatorios y depósitos ayudan a bajarla."
                                else -> "Revisa recordatorios y la política de depósitos."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = UrbanColors.Muted
                        )
                    }
                }
            }
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

    UrbanModuleScreen(
        eyebrow = "SISTEMA",
        title = "Estado del sistema",
        subtitle = "Servicios, cola de trabajos y tareas programadas.",
        onBack = onBack,
        onRefresh = { vm.load() },
        refreshing = busy
    ) {
        val s = status
        when {
            s == null && error != null -> item {
                UrbanMascotState(UrbanStateKind.ERROR, "No pudimos revisar el servidor", error, "Reintentar") { vm.load() }
            }
            s == null -> item { UrbanSkeletonList(2) }
            else -> {
                val health = systemHealth(s)
                item {
                    UrbanHeroCard {
                        UrbanHeroLabel(s.app.name ?: "UrbanBlade")
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(healthIcon(health.level), null, tint = healthTone(health.level), modifier = Modifier.size(26.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(health.headline, style = MaterialTheme.typography.headlineSmall, color = UrbanColors.Ink)
                        }
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = UrbanColors.Ink.copy(alpha = 0.22f))
                        Spacer(Modifier.height(14.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            UrbanHeroStat("Entorno", s.app.env ?: "—", Icons.Default.Cloud, Modifier.weight(1f))
                            UrbanHeroStat("Laravel", s.app.laravelVersion ?: "—", Icons.Default.Code, Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("PHP ${s.app.phpVersion ?: "—"}", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                    }
                }

                if (health.issues.isNotEmpty()) {
                    item { UrbanSectionTitle("Requiere tu atención", "Ordenado de más a menos grave.") }
                    health.issues.forEach { issue ->
                        item { UrbanAttentionRow(healthIcon(issue.level), issue.title, issue.detail, healthTone(issue.level)) }
                    }
                }

                item { UrbanSectionTitle("Servicios", "Conexión y tiempo de respuesta.") }
                item { ServiceRow("Base de datos (MongoDB)", Icons.Default.Storage, s.database) }
                item { ServiceRow("Redis", Icons.Default.Memory, s.redis) }
                item {
                    val failed = s.queue.failed ?: 0
                    UrbanAttentionRow(
                        Icons.Default.Dns,
                        "Cola de trabajos",
                        "${s.queue.connection ?: "—"} · ${s.queue.pending ?: 0} pendientes · $failed fallidos",
                        if (failed > 0) UrbanColors.Warning else UrbanColors.Success
                    )
                }

                if (s.scheduledTasks.isNotEmpty()) {
                    item { UrbanSectionTitle("Tareas programadas", UrbanFormat.count(s.scheduledTasks.size, "tarea", "tareas")) }
                    items(s.scheduledTasks) { task ->
                        val failed = task.status.equals("failed", ignoreCase = true)
                        val ok = task.status.equals("success", ignoreCase = true)
                        UrbanAttentionRow(
                            icon = if (failed) Icons.Default.Error else Icons.Default.Schedule,
                            text = task.name ?: "—",
                            subtitle = listOfNotNull(
                                task.expression,
                                task.ranAt?.let { "Última: $it" },
                                task.error
                            ).joinToString(" · ").ifBlank { null },
                            tone = when {
                                failed -> UrbanColors.Danger
                                ok -> UrbanColors.Success
                                else -> UrbanColors.Muted
                            },
                            trailing = { SimpleStatusPill(if (failed) "error" else if (ok) "ok" else "sin datos") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceRow(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, service: SystemServiceStatus) {
    UrbanAttentionRow(
        icon = icon,
        text = title,
        subtitle = when {
            service.status.equals(SERVICE_NOT_USED, ignoreCase = true) -> "Este entorno no lo necesita (caché y cola sin Redis)."
            else -> service.error ?: service.latencyMs?.let { "Responde en $it ms" } ?: "Sin medición de latencia"
        },
        tone = serviceTone(service.status),
        trailing = {
            SimpleStatusPill(if (service.status.equals(SERVICE_NOT_USED, ignoreCase = true)) "no se usa" else service.status ?: "desconocido")
        }
    )
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
    // Misma pastilla que UrbanStatusPill y UrbanTrendPill: una sola implementación del tinte, el borde y el texto.
    UrbanTonePill(label = status.replaceFirstChar { it.uppercase() }, icon = null, tone = resolved)
}

// ── Sorteos ──────────────────────────────────────────────────────────────

@Composable
fun RafflesScreen(onBack: () -> Unit, vm: RafflesViewModel = viewModel()) {
    val raffles by vm.raffles.collectAsState()
    val loadedOnce by vm.hasLoaded.collectAsState()
    val busy by vm.busy.collectAsState()
    val error by vm.error.collectAsState()
    LaunchedEffect(Unit) { vm.load() }

    var filter by remember { mutableStateOf("todos") }
    val shown = remember(raffles, filter) {
        raffles.data.filter {
            when (filter) {
                "vigentes" -> !it.isClaimed && !it.isExpired
                "reclamados" -> it.isClaimed
                "caducados" -> it.isExpired && !it.isClaimed
                else -> true
            }
        }
    }

    UrbanModuleScreen(
        eyebrow = "ADMINISTRACIÓN",
        title = "Sorteos",
        subtitle = "Ganadores del sorteo mensual y si ya reclamaron su premio.",
        onBack = onBack,
        onRefresh = { vm.load() },
        refreshing = busy
    ) {
        urbanListFeedback(
            loadedOnce, busy, shown.isNotEmpty(), error,
            "No pudimos cargar los sorteos",
            if (raffles.data.isEmpty()) "Sin sorteos" else "Sin resultados",
            if (raffles.data.isEmpty()) "Todavía no hay resultados de sorteos." else "No hay sorteos con este estado."
        ) { vm.load() }

        if (loadedOnce) item {
            // Tres números en una sola tarjeta: en tres tarjetas angostas las etiquetas se partían.
            UrbanStatStrip(
                listOf(
                    Triple("Total", raffles.stats.total.toString(), UrbanColors.Ink),
                    Triple("Reclamados", raffles.stats.reclamados.toString(), UrbanColors.Success),
                    Triple("Vigentes", raffles.stats.vigentes.toString(), UrbanColors.Ink)
                )
            )
        }

        if (raffles.data.isNotEmpty()) {
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("todos" to "Todos", "vigentes" to "Vigentes", "reclamados" to "Reclamados", "caducados" to "Caducados").forEach { (key, label) ->
                        FilterChip(selected = filter == key, onClick = { filter = key }, label = { Text(label) }, colors = urbanFilterChipColors())
                    }
                }
            }
        }


        items(shown) { raffle ->
            UrbanCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(raffle.client?.user?.name ?: "Cliente sin nombre", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(raffle.premio ?: "—", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.width(8.dp))
                    SimpleStatusPill(
                        when {
                            raffle.isClaimed -> "reclamado"
                            raffle.isExpired -> "caducado"
                            else -> "vigente"
                        }
                    )
                }
                Spacer(Modifier.height(8.dp))
                UrbanKeyValue("Mes", raffle.mes ?: "—")
                UrbanKeyValue("Nivel ganador", raffle.nivelGanador ?: "—", Modifier.padding(top = 6.dp))
                UrbanKeyValue("Vence", raffle.venceEn?.let { UrbanFormat.date(it) } ?: "—", Modifier.padding(top = 6.dp))
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
            loaded.find { it.dayOfWeek == key }?.let { it.copy(startTime = normalizeTime(it.startTime), endTime = normalizeTime(it.endTime)) }
                ?: BarberScheduleDay(key, "09:00", "18:00", isActive = false)
        }
    }

    val activeDays = days.filter { it.isActive }
    val weeklyMinutes = activeDays.sumOf { minutesBetween(it.startTime, it.endTime) ?: 0 }

    UrbanModuleScreen(
        eyebrow = "BARBERO",
        title = "Mi horario",
        subtitle = "Los días y horas en que aceptas citas.",
        onBack = onBack,
        onRefresh = { vm.load() },
        refreshing = busy
    ) {
        if (error != null && loaded.isEmpty()) {
            item { UrbanMascotState(UrbanStateKind.ERROR, "No pudimos cargar tu horario", error, "Reintentar") { vm.load() } }
            return@UrbanModuleScreen
        }
        error?.let { item { UrbanErrorBanner(it) } }
        message?.let { item { UrbanInfoBanner(it, Icons.Default.CheckCircle) } }

        item {
            // Tres números en una franja en lugar de una tarjeta alta con foto.
            UrbanStatStrip(
                listOf(
                    Triple("Días activos", activeDays.size.toString(), UrbanColors.Ink),
                    Triple("Horas a la semana", String.format("%.0f", weeklyMinutes / 60.0), UrbanColors.Ink),
                    Triple("Días libres", (7 - activeDays.size).toString(), UrbanColors.Ink)
                )
            )
        }
        if (activeDays.isEmpty()) item {
            Text("Activa al menos un día para recibir reservas.", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
        }

        item {
            // Los siete días en una sola tarjeta: interruptor, y las horas se eligen con el reloj, no se escriben.
            UrbanCard(Modifier.fillMaxWidth()) {
                days.forEachIndexed { index, day ->
                    val label = WEEK_DAYS.first { it.first == day.dayOfWeek }.second
                    if (index > 0) HorizontalDivider(color = UrbanColors.Line, modifier = Modifier.padding(vertical = 8.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.width(92.dp))
                        if (day.isActive) {
                            ScheduleTimeChip(day.startTime, !isValidTime(day.startTime)) { v ->
                                days = days.toMutableList().apply { this[index] = day.copy(startTime = v) }
                            }
                            Text("a", color = UrbanColors.Muted)
                            ScheduleTimeChip(day.endTime, !isValidTime(day.endTime) || minutesBetween(day.startTime, day.endTime) == null) { v ->
                                days = days.toMutableList().apply { this[index] = day.copy(endTime = v) }
                            }
                        } else {
                            Text("Descanso", style = MaterialTheme.typography.bodyMedium, color = UrbanColors.Muted)
                        }
                        Spacer(Modifier.weight(1f))
                        UrbanSwitch(
                            checked = day.isActive,
                            onCheckedChange = { checked -> days = days.toMutableList().apply { this[index] = day.copy(isActive = checked) } }
                        )
                    }
                }
            }
        }

        item {
            UrbanOutlineButton(
                text = "Copiar las horas del lunes a todos los días",
                onClick = {
                    val monday = days.firstOrNull()
                    if (monday != null) days = days.map { it.copy(startTime = monday.startTime, endTime = monday.endTime) }
                },
                icon = Icons.Default.ContentCopy,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            val valid = remember(days) {
                days.all { !it.isActive || (isValidTime(it.startTime) && isValidTime(it.endTime) && minutesBetween(it.startTime, it.endTime) != null) }
            }
            if (!valid) {
                Text(
                    "Usa el formato HH:mm (ej. 09:00) y una salida posterior a la entrada en los días activos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = UrbanColors.Danger,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            UrbanPrimaryButton(
                text = "Guardar horario",
                onClick = { vm.save(days) },
                enabled = valid,
                loading = saving,
                icon = Icons.Default.Save,
                modifier = Modifier.fillMaxWidth()
            )
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

    UrbanModuleScreen(
        eyebrow = "OPERACIÓN",
        title = "Corte de caja",
        subtitle = "${preview.fecha?.let { UrbanFormat.date(it) } ?: "Hoy"} · Revisa y concilia la caja.",
        onBack = onBack,
        onRefresh = { vm.load() },
        refreshing = busy
    ) {
        if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = UrbanColors.Gold) }
        error?.let { item { UrbanErrorBanner(it) } }
        message?.let { item { UrbanInfoBanner(it, Icons.Default.CheckCircle) } }

        item {
            UrbanPremiumCard(Modifier.fillMaxWidth()) {
                Text("TOTAL REGISTRADO", style = MaterialTheme.typography.labelLarge, color = UrbanColors.GoldText)
                Text(
                    UrbanFormat.money(preview.esperadoTotal),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = UrbanColors.Ink
                )
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = UrbanColors.Line)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Efectivo esperado", style = MaterialTheme.typography.labelMedium, color = UrbanColors.Muted)
                        Text(UrbanFormat.money(preview.efectivoEsperado), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = UrbanColors.Ink)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Propinas", style = MaterialTheme.typography.labelMedium, color = UrbanColors.Muted)
                        Text(UrbanFormat.money(preview.propinas), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = UrbanColors.Ink)
                    }
                }
            }
        }

        if (preview.esperado.values.any { it > 0.0 }) {
            item {
                val byMethod = preview.esperado.entries.filter { it.value > 0.0 }.sortedByDescending { it.value }
                UrbanCard(Modifier.fillMaxWidth()) {
                    Text("Cobros registrados", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    byMethod.forEachIndexed { index, (method, amount) ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(METODO_LABEL[method] ?: method.replaceFirstChar { c -> c.uppercase() }, color = UrbanColors.Muted)
                            Text(UrbanFormat.money(amount), fontWeight = FontWeight.SemiBold)
                        }
                        if (index < byMethod.lastIndex) HorizontalDivider(Modifier.padding(vertical = 8.dp), color = UrbanColors.Line)
                    }
                    Text(
                        "${preview.pagos} pagos · ${preview.pedidos} pedidos · ${preview.paquetes} paquetes",
                        style = MaterialTheme.typography.bodySmall,
                        color = UrbanColors.Muted,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            }
        }

        val cierre = preview.cierre
        if (cierre != null) {
            item {
                UrbanCard(Modifier.fillMaxWidth()) {
                    Text("Caja ya cerrada hoy", style = MaterialTheme.typography.titleMedium, color = UrbanColors.GoldText)
                    Spacer(Modifier.height(8.dp))
                    UrbanKeyValue("Efectivo contado", "${UrbanFormat.money(cierre.efectivoContado)}")
                    val diff = cierre.diferencia
                    val (diffText, diffTone) = when {
                        kotlin.math.abs(diff) < 0.005 -> "Cuadró" to UrbanColors.Success
                        diff > 0 -> "Sobran " + UrbanFormat.money(diff) to UrbanColors.Warning
                        else -> "Faltan " + UrbanFormat.money(-diff) to UrbanColors.Danger
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Diferencia", style = MaterialTheme.typography.bodyMedium, color = UrbanColors.Muted)
                        Text(diffText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = diffTone)
                    }
                    cierre.cerradoPorNombre?.let { UrbanKeyValue("Cerrado por", it, Modifier.padding(top = 6.dp)) }
                    cierre.notas?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                    }
                }
            }
        } else {
            item { UrbanSectionTitle("1. Cuenta el efectivo", "Compara el monto físico contra lo registrado.") }
            item {
                UrbanCard(Modifier.fillMaxWidth()) {
                    UrbanFieldLabel("Efectivo contado")
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
                    Text(
                        "Esperado en efectivo: " + UrbanFormat.money(preview.efectivoEsperado),
                        style = MaterialTheme.typography.bodySmall,
                        color = UrbanColors.Muted,
                        modifier = Modifier.padding(top = 6.dp)
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
                    text = "Revisar y cerrar caja",
                    onClick = { showConfirm = true },
                    enabled = contado != null && contado >= 0,
                    loading = closing,
                    icon = Icons.Default.Lock,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showConfirm) {
        val contado = efectivoContado.toDoubleOrNull() ?: 0.0
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("¿Cerrar la caja de hoy?") },
            text = { Text("Se registrará ${UrbanFormat.money(contado)} como efectivo contado. Esta acción no se puede deshacer.") },
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

/** Hora de entrada o salida: un botón con la hora que abre el reloj, en lugar de escribirla a mano. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleTimeChip(value: String, invalid: Boolean, onChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val parts = value.split(":")
    OutlinedButton(
        onClick = { open = true },
        border = androidx.compose.foundation.BorderStroke(1.dp, if (invalid) UrbanColors.Danger else UrbanColors.Line),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    ) { Text(value, color = if (invalid) UrbanColors.Danger else UrbanColors.Ink) }
    if (open) {
        val state = androidx.compose.material3.rememberTimePickerState(
            initialHour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 9,
            initialMinute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0,
            is24Hour = true
        )
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { open = false },
            containerColor = UrbanColors.Card,
            text = { androidx.compose.material3.TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = { onChange(String.format("%02d:%02d", state.hour, state.minute)); open = false }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancelar") } }
        )
    }
}
