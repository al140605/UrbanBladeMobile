package com.urbanblade.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.urbanblade.mobile.core.network.AppContainer
import com.urbanblade.mobile.core.validation.FormLimits
import com.urbanblade.mobile.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import com.urbanblade.mobile.data.repository.UrbanRepository


class BarberAgendaViewModel @JvmOverloads constructor(private val repo: UrbanRepository = AppContainer.urbanRepository) : ViewModel() {
    private val _hasLoaded = MutableStateFlow(false); val hasLoaded = _hasLoaded.asStateFlow()
    private val _agenda = MutableStateFlow(BarberAgendaResponse()); val agenda = _agenda.asStateFlow()
    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _updating = MutableStateFlow<String?>(null); val updating = _updating.asStateFlow()
    private val _error = MutableStateFlow<String?>(null); val error = _error.asStateFlow()

    /** Llegadas registradas en esta sesión (código de cita → hora ISO): la agenda del servidor no trae `llegada_en`. */
    private val _arrivals = MutableStateFlow<Map<String, String>>(emptyMap()); val arrivals = _arrivals.asStateFlow()

    /** «Llegó»: registra la llegada del cliente sin cambiar el estado de la cita; el servidor es idempotente. */
    fun checkIn(code: String) = viewModelScope.launch {
        if (_updating.value != null) return@launch
        _updating.value = code; _error.value = null
        try {
            val res = repo.checkInAppointment(code)
            res.llegadaEn?.let { _arrivals.value = _arrivals.value + (code to it) }
            _notice.value = res.message ?: "Llegada registrada."
        }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo registrar la llegada.") }
        finally { _updating.value = null }
    }

    fun load(period: String = "day", estado: String? = null, offset: Int = 0) = viewModelScope.launch {
        _busy.value = true; _error.value = null
        try { _agenda.value = repo.barberAgenda(period, estado, offset); _hasLoaded.value = true }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo cargar tu agenda.") }
        finally { _busy.value = false }
    }

    private val _ticket = MutableStateFlow<ServiceTicket?>(null); val ticket = _ticket.asStateFlow()
    private val _notice = MutableStateFlow<String?>(null); val notice = _notice.asStateFlow()
    /** Agregar tiempo choca con la siguiente cita: espera la confirmación del barbero (código, minutos, motivo). */
    private val _forceExtend = MutableStateFlow<ForceExtend?>(null); val forceExtend = _forceExtend.asStateFlow()

    data class ForceExtend(val code: String, val minutos: Int, val message: String)

    fun dismissTicket() { _ticket.value = null }
    fun dismissNotice() { _notice.value = null }
    fun dismissForceExtend() { _forceExtend.value = null }

    fun updateStatus(code: String, estado: String, period: String, filtroEstado: String?, offset: Int) = viewModelScope.launch {
        _updating.value = code; _error.value = null
        try {
            val res = repo.updateAppointmentStatus(code, estado)
            // Al terminar el servicio la API trae el ticket para mostrarlo de inmediato.
            if (estado == "completada") _ticket.value = res.ticket
            load(period, filtroEstado, offset)
        }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo actualizar la cita.") }
        finally { _updating.value = null }
    }

    /** Agrega [minutos] al servicio en curso. Si choca con la siguiente cita pide confirmar y repite con `forzar`. */
    fun extend(code: String, minutos: Int, period: String, filtroEstado: String?, offset: Int, forzar: Boolean = false) = viewModelScope.launch {
        _updating.value = code; _error.value = null
        try {
            repo.extendAppointment(code, minutos, forzar)
            _notice.value = "Se agregaron $minutos minutos. Avisamos al cliente."
            load(period, filtroEstado, offset)
        } catch (e: Exception) {
            val body = (e as? retrofit2.HttpException)?.takeIf { it.code() == 422 }?.response()?.errorBody()?.string()
            val serverMessage = body?.let { b -> runCatching { com.google.gson.JsonParser.parseString(b).asJsonObject.get("message").asString }.getOrNull() }
            if (!forzar && com.urbanblade.mobile.core.appointments.canForceFromBody(body)) {
                _forceExtend.value = ForceExtend(code, minutos, serverMessage ?: "Agregar ese tiempo choca con la siguiente cita.")
            } else {
                _error.value = serverMessage ?: e.toFriendlyMessage("No se pudo agregar tiempo.")
            }
        }
        finally { _updating.value = null }
    }

