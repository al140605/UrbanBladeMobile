package com.urbanblade.mobile.ui.viewmodel

import com.urbanblade.mobile.data.model.MembershipPlanItem
import com.urbanblade.mobile.data.model.MembershipPlanRequest
import com.urbanblade.mobile.data.model.PackageServiceRef
import com.urbanblade.mobile.data.model.ServicePackageItem
import com.urbanblade.mobile.data.model.ServicePackageRequest
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class OffersAdminViewModelTest {

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

    private val plan = MembershipPlanItem("p1", "Gold", "Todo incluido", 299.0, 15, true)
    private val pack = ServicePackageItem("k1", "5 cortes", PackageServiceRef("s1", "Corte"), 5, 800.0, 90, true)

    @Test
    fun `desactivar un plan conserva el precio para no crear otro Price en Stripe`() = runTest(dispatcher) {
        val vm = OffersAdminViewModel(repo)

        vm.togglePlan(plan)
        advanceUntilIdle()

        val body = argumentCaptor<MembershipPlanRequest>()
        verify(repo).updateMembershipPlan(eq("p1"), body.capture())
        assertEquals(299.0, body.firstValue.precioMensual, 0.0)
        assertEquals(15, body.firstValue.descuentoPct)
        assertFalse(body.firstValue.activo)
    }

    @Test
    fun `desactivar un paquete manda el servicio y la vigencia tal cual`() = runTest(dispatcher) {
        val vm = OffersAdminViewModel(repo)

        vm.togglePackage(pack)
        advanceUntilIdle()

        val body = argumentCaptor<ServicePackageRequest>()
        verify(repo).updateServicePackage(eq("k1"), body.capture())
        assertEquals("s1", body.firstValue.serviceId)
        assertEquals(90, body.firstValue.vigenciaDias)
        assertFalse(body.firstValue.activo)
    }

    @Test
    fun `si Stripe rechaza el plan se muestra el mensaje del servidor y no se cierra la hoja`() = runTest(dispatcher) {
        val response = "{\"message\":\"No se pudo crear el plan en Stripe. Intenta de nuevo.\"}".toResponseBody("application/json".toMediaType())
        whenever(repo.createMembershipPlan(any())).thenThrow(HttpException(Response.error<Any>(422, response)))
        val vm = OffersAdminViewModel(repo)
        var closed = false

        vm.savePlan(null, MembershipPlanRequest("Gold", null, 299.0, 15, true)) { closed = true }
        advanceUntilIdle()

        assertEquals("No se pudo crear el plan en Stripe. Intenta de nuevo.", vm.state.value.error)
        assertFalse(closed)
        assertFalse(vm.state.value.saving)
    }

    @Test
    fun `un paquete guardado bien cierra la hoja y avisa`() = runTest(dispatcher) {
        val vm = OffersAdminViewModel(repo)
        var closed = false

        vm.savePackage(null, ServicePackageRequest("5 cortes", "s1", 5, 800.0, null, true)) { closed = true }
        advanceUntilIdle()

        assertTrue(closed)
        assertEquals("Paquete creado.", vm.state.value.notice)
    }
}
