package com.urbanblade.mobile.ui.viewmodel

import com.urbanblade.mobile.data.model.InventoryMovementRow
import com.urbanblade.mobile.data.model.InventoryMovementStats
import com.urbanblade.mobile.data.model.InventoryMovementsMeta
import com.urbanblade.mobile.data.model.InventoryMovementsResponse
import com.urbanblade.mobile.data.model.WaitlistEntry
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class StaffOperationsViewModelsTest {

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

    private fun page(id: String, current: Int, last: Int) = InventoryMovementsResponse(
        data = listOf(InventoryMovementRow(id = id, tipo = "entrada", cantidad = 3)),
        meta = InventoryMovementsMeta(current, last, 2, InventoryMovementStats(2, 1, 1, 0))
    )

    @Test
    fun `movimientos cargar mas agrega la siguiente pagina y termina en la ultima`() = runTest(dispatcher) {
        whenever(repo.inventoryMovementsPage(eq(1), anyOrNull(), anyOrNull())).thenReturn(page("a", 1, 2))
        whenever(repo.inventoryMovementsPage(eq(2), anyOrNull(), anyOrNull())).thenReturn(page("b", 2, 2))
        val vm = InventoryMovementsViewModel(repo)

        vm.load()
        advanceUntilIdle()
        assertTrue(vm.state.value.hasMore)

        vm.loadMore()
        advanceUntilIdle()
        assertEquals(listOf("a", "b"), vm.state.value.items.map { it.id })
        assertFalse(vm.state.value.hasMore)
    }

    @Test
    fun `el filtro de salidas pide tipo salida al servidor`() = runTest(dispatcher) {
        whenever(repo.inventoryMovementsPage(any(), anyOrNull(), anyOrNull())).thenReturn(page("a", 1, 1))
        val vm = InventoryMovementsViewModel(repo)

        vm.setFilter(MovementFilter.Salidas)
        advanceUntilIdle()

        verify(repo).inventoryMovementsPage(1, "salida", null)
    }

    @Test
    fun `la lista de espera arranca en las que siguen esperando y el filtro se manda al servidor`() = runTest(dispatcher) {
        whenever(repo.waitlistForStaff(anyOrNull())).thenReturn(listOf(WaitlistEntry(id = "w1", estado = "activo")))
        val vm = WaitlistStaffViewModel(repo)

        vm.load()
        advanceUntilIdle()
        verify(repo).waitlistForStaff("activo")
        assertEquals(1, vm.state.value.items.size)

        vm.setFilter(WaitlistFilter.Todas)
        advanceUntilIdle()
        verify(repo).waitlistForStaff(null)
    }
}