    /** Abre el ticket de un servicio ya terminado (GET /appointments/{code}/ticket). */
    fun openTicket(code: String) = viewModelScope.launch {
        _updating.value = code; _error.value = null
        try { _ticket.value = repo.appointmentTicket(code) }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("Esta cita todavía no tiene ticket.") }
        finally { _updating.value = null }
    }
}

class ReportsViewModel : ViewModel() {
    private val repo = AppContainer.urbanRepository
    private val _manifest = MutableStateFlow(ReportManifest()); val manifest = _manifest.asStateFlow()
    private val _report = MutableStateFlow<ReportData?>(null); val report = _report.asStateFlow()
    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null); val error = _error.asStateFlow()

    fun loadManifest() = viewModelScope.launch {
        try { _manifest.value = repo.reportManifest() }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudieron cargar los tipos de reporte.") }
    }

    fun generate(type: String, startDate: String?, endDate: String?) = viewModelScope.launch {
        _busy.value = true; _error.value = null; _report.value = null
        try { _report.value = repo.exportReport(type, "json", startDate, endDate) }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo generar el reporte.") }
        finally { _busy.value = false }
    }
}


class SocialFeedViewModel @JvmOverloads constructor(private val repo: UrbanRepository = AppContainer.urbanRepository) : ViewModel() {
    private val _feed = MutableStateFlow(SocialFeedResponse()); val feed = _feed.asStateFlow()
    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null); val error = _error.asStateFlow()
    private val _pending = MutableStateFlow<Set<String>>(emptySet()); val pending = _pending.asStateFlow()
    private val _bookingIds = MutableStateFlow<Map<String, String>>(emptyMap()); val bookingIds = _bookingIds.asStateFlow()
    private val _hasMore = MutableStateFlow(false); val hasMore = _hasMore.asStateFlow()
    private val _loadingMore = MutableStateFlow(false); val loadingMore = _loadingMore.asStateFlow()
    private var page = 1
    private fun lastPage(response: SocialFeedResponse) = runCatching { response.meta?.get("last_page")?.asInt }.getOrNull() ?: 1

    fun load(canBook: Boolean = false) {
        if (_busy.value || _loadingMore.value || _pending.value.isNotEmpty()) return
        _busy.value = true; _error.value = null
        if (!canBook) _bookingIds.value = emptyMap()
        viewModelScope.launch {
            try {
                val response = repo.socialFeed(1)
                ensureActive()
                _feed.value = response; page = 1; _hasMore.value = page < lastPage(response)
                if (canBook) {
                    val barbers = try { repo.barbers() }
                    catch (e: CancellationException) { throw e }
                    catch (e: Exception) { _bookingIds.value = emptyMap(); throw e }
                    ensureActive()
                    _bookingIds.value = barbers.mapNotNull { barber -> barber.user?.id?.let { it to barber.id } }.toMap()
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo actualizar el muro o preparar la reserva. Reintenta la consulta.") }
            finally { _busy.value = false }
        }
    }

    fun loadMore() {
        if (_busy.value || _loadingMore.value || !_hasMore.value || _pending.value.isNotEmpty()) return
        _loadingMore.value = true; _error.value = null
        viewModelScope.launch {
            try {
                val response = repo.socialFeed(page + 1)
                ensureActive()
                _feed.value = response.copy(data = (_feed.value.data + response.data).distinctBy { it.id })
                page += 1; _hasMore.value = page < lastPage(response)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudieron cargar más publicaciones.") }
            finally { _loadingMore.value = false }
        }
    }

    private fun action(id: String, fallback: String, block: suspend () -> Unit) {
        if (_busy.value || id in _pending.value) return
        _pending.value += id; _error.value = null
        viewModelScope.launch {
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { _error.value = e.toFriendlyMessage(fallback) }
            finally { _pending.value -= id }
        }
    }

    private fun updateWork(id: String, transform: (SocialWork) -> SocialWork) {
        _feed.value = _feed.value.copy(data = _feed.value.data.map { if (it.id == id) transform(it) else it })
    }

    fun toggleReaction(id: String) = action(id, "No se pudo reaccionar.") {
            val res = repo.reactWork(id)
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            updateWork(id) { it.copy(isReacted = res.status == "added", reactionsCount = res.count ?: it.reactionsCount) }
    }

    fun toggleSave(id: String) = action(id, "No se pudo guardar.") {
            val res = repo.saveWork(id)
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            val added = res.status == "added"
            updateWork(id) { it.copy(isSaved = added, savedCount = (it.savedCount + if (added) 1 else -1).coerceAtLeast(0)) }
    }

    private fun hideComment(workId: String, commentId: String, authorName: String? = null) = updateWork(workId) { work ->
        val kept = work.comments.filterNot { c -> c.id == commentId || (authorName != null && c.user?.name == authorName && !c.isMine) }
        work.copy(comments = kept, commentsCount = (work.commentsCount - (work.comments.size - kept.size)).coerceAtLeast(0))
    }

    /** Reporta un comentario ajeno (política de contenido de usuarios de Google Play) y lo oculta de inmediato. */
    fun reportComment(workId: String, commentId: String, reason: String, details: String? = null, onDone: () -> Unit = {}) =
        action(workId, "No se pudo enviar el reporte.") {
            repo.reportComment(commentId, reason, details)
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            hideComment(workId, commentId)
            onDone()
        }

    /** Bloquea al autor del comentario: sus comentarios dejan de mostrarse a esta cuenta. */
    fun blockCommentAuthor(workId: String, commentId: String, onDone: () -> Unit = {}) =
        action(workId, "No se pudo bloquear al usuario.") {
            val authorName = _feed.value.data.firstOrNull { it.id == workId }?.comments?.firstOrNull { it.id == commentId }?.user?.name
            repo.blockCommentAuthor(commentId)
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            hideComment(workId, commentId, authorName)
            onDone()
        }

    fun comment(id: String, text: String, onDone: () -> Unit) {
        val comment = text.trim()
        if (comment.isEmpty() || comment.length > FormLimits.COMMENT_MAX) return
        action(id, "No se pudo publicar el comentario.") {
            val res = repo.commentWork(id, comment)
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            val newComment = res.data
            updateWork(id) {
                it.copy(
                    commentsCount = it.commentsCount + 1,
                    comments = if (newComment != null) (listOf(newComment) + it.comments).take(3) else it.comments
                )
            }
            onDone()
        }
    }
}

class InsightsViewModel : ViewModel() {
    private val repo = AppContainer.urbanRepository
    private val _data = MutableStateFlow(InsightsData()); val data = _data.asStateFlow()
    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null); val error = _error.asStateFlow()
    fun load() = viewModelScope.launch {
        _busy.value = true; _error.value = null
        try { _data.value = repo.predictionInsights() }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudieron generar los insights.") }
        finally { _busy.value = false }
    }
}

class AnalyticsViewModel : ViewModel() {
    private val repo = AppContainer.urbanRepository
    private val _data = MutableStateFlow(AnalyticsResponse()); val data = _data.asStateFlow()
    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null); val error = _error.asStateFlow()
    fun load() = viewModelScope.launch {
        _busy.value = true; _error.value = null
        try { _data.value = repo.analytics() }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo cargar la analítica.") }
        finally { _busy.value = false }
    }
}
