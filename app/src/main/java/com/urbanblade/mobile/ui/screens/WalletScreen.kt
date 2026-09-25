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
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }
    var confirmCancelMembership by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.load() }

    // Wallet es una pestaña de la barra inferior: sin barra superior con título (antes decía "Wallet"
    // arriba y "Tus beneficios" abajo, el mismo encabezado dos veces).
    val nothingYet = data.membership == null && data.packages.isEmpty() && data.giftCards.isEmpty()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            UrbanPageHeader(
                title = "Tus beneficios",
                subtitle = "Puntos, membresía, paquetes y referidos en un solo lugar.",
                eyebrow = "Wallet"
            )
        }
        message?.let { item { UrbanInfoBanner(it, Icons.Default.CheckCircle) } }

        when {
            error != null && data.loyalty == null && !loading -> item {
                UrbanMascotState(UrbanStateKind.ERROR, "No pudimos cargar tu wallet", error, "Reintentar") { vm.load() }
            }
            loading && data.loyalty == null -> item { UrbanSkeletonList(2) }
            else -> {
                error?.let { item { UrbanErrorBanner(it) } }
                data.loyalty?.let { loyalty ->
                    item { LoyaltyCard(loyalty) }
                    loyalty.wonRaffle?.takeIf { !it.isExpired && it.premio != null }?.let { raffle ->
                        item {
                            UrbanAttentionRow(
                                Icons.Default.EmojiEvents,
                                "Ganaste el sorteo${raffle.mes?.let { " de $it" } ?: ""}: ${raffle.premio}",
                                raffle.venceEn?.let { "Reclámalo en recepción antes del $it." } ?: "Reclámalo en recepción.",
                                UrbanColors.Success
                            )
                        }
                    }
                    if (loyalty.levels.size > 1) {
                        item { UrbanSectionTitle("Tu camino", "Más visitas, más descuento en cada servicio.") }
                        item { LevelLadder(loyalty) }
                    }
                    item { UrbanSectionTitle("Tus puntos", "Cómo se ganan y en qué los usas.") }
                    item { PointsCard(loyalty) }
                }

                if (nothingYet) {
                    item {
                        UrbanAttentionRow(
                            Icons.Default.Redeem,
                            "Aún no tienes membresía, paquetes ni gift cards",
                            "Cuando contrates uno aparecerá aquí. Pregunta en recepción por los planes.",
                            UrbanColors.Gold
                        )
                    }
                } else {
                    item { UrbanSectionTitle("Mi membresía", null) }
                    item {
                        val membership = data.membership
                        if (membership != null) {
                            MembershipCard(membership, busy) { confirmCancelMembership = true }
                        } else {
                            UrbanInlineEmpty("Sin membresía activa", Icons.Default.CardMembership, subtitle = "Pregunta en recepción por los planes disponibles.")
                        }
                    }
                    if (data.packages.isNotEmpty()) {
                        item { UrbanSectionTitle("Mis paquetes", null) }
                        items(data.packages, key = { it.id }) { PackageCard(it) }
                    }
                    if (data.giftCards.isNotEmpty()) {
                        item { UrbanSectionTitle("Mis gift cards", null) }
                        items(data.giftCards, key = { it.code }) { GiftCardRow(it) }
                    }
                }

                data.referrals?.let { referrals ->
                    item { UrbanSectionTitle("Invita y gana", "Comparte tu código y suma puntos por cada referido") }
                    item {
                        ReferralCard(
                            referrals,
                            copied = copied,
                            onCopyCode = { clipboard.setText(AnnotatedString(it)); copied = true },
                            onShare = { code ->
                                val text = "Te invito a UrbanBlade: regístrate con mi código $code y agenda tu primera cita."
                                val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
                                context.startActivity(Intent.createChooser(send, "Compartir mi código"))
                            }
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
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
    UrbanHeroCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { UrbanHeroLabel("Tu nivel") }
            if (loyalty.discountPct > 0) {
                Surface(shape = RoundedCornerShape(12.dp), color = UrbanColors.Gold.copy(alpha = 0.18f)) {
                    Text(
                        "${loyalty.discountPct.toInt()}% de descuento",
                        Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = UrbanColors.Gold
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(loyalty.nivelLabel ?: loyalty.nivel ?: "Nivel", style = MaterialTheme.typography.displaySmall, color = UrbanColors.Ink)
        Text(
            UrbanFormat.count(loyalty.puntos, "punto", "puntos") +
                if (loyalty.puntos > 0 && loyalty.maxRedeemPct != null) " · valen \$${loyalty.puntos} al pagar" else "",
            style = MaterialTheme.typography.titleMedium,
            color = UrbanColors.Gold
        )
        if (loyalty.nextNivelLabel != null) {
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { (loyalty.progressPct / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = UrbanColors.Gold,
                trackColor = UrbanColors.Ink.copy(alpha = 0.18f)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Te faltan ${UrbanFormat.count(loyalty.citasFaltan, "cita", "citas")} para ${loyalty.nextNivelLabel}" +
                    (loyalty.nextDiscountPct?.takeIf { it > 0 }?.let { ": $it % de descuento en cada servicio." } ?: "."),
                style = MaterialTheme.typography.bodySmall,
                color = UrbanColors.Muted
            )
        } else {
            Spacer(Modifier.height(10.dp))
            Text("Estás en el nivel más alto. Gracias por tu lealtad.", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
        }
    }
}

/** Escalera de niveles: los alcanzados en dorado y el actual resaltado. */
@Composable
private fun LevelLadder(loyalty: ClientLoyalty) {
    val currentIndex = loyalty.levels.indexOfFirst { it.nivel == loyalty.nivel }.coerceAtLeast(0)
    UrbanCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            loyalty.levels.forEachIndexed { index, level ->
                val reached = index <= currentIndex
                val current = index == currentIndex
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(Modifier.fillMaxWidth().height(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        val left = when { index == 0 -> Color.Transparent; reached -> UrbanColors.Gold; else -> UrbanColors.Line }
                        val right = when { index == loyalty.levels.lastIndex -> Color.Transparent; index < currentIndex -> UrbanColors.Gold; else -> UrbanColors.Line }
                        Box(Modifier.weight(1f).height(2.dp).background(left))
                        Box(Modifier.size(if (current) 18.dp else 12.dp).clip(CircleShape).background(if (reached) UrbanColors.Gold else UrbanColors.Line))
                        Box(Modifier.weight(1f).height(2.dp).background(right))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        level.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (current) UrbanColors.Gold else if (reached) UrbanColors.Ink else UrbanColors.Muted,
                        maxLines = 1
                    )
                    Text(if (level.discountPct > 0) "${level.discountPct} %" else "Base", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                    Text(if (level.citas > 0) "${level.citas} citas" else "Inicio", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                }
            }
        }
    }
}

/** Cómo se ganan los puntos, cómo se canjean y los últimos movimientos. */
@Composable
private fun PointsCard(loyalty: ClientLoyalty) {
    UrbanCard(Modifier.fillMaxWidth()) {
        if (loyalty.earnRules.isNotEmpty()) {
            Text("Cómo ganarlos", style = MaterialTheme.typography.titleSmall, color = UrbanColors.Ink)
            Spacer(Modifier.height(6.dp))
            loyalty.earnRules.forEach { rule ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(rule.descripcion, style = MaterialTheme.typography.bodyMedium, color = UrbanColors.Muted, modifier = Modifier.weight(1f))
                    Text("+${rule.puntos}", style = MaterialTheme.typography.titleSmall, color = UrbanColors.Gold)
                }
            }
            loyalty.maxRedeemPct?.let {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Canjéalos al pagar: cada punto vale \$1, hasta el $it % de tu cita.",
                    style = MaterialTheme.typography.bodySmall,
                    color = UrbanColors.Gold
                )
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = UrbanColors.Line)
            Spacer(Modifier.height(12.dp))
        }
        Text("Movimientos recientes", style = MaterialTheme.typography.titleSmall, color = UrbanColors.Ink)
        Spacer(Modifier.height(6.dp))
        if (loyalty.recentTransactions.isEmpty()) {
            Text("Aún no tienes movimientos. Tu primera cita completada suma puntos.", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
        } else {
            loyalty.recentTransactions.forEach { tx ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(tx.descripcion ?: "Movimiento", style = MaterialTheme.typography.bodyMedium, color = UrbanColors.Ink, modifier = Modifier.weight(1f))
                    Text(
                        (if (tx.puntos > 0) "+" else "") + tx.puntos,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (tx.puntos >= 0) UrbanColors.Success else UrbanColors.Danger
                    )
                }
            }
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
private fun ReferralCard(referrals: ReferralInfo, copied: Boolean, onCopyCode: (String) -> Unit, onShare: (String) -> Unit) {
    UrbanPremiumCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Tu código", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                Text(referrals.codigoReferido ?: "—", style = MaterialTheme.typography.headlineSmall, color = UrbanColors.Gold, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = { referrals.codigoReferido?.let(onCopyCode) }) {
                Icon(
                    if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                    if (copied) "Código copiado" else "Copiar código",
                    tint = UrbanColors.Gold
                )
            }
        }
        referrals.codigoReferido?.let { code ->
            Spacer(Modifier.height(10.dp))
            UrbanPrimaryButton(text = "Compartir mi código", onClick = { onShare(code) }, icon = Icons.Default.Share, modifier = Modifier.fillMaxWidth())
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
