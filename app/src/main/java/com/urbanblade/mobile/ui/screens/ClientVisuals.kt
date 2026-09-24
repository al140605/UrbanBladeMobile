package com.urbanblade.mobile.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Spa
import androidx.compose.ui.graphics.vector.ImageVector

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

fun serviceIcon(name: String): ImageVector = when (serviceKind(name)) {
    ServiceKind.COMBO -> Icons.Default.AutoAwesome
    ServiceKind.INFANTIL -> Icons.Default.ChildCare
    ServiceKind.AFEITADO, ServiceKind.BARBA -> Icons.Default.Face
    ServiceKind.TRATAMIENTO -> Icons.Default.Spa
    ServiceKind.CORTE -> Icons.Default.ContentCut
}
