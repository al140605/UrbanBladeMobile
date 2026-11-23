package com.urbanblade.mobile.core.access

/** Experiencia visual compartida con web y API. Los permisos los valida Laravel. */
object UserExperience {
    /**
     * Roles vigentes, del más al menos poderoso. El rol «ingeniero» se retiró (decisión del propietario, 23-nov-2026):
     * una cuenta vieja que aún lo traiga no coincide con ninguno y se trata como cuenta sin rol conocido.
     */
    val priority = listOf("administrador", "recepcionista", "barbero", "cliente")
    fun primaryRole(roles: List<String>): String = priority.firstOrNull { it in roles } ?: "guest"
    /** Falso para una cuenta sin ningún rol vigente (p. ej. una que solo conserva el rol «ingeniero» retirado). */
    fun hasKnownRole(roles: List<String>): Boolean = primaryRole(roles) != "guest"
    fun isClient(roles: List<String>): Boolean = primaryRole(roles) == "cliente"
    fun canBook(roles: List<String>): Boolean = primaryRole(roles) in listOf("administrador", "recepcionista", "cliente")
    fun label(roles: List<String>): String = when (primaryRole(roles)) {
        "administrador" -> "Administrador"
        "recepcionista" -> "Recepción"
        "barbero" -> "Barbero"
        "cliente" -> "Cliente"
        else -> "Cuenta"
    }
}
