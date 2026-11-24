package com.urbanblade.mobile.ui.components

import android.content.ClipData
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import kotlinx.coroutines.launch

/**
 * Función para copiar un texto al portapapeles. Sustituye a `LocalClipboardManager.current.setText(...)`, que
 * Compose marcó como obsoleto: la API nueva (`LocalClipboard`) es suspendida, así que aquí se lanza en el alcance
 * de la pantalla. La usan la Billetera (código de referido), el pago de reserva (datos bancarios) y las tarjetas
 * regalo del personal (código de la tarjeta vendida).
 */
@Composable
fun rememberCopyToClipboard(): (String) -> Unit {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    return remember(clipboard, scope) {
        { text: String ->
            scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("UrbanBlade", text))) }
            Unit
        }
    }
}
