package com.urbanblade.mobile.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.urbanblade.mobile.ui.theme.UrbanColors

/** Revisión de tarjeta con importe del proveedor y acciones en una sola columna. */
@Composable
fun UrbanPaymentReviewDialog(
    merchant: String, card: String, amount: String, verifying: Boolean,
    onPay: () -> Unit, onEdit: () -> Unit, onCancel: () -> Unit
) {
    Dialog(
        onDismissRequest = { if (!verifying) onCancel() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.padding(20.dp).widthIn(max = 420.dp).fillMaxWidth(),
            shape = RoundedCornerShape(24.dp), color = UrbanColors.Card,
            border = BorderStroke(1.dp, UrbanColors.Line), tonalElevation = 0.dp
        ) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(shape = RoundedCornerShape(14.dp), color = UrbanColors.CardAlt) {
                        Icon(Icons.Default.Lock, null, tint = UrbanColors.GoldText, modifier = Modifier.padding(12.dp).size(22.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text("PAGO CON TARJETA", style = MaterialTheme.typography.labelSmall, color = UrbanColors.GoldText)
                        Text(merchant, style = MaterialTheme.typography.bodyMedium, color = UrbanColors.Muted)
                    }
                }
                Spacer(Modifier.height(22.dp))
                Text("Confirmar pago", style = MaterialTheme.typography.headlineSmall, color = UrbanColors.Ink)
                Spacer(Modifier.height(6.dp))
                Text("Revisa el total antes de continuar.", style = MaterialTheme.typography.bodyMedium, color = UrbanColors.Muted)
                Spacer(Modifier.height(24.dp))
                Text("Total a pagar", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                Spacer(Modifier.height(4.dp))
                Text(amount, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold, color = UrbanColors.GoldText)
                Spacer(Modifier.height(20.dp))
                Surface(shape = RoundedCornerShape(16.dp), color = UrbanColors.CardAlt) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.CreditCard, null, tint = UrbanColors.Muted, modifier = Modifier.size(24.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Tarjeta seleccionada", style = MaterialTheme.typography.labelSmall, color = UrbanColors.Muted)
                                Text(card, style = MaterialTheme.typography.titleMedium, color = UrbanColors.Ink)
                            }
                        }
                        UrbanTextButton(onClick = onEdit, enabled = !verifying, contentPadding = PaddingValues(horizontal = 0.dp)) {
                            Text("Cambiar tarjeta", color = if (verifying) UrbanColors.Muted else UrbanColors.GoldText)
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text("Confirma con tu huella o PIN al pagar.", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                Spacer(Modifier.height(20.dp))
                UrbanPrimaryButton("Pagar $amount", onPay, Modifier.fillMaxWidth(), enabled = !verifying, loading = verifying)
                Spacer(Modifier.height(4.dp))
                UrbanTextButton(onClick = onCancel, enabled = !verifying, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancelar", color = UrbanColors.Muted)
                }
            }
        }
    }
}
