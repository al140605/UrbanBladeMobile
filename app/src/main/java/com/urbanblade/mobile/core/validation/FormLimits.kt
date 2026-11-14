package com.urbanblade.mobile.core.validation

/**
 * Topes de longitud que el servidor aplica (reglas `max:` de los FormRequest y controladores de `barber`). La app los
 * respeta al escribir para no hacer un viaje que el servidor rechazaría con un 422. Si el backend cambia un tope, se
 * cambia aquí y en `FormLimitsTest`, que fija los valores.
 */
object FormLimits {
    /** `name` en registro y perfil: `max:255`. */
    const val NAME_MAX = 255

    /** `email` en registro y perfil: `max:255`. */
    const val EMAIL_MAX = 255

    /** `telefono` en el perfil: `max:30`. */
    const val PHONE_MAX = 30

    /** `notas` de una cita: `max:1000`. */
    const val NOTES_MAX = 1000

    /** `comment` en el muro y en reseñas: `max:500`. */
    const val COMMENT_MAX = 500

    /** `descripcion` de recursos (trabajos, servicios…): `max:1000`. */
    const val DESCRIPTION_MAX = 1000

    /** `especialidades` del perfil del barbero (`POST barber/profile`): `max:1000`. */
    const val SPECIALTIES_MAX = 1000

    /** ¿Conviene mostrar un contador? Solo cuando queda poco, para no saturar el formulario. */
    fun nearLimit(text: String, max: Int): Boolean = text.length >= max * 4 / 5
}
