package com.urbanblade.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.urbanblade.mobile.ui.theme.UrbanColors
import kotlin.math.floor

data class UrbanBottomDestination(val route: String, val label: String, val icon: ImageVector)

/** Conserva todos los destinos visibles cuando las etiquetas necesitan más espacio. */
@Composable
fun UrbanBottomNavigation(
    destinations: List<UrbanBottomDestination>,
    selectedRoute: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (destinations.isEmpty()) return

    val labelStyle = if (destinations.size > 4) {
        MaterialTheme.typography.labelSmall
    } else {
        MaterialTheme.typography.labelMedium
    }
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val widestLabelPx = destinations.maxOf { destination ->
        textMeasurer.measure(destination.label, style = labelStyle, softWrap = false).size.width
    }
    BoxWithConstraints(
        modifier.fillMaxWidth().windowInsetsPadding(NavigationBarDefaults.windowInsets)
    ) {
        // Geometría de NavigationBar (Material 3): 8 dp de separación entre pestañas y 4 dp de aire por lado dentro
        // de cada una. Si la etiqueta más ancha no cabe en esa ranura, se pasa a la cuadrícula en vez de cortarla.
        // La cuadrícula de varias filas lleva 8 dp de relleno exterior por lado y usa su propio margen.
        val itemPaddingPx = with(density) { 8.dp.toPx() }
        val itemSpacingPx = with(density) { 8.dp.toPx() }
        val gridPaddingPx = with(density) { 16.dp.toPx() }
        val minimumItemPx = maxOf(widestLabelPx + itemPaddingPx, with(density) { 48.dp.toPx() })
        val availableWidthPx = constraints.maxWidth.toFloat()
        val slotPx = (availableWidthPx - itemSpacingPx * (destinations.size - 1)) / destinations.size
        if (minimumItemPx <= slotPx) {
            NavigationBar(
                containerColor = Color.Transparent,
                tonalElevation = 0.dp,
                windowInsets = WindowInsets(0, 0, 0, 0)
            ) {
                destinations.forEach { destination ->
                    NavigationBarItem(
                        selected = selectedRoute == destination.route,
                        onClick = { onSelect(destination.route) },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = {
                            Text(destination.label, style = labelStyle, maxLines = 1, softWrap = false)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = UrbanColors.OnGold,
                            selectedTextColor = UrbanColors.GoldText,
                            indicatorColor = UrbanColors.Gold,
                            unselectedIconColor = UrbanColors.Muted,
                            unselectedTextColor = UrbanColors.Muted
                        )
                    )
                }
            }
        } else {
            val gridWidthPx = availableWidthPx - gridPaddingPx
            val columns = floor(gridWidthPx / minimumItemPx).toInt().coerceIn(1, minOf(3, destinations.size))
            Column(
                Modifier.fillMaxWidth().selectableGroup().padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                destinations.chunked(columns).forEach { row ->
                    Row(Modifier.fillMaxWidth()) {
                        row.forEach { destination ->
                            val selected = selectedRoute == destination.route
                            Column(
                                Modifier.weight(1f)
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .selectable(
                                        selected = selected,
                                        role = Role.Tab,
                                        onClick = { onSelect(destination.route) }
                                    )
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    Modifier.background(
                                        if (selected) UrbanColors.Gold else Color.Transparent,
                                        RoundedCornerShape(16.dp)
                                    ).padding(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        destination.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = if (selected) UrbanColors.OnGold else UrbanColors.Muted
                                    )
                                }
                                Text(
                                    destination.label,
                                    style = labelStyle,
                                    color = if (selected) UrbanColors.GoldText else UrbanColors.Muted,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}
