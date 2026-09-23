package com.urbanblade.mobile.ui.viewmodel

import com.urbanblade.mobile.data.model.SystemUserMutationResponse
import com.urbanblade.mobile.data.model.SystemUserRow
import com.urbanblade.mobile.data.model.SystemUsersResponse
import com.urbanblade.mobile.data.model.UsersMeta
import com.urbanblade.mobile.data.repository.UrbanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
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
class UsersAdminViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: UrbanRepository

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repo = mock()
        runBlocking {
            whenever(repo.systemUsers(anyOrNull(), anyOrNull(), eq(1))).thenReturn(page("a", 1, 2))
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun page(id: String, current: Int, last: Int) = SystemUsersResponse(
        data = listOf(SystemUserRow(id = id, name = "Usuario $id", email = "$id@test.local")),
        meta = UsersMeta(current, last, 2),
        roles = listOf("administrador", "cliente")
    )

    private fun http(code: Int, message: String) =
        HttpException(Response.error<Any>(code, "{\"message\":\"$message\"}".toResponseBody("application/json".toMediaType())))

    @Test
    fun `la contraseña obligatoria exige minimo y confirmacion`() {
        assertEquals("Mínimo 8 caracteres.", passwordProblem("", "", required = true))
        assertEquals("Mínimo 8 caracteres.", passwordProblem("corta", "corta", required = true))
        assertEquals("Las contraseñas no coinciden.", passwordProblem("suficiente1", "suficiente2", required = true))
        assertNull(passwordProblem("suficiente1", "suficiente1", required = true))
    }

    @Test
    fun `al editar la contraseña vacia significa no cambiarla`() {
        assertNull(passwordProblem("", "", required = false))
        assertEquals("Las contraseñas no coinciden.", passwordProblem("suficiente1", "", required = false))
    }

    @Test
    fun `cargar mas agrega la siguiente pagina de usuarios`() = runTest(dispatcher) {
        whenever(repo.systemUsers(anyOrNull(), anyOrNull(), eq(2))).thenReturn(page("b", 2, 2))
        val vm = UsersAdminViewModel(repo)

        vm.load()
        advanceUntilIdle()
        assertTrue(vm.state.value.hasMore)
        assertEquals(listOf("administrador", "cliente"), vm.state.value.roles)

        vm.loadMore()
        advanceUntilIdle()
        assertEquals(listOf("a", "b"), vm.state.value.items.map { it.id })
        assertFalse(vm.state.value.hasMore)
    }

    @Test
    fun `el filtro de rol se manda al servidor`() = runTest(dispatcher) {
        val vm = UsersAdminViewModel(repo)

        vm.setRoleFilter("cliente")
        advanceUntilIdle()

        verify(repo).systemUsers(null, "cliente", 1)
    }

    @Test
    fun `un correo repetido muestra el mensaje del servidor y no cierra el formulario`() = runTest(dispatcher) {
        whenever(repo.createUser(any(), any(), any(), any())).thenThrow(http(422, "El correo ya está en uso."))
        val vm = UsersAdminViewModel(repo)
        var closed = false

        vm.create("Ana", "ana@test.local", "suficiente1", "cliente") { closed = true }
        advanceUntilIdle()

        assertEquals("El correo ya está en uso.", vm.state.value.error)
        assertFalse(closed)
        assertFalse(vm.state.value.saving)
    }

    @Test
    fun `borrar tu propia cuenta muestra el rechazo del servidor`() = runTest(dispatcher) {
        whenever(repo.deleteUser(any())).thenThrow(http(403, "No puedes eliminar tu propia cuenta."))
        val vm = UsersAdminViewModel(repo)

        vm.delete("yo")
        advanceUntilIdle()

        assertEquals("No puedes eliminar tu propia cuenta.", vm.state.value.error)
    }

    @Test
    fun `crear bien avisa, cierra y recarga`() = runTest(dispatcher) {
        whenever(repo.createUser(any(), any(), any(), any())).thenReturn(SystemUserMutationResponse("ok"))
        val vm = UsersAdminViewModel(repo)
        var closed = false

        vm.create("Ana", "ana@test.local", "suficiente1", "cliente") { closed = true }
        advanceUntilIdle()

        assertTrue(closed)
        assertEquals("Usuario creado.", vm.state.value.notice)
    }
}
