package com.urbanblade.mobile.core.booking

/**
 * Selección de servicio/barbero hecha como invitado en el catálogo, antes de
 * que la reserva exija sesión. GuestNav y AuthenticatedNav son NavHost
 * distintos (un swap completo al cambiar AuthState, no una navegación dentro
 * del mismo grafo), así que la selección no sobrevive por argumentos de ruta
 * normales -- se guarda aquí en memoria (nunca en DataStore, es efímera a
 * propósito) y BookingScreen la consume-y-limpia en cuanto monta tras un
 * login/registro exitoso.
 */
object PendingBooking {
    var serviceId: String? = null
        private set
    var barberId: String? = null
        private set

    fun set(serviceId: String?, barberId: String?) {
        this.serviceId = serviceId
        this.barberId = barberId
    }

    /** Devuelve la selección pendiente y la limpia -- se consume una sola vez. */
    fun consume(): Pair<String?, String?> {
        val result = serviceId to barberId
        serviceId = null
        barberId = null
        return result
    }
}
