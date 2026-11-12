package com.urbanblade.mobile.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NavigationActionsTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var nav: NavHostController
    private val clientRoots = listOf("home", "catalog", "appointments", "social", "profile")
    private val staffRoots = listOf("home", "appointments", "more", "profile")

    private fun showNavigation() {
        compose.setContent {
            nav = rememberNavController()
            NavHost(navController = nav, startDestination = "home") {
                listOf("home", "catalog", "appointments", "profile", "more", "notifications", "barber_profile").forEach { route ->
                    composable(route) { Text(route) }
                }
                composable("social") {
                    var selectedPost by rememberSaveable { mutableIntStateOf(0) }
                    Column {
                        Text("Publicación $selectedPost")
                        Button(onClick = { selectedPost++ }) { Text("Siguiente publicación") }
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun open(route: String, roots: List<String> = clientRoots, singleTop: Boolean = false) {
        compose.runOnIdle { nav.openDestination(route, roots, singleTop) }
        compose.waitForIdle()
    }

    private fun backTo(route: String) {
        compose.runOnIdle {
            assertTrue(nav.popBackStack())
            assertEquals(route, nav.currentDestination?.route)
        }
        compose.waitForIdle()
    }

    @Test
    fun `el atajo del catalogo al muro vuelve a inicio`() {
        showNavigation()

        open("catalog")
        open("social")

        backTo("home")
    }

    @Test
    fun `abrir el muro repetidamente conserva una sola entrada`() {
        showNavigation()

        open("social")
        open("social")
        open("social")

        backTo("home")
    }

    @Test
    fun `el perfil del barbero regresa al muro con su estado`() {
        showNavigation()
        open("social")
        compose.onNodeWithText("Siguiente publicación").performClick()

        open("barber_profile")
        backTo("social")

        compose.onNodeWithText("Publicación 1").assertIsDisplayed()
    }

    @Test
    fun `cambiar de pestana restaura el muro sin apilar el catalogo`() {
        showNavigation()
        open("social")
        compose.onNodeWithText("Siguiente publicación").performClick()

        open("catalog")
        open("social")

        compose.onNodeWithText("Publicación 1").assertIsDisplayed()
        backTo("home")
        open("catalog")
        compose.runOnIdle { assertEquals("catalog", nav.currentDestination?.route) }
    }

    @Test
    fun `abrir el muro desde una notificacion vuelve a inicio`() {
        showNavigation()

        open("notifications")
        open("social", singleTop = true)

        backTo("home")
    }

    @Test
    fun `inicio desde la pestana del muro no restaura una notificacion anterior`() {
        showNavigation()

        open("notifications")
        open("social", singleTop = true)
        open("home")

        compose.runOnIdle { assertEquals("home", nav.currentDestination?.route) }
        compose.onNodeWithText("home").assertIsDisplayed()
    }

    @Test
    fun `restaurar la pestana del muro conserva su detalle abierto y permite volver`() {
        showNavigation()
        open("social")
        compose.onNodeWithText("Siguiente publicación").performClick()
        open("barber_profile")

        open("catalog")
        open("social")

        compose.runOnIdle { assertEquals("barber_profile", nav.currentDestination?.route) }
        compose.onNodeWithText("barber_profile").assertIsDisplayed()
        backTo("social")
        compose.onNodeWithText("Publicación 1").assertIsDisplayed()
        backTo("home")
    }

    @Test
    fun `para el personal el muro sigue siendo un modulo secundario`() {
        showNavigation()

        open("more", staffRoots)
        open("social", staffRoots)

        backTo("more")
    }
}
