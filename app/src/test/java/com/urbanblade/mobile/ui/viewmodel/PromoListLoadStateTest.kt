package com.urbanblade.mobile.ui.viewmodel

import com.urbanblade.mobile.data.model.CampaignRow
import com.urbanblade.mobile.data.model.CampaignsResponse
import com.urbanblade.mobile.data.model.CreateCampaignRequest
import com.urbanblade.mobile.data.model.MembershipPlanItem
import com.urbanblade.mobile.data.model.PackageServiceRef
import com.urbanblade.mobile.data.model.ServicePackageItem
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
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Campañas y membresías deben distinguir «aún no sé» de «confirmado vacío», no confundir un fallo al
 * enviar o al desactivar con un fallo de consulta, y descartar la respuesta tardía de una consulta
 * anterior. `listError` es el de la consulta; `error` queda para las acciones.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PromoListLoadStateTest {

    private val dispatcher = StandardTestDispatcher()
    private val repo: UrbanRepository = mock()
    private val request = CreateCampaignRequest("Promo", "20%", null, null, "todos", "ahora", null)

    @Before fun setup() { Dispatchers.setMain(dispatcher) }

    @After fun tearDown() { Dispatchers.resetMain() }

    private fun campanas(id: String) =
        CampaignsResponse(mapOf("vip" to "VIP"), mapOf("todos" to 12), listOf(CampaignRow(id = id, titulo = "Promo $id")))

    private fun plan(id: String) = MembershipPlanItem(id, "Plan $id", "Todo incluido", 299.0, 15, true)

    private val paquete = ServicePackageItem("k1", "5 cortes", PackageServiceRef("s1", "Corte"), 5, 800.0, 90, true)

    // ───────────── Campañas ─────────────

    @Test
    fun `campañas primera consulta fallida sigue sin confirmar hasta reintentar`() = runTest(dispatcher) {
        whenever(repo.campaigns()).thenThrow(IllegalStateException("sin red"))
        val vm = CampaignsViewModel(repo)

        vm.load(); advanceUntilIdle()
        assertFalse(vm.state.value.loaded)
        assertNotNull(vm.state.value.listError)
        assertNull(vm.state.value.error)
        assertFalse(vm.state.value.loading)

        doReturn(campanas("1")).whenever(repo).campaigns()
        vm.load(); advanceUntilIdle()
        assertTrue(vm.state.value.loaded)
        assertNull(vm.state.value.listError)
        assertEquals(listOf("1"), vm.state.value.data.data.map { it.id })
    }

    @Test
    fun `campañas un fallo al enviar no se presenta como fallo de consulta`() = runTest(dispatcher) {
        whenever(repo.campaigns()).thenReturn(campanas("1"))
        whenever(repo.createCampaign(any())).thenThrow(IllegalStateException("rechazado"))
        val vm = CampaignsViewModel(repo)
        vm.load(); advanceUntilIdle()

        vm.create(request) {}; advanceUntilIdle()

        assertNotNull(vm.state.value.error)
        assertNull(vm.state.value.listError)
        assertTrue(vm.state.value.loaded)
        assertEquals(listOf("1"), vm.state.value.data.data.map { it.id })
    }

    @Test
    fun `campañas una respuesta tardia no pisa la ultima consulta`() = runTest(dispatcher) {
        var llamadas = 0
        whenever(repo.campaigns()).doSuspendableAnswer {
            llamadas += 1
            if (llamadas == 1) {
                withContext(NonCancellable) { delay(100) }
                campanas("viejo")
            } else {
                campanas("nuevo")
            }
        }
        val vm = CampaignsViewModel(repo)

        vm.load(); runCurrent()
        vm.load(); advanceUntilIdle()

        assertEquals(listOf("nuevo"), vm.state.value.data.data.map { it.id })
        assertFalse(vm.state.value.loading)
        assertNull(vm.state.value.listError)
    }

    // ───────────── Membresías y paquetes ─────────────

    @Test
    fun `membresias primera consulta fallida sigue sin confirmar hasta reintentar`() = runTest(dispatcher) {
        whenever(repo.adminMembershipPlans()).thenThrow(IllegalStateException("sin red"))
        whenever(repo.servicePackages()).thenReturn(emptyList())
        whenever(repo.services()).thenReturn(emptyList())
        val vm = OffersAdminViewModel(repo)

        vm.load(); advanceUntilIdle()
        assertFalse(vm.state.value.loaded)
        assertNotNull(vm.state.value.listError)
        assertNull(vm.state.value.error)

        doReturn(listOf(plan("p1"))).whenever(repo).adminMembershipPlans()
        vm.load(); advanceUntilIdle()
        assertTrue(vm.state.value.loaded)
        assertNull(vm.state.value.listError)
        assertEquals(listOf("p1"), vm.state.value.plans.map { it.id })
    }

    @Test
    fun `membresias un fallo al desactivar no se presenta como fallo de consulta`() = runTest(dispatcher) {
        whenever(repo.adminMembershipPlans()).thenReturn(listOf(plan("p1")))
        whenever(repo.servicePackages()).thenReturn(listOf(paquete))
        whenever(repo.services()).thenReturn(emptyList())
        whenever(repo.updateMembershipPlan(any(), any())).thenThrow(IllegalStateException("rechazado"))
        val vm = OffersAdminViewModel(repo)
        vm.load(); advanceUntilIdle()

        vm.togglePlan(plan("p1")); advanceUntilIdle()

        assertNotNull(vm.state.value.error)
        assertNull(vm.state.value.listError)
        assertTrue(vm.state.value.loaded)
        assertEquals(listOf("p1"), vm.state.value.plans.map { it.id })
    }

    @Test
    fun `membresias una respuesta tardia no pisa la ultima consulta`() = runTest(dispatcher) {
        var llamadas = 0
        whenever(repo.adminMembershipPlans()).doSuspendableAnswer {
            llamadas += 1
            if (llamadas == 1) {
                withContext(NonCancellable) { delay(100) }
                listOf(plan("viejo"))
            } else {
                listOf(plan("nuevo"))
            }
        }
        whenever(repo.servicePackages()).thenReturn(emptyList())
        whenever(repo.services()).thenReturn(emptyList())
        val vm = OffersAdminViewModel(repo)

        vm.load(); runCurrent()
        vm.load(); advanceUntilIdle()

        assertEquals(listOf("nuevo"), vm.state.value.plans.map { it.id })
        assertFalse(vm.state.value.loading)
        assertNull(vm.state.value.listError)
    }
}
