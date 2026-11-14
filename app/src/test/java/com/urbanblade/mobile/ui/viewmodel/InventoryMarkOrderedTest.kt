package com.urbanblade.mobile.ui.viewmodel

import com.urbanblade.mobile.data.model.InventoryProductRow
import com.urbanblade.mobile.data.model.InventoryResponse
import com.urbanblade.mobile.data.model.MessageResponse
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
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class InventoryMarkOrderedTest {
    private val dispatcher = StandardTestDispatcher()
    private val repo: UrbanRepository = mock()

    @Before fun setup() { Dispatchers.setMain(dispatcher) }

    @After fun tearDown() { Dispatchers.resetMain() }

    private fun lista() = InventoryResponse(data = listOf(InventoryProductRow(id = "p1", nombre = "Cera")))

    @Test
    fun `marcar como pedido muestra el mensaje del servidor y recarga la lista`() = runTest(dispatcher) {
        whenever(repo.inventoryProducts()) doReturn lista()
        whenever(repo.markProductOrdered("p1")) doReturn MessageResponse(message = "Producto marcado como pedido.")
        val vm = InventoryViewModel(repo)

        vm.markOrdered("p1")
        advanceUntilIdle()

        assertEquals("Producto marcado como pedido.", vm.message.value)
        assertNull(vm.error.value)
        assertFalse(vm.saving.value)
        verify(repo, times(1)).inventoryProducts()
    }

    @Test
    fun `si el servidor falla informa el error y no recarga`() = runTest(dispatcher) {
        whenever(repo.markProductOrdered("p1")).thenAnswer { throw RuntimeException("sin red") }
        val vm = InventoryViewModel(repo)

        vm.markOrdered("p1")
        advanceUntilIdle()

        assertNotNull(vm.error.value)
        assertNull(vm.message.value)
        assertFalse(vm.saving.value)
        verify(repo, times(0)).inventoryProducts()
    }
}
