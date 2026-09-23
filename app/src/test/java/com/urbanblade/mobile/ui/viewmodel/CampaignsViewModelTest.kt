package com.urbanblade.mobile.ui.viewmodel

import com.urbanblade.mobile.data.model.CampaignRow
import com.urbanblade.mobile.data.model.CampaignsResponse
import com.urbanblade.mobile.data.model.CreateCampaignRequest
import com.urbanblade.mobile.data.model.MessageResponse
import com.urbanblade.mobile.data.repository.UrbanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class CampaignsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: UrbanRepository
    private val request = CreateCampaignRequest("Promo", "20%", null, null, "todos", "ahora", null)

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repo = mock()
        runBlocking {
            whenever(repo.campaigns()).thenReturn(
                CampaignsResponse(mapOf("vip" to "VIP"), mapOf("todos" to 12), listOf(CampaignRow(id = "1", titulo = "Promo")))
            )
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `carga trae historial y conteos por segmento`() = runTest(dispatcher) {
        val vm = CampaignsViewModel(repo)

        vm.load()
        advanceUntilIdle()

        assertEquals(12, vm.state.value.data.segmentCounts["todos"])
        assertEquals(1, vm.state.value.data.data.size)
    }

    @Test
    fun `un envio aceptado muestra el mensaje del servidor y cierra`() = runTest(dispatcher) {
        whenever(repo.createCampaign(any())).thenReturn(MessageResponse("Campaña enviada a 12 cliente(s)."))
        val vm = CampaignsViewModel(repo)
        var closed = false

        vm.create(request) { closed = true }
        advanceUntilIdle()

        assertTrue(closed)
        assertEquals("Campaña enviada a 12 cliente(s).", vm.state.value.notice)
        assertFalse(vm.state.value.sending)
    }

    @Test
    fun `un 422 conserva el formulario abierto y explica el motivo`() = runTest(dispatcher) {
        val body = "{\"message\":\"El segmento elegido no tiene clientes.\"}".toResponseBody("application/json".toMediaType())
        whenever(repo.createCampaign(any())).thenThrow(HttpException(Response.error<Any>(422, body)))
        val vm = CampaignsViewModel(repo)
        var closed = false

        vm.create(request) { closed = true }
        advanceUntilIdle()

        assertFalse(closed)
        assertEquals("El segmento elegido no tiene clientes.", vm.state.value.error)
        assertFalse(vm.state.value.sending)
    }
}
