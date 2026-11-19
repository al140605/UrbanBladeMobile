package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.payments.paymentlauncher.rememberPaymentLauncher
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.urbanblade.mobile.BuildConfig
import com.urbanblade.mobile.core.payment.isStripeConfigured
import com.urbanblade.mobile.core.payment.rememberUrbanPaymentSheet
import com.urbanblade.mobile.data.model.AppointmentRow
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.components.UrbanFormat
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.AppointmentsViewModel
import com.urbanblade.mobile.ui.theme.UrbanSpace
import com.urbanblade.mobile.ui.components.UrbanTextButton


private enum class PaymentMethodChoice { TARJETA, TRANSFERENCIA }

/**
 * Checkout de cita: tarjeta (Stripe PaymentSheet) o transferencia (subir
 * comprobante) + propina opcional -- sin efectivo, que es inherentemente
 * presencial (lo cobra recepción, no algo que el cliente "pague" desde el
 * teléfono). El monto real SIEMPRE lo calcula barber al crear el intent o
 * al recibir el comprobante; aquí solo se muestra una estimación.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CheckoutSheet(appt: AppointmentRow, vm: AppointmentsViewModel, onDismiss: () -> Unit) {
    val checkoutBusy by vm.checkoutBusy.collectAsState()
    val uploadingReceipt by vm.uploadingReceipt.collectAsState()
    val clientSecret by vm.stripeClientSecret.collectAsState()
    val confirmingPayment by vm.confirmingPayment.collectAsState()
    val paymentNotice by vm.paymentNotice.collectAsState()
    val cardAvailable = remember { isStripeConfigured() }
    val response by vm.data.collectAsState()
    val error by vm.error.collectAsState()
    val transferInfo by vm.transferInfo.collectAsState()
    val transferInfoError by vm.transferInfoError.collectAsState()
    val context = LocalContext.current

    var method by remember { mutableStateOf<PaymentMethodChoice?>(null) }
    var tipOption by remember { mutableStateOf(0) }
    var customTip by remember { mutableStateOf("") }
    var giftCardCode by remember { mutableStateOf("") }
    var puntos by remember { mutableStateOf("") }
    var showExtras by remember { mutableStateOf(false) }
    var receiptUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val savedCards by vm.savedCards.collectAsState()
    val savedCardConfirm by vm.savedCardConfirm.collectAsState()
    // Tarjeta al centro del carrusel; null = «Otra tarjeta» (se paga con el PaymentSheet de Stripe).
    var selectedCardId by remember { mutableStateOf<String?>(null) }

    val basePrice = appt.precioCobrado ?: appt.service?.precio ?: 0.0
    val tipAmount = when (tipOption) {
        1 -> (basePrice * 0.10).let { Math.round(it * 100) / 100.0 }
        2 -> (basePrice * 0.15).let { Math.round(it * 100) / 100.0 }
        3 -> customTip.toDoubleOrNull() ?: 0.0
        else -> 0.0
    }

    LaunchedEffect(method, appt.id, tipAmount) {
        if (method == PaymentMethodChoice.TRANSFERENCIA) vm.loadTransferInfo(appt.id, tipAmount)
    }
    val validTransferQuote = transferInfo?.cotizacion?.matches(appointmentId = appt.id, tip = tipAmount) == true
    val displayTransferInfo = transferInfo?.let { if (validTransferQuote) it else it.copy(cotizacion = null) }

    // La cita puede haberse marcado como pagada mientras la hoja sigue
    // abierta (confirmAppointmentPayment refresca la lista)
    val justPaid = response.data.firstOrNull { it.id == appt.id }?.hasPayment == true

    val pickReceipt = rememberSingleImagePicker { receiptUri = it }

    val presentPaymentSheet = rememberUrbanPaymentSheet(onAuthorizationDenied = { vm.onStripeCanceled() }, onPreparationFailed = { vm.onStripeFailed(it.localizedMessage) }) { result ->
        when (result) {
            is PaymentSheetResult.Completed -> {
                vm.clearStripeClientSecret()
                vm.confirmAppointmentPayment(appt.id)
            }
            is PaymentSheetResult.Canceled -> vm.onStripeCanceled()
            is PaymentSheetResult.Failed -> vm.onStripeFailed(result.error.localizedMessage)
        }
    }

    LaunchedEffect(clientSecret) {
        clientSecret?.let { presentPaymentSheet(it) }
    }

    // Pago con una tarjeta guardada: Stripe lo confirma directo con esa tarjeta, sin pedir datos.
    val paymentLauncher = rememberPaymentLauncher(BuildConfig.STRIPE_PUBLISHABLE_KEY) { result ->
        when (result) {
            is PaymentResult.Completed -> vm.confirmAppointmentPayment(appt.id)
            is PaymentResult.Canceled -> vm.onStripeCanceled()
            is PaymentResult.Failed -> vm.onStripeFailed(result.throwable.localizedMessage)
        }
    }
    val authorizeSavedPayment = com.urbanblade.mobile.core.payment.rememberPaymentAuthorization { vm.onStripeCanceled() }
    LaunchedEffect(savedCardConfirm) {
        val confirm = savedCardConfirm ?: return@LaunchedEffect
        vm.clearSavedCardConfirm()
        confirm.paymentMethodId?.let { id -> authorizeSavedPayment { paymentLauncher.confirm(ConfirmPaymentIntentParams.createWithPaymentMethodId(id, confirm.clientSecret)) } }
    }
    LaunchedEffect(cardAvailable) { if (cardAvailable) vm.loadSavedCards() }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = UrbanColors.Card) {
        if (justPaid) {
            Box(Modifier.fillMaxWidth().padding(bottom = 32.dp), contentAlignment = Alignment.Center) {
                com.urbanblade.mobile.ui.components.UrbanReceiptAnimation(
                    serviceName = appt.service?.nombre ?: "Servicio",
                    total = basePrice + tipAmount,
                    date = "${UrbanFormat.date(appt.fecha)} • ${UrbanFormat.time(appt.horaInicio)}",
                    onAnimationEnd = onDismiss
                )
            }
        } else {
            // Con carrusel de tarjetas el contenido supera la altura de pantallas chicas: la hoja se desplaza
            // para que el botón de pagar siempre se pueda alcanzar.
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = UrbanSpace.Gutter, vertical = 8.dp)) {
            UrbanSectionTitle("Pagar cita", appt.service?.nombre)
            Spacer(Modifier.height(14.dp))

            val transferQuote = displayTransferInfo?.cotizacion.takeIf { method == PaymentMethodChoice.TRANSFERENCIA }
            UrbanKeyValue(if (transferQuote != null) "Servicio con descuentos" else "Servicio", UrbanFormat.money(transferQuote?.serviceAmount ?: basePrice))
            if (tipAmount > 0) UrbanKeyValue("Propina", "${UrbanFormat.money(tipAmount)}")
            UrbanKeyValue(if (transferQuote != null) "Total a transferir" else "Total estimado", UrbanFormat.money(transferQuote?.total ?: (basePrice + tipAmount)))
            Spacer(Modifier.height(4.dp))
            Text(
                if (transferQuote != null) "Importe consultado con tus descuentos actuales. El comprobante queda pendiente de revisión." else "El monto final (con descuentos de nivel/membresía o tarjeta regalo aplicados) lo confirma UrbanBlade al procesar el pago.",
                style = MaterialTheme.typography.bodySmall,
                color = UrbanColors.Muted
            )

            Spacer(Modifier.height(16.dp))
            UrbanFieldLabel("Propina (opcional)")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Sin propina" to 0, "10%" to 1, "15%" to 2, "Otro monto" to 3).forEach { (label, value) ->
                    FilterChip(selected = tipOption == value, onClick = { tipOption = value }, label = { Text(label) })
                }
            }
            if (tipOption == 3) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    customTip, { customTip = it.filter { c -> c.isDigit() || c == '.' } },
                    placeholder = { Text("Monto en pesos") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
            }

            Spacer(Modifier.height(8.dp))
            // Gift card y puntos son poco frecuentes: se esconden para que el método de pago quede a la vista.
            val extrasOpen = showExtras || giftCardCode.isNotBlank() || puntos.isNotBlank()
            if (method == PaymentMethodChoice.TRANSFERENCIA && (giftCardCode.isNotBlank() || puntos.isNotBlank())) {
                Text("Para usar puntos o tarjeta regalo, selecciona Tarjeta. La transferencia no incluye esos beneficios.", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
            }
            if (method != PaymentMethodChoice.TRANSFERENCIA) UrbanTextButton(onClick = { showExtras = !extrasOpen }) {
                Text(if (extrasOpen) "Ocultar tarjeta regalo y puntos" else "¿Tienes tarjeta regalo o puntos?", color = UrbanColors.GoldText)
                Icon(if (extrasOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = UrbanColors.Gold)
            }
            if (extrasOpen && method != PaymentMethodChoice.TRANSFERENCIA) {
                Spacer(Modifier.height(16.dp))
                UrbanFieldLabel("Código de tarjeta regalo (opcional)")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    giftCardCode, { giftCardCode = it },
                    placeholder = { Text("Ej. UB-XXXXXX") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )

                Spacer(Modifier.height(16.dp))
                UrbanFieldLabel("Puntos a canjear (opcional)")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    puntos, { puntos = it.filter(Char::isDigit) },
                    placeholder = { Text("0") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )

            }

            Spacer(Modifier.height(20.dp))
            UrbanFieldLabel("Método de pago")
            Spacer(Modifier.height(8.dp))
            // Mismos mosaicos que al reservar (kit): antes eran dos botones sin estado seleccionado.
            UrbanChoiceTiles(
                options = listOf(
                    UrbanChoice(PaymentMethodChoice.TARJETA, "Tarjeta", "Paga ahora", Icons.Default.CreditCard),
                    UrbanChoice(PaymentMethodChoice.TRANSFERENCIA, "Transferencia", "SPEI", Icons.Default.AccountBalance)
                ),
                selected = method,
                onSelect = {
                    method = it
                    vm.clearError()
                }
            )
            Text(
                "¿Prefieres pagar en efectivo? Puedes hacerlo directamente en la barbería.",
                style = MaterialTheme.typography.bodySmall,
                color = UrbanColors.Muted,
                modifier = Modifier.padding(top = 6.dp)
            )

            when (method) {
                PaymentMethodChoice.TARJETA -> {
                    Spacer(Modifier.height(16.dp))
                    if (!cardAvailable) {
                        UrbanInfoBanner("El pago con tarjeta no está disponible en esta versión de la app. Paga por transferencia o en la barbería.")
                        Spacer(Modifier.height(10.dp))
                    }
                    // Antes era solo un botón al PaymentSheet: ahora las tarjetas guardadas se eligen deslizando.
                    val chosenCard = savedCards.firstOrNull { it.id == selectedCardId }
                    if (cardAvailable && savedCards.isNotEmpty()) {
                        UrbanFieldLabel("Tus tarjetas")
                        Spacer(Modifier.height(10.dp))
                        UrbanCreditCardCarousel(
                            cards = savedCards,
                            selectedCardId = selectedCardId,
                            onSelect = { selectedCardId = it }
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                    UrbanPrimaryButton(
                        text = chosenCard?.let { "Pagar con •••• ${it.last4}" } ?: "Proceder al pago",
                        onClick = {
                            vm.startStripeCheckout(appt.id, puntos.toIntOrNull() ?: 0, giftCardCode, tipAmount, chosenCard?.id)
                        },
                        enabled = cardAvailable && !confirmingPayment,
                        loading = checkoutBusy,
                        icon = Icons.Default.CreditCard,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                PaymentMethodChoice.TRANSFERENCIA -> {
                    Spacer(Modifier.height(16.dp))
                    if (transferInfoError != null) {
                        UrbanErrorBanner(transferInfoError!!)
                        UrbanTextButton(onClick = { vm.loadTransferInfo(appt.id, tipAmount) }) { Text("Reintentar cotización") }
                    } else {
                        TransferAccount(displayTransferInfo, basePrice + tipAmount)
                        if (transferInfo != null && !validTransferQuote) UrbanInfoBanner("El servidor todavía no confirmó el importe. Reintenta la cotización o elige otro método.")
                    }
                    Spacer(Modifier.height(14.dp))
                    UrbanOutlineButton(
                        text = if (receiptUri != null) "Comprobante seleccionado" else "Elegir comprobante",
                        onClick = pickReceipt,
                        icon = Icons.Default.Image,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    UrbanPrimaryButton(
                        text = "Subir comprobante",
                        onClick = {
                            val uri = receiptUri
                            val code = appt.code
                            if (uri != null && code != null) {
                                vm.uploadPaymentReceipt(context, code, tipAmount, uri, onDismiss)
                            }
                        },
                        enabled = receiptUri != null && validTransferQuote && transferInfo?.configurado == true,
                        loading = uploadingReceipt,
                        icon = Icons.Default.Upload,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                null -> {}
            }

            if (confirmingPayment) {
                Spacer(Modifier.height(14.dp))
                LinearProgressIndicator(Modifier.fillMaxWidth(), color = UrbanColors.Gold)
                Spacer(Modifier.height(8.dp))
                UrbanInfoBanner("Confirmando tu pago con UrbanBlade…", Icons.Default.HourglassTop)
            }

            paymentNotice?.let { Spacer(Modifier.height(12.dp)); UrbanInfoBanner(it) }

            error?.let { Spacer(Modifier.height(12.dp)); UrbanErrorBanner(it) }
            Spacer(Modifier.height(24.dp))
            } // fin del Column
        } // fin del else
    } // fin del ModalBottomSheet
}
