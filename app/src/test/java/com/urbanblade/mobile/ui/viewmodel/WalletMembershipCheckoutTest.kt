package com.urbanblade.mobile.ui.viewmodel

import com.urbanblade.mobile.data.model.SubscribeMembershipData
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/** Contratar membresía desde Beneficios: barber da el client_secret y la pantalla confirma con Stripe. */
@OptIn(ExperimentalCoroutinesApi::class)
class WalletMembershipCheckoutTest {

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

    @Test
    fun `contratar deja listo el cobro con la tarjeta guardada elegida`() = runTest(dispatcher) {
        whenever(repo.subscribeMembership("plan-oro")).thenReturn(SubscribeMembershipData(clientSecret = "pi_secret"))
        val vm = WalletViewModel(repo)

        vm.subscribe("plan-oro", "pm_guardada")
        advanceUntilIdle()

        assertEquals(MembershipCheckout("pi_secret", "pm_guardada"), vm.checkout.value)
        assertNull(vm.error.value)
    }

    @Test
    fun `sin client_secret no intenta cobrar y avisa`() = runTest(dispatcher) {
        whenever(repo.subscribeMembership("plan-oro")).thenReturn(SubscribeMembershipData(clientSecret = null))
        val vm = WalletViewModel(repo)

        vm.subscribe("plan-oro", null)
        advanceUntilIdle()

        assertNull(vm.checkout.value)
        assertNotNull(vm.error.value)
        assertFalse(vm.busy.value)
    }

    @Test
    fun `cancelar el pago de Stripe no marca error y libera la hoja`() = runTest(dispatcher) {
        whenever(repo.subscribeMembership("plan-oro")).thenReturn(SubscribeMembershipData(clientSecret = "pi_secret"))
        val vm = WalletViewModel(repo)
        vm.subscribe("plan-oro", null)
        advanceUntilIdle()

        vm.onCheckoutResult(ok = false, canceled = true, detail = null)
        advanceUntilIdle()

        assertNull(vm.checkout.value)
        assertNull(vm.error.value)
        assertNotNull(vm.message.value)
        assertFalse(vm.busy.value)
    }
}
