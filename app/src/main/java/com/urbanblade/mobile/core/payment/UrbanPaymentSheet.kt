package com.urbanblade.mobile.core.payment

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.stripe.android.Stripe
import com.stripe.android.ApiResultCallback
import com.stripe.android.model.PaymentIntent
import com.stripe.android.paymentsheet.PaymentOptionCallback
import com.stripe.android.paymentsheet.PaymentSheetResultCallback
import com.stripe.android.paymentsheet.rememberPaymentSheetFlowController
import com.urbanblade.mobile.ui.components.UrbanPrimaryButton
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.urbanblade.mobile.BuildConfig
import com.urbanblade.mobile.ui.theme.UrbanColors

/**
 * true solo si el build trae una publishable key real de Stripe. Sin ella
 * (valor por defecto "pk_test_PENDIENTE_CONFIGURAR" en app/build.gradle.kts)
 * el PaymentSheet fallaría al abrirse, así que la pantalla de pago oculta la
 * opción de tarjeta en lugar de mostrar un error de Stripe al cliente.
 */
fun isStripeConfigured(key: String = BuildConfig.STRIPE_PUBLISHABLE_KEY): Boolean =
    key.startsWith("pk_") && !key.contains("PENDIENTE")

/**
 * Inicializa Stripe con la publishable key una sola vez por pantalla y devuelve si la tarjeta está
 * disponible en este build. Reemplaza el `remember { PaymentConfiguration.init(...) }` que devolvía
 * Unit (lint RememberReturnType lo marcaba como error y frenaba el CI de Android, T141).
 */
@Composable
fun rememberStripeReady(): Boolean {
    val context = LocalContext.current
    return remember {
        isStripeConfigured().also { ready ->
            if (ready) PaymentConfiguration.init(context, BuildConfig.STRIPE_PUBLISHABLE_KEY)
        }
    }
}

/**
 * Colores de la hoja de Stripe con el tema elegido en la app (no el modo claro/oscuro del
 * sistema): Stripe escoge colorsLight o colorsDark según el sistema, así que ambos llevan la
 * misma paleta de UrbanBlade y la hoja se ve igual que el resto de la app en los cuatro temas.
 */
private fun urbanStripeColors() = PaymentSheet.Colors(
    primary = UrbanColors.Gold.toArgb(),
    surface = UrbanColors.Background.toArgb(),
    component = UrbanColors.Card.toArgb(),
    componentBorder = UrbanColors.Line.toArgb(),
    componentDivider = UrbanColors.Line.toArgb(),
    onComponent = UrbanColors.Ink.toArgb(),
    onSurface = UrbanColors.Ink.toArgb(),
    subtitle = UrbanColors.Muted.toArgb(),
    placeholderText = UrbanColors.Muted.toArgb(),
    appBarIcon = UrbanColors.Ink.toArgb(),
    error = UrbanColors.Danger.toArgb()
)

/**
 * Envoltura delgada sobre el PaymentSheet de Stripe: inicializa la
 * publishable key una sola vez y expone una función simple
 * (clientSecret) -> Unit para presentar la hoja de pago. Reusable por el
 * checkout de citas y, en la siguiente ronda, por paquetes/membresías/gift
 * cards -- la confirmación real del pago siempre ocurre por webhook en
 * barber, nunca aquí (ver StripeWebhookController).
 */
