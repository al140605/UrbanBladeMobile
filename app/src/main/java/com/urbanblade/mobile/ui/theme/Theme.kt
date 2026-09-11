package com.urbanblade.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

/**
 * UrbanBlade — Sastrería Nocturna.
 * Tokens portados de la identidad visual del frontend web.
 */
object UrbanColors {
    val Background = Color(0xFF0A0A0A)
    val Surface = Color(0xFF111111)
    val Card = Color(0xFF161616)
    val CardAlt = Color(0xFF1F1F1F)
    val Line = Color(0xFF2C2C2C)
    val Gold = Color(0xFFD4AF37)
    val GoldDim = Color(0xFFA8842C)
    val Ink = Color(0xFFF2F2F2)
    val Muted = Color(0xFF9C9C9C)
    val Success = Color(0xFF4BB983)
    val Warning = Color(0xFFE2A84D)
    val Danger = Color(0xFFE36D6D)
    val Info = Color(0xFF72A5D8)
}

private val UrbanBladeColors = darkColorScheme(
    primary = UrbanColors.Gold,
    onPrimary = Color(0xFF0A0A0A),
    primaryContainer = Color(0xFF332B12),
    onPrimaryContainer = Color(0xFFF4D978),
    secondary = Color(0xFFC9B979),
    onSecondary = Color(0xFF111111),
    secondaryContainer = Color(0xFF292519),
    onSecondaryContainer = Color(0xFFE9DDAF),
    tertiary = Color(0xFFC1703D),
    background = UrbanColors.Background,
    onBackground = UrbanColors.Ink,
    surface = UrbanColors.Surface,
    onSurface = UrbanColors.Ink,
    surfaceVariant = UrbanColors.CardAlt,
    onSurfaceVariant = UrbanColors.Muted,
    outline = UrbanColors.Line,
    outlineVariant = Color(0xFF222222),
    error = UrbanColors.Danger,
    errorContainer = Color(0xFF3A1D1D),
    onErrorContainer = Color(0xFFFFC9C9)
)

private val UrbanTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.5f).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 31.sp,
        lineHeight = 37.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 27.sp,
        lineHeight = 33.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        letterSpacing = 0.2.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        letterSpacing = 0.3.sp
    )
)

private val UrbanShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun UrbanBladeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = UrbanBladeColors,
        typography = UrbanTypography,
        shapes = UrbanShapes,
        content = content
    )
}
