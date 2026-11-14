package com.urbanblade.mobile.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.Notifications
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
    unread: Int = 0,
    onNotifications: (() -> Unit)? = null,
    onAccount: (() -> Unit)? = null,
    onRefresh: () -> Unit
) {
    UrbanPageHeader(
        title = "Hola, ${userName.substringBefore(' ')}",
        subtitle = UrbanFormat.date(LocalDate.now().toString()),
        eyebrow = roleLabel,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UrbanRefreshButton(refreshing, onRefresh)
                // La campana y la foto están en el inicio de los cinco roles: avisos y Cuenta a un toque.
                if (onNotifications != null) UrbanBellButton(unread, onNotifications)
                UrbanAccountAvatar(userName, avatarUrl, onAccount)
            }
        }
    )
}

/** Campana de avisos con el contador de sin leer; TalkBack lee cuántos hay. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun UrbanBellButton(unread: Int, onClick: () -> Unit) {
    androidx.compose.material3.IconButton(onClick = onClick) {
        androidx.compose.material3.BadgedBox(
            badge = {
                if (unread > 0) androidx.compose.material3.Badge { Text(if (unread > 9) "9+" else unread.toString()) }
            }
        ) {
            Icon(
                androidx.compose.material.icons.Icons.Default.Notifications,
                if (unread > 0) "Notificaciones, $unread sin leer" else "Notificaciones",
                tint = UrbanColors.Gold
            )
        }
    }
}

/** La foto de la persona; con [onClick] abre su Cuenta. */
@Composable
fun UrbanAccountAvatar(name: String, imageUrl: String?, onClick: (() -> Unit)?) {
    val modifier = if (onClick != null) {
        Modifier
            .clip(androidx.compose.foundation.shape.CircleShape)
            .clickable(onClickLabel = "Abrir mi cuenta", role = androidx.compose.ui.semantics.Role.Button, onClick = onClick)
    } else Modifier
    UrbanAvatar(name, modifier, imageUrl = imageUrl)
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
