package com.urbanblade.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.urbanblade.mobile.core.network.AppContainer
import com.urbanblade.mobile.data.model.SuggestedService
import com.urbanblade.mobile.data.repository.UrbanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

/**
 * Un mensaje del chat. Las respuestas de Bladebot guardan la pregunta que contestan ([question])
 * para mandar el "¿te sirvió?"; [helpful] es null mientras el usuario no califica.
 */
data class ChatBubble(
    val id: Long,
    val fromUser: Boolean,
    val text: String,
    val question: String? = null,
    val helpful: Boolean? = null,
    /** Servicio que la IA recomendó en esta respuesta: la burbuja muestra «Reservar». */
    val suggestion: SuggestedService? = null
)

data class ChatState(
    val messages: List<ChatBubble> = emptyList(),
    val loadingHistory: Boolean = false,
    val typing: Boolean = false,
    val error: String? = null
)

/**
 * Bladebot: retoma el historial guardado del usuario, envía preguntas, recibe el "¿te sirvió?"
 * y permite empezar una conversación nueva (borra el historial en el servidor).
 */
class ChatbotViewModel @JvmOverloads constructor(
    private val repo: UrbanRepository = AppContainer.urbanRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()
    private val ids = AtomicLong()

    fun loadHistory() = viewModelScope.launch {
        if (_state.value.messages.isNotEmpty()) return@launch
        _state.update { it.copy(loadingHistory = true) }
        // Sin historial (o sin sesión) se empieza vacío: no es un error que valga mostrar.
        val history = runCatching { repo.chatbotHistory() }.getOrDefault(emptyList())
        val restored = history.flatMap { item ->
            val question = item.message.orEmpty()
            listOfNotNull(
                question.takeIf { it.isNotBlank() }?.let { ChatBubble(ids.incrementAndGet(), true, it) },
                item.response?.takeIf { it.isNotBlank() }?.let { ChatBubble(ids.incrementAndGet(), false, it, question = question) }
            )
        }
        _state.update { it.copy(messages = restored, loadingHistory = false) }
    }

    fun send(text: String) = viewModelScope.launch {
        val question = text.trim()
        if (question.isBlank() || _state.value.typing) return@launch
        _state.update { it.copy(messages = it.messages + ChatBubble(ids.incrementAndGet(), true, question), typing = true, error = null) }
        try {
            val r = repo.chatbot(question)
            val answer = sequenceOf("response", "answer", "message", "reply")
                .mapNotNull { k -> r.get(k)?.takeIf { !it.isJsonNull }?.asString }
                .firstOrNull() ?: "No encontré una respuesta. Intenta preguntarlo de otra forma."
            val suggestion = runCatching {
                r.get("suggested_service")?.takeIf { it.isJsonObject }?.let { com.google.gson.Gson().fromJson(it, SuggestedService::class.java) }
            }.getOrNull()
            _state.update { it.copy(messages = it.messages + ChatBubble(ids.incrementAndGet(), false, answer, question = question, suggestion = suggestion)) }
        } catch (e: Exception) {
            _state.update { it.copy(error = e.toFriendlyMessage("Bladebot no pudo responder ahora. Intenta de nuevo en un momento.")) }
        } finally {
            _state.update { it.copy(typing = false) }
        }
    }

    /** Califica una respuesta una sola vez; si el servidor falla, se regresa a sin calificar. */
    fun rate(bubble: ChatBubble, helpful: Boolean) = viewModelScope.launch {
        val question = bubble.question ?: return@launch
        if (bubble.helpful != null) return@launch
        setHelpful(bubble.id, helpful)
        runCatching { repo.chatbotFeedback(question, bubble.text, helpful) }.onFailure { setHelpful(bubble.id, null) }
    }

    fun newConversation() = viewModelScope.launch {
        runCatching { repo.clearChatbot() }
            .onSuccess { _state.update { ChatState() } }
            .onFailure { e -> _state.update { it.copy(error = (e as? Exception)?.toFriendlyMessage("No se pudo borrar la conversación.") ?: "No se pudo borrar la conversación.") } }
    }

    fun dismissError() = _state.update { it.copy(error = null) }

    private fun setHelpful(id: Long, helpful: Boolean?) = _state.update { s ->
        s.copy(messages = s.messages.map { if (it.id == id) it.copy(helpful = helpful) else it })
    }
}
