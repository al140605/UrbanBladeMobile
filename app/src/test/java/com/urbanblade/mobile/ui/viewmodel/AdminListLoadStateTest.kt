package com.urbanblade.mobile.ui.viewmodel

import com.urbanblade.mobile.data.model.BarberAdminItem
import com.urbanblade.mobile.data.model.BarberAdminUser
import com.urbanblade.mobile.data.model.BarbersAdminResponse
import com.urbanblade.mobile.data.model.ServiceAdminItem
import com.urbanblade.mobile.data.model.ServicesAdminMeta
import com.urbanblade.mobile.data.model.ServicesAdminResponse
import com.urbanblade.mobile.data.model.SystemUserRow
import com.urbanblade.mobile.data.model.SystemUsersResponse
import com.urbanblade.mobile.data.model.UsersMeta
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
 * Servicios, barberos y usuarios deben distinguir «aún no sé» de «confirmado vacío», no confundir un
 * fallo al guardar o eliminar con un fallo de consulta, y descartar la respuesta tardía de un filtro
 * anterior. `listError` es el de la consulta; `error` queda para las acciones.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AdminListLoadStateTest {

    private val dispatcher = StandardTestDispatcher()
    private val repo: UrbanRepository = mock()

    @Before fun setup() { Dispatchers.setMain(dispatcher) }

    @After fun tearDown() { Dispatchers.resetMain() }

    private fun servicio(id: String) = ServiceAdminItem(
        id = id, slug = "corte-$id", nombre = "Corte $id", categoria = "Cabello", precio = 180.0, duracionMin = 30, activo = true
    )

    private fun servicios(id: String) = ServicesAdminResponse(
        data = listOf(servicio(id)),
        meta = ServicesAdminMeta(currentPage = 1, lastPage = 1, total = 1),
        categories = listOf("Cabello")
    )

    private fun barbero(id: String) = BarberAdminItem(
        id = id, slug = "barbero-$id", especialidades = "Fade", descripcion = "Oficio", foto = null, activo = true,
        comisionPct = 40.0, user = BarberAdminUser("u$id", "Barbero $id", "$id@test.local")
    )

    private fun barberos(id: String) =
        BarbersAdminResponse(listOf(barbero(id)), ServicesAdminMeta(currentPage = 1, lastPage = 1, total = 1))

    private fun usuarios(id: String) = SystemUsersResponse(
        data = listOf(SystemUserRow(id = id, name = "Usuario $id", email = "$id@test.local")),
        meta = UsersMeta(1, 1, 1),
        roles = listOf("administrador", "cliente")
    )

    // ───────────── Servicios ─────────────

    @Test
    fun `servicios primera consulta fallida sigue sin confirmar hasta reintentar`() = runTest(dispatcher) {
        whenever(repo.servicesAdmin(any(), anyOrNull(), anyOrNull(), anyOrNull())).thenThrow(IllegalStateException("sin red"))
        val vm = ServicesAdminViewModel(repo)

        vm.load(); advanceUntilIdle()
        assertFalse(vm.state.value.loaded)
        assertNotNull(vm.state.value.listError)
        assertNull(vm.state.value.error)

        doReturn(servicios("1")).whenever(repo).servicesAdmin(any(), anyOrNull(), anyOrNull(), anyOrNull())
        vm.load(); advanceUntilIdle()
        assertTrue(vm.state.value.loaded)
        assertNull(vm.state.value.listError)
        assertEquals(listOf("1"), vm.state.value.items.map { it.id })
    }

    @Test
    fun `servicios un fallo al eliminar no se presenta como fallo de consulta`() = runTest(dispatcher) {
        whenever(repo.servicesAdmin(any(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(servicios("1"))
        whenever(repo.deleteService(any())).thenThrow(IllegalStateException("rechazado"))
        val vm = ServicesAdminViewModel(repo)
        vm.load(); advanceUntilIdle()

        vm.delete(servicio("1")); advanceUntilIdle()

        assertNotNull(vm.state.value.error)
        assertNull(vm.state.value.listError)
        assertTrue(vm.state.value.loaded)
        assertEquals(listOf("1"), vm.state.value.items.map { it.id })
    }

    @Test
    fun `servicios una respuesta tardia de un filtro anterior no pisa la ultima`() = runTest(dispatcher) {
        whenever(repo.servicesAdmin(eq(1), anyOrNull(), eq("1"), anyOrNull())).doSuspendableAnswer {
            withContext(NonCancellable) { delay(100) }
            servicios("viejo")
        }
        whenever(repo.servicesAdmin(eq(1), anyOrNull(), eq("0"), anyOrNull())).thenReturn(servicios("nuevo"))
        val vm = ServicesAdminViewModel(repo)

        vm.setStatus(ServiceStatusFilter.Activos); runCurrent()
        vm.setStatus(ServiceStatusFilter.Inactivos); advanceUntilIdle()

        assertEquals(listOf("nuevo"), vm.state.value.items.map { it.id })
        assertFalse(vm.state.value.loading)
        assertNull(vm.state.value.listError)
    }

    // ───────────── Barberos ─────────────

    @Test
    fun `barberos primera consulta fallida sigue sin confirmar hasta reintentar`() = runTest(dispatcher) {
        whenever(repo.barbersAdmin(any(), anyOrNull(), anyOrNull())).thenThrow(IllegalStateException("sin red"))
        val vm = BarbersAdminViewModel(repo)

        vm.load(); advanceUntilIdle()
        assertFalse(vm.state.value.loaded)
        assertNotNull(vm.state.value.listError)
        assertNull(vm.state.value.error)

        doReturn(barberos("1")).whenever(repo).barbersAdmin(any(), anyOrNull(), anyOrNull())
        vm.load(); advanceUntilIdle()
        assertTrue(vm.state.value.loaded)
        assertNull(vm.state.value.listError)
        assertEquals(listOf("1"), vm.state.value.items.map { it.id })
    }

    @Test
    fun `barberos un fallo al activar no se presenta como fallo de consulta`() = runTest(dispatcher) {
        whenever(repo.barbersAdmin(any(), anyOrNull(), anyOrNull())).thenReturn(barberos("1"))
        whenever(repo.updateBarber(any(), any())).thenThrow(IllegalStateException("rechazado"))
        val vm = BarbersAdminViewModel(repo)
        vm.load(); advanceUntilIdle()

        vm.toggleActive(barbero("1")); advanceUntilIdle()

        assertNotNull(vm.state.value.error)
        assertNull(vm.state.value.listError)
        assertTrue(vm.state.value.loaded)
        assertEquals(listOf("1"), vm.state.value.items.map { it.id })
    }

    @Test
    fun `barberos una respuesta tardia de un filtro anterior no pisa la ultima`() = runTest(dispatcher) {
        whenever(repo.barbersAdmin(eq(1), anyOrNull(), eq("1"))).doSuspendableAnswer {
            withContext(NonCancellable) { delay(100) }
            barberos("viejo")
        }
        whenever(repo.barbersAdmin(eq(1), anyOrNull(), eq("0"))).thenReturn(barberos("nuevo"))
        val vm = BarbersAdminViewModel(repo)

        vm.setStatus(ServiceStatusFilter.Activos); runCurrent()
        vm.setStatus(ServiceStatusFilter.Inactivos); advanceUntilIdle()

        assertEquals(listOf("nuevo"), vm.state.value.items.map { it.id })
        assertFalse(vm.state.value.loading)
        assertNull(vm.state.value.listError)
    }

    // ───────────── Usuarios ─────────────

    @Test
    fun `usuarios primera consulta fallida sigue sin confirmar hasta reintentar`() = runTest(dispatcher) {
        whenever(repo.systemUsers(anyOrNull(), anyOrNull(), any())).thenThrow(IllegalStateException("sin red"))
        val vm = UsersAdminViewModel(repo)

        vm.load(); advanceUntilIdle()
        assertFalse(vm.state.value.loaded)
        assertNotNull(vm.state.value.listError)
        assertNull(vm.state.value.error)

        doReturn(usuarios("1")).whenever(repo).systemUsers(anyOrNull(), anyOrNull(), any())
        vm.load(); advanceUntilIdle()
        assertTrue(vm.state.value.loaded)
        assertNull(vm.state.value.listError)
        assertEquals(listOf("1"), vm.state.value.items.map { it.id })
    }

    @Test
    fun `usuarios un fallo al eliminar no se presenta como fallo de consulta`() = runTest(dispatcher) {
        whenever(repo.systemUsers(anyOrNull(), anyOrNull(), any())).thenReturn(usuarios("1"))
        whenever(repo.deleteUser(any())).thenThrow(IllegalStateException("rechazado"))
        val vm = UsersAdminViewModel(repo)
        vm.load(); advanceUntilIdle()

        vm.delete("1"); advanceUntilIdle()

        assertNotNull(vm.state.value.error)
        assertNull(vm.state.value.listError)
        assertTrue(vm.state.value.loaded)
        assertEquals(listOf("1"), vm.state.value.items.map { it.id })
    }

    @Test
    fun `usuarios una respuesta tardia de un filtro de rol anterior no pisa la ultima`() = runTest(dispatcher) {
        whenever(repo.systemUsers(anyOrNull(), eq("administrador"), eq(1))).doSuspendableAnswer {
            withContext(NonCancellable) { delay(100) }
            usuarios("viejo")
        }
        whenever(repo.systemUsers(anyOrNull(), eq("cliente"), eq(1))).thenReturn(usuarios("nuevo"))
        val vm = UsersAdminViewModel(repo)

        vm.setRoleFilter("administrador"); runCurrent()
        vm.setRoleFilter("cliente"); advanceUntilIdle()

        assertEquals(listOf("nuevo"), vm.state.value.items.map { it.id })
        assertFalse(vm.state.value.loading)
        assertNull(vm.state.value.listError)
    }
}
