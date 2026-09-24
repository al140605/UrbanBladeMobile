package com.urbanblade.mobile.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urbanblade.mobile.data.model.PaymentRow
import com.urbanblade.mobile.data.model.PendingPaymentRow
import com.urbanblade.mobile.ui.components.UrbanCard
import com.urbanblade.mobile.ui.components.UrbanEmptyState
import com.urbanblade.mobile.ui.components.UrbanErrorBanner
import com.urbanblade.mobile.ui.components.UrbanFormat
import com.urbanblade.mobile.ui.components.UrbanHBars
import com.urbanblade.mobile.ui.components.UrbanInfoBanner
import com.urbanblade.mobile.ui.components.UrbanOutlineButton
import com.urbanblade.mobile.ui.components.UrbanPremiumCard
import com.urbanblade.mobile.ui.components.UrbanPrimaryButton
import com.urbanblade.mobile.ui.components.UrbanSectionTitle
import com.urbanblade.mobile.ui.components.UrbanSkeletonList
import com.urbanblade.mobile.ui.components.UrbanTextField
import com.urbanblade.mobile.ui.components.UrbanTopBar
import com.urbanblade.mobile.ui.components.UrbanMascotState
import com.urbanblade.mobile.ui.components.UrbanStateKind
import com.urbanblade.mobile.ui.components.UrbanPageHeader
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.PaymentMethodFilter
import com.urbanblade.mobile.ui.viewmodel.PaymentsStaffViewModel
import com.urbanblade.mobile.ui.viewmodel.REJECT_REASON_MAX

private fun money(value: Double) = "$" + String.format(java.util.Locale("es", "MX"), "%,.2f", value)
private fun moneyShort(value: Double) = "$" + String.format(java.util.Locale("es", "MX"), "%,.0f", value)

