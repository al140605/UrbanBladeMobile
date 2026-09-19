package com.urbanblade.mobile.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.urbanblade.mobile.ui.theme.UrbanColors
import kotlinx.coroutines.launch

/**
 * Palomita de éxito animada (círculo que "rebota" y trazo que se dibuja). Está dibujada con
 * Canvas, sin archivos externos. Las animaciones de Compose respetan la escala de animación del
 * sistema, así que con "Quitar animaciones" activado aparece ya terminada.
 */
@Composable
fun UrbanSuccessCheck(modifier: Modifier = Modifier, diameter: Dp = 112.dp) {
    val scale = remember { Animatable(0.6f) }
    val progress = remember { Animatable(0f) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        launch { scale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow)) }
        progress.animateTo(1f, tween(durationMillis = 480, delayMillis = 160))
    }

    val gold = UrbanColors.Gold
    Canvas(
        modifier
            .size(diameter)
            .scale(scale.value)
            .semantics { contentDescription = "Confirmado" }
    ) {
        val stroke = size.minDimension * 0.06f
        drawCircle(gold.copy(alpha = 0.16f))
        drawCircle(gold, radius = (size.minDimension - stroke) / 2f, style = Stroke(width = stroke))

        val w = size.width
        val h = size.height
        val check = Path().apply {
            moveTo(w * 0.29f, h * 0.53f)
            lineTo(w * 0.44f, h * 0.68f)
            lineTo(w * 0.72f, h * 0.37f)
        }
        val measure = PathMeasure().apply { setPath(check, false) }
        val partial = Path()
        measure.getSegment(0f, measure.length * progress.value, partial, true)
        drawPath(
            partial,
            gold,
            style = Stroke(width = stroke * 1.4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/**
 * Pantalla completa de confirmación: palomita animada, mensaje, resumen y acción principal.
 */
@Composable
fun UrbanSuccessScreen(
    title: String,
    message: String,
    details: List<Pair<String, String>>,
    primaryText: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        UrbanSuccessCheck()
        Spacer(Modifier.height(24.dp))
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            color = UrbanColors.Ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = UrbanColors.Muted,
            textAlign = TextAlign.Center
        )
        if (details.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            UrbanCard(Modifier.fillMaxWidth()) {
                details.forEachIndexed { index, (label, value) ->
                    if (index > 0) Spacer(Modifier.height(10.dp))
                    UrbanKeyValue(label, value)
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        UrbanPrimaryButton(text = primaryText, onClick = onPrimary, modifier = Modifier.fillMaxWidth())
    }
}
