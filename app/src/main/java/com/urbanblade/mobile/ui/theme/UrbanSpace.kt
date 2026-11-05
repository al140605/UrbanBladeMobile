package com.urbanblade.mobile.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Escala de espaciado de UrbanBlade: retícula de 4 dp, sin valores intermedios.
 *
 * Los radios ya viven en `MaterialTheme.shapes` (8, 12, 16 y 24 dp); aquí se define el ritmo
 * de márgenes y separaciones para que todas las pantallas respiren igual. Una separación que
 * no esté en la escala se redondea al paso más cercano, no se inventa un valor nuevo.
 */
object UrbanSpace {
    /** Separación mínima entre un icono y su texto, o entre líneas muy pegadas. */
    val Xxs: Dp = 4.dp

    /** Separación entre elementos del mismo grupo (pastillas, chips, filas de datos). */
    val Xs: Dp = 8.dp

    /** Separación interna de una tarjeta o entre un título y su contenido. */
    val Sm: Dp = 12.dp

    /** Separación entre bloques de una pantalla. */
    val Md: Dp = 16.dp

    /** Separación entre secciones. */
    val Lg: Dp = 24.dp

    /** Aire al final de una pantalla o alrededor de un estado destacado. */
    val Xl: Dp = 32.dp

    /** Margen lateral de toda pantalla; sustituye a los 18 dp que cada pantalla escribía a mano. */
    val Gutter: Dp = Md

    /** Espacio inferior de una pantalla que lleva un botón flotante, para que no tape el último elemento. */
    val FabClearance: Dp = 88.dp
}
