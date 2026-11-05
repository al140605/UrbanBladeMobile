package com.urbanblade.mobile.ui.viewmodel

import com.urbanblade.mobile.data.model.NoShowFeesResponse
import com.urbanblade.mobile.data.model.PaymentRow
import com.urbanblade.mobile.data.model.PaymentsMeta
import com.urbanblade.mobile.data.model.PaymentsResponse
import com.urbanblade.mobile.data.model.PaymentsStats
import com.urbanblade.mobile.data.model.PendingPaymentRow
import com.urbanblade.mobile.data.model.PendingPaymentsResponse
import com.urbanblade.mobile.data.repository.UrbanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Cobros distingue «aún no sé» de «confirmado», no confunde un fallo al aprobar con un fallo de consulta,
 * descarta la respuesta tardía de un filtro anterior y, sobre todo, nunca afirma «todo al día» cuando la
 * consulta de comprobantes o anticipos falló: el historial se muestra igual, pero con un aviso de fallo parcial.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PaymentsListLoadStateTest {

    private val dispatcher = StandardTestDispatcher()
    private val repo: UrbanRepository = mock()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After fun tearDown() { Dispatchers.resetMain() }

    private fun historial(id: String) = PaymentsResponse(
        data = listOf(PaymentRow(id = id, monto = 100.0, metodoPago = "efectivo")),
        meta = PaymentsMeta(currentPage = 1, lastPage = 1, total = 1, stats = PaymentsStats(180.0, 900.0, 12, emptyMap()))
    )

    /** Prepara las listas de revisión sin fallos; cada prueba rompe la que necesita. */
    private suspend fun listasSanas() {
        whenever(repo.pendingPayments()).thenReturn(PendingPaymentsResponse(listOf(PendingPaymentRow(id = "p1", monto = 180.0))))
        whenever(repo.pendingDeposits()).thenReturn(PendingPaymentsResponse(listOf(PendingPaymentRow(id = "d1", monto = 450.0))))
        whenever(repo.noShowFees(anyOrNull())).thenReturn(NoShowFeesResponse())
    }

    @Test
    fun `cobros primera consulta fallida sigue sin confirmar hasta reintentar`() = runTest(dispatcher) {
        listasSanas()
        whenever(repo.paymentsForStaff(any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())).thenThrow(IllegalStateException("sin red"))
        val vm = PaymentsStaffViewModel(repo)

        vm.load(); advanceUntilIdle()
        assertFalse(vm.state.value.loaded)
        assertNotNull(vm.state.value.listError)
        assertNull(vm.state.value.error)
        assertFalse(vm.state.value.loading)

        doReturn(historial("a")).whenever(repo).paymentsForStaff(any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())
        vm.load(); advanceUntilIdle()
        assertTrue(vm.state.value.loaded)
        assertNull(vm.state.value.listError)
        assertEquals(listOf("a"), vm.state.value.items.map { it.id })
    }

    @Test
    fun `cobros un fallo al aprobar no se presenta como fallo de consulta`() = runTest(dispatcher) {
        listasSanas()
        whenever(repo.paymentsForStaff(any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(historial("a"))
        whenever(repo.approvePayment(any())).thenThrow(IllegalStateException("rechazado"))
        val vm = PaymentsStaffViewModel(repo)
        vm.load(); advanceUntilIdle()

        vm.approve("p1"); advanceUntilIdle()

        assertNotNull(vm.state.value.error)
        assertNull(vm.state.value.listError)
        assertTrue(vm.state.value.loaded)
        assertEquals(listOf("a"), vm.state.value.items.map { it.id })
    }

    @Test
    fun `cobros una respuesta tardia de un filtro anterior no pisa la ultima`() = runTest(dispatcher) {
        listasSanas()
        whenever(repo.paymentsForStaff(eq(1), anyOrNull(), eq("efectivo"), anyOrNull(), anyOrNull())).doSuspendableAnswer {
            withContext(NonCancellable) { delay(100) }
            historial("viejo")
        }
        whenever(repo.paymentsForStaff(eq(1), anyOrNull(), eq("tarjeta"), anyOrNull(), anyOrNull())).thenReturn(historial("nuevo"))
        val vm = PaymentsStaffViewModel(repo)

        vm.setMethod(PaymentMethodFilter.Efectivo); runCurrent()
        vm.setMethod(PaymentMethodFilter.Tarjeta); advanceUntilIdle()

        assertEquals(listOf("nuevo"), vm.state.value.items.map { it.id })
        assertFalse(vm.state.value.loading)
        assertNull(vm.state.value.listError)
    }

    @Test
    fun `cobros si fallan los comprobantes se muestra el historial pero no se afirma todo al dia`() = runTest(dispatcher) {
        listasSanas()
        whenever(repo.paymentsForStaff(any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(historial("a"))
        whenever(repo.pendingPayments()).thenThrow(IllegalStateException("sin red"))
        val vm = PaymentsStaffViewModel(repo)

        vm.load(); advanceUntilIdle()

        assertTrue(vm.state.value.loaded)
        assertEquals(listOf("a"), vm.state.value.items.map { it.id })
        assertTrue(vm.state.value.reviewUnknown)
        assertNotNull(vm.state.value.reviewListsError)
        assertTrue(vm.state.value.reviewListsError!!.contains("comprobantes por revisar"))
        assertNull(vm.state.value.listError)
        assertNull(vm.state.value.error)
    }

    @Test
    fun `cobros el aviso de fallo parcial se limpia cuando una consulta completa sale bien`() = runTest(dispatcher) {
        listasSanas()
        whenever(repo.paymentsForStaff(any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(historial("a"))
        whenever(repo.pendingDeposits()).thenThrow(IllegalStateException("sin red"))
        val vm = PaymentsStaffViewModel(repo)
        vm.load(); advanceUntilIdle()
        assertTrue(vm.state.value.reviewUnknown)

        doReturn(PendingPaymentsResponse(listOf(PendingPaymentRow(id = "d1", monto = 450.0)))).whenever(repo).pendingDeposits()
        vm.load(); advanceUntilIdle()

        assertFalse(vm.state.value.reviewUnknown)
        assertNull(vm.state.value.reviewListsError)
        assertEquals(listOf("d1"), vm.state.value.deposits.map { it.id })
    }

    @Test
    fun `cobros si solo fallan los adeudos se avisa pero el subtitulo no queda desconocido`() = runTest(dispatcher) {
        listasSanas()
        whenever(repo.paymentsForStaff(any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(historial("a"))
        whenever(repo.noShowFees(anyOrNull())).thenThrow(IllegalStateException("sin red"))
        val vm = PaymentsStaffViewModel(repo)

        vm.load(); advanceUntilIdle()

        assertFalse(vm.state.value.reviewUnknown)
        assertNotNull(vm.state.value.reviewListsError)
        assertTrue(vm.state.value.reviewListsError!!.contains("adeudos"))
    }

    @Test
    fun `cobros una consulta cancelada no publica sus comprobantes`() = runTest(dispatcher) {
        whenever(repo.paymentsForStaff(any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(historial("a"))
        whenever(repo.pendingDeposits()).thenReturn(PendingPaymentsResponse())
        whenever(repo.noShowFees(anyOrNull())).thenReturn(NoShowFeesResponse())
        var llamadas = 0
        whenever(repo.pendingPayments()).doSuspendableAnswer {
            llamadas += 1
            if (llamadas == 1) {
                withContext(NonCancellable) { delay(100) }
                PendingPaymentsResponse(listOf(PendingPaymentRow(id = "viejo", monto = 1.0)))
            } else {
                PendingPaymentsResponse(listOf(PendingPaymentRow(id = "nuevo", monto = 2.0)))
            }
        }
        val vm = PaymentsStaffViewModel(repo)

        vm.load(); runCurrent()
        vm.load(); advanceUntilIdle()

        assertEquals(listOf("nuevo"), vm.state.value.pending.map { it.id })
        assertFalse(vm.state.value.reviewUnknown)
        assertFalse(vm.state.value.loading)
    }
}
