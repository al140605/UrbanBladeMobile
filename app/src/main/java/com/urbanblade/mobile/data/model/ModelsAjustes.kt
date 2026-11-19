package com.urbanblade.mobile.data.model

import com.google.gson.TypeAdapter
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter


// ── Configuración de la barbería (settings, admin-only) ──────────────────
@JsonAdapter(RedesSocialesAdapter::class, nullSafe = false)
data class RedesSociales(val instagram: String? = null, val facebook: String? = null, val tiktok: String? = null)

/**
 * Laravel serializa un arreglo vacío de PHP como `[]`, no como `{}`: un negocio sin redes
 * guardadas devuelve `"redes_sociales": []` y Gson fallaba al esperar un objeto, lo que dejaba
 * la pantalla de Configuración con «No se pudo cargar». `[]` y `null` equivalen a «sin redes».
 */
class RedesSocialesAdapter : TypeAdapter<RedesSociales>() {
    override fun read(reader: JsonReader): RedesSociales {
        if (reader.peek() != JsonToken.BEGIN_OBJECT) {
            reader.skipValue()
            return RedesSociales()
        }
        var instagram: String? = null
        var facebook: String? = null
        var tiktok: String? = null
        reader.beginObject()
        while (reader.hasNext()) {
            val name = reader.nextName()
            if (reader.peek() == JsonToken.STRING && name in REDES) {
                val value = reader.nextString()
                when (name) {
                    "instagram" -> instagram = value
                    "facebook" -> facebook = value
                    "tiktok" -> tiktok = value
                }
            } else {
                reader.skipValue()
            }
        }
        reader.endObject()
        return RedesSociales(instagram, facebook, tiktok)
    }

    override fun write(writer: JsonWriter, value: RedesSociales?) {
        if (value == null) {
            writer.nullValue()
            return
        }
        writer.beginObject()
        writer.name("instagram").value(value.instagram)
        writer.name("facebook").value(value.facebook)
        writer.name("tiktok").value(value.tiktok)
        writer.endObject()
    }

    private companion object {
        val REDES = setOf("instagram", "facebook", "tiktok")
    }
}
data class DatosBancarios(val clabe: String? = null, val banco: String? = null, val beneficiario: String? = null, val concepto: String? = null)
data class BarbershopSetting(
    val id: String? = null,
    val nombre: String = "",
    val direccion: String? = null,
    val telefono: String? = null,
    @SerializedName("horario_apertura") val horarioApertura: String? = null,
    @SerializedName("horario_cierre") val horarioCierre: String? = null,
    @SerializedName("politica_cancelacion") val politicaCancelacion: Int = 24,
    @SerializedName("deposito_no_show_umbral") val depositoNoShowUmbral: Int = 2,
    @SerializedName("deposito_no_show_porcentaje") val depositoNoShowPorcentaje: Int = 50,
    @SerializedName("maintenance_mode") val maintenanceMode: Boolean = false,
    @SerializedName("redes_sociales") val redesSociales: RedesSociales = RedesSociales(),
    @SerializedName("datos_bancarios") val datosBancarios: DatosBancarios = DatosBancarios()
)
data class BarbershopSettingResponse(val data: BarbershopSetting = BarbershopSetting())
data class UpdateSettingRequest(
    val nombre: String,
    val direccion: String?,
    val telefono: String?,
    @SerializedName("horario_apertura") val horarioApertura: String?,
    @SerializedName("horario_cierre") val horarioCierre: String?,
    @SerializedName("politica_cancelacion") val politicaCancelacion: Int,
    @SerializedName("deposito_no_show_umbral") val depositoNoShowUmbral: Int?,
    @SerializedName("deposito_no_show_porcentaje") val depositoNoShowPorcentaje: Int?,
    val instagram: String?,
    val facebook: String?,
    val tiktok: String?,
    val clabe: String?,
    val banco: String?,
    val beneficiario: String?,
    val concepto: String?
)
data class MaintenanceToggleResponse(val message: String? = null, val data: MaintenanceStatus = MaintenanceStatus())
data class MaintenanceStatus(@SerializedName("maintenance_mode") val maintenanceMode: Boolean = false)

// ── Agenda de barbero (barber/agenda) ────────────────────────────────────
data class AgendaRange(val start: String? = null, val end: String? = null, val label: String? = null)
data class AgendaStats(
    @SerializedName("completed_count") val completedCount: Int = 0,
    @SerializedName("income_total") val incomeTotal: Double = 0.0,
    val productivity: Int = 0,
    @SerializedName("total_period") val totalPeriod: Int = 0,
    @SerializedName("pending_period") val pendingPeriod: Int = 0,
    @SerializedName("confirmed_period") val confirmedPeriod: Int = 0,
    @SerializedName("in_process_period") val inProcessPeriod: Int = 0,
    @SerializedName("completed_period") val completedPeriod: Int = 0,
    @SerializedName("cancelled_period") val cancelledPeriod: Int = 0,
    @SerializedName("no_show_period") val noShowPeriod: Int = 0
)
data class BarberAgendaResponse(
    val data: List<AppointmentRow> = emptyList(),
    val period: String = "day",
    val estado: String? = null,
    val offset: Int = 0,
    val range: AgendaRange = AgendaRange(),
    val stats: AgendaStats = AgendaStats()
)
