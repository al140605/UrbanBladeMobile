package com.urbanblade.mobile.core.payment

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w320dp-h1400dp")
class FinalPaymentReviewTest {
    @get:Rule val compose = createComposeRule()
    @Test fun `review shows actual amount and only Pay requests verification`() {
        var pay = 0
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                MaterialTheme {
                    FinalPaymentReview("UrbanBlade", "Visa 4242", "$95.00 MXN", false,
                        { pay++ }, {}, {})
                }
            }
        }
        compose.onNodeWithText("Total a pagar: $95.00 MXN").assertIsDisplayed()
        compose.onNodeWithText("Visa 4242").assertIsDisplayed()
        assertEquals(0, pay)
        compose.onNodeWithText("Pagar $95.00 MXN").performClick()
        assertEquals(1, pay)
    }
    @Test fun `edit and cancel never confirm a charge`() {
        var pay = 0; var edit = 0; var cancel = 0
        compose.setContent { MaterialTheme {
            FinalPaymentReview("UrbanBlade", "Visa 4242", "$95.00 MXN", false,
                { pay++ }, { edit++ }, { cancel++ })
        } }
        compose.onNodeWithText("Cambiar tarjeta").performClick()
        compose.onNodeWithText("Cancelar").performClick()
        assertEquals(0, pay); assertEquals(1, edit); assertEquals(1, cancel)
    }
    @Test fun `review cannot change operation during system authorization`() {
        compose.setContent { MaterialTheme {
            FinalPaymentReview("UrbanBlade", "Visa 4242", "$95.00 MXN", true, {}, {}, {})
        } }
        compose.onNodeWithText("Cancelar").assertIsNotEnabled()
        compose.onNodeWithText("Cambiar tarjeta").assertIsNotEnabled()
    }
}
