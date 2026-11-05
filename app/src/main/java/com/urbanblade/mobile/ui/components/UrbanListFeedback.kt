package com.urbanblade.mobile.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
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

/** Solo para consultas: los errores de guardar o ejecutar acciones mantienen su aviso propio. */
internal fun LazyListScope.urbanListFeedback(
    loaded: Boolean, loading: Boolean, hasItems: Boolean, error: String?,
    errorTitle: String, emptyTitle: String, emptySubtitle: String, onRetry: () -> Unit
) {
    val state = listFeedbackState(loaded, loading, hasItems, error)
    if (state != ListFeedbackState.CONTENT) item {
        UrbanListFeedback(state, error, errorTitle, emptyTitle, emptySubtitle, onRetry)
    }
}

@Composable
internal fun UrbanListFeedback(
    state: ListFeedbackState, error: String?, errorTitle: String,
    emptyTitle: String, emptySubtitle: String, onRetry: () -> Unit
) {
    when (state) {
        ListFeedbackState.INITIAL_LOADING -> UrbanSkeletonList(1)
        ListFeedbackState.INITIAL_ERROR -> UrbanMascotState(UrbanStateKind.ERROR, errorTitle, error, "Reintentar", onAction = onRetry)
        ListFeedbackState.STALE_ERROR -> UrbanStaleContentNotice(error.orEmpty(), onRetry)
        ListFeedbackState.REFRESHING -> LinearProgressIndicator(
            Modifier.fillMaxWidth().semantics { contentDescription = "Actualizando resultados" }, color = UrbanColors.Gold
        )
        ListFeedbackState.EMPTY -> UrbanMascotState(UrbanStateKind.EMPTY, emptyTitle, emptySubtitle)
        ListFeedbackState.CONTENT -> Unit
    }
}
