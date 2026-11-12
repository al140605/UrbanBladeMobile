package com.urbanblade.mobile.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
@Config(sdk = [34], qualifiers = "w411dp-h1000dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class UrbanBottomNavigationTest {
    @get:Rule
    val compose = createComposeRule()

    private val destinations = listOf(
        UrbanBottomDestination("home", "Inicio", Icons.Default.Home),
        UrbanBottomDestination("booking", "Reservar", Icons.Default.Home),
        UrbanBottomDestination("appointments", "Mis citas", Icons.Default.Home),
        UrbanBottomDestination("social", "Muro", Icons.Default.Home),
        UrbanBottomDestination("account", "Cuenta", Icons.Default.Home)
    )
    private val selectedRoute = mutableStateOf("home")

    private fun showNavigation(
        fontScale: Float,
        items: List<UrbanBottomDestination> = destinations,
        width: Dp = 320.dp
    ) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                MaterialTheme {
                    Box(Modifier.width(width)) {
                        UrbanBottomNavigation(items, selectedRoute.value, { selectedRoute.value = it })
                    }
                }
            }
        }
    }

    private fun assertCompleteLabel(label: String) {
        val results = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(label, useUnmergedTree = true).assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
                assertTrue(action(results))
            }
        assertEquals(1, results.size)
        val layout = results.single()
        val bounds = compose.onNodeWithText(label, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val visibleWidth = minOf(layout.size.width.toFloat(), bounds.width)
        val visibleHeight = minOf(layout.size.height.toFloat(), bounds.height)
        val diagnostic = "Etiqueta $label: size=${layout.size}, bounds=$bounds, " +
            "paragraph=${layout.multiParagraph.width}x${layout.multiParagraph.height}, lines=${layout.lineCount}"
        assertEquals(label, layout.layoutInput.text.text)
        assertTrue("$diagnostic; debe haber líneas visibles", layout.lineCount > 0)
        assertFalse("$diagnostic; no debe exceder maxLines", layout.multiParagraph.didExceedMaxLines)
        assertEquals("$diagnostic; debe mostrarse hasta el último carácter", label.length, layout.getLineEnd(layout.lineCount - 1))
        // En Compose 1.8.1, GetTextLayoutResult reconstruye el párrafo con maxWidth, aunque
        // Text(String) ocupe solo su ancho natural. Se miden las líneas, no ese ancho artificial.
        for (line in 0 until layout.lineCount) {
            val lineWidth = layout.getLineRight(line) - layout.getLineLeft(line)
            assertTrue("$diagnostic; línea $line demasiado ancha: $lineWidth > $visibleWidth", lineWidth <= visibleWidth + 1f)
            assertTrue("$diagnostic; línea $line fuera del alto visible", layout.getLineTop(line) >= -1f && layout.getLineBottom(line) <= visibleHeight + 1f)
            assertFalse("La etiqueta $label no debe usar elipsis en la línea $line", layout.isLineEllipsized(line))
        }
    }

    @Test
    fun `a 320 dp y fuente 200 todas las pestanas se leen y seleccionan`() {
        showNavigation(2f)

        destinations.forEach { destination ->
            assertCompleteLabel(destination.label)
            compose.onNodeWithText(destination.label)
                .assertIsDisplayed()
                .assertHasClickAction()
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
                .performClick()
                .assertIsSelected()
            assertEquals(destination.route, selectedRoute.value)
        }
        val first = compose.onNodeWithText("Inicio").fetchSemanticsNode().boundsInRoot
        val last = compose.onNodeWithText("Cuenta").fetchSemanticsNode().boundsInRoot
        assertTrue("La fuente grande requiere varias filas", last.top >= first.bottom)
    }

    @Test
    fun `la escala normal conserva los cinco destinos en una fila a 360 dp`() {
        // A 320 dp «Mis citas» no cabe en una pestaña de Material 3 sin cortarse: ahí corresponde la cuadrícula.
        showNavigation(1f, width = 360.dp)

        destinations.forEach { assertCompleteLabel(it.label) }
        val first = compose.onNodeWithText("Inicio").fetchSemanticsNode().boundsInRoot
        val last = compose.onNodeWithText("Cuenta").fetchSemanticsNode().boundsInRoot
        assertEquals(first.top, last.top, 0.5f)
        assertTrue(last.left > first.right)
    }

    @Test
    fun `una etiqueta extensa conserva todo el texto y el destino`() {
        val longLabel = "Historial de citas y servicios anteriores"
        val items = destinations.map { if (it.route == "appointments") it.copy(label = longLabel) else it }
        showNavigation(2f, items)

        items.forEach { assertCompleteLabel(it.label) }
        compose.onNodeWithText(longLabel).performClick().assertIsSelected()
        assertEquals("appointments", selectedRoute.value)
    }
}
