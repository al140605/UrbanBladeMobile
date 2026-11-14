package com.urbanblade.mobile.ui.viewmodel

import android.content.Context
import com.urbanblade.mobile.core.media.UploadRejectedException
import com.urbanblade.mobile.data.model.BarberMeResponse
import com.urbanblade.mobile.data.model.BarberProfileUpdateResponse
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/** «Mi perfil» del barbero: carga, validación de topes, guardado y mensajes de error. */
@OptIn(ExperimentalCoroutinesApi::class)
class BarberMeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: UrbanRepository
    private val context: Context = mock()

    private val me = BarberMeResponse(
        id = "u1", name = "Luis", email = "luis@example.com",
        especialidades = "Degradados", descripcion = "Diez años cortando.",
        fotoUrl = "https://example.com/foto.jpg", calificacionPromedio = 4.8, totalResenas = 12
    )

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
    fun `el perfil se carga y queda confirmado`() = runTest(dispatcher) {
        whenever(repo.barberMe()).thenReturn(me)
        val vm = BarberMeViewModel(repo)

        vm.load()
        advanceUntilIdle()

        assertEquals("Luis", vm.state.value.profile?.name)
        assertTrue(vm.state.value.loaded)
        assertNull(vm.state.value.loadError)
    }

    @Test
    fun `si cargar falla el error de la consulta va aparte`() = runTest(dispatcher) {
        whenever(repo.barberMe()).thenThrow(IllegalStateException("x"))
        val vm = BarberMeViewModel(repo)

        vm.load()
        advanceUntilIdle()

        assertFalse(vm.state.value.loaded)
        assertEquals("No se pudo cargar tu perfil.", vm.state.value.loadError)
        assertNull(vm.state.value.saveError)
    }

    @Test
    fun `guardar actualiza el perfil y avisa`() = runTest(dispatcher) {
        whenever(repo.barberMe()).thenReturn(me)
        whenever(repo.updateBarberProfile(any(), any(), any(), anyOrNull()))
            .thenReturn(BarberProfileUpdateResponse("Perfil de barbero actualizado.", "Fades", "Nueva bio", null))
        val vm = BarberMeViewModel(repo)
        vm.load()
        advanceUntilIdle()

        vm.save(context, "Fades", "Nueva bio", null)
        advanceUntilIdle()

        assertEquals("Fades", vm.state.value.profile?.especialidades)
        assertEquals("Nueva bio", vm.state.value.profile?.descripcion)
        assertEquals("https://example.com/foto.jpg", vm.state.value.profile?.fotoUrl)
        assertEquals("Perfil de barbero actualizado.", vm.state.value.notice)
        assertFalse(vm.state.value.saving)
    }

    @Test
    fun `una descripcion mas larga que el tope no se envia`() = runTest(dispatcher) {
        whenever(repo.barberMe()).thenReturn(me)
        val vm = BarberMeViewModel(repo)
        vm.load()
        advanceUntilIdle()

        vm.save(context, "Fades", "a".repeat(1001), null)
        advanceUntilIdle()

        assertEquals("La descripción puede tener hasta 1000 caracteres.", vm.state.value.saveError)
        verify(repo, never()).updateBarberProfile(any(), any(), any(), anyOrNull())
    }

    @Test
    fun `una foto rechazada por la politica muestra su motivo`() = runTest(dispatcher) {
        whenever(repo.barberMe()).thenReturn(me)
        whenever(repo.updateBarberProfile(any(), any(), any(), anyOrNull()))
            // UploadRejectedException es comprobada: Mockito solo admite lanzarla con thenAnswer en una función suspendida.
            .thenAnswer { throw UploadRejectedException("La foto pesa más de 4 MB. Elige una más ligera.") }
        val vm = BarberMeViewModel(repo)
        vm.load()
        advanceUntilIdle()

        vm.save(context, "Fades", "Bio", null)
        advanceUntilIdle()

        assertEquals("La foto pesa más de 4 MB. Elige una más ligera.", vm.state.value.saveError)
        assertEquals("Degradados", vm.state.value.profile?.especialidades)
    }

    @Test
    fun `un fallo inesperado al guardar da un mensaje y conserva el perfil`() = runTest(dispatcher) {
        whenever(repo.barberMe()).thenReturn(me)
        whenever(repo.updateBarberProfile(any(), any(), any(), anyOrNull())).thenThrow(IllegalStateException("x"))
        val vm = BarberMeViewModel(repo)
        vm.load()
        advanceUntilIdle()

        vm.save(context, "Fades", "Bio", null)
        advanceUntilIdle()

        assertEquals("No se pudo guardar tu perfil. Inténtalo de nuevo.", vm.state.value.saveError)
        assertFalse(vm.state.value.saving)
    }

    @Test
    fun `un segundo toque mientras guarda no repite el envio`() = runTest(dispatcher) {
        whenever(repo.barberMe()).thenReturn(me)
        whenever(repo.updateBarberProfile(any(), any(), any(), anyOrNull()))
            .thenReturn(BarberProfileUpdateResponse("ok", "Fades", "Bio", null))
        val vm = BarberMeViewModel(repo)
        vm.load()
        advanceUntilIdle()

        vm.save(context, "Fades", "Bio", null)
        vm.save(context, "Fades", "Bio", null)
        advanceUntilIdle()

        verify(repo, times(1)).updateBarberProfile(any(), any(), any(), anyOrNull())
    }

    @Test
    fun `los topes de especialidades y descripcion se validan juntos`() {
        assertNull(validateBarberProfile("a".repeat(1000), "b".repeat(1000)))
        assertEquals("Las especialidades pueden tener hasta 1000 caracteres.", validateBarberProfile("a".repeat(1001), ""))
        assertEquals("La descripción puede tener hasta 1000 caracteres.", validateBarberProfile("", "b".repeat(1001)))
    }
}
