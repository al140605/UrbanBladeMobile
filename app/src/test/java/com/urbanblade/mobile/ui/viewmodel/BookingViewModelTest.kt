package com.urbanblade.mobile.ui.viewmodel

import com.urbanblade.mobile.data.model.MessageResponse
import com.urbanblade.mobile.data.model.WaitlistJoinResponse
import com.urbanblade.mobile.data.repository.UrbanRepository
import kotlinx.coroutines.Dispatchers
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
        whenever(repo.createAppointment(any())).thenReturn(MessageResponse("Cita reservada correctamente."))

        val vm = BookingViewModel(repo)
        var doneCalled = false
        vm.create("barber1", "svc1", "2026-10-01", "10:00", "", { doneCalled = true })
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(doneCalled)
        assertEquals("Cita reservada correctamente.", vm.message.value)
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
