package com.urbanblade.mobile.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.platform.LocalDensity
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
 * tendencia opcional a su lado), un detalle opcional y datos secundarios que se apilan con poco ancho
 * o fuente grande. En tamaño normal conserva una sola fila, sin cajas
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
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val stacked = maxWidth < 320.dp || LocalDensity.current.fontScale >= 1.3f
            Column {
                UrbanHeroLabel(label)
                Spacer(Modifier.height(UrbanSpace.Xxs))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        headline,
                        style = if (large) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.titleLarge,
                        color = UrbanColors.Ink,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (!stacked) {
                        headlineTrailing?.let {
                            Spacer(Modifier.width(UrbanSpace.Xs))
                            it()
                        }
                    }
                }
                if (stacked) {
                    headlineTrailing?.let {
                        Spacer(Modifier.height(UrbanSpace.Xs))
                        it()
                    }
                }
                belowHeadline?.let {
                    Spacer(Modifier.height(UrbanSpace.Xxs))
                    it()
                }
                Spacer(Modifier.height(UrbanSpace.Sm))
                if (stacked) {
                    Column(verticalArrangement = Arrangement.spacedBy(UrbanSpace.Sm)) {
                        stats.forEach { stat -> HeroSummaryStat(stat, Modifier.fillMaxWidth()) }
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(UrbanSpace.Sm)) {
                        stats.forEach { stat -> HeroSummaryStat(stat, Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroSummaryStat(stat: UrbanHeroStatSpec, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(stat.icon, null, tint = stat.tone, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(UrbanSpace.Xs))
        Column(Modifier.weight(1f)) {
            Text(stat.value, style = MaterialTheme.typography.titleMedium, color = UrbanColors.Ink)
            Text(stat.label, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
        }
    }
}
