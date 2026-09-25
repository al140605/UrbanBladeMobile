package com.urbanblade.mobile.ui.screens

import android.content.res.ColorStateList
import android.net.Uri
import android.view.ContextThemeWrapper
import android.widget.EditText
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.stripe.android.view.CardMultilineWidget
import com.urbanblade.mobile.data.model.SavedCard
import com.urbanblade.mobile.data.model.TransferInfo
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.BookingPayMethod

/** Lo que el cliente eligió en el paso de pago de la reserva. */
class BookingPaymentState {
    var method by mutableStateOf(BookingPayMethod.EFECTIVO)
    var tipOption by mutableStateOf(TIP_NONE)
    var customTip by mutableStateOf("")
    var receiptUri by mutableStateOf<Uri?>(null)
    var useNewCard by mutableStateOf(false)
    var selectedCardId by mutableStateOf<String?>(null)
    var saveCard by mutableStateOf(true)

    /** Formulario de Stripe en pantalla; no es estado de Compose, solo la vista viva para leer lo que escribió el cliente. */
    var cardWidget: CardMultilineWidget? = null

    /** Propina estimada sobre el precio de lista; barber la vuelve a validar y solo la suma aparte. */
    fun tipFor(base: Double): Double = when (tipOption) {
        TIP_10 -> base * 0.10
        TIP_15 -> base * 0.15
        TIP_OTHER -> customTip.toDoubleOrNull() ?: 0.0
        else -> 0.0
    }.let { Math.round(it * 100) / 100.0 }

    /** Tarjeta guardada con la que se paga, o null si es tarjeta nueva (o el método no es tarjeta). */
    fun savedCardToUse(cards: List<SavedCard>): String? {
        if (method != BookingPayMethod.TARJETA || useNewCard || cards.isEmpty()) return null
        return selectedCardId?.takeIf { id -> cards.any { it.id == id } } ?: cards.first().id
    }

    companion object {
        const val TIP_NONE = 0
        const val TIP_10 = 1
        const val TIP_15 = 2
        const val TIP_OTHER = 3
    }
}

@Composable
fun rememberBookingPaymentState() = remember { BookingPaymentState() }

/** "visa" -> "Visa", como lo muestran las apps de pago. */
fun cardBrandLabel(brand: String): String = when (brand.lowercase()) {
    "visa" -> "Visa"
    "mastercard" -> "Mastercard"
    "amex" -> "American Express"
    "discover" -> "Discover"
    else -> brand.replaceFirstChar { it.uppercase() }.ifBlank { "Tarjeta" }
}

fun cardExpiryLabel(card: SavedCard): String = "Vence %02d/%02d".format(card.expMonth, card.expYear % 100)

private fun money(value: Double) = "\$" + "%,.0f".format(value)

/**
 * Paso de pago de la reserva, igual que la web: propina, total estimado y
 * los tres métodos (efectivo en el salón, transferencia con los datos
 * bancarios del negocio y tarjeta con tarjetas guardadas o una nueva).
 */
