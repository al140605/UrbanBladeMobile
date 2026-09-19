package com.urbanblade.mobile.ui.viewmodel

import com.urbanblade.mobile.data.model.BarberAdminItem
import com.urbanblade.mobile.data.model.BarberAdminUser
import com.urbanblade.mobile.data.model.BarberPerformance
import com.urbanblade.mobile.data.model.BarberUpsertRequest
import com.urbanblade.mobile.data.model.BarbersAdminResponse
import com.urbanblade.mobile.data.model.ServicesAdminMeta
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
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class BarbersAdminViewModelTest {

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

    private val barber = BarberAdminItem(
        id = "1", slug = "carlos", especialidades = "Fade", descripcion = "Diez años de oficio",
        foto = "barbers/carlos.jpg", activo = true, comisionPct = 40.0,
        user = BarberAdminUser("u1", "Carlos", "carlos@test.local")
    )

    private fun page(items: List<BarberAdminItem>) =
        BarbersAdminResponse(items, ServicesAdminMeta(currentPage = 1, lastPage = 1, total = items.size))

    @Test
    fun `activar o desactivar manda el perfil completo incluida la foto`() = runTest(dispatcher) {
        whenever(repo.barbersAdmin(any(), anyOrNull(), anyOrNull())).thenReturn(page(listOf(barber)))
        val vm = BarbersAdminViewModel(repo)

        vm.toggleActive(barber)
        advanceUntilIdle()

        val body = argumentCaptor<BarberUpsertRequest>()
        verify(repo).updateBarber(eq("carlos"), body.capture())
        assertEquals("barbers/carlos.jpg", body.firstValue.foto)
        assertEquals(40.0, body.firstValue.comisionPct, 0.0)
        assertEquals("Fade", body.firstValue.especialidades)
        assertFalse(body.firstValue.activo)
        assertEquals("Carlos ya no recibe citas.", vm.state.value.notice)
    }

    @Test
    fun `un 422 al guardar muestra el mensaje del servidor y no cierra la hoja`() = runTest(dispatcher) {
        val response = "{\"message\":\"El correo ya está en uso.\"}".toResponseBody("application/json".toMediaType())
        whenever(repo.updateBarber(any(), any())).thenThrow(HttpException(Response.error<Any>(422, response)))
        val vm = BarbersAdminViewModel(repo)
        var closed = false

        vm.save(barber, BarberUpsertRequest("Carlos", "otro@test.local", null, null, null, true, 0.0)) { closed = true }
        advanceUntilIdle()

        assertEquals("El correo ya está en uso.", vm.state.value.error)
        assertFalse(closed)
    }

    @Test
    fun `si falla el rendimiento la edicion sigue disponible sin error`() = runTest(dispatcher) {
        whenever(repo.barberPerformance("carlos")).thenThrow(RuntimeException("sin red"))
        val vm = BarbersAdminViewModel(repo)

        vm.loadPerformance(barber)
        advanceUntilIdle()

        assertNull(vm.state.value.performance)
        assertNull(vm.state.value.error)
        assertFalse(vm.state.value.performanceLoading)
    }

    @Test
    fun `el rendimiento cargado queda en el estado`() = runTest(dispatcher) {
        whenever(repo.barberPerformance("carlos")).thenReturn(BarberPerformance(12, 10, 20, 4.8, 33))
        val vm = BarbersAdminViewModel(repo)

        vm.loadPerformance(barber)
        advanceUntilIdle()

        assertEquals(12, vm.state.value.performance?.appointmentsThisMonth)
        assertEquals(33, vm.state.value.performance?.totalClients)
    }
}
