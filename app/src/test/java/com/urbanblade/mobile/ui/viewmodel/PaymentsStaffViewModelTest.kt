package com.urbanblade.mobile.ui.viewmodel

import com.urbanblade.mobile.data.model.MessageResponse
import com.urbanblade.mobile.data.model.PaymentRow
import com.urbanblade.mobile.data.model.PaymentsMeta
import com.urbanblade.mobile.data.model.PaymentsResponse
import com.urbanblade.mobile.data.model.PaymentsStats
import com.urbanblade.mobile.data.model.PendingPaymentRow
import com.urbanblade.mobile.data.model.PendingPaymentsResponse
import com.urbanblade.mobile.data.repository.UrbanRepository
import com.urbanblade.mobile.ui.screens.ReportRange
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentsStaffViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: UrbanRepository

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repo = mock()
        runBlocking {
            whenever(repo.paymentsForStaff(any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(firstPage())
            whenever(repo.pendingPayments()).thenReturn(PendingPaymentsResponse(listOf(PendingPaymentRow(id = "p1", monto = 180.0))))
            whenever(repo.pendingDeposits()).thenReturn(PendingPaymentsResponse(listOf(PendingPaymentRow(id = "d1", monto = 450.0))))
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun firstPage() = PaymentsResponse(
        data = listOf(PaymentRow(id = "a", monto = 100.0, metodoPago = "efectivo")),
        meta = PaymentsMeta(currentPage = 1, lastPage = 2, total = 2, stats = PaymentsStats(180.0, 900.0, 12, mapOf("efectivo" to 8, "tarjeta" to 4)))
    )

    @Test
    fun `carga usa las estadisticas del servidor y trae los pendientes`() = runTest(dispatcher) {
        val vm = PaymentsStaffViewModel(repo)

        vm.load()
        advanceUntilIdle()

        assertEquals(180.0, vm.state.value.stats.totalHoy, 0.0)
        assertEquals(12, vm.state.value.stats.count)
        assertEquals(1, vm.state.value.pending.size)
        assertTrue(vm.state.value.hasMore)
    }

    @Test
    fun `el filtro de metodo y el rango se mandan al servidor`() = runTest(dispatcher) {
        val vm = PaymentsStaffViewModel(repo)

        vm.setMethod(PaymentMethodFilter.Tarjeta)
        advanceUntilIdle()
        verify(repo).paymentsForStaff(eq(1), anyOrNull(), eq("tarjeta"), anyOrNull(), anyOrNull())

        vm.setRange(ReportRange.Last7)
        advanceUntilIdle()
        assertEquals(ReportRange.Last7, vm.state.value.range)
    }

    @Test
    fun `rechazar sin motivo no llama al servidor`() = runTest(dispatcher) {
        val vm = PaymentsStaffViewModel(repo)

        vm.reject("p1", "   ") {}
        advanceUntilIdle()

        verify(repo, never()).rejectPayment(any(), any())
        assertEquals("Escribe el motivo del rechazo.", vm.state.value.error)
    }

    @Test
    fun `rechazar con motivo lo manda tal cual y recarga`() = runTest(dispatcher) {
        whenever(repo.rejectPayment(any(), any())).thenReturn(MessageResponse("ok"))
        val vm = PaymentsStaffViewModel(repo)
        var done = false

        vm.reject("p1", "  Monto ilegible ") { done = true }
        advanceUntilIdle()

        verify(repo).rejectPayment("p1", "Monto ilegible")
        assertTrue(done)
        assertEquals("Comprobante rechazado. El cliente puede subir uno nuevo.", vm.state.value.notice)
        assertNull(vm.state.value.busyPaymentId)
    }

    @Test
    fun `carga tambien los anticipos por revisar`() = runTest(dispatcher) {
        val vm = PaymentsStaffViewModel(repo)
        vm.load()
        advanceUntilIdle()

        assertEquals(listOf("d1"), vm.state.value.deposits.map { it.id })
        assertEquals(listOf("p1"), vm.state.value.pending.map { it.id })
    }

    @Test
    fun `si fallan los anticipos el historial se muestra igual`() = runTest(dispatcher) {
        whenever(repo.pendingDeposits()).thenThrow(RuntimeException("sin red"))
        val vm = PaymentsStaffViewModel(repo)
        vm.load()
        advanceUntilIdle()

        assertTrue(vm.state.value.deposits.isEmpty())
        assertEquals(1, vm.state.value.items.size)
        assertNull(vm.state.value.error)
    }

    @Test
    fun `aprobar un anticipo usa su endpoint y lo explica`() = runTest(dispatcher) {
        whenever(repo.approveDeposit(any())).thenReturn(MessageResponse("ok"))
        val vm = PaymentsStaffViewModel(repo)

        vm.approveDeposit("d1")
        advanceUntilIdle()

        verify(repo).approveDeposit("d1")
        verify(repo, never()).approvePayment(any())
        assertEquals("Anticipo aprobado. Se descontará al cobrar la cita.", vm.state.value.notice)
    }

    @Test
    fun `rechazar un anticipo exige motivo`() = runTest(dispatcher) {
        val vm = PaymentsStaffViewModel(repo)
        var closed = false
        vm.rejectDeposit("d1", "   ") { closed = true }
        advanceUntilIdle()

        assertEquals("Escribe el motivo del rechazo.", vm.state.value.error)
        assertFalse(closed)
        verify(repo, never()).rejectDeposit(any(), any())
    }

    @Test
    fun `aprobar avisa que la cita quedo completada`() = runTest(dispatcher) {
        whenever(repo.approvePayment(any())).thenReturn(MessageResponse("ok"))
        val vm = PaymentsStaffViewModel(repo)

        vm.approve("p1")
        advanceUntilIdle()

        assertEquals("Comprobante aprobado. La cita quedó completada.", vm.state.value.notice)
    }

    @Test
    fun `sin url de comprobante avisa en vez de abrir algo vacio`() = runTest(dispatcher) {
        whenever(repo.paymentReceiptUrl("a")).thenReturn(null)
        val vm = PaymentsStaffViewModel(repo)
        var opened = false

        vm.receiptUrl("a") { opened = true }
        advanceUntilIdle()

        assertFalse(opened)
        assertEquals("Este pago no tiene comprobante disponible.", vm.state.value.error)
    }
}
