package com.urbanblade.mobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.urbanblade.mobile.ui.theme.UrbanColors

/** Misma acción de actualización para los inicios de todos los roles. */
@Composable
fun UrbanRefreshButton(loading: Boolean, onRefresh: () -> Unit) {
    IconButton(onClick = onRefresh, enabled = !loading, modifier = Modifier.size(48.dp)) {
        if (loading) CircularProgressIndicator(
            modifier = Modifier.size(20.dp).semantics { contentDescription = "Actualizando información" },
            strokeWidth = 2.dp, color = UrbanColors.Gold
        ) else Icon(Icons.Default.Refresh, "Actualizar información", tint = UrbanColors.Gold)
    }
}

/** Un fallo de actualización no debe ocultarse detrás del último resumen disponible. */
@Composable
fun UrbanStaleContentNotice(error: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        UrbanErrorBanner("No pudimos actualizar. La información visible corresponde a la última consulta. $error")
        UrbanOutlineButton("Reintentar actualización", onRetry, modifier = Modifier.fillMaxWidth())
    }
}
