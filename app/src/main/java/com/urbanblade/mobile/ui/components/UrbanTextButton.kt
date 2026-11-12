package com.urbanblade.mobile.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Acción terciaria dentro de una pantalla («Reintentar», «Editar», «Vender otra»…).
 *
 * Garantiza el objetivo táctil mínimo de 48 dp en todas las pantallas; antes cada una
 * lo resolvía (o no) por su cuenta. Los botones de diálogo siguen usando `TextButton`,
 * que es el idioma estándar de Material para `confirmButton` y `dismissButton`.
 */
@Composable
fun UrbanTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        contentPadding = contentPadding,
        content = content
    )
}
