package com.urbanblade.mobile.ui.viewmodel

import com.google.gson.JsonObject
import com.urbanblade.mobile.data.model.ChatHistoryItem
import com.urbanblade.mobile.data.model.MessageResponse
import com.urbanblade.mobile.data.repository.UrbanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ChatbotViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: UrbanRepository

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repo = mock()
        runTest(dispatcher) { whenever(repo.chatbotHistory()).thenReturn(emptyList()) }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `al abrir retoma el historial como pares de pregunta y respuesta`() = runTest(dispatcher) {
        whenever(repo.chatbotHistory()).thenReturn(
            listOf(ChatHistoryItem(message = "¿horario?", response = "De 9 a 20 h."))
        )
        val vm = ChatbotViewModel(repo)

        vm.loadHistory()
        advanceUntilIdle()

        val messages = vm.state.value.messages
        assertEquals(listOf(true, false), messages.map { it.fromUser })
        assertEquals("¿horario?", messages[1].question)
    }

    @Test
    fun `enviar agrega la pregunta y la respuesta y apaga el escribiendo`() = runTest(dispatcher) {
        whenever(repo.chatbot("¿Dónde están?")).thenReturn(JsonObject().apply { addProperty("response", "En el centro.") })
        val vm = ChatbotViewModel(repo)

        vm.send("  ¿Dónde están?  ")
        advanceUntilIdle()

        val s = vm.state.value
        assertEquals(listOf("¿Dónde están?", "En el centro."), s.messages.map { it.text })
        assertFalse(s.typing)
        assertNull(s.error)
    }

    @Test
    fun `si falla la consulta se avisa sin inventar respuesta`() = runTest(dispatcher) {
        whenever(repo.chatbot(any())).thenThrow(RuntimeException("sin red"))
        val vm = ChatbotViewModel(repo)

        vm.send("hola")
        advanceUntilIdle()

        assertEquals(1, vm.state.value.messages.size)
        assertTrue(vm.state.value.error != null)
    }

    @Test
    fun `una respuesta se califica una sola vez`() = runTest(dispatcher) {
        whenever(repo.chatbot("precio")).thenReturn(JsonObject().apply { addProperty("response", "$180") })
        whenever(repo.chatbotFeedback("precio", "$180", true)).thenReturn(MessageResponse("Gracias"))
        val vm = ChatbotViewModel(repo)
        vm.send("precio")
        advanceUntilIdle()
        val answer = vm.state.value.messages.last()

        vm.rate(answer, true)
        advanceUntilIdle()
        vm.rate(vm.state.value.messages.last(), false)
        advanceUntilIdle()

        assertEquals(true, vm.state.value.messages.last().helpful)
        verify(repo, never()).chatbotFeedback("precio", "$180", false)
    }

    @Test
    fun `nueva conversacion borra el historial local solo si el servidor lo borro`() = runTest(dispatcher) {
        whenever(repo.chatbot("hola")).thenReturn(JsonObject().apply { addProperty("response", "¡Hola!") })
        whenever(repo.clearChatbot()).thenReturn(MessageResponse("Historial y perfil limpiados"))
        val vm = ChatbotViewModel(repo)
        vm.send("hola")
        advanceUntilIdle()

        vm.newConversation()
        advanceUntilIdle()

        assertTrue(vm.state.value.messages.isEmpty())
    }

    @Test
    fun `la respuesta con servicio recomendado lleva el boton de reservar`() = runTest(dispatcher) {
        val answer = JsonObject().apply {
            addProperty("response", "Te va muy bien un Corte Clásico.")
            add("suggested_service", JsonObject().apply {
                addProperty("id", "svc-1"); addProperty("nombre", "Corte Clásico"); addProperty("precio", 180); addProperty("duracion_min", 30)
            })
        }
        whenever(repo.chatbot("tengo el cabello rizado")).thenReturn(answer)
        val vm = ChatbotViewModel(repo)

        vm.send("tengo el cabello rizado")
        advanceUntilIdle()

        val bot = vm.state.value.messages.last()
        assertEquals("svc-1", bot.suggestion?.id)
        assertEquals(180.0, bot.suggestion?.precio ?: 0.0, 0.0)
    }

    @Test
    fun `sin servicio recomendado no hay boton`() = runTest(dispatcher) {
        whenever(repo.chatbot("hola")).thenReturn(JsonObject().apply { addProperty("response", "¡Hola!"); add("suggested_service", com.google.gson.JsonNull.INSTANCE) })
        val vm = ChatbotViewModel(repo)

        vm.send("hola")
        advanceUntilIdle()

        assertNull(vm.state.value.messages.last().suggestion)
    }
}
