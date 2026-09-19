package com.urbanblade.mobile.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.urbanblade.mobile.ui.theme.UrbanColors
import java.util.Locale

/**
 * Gráficos propios de analítica, dibujados con Canvas (sin dependencias extra) y con los colores
 * del tema activo. Se animan al aparecer; con "Quitar animaciones" del sistema salen completos.
 */
private fun chartPalette(): List<Color> = listOf(
    UrbanColors.Gold,
    UrbanColors.Info,
    UrbanColors.Success,
    UrbanColors.Warning,
    UrbanColors.Danger
)

private fun formatNumber(value: Double): String =
    if (value >= 1000 || value == value.toLong().toDouble()) {
        String.format(Locale("es", "MX"), "%,.0f", value)
    } else {
        String.format(Locale("es", "MX"), "%,.1f", value)
    }

/** Serie de tiempo: línea con área degradada y puntos. Requiere al menos 2 valores. */
@Composable
fun UrbanLineChart(
    labels: List<String>,
    values: List<Double>,
    modifier: Modifier = Modifier,
    color: Color = UrbanColors.Gold
) {
    if (values.size < 2) return
    val progress = remember(values) { Animatable(0f) }
    LaunchedEffect(values) { progress.animateTo(1f, tween(durationMillis = 800)) }

    val lineColor = color
    val gridColor = UrbanColors.Line
    val summary = "Gráfica de línea de ${values.size} puntos, de ${formatNumber(values.first())} a ${formatNumber(values.last())}"

    Column(modifier.fillMaxWidth().semantics { contentDescription = summary }) {
        Canvas(Modifier.fillMaxWidth().height(120.dp)) {
            val padTop = 8.dp.toPx()
            val padBottom = 8.dp.toPx()
            val padH = 6.dp.toPx()
            val plotW = size.width - padH * 2
            val plotH = size.height - padTop - padBottom
            val min = minOf(0.0, values.min())
            val max = values.max().takeIf { it > min } ?: (min + 1.0)

            fun x(i: Int) = padH + plotW * i / (values.size - 1)
            fun y(v: Double) = padTop + plotH * (1f - ((v - min) / (max - min)).toFloat())

            repeat(3) { i ->
                val gy = padTop + plotH * i / 2f
                drawLine(gridColor, Offset(padH, gy), Offset(size.width - padH, gy), strokeWidth = 1.dp.toPx())
            }

            val line = Path().apply {
                values.forEachIndexed { i, v -> if (i == 0) moveTo(x(i), y(v)) else lineTo(x(i), y(v)) }
            }
            val area = Path().apply {
                addPath(line)
                lineTo(x(values.lastIndex), padTop + plotH)
                lineTo(x(0), padTop + plotH)
                close()
            }

            clipRect(left = 0f, top = 0f, right = size.width * progress.value, bottom = size.height) {
                drawPath(
                    area,
                    Brush.verticalGradient(
                        listOf(lineColor.copy(alpha = 0.32f), lineColor.copy(alpha = 0f)),
                        startY = padTop,
                        endY = padTop + plotH
                    )
                )
                drawPath(
                    line,
                    lineColor,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                if (values.size <= 14) {
                    values.forEachIndexed { i, v -> drawCircle(lineColor, radius = 3.5.dp.toPx(), center = Offset(x(i), y(v))) }
                }
            }
        }
        if (labels.size >= 2) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(labels.first(), style = MaterialTheme.typography.labelSmall, color = UrbanColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                Spacer(Modifier.width(8.dp))
                Text(labels.last(), style = MaterialTheme.typography.labelSmall, color = UrbanColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            }
        }
    }
}

/** Distribución: dona con leyenda de porcentajes. Ignora valores no positivos. */
@Composable
fun UrbanDonutChart(
    labels: List<String>,
    values: List<Double>,
    modifier: Modifier = Modifier
) {
    val total = values.filter { it > 0 }.sum()
    if (total <= 0.0) return
    val progress = remember(values) { Animatable(0f) }
    LaunchedEffect(values) { progress.animateTo(1f, tween(durationMillis = 800)) }
    val palette = chartPalette()

    Row(
        modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Gráfica de dona con ${values.size} categorías" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Canvas(Modifier.size(112.dp)) {
            val stroke = 20.dp.toPx()
            val diameter = size.minDimension - stroke
            var start = -90f
            values.forEachIndexed { i, v ->
                if (v <= 0) return@forEachIndexed
                val fullSweep = (v / total * 360.0).toFloat()
                drawArc(
                    color = palette[i % palette.size],
                    startAngle = start,
                    sweepAngle = (fullSweep - 1.5f).coerceAtLeast(0f) * progress.value,
                    useCenter = false,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(diameter, diameter),
                    style = Stroke(width = stroke)
                )
                start += fullSweep
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            values.forEachIndexed { i, v ->
                if (v <= 0) return@forEachIndexed
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(palette[i % palette.size])
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        labels.getOrNull(i).orEmpty().ifBlank { "Categoría ${i + 1}" },
                        style = MaterialTheme.typography.bodySmall,
                        color = UrbanColors.Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        String.format(Locale("es", "MX"), "%.0f%%", v / total * 100),
                        style = MaterialTheme.typography.bodySmall,
                        color = UrbanColors.Muted
                    )
                }
            }
        }
    }
}

