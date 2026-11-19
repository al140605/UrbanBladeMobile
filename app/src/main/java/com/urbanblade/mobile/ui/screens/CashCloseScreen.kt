package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.components.UrbanFormat
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.*
import com.urbanblade.mobile.ui.components.UrbanModuleScreen


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
