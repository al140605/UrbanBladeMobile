package com.urbanblade.mobile.ui.viewmodel

import com.urbanblade.mobile.data.model.ServiceAdminItem
import com.urbanblade.mobile.data.model.ServiceUpsertRequest
import com.urbanblade.mobile.data.model.ServicesAdminMeta
import com.urbanblade.mobile.data.model.ServicesAdminResponse
import com.urbanblade.mobile.data.repository.UrbanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class ServicesAdminViewModelTest {

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

    private fun service(id: String, activo: Boolean = true) = ServiceAdminItem(
        id = id, slug = "corte-$id", nombre = "Corte $id", categoria = "Cabello", precio = 180.0, duracionMin = 30, activo = activo
    )

    private fun page(items: List<ServiceAdminItem>, current: Int, last: Int) = ServicesAdminResponse(
        data = items,
        meta = ServicesAdminMeta(currentPage = current, lastPage = last, total = 2),
        categories = listOf("Cabello")
    )

    @Test
    fun `cargar mas agrega la siguiente pagina y termina en la ultima`() = runTest(dispatcher) {
        whenever(repo.servicesAdmin(eq(1), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(page(listOf(service("1")), 1, 2))
        whenever(repo.servicesAdmin(eq(2), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(page(listOf(service("2")), 2, 2))
        val vm = ServicesAdminViewModel(repo)

        vm.load()
        advanceUntilIdle()
        assertTrue(vm.state.value.hasMore)

        vm.loadMore()
        advanceUntilIdle()
        assertEquals(listOf("1", "2"), vm.state.value.items.map { it.id })
        assertFalse(vm.state.value.hasMore)
    }

    @Test
    fun `el filtro de inactivos pide activo cero al servidor`() = runTest(dispatcher) {
        whenever(repo.servicesAdmin(any(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(page(emptyList(), 1, 1))
        val vm = ServicesAdminViewModel(repo)

        vm.setStatus(ServiceStatusFilter.Inactivos)
        advanceUntilIdle()

        verify(repo).servicesAdmin(1, null, "0", null)
    }

    @Test
    fun `un 422 al guardar muestra el mensaje del servidor y no cierra el formulario`() = runTest(dispatcher) {
        val body = "{\"message\":\"El precio debe ser mayor o igual a 0.\"}".toResponseBody("application/json".toMediaType())
        whenever(repo.createService(any())).thenThrow(HttpException(Response.error<Any>(422, body)))
        val vm = ServicesAdminViewModel(repo)
        var closed = false

        vm.save(null, ServiceUpsertRequest("Corte", "Cabello", -1.0, 30, null, true)) { closed = true }
        advanceUntilIdle()

        assertEquals("El precio debe ser mayor o igual a 0.", vm.state.value.error)
        assertFalse(closed)
        assertFalse(vm.state.value.saving)
    }

    @Test
    fun `al guardar bien avisa y recarga la lista`() = runTest(dispatcher) {
        whenever(repo.servicesAdmin(any(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(page(listOf(service("1")), 1, 1))
        val vm = ServicesAdminViewModel(repo)
        var closed = false

        vm.save(null, ServiceUpsertRequest("Corte", "Cabello", 180.0, 30, null, true)) { closed = true }
        advanceUntilIdle()

        assertTrue(closed)
        assertEquals("Servicio creado.", vm.state.value.notice)
        assertNull(vm.state.value.error)
        assertEquals(1, vm.state.value.items.size)
    }
}
