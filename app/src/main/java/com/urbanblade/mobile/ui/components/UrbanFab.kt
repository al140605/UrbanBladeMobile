package com.urbanblade.mobile.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.urbanblade.mobile.ui.theme.UrbanColors

/**
 * Botón flotante de la acción principal de una pantalla (nueva cita, campaña, servicio…).
 *
 * Es el único botón flotante de la app: mismo color, misma forma y mismo texto en negrita.
 * Con [expanded] en false se contrae a solo el icono, de modo que al desplazar la lista no
 * tape el contenido; el icono conserva el nombre de la acción para los lectores de pantalla.
 */
@Composable
fun UrbanFab(text: String, icon: ImageVector, expanded: Boolean, onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        expanded = expanded,
        containerColor = UrbanColors.Gold,
        contentColor = UrbanColors.OnGold,
        icon = { Icon(icon, contentDescription = if (expanded) null else text) },
        text = { Text(text, fontWeight = FontWeight.Bold) }
    )
}

/** El botón flotante se expande en lo alto de la lista y se contrae en cuanto se desplaza. */
@Composable
fun rememberFabExpanded(listState: LazyListState): Boolean {
    val expanded by remember(listState) { derivedStateOf { !listState.canScrollBackward } }
    return expanded
}
