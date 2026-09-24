package com.urbanblade.mobile.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.urbanblade.mobile.R
import com.urbanblade.mobile.ui.theme.UrbanColors

/*
 * Patrón visual de los módulos (el mismo del acceso y del inicio del administrador):
 * encabezado con eyebrow dorado y título grande, una tarjeta héroe con la foto de la
 * barbería y el dato principal, filas de "atención" con su tono y estados de vacío/error
 * con las mascotas de la marca (Bruno para errores y datos técnicos, Nava para vacíos).
 */

/**
 * Esqueleto común de un módulo: barra con volver y actualizar, encabezado con eyebrow y el
 * contenido en una lista. El título vive en el encabezado (no se repite en la barra).
 */
@Composable
fun UrbanModuleScreen(
    eyebrow: String,
    title: String,
    subtitle: String?,
    onBack: () -> Unit,
    onRefresh: (() -> Unit)? = null,
    refreshing: Boolean = false,
    content: LazyListScope.() -> Unit
) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            UrbanTopBar("", onBack) {
                if (onRefresh != null) {
                    if (refreshing) {
                        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = UrbanColors.Gold)
                        }
                    } else {
                        IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "Actualizar") }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { UrbanPageHeader(title = title, subtitle = subtitle, eyebrow = eyebrow) }
            content()
        }
    }
}

/** Tarjeta principal del módulo: foto de la barbería con degradado oscuro y borde dorado. */
@Composable
fun UrbanHeroCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = MaterialTheme.shapes.large
    Box(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, UrbanColors.Gold.copy(alpha = 0.45f), shape)
    ) {
        Image(
            painter = painterResource(R.drawable.auth_barbershop_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
        Box(
            Modifier
                .matchParentSize()
                .background(Brush.horizontalGradient(listOf(Color(0xF2161210), Color(0xD9110F0E), Color(0x74110F0E))))
        )
        Column(Modifier.padding(18.dp), content = content)
    }
}

/** Etiqueta dorada en mayúsculas dentro de la tarjeta héroe. */
@Composable
fun UrbanHeroLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = UrbanColors.Gold,
        modifier = Modifier.semantics { heading() }
    )
}

/** Dato secundario de la tarjeta héroe: icono en caja dorada, valor y etiqueta. */
@Composable
fun UrbanHeroStat(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier, tone: Color = UrbanColors.Gold) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tone.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = tone, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(9.dp))
        Column {
            Text(value, style = MaterialTheme.typography.titleLarge, color = UrbanColors.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Fila de algo que conviene revisar, con el color de su gravedad. Sin [onClick] es solo informativa. */
@Composable
fun UrbanAttentionRow(
    icon: ImageVector,
    text: String,
    subtitle: String?,
    tone: Color,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    UrbanCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tone.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = tone, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(text, style = MaterialTheme.typography.titleSmall, color = UrbanColors.Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis) }
            }
            when {
                trailing != null -> { Spacer(Modifier.width(8.dp)); trailing() }
                onClick != null -> Icon(Icons.Default.ChevronRight, null, tint = UrbanColors.Muted)
            }
        }
    }
}

/** Qué mascota acompaña un estado: Bruno cuando algo falló, Nava cuando todavía no hay datos. */
enum class UrbanStateKind { ERROR, EMPTY }

/** Estado de error o vacío con mascota de la marca y, si aplica, una acción para reintentar. */
@Composable
fun UrbanMascotState(
    kind: UrbanStateKind,
    title: String,
    subtitle: String?,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val image = if (kind == UrbanStateKind.ERROR) R.drawable.mascot_bruno_error else R.drawable.mascot_nava_empty
    Column(
        Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(image),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.height(150.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = UrbanColors.Ink, textAlign = TextAlign.Center)
        subtitle?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted, textAlign = TextAlign.Center)
        }
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(14.dp))
            UrbanOutlineButton(text = actionLabel, onClick = onAction, icon = Icons.Default.Refresh)
        }
    }
}

/** Mosaico de acceso a un módulo, como las acciones rápidas del inicio del administrador. */
@Composable
fun UrbanModuleTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    UrbanCard(modifier, onClick = onClick) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(UrbanColors.Gold.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = UrbanColors.Gold, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, color = UrbanColors.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** Lista de mosaicos en dos columnas. */
@Composable
fun UrbanModuleGrid(tiles: List<Triple<String, String, ImageVector>>, routes: List<String>, onNavigate: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        tiles.zip(routes).chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (tile, route) ->
                    UrbanModuleTile(tile.first, tile.second, tile.third, { onNavigate(route) }, Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
