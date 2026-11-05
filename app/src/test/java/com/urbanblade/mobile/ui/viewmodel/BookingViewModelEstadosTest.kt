package com.urbanblade.mobile.ui.viewmodel

import com.urbanblade.mobile.data.model.AppointmentRequest
import com.urbanblade.mobile.data.model.CreateAppointmentResponse
import com.urbanblade.mobile.data.model.CreatedAppointmentData
import com.urbanblade.mobile.data.model.SlotItem
import com.urbanblade.mobile.data.repository.UrbanRepository
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import retrofit2.HttpException
import retrofit2.Response

/**
 * T085 (HT-38): pruebas unitarias del ViewModel de reserva de la app ([BookingViewModel]).
 *
 * Cubren los estados de carga, la solicitud registrada y el error al reservar. La integración es simulada: el
 * repositorio es un mock y algunas respuestas «tardan» con delay() para poder revisar el estado mientras la petición
 * sigue en vuelo. Complementan a BookingViewModelTest (métodos de pago) y a SlotsAuditTest (horarios).
 * Cada prueba sigue el patrón Preparar -> Ejecutar -> Verificar.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BookingViewModelEstadosTest {

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

    private fun citaCreada() =
        CreateAppointmentResponse("Cita reservada correctamente.", CreatedAppointmentData("abc123", "UB-0042"))

    private fun errorHttp(codigo: Int, json: String = "{}") =
        HttpException(Response.error<Any>(codigo, json.toResponseBody("application/json".toMediaType())))

    /** Reserva en efectivo una cita válida, igual que el último paso de BookingScreen. */
    private fun BookingViewModel.reservarEnEfectivo(alTerminar: (String?) -> Unit) =
        reserve(
            null, "b1", "s1", "2026-11-20", "18:30", "  ",
            BookingPayMethod.EFECTIVO, 0.0, null,
            aceptaCargo = true,
            onDone = alTerminar
        )

    // ── Estados de carga ────────────────────────────────────────────────────────────────────────────────────────

    @Test
    fun `mientras se registra la cita busy es true y al terminar vuelve a false`() = runTest(dispatcher) {
        whenever(repo.createAppointment(any())).doSuspendableAnswer { delay(1_000); citaCreada() }
        val vm = BookingViewModel(repo)

        vm.reservarEnEfectivo { }
        runCurrent()
        assertTrue("debe marcar busy mientras espera al servidor", vm.busy.value)

        advanceUntilIdle()
        assertFalse(vm.busy.value)
    }

    @Test
    fun `si la reserva falla busy tambien vuelve a false`() = runTest(dispatcher) {
        whenever(repo.createAppointment(any())).doSuspendableAnswer { delay(1_000); throw IOException("sin red") }
        val vm = BookingViewModel(repo)

        vm.reservarEnEfectivo { }
        runCurrent()
        assertTrue(vm.busy.value)

        advanceUntilIdle()
        assertFalse("el botón no puede quedarse bloqueado después de un error", vm.busy.value)
    }

    @Test
    fun `mientras cargan los horarios slotsLoading es true y al llegar vuelve a false`() = runTest(dispatcher) {
        whenever(repo.slots("b1", "s1", "2026-11-20")).doSuspendableAnswer {
            delay(1_000)
            listOf(SlotItem("18:30", "18:30"))
        }
        val vm = BookingViewModel(repo)

        vm.loadSlots("b1", "s1", "2026-11-20")
        runCurrent()
        assertTrue(vm.slotsLoading.value)
        assertTrue("mientras carga no se muestran horarios viejos", vm.slots.value.isEmpty())

        advanceUntilIdle()
        assertFalse(vm.slotsLoading.value)
        assertEquals(listOf("18:30"), vm.slots.value.map { it.time })
        assertEquals("b1|s1|2026-11-20", vm.slotsFor.value)
    }

    @Test
    fun `mientras carga el catalogo catalogLoading es true y un segundo toque no lo pide otra vez`() = runTest(dispatcher) {
        whenever(repo.products()).thenReturn(emptyList())
        whenever(repo.services()).doSuspendableAnswer { delay(1_000); emptyList() }
        whenever(repo.barbers()).thenReturn(emptyList())
        val vm = BookingViewModel(repo)

        vm.loadCatalog()
        runCurrent()
        assertTrue(vm.catalogLoading.value)

        vm.loadCatalog()
        advanceUntilIdle()

        assertFalse(vm.catalogLoading.value)
        verify(repo, times(1)).services()
    }

    // ── Solicitud registrada ────────────────────────────────────────────────────────────────────────────────────

    @Test
    fun `la solicitud viaja con los datos elegidos y entrega el id de la cita`() = runTest(dispatcher) {
        whenever(repo.createAppointment(any())).thenReturn(citaCreada())
        val vm = BookingViewModel(repo)
        var idRecibido: String? = null

        vm.reservarEnEfectivo { idRecibido = it }
        advanceUntilIdle()

        val enviada = argumentCaptor<AppointmentRequest>().apply { verify(repo).createAppointment(capture()) }.firstValue
        assertEquals("b1", enviada.barberId)
        assertEquals("s1", enviada.serviceId)
        assertEquals("2026-11-20", enviada.fecha)
        assertEquals("18:30", enviada.horaInicio)
        assertNull("las notas en blanco no se mandan", enviada.notas)
        assertNull("en efectivo no se cobra al reservar", enviada.pagarAhora)
        assertEquals(true, enviada.aceptaCargoInasistencia)
        assertEquals("abc123", idRecibido)
        assertNull(vm.error.value)
    }

    @Test
    fun `al registrarse limpia el error de un intento anterior`() = runTest(dispatcher) {
        whenever(repo.createAppointment(any()))
            .thenAnswer { throw IOException("sin red") }
            .thenReturn(citaCreada())
        val vm = BookingViewModel(repo)

        vm.reservarEnEfectivo { }
        advanceUntilIdle()
        assertEquals("Sin conexión. Revisa tu red e inténtalo de nuevo.", vm.error.value)

        var idRecibido: String? = null
        vm.reservarEnEfectivo { idRecibido = it }
        advanceUntilIdle()

        assertNull("el error del intento anterior no debe quedarse en pantalla", vm.error.value)
        assertEquals("abc123", idRecibido)
    }

    // ── Error al reservar ───────────────────────────────────────────────────────────────────────────────────────

    @Test
    fun `reserve con datos incompletos no llama al servidor ni activa la carga`() = runTest(dispatcher) {
        val vm = BookingViewModel(repo)
        var llamado = false

        vm.reserve(null, "b1", "s1", "2026-11-20", "", "", BookingPayMethod.EFECTIVO, 0.0, null) { llamado = true }
        advanceUntilIdle()

        assertEquals("Selecciona barbero, servicio, fecha y horario.", vm.error.value)
        assertFalse(vm.busy.value)
        assertFalse(llamado)
        verify(repo, never()).createAppointment(any())
    }

    @Test
    fun `sin conexion muestra un mensaje claro y no da la cita por registrada`() = runTest(dispatcher) {
        whenever(repo.createAppointment(any())).thenAnswer { throw IOException("Unable to resolve host") }
        val vm = BookingViewModel(repo)
        var llamado = false

        vm.reservarEnEfectivo { llamado = true }
        advanceUntilIdle()

        assertEquals("Sin conexión. Revisa tu red e inténtalo de nuevo.", vm.error.value)
        assertFalse(llamado)
        assertNull("sin cita no hay instrucciones de pago", vm.paymentNote.value)
    }

    @Test
    fun `un error del servidor sin motivo usa el mensaje de respaldo`() = runTest(dispatcher) {
        whenever(repo.createAppointment(any())).thenAnswer { throw errorHttp(500) }
        val vm = BookingViewModel(repo)

        vm.reservarEnEfectivo { }
        advanceUntilIdle()

        assertEquals("No se pudo reservar la cita.", vm.error.value)
    }

    @Test
    fun `con la plataforma en mantenimiento lo dice en vez de un error generico`() = runTest(dispatcher) {
        whenever(repo.createAppointment(any())).thenAnswer {
            throw errorHttp(503, """{"message":"UrbanBlade está en mantenimiento. Vuelve pronto.","maintenance":true}""")
        }
        val vm = BookingViewModel(repo)

        vm.reservarEnEfectivo { }
        advanceUntilIdle()

        assertEquals("UrbanBlade está en mantenimiento.", vm.error.value)
        assertFalse(vm.busy.value)
    }
}
