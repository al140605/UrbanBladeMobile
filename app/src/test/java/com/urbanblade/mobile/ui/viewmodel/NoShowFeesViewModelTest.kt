package com.urbanblade.mobile.ui.viewmodel

import com.urbanblade.mobile.data.model.MessageResponse
import com.urbanblade.mobile.data.model.NoShowFeeCita
import com.urbanblade.mobile.data.model.NoShowFeeItem
import com.urbanblade.mobile.data.model.NoShowFeesResponse
import com.urbanblade.mobile.data.model.PaymentsResponse
import com.urbanblade.mobile.data.model.PendingPaymentsResponse
import com.urbanblade.mobile.data.model.ServiceTicket
import com.urbanblade.mobile.data.repository.UrbanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/** Flujo de citas V2: adeudos por inasistencia (recepción y cliente) y ticket del servicio terminado. */
@OptIn(ExperimentalCoroutinesApi::class)
class NoShowFeesViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: UrbanRepository

    private val fee = NoShowFeeItem(
        id = "f1", appointmentId = "a1", monto = 100.0, montoBase = 100.0, porcentaje = 50,
        cita = NoShowFeeCita(code = "UB1", fecha = "2026-10-08", servicio = "Corte", cliente = "Ana")
    )

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repo = mock()
        runBlocking {
            whenever(repo.paymentsForStaff(any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(PaymentsResponse())
            whenever(repo.pendingPayments()).thenReturn(PendingPaymentsResponse())
            whenever(repo.pendingDeposits()).thenReturn(PendingPaymentsResponse())
            whenever(repo.noShowFees(anyOrNull())).thenReturn(NoShowFeesResponse(listOf(fee)))
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `recepcion ve los adeudos pendientes al cargar`() = runTest(dispatcher) {
        val vm = PaymentsStaffViewModel(repo)

        vm.load()
        advanceUntilIdle()

        assertEquals(listOf("f1"), vm.state.value.fees.map { it.id })
    }

    @Test
    fun `cobrar un adeudo lo quita de la lista y avisa`() = runTest(dispatcher) {
        whenever(repo.payNoShowFee("f1", "efectivo")).thenReturn(MessageResponse("Cargo cobrado."))
        val vm = PaymentsStaffViewModel(repo)
        vm.load()
        advanceUntilIdle()

        vm.payFee("f1", "efectivo")
        advanceUntilIdle()

        assertTrue(vm.state.value.fees.isEmpty())
        assertEquals("Cargo cobrado. El cliente ya puede volver a reservar.", vm.state.value.notice)
        assertNull(vm.state.value.busyFeeId)
        verify(repo).payNoShowFee("f1", "efectivo")
    }

    @Test
    fun `condonar exige un motivo`() = runTest(dispatcher) {
        val vm = PaymentsStaffViewModel(repo)
        var closed = false

        vm.waiveFee("f1", " a ") { closed = true }
        advanceUntilIdle()

        assertEquals("Escribe el motivo para condonar el cargo.", vm.state.value.error)
        assertEquals(false, closed)
        verify(repo, never()).waiveNoShowFee(any(), any())
    }

    @Test
    fun `condonar con motivo quita el adeudo y cierra`() = runTest(dispatcher) {
        whenever(repo.waiveNoShowFee("f1", "Emergencia médica")).thenReturn(MessageResponse("Cargo condonado."))
        val vm = PaymentsStaffViewModel(repo)
        vm.load()
        advanceUntilIdle()
        var closed = false

        vm.waiveFee("f1", "  Emergencia médica ") { closed = true }
        advanceUntilIdle()

        assertTrue(closed)
        assertTrue(vm.state.value.fees.isEmpty())
        assertEquals("Cargo condonado.", vm.state.value.notice)
    }

    @Test
    fun `el cliente ve cuanto debe y abre su ticket`() = runTest(dispatcher) {
        whenever(repo.noShowFees(anyOrNull())).thenReturn(NoShowFeesResponse(listOf(fee), adeudoTotal = 100.0))
        whenever(repo.appointmentTicket("UB1")).thenReturn(ServiceTicket(folio = "F-ABC123", totalPagado = 220.0))
        val vm = AppointmentsViewModel(repo)

        vm.loadDebt()
        vm.openTicket("UB1")
        advanceUntilIdle()

        assertEquals(100.0, vm.debt.value, 0.0)
        assertEquals("F-ABC123", vm.ticket.value?.folio)
        vm.dismissTicket()
        assertNull(vm.ticket.value)
    }

    @Test
    fun `si falla la consulta del adeudo no se muestra ningun aviso`() = runTest(dispatcher) {
        whenever(repo.noShowFees(anyOrNull())).thenThrow(RuntimeException("sin red"))
        val vm = AppointmentsViewModel(repo)

        vm.loadDebt()
        advanceUntilIdle()

        assertEquals(0.0, vm.debt.value, 0.0)
    }
}
