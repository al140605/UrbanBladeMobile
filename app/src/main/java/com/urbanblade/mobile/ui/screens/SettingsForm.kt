package com.urbanblade.mobile.ui.screens

/** Valores del formulario de configuración tal como están escritos (todos texto, como en pantalla). */
data class SettingsDraft(
    val nombre: String = "",
    val direccion: String = "",
    val telefono: String = "",
    val apertura: String = "",
    val cierre: String = "",
    val politica: String = "24",
    val instagram: String = "",
    val facebook: String = "",
    val tiktok: String = "",
    val clabe: String = "",
    val banco: String = "",
    val beneficiario: String = "",
    val concepto: String = ""
)

/** Nombres de campo para indexar los errores. */
object SettingsField {
    const val NOMBRE = "nombre"
    const val DIRECCION = "direccion"
    const val TELEFONO = "telefono"
    const val APERTURA = "apertura"
    const val CIERRE = "cierre"
    const val POLITICA = "politica"
    const val INSTAGRAM = "instagram"
    const val FACEBOOK = "facebook"
    const val TIKTOK = "tiktok"
    const val CLABE = "clabe"
    const val BANCO = "banco"
    const val BENEFICIARIO = "beneficiario"
    const val CONCEPTO = "concepto"
}

private val TIME = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")

private fun tooLong(value: String, max: Int) = if (value.length > max) "Máximo $max caracteres." else null

/**
 * Reglas del servidor (SettingController::update) más la de la CLABE, que en México siempre son 18
 * dígitos. Devuelve un error por campo; un mapa vacío significa que se puede guardar.
 */
fun validateSettings(d: SettingsDraft): Map<String, String> {
    val errors = mutableMapOf<String, String>()
    fun put(field: String, message: String?) {
        if (message != null) errors[field] = message
    }

    put(SettingsField.NOMBRE, if (d.nombre.isBlank()) "Escribe el nombre del negocio." else tooLong(d.nombre, 255))
    put(SettingsField.DIRECCION, tooLong(d.direccion, 255))
    put(SettingsField.TELEFONO, tooLong(d.telefono, 30))

    val opening = d.apertura.trim()
    val closing = d.cierre.trim()
    put(SettingsField.APERTURA, if (opening.isNotEmpty() && !TIME.matches(opening)) "Usa el formato HH:mm." else null)
    put(SettingsField.CIERRE, if (closing.isNotEmpty() && !TIME.matches(closing)) "Usa el formato HH:mm." else null)
    if (SettingsField.APERTURA !in errors && SettingsField.CIERRE !in errors && opening.isNotEmpty() && closing.isNotEmpty() && closing <= opening) {
        errors[SettingsField.CIERRE] = "El cierre debe ser después de la apertura."
    }

    val policy = d.politica.trim().toIntOrNull()
    put(SettingsField.POLITICA, if (policy == null || policy !in 1..168) "Entre 1 y 168 horas." else null)

    put(SettingsField.INSTAGRAM, tooLong(d.instagram, 255))
    put(SettingsField.FACEBOOK, tooLong(d.facebook, 255))
    put(SettingsField.TIKTOK, tooLong(d.tiktok, 255))

    val clabe = d.clabe.trim()
    put(SettingsField.CLABE, if (clabe.isNotEmpty() && !(clabe.length == 18 && clabe.all { it.isDigit() })) "La CLABE tiene 18 dígitos." else null)
    put(SettingsField.BANCO, tooLong(d.banco, 100))
    put(SettingsField.BENEFICIARIO, tooLong(d.beneficiario, 150))
    put(SettingsField.CONCEPTO, tooLong(d.concepto, 100))
    return errors
}
