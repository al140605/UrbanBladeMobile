package com.urbanblade.mobile.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
 * Resumen destacado de un inicio sobre [UrbanHeroCard]: etiqueta, cifra o titular, un espacio opcional
 * (tendencia, próxima cita), los datos secundarios en fila y una nota al pie opcional. Sustituye las
 * copias de Administración, Recepción, Ingeniería y Barbero con el mismo ritmo en todas.
 */
@Composable
fun UrbanHeroSummary(
    label: String,
    headline: String,
    stats: List<UrbanHeroStatSpec>,
    modifier: Modifier = Modifier,
    large: Boolean = true,
    onClick: (() -> Unit)? = null,
    belowHeadline: (@Composable () -> Unit)? = null,
    footer: String? = null
) {
    UrbanHeroCard(if (onClick != null) modifier.clickable(onClick = onClick) else modifier) {
        UrbanHeroLabel(label)
        Spacer(Modifier.height(UrbanSpace.Xxs))
        Text(
            headline,
            style = if (large) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineSmall,
            color = UrbanColors.Ink
        )
        belowHeadline?.let {
            Spacer(Modifier.height(UrbanSpace.Xs))
            it()
        }
        Spacer(Modifier.height(UrbanSpace.Md))
        HorizontalDivider(color = UrbanColors.Ink.copy(alpha = 0.22f))
        Spacer(Modifier.height(UrbanSpace.Sm))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(UrbanSpace.Sm)) {
            stats.forEach { UrbanHeroStat(it.label, it.value, it.icon, Modifier.weight(1f), tone = it.tone) }
        }
        footer?.let {
            Spacer(Modifier.height(UrbanSpace.Sm))
            Text(it, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
        }
    }
}
