package com.urbanblade.mobile.data.model

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val email: String,
    val password: String,
    @SerializedName("device_name") val deviceName: String = "Android UrbanBlade"
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    @SerializedName("password_confirmation") val passwordConfirmation: String,
    @SerializedName("device_name") val deviceName: String = "Android UrbanBlade"
)

data class ForgotPasswordRequest(val email: String)

data class LoginResponse(
    val message: String? = null,
    @SerializedName("token_type") val tokenType: String? = null,
    val token: String,
    val user: AuthUser
)

data class MeResponse(val user: AuthUser)

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

data class MessageResponse(val message: String? = null)

data class ApiList<T>(val data: List<T> = emptyList())

data class ServiceItem(
    val id: String,
    val nombre: String,
    val precio: Double = 0.0,
    @SerializedName("duracion_min") val duracionMin: Int = 0,
    val descripcion: String? = null
)

data class BarberUser(val id: String? = null, val name: String? = null)

data class BarberItem(
    val id: String,
    val slug: String? = null,
    val user: BarberUser? = null,
    val descripcion: String? = null,
    val foto: String? = null,
    val especialidades: String? = null
)

data class SlotItem(
    val time: String,
    val label: String,
    @SerializedName("end_time") val endTime: String? = null,
    @SerializedName("end_label") val endLabel: String? = null
)

data class SlotsResponse(val slots: List<SlotItem> = emptyList())

data class AppointmentBarber(
    val id: String? = null,
    val slug: String? = null,
    val user: BarberUser? = null
)

data class AppointmentService(
    val id: String? = null,
    val nombre: String? = null,
    val precio: Double? = null,
    @SerializedName("duracion_min") val duracionMin: Int? = null
)

data class AppointmentRow(
    val id: String,
    val code: String? = null,
    val fecha: String,
    @SerializedName("hora_inicio") val horaInicio: String,
    @SerializedName("hora_fin") val horaFin: String? = null,
    val estado: String,
    val notas: String? = null,
    @SerializedName("precio_cobrado") val precioCobrado: Double? = null,
    @SerializedName("has_payment") val hasPayment: Boolean = false,
    @SerializedName("is_chargeable") val isChargeable: Boolean = false,
    val barber: AppointmentBarber? = null,
    val service: AppointmentService? = null
)

data class AppointmentStats(
    val total: Int = 0,
    val proximas: Int = 0,
    val completadas: Int = 0,
    val canceladas: Int = 0
)

data class AppointmentsResponse(
    val data: List<AppointmentRow> = emptyList(),
    val stats: AppointmentStats = AppointmentStats(),
    val next: AppointmentRow? = null,
    @SerializedName("cancellation_policy_hours") val cancellationPolicyHours: Int = 24
)

data class AppointmentRequest(
    @SerializedName("barber_id") val barberId: String,
    @SerializedName("service_id") val serviceId: String,
    val fecha: String,
    @SerializedName("hora_inicio") val horaInicio: String,
    val notas: String? = null
)

data class DashboardResponse(
    val role: String? = null,
    val data: JsonObject = JsonObject()
)

data class ProfileResponse(val user: ProfileUser)

data class ProfileUser(
    val name: String,
    val email: String,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    val roles: List<String> = emptyList(),
    val client: ClientInfo? = null
)

data class UpdateProfileRequest(
    val name: String,
    val email: String,
    val telefono: String? = null,
    @SerializedName("fecha_nacimiento") val fechaNacimiento: String? = null,
    val sexo: String? = null
)

data class UpdateProfileResponse(
    val message: String? = null,
    val user: AuthUser? = null
)

data class ChangePasswordRequest(
    @SerializedName("current_password") val currentPassword: String,
    val password: String,
    @SerializedName("password_confirmation") val passwordConfirmation: String
)

data class NotificationItem(
    val id: String,
    val title: String? = null,
    val message: String? = null,
    @SerializedName("read_at") val readAt: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class ProductItem(
    val id: String,
    val nombre: String,
    val categoria: String? = null,
    val descripcion: String? = null,
    @SerializedName("precio_venta") val precioVenta: Double = 0.0,
    @SerializedName("stock_actual") val stockActual: Int = 0,
    val imagen: String? = null
)

data class OrderItemRequest(
    @SerializedName("product_id") val productId: String,
    val cantidad: Int
)
data class OrderRequest(val items: List<OrderItemRequest>)
data class DeliverOrderRequest(@SerializedName("metodo_pago") val metodoPago: String)
data class OrderLine(
    @SerializedName("product_id") val productId: String? = null,
    val nombre: String? = null,
    val precio: Double = 0.0,
    val cantidad: Int = 0,
    val subtotal: Double = 0.0
)
data class OrderClient(val id: String? = null, val name: String? = null)
data class OrderRow(
    val id: String,
    val folio: String? = null,
    val estado: String = "pendiente",
    val tipo: String? = null,
    val total: Double = 0.0,
    @SerializedName("metodo_pago") val metodoPago: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    val items: List<OrderLine> = emptyList(),
    val client: OrderClient? = null
)
data class OrderMeta(val current_page: Int? = null, val last_page: Int? = null, val total: Int? = null)
data class OrdersResponse(val data: List<OrderRow> = emptyList(), val meta: JsonObject? = null)
data class OrderMutationResponse(val message: String? = null, val data: OrderRow? = null)

data class PaymentAppointment(
    val id: String? = null,
    val fecha: String? = null,
    @SerializedName("hora_inicio") val horaInicio: String? = null,
    val service: String? = null,
    val client: String? = null,
    val barber: String? = null
)
data class PaymentRow(
    val id: String,
    val monto: Double = 0.0,
    @SerializedName("metodo_pago") val metodoPago: String? = null,
    val propina: Double = 0.0,
    @SerializedName("receipt_url") val receiptUrl: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    val appointment: PaymentAppointment? = null
)
data class PaymentsResponse(val data: List<PaymentRow> = emptyList(), val meta: JsonObject? = null)
data class PendingPaymentRow(
    val id: String,
    val monto: Double = 0.0,
    @SerializedName("comprobante_url") val comprobanteUrl: String? = null,
    @SerializedName("ocr_texto") val ocrTexto: String? = null,
    @SerializedName("ocr_monto_detectado") val ocrMontoDetectado: Double? = null,
    val appointment: JsonObject? = null
)
data class PendingPaymentsResponse(val data: List<PendingPaymentRow> = emptyList())
data class RejectPaymentRequest(@SerializedName("motivo_rechazo") val motivo: String)
data class StripeIntentRequest(@SerializedName("appointment_id") val appointmentId: String, @SerializedName("puntos_canjeados") val puntos: Int = 0)