/** Pagos del negocio: totales del servidor, comprobantes por revisar e historial filtrable. */
@Composable
fun PaymentsStaffScreen(onBack: () -> Unit, vm: PaymentsStaffViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    var approving by remember { mutableStateOf<PendingPaymentRow?>(null) }
    var rejecting by remember { mutableStateOf<PendingPaymentRow?>(null) }

    LaunchedEffect(Unit) { vm.load() }

    fun openUrl(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = { UrbanTopBar("", onBack) { IconButton(onClick = { vm.load() }) { Icon(Icons.Default.Refresh, "Actualizar") } } }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { UrbanPageHeader(title = "Cobros y pagos", subtitle = "Verifica comprobantes y revisa el historial.", eyebrow = "OPERACIÓN") }
            if (state.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = UrbanColors.Gold) }
            state.notice?.let { item { UrbanInfoBanner(it, Icons.Default.CheckCircle) } }
            if (rejecting == null) state.error?.let { item { UrbanErrorBanner(it) } }

            item {
                UrbanPremiumCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("Cobrado hoy", style = MaterialTheme.typography.labelLarge, color = UrbanColors.Gold)
                            Text(moneyShort(state.stats.totalHoy), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = UrbanColors.Ink)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Este mes", style = MaterialTheme.typography.labelLarge, color = UrbanColors.Gold)
                            Text(moneyShort(state.stats.totalMes), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = UrbanColors.Ink)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        UrbanFormat.count(state.stats.count, "pago registrado", "pagos registrados") + " en total",
                        style = MaterialTheme.typography.bodySmall,
                        color = UrbanColors.Muted
                    )
                }
            }
            if (state.stats.metodos.values.any { it > 0 }) {
                item {
                    val methods = state.stats.metodos.entries.filter { it.value > 0 }.sortedByDescending { it.value }
                    UrbanCard(Modifier.fillMaxWidth()) {
                        UrbanSectionTitle("Pagos por método")
                        Spacer(Modifier.height(12.dp))
                        UrbanHBars(
                            labels = methods.map { it.key.replaceFirstChar { c -> c.uppercase() } },
                            values = methods.map { it.value.toDouble() },
                            format = { it.toInt().toString() }
                        )
                    }
                }
            }

            if (state.pending.isNotEmpty()) {
                item { UrbanSectionTitle("Por revisar", UrbanFormat.count(state.pending.size, "transferencia pendiente", "transferencias pendientes")) }
                items(state.pending, key = { it.id }) { payment ->
                    PendingCard(
                        payment = payment,
                        busy = state.busyPaymentId == payment.id,
                        onReceipt = { payment.comprobanteUrl?.let(::openUrl) },
                        onApprove = { approving = payment },
                        onReject = { vm.clearMessages(); rejecting = payment }
                    )
                }
            }

            item { UrbanSectionTitle("Historial", "Pagos registrados en UrbanBlade") }
            item {
                UrbanTextField(
                    value = state.query,
                    onValueChange = vm::setQuery,
                    label = "Buscar por cliente, servicio o barbero",
                    leadingIcon = Icons.Default.Search,
                    imeAction = ImeAction.Search,
                    onImeAction = { vm.load() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PaymentMethodFilter.entries.forEach { m ->
                        FilterChip(selected = state.method == m, onClick = { vm.setMethod(m) }, label = { Text(m.label) })
                    }
                }
            }
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReportRange.entries.forEach { r ->
                        FilterChip(selected = state.range == r, onClick = { vm.setRange(r) }, label = { Text(if (r == ReportRange.All) "Todo el tiempo" else r.label) })
                    }
                }
            }
            if (state.loading && state.items.isEmpty()) item { UrbanSkeletonList(3) }
            if (!state.loading && state.items.isEmpty() && state.error == null) {
                item { UrbanMascotState(UrbanStateKind.EMPTY, "No hay pagos con estos filtros", "Prueba con otro método o rango de fechas.") }
            }
            items(state.items, key = { it.id }) { payment ->
                PaymentCard(payment, onReceipt = { vm.receiptUrl(payment.id, ::openUrl) })
            }
            if (state.hasMore) {
                item {
                    UrbanOutlineButton(
                        text = if (state.loadingMore) "Cargando…" else "Cargar más pagos",
                        onClick = { vm.loadMore() },
                        icon = Icons.Default.ExpandMore,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    approving?.let { payment ->
        AlertDialog(
            onDismissRequest = { approving = null },
            containerColor = UrbanColors.Card,
            title = { Text("Aprobar comprobante") },
            text = {
                Text(
                    "¿Confirmas que recibiste ${money(payment.monto)}${payment.appointment?.client?.let { " de $it" } ?: ""}? La cita quedará completada.",
                    color = UrbanColors.Muted
                )
            },
            confirmButton = { TextButton(onClick = { vm.approve(payment.id); approving = null }) { Text("Sí, aprobar") } },
            dismissButton = { TextButton(onClick = { approving = null }) { Text("Volver") } }
        )
    }

    rejecting?.let { payment ->
        RejectDialog(
            payment = payment,
            busy = state.busyPaymentId == payment.id,
            error = state.error,
            onDismiss = { rejecting = null },
            onConfirm = { reason -> vm.reject(payment.id, reason) { rejecting = null } }
        )
    }
}

@Composable
private fun PendingCard(payment: PendingPaymentRow, busy: Boolean, onReceipt: () -> Unit, onApprove: () -> Unit, onReject: () -> Unit) {
    val price = payment.appointment?.servicePrice
    val check = compareTransfer(payment.monto, price)
    UrbanPremiumCard(Modifier.fillMaxWidth()) {
        Text("Transferencia", style = MaterialTheme.typography.labelMedium, color = UrbanColors.Gold)
        Text(money(payment.monto), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = UrbanColors.Ink)
        Text(
            listOfNotNull(payment.appointment?.client, payment.appointment?.service).joinToString(" · ").ifEmpty { "Cita sin datos" },
            style = MaterialTheme.typography.bodyMedium,
            color = UrbanColors.Ink
        )
        payment.createdAt?.let { Text("Enviado el ${formatWhen(it)}", style = MaterialTheme.typography.labelSmall, color = UrbanColors.Muted) }

        Spacer(Modifier.height(10.dp))
        when (check) {
            TransferCheck.Match -> Text("Coincide con el precio de lista (${money(price ?: 0.0)}).", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Success)
            TransferCheck.Differs -> Text(
                "Difiere del precio de lista (${money(price ?: 0.0)}). Puede deberse a un descuento; confírmalo en el comprobante.",
                style = MaterialTheme.typography.bodySmall,
                color = UrbanColors.Warning
            )
            TransferCheck.Unknown -> Unit
        }
        payment.ocrMontoDetectado?.let { ocr ->
            Text(
                "Monto que leyó el sistema en la imagen: ${money(ocr)}" + if (ocrDisagrees(payment.monto, ocr)) " (no coincide)" else "",
                style = MaterialTheme.typography.bodySmall,
                color = if (ocrDisagrees(payment.monto, ocr)) UrbanColors.Warning else UrbanColors.Muted
            )
        }

        Spacer(Modifier.height(12.dp))
        if (!payment.comprobanteUrl.isNullOrBlank()) {
            UrbanOutlineButton("Ver comprobante", onReceipt, Modifier.fillMaxWidth(), Icons.Default.Receipt)
            Spacer(Modifier.height(8.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UrbanPrimaryButton("Aprobar", onApprove, Modifier.weight(1f), enabled = !busy, icon = Icons.Default.Check, loading = busy)
            UrbanOutlineButton("Rechazar", onReject, Modifier.weight(1f))
        }
    }
}

@Composable
private fun PaymentCard(payment: PaymentRow, onReceipt: () -> Unit) {
    UrbanCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    payment.metodoPago?.replaceFirstChar { it.uppercase() } ?: "Pago",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = UrbanColors.Ink
                )
                payment.appointment?.let { a ->
                    Text(listOfNotNull(a.service, a.client, a.barber).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted, maxLines = 2)
                }
                Text(formatWhen(payment.createdAt), style = MaterialTheme.typography.labelSmall, color = UrbanColors.Muted)
            }
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                Text(money(payment.monto + payment.propina), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = UrbanColors.Gold)
                if (payment.propina > 0) Text("+${moneyShort(payment.propina)} propina", style = MaterialTheme.typography.labelSmall, color = UrbanColors.Muted)
            }
            IconButton(onClick = onReceipt) { Icon(Icons.Default.Payments, "Ver comprobante", tint = UrbanColors.Muted) }
        }
    }
}

@Composable
private fun RejectDialog(payment: PendingPaymentRow, busy: Boolean, error: String?, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = UrbanColors.Card,
        title = { Text("Rechazar comprobante") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "El cliente${payment.appointment?.client?.let { " ($it)" } ?: ""} verá este motivo y podrá subir otro comprobante.",
                    color = UrbanColors.Muted,
                    style = MaterialTheme.typography.bodyMedium
                )
                UrbanTextField(
                    value = reason,
                    onValueChange = { reason = it.take(REJECT_REASON_MAX) },
                    label = "Motivo del rechazo",
                    capitalization = KeyboardCapitalization.Sentences,
                    helper = "${reason.length}/$REJECT_REASON_MAX",
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let { UrbanErrorBanner(it) }
            }
        },
        confirmButton = {
            TextButton(enabled = !busy && reason.isNotBlank(), onClick = { onConfirm(reason) }) {
                Text(if (busy) "Enviando…" else "Rechazar", color = UrbanColors.Danger)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Volver") } }
    )
}