@Composable
fun BookingPaymentSection(
    state: BookingPaymentState,
    servicePrice: Double,
    transferInfo: TransferInfo?,
    savedCards: List<SavedCard>,
    cardAvailable: Boolean,
    testMode: Boolean,
    onPickReceipt: () -> Unit
) {
    val tip = state.tipFor(servicePrice)

    UrbanSectionTitle("Pago", "Elige cómo quieres pagar tu cita")
    Spacer(Modifier.height(16.dp))

    UrbanFieldLabel("Propina para tu barbero (opcional)")
    Spacer(Modifier.height(8.dp))
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            "Sin propina" to BookingPaymentState.TIP_NONE,
            "10%" to BookingPaymentState.TIP_10,
            "15%" to BookingPaymentState.TIP_15,
            "Otro monto" to BookingPaymentState.TIP_OTHER
        ).forEach { (label, value) ->
            FilterChip(
                selected = state.tipOption == value,
                onClick = { state.tipOption = value },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = UrbanColors.Gold, selectedLabelColor = UrbanColors.OnGold)
            )
        }
    }
    if (state.tipOption == BookingPaymentState.TIP_OTHER) {
        Spacer(Modifier.height(8.dp))
        UrbanTextField(
            value = state.customTip,
            onValueChange = { state.customTip = it.filter { c -> c.isDigit() || c == '.' }.take(7) },
            label = "Monto de la propina",
            placeholder = "Monto en pesos",
            leadingIcon = Icons.Default.AttachMoney,
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
            imeAction = androidx.compose.ui.text.input.ImeAction.Done
        )
    }

    Spacer(Modifier.height(16.dp))
    UrbanPremiumCard(Modifier.fillMaxWidth()) {
        UrbanKeyValue("Servicio", money(servicePrice))
        if (tip > 0) {
            Spacer(Modifier.height(6.dp))
            UrbanKeyValue("Propina", money(tip))
        }
        HorizontalDivider(Modifier.padding(vertical = 10.dp), color = UrbanColors.Muted.copy(alpha = 0.2f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Total de la cita", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(money(servicePrice + tip), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = UrbanColors.Gold)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Si tienes descuento por nivel o membresía, se aplica al cobrar.",
            style = MaterialTheme.typography.bodySmall,
            color = UrbanColors.Muted
        )
    }

    Spacer(Modifier.height(24.dp))
    UrbanFieldLabel("Método de pago")
    Spacer(Modifier.height(10.dp))
    // Propuesta A (25-sep): tres mosaicos lado a lado; debajo solo el detalle del método elegido.
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Max).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PayMethodTile(
            state.method == BookingPayMethod.EFECTIVO, "Efectivo", "En el salón", Icons.Default.Payments, Modifier.weight(1f)
        ) { state.method = BookingPayMethod.EFECTIVO }
        PayMethodTile(
            state.method == BookingPayMethod.TRANSFERENCIA, "Transferencia", "SPEI", Icons.Default.AccountBalance, Modifier.weight(1f)
        ) { state.method = BookingPayMethod.TRANSFERENCIA }
        if (cardAvailable) {
            PayMethodTile(
                state.method == BookingPayMethod.TARJETA, "Tarjeta", "Paga ahora", Icons.Default.CreditCard, Modifier.weight(1f)
            ) { state.method = BookingPayMethod.TARJETA }
        }
    }

    when (state.method) {
        BookingPayMethod.TRANSFERENCIA -> {
            Spacer(Modifier.height(16.dp))
            TransferDetails(transferInfo, servicePrice + tip, state.receiptUri != null, onPickReceipt)
        }
        BookingPayMethod.TARJETA -> {
            Spacer(Modifier.height(16.dp))
            CardDetails(state, savedCards, testMode)
        }
        BookingPayMethod.EFECTIVO -> {
            Spacer(Modifier.height(12.dp))
            UrbanInfoBanner("Pagas ${money(servicePrice + tip)} al llegar a recepción. Hoy no se hace ningún cargo.", Icons.Default.Storefront, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun PayMethodTile(
    selected: Boolean,
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (selected) UrbanColors.Gold.copy(alpha = 0.14f) else UrbanColors.Card,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) UrbanColors.Gold else UrbanColors.Muted.copy(alpha = 0.3f)),
        modifier = modifier
            .fillMaxHeight()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
    ) {
        Column(
            Modifier.padding(horizontal = 8.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (selected) UrbanColors.Gold else UrbanColors.Gold.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = if (selected) UrbanColors.OnGold else UrbanColors.Gold, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) UrbanColors.Gold else UrbanColors.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = UrbanColors.Muted,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Datos bancarios del negocio con botón de copiar; el comprobante es opcional y puede subirse después. */
@Composable
private fun TransferDetails(info: TransferInfo?, total: Double, receiptPicked: Boolean, onPickReceipt: () -> Unit) {
    when {
        info == null -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = UrbanColors.Gold)
            Spacer(Modifier.width(10.dp))
            Text("Cargando datos bancarios…", style = MaterialTheme.typography.bodyMedium, color = UrbanColors.Muted)
        }
        !info.configurado -> UrbanInfoBanner(
            "La barbería todavía no registra sus datos bancarios. Elige otro método o pregunta en recepción.",
            Icons.Default.Info
        )
        else -> {
            UrbanPremiumCard(Modifier.fillMaxWidth()) {
                Text("Transfiere a esta cuenta", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                info.banco?.takeIf { it.isNotBlank() }?.let { BankRow("Banco", it, copyable = false) }
                info.beneficiario?.takeIf { it.isNotBlank() }?.let { BankRow("Beneficiario", it, copyable = false) }
                info.clabe?.let { BankRow("CLABE", it.chunked(4).joinToString(" "), copyValue = it, mono = true) }
                info.concepto?.takeIf { it.isNotBlank() }?.let { BankRow("Concepto", it) }
                BankRow("Monto", money(total), copyable = false, highlight = true)
            }
            Spacer(Modifier.height(12.dp))
            UrbanOutlineButton(
                text = if (receiptPicked) "Comprobante listo · cambiar" else "Adjuntar comprobante (opcional)",
                onClick = onPickReceipt,
                icon = if (receiptPicked) Icons.Default.CheckCircle else Icons.Default.Upload,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "¿Aún no transfieres? Reserva ahora y sube el comprobante después desde Mis citas.",
                style = MaterialTheme.typography.bodySmall,
                color = UrbanColors.Muted
            )
        }
    }
}

@Composable
private fun BankRow(
    label: String,
    value: String,
    copyValue: String = value,
    copyable: Boolean = true,
    mono: Boolean = false,
    highlight: Boolean = false
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = UrbanColors.Muted)
            Text(
                value,
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = if (mono) FontFamily.Monospace else null,
                fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium,
                color = if (highlight) UrbanColors.Gold else UrbanColors.Ink
            )
        }
        if (copyable) {
            IconButton(onClick = {
                clipboard.setText(AnnotatedString(copyValue))
                Toast.makeText(context, "$label copiado", Toast.LENGTH_SHORT).show()
            }) {
                Icon(Icons.Default.ContentCopy, "Copiar $label", tint = UrbanColors.Gold)
            }
        }
    }
}

