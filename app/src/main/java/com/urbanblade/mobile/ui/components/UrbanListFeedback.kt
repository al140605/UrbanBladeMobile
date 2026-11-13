package com.urbanblade.mobile.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.urbanblade.mobile.ui.theme.UrbanColors

internal enum class ListFeedbackState { INITIAL_LOADING, INITIAL_ERROR, STALE_ERROR, REFRESHING, EMPTY, CONTENT }

internal fun listFeedbackState(loaded: Boolean, loading: Boolean, hasItems: Boolean, error: String?): ListFeedbackState = when {
    !loaded && (loading || error == null) -> ListFeedbackState.INITIAL_LOADING
    !loaded -> ListFeedbackState.INITIAL_ERROR
    error != null -> ListFeedbackState.STALE_ERROR
    loading -> ListFeedbackState.REFRESHING
    !hasItems -> ListFeedbackState.EMPTY
    else -> ListFeedbackState.CONTENT
}

/**
 * Vacío confirmado de un listado: ya cargó, no hay carga ni error en curso y no hay elementos. En ese estado el vacío
 * lleva su propia acción, así que el botón flotante del módulo se oculta (`hideFloatingActionButton` de
 * `UrbanModuleScreen`) en vez de tapar el texto o duplicar el mismo botón.
 */
internal fun isConfirmedEmpty(loaded: Boolean, loading: Boolean, hasItems: Boolean, error: String?): Boolean =
    listFeedbackState(loaded, loading, hasItems, error) == ListFeedbackState.EMPTY

/**
 * Solo para consultas: los errores de guardar o ejecutar acciones mantienen su aviso propio.
 * [emptyActionLabel] añade una acción al vacío confirmado (por ejemplo «Registrar cliente»).
 */
internal fun LazyListScope.urbanListFeedback(
    loaded: Boolean, loading: Boolean, hasItems: Boolean, error: String?,
    errorTitle: String, emptyTitle: String, emptySubtitle: String,
    emptyActionLabel: String? = null, emptyActionIcon: ImageVector = Icons.Default.Refresh, onEmptyAction: (() -> Unit)? = null,
    onRetry: () -> Unit
) {
    val state = listFeedbackState(loaded, loading, hasItems, error)
    if (state != ListFeedbackState.CONTENT) item {
        UrbanListFeedback(state, error, errorTitle, emptyTitle, emptySubtitle, onRetry, emptyActionLabel, emptyActionIcon, onEmptyAction)
    }
}

@Composable
internal fun UrbanListFeedback(
    state: ListFeedbackState, error: String?, errorTitle: String,
    emptyTitle: String, emptySubtitle: String, onRetry: () -> Unit,
    emptyActionLabel: String? = null, emptyActionIcon: ImageVector = Icons.Default.Refresh, onEmptyAction: (() -> Unit)? = null
) {
    when (state) {
        ListFeedbackState.INITIAL_LOADING -> UrbanSkeletonList(1)
        ListFeedbackState.INITIAL_ERROR -> UrbanMascotState(UrbanStateKind.ERROR, errorTitle, error, "Reintentar", onAction = onRetry)
        ListFeedbackState.STALE_ERROR -> UrbanStaleContentNotice(error.orEmpty(), onRetry)
        ListFeedbackState.REFRESHING -> LinearProgressIndicator(
            Modifier.fillMaxWidth().semantics { contentDescription = "Actualizando resultados" }, color = UrbanColors.Gold
        )
        ListFeedbackState.EMPTY -> UrbanMascotState(UrbanStateKind.EMPTY, emptyTitle, emptySubtitle, emptyActionLabel, emptyActionIcon, onEmptyAction)
        ListFeedbackState.CONTENT -> Unit
    }
}
