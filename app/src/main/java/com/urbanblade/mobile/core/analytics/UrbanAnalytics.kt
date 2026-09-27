package com.urbanblade.mobile.core.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Eventos de Google Analytics for Firebase (proyecto barber-c6b3a). Sin app/google-services.json
 * no hay FirebaseApp y todo queda en silencio. Nunca se mandan datos personales: nada de nombres,
 * correos, teléfonos ni códigos de cita; solo pantallas, pasos, método de pago y montos.
 *
 * Embudo de reserva: reserva_inicio → reserva_paso (servicio, horario, extras, pago) → reserva_confirmada.
 */
object UrbanAnalytics {
    private var analytics: FirebaseAnalytics? = null

    fun init(context: Context) {
        if (FirebaseApp.getApps(context).isNotEmpty()) {
            analytics = FirebaseAnalytics.getInstance(context)
        }
    }

    fun log(name: String, vararg params: Pair<String, Any?>) {
        val target = analytics ?: return
        val bundle = Bundle()
        params.forEach { (key, value) ->
            when (value) {
                is String -> bundle.putString(key, value.take(100))
                is Int -> bundle.putLong(key, value.toLong())
                is Long -> bundle.putLong(key, value)
                is Double -> bundle.putDouble(key, value)
                is Boolean -> bundle.putString(key, if (value) "si" else "no")
                null -> Unit
                else -> bundle.putString(key, value.toString().take(100))
            }
        }
        target.logEvent(name, bundle)
    }

    /** La app es una sola Activity con Compose: sin esto Analytics solo vería una pantalla. */
    fun screen(route: String) = log(
        FirebaseAnalytics.Event.SCREEN_VIEW,
        FirebaseAnalytics.Param.SCREEN_NAME to route,
        FirebaseAnalytics.Param.SCREEN_CLASS to "Compose"
    )
}
