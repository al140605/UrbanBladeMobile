package com.urbanblade.mobile.ui.viewmodel

import com.urbanblade.mobile.data.model.TransferInfo
import com.urbanblade.mobile.data.model.TransferQuote
import com.urbanblade.mobile.data.repository.UrbanRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class TransferQuoteLoadingTest {
    private val dispatcher = StandardTestDispatcher()
    private val repo: UrbanRepository = mock()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }
    @Test fun `changing appointment clears previous quote before request finishes`() = runTest(dispatcher) {
        whenever(repo.transferInfo("a", 0.0)).thenReturn(TransferInfo(cotizacion = TransferQuote(appointmentId = "a")))
        whenever(repo.transferInfo("b", 0.0)).thenReturn(TransferInfo(cotizacion = TransferQuote(appointmentId = "b")))
        val vm = AppointmentsViewModel(repo)
        vm.loadTransferInfo("a"); advanceUntilIdle()
        assertEquals("a", vm.transferInfo.value?.cotizacion?.appointmentId)
        vm.loadTransferInfo("b")
        assertNull(vm.transferInfo.value)
        advanceUntilIdle()
        assertEquals("b", vm.transferInfo.value?.cotizacion?.appointmentId)
    }
    @Test fun `late quote cannot replace newer tip`() = runTest(dispatcher) {
        whenever(repo.transferInfo("a", 0.0)).doSuspendableAnswer {
            withContext(NonCancellable) { delay(100) }
            TransferInfo(cotizacion = TransferQuote(appointmentId = "a", propina = 0.0))
        }
        whenever(repo.transferInfo("a", 20.0)).thenReturn(TransferInfo(cotizacion = TransferQuote(appointmentId = "a", propina = 20.0)))
        val vm = AppointmentsViewModel(repo)
        vm.loadTransferInfo("a", 0.0); runCurrent()
        vm.loadTransferInfo("a", 20.0); advanceUntilIdle()
        assertEquals(20.0, vm.transferInfo.value?.cotizacion?.propina)
    }
    @Test fun `network error is not reported as missing bank configuration`() = runTest(dispatcher) {
        whenever(repo.transferInfo("a", 0.0)).thenThrow(RuntimeException("sin red"))
        val vm = AppointmentsViewModel(repo)
        vm.loadTransferInfo("a"); advanceUntilIdle()
        assertNull(vm.transferInfo.value)
        assertNotNull(vm.transferInfoError.value)
    }
    @Test fun `booking requests quote for selected service and tip`() = runTest(dispatcher) {
        whenever(repo.transferInfo(propina = 20.0, serviceId = "s")).thenReturn(TransferInfo(cotizacion = TransferQuote(serviceId = "s", propina = 20.0)))
        val vm = BookingViewModel(repo)
        vm.loadTransferQuote("s", 20.0); advanceUntilIdle()
        assertEquals("s", vm.transferInfo.value?.cotizacion?.serviceId)
        assertNull(vm.transferInfoError.value)
    }
}
