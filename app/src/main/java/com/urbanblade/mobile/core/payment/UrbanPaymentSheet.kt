package com.urbanblade.mobile.core.payment

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.rememberPaymentSheet
import com.urbanblade.mobile.BuildConfig

/**
 * true solo si el build trae una publishable key real de Stripe. Sin ella
 * (valor por defecto "pk_test_PENDIENTE_CONFIGURAR" en app/build.gradle.kts)
 * el PaymentSheet fallaría al abrirse, así que la pantalla de pago oculta la
 * opción de tarjeta en lugar de mostrar un error de Stripe al cliente.
 */
fun isStripeConfigured(key: String = BuildConfig.STRIPE_PUBLISHABLE_KEY): Boolean =
    key.startsWith("pk_") && !key.contains("PENDIENTE")

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
    onResult: (PaymentSheetResult) -> Unit
): (String) -> Unit {
    val context = LocalContext.current
    remember { if (isStripeConfigured()) PaymentConfiguration.init(context, BuildConfig.STRIPE_PUBLISHABLE_KEY) }
    val paymentSheet = rememberPaymentSheet(onResult)

    return remember(paymentSheet) {
        { clientSecret: String ->
            paymentSheet.presentWithPaymentIntent(
                clientSecret,
                PaymentSheet.Configuration(merchantDisplayName = merchantDisplayName)
            )
        }
    }
}
