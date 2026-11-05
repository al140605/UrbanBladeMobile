package com.urbanblade.mobile.ui.viewmodel

import com.urbanblade.mobile.data.model.InventoryMovementRow
import com.urbanblade.mobile.data.model.InventoryMovementStats
import com.urbanblade.mobile.data.model.InventoryMovementsMeta
import com.urbanblade.mobile.data.model.InventoryMovementsResponse
import com.urbanblade.mobile.data.model.WaitlistEntry
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
 * Las listas de movimientos y de espera deben distinguir «aún no sé» de «confirmado vacío»:
 * `loaded` solo pasa a true con una primera consulta correcta, un fallo posterior conserva lo
 * confirmado, y una respuesta tardía de un filtro anterior no pisa la última.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StaffListLoadStateTest {

    private val dispatcher = StandardTestDispatcher()
    private val repo: UrbanRepository = mock()

    @Before fun setup() { Dispatchers.setMain(dispatcher) }

    @After fun tearDown() { Dispatchers.resetMain() }

    private fun page(id: String) = InventoryMovementsResponse(
        data = listOf(InventoryMovementRow(id = id, tipo = "entrada", cantidad = 3)),
        meta = InventoryMovementsMeta(1, 1, 1, InventoryMovementStats(1, 1, 0, 0))
    )

    @Test
    fun `movimientos sin primera consulta correcta siguen sin confirmar hasta reintentar`() = runTest(dispatcher) {
        whenever(repo.inventoryMovementsPage(any(), anyOrNull(), anyOrNull())).thenThrow(IllegalStateException("sin red"))
        val vm = InventoryMovementsViewModel(repo)

        vm.load(); advanceUntilIdle()
        assertFalse(vm.state.value.loaded)
        assertNotNull(vm.state.value.error)
        assertFalse(vm.state.value.loading)

        doReturn(page("a")).whenever(repo).inventoryMovementsPage(any(), anyOrNull(), anyOrNull())
        vm.load(); advanceUntilIdle()
        assertTrue(vm.state.value.loaded)
        assertNull(vm.state.value.error)
        assertEquals(listOf("a"), vm.state.value.items.map { it.id })
    }

    @Test
    fun `movimientos conservan lo confirmado cuando falla una actualizacion`() = runTest(dispatcher) {
        whenever(repo.inventoryMovementsPage(any(), anyOrNull(), anyOrNull())).thenReturn(page("a"))
        val vm = InventoryMovementsViewModel(repo)
        vm.load(); advanceUntilIdle()

        whenever(repo.inventoryMovementsPage(any(), anyOrNull(), anyOrNull())).thenThrow(IllegalStateException("sin red"))
        vm.load(); advanceUntilIdle()

        assertTrue(vm.state.value.loaded)
        assertNotNull(vm.state.value.error)
        assertEquals(listOf("a"), vm.state.value.items.map { it.id })
        assertFalse(vm.state.value.loading)
    }

    @Test
    fun `movimientos una respuesta tardia de un filtro anterior no pisa la ultima`() = runTest(dispatcher) {
        whenever(repo.inventoryMovementsPage(eq(1), eq("entrada"), anyOrNull())).doSuspendableAnswer {
            withContext(NonCancellable) { delay(100) }
            page("viejo")
        }
        whenever(repo.inventoryMovementsPage(eq(1), eq("salida"), anyOrNull())).thenReturn(page("nuevo"))
        val vm = InventoryMovementsViewModel(repo)

        vm.setFilter(MovementFilter.Entradas); runCurrent()
        vm.setFilter(MovementFilter.Salidas); advanceUntilIdle()

        assertEquals(listOf("nuevo"), vm.state.value.items.map { it.id })
        assertFalse(vm.state.value.loading)
        assertNull(vm.state.value.error)
    }

    @Test
    fun `lista de espera sin primera consulta correcta sigue sin confirmar hasta reintentar`() = runTest(dispatcher) {
        whenever(repo.waitlistForStaff(anyOrNull())).thenThrow(IllegalStateException("sin red"))
        val vm = WaitlistStaffViewModel(repo)

        vm.load(); advanceUntilIdle()
        assertFalse(vm.state.value.loaded)
        assertNotNull(vm.state.value.error)

        doReturn(listOf(WaitlistEntry(id = "w1", estado = "activo"))).whenever(repo).waitlistForStaff(anyOrNull())
        vm.load(); advanceUntilIdle()
        assertTrue(vm.state.value.loaded)
        assertNull(vm.state.value.error)
        assertEquals(1, vm.state.value.items.size)
    }

    @Test
    fun `lista de espera conserva lo confirmado cuando falla una actualizacion`() = runTest(dispatcher) {
        whenever(repo.waitlistForStaff(anyOrNull())).thenReturn(listOf(WaitlistEntry(id = "w1", estado = "activo")))
        val vm = WaitlistStaffViewModel(repo)
        vm.load(); advanceUntilIdle()

        whenever(repo.waitlistForStaff(anyOrNull())).thenThrow(IllegalStateException("sin red"))
        vm.load(); advanceUntilIdle()

        assertTrue(vm.state.value.loaded)
        assertNotNull(vm.state.value.error)
        assertEquals(1, vm.state.value.items.size)
    }

    @Test
    fun `lista de espera una respuesta tardia de un filtro anterior no pisa la ultima`() = runTest(dispatcher) {
        whenever(repo.waitlistForStaff(eq("activo"))).doSuspendableAnswer {
            withContext(NonCancellable) { delay(100) }
            listOf(WaitlistEntry(id = "viejo", estado = "activo"))
        }
        whenever(repo.waitlistForStaff(eq("reservado"))).thenReturn(listOf(WaitlistEntry(id = "nuevo", estado = "reservado")))
        val vm = WaitlistStaffViewModel(repo)

        vm.load(); runCurrent()
        vm.setFilter(WaitlistFilter.Reservadas); advanceUntilIdle()

        assertEquals(listOf("nuevo"), vm.state.value.items.map { it.id })
        assertFalse(vm.state.value.loading)
    }
}
