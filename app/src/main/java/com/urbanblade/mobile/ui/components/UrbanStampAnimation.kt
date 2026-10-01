package com.urbanblade.mobile.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urbanblade.mobile.ui.theme.UrbanColors
import kotlinx.coroutines.launch

/**
 * Un sello de goma estilo "Premium" que impacta sobre la pantalla.
 * Ideal para dar feedback cuando una cita cambia de estado.
 */
@Composable
fun UrbanStampAnimation(
    text: String,
    color: Color = UrbanColors.Success,
    modifier: Modifier = Modifier
) {
    val scale = remember { Animatable(3f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Efecto de sello golpeando la superficie
        launch {
            alpha.animateTo(0.85f, tween(150, easing = LinearOutSlowInEasing))
        }
        scale.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium))
    }

    Box(
        modifier = modifier
            .scale(scale.value)
            .alpha(alpha.value)
            .rotate(-8f)
            .border(4.dp, color.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
            .padding(horizontal = 20.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.headlineMedium,
            color = color,
            fontWeight = FontWeight.Black,
            letterSpacing = 4.sp
        )
    }
}
