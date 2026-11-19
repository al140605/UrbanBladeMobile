package com.urbanblade.mobile.data.model

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName


/** Servicio del catálogo tal como lo ve el administrador (GET /services/manage). */
data class ServiceAdminItem(
    val id: String = "",
    val slug: String = "",
    val nombre: String = "",
    val categoria: String = "",
    val precio: Double = 0.0,
    @SerializedName("duracion_min") val duracionMin: Int = 0,
    val descripcion: String? = null,
    val activo: Boolean = true,
    @SerializedName("imagen_url") val imagenUrl: String? = null
)

data class ServicesAdminMeta(
    @SerializedName("current_page") val currentPage: Int = 1,
    @SerializedName("last_page") val lastPage: Int = 1,
    val total: Int = 0
)

data class ServicesAdminResponse(
    val data: List<ServiceAdminItem> = emptyList(),
    val meta: ServicesAdminMeta = ServicesAdminMeta(),
    val categories: List<String> = emptyList()
)

/** Cuerpo de crear/editar servicio; el servidor valida precio >= 0 y duración de 5 a 600 min. */
data class ServiceUpsertRequest(
    val nombre: String,
    val categoria: String,
    val precio: Double,
    @SerializedName("duracion_min") val duracionMin: Int,
    val descripcion: String?,
    val activo: Boolean
)

data class BarberAdminUser(val id: String? = null, val name: String = "", val email: String = "")

/** Barbero tal como lo ve el administrador (GET /barbers/manage). */
data class BarberAdminItem(
    val id: String = "",
    val slug: String = "",
    val especialidades: String? = null,
    val descripcion: String? = null,
    val foto: String? = null,
    @SerializedName("foto_url") val fotoUrl: String? = null,
    val activo: Boolean = true,
    @SerializedName("comision_pct") val comisionPct: Double = 0.0,
    val user: BarberAdminUser = BarberAdminUser()
)

data class BarbersAdminResponse(
    val data: List<BarberAdminItem> = emptyList(),
    val meta: ServicesAdminMeta = ServicesAdminMeta()
)

/**
 * El servidor reescribe todo el perfil en cada PUT (lo que falte queda en null/false), por eso
 * siempre se manda el perfil completo, incluida la foto que hoy no se edita desde la app.
 */
data class BarberUpsertRequest(
    val name: String,
    val email: String,
    val especialidades: String?,
    val descripcion: String?,
    val foto: String?,
    val activo: Boolean,
    @SerializedName("comision_pct") val comisionPct: Double
)

data class BarberPerformance(
    val appointmentsThisMonth: Int = 0,
    val appointmentsLastMonth: Int = 0,
    val growth: Int = 0,
    val averageRating: Double = 0.0,
    val totalClients: Int = 0
)

data class BarberPerformanceResponse(val data: BarberPerformance = BarberPerformance())

/** Plan de membresía recurrente (GET /admin/membership-plans). */
data class MembershipPlanItem(
    val id: String = "",
    val nombre: String = "",
    val descripcion: String? = null,
    @SerializedName("precio_mensual") val precioMensual: Double = 0.0,
    @SerializedName("descuento_pct") val descuentoPct: Int = 0,
    val activo: Boolean = true
)

data class AdminMembershipPlansResponse(val data: List<MembershipPlanItem> = emptyList())

/** Crear o cambiar el precio de un plan crea un Price nuevo en Stripe; activar/desactivar no. */
data class MembershipPlanRequest(
    val nombre: String,
    val descripcion: String?,
    @SerializedName("precio_mensual") val precioMensual: Double,
    @SerializedName("descuento_pct") val descuentoPct: Int,
    val activo: Boolean
)

/** Paquete prepagado de usos de un servicio (GET /admin/service-packages). */
data class ServicePackageItem(
    val id: String = "",
    val nombre: String = "",
    val service: PackageServiceRef = PackageServiceRef(),
    @SerializedName("cantidad_usos") val cantidadUsos: Int = 0,
    val precio: Double = 0.0,
    @SerializedName("vigencia_dias") val vigenciaDias: Int? = null,
    val activo: Boolean = true
)

data class ServicePackagesResponse(val data: List<ServicePackageItem> = emptyList())

data class ServicePackageRequest(
    val nombre: String,
    @SerializedName("service_id") val serviceId: String,
    @SerializedName("cantidad_usos") val cantidadUsos: Int,
    val precio: Double,
    @SerializedName("vigencia_dias") val vigenciaDias: Int?,
    val activo: Boolean
)

/** Bloque `meta` que el servidor agrega solo cuando se pide `page`. */
data class PageMeta(
    val page: Int = 1,
    @SerializedName("per_page") val perPage: Int = 0,
    val total: Int = 0,
    @SerializedName("has_more") val hasMore: Boolean = false
)

data class AppointmentsResponse(
    val data: List<AppointmentRow> = emptyList(),
    val meta: PageMeta? = null,
    val stats: AppointmentStats = AppointmentStats(),
    val next: AppointmentRow? = null,
    @SerializedName("cancellation_policy_hours") val cancellationPolicyHours: Int = 24
)

data class AppointmentRequest(
    @SerializedName("barber_id") val barberId: String,
    @SerializedName("service_id") val serviceId: String,
    val fecha: String,
    @SerializedName("hora_inicio") val horaInicio: String,
    val notas: String? = null,
    /** Pagar todo al reservar: barber fija el monto (con descuento) como cobro de la cita pendiente. */
    @SerializedName("pagar_ahora") val pagarAhora: Boolean? = null,
    @SerializedName("propina_sugerida") val propinaSugerida: Double? = null,
    /** El cliente acepta el cargo por inasistencia en el último paso de la reserva. */
    @SerializedName("acepta_cargo_inasistencia") val aceptaCargoInasistencia: Boolean? = null,
    /** Productos opcionales de la visita: barber crea un pedido ligado a la cita que se paga en el salón. */
    val productos: List<OrderItemRequest>? = null
)

/** POST /appointments/{code}/deposit/stripe-intent: cobro de "pagar ahora" (o depósito anti-no-show). */
data class DepositIntentRequest(
    @SerializedName("guardar_tarjeta") val guardarTarjeta: Boolean? = null,
    @SerializedName("tarjeta_guardada") val tarjetaGuardada: Boolean? = null
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
