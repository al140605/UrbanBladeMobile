package com.urbanblade.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.urbanblade.mobile.core.network.AppContainer
import com.urbanblade.mobile.data.model.BlockedAuthor
import com.urbanblade.mobile.data.model.ReportedComment
import com.urbanblade.mobile.data.repository.UrbanRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Etiqueta en español de un motivo de reporte (`spam`, `ofensivo`, `sexual_violento`, `otro`). */
internal fun reportReasonLabel(code: String): String = when (code) {
    "spam" -> "Spam o publicidad"
    "ofensivo" -> "Ofensivo o acoso"
    "sexual_violento" -> "Contenido sexual o violento"
    "otro" -> "Otro motivo"
    else -> code.replace('_', ' ').replaceFirstChar { it.uppercase() }
}

/** «Spam o publicidad (2) · Ofensivo o acoso (1)», con el motivo más citado primero. */
internal fun reasonsSummary(reasons: Map<String, Int>): String =
    reasons.entries.sortedByDescending { it.value }.joinToString(" · ") { "${reportReasonLabel(it.key)} (${it.value})" }

data class BlockedAuthorsState(
    val items: List<BlockedAuthor> = emptyList(),
    /** True cuando la primera consulta terminó bien; hasta entonces un vacío no está confirmado. */
    val loaded: Boolean = false,
    val loading: Boolean = false,
    /** Error de la consulta de la lista; el de desbloquear va aparte en [error]. */
    val listError: String? = null,
    val error: String? = null,
    val notice: String? = null,
    /** Id del bloqueo que se está deshaciendo, para deshabilitar su botón y evitar dobles toques. */
    val unblockingId: String? = null
)

/** Autores que el usuario bloqueó en el muro: verlos y deshacer cada bloqueo. */
class BlockedAuthorsViewModel @JvmOverloads constructor(
    private val repo: UrbanRepository = AppContainer.urbanRepository
) : ViewModel() {
    private val _state = MutableStateFlow(BlockedAuthorsState())
    val state: StateFlow<BlockedAuthorsState> = _state.asStateFlow()

    private var loadRequest: Job? = null

    fun load() {
        loadRequest?.cancel()
        _state.value = _state.value.copy(loading = true, listError = null, error = null)
        loadRequest = viewModelScope.launch {
            try {
                val response = repo.blockedAuthors()
                coroutineContext.ensureActive()
                _state.value = _state.value.copy(items = response.data, loaded = true, loading = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                coroutineContext.ensureActive()
                _state.value = _state.value.copy(loading = false, listError = e.toFriendlyMessage("No se pudieron cargar tus bloqueos."))
            }
        }
    }

    fun unblock(author: BlockedAuthor) {
        if (_state.value.unblockingId != null) return
        _state.value = _state.value.copy(unblockingId = author.id, error = null, notice = null)
        viewModelScope.launch {
            try {
                repo.unblockAuthor(author.id)
                _state.value = _state.value.copy(
                    items = _state.value.items.filterNot { it.id == author.id },
                    unblockingId = null,
                    notice = "Desbloqueaste a ${author.name ?: "este usuario"}: ya verás sus comentarios."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(unblockingId = null, error = e.toFriendlyMessage("No se pudo desbloquear. Inténtalo de nuevo."))
            }
        }
    }

    fun clearMessages() {
        _state.value = _state.value.copy(error = null, notice = null)
    }
}

/** Qué decide el administrador sobre un comentario reportado; [api] es el valor que espera el servidor. */
enum class ModerationAction(val api: String, val doneNotice: String) {
    Remove("remove", "Comentario eliminado."),
    RemoveAndBan("remove_and_ban", "Comentario eliminado y autor vetado para comentar."),
    Dismiss("dismiss", "Reportes descartados; el comentario se conserva.")
}

data class ModerationState(
    val items: List<ReportedComment> = emptyList(),
    val loaded: Boolean = false,
    val loading: Boolean = false,
    val listError: String? = null,
    val error: String? = null,
    val notice: String? = null,
    /** Comentario que se está resolviendo, para bloquear sus botones mientras se envía la decisión. */
    val resolvingId: String? = null
)

/** Moderación del muro por el administrador: comentarios con reportes pendientes y su decisión. */
class ModerationViewModel @JvmOverloads constructor(
    private val repo: UrbanRepository = AppContainer.urbanRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ModerationState())
    val state: StateFlow<ModerationState> = _state.asStateFlow()

    private var loadRequest: Job? = null

    fun load() {
        loadRequest?.cancel()
        _state.value = _state.value.copy(loading = true, listError = null, error = null)
        loadRequest = viewModelScope.launch {
            try {
                val response = repo.commentReports()
                coroutineContext.ensureActive()
                _state.value = _state.value.copy(items = response.data, loaded = true, loading = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                coroutineContext.ensureActive()
                _state.value = _state.value.copy(loading = false, listError = e.toFriendlyMessage("No se pudieron cargar los reportes."))
            }
        }
    }

    fun resolve(item: ReportedComment, action: ModerationAction) {
        if (_state.value.resolvingId != null) return
        _state.value = _state.value.copy(resolvingId = item.commentId, error = null, notice = null)
        viewModelScope.launch {
            try {
                repo.resolveCommentReport(item.commentId, action.api)
                _state.value = _state.value.copy(
                    items = _state.value.items.filterNot { it.commentId == item.commentId },
                    resolvingId = null,
                    notice = action.doneNotice
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(resolvingId = null, error = e.toFriendlyMessage("No se pudo resolver el reporte. Inténtalo de nuevo."))
            }
        }
    }

    fun clearMessages() {
        _state.value = _state.value.copy(error = null, notice = null)
    }
}
