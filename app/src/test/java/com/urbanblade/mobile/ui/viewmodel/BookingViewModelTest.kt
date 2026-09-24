package com.urbanblade.mobile.ui.viewmodel

import com.urbanblade.mobile.data.model.CreateAppointmentResponse
import com.urbanblade.mobile.data.model.CreatedAppointmentData
import com.urbanblade.mobile.data.model.AppointmentRequest
import com.urbanblade.mobile.data.model.DepositIntentRequest
import com.urbanblade.mobile.data.model.MessageResponse
import com.urbanblade.mobile.data.model.StripeIntentRequest
import com.urbanblade.mobile.data.model.StripeIntentResponseData
import com.urbanblade.mobile.data.model.TransferInfo
import com.urbanblade.mobile.data.model.WaitlistJoinResponse
import com.urbanblade.mobile.data.repository.UrbanRepository
import kotlinx.coroutines.Dispatchers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class BookingViewModelTest {

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

    @Test
    fun `create con campos vacios no llama al repositorio`() = runTest(dispatcher) {
        val vm = BookingViewModel(repo)
        var doneCalled = false
        vm.create("", "svc1", "2026-10-01", "10:00", "", { doneCalled = true })
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Selecciona barbero, servicio, fecha y horario.", vm.error.value)
        assertFalse(doneCalled)
        verify(repo, never()).createAppointment(any())
    }

    @Test
    fun `create exitoso llama onDone y guarda el mensaje`() = runTest(dispatcher) {
        whenever(repo.createAppointment(any())).thenReturn(CreateAppointmentResponse("Cita reservada correctamente.", CreatedAppointmentData("abc123")))

        val vm = BookingViewModel(repo)
        var doneCalled = false
        vm.create("barber1", "svc1", "2026-10-01", "10:00", "", { doneCalled = true })
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(doneCalled)
        assertEquals("Cita reservada correctamente.", vm.message.value)
    }

    private fun created() = CreateAppointmentResponse("ok", CreatedAppointmentData("abc123", "UB-1"))

    @Test
    fun `reserve en efectivo no cobra nada y avisa que se paga en el salon`() = runTest(dispatcher) {
        whenever(repo.createAppointment(any())).thenReturn(created())

        val vm = BookingViewModel(repo)
        var doneId: String? = null
        vm.reserve(null, "b1", "s1", "2026-10-01", "10:00", "", BookingPayMethod.EFECTIVO, 0.0, null) { doneId = it }
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("abc123", doneId)
        assertEquals("Pagas en el salón el día de tu cita.", vm.paymentNote.value)
        verify(repo, never()).depositStripeIntent(any(), any())
    }

    @Test
    fun `reserve con tarjeta pide el intent con la propina y espera la confirmacion`() = runTest(dispatcher) {
        whenever(repo.createAppointment(any())).thenReturn(created())
        whenever(repo.depositStripeIntent(any(), any())).thenReturn(StripeIntentResponseData("secret_1", "pi_1"))

        val vm = BookingViewModel(repo)
        var doneId: String? = null
        vm.reserve(null, "b1", "s1", "2026-10-01", "10:00", "", BookingPayMethod.TARJETA, 50.0, null) { doneId = it }
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(CardConfirm("secret_1", null), vm.cardConfirm.value)
        assertEquals(null, doneId) // todavia no: falta que Stripe confirme la tarjeta
        // La cita se crea con "pagar ahora" y la propina; barber fija el monto y se cobra por el depósito.
        verify(repo).createAppointment(AppointmentRequest("b1", "s1", "2026-10-01", "10:00", null, pagarAhora = true, propinaSugerida = 50.0))
        verify(repo).depositStripeIntent("UB-1", DepositIntentRequest())

        vm.onCardResult(completed = true, canceled = false, reason = null)
        assertEquals("abc123", doneId)
        assertEquals(null, vm.cardConfirm.value)
        assertEquals("Pago con tarjeta enviado. UrbanBlade lo está confirmando.", vm.paymentNote.value)
    }

    @Test
    fun `reserve con tarjeta cancelada deja la cita reservada y lo avisa`() = runTest(dispatcher) {
        whenever(repo.createAppointment(any())).thenReturn(created())
        whenever(repo.depositStripeIntent(any(), any())).thenReturn(StripeIntentResponseData("secret_1", "pi_1"))

        val vm = BookingViewModel(repo)
        var doneId: String? = null
        vm.reserve(null, "b1", "s1", "2026-10-01", "10:00", "", BookingPayMethod.TARJETA, 0.0, null) { doneId = it }
        dispatcher.scheduler.advanceUntilIdle()
        vm.onCardResult(completed = false, canceled = true, reason = null)

        assertEquals("abc123", doneId)
        assertEquals("Cancelaste el pago. Tu cita quedó reservada y puedes pagarla desde Mis citas.", vm.paymentNote.value)
    }

    @Test
    fun `reserve con transferencia sin comprobante reserva y pide subirlo despues`() = runTest(dispatcher) {
        whenever(repo.createAppointment(any())).thenReturn(created())

        val vm = BookingViewModel(repo)
        var doneId: String? = null
        vm.reserve(null, "b1", "s1", "2026-10-01", "10:00", "", BookingPayMethod.TRANSFERENCIA, 0.0, null) { doneId = it }
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("abc123", doneId)
        assertEquals("Transfiere a la CLABE que te mostramos y sube tu comprobante desde Mis citas cuando el barbero confirme tu cita.", vm.paymentNote.value)
        // Sin comprobante no es "pagar ahora": la cita queda normal y se paga después.
        verify(repo).createAppointment(AppointmentRequest("b1", "s1", "2026-10-01", "10:00", null))
    }

    @Test
    fun `reserve con tarjeta guardada la manda al intent y la usa al confirmar`() = runTest(dispatcher) {
        whenever(repo.createAppointment(any())).thenReturn(created())
        whenever(repo.depositStripeIntent(any(), any())).thenReturn(StripeIntentResponseData("secret_2", "pi_2"))

        val vm = BookingViewModel(repo)
        vm.reserve(null, "b1", "s1", "2026-10-01", "10:00", "", BookingPayMethod.TARJETA, 0.0, null, savedCardId = "pm_42", saveCard = true) { }
        dispatcher.scheduler.advanceUntilIdle()

        verify(repo).depositStripeIntent("UB-1", DepositIntentRequest(tarjetaGuardada = true))
        assertEquals(CardConfirm("secret_2", "pm_42"), vm.cardConfirm.value)
    }

    @Test
    fun `reserve con tarjeta nueva pide guardarla solo si el cliente lo marco`() = runTest(dispatcher) {
        whenever(repo.createAppointment(any())).thenReturn(created())
        whenever(repo.depositStripeIntent(any(), any())).thenReturn(StripeIntentResponseData("secret_3", "pi_3"))

        val vm = BookingViewModel(repo)
        vm.reserve(null, "b1", "s1", "2026-10-01", "10:00", "", BookingPayMethod.TARJETA, 0.0, null, saveCard = true) { }
        dispatcher.scheduler.advanceUntilIdle()

        verify(repo).depositStripeIntent("UB-1", DepositIntentRequest(guardarTarjeta = true))
    }

    @Test
    fun `loadPaymentOptions deja la reserva usable aunque fallen los datos de pago`() = runTest(dispatcher) {
        whenever(repo.transferInfo()).thenThrow(RuntimeException("sin red"))
        whenever(repo.savedCards()).thenThrow(RuntimeException("sin red"))

        val vm = BookingViewModel(repo)
        vm.loadPaymentOptions()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(TransferInfo(), vm.transferInfo.value)
        assertTrue(vm.savedCards.value.isEmpty())
    }

    @Test
    fun `reserve muestra el motivo real cuando el servidor rechaza la cita`() = runTest(dispatcher) {
        val body = """{"message":"Ya tienes una cita agendada para el viernes 25 de septiembre. Solo se permite una cita por día."}"""
            .toResponseBody("application/json".toMediaType())
        whenever(repo.createAppointment(any())).thenAnswer { throw HttpException(Response.error<Any>(422, body)) }

        val vm = BookingViewModel(repo)
        var called = false
        vm.reserve(null, "b1", "s1", "2026-09-25", "16:00", "", BookingPayMethod.TARJETA, 0.0, null) { called = true }
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Ya tienes una cita agendada para el viernes 25 de septiembre. Solo se permite una cita por día.", vm.error.value)
        assertFalse(called)
        verify(repo, never()).depositStripeIntent(any(), any())
    }

    @Test
    fun `create entrega el id de la cita para poder pagar ahora`() = runTest(dispatcher) {
        whenever(repo.createAppointment(any())).thenReturn(CreateAppointmentResponse("ok", CreatedAppointmentData("abc123")))

        val vm = BookingViewModel(repo)
        var receivedId: String? = null
        vm.create("barber1", "svc1", "2026-10-01", "10:00", "", { receivedId = it })
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("abc123", receivedId)
    }

    @Test
    fun `joinWaitlist con campos vacios no llama al repositorio`() = runTest(dispatcher) {
        val vm = BookingViewModel(repo)
        vm.joinWaitlist("", "svc1", "2026-10-01")
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.waitlistJoined.value)
        verify(repo, never()).joinWaitlist(any(), any(), any())
    }

    @Test
    fun `joinWaitlist exitoso marca waitlistJoined`() = runTest(dispatcher) {
        whenever(repo.joinWaitlist(any(), any(), any())).thenReturn(
            WaitlistJoinResponse(message = "Te anotamos en la lista de espera.")
        )

        val vm = BookingViewModel(repo)
        vm.joinWaitlist("barber1", "svc1", "2026-10-01")
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.waitlistJoined.value)
        assertEquals("Te anotamos en la lista de espera.", vm.message.value)
    }
}
