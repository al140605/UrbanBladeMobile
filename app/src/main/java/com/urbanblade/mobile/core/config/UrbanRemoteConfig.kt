package com.urbanblade.mobile.core.config

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.urbanblade.mobile.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Interruptores de Firebase Remote Config: se cambian desde la consola y la app los aplica en
 * segundos (escucha en tiempo real), sin publicar otro APK. Los valores por defecto son los de
 * siempre, así que sin Firebase o sin red la app se comporta igual que antes.
 *
 * - bladebot_burbuja_activa (bool, true): muestra la burbuja de Bladebot.
 * - pago_tarjeta_activo (bool, true): ofrece pagar con tarjeta al reservar (si Stripe falla, se apaga).
 * - aviso_inicio (texto, vacío): mensaje para los clientes en su Inicio («Cerramos el lunes 30»).
 */
object UrbanRemoteConfig {
    const val KEY_BLADEBOT = "bladebot_burbuja_activa"
    const val KEY_CARD = "pago_tarjeta_activo"
    const val KEY_NOTICE = "aviso_inicio"

    private val defaults = mapOf<String, Any>(KEY_BLADEBOT to true, KEY_CARD to true, KEY_NOTICE to "")

    private val _bladebotBubble = MutableStateFlow(true)
    val bladebotBubble: StateFlow<Boolean> = _bladebotBubble.asStateFlow()
    private val _cardPayments = MutableStateFlow(true)
    val cardPayments: StateFlow<Boolean> = _cardPayments.asStateFlow()
    private val _homeNotice = MutableStateFlow("")
    val homeNotice: StateFlow<String> = _homeNotice.asStateFlow()

    fun init(context: Context) {
        if (FirebaseApp.getApps(context).isEmpty()) return
        val config = FirebaseRemoteConfig.getInstance()
        config.setConfigSettingsAsync(
            FirebaseRemoteConfigSettings.Builder()
                // En desarrollo se ve el cambio al momento; en release, a lo más cada hora (más la escucha en vivo).
                .setMinimumFetchIntervalInSeconds(if (BuildConfig.DEBUG) 0 else 3600)
                .build()
        )
        config.setDefaultsAsync(defaults).addOnCompleteListener { publish(config) }
        config.fetchAndActivate().addOnCompleteListener { publish(config) }
        config.addOnConfigUpdateListener(object : ConfigUpdateListener {
            override fun onUpdate(configUpdate: ConfigUpdate) {
                config.activate().addOnCompleteListener { publish(config) }
            }

            override fun onError(error: FirebaseRemoteConfigException) = Unit
        })
    }

    private fun publish(config: FirebaseRemoteConfig) {
        _bladebotBubble.value = config.getBoolean(KEY_BLADEBOT)
        _cardPayments.value = config.getBoolean(KEY_CARD)
        _homeNotice.value = config.getString(KEY_NOTICE).trim()
    }
}
