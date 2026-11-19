package com.urbanblade.mobile.data.model

import com.google.gson.annotations.SerializedName


// `plataforma` le dice al backend que este token es de la app móvil: tiene su propia vigencia
// (config/auth.php de barber: 180 días con ventana deslizante, la web usa 30). Sin ella el
// backend la infiere del device_name, pero declararla evita depender del texto del nombre.
data class LoginRequest(
    val email: String,
    val password: String,
    @SerializedName("device_name") val deviceName: String = "Android UrbanBlade",
    val plataforma: String = "movil"
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    @SerializedName("password_confirmation") val passwordConfirmation: String,
    @SerializedName("device_name") val deviceName: String = "Android UrbanBlade",
    val plataforma: String = "movil"
)

data class ForgotPasswordRequest(val email: String)
data class GoogleLoginRequest(@SerializedName("id_token") val idToken: String)

data class LoginResponse(
    val message: String? = null,
    @SerializedName("token_type") val tokenType: String? = null,
    val token: String,
    val user: AuthUser
)

data class MeResponse(
    val user: AuthUser,
    /** La sesión se abrió con Google: para eliminar la cuenta se confirma con ELIMINAR, no con contraseña. */
    @SerializedName("sesion_con_google") val sesionConGoogle: Boolean = false
)

data class AuthUser(
    val id: String? = null,
    val name: String,
    val email: String,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    val roles: List<String> = emptyList(),
    @SerializedName("profile_complete") val profileComplete: Boolean = true,
    @SerializedName("profile_missing") val profileMissing: List<String> = emptyList(),
    @SerializedName("client_id") val clientId: String? = null,
    @SerializedName("barber_id") val barberId: String? = null,
    val client: ClientInfo? = null
)

data class ClientInfo(
    val telefono: String? = null,
    @SerializedName("fecha_nacimiento") val fechaNacimiento: String? = null,
    val sexo: String? = null
)

data class MessageResponse(
    val message: String? = null,
    /** Solo en PATCH /appointments/{code}/status al pasar a «completada»: el ticket del servicio. */
    val ticket: ServiceTicket? = null
)

/** Ticket del servicio terminado (GET /appointments/{code}/ticket y la respuesta de «completada»). */
data class ServiceTicket(
    val folio: String = "",
    val cita: String? = null,
    val fecha: String? = null,
    val cliente: String? = null,
    val barbero: String? = null,
    val servicio: String? = null,
    @SerializedName("duracion_min") val duracionMin: Int = 0,
    @SerializedName("minutos_extra") val minutosExtra: Int = 0,
    @SerializedName("metodo_pago") val metodoPago: String? = null,
    @SerializedName("precio_servicio") val precioServicio: Double = 0.0,
    val descuentos: Double = 0.0,
    @SerializedName("deposito_aplicado") val depositoAplicado: Double = 0.0,
    val monto: Double = 0.0,
    val propina: Double = 0.0,
    @SerializedName("total_pagado") val totalPagado: Double = 0.0,
    @SerializedName("comprobante_url") val comprobanteUrl: String? = null
)

data class ServiceTicketResponse(val data: ServiceTicket? = null)

/** Cargo por inasistencia (GET /no-show-fees): el cliente ve los suyos; recepción, los pendientes. */
data class NoShowFeeCita(
    val code: String? = null,
    val fecha: String? = null,
    val servicio: String? = null,
    val cliente: String? = null
)

data class NoShowFeeItem(
    val id: String = "",
    @SerializedName("appointment_id") val appointmentId: String = "",
    val cita: NoShowFeeCita? = null,
    val monto: Double = 0.0,
    @SerializedName("monto_base") val montoBase: Double = 0.0,
    @SerializedName("credito_anticipo") val creditoAnticipo: Double = 0.0,
    val porcentaje: Int = 0,
    val estado: String = "pendiente",
    @SerializedName("metodo_cobro") val metodoCobro: String? = null
)

data class NoShowFeesResponse(
    val data: List<NoShowFeeItem> = emptyList(),
    /** Solo para el cliente: lo que debe en total. */
    @SerializedName("adeudo_total") val adeudoTotal: Double? = null
)

/** POST /appointments (201): trae la cita creada para poder ofrecer "Pagar ahora" sin buscarla en la lista. */
data class CreatedAppointmentData(val id: String? = null, val code: String? = null)
data class CreateAppointmentResponse(
    val message: String? = null,
    val data: CreatedAppointmentData? = null,
    /** Si los productos no se pudieron agregar (p. ej. sin stock); la cita igual queda reservada. */
    @SerializedName("productos_error") val productosError: String? = null
)

/** POST /profile/push-token: token de Firebase Cloud Messaging del dispositivo (T142). */
data class PushTokenRequest(val token: String, val provider: String = "fcm")
