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
    remember { PaymentConfiguration.init(context, BuildConfig.STRIPE_PUBLISHABLE_KEY) }
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
