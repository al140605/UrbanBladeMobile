package com.urbanblade.mobile.data.model

import com.google.gson.annotations.SerializedName

/**
 * `POST appointments/{code}/check-in` («Llegó»): registra cuándo llegó el cliente sin cambiar el estado de la cita.
 * Es idempotente: si ya estaba marcada devuelve la misma hora en [llegadaEn] (ISO-8601).
 */
data class CheckInResponse(
    val message: String? = null,
    @SerializedName("llegada_en") val llegadaEn: String? = null
)
