package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.urbanblade.mobile.ui.viewmodel.CampaignsViewModel
import com.urbanblade.mobile.data.model.CreateCampaignRequest

private val SEGMENT_LABEL_EXTRA = mapOf("todos" to "Todos", "inactive" to "Inactivos")

@Composable
fun CampaignsScreen(onBack: () -> Unit, vm: CampaignsViewModel = viewModel()) {
    val campaigns by vm.campaigns.collectAsState()
    val busy by vm.busy.collectAsState()
    val sending by vm.sending.collectAsState()
    val message by vm.message.collectAsState()
    val error by vm.error.collectAsState()
    var showCreate by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.load() }

    val segmentLabels = remember(campaigns) { SEGMENT_LABEL_EXTRA + campaigns.levels }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            UrbanTopBar("Campañas", onBack) {
                IconButton(onClick = { showCreate = true }) { Icon(Icons.Default.Add, "Nueva campaña") }
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = UrbanColors.Gold) }
            error?.let { item { UrbanErrorBanner(it) } }
            message?.let { item { UrbanInfoBanner(it, Icons.Default.CheckCircle) } }

            if (campaigns.segmentCounts.isNotEmpty()) {
                item { UrbanSectionTitle("Clientes por segmento") }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        campaigns.segmentCounts.entries.chunked(2).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                row.forEach { (key, count) ->
                                    UrbanMetricCard(segmentLabels[key] ?: key, count.toString(), Icons.Default.Groups, Modifier.weight(1f))
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            item { UrbanSectionTitle("Últimas campañas", "10 más recientes") }
            if (campaigns.data.isEmpty() && !busy) {
                item { UrbanEmptyState("Sin campañas todavía", "Crea la primera desde el botón +.", Icons.Default.Campaign) }
            }
            items(campaigns.data) { c ->
                UrbanCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text(c.titulo ?: "—", style = MaterialTheme.typography.titleMedium)
                            Text(segmentLabels[c.segmento] ?: c.segmento.orEmpty(), style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                        }
                        c.estado?.let { SimpleStatusPill(it) }
                    }
                    c.cuerpo?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 3)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("${c.destinatarios} destinatarios", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                        Text("${"%.0f".format(c.openRate)}% aperturas", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                        Text("${"%.0f".format(c.clickRate)}% clics", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                    }
                }
            }
        }
    }

    if (showCreate) {
        CreateCampaignDialog(
            segmentLabels = segmentLabels,
            sending = sending,
            onDismiss = { showCreate = false },
            onCreate = { req -> vm.create(req) { showCreate = false } }
        )
    }
}

@Composable
private fun CreateCampaignDialog(
    segmentLabels: Map<String, String>,
    sending: Boolean,
    onDismiss: () -> Unit,
    onCreate: (CreateCampaignRequest) -> Unit
) {
    var titulo by remember { mutableStateOf("") }
    var cuerpo by remember { mutableStateOf("") }
    var ctaLabel by remember { mutableStateOf("") }
    var ctaUrl by remember { mutableStateOf("") }
    var segmento by remember { mutableStateOf(segmentLabels.keys.firstOrNull() ?: "todos") }
    var modo by remember { mutableStateOf("ahora") }
    var programadaPara by remember { mutableStateOf("") }

    val valid = titulo.isNotBlank() && cuerpo.isNotBlank() && (modo == "ahora" || programadaPara.isNotBlank())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva campaña") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(titulo, { titulo = it }, label = { Text("Título") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(cuerpo, { cuerpo = it }, label = { Text("Mensaje") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(ctaLabel, { ctaLabel = it }, label = { Text("Texto del botón (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(ctaUrl, { ctaUrl = it }, label = { Text("Enlace del botón (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                Text("Segmento", style = MaterialTheme.typography.labelMedium, color = UrbanColors.Muted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    segmentLabels.forEach { (key, label) ->
                        FilterChip(selected = segmento == key, onClick = { segmento = key }, label = { Text(label) })
                    }
                }

                Text("Envío", style = MaterialTheme.typography.labelMedium, color = UrbanColors.Muted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = modo == "ahora", onClick = { modo = "ahora" }, label = { Text("Ahora") })
                    FilterChip(selected = modo == "programar", onClick = { modo = "programar" }, label = { Text("Programar") })
                }
                if (modo == "programar") {
                    OutlinedTextField(
                        programadaPara,
                        { programadaPara = it },
                        label = { Text("Fecha y hora") },
                        placeholder = { Text("AAAA-MM-DD HH:mm") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !sending && valid,
                onClick = {
                    onCreate(
                        CreateCampaignRequest(
                            titulo = titulo,
                            cuerpo = cuerpo,
                            ctaLabel = ctaLabel.takeIf { it.isNotBlank() },
                            ctaUrl = ctaUrl.takeIf { it.isNotBlank() },
                            segmento = segmento,
                            modo = modo,
                            programadaPara = if (modo == "programar") programadaPara else null
                        )
                    )
                }
            ) { Text(if (sending) "Enviando…" else if (modo == "ahora") "Enviar ahora" else "Programar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
