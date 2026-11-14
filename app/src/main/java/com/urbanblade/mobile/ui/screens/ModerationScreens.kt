package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urbanblade.mobile.data.model.ReportedComment
import com.urbanblade.mobile.ui.components.UrbanCard
import com.urbanblade.mobile.ui.components.UrbanErrorBanner
import com.urbanblade.mobile.ui.components.UrbanFormat
import com.urbanblade.mobile.ui.components.UrbanInfoBanner
import com.urbanblade.mobile.ui.components.UrbanModuleScreen
import com.urbanblade.mobile.ui.components.urbanListFeedback
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.BlockedAuthorsViewModel
import com.urbanblade.mobile.ui.viewmodel.ModerationAction
import com.urbanblade.mobile.ui.viewmodel.ModerationViewModel
import com.urbanblade.mobile.ui.viewmodel.reasonsSummary

/** Autores que el usuario bloqueó en el muro, con un botón para deshacer cada bloqueo. */
@Composable
fun BlockedAuthorsScreen(onBack: () -> Unit, vm: BlockedAuthorsViewModel = viewModel()) {
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) { vm.load() }

    UrbanModuleScreen(
        eyebrow = "COMUNIDAD",
        title = "Autores bloqueados",
        subtitle = "No ves sus comentarios en el muro. Puedes deshacerlo cuando quieras.",
        onBack = onBack,
        onRefresh = { vm.load() },
        refreshing = state.loading
    ) {
        state.notice?.let { item { UrbanInfoBanner(it, Icons.Default.CheckCircle) } }
        state.error?.let { item { UrbanErrorBanner(it) } }
        urbanListFeedback(
            state.loaded, state.loading, state.items.isNotEmpty(), state.listError,
            "No pudimos cargar tus bloqueos", "No has bloqueado a nadie",
            "Si bloqueas a alguien desde un comentario del muro, aparecerá aquí para que puedas deshacerlo."
        ) { vm.load() }
        items(state.items, key = { it.id }) { author ->
            UrbanCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(author.name ?: "Usuario", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = { vm.unblock(author) }, enabled = state.unblockingId == null) {
                        Text(if (state.unblockingId == author.id) "Desbloqueando…" else "Desbloquear")
                    }
                }
            }
        }
    }
}

/** Moderación del muro (administrador): conservar, eliminar o eliminar y vetar al autor de un comentario reportado. */
@Composable
fun ModerationScreen(onBack: () -> Unit, vm: ModerationViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    var pending by remember { mutableStateOf<Pair<ReportedComment, ModerationAction>?>(null) }

    LaunchedEffect(Unit) { vm.load() }

    UrbanModuleScreen(
        eyebrow = "ADMINISTRACIÓN",
        title = "Moderación",
        subtitle = "Comentarios del muro que los clientes reportaron.",
        onBack = onBack,
        onRefresh = { vm.load() },
        refreshing = state.loading
    ) {
        state.notice?.let { item { UrbanInfoBanner(it, Icons.Default.CheckCircle) } }
        state.error?.let { item { UrbanErrorBanner(it) } }
        urbanListFeedback(
            state.loaded, state.loading, state.items.isNotEmpty(), state.listError,
            "No pudimos cargar los reportes", "Sin reportes pendientes",
            "Cuando un cliente reporte un comentario del muro, aparecerá aquí para que decidas."
        ) { vm.load() }
        items(state.items, key = { it.commentId }) { report ->
            val busy = state.resolvingId == report.commentId
            UrbanCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(report.author?.name ?: "Usuario", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text(
                            UrbanFormat.count(report.reportsCount, "reporte", "reportes"),
                            style = MaterialTheme.typography.labelLarge,
                            color = UrbanColors.Danger
                        )
                    }
                    report.work?.title?.takeIf { it.isNotBlank() }?.let {
                        Text("En «$it»", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                    }
                    Text("«${report.comment.orEmpty()}»", style = MaterialTheme.typography.bodyMedium)
                    if (report.reasons.isNotEmpty()) {
                        Text(reasonsSummary(report.reasons), style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                    }
                    report.firstReportedAt?.takeIf { it.isNotBlank() }?.let {
                        Text("Primer reporte: ${UrbanFormat.date(it)}", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                    }
                    if (report.author?.commentsBanned == true) {
                        Text("Este autor ya está vetado para comentar.", style = MaterialTheme.typography.bodySmall, color = UrbanColors.GoldText)
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { vm.resolve(report, ModerationAction.Dismiss) }, enabled = !busy && state.resolvingId == null) {
                            Text("Conservar")
                        }
                        TextButton(onClick = { pending = report to ModerationAction.Remove }, enabled = !busy && state.resolvingId == null) {
                            Text("Eliminar", color = UrbanColors.Danger)
                        }
                        TextButton(onClick = { pending = report to ModerationAction.RemoveAndBan }, enabled = !busy && state.resolvingId == null) {
                            Text("Eliminar y vetar", color = UrbanColors.Danger)
                        }
                    }
                    if (busy) Text("Enviando decisión…", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                }
            }
        }
    }

    pending?.let { (report, action) ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(if (action == ModerationAction.RemoveAndBan) "¿Eliminar y vetar al autor?" else "¿Eliminar el comentario?") },
            text = {
                Text(
                    if (action == ModerationAction.RemoveAndBan) {
                        "Se elimina el comentario y ${report.author?.name ?: "el autor"} ya no podrá comentar en el muro. Esta acción no se deshace desde la app."
                    } else {
                        "Se elimina el comentario y se cierran sus reportes. Esta acción no se deshace."
                    }
                )
            },
            confirmButton = { TextButton(onClick = { vm.resolve(report, action); pending = null }) { Text("Confirmar", color = UrbanColors.Danger) } },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("Cancelar") } }
        )
    }
}
