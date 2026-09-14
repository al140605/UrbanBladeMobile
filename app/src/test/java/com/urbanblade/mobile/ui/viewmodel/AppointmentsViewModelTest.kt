package com.urbanblade.mobile.ui.viewmodel

import com.urbanblade.mobile.data.model.AppointmentRow
import com.urbanblade.mobile.data.repository.UrbanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import retrofit2.HttpException
import retrofit2.Response

/**
 * Reflejo del ajuste de P0 #4 a la ronda de "gestión de citas": el 422 de
 * cancelar/reagendar debe mostrar el mensaje real que devuelve Laravel (la
 * política de cancelación o "ya no se puede gestionar"), no el genérico de
 * toFriendlyMessage().
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppointmentsViewModelTest {

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

    private fun httpException(code: Int, message: String? = null): HttpException {
        val body = if (message != null) "{\"message\":\"$message\"}" else ""
        return HttpException(Response.error<Any>(code, body.toResponseBody("application/json".toMediaType())))
    }

    private fun appointment(code: String = "abc123", estado: String = "pendiente") =
        AppointmentRow(id = "1", code = code, fecha = "2026-10-01", horaInicio = "10:00:00", estado = estado)

    @Test
    fun `cancelar con 422 muestra el mensaje real del servidor`() = runTest(dispatcher) {
        whenever(repo.cancelAppointment(any())).thenThrow(
            httpException(422, "Solo puedes cancelar con al menos 24 horas de anticipación.")
        )

        val vm = AppointmentsViewModel(repo)
        vm.cancel(appointment())
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("Solo puedes cancelar con al menos 24 horas de anticipación.", vm.error.value)
    }

    @Test
    fun `cancelar con 422 sin cuerpo usable cae al mensaje por defecto`() = runTest(dispatcher) {
        whenever(repo.cancelAppointment(any())).thenThrow(httpException(422, null))

        val vm = AppointmentsViewModel(repo)
        vm.cancel(appointment())
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("Esta cita ya no se puede cancelar.", vm.error.value)
    }

    @Test
    fun `reagendar con campos vacios no llama al repositorio`() = runTest(dispatcher) {
        val vm = AppointmentsViewModel(repo)
        var doneCalled = false
        vm.reschedule("abc123", "", "svc1", "2026-10-01", "10:00", null) { doneCalled = true }
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Selecciona barbero, servicio, fecha y horario.", vm.error.value)
        assertEquals(false, doneCalled)
        verify(repo, never()).updateAppointment(any(), any())
    }

    @Test
    fun `reagendar con 422 muestra el mensaje real del servidor`() = runTest(dispatcher) {
        whenever(repo.updateAppointment(any(), any())).thenThrow(
            httpException(422, "Esta cita ya no se puede reagendar.")
        )

        val vm = AppointmentsViewModel(repo)
        vm.reschedule("abc123", "barber1", "svc1", "2026-10-01", "10:00", null) {}
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("Esta cita ya no se puede reagendar.", vm.error.value)
    }

    @Test
    fun `reagendar exitoso llama onDone`() = runTest(dispatcher) {
        val vm = AppointmentsViewModel(repo)
        var doneCalled = false
        vm.reschedule("abc123", "barber1", "svc1", "2026-10-01", "10:00", null) { doneCalled = true }
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(true, doneCalled)
        assertNull(vm.error.value)
    }
}