private fun defaultFormat(value: Double): String = formatNumber(value)

/**
 * Barras verticales: resalta la más alta (o la que se toque) y muestra su valor arriba, en lugar de
 * rotular cada barra. Con muchas barras solo se rotulan algunas para que el eje no se amontone.
 */
@Composable
fun UrbanBarChart(
    labels: List<String>,
    values: List<Double>,
    modifier: Modifier = Modifier,
    color: Color = UrbanColors.Gold,
    format: (Double) -> String = ::defaultFormat
) {
    if (values.isEmpty() || values.all { it <= 0.0 }) return
    val progress = remember(values) { Animatable(0f) }
    LaunchedEffect(values) { progress.animateTo(1f, tween(durationMillis = 700)) }
    var selected by remember(values) { mutableStateOf<Int?>(null) }
    val max = values.max()
    val focus = selected ?: values.indexOf(max)
    val step = if (values.size <= 8) 1 else (values.size + 5) / 6

    Column(
        modifier.fillMaxWidth().semantics {
            contentDescription = "Gráfica de barras con ${values.size} valores; el más alto es ${format(max)}"
        }
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                labels.getOrElse(focus) { "" },
                style = MaterialTheme.typography.labelMedium,
                color = UrbanColors.Muted,
                modifier = Modifier.weight(1f)
            )
            Text(format(values[focus]), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = UrbanColors.Ink)
        }
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth().height(128.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            values.forEachIndexed { i, v ->
                val fraction = if (v <= 0.0) 0f else ((v / max).toFloat() * progress.value).coerceIn(0.03f, 1f)
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            selected = if (selected == i) null else i
                        },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraction)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(if (i == focus) color else color.copy(alpha = 0.32f))
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            values.indices.forEach { i ->
                Text(
                    if (i % step == 0) labels.getOrElse(i) { "" } else "",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    style = MaterialTheme.typography.labelSmall,
                    color = UrbanColors.Muted
                )
            }
        }
    }
}

/** Ranking o desglose: una fila por elemento con su barra proporcional al mayor. */
@Composable
fun UrbanHBars(
    labels: List<String>,
    values: List<Double>,
    modifier: Modifier = Modifier,
    color: Color = UrbanColors.Gold,
    format: (Double) -> String = ::defaultFormat
) {
    if (values.isEmpty() || values.all { it <= 0.0 }) return
    val progress = remember(values) { Animatable(0f) }
    LaunchedEffect(values) { progress.animateTo(1f, tween(durationMillis = 700)) }
    val max = values.max()

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        values.forEachIndexed { i, v ->
            Column(Modifier.semantics { contentDescription = "${labels.getOrElse(i) { "" }}: ${format(v)}" }) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        labels.getOrElse(i) { "" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = UrbanColors.Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(format(v), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = UrbanColors.Ink)
                }
                Spacer(Modifier.height(5.dp))
                Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)).background(UrbanColors.Line.copy(alpha = 0.5f))) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(((v / max).toFloat() * progress.value).coerceIn(0f, 1f))
                            .clip(RoundedCornerShape(50))
                            .background(if (i == 0) color else color.copy(alpha = 0.55f))
                    )
                }
            }
        }
    }
}

/** Medidor circular de un porcentaje (0 a 1) con el valor al centro. */
@Composable
fun UrbanRingGauge(
    fraction: Float,
    centerText: String,
    modifier: Modifier = Modifier,
    color: Color = UrbanColors.Gold,
    diameter: androidx.compose.ui.unit.Dp = 96.dp
) {
    val target = fraction.coerceIn(0f, 1f)
    val progress = remember(target) { Animatable(0f) }
    LaunchedEffect(target) { progress.animateTo(target, tween(durationMillis = 800)) }
    val track = UrbanColors.Line.copy(alpha = 0.5f)

    Box(modifier.size(diameter).semantics { contentDescription = centerText }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(diameter)) {
            val stroke = 10.dp.toPx()
            val d = size.minDimension - stroke
            drawArc(track, -90f, 360f, useCenter = false, topLeft = Offset(stroke / 2, stroke / 2), size = Size(d, d), style = Stroke(stroke))
            drawArc(
                color, -90f, 360f * progress.value, useCenter = false,
                topLeft = Offset(stroke / 2, stroke / 2), size = Size(d, d), style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        Text(centerText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = UrbanColors.Ink)
    }
}
