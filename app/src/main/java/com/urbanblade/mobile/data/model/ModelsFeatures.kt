package com.urbanblade.mobile.data.model

import com.google.gson.annotations.SerializedName

/**
 * Interruptores de módulos que el backend expone en `GET barbershop` (config/features.php de barber).
 * Con un módulo apagado sus rutas del backend NO existen (404), así que la app no debe mostrarlo.
 * Todos arrancan en `false`: sin respuesta (o con una clave ausente) el módulo se trata como apagado.
 * Membresía y puntos de lealtad no tienen interruptor: siempre están.
 */
data class ModuleFeatures(
    val packages: Boolean = false,
    @SerializedName("gift_cards") val giftCards: Boolean = false,
    val referrals: Boolean = false,
    val raffles: Boolean = false,
    val social: Boolean = false,
    val campaigns: Boolean = false
) {
    /**
     * Si la app puede abrir [route]. Una ruta de un módulo apagado no se abre desde ningún lado (atajo, aviso
     * viejo, enlace `urbanblade://open?route=…`); las demás rutas siempre están permitidas.
     */
    fun allowsRoute(route: String?): Boolean = when (route?.substringBefore('?')?.trim()) {
        "social", "moderation", "blocked_authors" -> social
        "campaigns" -> campaigns
        "raffles" -> raffles
        "gift_cards" -> giftCards
        else -> true
    }
}

/** Cuerpo de `GET barbershop`: la app solo lee `data.features`; lo demás de la ficha pública no le sirve. */
data class BarbershopPublicResponse(val data: BarbershopPublicData? = null)

data class BarbershopPublicData(val features: ModuleFeatures? = null)
