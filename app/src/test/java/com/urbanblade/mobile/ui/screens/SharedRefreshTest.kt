package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.urbanblade.mobile.ui.components.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w320dp-h3000dp")
class SharedRefreshTest {
    @get:Rule val compose = createComposeRule()

    @Test fun `large font header preserves title and reachable refresh action`() {
        var requests = 0
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                MaterialTheme {
                    UrbanPageHeader("Hola, Alejandra Fernanda", "Tu información del día", "RECEPCIÓN", trailing = {
                        Row { UrbanRefreshButton(false) { requests++ }; Text("AF") }
                    })
                }
            }
        }
        val title = compose.onNodeWithText("Hola, Alejandra Fernanda")
        title.assertIsDisplayed()
        val refresh = compose.onNodeWithContentDescription("Actualizar información")
        refresh.assertIsDisplayed().assertTouchTargetAtLeast(48.dp)
        org.junit.Assert.assertTrue(refresh.fetchSemanticsNode().boundsInRoot.top >= title.fetchSemanticsNode().boundsInRoot.bottom)
        refresh.performClick()
        assertEquals(1, requests)
    }

    @Test fun `refresh in progress has an accessible status and cannot trigger again`() {
        var requests = 0
        compose.setContent { MaterialTheme { UrbanRefreshButton(true) { requests++ } } }
        compose.onNodeWithContentDescription("Actualizando información").assertExists()
        compose.onNode(hasClickAction()).assertIsNotEnabled()
        assertEquals(0, requests)
    }

    @Test fun `stale data notice explains the last response and offers retry`() {
        var retries = 0
        compose.setContent { MaterialTheme { Column { UrbanStaleContentNotice("Sin conexión.") { retries++ } } } }
        compose.onNodeWithText("La información visible corresponde a la última consulta.", substring = true).assertExists()
        compose.onNodeWithText("Reintentar actualización").performClick()
        assertEquals(1, retries)
    }

    @Test fun `section action follows the title with large font and stays reachable`() {
        var clicks = 0
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                MaterialTheme { UrbanSectionTitle("Trabajos y disponibilidad", "Consulta tu agenda", "Ver agenda") { clicks++ } }
            }
        }
        val title = compose.onNodeWithText("Trabajos y disponibilidad")
        val action = compose.onNodeWithText("Ver agenda")
        title.assertIsDisplayed()
        action.assertIsDisplayed().assertTouchTargetAtLeast(48.dp)
        org.junit.Assert.assertTrue(action.fetchSemanticsNode().boundsInRoot.top >= title.fetchSemanticsNode().boundsInRoot.bottom)
        action.performClick()
        assertEquals(1, clicks)
    }

    @Test fun `initial list error offers retry without claiming the list is empty`() {
        var retries = 0
        compose.setContent { MaterialTheme {
            UrbanListFeedback(ListFeedbackState.INITIAL_ERROR, "Sin conexión", "No pudimos cargar", "Sin elementos", "Vacío confirmado", { retries++ })
        } }
        compose.onNodeWithText("Sin elementos").assertDoesNotExist()
        compose.onNodeWithText("Reintentar").performClick()
        assertEquals(1, retries)
    }
}
