package com.urbanblade.mobile.ui.viewmodel

import com.google.gson.JsonParser
import retrofit2.HttpException

/** Nombre en español de los campos que el servidor puede rechazar (claves de `errors` de un 422 de Laravel). */
private val FIELD_LABELS = mapOf(
    "name" to "Nombre",
    "email" to "Correo",
    "password" to "Contraseña",
    "current_password" to "Contraseña actual",
    "password_confirmation" to "Confirmación de contraseña",
    "telefono" to "Teléfono",
    "fecha_nacimiento" to "Fecha de nacimiento",
    "sexo" to "Sexo",
    "notas" to "Notas",
    "comment" to "Comentario",
    "descripcion" to "Descripción",
    "fecha" to "Fecha",
    "hora_inicio" to "Hora",
    "barber_id" to "Barbero",
    "service_id" to "Servicio",
    "client_id" to "Cliente",
    "monto" to "Monto",
    "propina" to "Propina",
    "metodo_pago" to "Método de pago",
    "cantidad" to "Cantidad",
    "precio" to "Precio",
    "nombre" to "Nombre",
    "rating" to "Calificación",
    "confirmacion" to "Confirmación",
    "acepta_cargo_inasistencia" to "Aceptar el cargo por inasistencia"
)

/** Cuántos campos se nombran antes de resumir el resto con «y N más». */
private const val MAX_NAMED_FIELDS = 3

/**
 * Nombre legible de un campo; si no está en el diccionario, la clave con espacios. En una clave anidada como
 * `items.0.cantidad` se usa el último tramo que no sea un índice («Cantidad»), que es lo que la persona edita.
 */
internal fun fieldLabel(key: String): String {
    val base = key.split('.').lastOrNull { it.toIntOrNull() == null } ?: key
    return FIELD_LABELS[base] ?: base.replace('_', ' ').replaceFirstChar { it.uppercase() }
}

/** Primer mensaje por campo del `errors` de un 422 de Laravel; vacío si el cuerpo no lo trae o no es JSON. */
internal fun parseFieldErrors(body: String?): Map<String, String> {
    if (body.isNullOrBlank()) return emptyMap()
    return try {
        val errors = JsonParser.parseString(body).asJsonObject.get("errors")?.takeIf { it.isJsonObject }?.asJsonObject
            ?: return emptyMap()
        errors.entrySet().mapNotNull { (key, value) ->
            val first = if (value.isJsonArray) value.asJsonArray.firstOrNull()?.asString else value.asString
            first?.takeIf { it.isNotBlank() }?.let { key to it }
        }.toMap()
    } catch (_: Exception) {
        emptyMap()
    }
}

/** «Revisa: Correo, Teléfono.» a partir de los campos rechazados; null si no hay ninguno. */
internal fun fieldErrorsSummary(errors: Map<String, String>): String? {
    val labels = errors.keys.map { fieldLabel(it) }.distinct()
    if (labels.isEmpty()) return null
    val shown = labels.take(MAX_NAMED_FIELDS).joinToString(", ")
    val rest = labels.size - MAX_NAMED_FIELDS
    return if (rest > 0) "Revisa: $shown y $rest más." else "Revisa: $shown."
}

/**
 * Mensaje para un 422. Prioridad: el motivo que el servidor explica en español (p. ej. «el barbero ya tiene una cita
 * en ese horario»); si solo hay el resumen genérico de Laravel en inglés, los campos rechazados; y, si no, un aviso
 * genérico. `body` es el cuerpo del error, que Retrofit solo deja leer una vez.
 */
internal fun validationMessage(body: String?): String {
    val message = try {
        if (body.isNullOrBlank()) null else JsonParser.parseString(body).asJsonObject.get("message")?.asString
    } catch (_: Exception) {
        null
    }
    val spanish = message?.takeIf { it.isNotBlank() && !it.startsWith("The ") && !it.contains("(and ") }
    return spanish ?: fieldErrorsSummary(parseFieldErrors(body)) ?: "Revisa los datos capturados."
}

/** Cuerpo del error como texto; solo se puede leer una vez, así que quien lo use debe guardarlo. */
internal fun HttpException.errorBodyText(): String? = try {
    response()?.errorBody()?.string()
} catch (_: Exception) {
    null
}
