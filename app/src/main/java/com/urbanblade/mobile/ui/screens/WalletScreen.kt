package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urbanblade.mobile.data.model.*
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.WalletViewModel

/**
 * Wallet del cliente: puntos/nivel, membresía, paquetes, gift cards y
 * referidos -- todo desde endpoints de autoservicio ya existentes en
 * barber (memberships/mine, packages, gift-cards/mine, referrals/mine,
 * lealtad embebida en dashboard). Solo lectura esta ronda: comprar
 * paquetes/membresías/gift cards queda para la ronda de checkout con
 * Stripe -- no hay botones de compra a medias.
 */
@Composable
fun WalletScreen(onBack: () -> Unit, vm: WalletViewModel = viewModel()) {
    val data by vm.data.collectAsState()
    val loading by vm.loading.collectAsState()
    val busy by vm.busy.collectAsState()
    val message by vm.message.collectAsState()
    val error by vm.error.collectAsState()
    val clipboard = LocalClipboardManager.current
    var confirmCancelMembership by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.load() }

    Scaffold(containerColor = Color.Transparent, topBar = { UrbanTopBar(title = "Wallet", onBack = onBack) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                UrbanPageHeader(
                    title = "Tus beneficios",
                    subtitle = "Puntos, membresía, paquetes y referidos en un solo lugar.",
                    eyebrow = "Wallet"
                )
            }
            if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = UrbanColors.Gold) }
            error?.let { item { UrbanErrorBanner(it) } }
            message?.let { item { UrbanInfoBanner(it, Icons.Default.CheckCircle) } }

            data.loyalty?.let { loyalty -> item { LoyaltyCard(loyalty) } }

            item { UrbanSectionTitle("Mi membresía", null) }
            item {
                val membership = data.membership
                if (membership != null) {
                    MembershipCard(membership, busy) { confirmCancelMembership = true }
                } else {
                    UrbanEmptyState("Sin membresía activa", "Pregunta en recepción por los planes disponibles.", Icons.Default.CardMembership)
                }
            }

            item { UrbanSectionTitle("Mis paquetes", null) }
            if (data.packages.isEmpty()) {
                item { UrbanEmptyState("Sin paquetes activos", null, Icons.Default.Redeem) }
            } else {
                items(data.packages, key = { it.id }) { PackageCard(it) }
            }

            item { UrbanSectionTitle("Mis gift cards", null) }
            if (data.giftCards.isEmpty()) {
                item { UrbanEmptyState("Sin gift cards", null, Icons.Default.CardGiftcard) }
            } else {
                items(data.giftCards, key = { it.code }) { GiftCardRow(it) }
            }

            item { UrbanSectionTitle("Invita y gana", "Comparte tu código y suma puntos por cada referido") }
            data.referrals?.let { referrals -> item { ReferralCard(referrals) { clipboard.setText(AnnotatedString(it)) } } }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }

    if (confirmCancelMembership) {
        AlertDialog(
            onDismissRequest = { confirmCancelMembership = false },
            containerColor = UrbanColors.Card,
            title = { Text("Cancelar membresía") },
            text = { Text("Seguirás teniendo sus beneficios hasta el fin del periodo actual. ¿Confirmas?", color = UrbanColors.Muted) },
            confirmButton = {
                TextButton(onClick = { vm.cancelMembership(); confirmCancelMembership = false }) {
                    Text("Sí, cancelar", color = UrbanColors.Danger)
                }
            },
            dismissButton = { TextButton(onClick = { confirmCancelMembership = false }) { Text("Volver") } }
        )
    }
}

@Composable
private fun LoyaltyCard(loyalty: ClientLoyalty) {
    UrbanPremiumCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(loyalty.nivelLabel ?: loyalty.nivel ?: "Nivel", style = MaterialTheme.typography.titleLarge, color = UrbanColors.Gold)
                Text("${loyalty.puntos} puntos", style = MaterialTheme.typography.bodyMedium, color = UrbanColors.Muted)
            }
            if (loyalty.discountPct > 0) {
                Surface(shape = RoundedCornerShape(12.dp), color = Color(0x22D4AF37)) {
                    Text(
                        "${loyalty.discountPct.toInt()}% dto.",
                        Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = UrbanColors.Gold
                    )
                }
            }
        }
        if (loyalty.nextNivelLabel != null) {
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { (loyalty.progressPct / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = UrbanColors.Gold,
                trackColor = UrbanColors.Line
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Te faltan ${loyalty.citasFaltan} citas para ${loyalty.nextNivelLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = UrbanColors.Muted
            )
        }
    }
}

@Composable
private fun MembershipCard(membership: MyMembership, busy: Boolean, onCancel: () -> Unit) {
    UrbanPremiumCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(membership.plan?.nombre ?: "Membresía", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "\$${"%.0f".format(membership.plan?.precioMensual ?: 0.0)}/mes · ${membership.plan?.descuentoPct?.toInt() ?: 0}% dto.",
                    style = MaterialTheme.typography.bodySmall,
                    color = UrbanColors.Muted
                )
                membership.periodoActualFin?.let {
                    Text(
                        if (membership.cancelarAlFinalizar) "Se cancela el $it" else "Se renueva el $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = UrbanColors.Muted
                    )
                }
            }
            UrbanStatusPill(membership.estado)
        }
        if (!membership.cancelarAlFinalizar) {
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = onCancel, enabled = !busy) {
                Text(if (busy) "Cancelando…" else "Cancelar membresía", color = UrbanColors.Danger)
            }
        }
    }
}

@Composable
private fun PackageCard(item: MyPackage) {
    UrbanCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.nombre ?: item.service?.nombre ?: "Paquete", style = MaterialTheme.typography.titleMedium)
                Text("${item.usosRestantes} de ${item.usosTotales} usos restantes", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                item.expiraEn?.let { Text("Vence: $it", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted) }
            }
            UrbanStatusPill(item.estado ?: "activo")
        }
    }
}

@Composable
private fun GiftCardRow(card: GiftCard) {
    UrbanCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CardGiftcard, null, tint = UrbanColors.Gold)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(card.code, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Saldo inicial: \$${"%.0f".format(card.montoInicial)}", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
            }
            Text("\$${"%.0f".format(card.saldo)}", style = MaterialTheme.typography.titleLarge, color = UrbanColors.Gold)
        }
    }
}

@Composable
private fun ReferralCard(referrals: ReferralInfo, onCopyCode: (String) -> Unit) {
    UrbanPremiumCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Tu código", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                Text(referrals.codigoReferido ?: "—", style = MaterialTheme.typography.headlineSmall, color = UrbanColors.Gold, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = { referrals.codigoReferido?.let(onCopyCode) }) {
                Icon(Icons.Default.ContentCopy, "Copiar código", tint = UrbanColors.Gold)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Ganas ${referrals.puntosPorReferido} puntos por cada amigo que se registre y complete su primera cita.",
            style = MaterialTheme.typography.bodySmall,
            color = UrbanColors.Muted
        )
        if (referrals.referidos.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = UrbanColors.Line)
            Spacer(Modifier.height(12.dp))
            Text("${referrals.completados} referidos completados", style = MaterialTheme.typography.labelMedium, color = UrbanColors.Muted)
            referrals.referidos.forEach { entry ->
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(entry.referido ?: "Cliente", style = MaterialTheme.typography.bodyMedium)
                    UrbanStatusPill(entry.estado ?: "pendiente")
                }
            }
        }
    }
}
