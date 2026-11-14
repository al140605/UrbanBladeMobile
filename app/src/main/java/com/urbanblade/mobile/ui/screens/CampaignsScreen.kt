package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SmartButton
import com.urbanblade.mobile.ui.components.UrbanFormChoice
import com.urbanblade.mobile.ui.components.UrbanFormSection
import com.urbanblade.mobile.ui.components.UrbanFormSheet
import com.urbanblade.mobile.ui.components.formBlockerText
import com.urbanblade.mobile.ui.components.rememberUrbanFormState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urbanblade.mobile.data.model.CampaignRow
import com.urbanblade.mobile.data.model.CreateCampaignRequest
import com.urbanblade.mobile.ui.components.UrbanCard
import com.urbanblade.mobile.ui.components.UrbanEmptyState
import com.urbanblade.mobile.ui.components.UrbanErrorBanner
import com.urbanblade.mobile.ui.components.UrbanFieldLabel
import com.urbanblade.mobile.ui.components.UrbanFormat
import com.urbanblade.mobile.ui.components.UrbanInfoBanner
import com.urbanblade.mobile.ui.components.UrbanOutlineButton
import com.urbanblade.mobile.ui.components.UrbanPrimaryButton
import com.urbanblade.mobile.ui.components.UrbanSectionTitle
import com.urbanblade.mobile.ui.components.UrbanStatusPill
import com.urbanblade.mobile.ui.components.UrbanTextField
import com.urbanblade.mobile.ui.components.UrbanStatStrip
import com.urbanblade.mobile.ui.components.urbanFilterChipColors
import com.urbanblade.mobile.ui.components.statusStyle
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.CampaignsViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import com.urbanblade.mobile.ui.components.UrbanModuleScreen
import com.urbanblade.mobile.ui.components.UrbanFab
import com.urbanblade.mobile.ui.components.isConfirmedEmpty
import com.urbanblade.mobile.ui.components.urbanListFeedback
import com.urbanblade.mobile.ui.components.UrbanStatCard

/** Campañas de marketing por segmento (solo administrador): historial y envío inmediato o programado. */
@Composable
fun CampaignsScreen(onBack: () -> Unit, vm: CampaignsViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    var creating by remember { mutableStateOf(false) }
    val data = state.data
    val labels = remember(data) { (data.segmentCounts.keys + "todos" + "inactive").associateWith { segmentLabel(it, data.levels) } }

    LaunchedEffect(Unit) { vm.load() }

    UrbanModuleScreen(
        eyebrow = "ADMINISTRACIÓN",
        title = "Campañas",
        subtitle = "Promociones por correo y notificación para cada segmento de clientes.",
        onBack = onBack,
        onRefresh = { vm.load() },
        refreshing = state.loading || state.sending,
        floatingActionButton = { expanded ->
            UrbanFab("Nueva campaña", Icons.Default.Add, expanded) { vm.clearMessages(); creating = true }
        },
        // Con el vacío confirmado la acción va en el propio estado vacío: el flotante taparía su texto.
        hideFloatingActionButton = isConfirmedEmpty(state.loaded, state.loading, data.data.isNotEmpty(), state.listError)
    ) {
        if (state.sending) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = UrbanColors.Gold) }
        state.notice?.let { item { UrbanInfoBanner(it, Icons.Default.CheckCircle) } }
        // Con la hoja de nueva campaña abierta el error de enviar se muestra dentro de ella, no aquí.
        if (!creating) state.error?.let { item { UrbanErrorBanner(it) } }

        if (data.segmentCounts.isNotEmpty()) {
            item { UrbanSectionTitle("Clientes por segmento", "A quién puedes llegar") }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    data.segmentCounts.entries.chunked(2).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { (key, count) ->
                                UrbanStatCard(labels[key] ?: key, count.toString(), Modifier.weight(1f), icon = Icons.Default.Groups)
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        item { UrbanSectionTitle("Últimas campañas", "Las 10 más recientes") }
        urbanListFeedback(
            state.loaded, state.loading, data.data.isNotEmpty(), state.listError,
            "No pudimos cargar las campañas", "Sin campañas todavía",
            "Llega a tus clientes con una promoción por segmento.",
            emptyActionLabel = "Nueva campaña", emptyActionIcon = Icons.Default.Add,
            onEmptyAction = { vm.clearMessages(); creating = true }
        ) { vm.load() }
        items(data.data, key = { it.id }) { campaign -> CampaignCard(campaign, labels[campaign.segmento] ?: segmentLabel(campaign.segmento, data.levels)) }
    }

    if (creating) {
        CampaignFormSheet(
            segmentCounts = data.segmentCounts,
            labels = labels,
            sending = state.sending,
            error = state.error,
            onDismiss = { creating = false },
            onSend = { body -> vm.create(body) { creating = false } }
        )
    }
}

