package com.urbanblade.mobile.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.theme.UrbanSpace
import java.time.LocalDate

/**
 * Encabezado único de los inicios del personal: etiqueta de rol, «Hola, nombre», fecha, actualizar y avatar.
 * Antes cada inicio lo armaba a mano y el del barbero usaba un tamaño distinto.
 */
@Composable
fun UrbanHomeHeader(
    roleLabel: String,
    userName: String,
    avatarUrl: String?,
    refreshing: Boolean,
    onRefresh: () -> Unit
) {
    UrbanPageHeader(
        title = "Hola, ${userName.substringBefore(' ')}",
        subtitle = UrbanFormat.date(LocalDate.now().toString()),
        eyebrow = roleLabel,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UrbanRefreshButton(refreshing, onRefresh)
                UrbanAvatar(userName, imageUrl = avatarUrl)
            }
        }
    )
}

/** Dato secundario de [UrbanHeroSummary]. */
data class UrbanHeroStatSpec(
    val label: String,
    val value: String,
    val icon: ImageVector,
    val tone: Color = UrbanColors.Gold
)

/**
 * Resumen destacado de un inicio sobre [UrbanHeroCard], en poco alto: etiqueta, cifra o titular (con una
 * tendencia opcional a su lado), un detalle opcional y los datos secundarios en una sola fila, sin cajas
 * de icono ni nota al pie aparte. Sustituye las copias de Administración, Recepción, Ingeniería y Barbero.
 */
@Composable
fun UrbanHeroSummary(
    label: String,
    headline: String,
    stats: List<UrbanHeroStatSpec>,
    modifier: Modifier = Modifier,
    large: Boolean = true,
    onClick: (() -> Unit)? = null,
    headlineTrailing: (@Composable () -> Unit)? = null,
    belowHeadline: (@Composable () -> Unit)? = null
) {
    UrbanHeroCard(if (onClick != null) modifier.clickable(onClick = onClick) else modifier) {
        UrbanHeroLabel(label)
        Spacer(Modifier.height(UrbanSpace.Xxs))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                headline,
                style = if (large) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.titleLarge,
                color = UrbanColors.Ink,
                modifier = Modifier.weight(1f, fill = false)
            )
            headlineTrailing?.let {
                Spacer(Modifier.width(UrbanSpace.Xs))
                it()
            }
        }
        belowHeadline?.let {
            Spacer(Modifier.height(UrbanSpace.Xxs))
            it()
        }
        Spacer(Modifier.height(UrbanSpace.Sm))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(UrbanSpace.Sm)) {
            stats.forEach { stat ->
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Icon(stat.icon, null, tint = stat.tone, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(UrbanSpace.Xs))
                    Column {
                        Text(stat.value, style = MaterialTheme.typography.titleMedium, color = UrbanColors.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(stat.label, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                    }
                }
            }
        }
    }
}