/** Tarjetas guardadas (como Spotify o Netflix) o una tarjeta nueva, con opción de guardarla. */
@Composable
private fun CardDetails(state: BookingPaymentState, savedCards: List<SavedCard>, testMode: Boolean) {
    val usingSaved = state.savedCardToUse(savedCards)

    if (savedCards.isNotEmpty()) {
        UrbanFieldLabel("Tus tarjetas")
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            savedCards.forEach { card ->
                SavedCardRow(
                    title = "${cardBrandLabel(card.brand)} •••• ${card.last4}",
                    subtitle = cardExpiryLabel(card),
                    icon = Icons.Default.CreditCard,
                    selected = usingSaved == card.id
                ) {
                    state.useNewCard = false
                    state.selectedCardId = card.id
                }
            }
            SavedCardRow(
                title = "Usar otra tarjeta",
                subtitle = "Crédito o débito",
                icon = Icons.Default.AddCard,
                selected = usingSaved == null
            ) { state.useNewCard = true }
        }
    }

    if (usingSaved == null) {
        if (savedCards.isNotEmpty()) Spacer(Modifier.height(16.dp))
        UrbanFieldLabel("Datos de la tarjeta")
        Spacer(Modifier.height(8.dp))
        // El formulario de Stripe es una vista clásica con tema claro: se muestra sobre una tarjeta blanca
        // para que se lea bien en el tema oscuro de la app.
        Surface(shape = MaterialTheme.shapes.medium, color = Color.White, modifier = Modifier.fillMaxWidth()) {
            AndroidView(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                factory = { ctx ->
                    val themed = ContextThemeWrapper(ctx, com.google.android.material.R.style.Theme_MaterialComponents_Light_NoActionBar)
                    CardMultilineWidget(themed).also { widget ->
                        widget.setShouldShowPostalCode(false)
                        styleCardWidget(widget, UrbanColors.Gold.toArgb())
                        state.cardWidget = widget
                    }
                }
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = state.saveCard,
                onCheckedChange = { state.saveCard = it },
                colors = CheckboxDefaults.colors(checkedColor = UrbanColors.Gold, checkmarkColor = UrbanColors.OnGold)
            )
            Text("Guardar esta tarjeta para mis próximos pagos", style = MaterialTheme.typography.bodyMedium)
        }
    }

    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Lock, null, tint = UrbanColors.Muted, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            "Pago seguro con Stripe. UrbanBlade no ve ni guarda el número de tu tarjeta.",
            style = MaterialTheme.typography.bodySmall,
            color = UrbanColors.Muted
        )
    }
    if (testMode) {
        Spacer(Modifier.height(4.dp))
        Text(
            "Modo de prueba: usa 4242 4242 4242 4242, cualquier fecha futura y cualquier CVC.",
            style = MaterialTheme.typography.bodySmall,
            color = UrbanColors.Gold
        )
    }
}

/**
 * El formulario de Stripe hereda los colores claros del tema oscuro de la app: sin esto los textos
 * ("Número de tarjeta", "Fecha de vencimiento", "CVC") salen blancos sobre el recuadro blanco.
 */
private fun styleCardWidget(widget: CardMultilineWidget, accent: Int) {
    val ink = android.graphics.Color.rgb(0x1F, 0x1F, 0x1F)
    val hint = android.graphics.Color.rgb(0x6B, 0x6B, 0x6B)
    fun id(name: String) = widget.resources.getIdentifier(name, "id", widget.context.packageName)
    // Los contenedores son TextInputLayout de Material, que la app no compila directamente (llega con
    // Stripe): se ajustan sus colores por nombre de método y, si algún día cambian, se ignora sin romper.
    listOf("tl_card_number", "tl_expiry", "tl_cvc").forEach { name ->
        val layout = widget.findViewById<android.view.View>(id(name)) ?: return@forEach
        runCatching { layout.javaClass.getMethod("setDefaultHintTextColor", ColorStateList::class.java).invoke(layout, ColorStateList.valueOf(hint)) }
        runCatching { layout.javaClass.getMethod("setHintTextColor", ColorStateList::class.java).invoke(layout, ColorStateList.valueOf(accent)) }
        runCatching { layout.javaClass.getMethod("setBoxStrokeColor", Int::class.javaPrimitiveType).invoke(layout, accent) }
    }
    listOf("et_card_number", "et_expiry", "et_cvc").forEach { name ->
        widget.findViewById<EditText>(id(name))?.apply {
            setTextColor(ink)
            setHintTextColor(hint)
        }
    }
}

@Composable
private fun SavedCardRow(title: String, subtitle: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) UrbanColors.Gold.copy(alpha = 0.10f) else UrbanColors.Card,
        border = BorderStroke(1.dp, if (selected) UrbanColors.Gold else UrbanColors.Muted.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = UrbanColors.Gold, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
            }
            RadioButton(selected = selected, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = UrbanColors.Gold))
        }
    }
}
