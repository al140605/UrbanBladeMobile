package com.urbanblade.mobile.data.model

import com.google.gson.annotations.SerializedName

/*
 * Modelos de "Mi cuenta", la pantalla que comparten todos los roles (perfil, seguridad y
 * preferencias). Viven aparte de Models.kt para que la cuenta se mantenga como un módulo propio.
 */

/** GET /profile: mismo usuario que /auth/me más la antigüedad y la verificación del correo. */
data class AccountProfileResponse(val user: AccountUser)

data class AccountUser(
    val name: String,
    val email: String,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    val roles: List<String> = emptyList(),
    @SerializedName("email_verified_at") val emailVerifiedAt: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    val client: AccountClientInfo? = null
)

data class AccountClientInfo(
    val telefono: String? = null,
    @SerializedName("fecha_nacimiento") val fechaNacimiento: String? = null,
    val sexo: String? = null,
    @SerializedName("descuento_activo_pct") val descuentoActivoPct: Double? = null
)

/** POST /profile/avatar (multipart, campo "avatar"): devuelve la URL nueva de la foto. */
data class AvatarResponse(val message: String? = null, val user: AuthUser? = null)

/**
 * GET/PATCH /notifications/preferences. El backend guarda más canales (sms, whatsapp, push),
 * pero la app solo muestra los que hoy sí entregan avisos: en la app, correo y promociones.
 * El PATCH hace merge en el servidor, así que se manda únicamente el canal que cambió.
 */
data class NotificationPreferences(
    @SerializedName("in_app") val inApp: Boolean = true,
    val email: Boolean = true,
    val promociones: Boolean = true
)

data class NotificationPreferencesResponse(
    val message: String? = null,
    val data: NotificationPreferences = NotificationPreferences()
)