@Composable
private fun CampaignCard(c: CampaignRow, segment: String) {
    UrbanCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(c.titulo ?: "—", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = UrbanColors.Ink)
                Text(segment, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
            }
            // statusStyle (dentro de UrbanStatusPill) ya pone la etiqueta y el tono de cada estado.
                                c.estado?.let { UrbanStatusPill(it) }
        }
        c.cuerpo?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Ink, maxLines = 3)
        }
        Spacer(Modifier.height(8.dp))
        val whenText = when (c.estado) {
            "programada" -> c.programadaPara?.let { "Programada para ${formatWhen(it)}" }
            else -> (c.enviadaEn ?: c.createdAt)?.let { "Enviada ${formatWhen(it)}" }
        }
        whenText?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = UrbanColors.Muted) }
        if (c.estado == "enviada") {
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(UrbanFormat.count(c.destinatarios, "destinatario", "destinatarios"), style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                Text("${"%.0f".format(c.openRate)}% aperturas", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                Text("${"%.0f".format(c.clickRate)}% clics", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CampaignFormSheet(
    segmentCounts: Map<String, Int>,
    labels: Map<String, String>,
    sending: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSend: (CreateCampaignRequest) -> Unit
) {
    var titulo by remember { mutableStateOf("") }
    var cuerpo by remember { mutableStateOf("") }
    var ctaLabel by remember { mutableStateOf("") }
    var ctaUrl by remember { mutableStateOf("") }
    var segmento by remember { mutableStateOf(if ("todos" in segmentCounts) "todos" else segmentCounts.keys.firstOrNull() ?: "todos") }
    var schedule by remember { mutableStateOf(false) }
    var scheduledAt by remember { mutableStateOf<LocalDateTime?>(null) }
    var pickingDate by remember { mutableStateOf(false) }
    var pickingTime by remember { mutableStateOf<LocalDate?>(null) }
    var submitted by remember { mutableStateOf(false) }
    var confirming by remember { mutableStateOf(false) }

    val recipients = segmentCounts[segmento] ?: 0
    val draft = CampaignDraft(titulo, cuerpo, ctaLabel, ctaUrl, schedule, scheduledAt)
    val errors = validateCampaign(draft, recipients)

    val form = rememberUrbanFormState()
    val blockerLabels = listOfNotNull(
        "Título".takeIf { errors.titulo != null }, "Mensaje".takeIf { errors.cuerpo != null },
        "Texto del botón".takeIf { errors.ctaLabel != null }, "Enlace del botón".takeIf { errors.ctaUrl != null },
        "Fecha y hora".takeIf { errors.scheduledAt != null }, "Segmento".takeIf { errors.segment != null }
    )

    UrbanFormSheet(
        eyebrow = "Campañas",
        title = "Nueva campaña",
        primaryLabel = if (schedule) "Programar campaña" else "Enviar ahora",
        onSubmit = { confirming = true },
        onDismiss = onDismiss,
        saving = sending,
        blocker = formBlockerText(blockerLabels),
        error = error
    ) {
        UrbanFormSection("Mensaje", icon = Icons.Default.Campaign) {
            UrbanTextField(
                titulo, { titulo = it }, "Título", Modifier.fillMaxWidth(),
                leadingIcon = Icons.Default.Campaign, placeholder = "2x1 en barba este viernes",
                error = form.error("titulo", errors.titulo), onBlur = { form.touch("titulo") },
                helper = "${titulo.length}/$CAMPAIGN_TITLE_MAX",
                capitalization = KeyboardCapitalization.Sentences
            )
            UrbanTextField(
                cuerpo, { cuerpo = it }, "Mensaje", Modifier.fillMaxWidth(),
                leadingIcon = Icons.Default.ChatBubbleOutline, placeholder = "Lo que recibirá el cliente",
                error = form.error("cuerpo", errors.cuerpo), onBlur = { form.touch("cuerpo") },
                helper = "${cuerpo.length}/$CAMPAIGN_BODY_MAX",
                capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Default, minLines = 4
            )
            UrbanTextField(
                ctaLabel, { ctaLabel = it }, "Texto del botón (opcional)", Modifier.fillMaxWidth(),
                leadingIcon = Icons.Default.SmartButton, placeholder = "Reservar ahora",
                error = form.error("ctaLabel", errors.ctaLabel), onBlur = { form.touch("ctaLabel") }
            )
            UrbanTextField(
                ctaUrl, { ctaUrl = it }, "Enlace del botón (opcional)", Modifier.fillMaxWidth(),
                leadingIcon = Icons.Default.Link, placeholder = "https://",
                error = form.error("ctaUrl", errors.ctaUrl), onBlur = { form.touch("ctaUrl") },
                keyboardType = KeyboardType.Uri
            )
        }
        UrbanFormSection("A quién", icon = Icons.Default.Groups) {
            UrbanFormChoice(
                label = "Segmento", options = segmentCounts.keys.toList(), selected = segmento, onSelect = { segmento = it },
                optionLabel = { "${labels[it] ?: it} (${segmentCounts[it] ?: 0})" }
            )
            errors.segment?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Danger) }
        }
        UrbanFormSection("Envío", icon = Icons.Default.Schedule) {
            UrbanFormChoice(
                label = "Cuándo", options = listOf(false, true), selected = schedule, onSelect = { schedule = it },
                optionLabel = { if (it) "Programar" else "Ahora" }
            )
            if (schedule) {
                UrbanOutlineButton(
                    text = scheduledAt?.let { formatWhen(formatScheduleForServer(it).replace(' ', 'T')) } ?: "Elegir fecha y hora",
                    onClick = { pickingDate = true },
                    icon = Icons.Default.Today,
                    modifier = Modifier.fillMaxWidth()
                )
                errors.scheduledAt?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted) }
            }
        }
    }

    if (pickingDate) {
        val todayUtc = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        val dateState = rememberDatePickerState(
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= todayUtc
            }
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(
                    enabled = dateState.selectedDateMillis != null,
                    onClick = {
                        dateState.selectedDateMillis?.let { millis ->
                            pickingTime = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        }
                        pickingDate = false
                    }
                ) { Text("Siguiente") }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text("Cancelar") } }
        ) { DatePicker(state = dateState) }
    }

    pickingTime?.let { date ->
        val timeState = rememberTimePickerState(initialHour = 10, initialMinute = 0, is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickingTime = null },
            containerColor = UrbanColors.Card,
            title = { Text("Hora de envío") },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    scheduledAt = LocalDateTime.of(date, LocalTime.of(timeState.hour, timeState.minute))
                    pickingTime = null
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { pickingTime = null }) { Text("Cancelar") } }
        )
    }

    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            containerColor = UrbanColors.Card,
            title = { Text(if (schedule) "Confirmar programación" else "Confirmar envío") },
            text = {
                Text(
                    sendConfirmation(recipients, schedule, scheduledAt?.let { formatWhen(formatScheduleForServer(it).replace(' ', 'T')) }),
                    color = UrbanColors.Muted
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirming = false
                    onSend(
                        CreateCampaignRequest(
                            titulo = titulo.trim(),
                            cuerpo = cuerpo.trim(),
                            ctaLabel = ctaLabel.trim().ifEmpty { null },
                            ctaUrl = ctaUrl.trim().ifEmpty { null },
                            segmento = segmento,
                            modo = if (schedule) "programar" else "ahora",
                            programadaPara = if (schedule) scheduledAt?.let(::formatScheduleForServer) else null
                        )
                    )
                }) { Text(if (schedule) "Sí, programar" else "Sí, enviar") }
            },
            dismissButton = { TextButton(onClick = { confirming = false }) { Text("Volver") } }
        )
    }
}
