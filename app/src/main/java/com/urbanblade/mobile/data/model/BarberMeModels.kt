package com.urbanblade.mobile.data.model

import com.google.gson.annotations.SerializedName

/** `GET barber/me`: perfil completo del barbero autenticado (bio, foto, calificación y actividad). */
data class BarberMeResponse(
    val id: String,
    val name: String,
    val email: String,
    val especialidades: String? = null,
    val descripcion: String? = null,
    @SerializedName("foto_url") val fotoUrl: String? = null,
    @SerializedName("calificacion_promedio") val calificacionPromedio: Double? = null,
    @SerializedName("total_resenas") val totalResenas: Int = 0,
    val activo: Boolean = true,
    @SerializedName("member_since") val memberSince: String? = null,
    @SerializedName("years_experience") val yearsExperience: Int = 0,
    @SerializedName("portfolio_total") val portfolioTotal: Int = 0,
    /** Nulable: Gson no aplica el valor por defecto de un objeto anidado si el servidor omite el campo. */
    val stats: BarberMeStats? = null
)

data class BarberMeStats(
    @SerializedName("citas_hoy") val citasHoy: Int = 0,
    @SerializedName("completadas_mes") val completadasMes: Int = 0,
    @SerializedName("total_completadas") val totalCompletadas: Int = 0
)

/** `POST barber/profile` (multipart: especialidades, descripcion y foto opcional). */
data class BarberProfileUpdateResponse(
    val message: String? = null,
    val especialidades: String? = null,
    val descripcion: String? = null,
    @SerializedName("foto_url") val fotoUrl: String? = null
)
