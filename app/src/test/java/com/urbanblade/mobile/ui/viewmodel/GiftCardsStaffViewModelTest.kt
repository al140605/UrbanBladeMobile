package com.urbanblade.mobile.ui.viewmodel

import com.urbanblade.mobile.data.model.CashGiftCardRequest
import com.urbanblade.mobile.data.model.GiftCard
import com.urbanblade.mobile.data.model.GiftCardSold
import com.urbanblade.mobile.data.repository.UrbanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class GiftCardsStaffViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: UrbanRepository

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repo = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun http(code: Int, message: String) =
        HttpException(Response.error<Any>(code, "{\"message\":\"$message\"}".toResponseBody("application/json".toMediaType())))

    @Test
    fun `un monto fuera de rango no llega al servidor`() = runTest(dispatcher) {
        val vm = GiftCardsStaffViewModel(repo)

        vm.sell(50.0, null, null) {}
        vm.sell(9000.0, null, null) {}
        advanceUntilIdle()

        verify(repo, never()).sellGiftCardCash(any())
        assertEquals("El monto debe estar entre $100 y $5,000.", vm.state.value.sellError)
    }

    @Test
    fun `una venta aceptada guarda el codigo y avisa`() = runTest(dispatcher) {
        whenever(repo.sellGiftCardCash(any())).thenReturn(GiftCardSold("ab12cd34", 500.0))
        val vm = GiftCardsStaffViewModel(repo)
        var done = false

        vm.sell(500.0, "Ana", "ana@test.local") { done = true }
        advanceUntilIdle()

        assertTrue(done)
        assertEquals("ab12cd34", vm.state.value.sold?.code)
        val body = argumentCaptor<CashGiftCardRequest>()
        verify(repo).sellGiftCardCash(body.capture())
        assertEquals(500.0, body.firstValue.monto, 0.0)
        assertEquals("ana@test.local", body.firstValue.destinatarioEmail)
    }

    @Test
    fun `un 422 muestra el mensaje del servidor y no marca la venta como hecha`() = runTest(dispatcher) {
        whenever(repo.sellGiftCardCash(any())).thenThrow(http(422, "Monto no permitido."))
        val vm = GiftCardsStaffViewModel(repo)
        var done = false

        vm.sell(500.0, null, null) { done = true }
        advanceUntilIdle()

        assertFalse(done)
        assertNull(vm.state.value.sold)
        assertEquals("Monto no permitido.", vm.state.value.sellError)
    }

    @Test
    fun `la consulta normaliza el codigo a minusculas sin espacios`() = runTest(dispatcher) {
        whenever(repo.giftCardByCode("ab12cd34")).thenReturn(GiftCard("ab12cd34", 300.0, 500.0, "activa"))
        val vm = GiftCardsStaffViewModel(repo)

        vm.lookup("  AB12CD34 ")
        advanceUntilIdle()

        assertEquals(300.0, vm.state.value.found?.saldo ?: 0.0, 0.0)
    }

    @Test
    fun `un codigo inexistente muestra el mensaje del servidor y uno vacio ni consulta`() = runTest(dispatcher) {
        whenever(repo.giftCardByCode("nope")).thenThrow(http(404, "Código no válido o tarjeta sin saldo."))
        val vm = GiftCardsStaffViewModel(repo)

        vm.lookup("nope")
        advanceUntilIdle()
        assertEquals("Código no válido o tarjeta sin saldo.", vm.state.value.lookupError)
        assertNull(vm.state.value.found)

        vm.lookup("   ")
        advanceUntilIdle()
        assertEquals("Escribe el código de la tarjeta.", vm.state.value.lookupError)
    }
}
