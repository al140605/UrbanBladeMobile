package com.urbanblade.mobile.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Spa
import androidx.compose.ui.graphics.vector.ImageVector
import com.urbanblade.mobile.data.model.BarberItem

/** Tipo de servicio según su nombre, para darle un ícono propio en el catálogo y la reserva. */
enum class ServiceKind { COMBO, BARBA, AFEITADO, INFANTIL, TRATAMIENTO, CORTE }

/** El orden importa: "Combo Corte + Barba" es combo aunque mencione barba y corte. */
fun serviceKind(name: String): ServiceKind {
    val n = name.lowercase()
    return when {
        "combo" in n || "paquete" in n -> ServiceKind.COMBO
        "infantil" in n || "niño" in n || "nino" in n -> ServiceKind.INFANTIL
        "afeitado" in n || "rasurado" in n -> ServiceKind.AFEITADO
        "barba" in n || "bigote" in n -> ServiceKind.BARBA
        "tratamiento" in n || "facial" in n || "mascarilla" in n || "masaje" in n -> ServiceKind.TRATAMIENTO
        else -> ServiceKind.CORTE
    }
}

/**
 * El Muro de Inspiración manda el id del *usuario* del barbero (barberUser->id en barber), pero la
 * reserva trabaja con el id del *perfil* de barbero. Acepta cualquiera de los dos (o el slug) y
 * devuelve el id de perfil; null si no corresponde a ningún barbero disponible.
 */
fun resolveBarberId(barbers: List<BarberItem>, ref: String): String? {
    if (ref.isBlank()) return null
    return barbers.firstOrNull { it.id == ref }?.id
        ?: barbers.firstOrNull { it.user?.id == ref || it.slug == ref }?.id
}

fun serviceIcon(name: String): ImageVector = when (serviceKind(name)) {
    ServiceKind.COMBO -> Icons.Default.AutoAwesome
    ServiceKind.INFANTIL -> Icons.Default.ChildCare
    ServiceKind.AFEITADO, ServiceKind.BARBA -> Icons.Default.Face
    ServiceKind.TRATAMIENTO -> Icons.Default.Spa
    ServiceKind.CORTE -> Icons.Default.ContentCut
}
