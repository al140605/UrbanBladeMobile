package com.urbanblade.mobile.ui.viewmodel

import com.urbanblade.mobile.data.model.ClientRow
import com.urbanblade.mobile.data.model.ClientsResponse
import com.urbanblade.mobile.data.model.InventoryProductRow
import com.urbanblade.mobile.data.model.InventoryResponse
import com.urbanblade.mobile.data.model.OrderMeta
import com.urbanblade.mobile.data.model.OrderRow
import com.urbanblade.mobile.data.model.OrdersResponse
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
 * Los errores de consultar una lista y los de guardar, eliminar o entregar son distintos: un fallo
 * al guardar nunca debe presentarse como «no se pudo cargar», ni una primera consulta fallida como
 * lista vacía. Cubre inventario, clientes y pedidos.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ListErrorSeparationTest {

    private val dispatcher = StandardTestDispatcher()
    private val repo: UrbanRepository = mock()

    @Before fun setup() { Dispatchers.setMain(dispatcher) }

    @After fun tearDown() { Dispatchers.resetMain() }

    private fun producto(id: String) = InventoryResponse(data = listOf(InventoryProductRow(id = id, nombre = "Producto $id")))

    private fun clientes(id: String) = ClientsResponse(data = listOf(ClientRow(id = id)), currentPage = 1, lastPage = 1)

    private fun pedidos(id: String) = OrdersResponse(listOf(OrderRow(id = id)), OrderMeta(1, 1, 1, null))

    // ───────────── Inventario ─────────────

    @Test
    fun `inventario un fallo al eliminar no se presenta como fallo de consulta`() = runTest(dispatcher) {
        whenever(repo.inventoryProducts()).thenReturn(producto("p1"))
        whenever(repo.deleteProduct(any())).thenThrow(IllegalStateException("rechazado"))
        val vm = InventoryViewModel(repo)
        vm.load(); advanceUntilIdle()

        vm.deleteProduct("p1"); advanceUntilIdle()

        assertNotNull(vm.error.value)
        assertNull(vm.listError.value)
        assertTrue(vm.hasLoaded.value)
        assertEquals(listOf("p1"), vm.products.value.data.map { it.id })
    }

    @Test
    fun `inventario una respuesta tardia no pisa la ultima consulta`() = runTest(dispatcher) {
        var llamadas = 0
        whenever(repo.inventoryProducts()).doSuspendableAnswer {
            llamadas += 1
            if (llamadas == 1) {
                withContext(NonCancellable) { delay(100) }
                producto("viejo")
            } else {
                producto("nuevo")
            }
        }
        val vm = InventoryViewModel(repo)

        vm.load(); runCurrent()
        vm.load(); advanceUntilIdle()

        assertEquals(listOf("nuevo"), vm.products.value.data.map { it.id })
        assertFalse(vm.busy.value)
        assertNull(vm.listError.value)
    }

    // ───────────── Clientes ─────────────

    @Test
    fun `clientes primera consulta fallida sigue sin confirmar hasta reintentar`() = runTest(dispatcher) {
        whenever(repo.clients(anyOrNull(), anyOrNull(), any())).thenThrow(IllegalStateException("sin red"))
        val vm = ClientsViewModel(repo)

        vm.load(); advanceUntilIdle()
        assertFalse(vm.hasLoaded.value)
        assertNotNull(vm.listError.value)
        assertNull(vm.error.value)
        assertFalse(vm.busy.value)

        doReturn(clientes("c1")).whenever(repo).clients(anyOrNull(), anyOrNull(), any())
        vm.load(); advanceUntilIdle()
        assertTrue(vm.hasLoaded.value)
        assertNull(vm.listError.value)
        assertEquals(listOf("c1"), vm.clients.value.data.map { it.id })
    }

    @Test
    fun `clientes un fallo al eliminar no se presenta como fallo de consulta`() = runTest(dispatcher) {
        whenever(repo.clients(anyOrNull(), anyOrNull(), any())).thenReturn(clientes("c1"))
        whenever(repo.deleteClient(any())).thenThrow(IllegalStateException("rechazado"))
        val vm = ClientsViewModel(repo)
        vm.load(); advanceUntilIdle()

        vm.delete("c1") {}; advanceUntilIdle()

        assertNotNull(vm.error.value)
        assertNull(vm.listError.value)
        assertTrue(vm.hasLoaded.value)
        assertEquals(listOf("c1"), vm.clients.value.data.map { it.id })
    }

    @Test
    fun `clientes buscar rapido descarta la respuesta tardia de la busqueda anterior`() = runTest(dispatcher) {
        whenever(repo.clients(eq("a"), anyOrNull(), eq(1))).doSuspendableAnswer {
            withContext(NonCancellable) { delay(100) }
            clientes("viejo")
        }
        whenever(repo.clients(eq("ab"), anyOrNull(), eq(1))).thenReturn(clientes("nuevo"))
        val vm = ClientsViewModel(repo)

        vm.load("a"); runCurrent()
        vm.load("ab"); advanceUntilIdle()

        assertEquals(listOf("nuevo"), vm.clients.value.data.map { it.id })
        assertFalse(vm.busy.value)
        assertNull(vm.listError.value)
    }

    // ───────────── Pedidos ─────────────

    @Test
    fun `pedidos primera consulta fallida sigue sin confirmar hasta reintentar`() = runTest(dispatcher) {
        whenever(repo.orders(any(), anyOrNull(), anyOrNull())).thenThrow(IllegalStateException("sin red"))
        val vm = OrdersViewModel(repo)

        vm.load(); advanceUntilIdle()
        assertFalse(vm.state.value.loaded)
        assertNotNull(vm.state.value.listError)
        assertNull(vm.state.value.error)

        doReturn(pedidos("a")).whenever(repo).orders(any(), anyOrNull(), anyOrNull())
        vm.load(); advanceUntilIdle()
        assertTrue(vm.state.value.loaded)
        assertNull(vm.state.value.listError)
        assertEquals(listOf("a"), vm.state.value.items.map { it.id })
    }

    @Test
    fun `pedidos un fallo al cancelar no se presenta como fallo de consulta`() = runTest(dispatcher) {
        whenever(repo.orders(any(), anyOrNull(), anyOrNull())).thenReturn(pedidos("a"))
        whenever(repo.cancelOrder(any())).thenThrow(IllegalStateException("rechazado"))
        val vm = OrdersViewModel(repo)
        vm.load(); advanceUntilIdle()

        vm.cancel("a"); advanceUntilIdle()

        assertNotNull(vm.state.value.error)
        assertNull(vm.state.value.listError)
        assertTrue(vm.state.value.loaded)
        assertEquals(listOf("a"), vm.state.value.items.map { it.id })
    }
}
