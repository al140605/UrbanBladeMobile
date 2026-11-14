package com.urbanblade.mobile.ui.viewmodel

import com.urbanblade.mobile.data.model.MessageResponse
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
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import retrofit2.HttpException
import retrofit2.Response

/** «¿Te invitó alguien?» en Beneficios: validación, éxito, motivos del servidor y código ya registrado. */
@OptIn(ExperimentalCoroutinesApi::class)
class ReferralLinkViewModelTest {
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

    private fun unprocessable(message: String) = HttpException(
        Response.error<Any>(422, """{"message":"$message"}""".toResponseBody("application/json".toMediaType()))
    )

    @Test
    fun `un codigo vacio no se envia`() = runTest(dispatcher) {
        val vm = WalletViewModel(repo)

        vm.linkReferral("   ")
        advanceUntilIdle()

        assertEquals("Escribe el código de quien te invitó.", vm.referralError.value)
        verify(repo, never()).linkReferral(any())
    }

    @Test
    fun `un codigo valido se registra y se deja de ofrecer el formulario`() = runTest(dispatcher) {
        whenever(repo.linkReferral("A1B2C3")).thenReturn(MessageResponse("¡Listo! Cuando completes tu primera cita, tu referido ganará sus puntos."))
        val vm = WalletViewModel(repo)

        vm.linkReferral("A1B2C3")
        advanceUntilIdle()

        assertEquals("¡Listo! Cuando completes tu primera cita, tu referido ganará sus puntos.", vm.referralNotice.value)
        assertTrue(vm.referralLinked.value)
        assertNull(vm.referralError.value)
        assertFalse(vm.referralBusy.value)
    }

    @Test
    fun `un codigo invalido muestra el motivo del servidor y deja reintentar`() = runTest(dispatcher) {
        whenever(repo.linkReferral("ZZZ")).thenThrow(unprocessable("El código de referido no es válido."))
        val vm = WalletViewModel(repo)

        vm.linkReferral("ZZZ")
        advanceUntilIdle()

        assertEquals("El código de referido no es válido.", vm.referralError.value)
        assertFalse(vm.referralLinked.value)
        assertFalse(vm.referralBusy.value)
    }

    @Test
    fun `si ya hay un codigo registrado se explica y se oculta el formulario`() = runTest(dispatcher) {
        whenever(repo.linkReferral("A1B2C3")).thenThrow(unprocessable("Ya tienes un código de referido registrado -- no se puede cambiar."))
        val vm = WalletViewModel(repo)

        vm.linkReferral("A1B2C3")
        advanceUntilIdle()

        assertEquals("Ya tienes un código de referido registrado -- no se puede cambiar.", vm.referralError.value)
        assertTrue(vm.referralLinked.value)
    }

    @Test
    fun `un segundo toque mientras se registra no repite la peticion`() = runTest(dispatcher) {
        whenever(repo.linkReferral("A1B2C3")).thenReturn(MessageResponse("ok"))
        val vm = WalletViewModel(repo)

        vm.linkReferral("A1B2C3")
        vm.linkReferral("A1B2C3")
        advanceUntilIdle()

        verify(repo, times(1)).linkReferral("A1B2C3")
    }

    @Test
    fun `la validacion respeta el tope de 20 caracteres del servidor`() {
        assertNull(validateReferralCode("A".repeat(20)))
        assertEquals("El código tiene como máximo 20 caracteres.", validateReferralCode("A".repeat(21)))
        assertEquals("Escribe el código de quien te invitó.", validateReferralCode(""))
    }
}
