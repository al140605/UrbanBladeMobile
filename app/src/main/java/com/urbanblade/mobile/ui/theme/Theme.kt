package com.urbanblade.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val UrbanBladeColors = darkColorScheme(
    primary = Color(0xFFD6B46A),
    onPrimary = Color(0xFF17130A),
    secondary = Color(0xFFB9A77A),
    background = Color(0xFF0B0B0D),
    onBackground = Color(0xFFF2EEE5),
    surface = Color(0xFF151519),
    onSurface = Color(0xFFF2EEE5),
    surfaceVariant = Color(0xFF202027),
    onSurfaceVariant = Color(0xFFC8C3B8),
    error = Color(0xFFFF7B7B)
)

@Composable
fun UrbanBladeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = UrbanBladeColors,
        content = content
    )
}
