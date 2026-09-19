package com.urbanblade.mobile.ui.theme

import com.urbanblade.mobile.R

/**
 * Expresión de la mascota según el momento de la pantalla. No todas las mascotas tienen todas
 * las expresiones: cuando falta una, se usa la principal.
 */
enum class MascotMood { DEFAULT, WELCOME, SUCCESS, ERROR }

fun UrbanTheme.mascot(mood: MascotMood): Int = when (this) {
    UrbanTheme.NOIR, UrbanTheme.ACERO -> when (mood) {
        MascotMood.WELCOME -> R.drawable.mascot_bladebot_welcome
        MascotMood.SUCCESS -> R.drawable.mascot_bladebot_success
        else -> R.drawable.mascot_bladebot
    }
    UrbanTheme.SALON -> when (mood) {
        MascotMood.ERROR -> R.drawable.mascot_nava_lost
        else -> R.drawable.mascot_nava
    }
    UrbanTheme.LIBRETA -> when (mood) {
        MascotMood.ERROR -> R.drawable.mascot_bruno_error
        else -> R.drawable.mascot_bruno
    }
}