@Composable
fun rememberUrbanPaymentSheet(
    merchantDisplayName: String = "UrbanBlade",
    onAuthorizationDenied: () -> Unit,
    onPreparationFailed: (Throwable) -> Unit,
    onResult: (PaymentSheetResult) -> Unit
): (String) -> Unit {
    rememberStripeReady()
    val context = LocalContext.current
    val latestResult by rememberUpdatedState(onResult)
    val latestCancel by rememberUpdatedState(onAuthorizationDenied)
    val latestError by rememberUpdatedState(onPreparationFailed)
    val flow = remember { FinalPaymentFlow() }
    var alive by remember { mutableStateOf(true) }
    var optionLabel by remember { mutableStateOf("") }
    var amountLabel by remember { mutableStateOf("") }
    var attempt by remember { mutableStateOf(0L) }
    val stripe = remember(context) { Stripe(context, BuildConfig.STRIPE_PUBLISHABLE_KEY) }
    val optionCallback = remember {
        PaymentOptionCallback { option ->
            if (alive && flow.step == FinalPaymentFlow.Step.SELECTING) {
                if (option == null) { flow.finish(); latestCancel() }
                else { optionLabel = option.label; flow.selected() }
            }
        }
    }
    val resultCallback = remember {
        PaymentSheetResultCallback { result ->
            if (alive) { flow.finish(); latestResult(result) }
        }
    }
    val controller = rememberPaymentSheetFlowController(optionCallback, resultCallback)
    val authorize = rememberPaymentAuthorization { flow.authorizationDenied() }
    DisposableEffect(flow) {
        onDispose { alive = false; flow.finish() }
    }
    if (flow.step == FinalPaymentFlow.Step.LOADING) {
        AlertDialog(
            onDismissRequest = { flow.finish(); latestCancel() },
            title = { Text("Preparando pago") },
            text = { Text("Verificando el importe con Stripe…") },
            confirmButton = {
                TextButton(onClick = { flow.finish(); latestCancel() }) { Text("Cancelar") }
            }
        )
    }
    if (flow.step == FinalPaymentFlow.Step.REVIEW || flow.step == FinalPaymentFlow.Step.AUTHORIZING) {
        val verifying = flow.step == FinalPaymentFlow.Step.AUTHORIZING
        FinalPaymentReview(
            merchant = merchantDisplayName, card = optionLabel, amount = amountLabel,
            verifying = verifying,
            onPay = {
                val id = attempt
                if (flow.pay()) authorize {
                    if (alive && flow.authorized(id)) controller.confirm()
                }
            },
            onEdit = { if (flow.edit()) controller.presentPaymentOptions() },
            onCancel = { flow.finish(); latestCancel() }
        )
    }

    return remember(controller, stripe, merchantDisplayName) {
        open@{ clientSecret ->
            val id = flow.begin() ?: return@open
            attempt = id
            optionLabel = ""
            amountLabel = ""
            fun fail(error: Exception) {
                if (alive && flow.isCurrent(id)) { flow.finish(); latestError(error) }
            }
            stripe.retrievePaymentIntent(clientSecret, callback = object : ApiResultCallback<PaymentIntent> {
                override fun onSuccess(result: PaymentIntent) {
                    if (!alive || !flow.isCurrent(id)) return
                    val cents = result.amount
                    if (result.currency != "mxn" || cents == null || cents <= 0) {
                        fail(IllegalStateException("No se pudo verificar el importe del pago.")); return
                    }
                    if (result.status == com.stripe.android.model.StripeIntent.Status.Succeeded ||
                        result.status == com.stripe.android.model.StripeIntent.Status.Processing) {
                        fail(IllegalStateException("El pago ya fue enviado. Actualiza tus citas antes de reintentar.")); return
                    }
                    amountLabel = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-MX"))
                        .format(BigDecimal.valueOf(cents).movePointLeft(2)) + " MXN"
                    val appearance = PaymentSheet.Appearance(
                        colorsLight = urbanStripeColors(), colorsDark = urbanStripeColors(),
                        shapes = PaymentSheet.Shapes(cornerRadiusDp = 16f, borderStrokeWidthDp = 1f)
                    )
                    controller.configureWithPaymentIntent(clientSecret,
                        PaymentSheet.Configuration(merchantDisplayName = merchantDisplayName, appearance = appearance)
                    ) { success, error ->
                        if (alive && flow.isCurrent(id)) {
                            if (success && flow.loaded(id)) controller.presentPaymentOptions()
                            else fail(IllegalStateException("No se pudo preparar el pago. Intenta de nuevo.", error))
                        }
                    }
                }
                override fun onError(e: Exception) { fail(IllegalStateException("No se pudo consultar el importe del pago. Intenta de nuevo.", e)) }
            })
        }
    }
}

@Composable
internal fun FinalPaymentReview(
    merchant: String, card: String, amount: String, verifying: Boolean,
    onPay: () -> Unit, onEdit: () -> Unit, onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!verifying) onCancel() },
        title = { Text("Confirmar pago") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(merchant)
                Text(card)
                Text("Total a pagar: $amount")
                Text("Al pulsar Pagar verificarás tu identidad con huella o bloqueo del teléfono.")
                TextButton(enabled = !verifying, onClick = onEdit) { Text("Cambiar tarjeta") }
            }
        },
        confirmButton = {
            UrbanPrimaryButton(
                text = "Pagar $amount", onClick = onPay, enabled = !verifying,
                loading = verifying, modifier = Modifier.fillMaxWidth()
            )
        },
        dismissButton = {
            TextButton(enabled = !verifying, onClick = onCancel) { Text("Cancelar") }
        }
    )
}
