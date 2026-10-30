package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.urbanblade.mobile.data.model.BarberItem
import com.urbanblade.mobile.data.model.BarberUser
import com.urbanblade.mobile.data.model.ServiceItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Matriz de accesibilidad de la reserva con el peor caso de pantalla: ancho de 320 dp y fuente al 200 %.
 * Comprueba que la información sigue en pantalla, que se puede elegir y que todo lo tocable mide al menos 48 dp.
 * La pantalla es alta para que el desplazamiento no interfiera con lo que se mide.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w320dp-h3000dp")
class AccesibilidadEscalaTest {
    @get:Rule
    val compose = createComposeRule()

    private val services = listOf(
        ServiceItem(id = "s1", nombre = "Corte clásico", precio = 180.0, duracionMin = 30),
        ServiceItem(id = "s2", nombre = "Barba tradicional", precio = 120.0, duracionMin = 25),
        ServiceItem(id = "s3", nombre = "Combo premium con ritual de toalla caliente", precio = 260.0, duracionMin = 60)
    )
    private val barber = BarberItem(id = "b1", slug = "nava", user = BarberUser(id = "u1", name = "Nava Panther"))

    /** Muestra el contenido con la fuente al 200 %, como cuando la persona la agranda en Ajustes. */
    private fun conFuenteAl200(content: @Composable () -> Unit) {
        compose.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale = 2f)) {
                MaterialTheme { Column(Modifier.fillMaxSize()) { content() } }
            }
        }
    }

    private fun assertTodoLoTocableMide48() {
        val tocables = compose.onAllNodes(hasClickAction())
        val total = tocables.fetchSemanticsNodes().size
        assertTrue("Debe haber elementos tocables", total > 0)
        for (i in 0 until total) tocables[i].assertTouchTargetAtLeast(48.dp)
    }

    @Test
    fun `a 320 dp y fuente al 200 por ciento cada servicio sigue visible y se puede elegir`() {
        var elegido = ""
        conFuenteAl200 { ServiceStep(services, selectedId = "", onSelect = { elegido = it }) }

        services.forEach { compose.onNodeWithText(it.nombre).assertIsDisplayed() }
        compose.onNodeWithText("Barba tradicional").performClick()

        assertEquals("s2", elegido)
    }

    @Test
    fun `a 320 dp y fuente al 200 por ciento lo tocable del paso de servicio mide al menos 48 dp`() {
        conFuenteAl200 { ServiceStep(services, selectedId = "", onSelect = {}) }

        assertTodoLoTocableMide48()
    }

    @Test
    fun `a 320 dp y fuente al 200 por ciento el resumen conserva hora fecha servicio y barbero`() {
        conFuenteAl200 {
            ReviewStep(service = services[0], barber = barber, date = "2026-10-23", time = "12:30", extras = emptyList(), notes = "") {
                Text("Aquí va el paso de pago")
            }
        }

        compose.onNodeWithText("12:30 PM").assertIsDisplayed()
        compose.onNodeWithText("Viernes 23 de octubre").assertIsDisplayed()
        compose.onNodeWithText("Corte clásico").assertIsDisplayed()
        compose.onNodeWithText("Nava Panther").assertIsDisplayed()
    }
}
