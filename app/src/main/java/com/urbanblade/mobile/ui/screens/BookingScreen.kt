package com.urbanblade.mobile.ui.screens

import androidx.compose.ui.draw.clipToBounds
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.payments.paymentlauncher.rememberPaymentLauncher
import com.urbanblade.mobile.BuildConfig
import com.urbanblade.mobile.core.payment.rememberStripeReady
import com.urbanblade.mobile.core.validation.FormLimits
import com.urbanblade.mobile.ui.viewmodel.BookingPayMethod
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.components.UrbanFormat
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.BookingViewModel
import androidx.compose.runtime.saveable.rememberSaveable
import com.urbanblade.mobile.ui.theme.UrbanSpace
import com.urbanblade.mobile.ui.components.UrbanTextButton


internal enum class BookingStep(val label: String, val title: String, val hint: String) {
    SERVICE("Servicio", "Elige tu servicio", "Precio y duración reales de UrbanBlade."),
    SCHEDULE("Horario", "Barbero, día y hora", "Solo ves horarios libres de verdad."),
    EXTRAS("Extras", "¿Algo más para tu visita?", "Productos opcionales y notas para tu barbero."),
    PAY("Pago", "Revisa y reserva", "Elige cómo pagar tu servicio.")
}

/**
 * Reserva en cuatro pasos cortos (propuesta A, 25-sep): servicio -> barbero, día y hora
 * (horarios reales; con un solo barbero se asigna solo) -> extras (productos opcionales que
 * se pagan en el salón, y notas) -> pago. Una barra fija muestra el total de la visita. `initialServiceId`/
 * `initialBarberId` llegan de un tap en el catálogo (autenticado, por
 * argumento de ruta) o de PendingBooking (invitado que acaba de iniciar
 * sesión) -- ver UrbanBladeRoot.kt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(
    initialServiceId: String? = null,
    initialBarberId: String? = null,
    onBack: () -> Unit,
    onCreated: () -> Unit,
    vm: BookingViewModel = viewModel()
) {
    val services by vm.services.collectAsState()
    val barbers by vm.barbers.collectAsState()
    val slots by vm.slots.collectAsState()
    val slotsLoading by vm.slotsLoading.collectAsState()
    val slotsError by vm.slotsError.collectAsState()
    val slotsFor by vm.slotsFor.collectAsState()
    val catalogLoading by vm.catalogLoading.collectAsState()
    val catalogError by vm.catalogError.collectAsState()
    val busy by vm.busy.collectAsState()
    val error by vm.error.collectAsState()
    val waitlistJoined by vm.waitlistJoined.collectAsState()
    val products by vm.products.collectAsState()
    val productsNote by vm.productsNote.collectAsState()
    // rememberSaveable: si Android recrea la pantalla (volver del navegador o de Stripe, rotar,
    // cambiar tema o tamaño de letra) el cliente sigue en el mismo paso con lo que ya eligió.
    // Productos de la visita: id -> cantidad (se pagan en el salón, como en la web).
    var cart by rememberSaveable { mutableStateOf(mapOf<String, Int>()) }

    // Si se llega con el servicio ya elegido (catálogo, Bladebot, "Reservar de nuevo"), se empieza
    // en el horario: antes volvía a pedir el servicio y había que tocarlo dos veces.
    val firstStep = if (initialServiceId.isNullOrBlank()) BookingStep.SERVICE else BookingStep.SCHEDULE
    var step by rememberSaveable { mutableStateOf(firstStep) }
    var firstVisitedStep by rememberSaveable { mutableStateOf(firstStep) }
    fun previousStep() {
        if (busy) return
        step = BookingStep.entries[step.ordinal - 1]
        if (step.ordinal < firstVisitedStep.ordinal) firstVisitedStep = step
    }
    // Embudo de reserva en Analytics: de dónde llegó y hasta qué paso avanza (sin datos personales).
    LaunchedEffect(Unit) {
        com.urbanblade.mobile.core.analytics.UrbanAnalytics.log(
            "reserva_inicio",
            "origen" to when {
                !initialServiceId.isNullOrBlank() -> "servicio_elegido"
                !initialBarberId.isNullOrBlank() -> "barbero_elegido"
                else -> "directo"
            }
        )
    }
    LaunchedEffect(step) { com.urbanblade.mobile.core.analytics.UrbanAnalytics.log("reserva_paso", "paso" to step.name.lowercase()) }
    // Remote Config: el pago con tarjeta se puede apagar desde Firebase (p. ej. si Stripe falla).
    val cardEnabledRemotely by com.urbanblade.mobile.core.config.UrbanRemoteConfig.cardPayments.collectAsState()
    var serviceId by rememberSaveable { mutableStateOf(initialServiceId.orEmpty()) }
    var barberId by rememberSaveable { mutableStateOf(initialBarberId.orEmpty()) }
    var date by rememberSaveable { mutableStateOf(vm.defaultDate()) }
    var time by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var slotsRequested by rememberSaveable { mutableStateOf(false) }
    var confirmed by rememberSaveable { mutableStateOf(false) }
    val reservationCompleted by vm.reservationCompleted.collectAsState()
    // El ViewModel sobrevive a una recreación durante Stripe; el callback anterior puede
    // pertenecer a la composición destruida. La nueva pantalla observa el resultado vigente.
    LaunchedEffect(reservationCompleted) {
        if (reservationCompleted) confirmed = true
    }
    // El cliente acepta el cargo por inasistencia en el último paso (el backend guarda cuándo lo aceptó).
    var acceptsNoShowFee by rememberSaveable { mutableStateOf(false) }
    val pay = rememberBookingPaymentState()
    var localError by remember { mutableStateOf<String?>(null) }
    val paymentNote by vm.paymentNote.collectAsState()
    val cardConfirm by vm.cardConfirm.collectAsState()
    val transferInfo by vm.transferInfo.collectAsState()
    val transferInfoError by vm.transferInfoError.collectAsState()
    val savedCards by vm.savedCards.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val cardAvailable = rememberStripeReady()
    val pickReceipt = rememberSingleImagePicker { pay.receiptUri = it }
    val paymentLauncher = rememberPaymentLauncher(BuildConfig.STRIPE_PUBLISHABLE_KEY) { result ->
        when (result) {
            is PaymentResult.Completed -> vm.onCardResult(true, false, null)
            is PaymentResult.Canceled -> vm.onCardResult(false, true, null)
            is PaymentResult.Failed -> vm.onCardResult(false, false, result.throwable.localizedMessage)
        }
    }
    // Con la cita ya creada, confirma el cobro con la tarjeta guardada o con lo que el cliente escribió.
    val authorizePayment = com.urbanblade.mobile.core.payment.rememberPaymentAuthorization { vm.onCardResult(false, true, null) }
    LaunchedEffect(cardConfirm) {
        val confirm = cardConfirm ?: return@LaunchedEffect
        authorizePayment {
            val savedId = confirm.paymentMethodId
            if (savedId != null) {
                paymentLauncher.confirm(ConfirmPaymentIntentParams.createWithPaymentMethodId(savedId, confirm.clientSecret))
            } else {
                val params = pay.paymentMethodParams()
                if (params != null) {
                    paymentLauncher.confirm(ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(params, confirm.clientSecret))
                } else {
                    vm.onCardResult(false, false, "no se pudieron leer los datos de la tarjeta")
                }
            }
        }
    }

    LaunchedEffect(Unit) { vm.loadPaymentOptions() }
    LaunchedEffect(Unit) { vm.loadCatalog() }
    // Combinación para la que se eligió la hora: si la pantalla se recrea con la misma, la hora se conserva.
    var slotsKey by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(barberId, serviceId, date) {
        val key = "$barberId|$serviceId|$date"
        if (key != slotsKey) {
            time = ""
            slotsKey = key
        }
        slotsRequested = false
        if (barberId.isNotBlank() && serviceId.isNotBlank() && date.isNotBlank()) {
            vm.loadSlots(barberId, serviceId, date)
            slotsRequested = true
        }
    }

    val selectedService = services.firstOrNull { it.id == serviceId }
    val transferTip = pay.tipFor(selectedService?.precio ?: 0.0)
    LaunchedEffect(pay.method, serviceId, transferTip) {
        if (pay.method == BookingPayMethod.TRANSFERENCIA && serviceId.isNotBlank()) vm.loadTransferQuote(serviceId, transferTip)
    }
    val validTransferQuote = transferInfo?.cotizacion?.matches(serviceId = serviceId, tip = transferTip) == true

    // Si llega desde el Muro de Inspiración trae el id del usuario del barbero: se traduce al de su perfil.
    LaunchedEffect(barbers) {
        if (barberId.isBlank()) barbers.firstOrNull { it.esFavorito }?.let { barberId = it.id }
        resolveBarberId(barbers, barberId)?.let { resolved -> if (resolved != barberId) barberId = resolved }
    }
    val selectedBarber = barbers.firstOrNull { it.id == barberId }
    // Con un solo barbero no hay nada que elegir: se asigna solo.
    LaunchedEffect(barbers) {
        if (barberId.isBlank() && barbers.size == 1) barberId = barbers.first().id
    }
    val cartLines = remember(products, cart) { products.mapNotNull { p -> cart[p.id]?.takeIf { it > 0 }?.let { p to it } } }
    val productsTotal = remember(cartLines) { cartLines.sumOf { (p, qty) -> p.precioVenta * qty } }
    val servicePrice = selectedService?.precio ?: 0.0
    val tip = if (step == BookingStep.PAY) pay.tipFor(servicePrice) else 0.0
    // "Reservar" queda donde estaba "Continuar": se activa un momento después de llegar a Pago para
    // que un doble toque en "Continuar" no confirme la reserva sin que el cliente la haya revisado.
    var reserveArmed by remember { mutableStateOf(false) }
    LaunchedEffect(step) {
        reserveArmed = false
        if (step == BookingStep.PAY) {
            kotlinx.coroutines.delay(700)
            reserveArmed = true
        }
    }
    val visitTotal = servicePrice + productsTotal + tip

    val validSlot = selectedService != null && selectedBarber != null &&
        slotsFor == "$barberId|$serviceId|$date" && !slotsLoading && slotsError == null &&
        time.isNotBlank() && slots.any { it.time == time }
    val canAdvance = !catalogLoading && catalogError == null && when (step) {
        BookingStep.SERVICE -> selectedService != null
        else -> validSlot
    }
    val nextActionHint = when {
        busy -> "Procesando tu solicitud…"
        catalogLoading -> "Consultando servicios y barberos…"
        catalogError != null -> "Reintenta la consulta para continuar."
        selectedService == null -> "Elige un servicio para ver los horarios."
        step != BookingStep.SERVICE && selectedBarber == null -> "Elige el barbero de tu visita."
        step != BookingStep.SERVICE && slotsLoading -> "Consultando horarios disponibles…"
        step != BookingStep.SERVICE && slotsError != null -> "Reintenta la consulta de horarios."
        step != BookingStep.SERVICE && !validSlot -> "Elige un horario disponible para continuar."
        step == BookingStep.PAY && !acceptsNoShowFee -> "Revisa y acepta la política de inasistencia para reservar."
        step == BookingStep.PAY && !reserveArmed -> "Preparando la confirmación…"
        // Sin bloqueo no hay nada que avisar: el subtítulo del paso y el botón ya dicen qué sigue.
        else -> null
    }
    // El "atrás" del teléfono regresa un paso; desde el paso en que se entró sale de la reserva.
    androidx.activity.compose.BackHandler(enabled = !confirmed && step.ordinal > firstVisitedStep.ordinal) {
        previousStep()
    }

    // Momento de éxito antes de salir del flujo (antes se saltaba de pantalla sin confirmar nada).
    if (confirmed) {
        UrbanSuccessScreen(
            title = "¡Cita reservada!",
            message = listOfNotNull(paymentNote ?: "Te avisaremos cuando el barbero la confirme.", productsNote).joinToString("\n\n"),
            details = listOfNotNull(
                selectedService?.let { "Servicio" to it.nombre },
                selectedBarber?.user?.name?.let { "Barbero" to it },
                cartLines.takeIf { it.isNotEmpty() }?.let { lines -> "Productos" to lines.joinToString(", ") { (p, q) -> if (q > 1) "${p.nombre} ×$q" else p.nombre } },
                "Fecha" to UrbanFormat.date(date),
                "Hora" to UrbanFormat.time(time)
            ),
            primaryText = "Ver mis citas",
            onPrimary = onCreated
        )
        return
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = { UrbanTopBar(title = "", onBack = {
            if (step.ordinal > firstVisitedStep.ordinal) previousStep()
            else onBack()
        }) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
            key(step) {
                Column(Modifier.weight(1f).fillMaxWidth().clipToBounds().verticalScroll(rememberScrollState())) {
                    BookingHeader(
                        step = step,
                        // Lo que ya eligió, visible en los pasos siguientes para no perder el hilo. En Pago no
                        // hace falta: la tarjeta "Tu cita" ya lo muestra completo.
                        summary = if (step == BookingStep.PAY) null else listOfNotNull(
                            selectedService?.takeIf { step != BookingStep.SERVICE }?.nombre,
                            selectedBarber?.takeIf { step.ordinal > BookingStep.SCHEDULE.ordinal }?.user?.name,
                            time.takeIf { it.isNotBlank() && step.ordinal > BookingStep.SCHEDULE.ordinal }?.let { "${UrbanFormat.dateShort(date)} · ${UrbanFormat.time(it)}" }
                        ).joinToString("  ·  ").ifBlank { null }
                    )
                    Spacer(Modifier.height(8.dp))

                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = UrbanSpace.Gutter)
                    ) {
                        nextActionHint?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp))
                        }
                        if (step == BookingStep.SCHEDULE && slotsLoading) {
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                        }
                        if (step == BookingStep.SCHEDULE) slotsError?.let {
                            UrbanErrorBanner(it)
                            UrbanOutlineButton("Reintentar horarios", { vm.loadSlots(barberId, serviceId, date) },
                                enabled = !slotsLoading, modifier = Modifier.fillMaxWidth())
                        }
                        if (step.ordinal > BookingStep.SCHEDULE.ordinal && !validSlot) {
                            UrbanInfoBanner(if (slotsLoading) "Volviendo a comprobar tu horario…" else "Revisa el horario antes de continuar.", Icons.Default.Schedule)
                            UrbanOutlineButton("Revisar horario", { step = BookingStep.SCHEDULE }, modifier = Modifier.fillMaxWidth())
                        }
                        when {
                            catalogLoading && services.isEmpty() -> UrbanSkeletonList(3)
                            catalogError != null -> UrbanMascotState(
                                UrbanStateKind.ERROR, "No pudimos cargar los servicios y barberos", catalogError,
                                "Reintentar", onAction = { vm.loadCatalog() }
                            )
                            else -> when (step) {
                                BookingStep.SERVICE -> Column {
                                    if (initialBarberId != null && selectedBarber != null && barbers.size > 1) {
                                        UrbanInfoBanner(
                                            "Reservando con ${selectedBarber.user?.name ?: "tu barbero"}. Puedes cambiarlo en el paso 2.",
                                            Icons.Default.Person
                                        )
                                        Spacer(Modifier.height(12.dp))
                                    }
                                    ServiceStep(services, serviceId) { serviceId = it }
                                }
                                BookingStep.SCHEDULE -> CalendarStep(
                                    header = { BarberPicker(barbers, barberId) { barberId = it } },
                                    date = date,
                                    onDateChange = { date = it },
                                    slots = slots,
                                    time = time,
                                    onTimeChange = { time = it },
                                    showEmptyState = slotsRequested && !slotsLoading && slotsError == null && slots.isEmpty(),
                                    loading = slotsLoading,
                                    waitlistJoined = waitlistJoined,
                                    onJoinWaitlist = { vm.joinWaitlist(barberId, serviceId, date) }
                                )
                                BookingStep.EXTRAS -> ExtrasStep(
                                    products = products,
                                    cart = cart,
                                    onQuantity = { id, qty -> cart = if (qty <= 0) cart - id else cart + (id to qty) },
                                    notes = notes,
                                    onNotesChange = { notes = it.take(FormLimits.NOTES_MAX) }
                                )
                                BookingStep.PAY -> ReviewStep(
                                    service = selectedService,
                                    barber = selectedBarber,
                                    date = date,
                                    time = time,
                                    extras = cartLines,
                                    notes = notes
                                ) {
                                    BookingPaymentSection(
                                        state = pay,
                                        servicePrice = selectedService?.precio ?: 0.0,
                                        transferInfo = transferInfo?.let { if (validTransferQuote) it else it.copy(cotizacion = null) },
                                        transferError = transferInfoError,
                                        savedCards = savedCards,
                                        cardAvailable = cardAvailable && cardEnabledRemotely,
                                        testMode = BuildConfig.STRIPE_PUBLISHABLE_KEY.startsWith("pk_test_"),
                                        onPickReceipt = pickReceipt
                                    )
                                }
                            }
                        }
                    }

                    if (pay.method == BookingPayMethod.TRANSFERENCIA && transferInfoError != null) {
                        UrbanTextButton(onClick = { vm.loadTransferQuote(serviceId, transferTip) }) { Text("Reintentar cotización") }
                    }
                    (localError ?: error)?.let {
                        Box(Modifier.padding(horizontal = UrbanSpace.Gutter)) { UrbanErrorBanner(it) }
                        Spacer(Modifier.height(8.dp))
                    }

                    if (step == BookingStep.PAY) {
                        Row(
                            Modifier.fillMaxWidth().clickable(enabled = !busy) { acceptsNoShowFee = !acceptsNoShowFee }
                                .padding(horizontal = UrbanSpace.Gutter, vertical = 6.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Checkbox(checked = acceptsNoShowFee, onCheckedChange = { acceptsNoShowFee = it }, enabled = !busy)
                            Text(
                                "Acepto el cargo por inasistencia: si no llego a mi cita sin cancelar a tiempo, se cobra un porcentaje del servicio a mi tarjeta guardada o queda como adeudo por pagar en la barbería para volver a reservar.",
                                style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted,
                                modifier = Modifier.weight(1f).padding(top = 12.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }

            // Barra fija: el total de la visita se va sumando conforme eliges.
            if (selectedService != null) {
                HorizontalDivider(color = UrbanColors.Line)
                Row(
                    Modifier.fillMaxWidth().padding(start = UrbanSpace.Gutter, end = UrbanSpace.Gutter, top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Total estimado", style = MaterialTheme.typography.labelMedium, color = UrbanColors.Muted)
                        if (productsTotal > 0 || tip > 0) {
                            Text(
                                listOfNotNull(
                                    "Servicio ${UrbanFormat.moneyShort(servicePrice)}",
                                    productsTotal.takeIf { it > 0 }?.let { "productos ${UrbanFormat.moneyShort(it)}" },
                                    tip.takeIf { it > 0 }?.let { "propina ${UrbanFormat.moneyShort(it)}" }
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = UrbanColors.Muted
                            )
                        }
                    }
                    Text("${UrbanFormat.moneyShort(visitTotal)}", style = MaterialTheme.typography.titleLarge, color = UrbanColors.GoldText)
                }
            }
            FlowRow(
                Modifier.fillMaxWidth().padding(horizontal = UrbanSpace.Gutter, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = if (androidx.compose.ui.platform.LocalDensity.current.fontScale >= 1.3f) 1 else 2
            ) {
                if (step != BookingStep.SERVICE) {
                    UrbanOutlineButton(
                        text = if (step == BookingStep.SCHEDULE) "Cambiar servicio" else "Atrás",
                        onClick = { previousStep() },
                        enabled = !busy,
                        icon = Icons.Default.ArrowBack,
                        modifier = Modifier.weight(1f)
                    )
                }
                UrbanPrimaryButton(
                    text = when (step) {
                        BookingStep.SERVICE -> "Elegir horario"
                        BookingStep.SCHEDULE -> "Elegir extras"
                        BookingStep.EXTRAS -> if (cartLines.isEmpty() && notes.isBlank()) "Continuar sin extras" else "Revisar reserva"
                        BookingStep.PAY -> "Reservar cita"
                    },
                    onClick = {
                        if (step == BookingStep.PAY) {
                            localError = null
                            val savedId = pay.savedCardToUse(savedCards)
                            if (pay.method == BookingPayMethod.TRANSFERENCIA && pay.receiptUri != null && !validTransferQuote) {
                                localError = "Consulta el importe de transferencia antes de enviar el comprobante."
                            } else if (pay.method == BookingPayMethod.TARJETA && savedId == null && pay.newCardProblem() != null) {
                                localError = pay.newCardProblem()
                                // Lleva al formulario para que se vea qué falta (antes quedaba abajo, fuera de la vista).
                                pay.cardAttention++
                            } else {
                                vm.reserve(
                                    context, barberId, serviceId, date, time, notes,
                                    pay.method, pay.tipFor(selectedService?.precio ?: 0.0), pay.receiptUri,
                                    savedCardId = savedId,
                                    saveCard = pay.saveCard,
                                    productos = cartLines.map { (p, qty) -> com.urbanblade.mobile.data.model.OrderItemRequest(p.id, qty) },
                                    aceptaCargo = acceptsNoShowFee
                                ) {
                                    confirmed = true
                                    com.urbanblade.mobile.core.analytics.UrbanAnalytics.log(
                                        "reserva_confirmada",
                                        "metodo_pago" to pay.method.name.lowercase(),
                                        "value" to (selectedService?.precio ?: 0.0),
                                        "currency" to "MXN",
                                        "con_productos" to cartLines.isNotEmpty()
                                    )
                                }
                            }
                        } else {
                            step = BookingStep.entries[step.ordinal + 1]
                        }
                    },
                    enabled = canAdvance && (step != BookingStep.PAY || (reserveArmed && acceptsNoShowFee)),
                    loading = busy && step == BookingStep.PAY,
                    icon = if (step == BookingStep.PAY) Icons.Default.CheckCircle else Icons.Default.ArrowForward,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
