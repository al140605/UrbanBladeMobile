package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.urbanblade.mobile.core.payment.rememberUrbanPaymentSheet
import com.urbanblade.mobile.data.model.AppointmentRow
import com.urbanblade.mobile.data.model.AuthUser
import com.urbanblade.mobile.data.model.SlotItem
import com.urbanblade.mobile.data.model.WaitlistEntry
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.AppointmentsViewModel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentsScreen(user: AuthUser, onBook: () -> Unit, vm: AppointmentsViewModel = viewModel()) {
    val response by vm.data.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val waitlistEntries by vm.waitlistEntries.collectAsState()
    var confirmCancel by remember { mutableStateOf<AppointmentRow?>(null) }
    var rescheduling by remember { mutableStateOf<AppointmentRow?>(null) }
    var checkingOut by remember { mutableStateOf<AppointmentRow?>(null) }
    val canBook = user.roles.any { it in listOf("cliente", "administrador", "recepcionista") }
    val isClient = user.roles.contains("cliente")

    LaunchedEffect(Unit) {
        vm.load()
        if (isClient) vm.loadWaitlist()
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            if (canBook) {
                ExtendedFloatingActionButton(
                    onClick = onBook,
                    containerColor = UrbanColors.Gold,
                    contentColor = UrbanColors.OnGold,
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("Nueva cita", fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                UrbanPageHeader(
                    title = "Citas",
                    subtitle = "Tu agenda, ordenada y siempre conectada.",
                    eyebrow = "Agenda"
                )
            }

            // Si la carga falló y no hay nada que mostrar, no se pintan contadores en 0 ni
            // "agenda libre": sería afirmar algo que no sabemos. Solo el error y "Reintentar".
            val failed = error != null && response.data.isEmpty()

            if (!failed) item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    UrbanMetricCard(
                        label = "Próximas",
                        value = response.stats.proximas.toString(),
                        icon = Icons.Default.Upcoming,
                        modifier = Modifier.weight(1f)
                    )
                    UrbanMetricCard(
                        label = "Completadas",
                        value = response.stats.completadas.toString(),
                        icon = Icons.Default.TaskAlt,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (loading) item { UrbanSkeletonList(3) }
            error?.let { item { UrbanErrorBanner(it) } }
            if (failed && !loading) {
                item {
                    UrbanOutlineButton(
                        text = "Reintentar",
                        onClick = { vm.load() },
                        icon = Icons.Default.Refresh,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (!loading && !failed && response.data.isEmpty()) {
                item {
                    UrbanPremiumCard(Modifier.fillMaxWidth()) {
                        UrbanEmptyState(
                            title = "Tu agenda está libre",
                            subtitle = "Cuando tengas una cita aparecerá aquí.",
                            icon = Icons.Default.EventAvailable,
                            actionLabel = if (canBook) "Reservar ahora" else null,
                            onAction = if (canBook) onBook else null
                        )
                    }
                }
            }

            if (response.data.isNotEmpty()) {
                item { UrbanSectionTitle("Tu agenda", UrbanFormat.count(response.stats.total, "cita registrada", "citas registradas")) }
            }

            items(response.data, key = { it.id }) { appt ->
                val manageable = appt.estado in listOf("pendiente", "confirmada") && appt.code != null
                val payable = isClient && appt.isChargeable && !appt.hasPayment && appt.code != null
                AppointmentCard(
                    appt = appt,
                    onCancel = if (manageable) { { confirmCancel = appt } } else null,
                    onReschedule = if (manageable && isClient) { { rescheduling = appt } } else null,
                    onPay = if (payable) { { checkingOut = appt } } else null
                )
            }

            if (isClient && waitlistEntries.isNotEmpty()) {
                item { UrbanSectionTitle("Mi lista de espera", "Te avisamos si se libera un horario") }
                items(waitlistEntries, key = { it.id }) { entry ->
                    WaitlistCard(entry, onLeave = { vm.leaveWaitlist(entry.id) })
                }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }

    rescheduling?.let { appt ->
        RescheduleSheet(
            appt = appt,
            vm = vm,
            onDismiss = { rescheduling = null }
        )
    }

    checkingOut?.let { appt ->
        CheckoutSheet(
            appt = appt,
            vm = vm,
            onDismiss = { checkingOut = null }
        )
    }

    confirmCancel?.let { appt ->
        AlertDialog(
            onDismissRequest = { confirmCancel = null },
            containerColor = UrbanColors.Card,
            title = { Text("Cancelar cita") },
            text = {
                Text(
                    "¿Quieres cancelar tu cita del ${UrbanFormat.date(appt.fecha)} a las ${UrbanFormat.time(appt.horaInicio)}?",
                    color = UrbanColors.Muted
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.cancel(appt); confirmCancel = null }) {
                    Text("Sí, cancelar", color = UrbanColors.Danger)
                }
            },
            dismissButton = { TextButton(onClick = { confirmCancel = null }) { Text("Volver") } }
        )
    }
}

@Composable
private fun AppointmentCard(
    appt: AppointmentRow,
    onCancel: (() -> Unit)?,
    onReschedule: (() -> Unit)? = null,
    onPay: (() -> Unit)? = null
) {
    UrbanPremiumCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            Surface(
                modifier = Modifier.size(width = 66.dp, height = 72.dp),
                color = UrbanColors.Gold.copy(alpha = 0.09f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, UrbanColors.Gold.copy(alpha = 0.25f))
            ) {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(appt.fecha.takeLast(2), style = MaterialTheme.typography.headlineMedium, color = UrbanColors.Gold)
                    Text(appt.horaInicio.take(5), style = MaterialTheme.typography.labelMedium, color = UrbanColors.Ink)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(appt.service?.nombre ?: "Servicio", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    UrbanStatusPill(appt.estado)
                }
                Text(appt.barber?.user?.name ?: "Barbero por confirmar", style = MaterialTheme.typography.bodyMedium, color = UrbanColors.Muted)
                Text(UrbanFormat.date(appt.fecha), style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                appt.notas?.takeIf { it.isNotBlank() }?.let {
                    Text("“$it”", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Ink)
                }
                if (onPay != null) {
                    Spacer(Modifier.height(6.dp))
                    UrbanPrimaryButton(
                        text = "Pagar cita",
                        onClick = onPay,
                        icon = Icons.Default.Payments,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (onCancel != null || onReschedule != null) {
                    Spacer(Modifier.height(4.dp))
                    Row {
                        if (onReschedule != null) {
                            TextButton(onClick = onReschedule, contentPadding = PaddingValues(0.dp)) {
                                Icon(Icons.Default.EditCalendar, null, modifier = Modifier.size(17.dp), tint = UrbanColors.Gold)
                                Spacer(Modifier.width(5.dp))
                                Text("Reagendar", color = UrbanColors.Gold)
                            }
                            Spacer(Modifier.width(16.dp))
                        }
                        if (onCancel != null) {
                            TextButton(onClick = onCancel, contentPadding = PaddingValues(0.dp)) {
                                Icon(Icons.Default.Close, null, modifier = Modifier.size(17.dp), tint = UrbanColors.Danger)
                                Spacer(Modifier.width(5.dp))
                                Text("Cancelar cita", color = UrbanColors.Danger)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WaitlistCard(entry: WaitlistEntry, onLeave: () -> Unit) {
    UrbanCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(entry.service?.nombre ?: "Servicio", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${entry.barber?.name ?: "Cualquier barbero"} · ${entry.fecha ?: "—"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = UrbanColors.Muted
                )
            }
            UrbanStatusPill(entry.estado)
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onLeave) { Icon(Icons.Default.Close, "Salir de la lista de espera", tint = UrbanColors.Danger) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RescheduleSheet(appt: AppointmentRow, vm: AppointmentsViewModel, onDismiss: () -> Unit) {
    val barbers by vm.barbers.collectAsState()
    val slots by vm.rescheduleSlots.collectAsState()
    val busy by vm.rescheduling.collectAsState()
    val error by vm.error.collectAsState()

    var barberId by remember { mutableStateOf(appt.barber?.id.orEmpty()) }
    var date by remember { mutableStateOf(appt.fecha) }
    var time by remember { mutableStateOf("") }
    val serviceId = appt.service?.id.orEmpty()

    LaunchedEffect(Unit) { vm.loadBarbers() }
    LaunchedEffect(barberId, date) {
        time = ""
        if (barberId.isNotBlank() && date.isNotBlank()) vm.loadRescheduleSlots(barberId, serviceId, date)
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = UrbanColors.Card) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)) {
            UrbanSectionTitle("Reagendar cita", appt.service?.nombre)
            Spacer(Modifier.height(14.dp))

            Text("Profesional", style = MaterialTheme.typography.labelMedium, color = UrbanColors.Muted)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(barbers, key = { it.id }) { barber ->
                    val selected = barber.id == barberId
                    Surface(
                        onClick = { barberId = barber.id },
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) UrbanColors.Gold else UrbanColors.Background,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) UrbanColors.Gold else UrbanColors.Line)
                    ) {
                        Text(
                            barber.user?.name ?: "Barbero",
                            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) UrbanColors.OnGold else UrbanColors.Ink
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Text("Fecha", style = MaterialTheme.typography.labelMedium, color = UrbanColors.Muted)
            Spacer(Modifier.height(8.dp))
            val days = remember { (0..29).map { LocalDate.now().plusDays(it.toLong()) } }
            val selectedDate = remember(date) { runCatching { LocalDate.parse(date) }.getOrNull() }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(days) { day ->
                    val selected = day == selectedDate
                    Surface(
                        onClick = { date = day.toString() },
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) UrbanColors.Gold else UrbanColors.Background,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) UrbanColors.Gold else UrbanColors.Line),
                        modifier = Modifier.width(54.dp)
                    ) {
                        Column(Modifier.padding(vertical = 8.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("es", "MX")).replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) UrbanColors.OnGold else UrbanColors.Muted
                            )
                            Text(day.dayOfMonth.toString(), style = MaterialTheme.typography.titleMedium, color = if (selected) UrbanColors.OnGold else UrbanColors.Ink)
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Text("Horario", style = MaterialTheme.typography.labelMedium, color = UrbanColors.Muted)
            Spacer(Modifier.height(8.dp))
            if (slots.isEmpty()) {
                Text("Sin horarios para este día.", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 180.dp)
                ) {
                    items(slots, key = { it.time }) { slot: SlotItem ->
                        val selected = slot.time == time
                        Surface(
                            onClick = { time = slot.time },
                            shape = MaterialTheme.shapes.medium,
                            color = if (selected) UrbanColors.Gold else UrbanColors.Background,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) UrbanColors.Gold else UrbanColors.Line)
                        ) {
                            Box(Modifier.padding(vertical = 10.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text(slot.label, style = MaterialTheme.typography.labelLarge, color = if (selected) UrbanColors.OnGold else UrbanColors.Ink)
                            }
                        }
                    }
                }
            }

            error?.let { Spacer(Modifier.height(12.dp)); UrbanErrorBanner(it) }

            Spacer(Modifier.height(18.dp))
            UrbanPrimaryButton(
                text = "Confirmar reagendado",
                onClick = {
                    appt.code?.let { code -> vm.reschedule(code, barberId, serviceId, date, time, appt.notas, onDismiss) }
                },
                enabled = barberId.isNotBlank() && date.isNotBlank() && time.isNotBlank(),
                loading = busy,
                icon = Icons.Default.EditCalendar,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

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
private fun CheckoutSheet(appt: AppointmentRow, vm: AppointmentsViewModel, onDismiss: () -> Unit) {
    val checkoutBusy by vm.checkoutBusy.collectAsState()
    val uploadingReceipt by vm.uploadingReceipt.collectAsState()
    val clientSecret by vm.stripeClientSecret.collectAsState()
    val confirmingPayment by vm.confirmingPayment.collectAsState()
    val response by vm.data.collectAsState()
    val error by vm.error.collectAsState()
    val context = LocalContext.current

    var method by remember { mutableStateOf<PaymentMethodChoice?>(null) }
    var tipOption by remember { mutableStateOf(0) }
    var customTip by remember { mutableStateOf("") }
    var giftCardCode by remember { mutableStateOf("") }
    var puntos by remember { mutableStateOf("") }
    var receiptUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val basePrice = appt.precioCobrado ?: appt.service?.precio ?: 0.0
    val tipAmount = when (tipOption) {
        1 -> (basePrice * 0.10).let { Math.round(it * 100) / 100.0 }
        2 -> (basePrice * 0.15).let { Math.round(it * 100) / 100.0 }
        3 -> customTip.toDoubleOrNull() ?: 0.0
        else -> 0.0
    }

    // La cita puede haberse marcado como pagada mientras la hoja sigue
    // abierta (confirmAppointmentPayment refresca la lista) -- cerrar sola.
    val justPaid = response.data.firstOrNull { it.id == appt.id }?.hasPayment == true
    LaunchedEffect(justPaid) { if (justPaid) onDismiss() }

    val pickReceipt = rememberSingleImagePicker { receiptUri = it }

    val presentPaymentSheet = rememberUrbanPaymentSheet { result ->
        when (result) {
            is PaymentSheetResult.Completed -> {
                vm.clearStripeClientSecret()
                vm.confirmAppointmentPayment(appt.id)
            }
            is PaymentSheetResult.Canceled -> vm.clearStripeClientSecret()
            is PaymentSheetResult.Failed -> vm.clearStripeClientSecret()
        }
    }

    LaunchedEffect(clientSecret) {
        clientSecret?.let { presentPaymentSheet(it) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = UrbanColors.Card) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)) {
            UrbanSectionTitle("Pagar cita", appt.service?.nombre)
            Spacer(Modifier.height(14.dp))

            UrbanKeyValue("Servicio", "\$${"%.0f".format(basePrice)}")
            if (tipAmount > 0) UrbanKeyValue("Propina", "\$${"%.0f".format(tipAmount)}")
            Spacer(Modifier.height(4.dp))
            Text(
                "El monto final (con descuentos de nivel/membresía o gift card aplicados) lo confirma UrbanBlade al procesar el pago.",
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

            Spacer(Modifier.height(16.dp))
            UrbanFieldLabel("Código de gift card (opcional)")
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

            Spacer(Modifier.height(20.dp))
            UrbanFieldLabel("Método de pago")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                UrbanOutlineButton(
                    text = "Tarjeta",
                    onClick = { method = PaymentMethodChoice.TARJETA },
                    icon = Icons.Default.CreditCard,
                    modifier = Modifier.weight(1f)
                )
                UrbanOutlineButton(
                    text = "Transferencia",
                    onClick = { method = PaymentMethodChoice.TRANSFERENCIA },
                    icon = Icons.Default.AccountBalance,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                "¿Prefieres pagar en efectivo? Puedes hacerlo directamente en la barbería.",
                style = MaterialTheme.typography.bodySmall,
                color = UrbanColors.Muted,
                modifier = Modifier.padding(top = 6.dp)
            )

            when (method) {
                PaymentMethodChoice.TARJETA -> {
                    Spacer(Modifier.height(16.dp))
                    UrbanPrimaryButton(
                        text = "Continuar con tarjeta",
                        onClick = {
                            vm.startStripeCheckout(appt.id, puntos.toIntOrNull() ?: 0, giftCardCode, tipAmount)
                        },
                        loading = checkoutBusy,
                        icon = Icons.Default.CreditCard,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                PaymentMethodChoice.TRANSFERENCIA -> {
                    Spacer(Modifier.height(16.dp))
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
                        enabled = receiptUri != null,
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

            error?.let { Spacer(Modifier.height(12.dp)); UrbanErrorBanner(it) }
            Spacer(Modifier.height(24.dp))
        }
    }
}
