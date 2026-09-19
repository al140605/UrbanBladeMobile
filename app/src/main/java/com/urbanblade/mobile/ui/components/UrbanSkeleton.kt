package com.urbanblade.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.valentinilk.shimmer.shimmer

/**
 * Esqueletos de carga con brillo (compose-shimmer) que reemplazan la barra de
 * progreso lineal: el usuario ve la forma del contenido que viene en lugar de una
 * pantalla vacía. Los colores salen de UrbanColors, así respetan los 4 temas.
 */
@Composable
fun UrbanSkeletonBlock(
    modifier: Modifier = Modifier,
    height: Dp = 14.dp,
    corner: Dp = 8.dp
) {
    Box(
        modifier
            .height(height)
            .clip(RoundedCornerShape(corner))
            .background(UrbanColors.Muted.copy(alpha = 0.18f))
    )
}

@Composable
fun UrbanSkeletonCard(modifier: Modifier = Modifier) {
    UrbanCard(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(UrbanColors.Muted.copy(alpha = 0.18f))
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                UrbanSkeletonBlock(Modifier.fillMaxWidth(0.6f), height = 16.dp)
                UrbanSkeletonBlock(Modifier.fillMaxWidth(0.9f), height = 12.dp)
            }
        }
    }
}

/** Lista de tarjetas esqueleto; el brillo recorre todo el bloque a la vez. */
@Composable
fun UrbanSkeletonList(count: Int = 3, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .shimmer()
            .semantics { contentDescription = "Cargando contenido" },
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(count) { UrbanSkeletonCard() }
    }
}

/**
 * Indicador de carga para listas: esqueleto solo mientras todavía no hay nada que
 * mostrar (primera carga); si ya hay contenido y `busy` viene de una acción (guardar,
 * borrar, refrescar), basta la barra fina para no tapar ni saltar el contenido.
 */
fun LazyListScope.urbanLoadingItem(busy: Boolean, isEmpty: Boolean, skeletonCount: Int = 3) {
    if (!busy) return
    item {
        if (isEmpty) {
            UrbanSkeletonList(skeletonCount)
        } else {
            LinearProgressIndicator(Modifier.fillMaxWidth(), color = UrbanColors.Gold)
        }
    }
}
