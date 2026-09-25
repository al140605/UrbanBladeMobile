package com.urbanblade.mobile.data.model

/*
 * Bladebot (barber: Chatbot\ChatbotController y Api\...\ChatbotManagementController). Las
 * preguntas de un usuario con sesión se guardan en Mongo; GET chatbot/history devuelve las
 * últimas 20 para retomar la conversación.
 */

/** GET chatbot/history: cada elemento es una pregunta con la respuesta que dio Bladebot. */
data class ChatHistoryResponse(val history: List<ChatHistoryItem> = emptyList())

data class ChatHistoryItem(
    val timestamp: String? = null,
    val message: String? = null,
    val response: String? = null
)

/** POST chatbot/feedback: si la respuesta le sirvió al usuario (entrena al bot). */
data class ChatFeedbackRequest(val message: String, val response: String, val helpful: Boolean)
