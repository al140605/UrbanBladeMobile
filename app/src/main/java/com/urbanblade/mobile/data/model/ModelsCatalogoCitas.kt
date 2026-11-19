package com.urbanblade.mobile.data.model

import com.google.gson.annotations.SerializedName


data class ApiList<T>(val data: List<T> = emptyList())

data class ServiceItem(
    val id: String,
    val nombre: String,
    val precio: Double = 0.0,
    @SerializedName("duracion_min") val duracionMin: Int = 0,
    val descripcion: String? = null,
    val imagen: String? = null
)

data class BarberUser(val id: String? = null, val name: String? = null)

data class BarberItem(
    val id: String,
    val slug: String? = null,
    val user: BarberUser? = null,
    val descripcion: String? = null,
    val foto: String? = null,
    val especialidades: String? = null,
    @SerializedName("es_favorito") val esFavorito: Boolean = false,
    @SerializedName("avg_rating") val avgRating: Double? = null,
    @SerializedName("total_reviews") val totalReviews: Int = 0
)

/** GET /barbers/{slug}: ficha pública del barbero con portafolio y reseñas. */
data class BarberProfileResponse(
    val barber: BarberItem,
    val works: List<BarberWork> = emptyList(),
    val reviews: List<BarberReviewItem> = emptyList(),
    @SerializedName("avg_rating") val avgRating: Double? = null,
    @SerializedName("total_reviews") val totalReviews: Int = 0,
    @SerializedName("citas_completadas") val citasCompletadas: Int = 0,
    @SerializedName("can_review") val canReview: Boolean = false,
    @SerializedName("already_reviewed") val alreadyReviewed: Boolean = false,
    /** Servicios que el cliente recibió con este barbero y puede calificar. */
    @SerializedName("reviewable_services") val reviewableServices: List<ReviewableService> = emptyList()
)

data class BarberWork(val id: String, val title: String? = null, val description: String? = null, val images: List<String> = emptyList())
data class ReviewAuthor(val user: BarberUser? = null)
data class BarberReviewItem(
    val id: String,
    val rating: Int = 0,
    val comment: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    val client: ReviewAuthor? = null,
    /** Nombre del servicio calificado, si la reseña es de un servicio. */
    val service: String? = null
)
data class ReviewableService(val id: String, val nombre: String)
data class BarberReviewRequest(
    val rating: Int,
    val comment: String? = null,
    @SerializedName("service_id") val serviceId: String? = null
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
    val user: BarberUser? = null,
    @SerializedName("foto_url") val fotoUrl: String? = null
)

data class AppointmentService(
    val id: String? = null,
    val nombre: String? = null,
    val precio: Double? = null,
    @SerializedName("duracion_min") val duracionMin: Int? = null
)

data class AppointmentClientUser(val name: String? = null, @SerializedName("avatar_url") val avatarUrl: String? = null)
data class AppointmentClientRef(val id: String? = null, val user: AppointmentClientUser? = null)
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
    /** sin_pago | pendiente_en_recepcion | transferencia_en_revision | pagado (null con un servidor anterior). */
    @SerializedName("payment_status") val paymentStatus: String? = null,
    @SerializedName("can_change_payment_method") val canChangePaymentMethod: Boolean? = null,
    @SerializedName("reminder_24h_sent") val reminder24hSent: Boolean = false,
    @SerializedName("reminder_2h_sent") val reminder2hSent: Boolean = false,
    /** Flujo V2 (solo citas confirmadas): si el pago ya está resuelto y si se puede iniciar el servicio, con el motivo. */
    @SerializedName("pago_resuelto") val pagoResuelto: Boolean? = null,
    @SerializedName("puede_iniciar") val puedeIniciar: Boolean? = null,
    @SerializedName("motivo_no_iniciar") val motivoNoIniciar: String? = null,
    /** Solo en servicio en proceso: fin estimado (ISO-8601, con el tiempo extra) y minutos agregados. */
    @SerializedName("fin_estimado") val finEstimado: String? = null,
    @SerializedName("minutos_extra") val minutosExtra: Int? = null,
    val client: AppointmentClientRef? = null,
    val barber: AppointmentBarber? = null,
    val service: AppointmentService? = null
)

data class AppointmentStats(
    val total: Int = 0,
    val proximas: Int = 0,
    val completadas: Int = 0,
    val canceladas: Int = 0
)
