package com.urbanblade.mobile.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h3000dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class UrbanHeroSummaryTest {
    @get:Rule
    val compose = createComposeRule()

    private val headline = "$12,345,678.90"
    private val trend = "Aumento del 18.5 %"
    private val stats = listOf(
        UrbanHeroStatSpec("Ingresos confirmados del periodo", "$1,250,000.00", Icons.Default.Info),
        UrbanHeroStatSpec("Servicios completados este mes", "987654321", Icons.Default.Info),
        UrbanHeroStatSpec("Porcentaje de citas completadas", "99.99 %", Icons.Default.Info)
    )

    private fun showSummary(width: Dp, fontScale: Float, longContent: Boolean = true) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                MaterialTheme {
                    Column(Modifier.width(width)) {
                        UrbanHeroSummary(
                            label = "Resumen del negocio",
                            headline = if (longContent) headline else "$250",
                            stats = if (longContent) stats else stats.mapIndexed { index, stat ->
                                stat.copy(value = "${index + 1}", label = "Dato ${index + 1}")
                            },
                            headlineTrailing = { Text(if (longContent) trend else "+5 %") }
                        )
                    }
                }
            }
        }
    }

    private fun assertCompleteText(text: String) {
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(text).assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
                assertTrue("Debe poder medirse el texto $text", action(layouts))
            }
        assertEquals(1, layouts.size)
        val layout = layouts.single()
        val bounds = compose.onNodeWithText(text).fetchSemanticsNode().boundsInRoot
        val visibleWidth = minOf(layout.size.width.toFloat(), bounds.width)
        val visibleHeight = minOf(layout.size.height.toFloat(), bounds.height)
        val diagnostic = "Texto $text: size=${layout.size}, bounds=$bounds, " +
            "paragraph=${layout.multiParagraph.width}x${layout.multiParagraph.height}, lines=${layout.lineCount}"
        assertEquals(text, layout.layoutInput.text.text)
        assertTrue("$diagnostic; debe haber líneas visibles", layout.lineCount > 0)
        assertFalse("$diagnostic; no debe exceder maxLines", layout.multiParagraph.didExceedMaxLines)
        assertEquals("$diagnostic; debe mostrarse hasta el último carácter", text.length, layout.getLineEnd(layout.lineCount - 1))
        // GetTextLayoutResult reconstruye Text(String) con el ancho máximo disponible en
        // Compose 1.8.1. Las líneas y el nodo conservan el tamaño que debe caber en pantalla.
        for (line in 0 until layout.lineCount) {
            val lineWidth = layout.getLineRight(line) - layout.getLineLeft(line)
            assertTrue("$diagnostic; línea $line demasiado ancha: $lineWidth > $visibleWidth", lineWidth <= visibleWidth + 1f)
            assertTrue("$diagnostic; línea $line fuera del alto visible", layout.getLineTop(line) >= -1f && layout.getLineBottom(line) <= visibleHeight + 1f)
            assertFalse("El texto $text no debe usar elipsis", layout.isLineEllipsized(line))
        }
    }

    private fun assertBelow(lowerText: String, upperText: String) {
        val upper = compose.onNodeWithText(upperText).fetchSemanticsNode().boundsInRoot
        val lower = compose.onNodeWithText(lowerText).fetchSemanticsNode().boundsInRoot
        assertTrue("$lowerText debe quedar debajo de $upperText", lower.top >= upper.bottom)
    }

    @Test
    fun `a 320 dp y fuente 200 los importes y etiquetas se leen completos y apilados`() {
        showSummary(320.dp, 2f)

        listOf("RESUMEN DEL NEGOCIO", headline, trend).forEach(::assertCompleteText)
        stats.forEach {
            assertCompleteText(it.value)
            assertCompleteText(it.label)
        }
        assertBelow(trend, headline)
        assertBelow(stats[1].value, stats[0].label)
        assertBelow(stats[2].value, stats[1].label)
    }

    @Test
    fun `el ancho de 320 dp apila los datos incluso con fuente normal`() {
        showSummary(320.dp, 1f)

        assertBelow(trend, headline)
        stats.forEach { assertCompleteText(it.value) }
        assertBelow(stats[1].value, stats[0].label)
    }

    @Test
    fun `con ancho normal la fuente grande tambien apila titular y datos`() {
        showSummary(411.dp, 1.3f)

        assertBelow(trend, headline)
        stats.forEach { assertCompleteText(it.value) }
        assertBelow(stats[1].value, stats[0].label)
    }

    @Test
    fun `el resumen conserva la fila compacta con ancho y fuente normales`() {
        showSummary(411.dp, 1f, longContent = false)

        val first = compose.onNodeWithText("1").fetchSemanticsNode().boundsInRoot
        val second = compose.onNodeWithText("2").fetchSemanticsNode().boundsInRoot
        val title = compose.onNodeWithText("$250").fetchSemanticsNode().boundsInRoot
        val trailing = compose.onNodeWithText("+5 %").fetchSemanticsNode().boundsInRoot
        assertEquals(first.top, second.top, 0.5f)
        assertTrue(second.left > first.right)
        assertTrue(trailing.top < title.bottom && trailing.bottom > title.top)
    }
}
