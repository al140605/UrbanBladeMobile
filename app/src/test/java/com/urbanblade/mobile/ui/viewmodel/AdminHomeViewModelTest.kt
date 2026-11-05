package com.urbanblade.mobile.ui.viewmodel

import com.google.gson.JsonObject
import com.urbanblade.mobile.data.model.DashboardResponse
import com.urbanblade.mobile.data.model.PendingPaymentRow
import com.urbanblade.mobile.data.model.PendingPaymentsResponse
import com.urbanblade.mobile.data.repository.UrbanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.mockito.kotlin.verify
import org.mockito.kotlin.times
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class AdminHomeViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: UrbanRepository

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repo = mock()
        runBlocking {
            val kpis = JsonObject().apply {
                addProperty("income_today", 3850)
                addProperty("appointments_today", 12)
                addProperty("low_stock_count", 3)
            }
            whenever(repo.dashboard()).thenReturn(
                DashboardResponse(data = JsonObject().apply { add("kpis", kpis) })
            )
            whenever(repo.pendingPayments()).thenReturn(
                PendingPaymentsResponse(listOf(PendingPaymentRow(id = "p1"), PendingPaymentRow(id = "p2")))
            )
            whenever(repo.adminStats()).thenReturn(
                JsonObject().apply {
                    add("stats", JsonObject().apply { addProperty("occupancyRate", 80) })
                }
            )
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `carga el pulso operativo del administrador`() = runTest(dispatcher) {
        val vm = AdminHomeViewModel(repo)

        vm.load()
        advanceUntilIdle()

        assertEquals(3850.0, vm.state.value.kpis?.get("income_today")?.asDouble ?: 0.0, 0.0)
        assertEquals(2, vm.state.value.pendingPayments)
        assertEquals(80, vm.state.value.occupancyRate)
        assertFalse(vm.state.value.loading)
    }

    @Test
    fun `si ocupacion falla conserva el resto del resumen`() = runTest(dispatcher) {
        whenever(repo.adminStats()).thenThrow(IllegalStateException("sin métricas"))
        val vm = AdminHomeViewModel(repo)

        vm.load()
        advanceUntilIdle()

        assertEquals(2, vm.state.value.pendingPayments)
        assertEquals(null, vm.state.value.occupancyRate)
        assertFalse(vm.state.value.loading)
    }

    @Test
    fun `fallar pagos pendientes no representa cero ni todo al dia`() = runTest(dispatcher) {
        whenever(repo.pendingPayments()).thenThrow(IllegalStateException("sin red"))
        val vm = AdminHomeViewModel(repo)
        vm.load(); advanceUntilIdle()
        assertNotNull(vm.state.value.kpis)
        assertNull(vm.state.value.pendingPayments)
        assertNotNull(vm.state.value.pendingPaymentsError)
        assertFalse(vm.state.value.loading)
    }

    @Test
    fun `fallar actualizacion conserva resumen y expone aviso`() = runTest(dispatcher) {
        val vm = AdminHomeViewModel(repo)
        vm.load(); advanceUntilIdle()
        val previous = vm.state.value.kpis
        whenever(repo.dashboard()).thenThrow(IllegalStateException("sin red"))
        vm.load(); advanceUntilIdle()
        assertEquals(previous, vm.state.value.kpis)
        assertNotNull(vm.state.value.error)
        assertFalse(vm.state.value.loading)
    }

    @Test
    fun `toques consecutivos no duplican la actualizacion`() = runTest(dispatcher) {
        val vm = AdminHomeViewModel(repo)
        vm.load(); vm.load(); advanceUntilIdle()
        verify(repo, times(1)).dashboard()
    }
}
