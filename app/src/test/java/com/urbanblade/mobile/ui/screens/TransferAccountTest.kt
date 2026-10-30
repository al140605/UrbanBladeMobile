package com.urbanblade.mobile.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.urbanblade.mobile.data.model.TransferInfo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Datos bancarios del pago por transferencia (reserva y Mis citas): cada estado dice algo claro al cliente en vez de
 * dejar la pantalla vacía.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp")
class TransferAccountTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `mientras cargan avisa que busca los datos bancarios`() {
        compose.setContent { MaterialTheme { TransferAccount(info = null, total = 150.0) } }

        compose.onNodeWithText("Cargando datos bancarios…").assertExists()
    }

    @Test
    fun `sin datos bancarios configurados explica que elija otro metodo`() {
        compose.setContent { MaterialTheme { TransferAccount(info = TransferInfo(configurado = false), total = 150.0) } }

        compose.onNodeWithText("La barbería todavía no registra sus datos bancarios", substring = true).assertExists()
    }

    @Test
    fun `con datos muestra cuenta y monto a transferir`() {
        val info = TransferInfo(
            configurado = true, banco = "BBVA", beneficiario = "UrbanBlade", clabe = "012345678901234567", concepto = "Cita"
        )
        compose.setContent { MaterialTheme { TransferAccount(info = info, total = 150.0) } }

        compose.onNodeWithText("Transfiere a esta cuenta").assertExists()
        compose.onNodeWithText("BBVA").assertExists()
        compose.onNodeWithText("0123 4567 8901 2345 67").assertExists()
        compose.onNodeWithText("Monto").assertExists()
    }
}
