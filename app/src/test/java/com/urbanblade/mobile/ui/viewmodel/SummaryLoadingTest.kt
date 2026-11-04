package com.urbanblade.mobile.ui.viewmodel

import com.urbanblade.mobile.data.model.InventoryResponse
import com.urbanblade.mobile.data.repository.UrbanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SummaryLoadingTest {
    private val dispatcher = StandardTestDispatcher()
    private val repo: UrbanRepository = mock()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }

    @Test fun `inventario vacio exitoso se distingue de carga inicial`() = runTest(dispatcher) {
        whenever(repo.inventoryProducts()).thenReturn(InventoryResponse())
        val vm = InventoryViewModel(repo)
        assertFalse(vm.hasLoaded.value)
        vm.load(); advanceUntilIdle()
        assertTrue(vm.hasLoaded.value)
        assertTrue(vm.products.value.data.isEmpty())
        assertFalse(vm.busy.value)
    }

    @Test fun `fallo inicial no se presenta como inventario vacio`() = runTest(dispatcher) {
        whenever(repo.inventoryProducts()).thenThrow(RuntimeException("sin conexion"))
        val vm = InventoryViewModel(repo)
        vm.load(); advanceUntilIdle()
        assertFalse(vm.hasLoaded.value)
        assertNotNull(vm.error.value)
        assertFalse(vm.busy.value)
    }

    @Test fun `fallo al actualizar conserva ultimo resultado confirmado`() = runTest(dispatcher) {
        whenever(repo.inventoryProducts()).thenReturn(InventoryResponse()).thenThrow(RuntimeException("sin conexion"))
        val vm = InventoryViewModel(repo)
        vm.load(); advanceUntilIdle()
        val previous = vm.products.value
        vm.load(); advanceUntilIdle()
        assertTrue(vm.hasLoaded.value)
        assertSame(previous, vm.products.value)
        assertNotNull(vm.error.value)
    }

    @Test fun `agenda vacia confirmada deja de ser carga inicial`() = runTest(dispatcher) {
        whenever(repo.barberAgenda("day", null, 0)).thenReturn(com.urbanblade.mobile.data.model.BarberAgendaResponse())
        val vm = BarberAgendaViewModel(repo)
        assertFalse(vm.hasLoaded.value)
        vm.load(); advanceUntilIdle()
        assertTrue(vm.hasLoaded.value)
        assertTrue(vm.agenda.value.data.isEmpty())
    }

    @Test fun `portafolio con error inicial permite reintentar sin ceros confirmados`() = runTest(dispatcher) {
        whenever(repo.portfolio()).thenThrow(RuntimeException("sin conexion"))
            .thenReturn(com.urbanblade.mobile.data.model.PortfolioResponse())
        val vm = PortfolioViewModel(repo)
        vm.load(); advanceUntilIdle()
        assertFalse(vm.hasLoaded.value)
        assertNotNull(vm.error.value)
        vm.load(); advanceUntilIdle()
        assertTrue(vm.hasLoaded.value)
        assertNull(vm.error.value)
    }
}
