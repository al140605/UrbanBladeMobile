package com.urbanblade.mobile.ui.viewmodel

import com.urbanblade.mobile.data.model.GiftCard
import com.urbanblade.mobile.data.model.MessageResponse
import com.urbanblade.mobile.data.model.ModuleFeatures
import com.urbanblade.mobile.data.model.ReferralInfo
import com.urbanblade.mobile.data.repository.UrbanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * WalletViewModel agrega cinco fuentes independientes (membresía, paquetes,
 * gift cards, referidos, lealtad) -- si una falla (p.ej. el cliente nunca ha
 * tenido membresía y ese endpoint no aplica), las demás secciones deben
 * seguir mostrando datos en vez de tumbar toda la pantalla.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WalletViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: UrbanRepository
    private val allOn = ModuleFeatures(packages = true, giftCards = true, referrals = true, raffles = true)

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repo = mock()
        // Mockito no reconoce el tipo de retorno real de una función suspend
        // (a nivel de reflection JVM es Object, no List<T>) -- sin stub
        // explícito devuelve null crudo en vez de una lista vacía, lo que
        // rompe el constructor no-nulo de WalletData. Se fijan defaults
        // seguros aquí; cada test sobreescribe solo lo que le interesa.
        runTest(dispatcher) {
            whenever(repo.clientLoyalty()).thenReturn(null)
            whenever(repo.myMembership()).thenReturn(null)
            whenever(repo.myPackages()).thenReturn(emptyList())
            whenever(repo.myGiftCards()).thenReturn(emptyList())
            whenever(repo.myReferrals()).thenReturn(ReferralInfo())
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `load avisa si una fuente falla sin descartar las otras`() = runTest(dispatcher) {
        whenever(repo.myMembership()).thenThrow(RuntimeException("sin membresía"))

        val vm = WalletViewModel(repo) { allOn }
        vm.load()
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(vm.data.value.membership)
        org.junit.Assert.assertNotNull(vm.error.value)
    }

    @Test
    fun `load carga las gift cards del cliente`() = runTest(dispatcher) {
        whenever(repo.myGiftCards()).thenReturn(listOf(GiftCard(code = "GC1", saldo = 100.0)))

        val vm = WalletViewModel(repo) { allOn }
        vm.load()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, vm.data.value.giftCards.size)
        assertEquals("GC1", vm.data.value.giftCards.first().code)
    }

    @Test
    fun `recarga sin red conserva tarjetas previas y ofrece error`() = runTest(dispatcher) {
        whenever(repo.myGiftCards()).thenReturn(listOf(GiftCard(code = "GC1", saldo = 100.0)))
        val vm = WalletViewModel(repo) { allOn }
        vm.load()
        dispatcher.scheduler.advanceUntilIdle()
        whenever(repo.myGiftCards()).thenAnswer { throw java.io.IOException("offline") }
        vm.load()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("GC1", vm.data.value.giftCards.single().code)
        org.junit.Assert.assertNotNull(vm.error.value)
        assertTrue(!vm.loading.value)
    }

    @Test
    fun `con paquetes tarjetas y referidos apagados no se piden y no hay error`() = runTest(dispatcher) {
        // Apagados, el backend responde 404: la pantalla no debe pedirlos ni mostrar un fallo por ellos.
        whenever(repo.myPackages()).thenAnswer { throw java.io.IOException("404") }
        whenever(repo.myGiftCards()).thenAnswer { throw java.io.IOException("404") }
        whenever(repo.myReferrals()).thenAnswer { throw java.io.IOException("404") }

        val vm = WalletViewModel(repo) { ModuleFeatures() }
        vm.load()
        dispatcher.scheduler.advanceUntilIdle()

        verify(repo, never()).myPackages()
        verify(repo, never()).myGiftCards()
        verify(repo, never()).myReferrals()
        assertTrue(vm.data.value.packages.isEmpty())
        assertTrue(vm.data.value.giftCards.isEmpty())
        assertNull(vm.data.value.referrals)
        assertNull(vm.error.value)
    }

    @Test
    fun `al apagarse un modulo se descartan los datos que ya se habian cargado`() = runTest(dispatcher) {
        whenever(repo.myGiftCards()).thenReturn(listOf(GiftCard(code = "GC1", saldo = 100.0)))
        var features = allOn
        val vm = WalletViewModel(repo) { features }
        vm.load()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, vm.data.value.giftCards.size)

        features = ModuleFeatures()
        vm.load()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.data.value.giftCards.isEmpty())
    }

    @Test
    fun `si los interruptores llegan mientras carga se vuelve a cargar con ellos`() = runTest(dispatcher) {
        whenever(repo.myGiftCards()).thenReturn(listOf(GiftCard(code = "GC1", saldo = 100.0)))
        var features = ModuleFeatures()
        val vm = WalletViewModel(repo) { features }
        // Durante la primera carga llegan los interruptores y la pantalla pide otra carga.
        var firstLoad = true
        whenever(repo.myMembership()).thenAnswer {
            if (firstLoad) { firstLoad = false; features = allOn; vm.load() }
            null
        }

        vm.load()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("GC1", vm.data.value.giftCards.single().code)
        assertTrue(!vm.loading.value)
    }

    @Test
    fun `cancelMembership exitoso muestra el mensaje y recarga`() = runTest(dispatcher) {
        whenever(repo.cancelMembership()).thenReturn(MessageResponse("Tu membresía se cancelará al finalizar el periodo actual."))

        val vm = WalletViewModel(repo)
        vm.cancelMembership()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Tu membresía se cancelará al finalizar el periodo actual.", vm.message.value)
        assertTrue(vm.loading.value == false)
    }
}
