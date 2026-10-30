package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Accesibilidad de la valoración del chatbot: cada botón mide 48 dp (antes se dibujaba de 36 dp y dependía de la
 * ampliación automática del toque) y se anuncia con su nombre para TalkBack.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w320dp-h640dp")
class ChatRatingButtonsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `cada boton de valoracion tiene nombre y mide al menos 48 dp`() {
        compose.setContent { MaterialTheme { Row { ChatRatingButtons {} } } }

        listOf("Me sirvió", "No me sirvió").forEach { nombre ->
            compose.onNodeWithContentDescription(nombre)
                .assertHasClickAction()
                .assertLayoutSizeAtLeast(48.dp)
        }
    }

    @Test
    fun `tocar cada boton avisa si la respuesta sirvio o no`() {
        val valoraciones = mutableListOf<Boolean>()
        compose.setContent { MaterialTheme { Row { ChatRatingButtons { valoraciones += it } } } }

        compose.onNodeWithContentDescription("Me sirvió").performClick()
        compose.onNodeWithContentDescription("No me sirvió").performClick()

        assertEquals(listOf(true, false), valoraciones)
    }
}
