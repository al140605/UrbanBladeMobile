package com.urbanblade.mobile.core.config

import com.urbanblade.mobile.core.network.ApiGson
import com.urbanblade.mobile.core.network.UrbanBladeApi
import com.urbanblade.mobile.data.model.BarbershopPublicResponse
import com.urbanblade.mobile.data.model.ModuleFeatures
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** Interruptores de módulos: lectura de `GET barbershop` y estado global que consulta toda la app. */
class ModuleFeaturesTest {
    private fun parse(json: String) = ApiGson.instance.fromJson(json, BarbershopPublicResponse::class.java)

    @After fun tearDown() = ModuleFeaturesStore.reset()

    @Test fun `todos los interruptores traidos por el backend se leen`() {
        val features = parse(
            """{"data":{"nombre":"UrbanBlade","features":{"packages":true,"gift_cards":true,"referrals":true,"raffles":true,"social":true,"campaigns":true}}}"""
        ).data?.features
        assertEquals(ModuleFeatures(packages = true, giftCards = true, referrals = true, raffles = true, social = true, campaigns = true), features)
    }

    @Test fun `una clave ausente queda apagada`() {
        val features = parse("""{"data":{"features":{"social":true}}}""").data?.features
        assertEquals(ModuleFeatures(social = true), features)
        assertFalse(features!!.packages)
        assertFalse(features.giftCards)
        assertFalse(features.referrals)
        assertFalse(features.raffles)
        assertFalse(features.campaigns)
    }

    @Test fun `un valor nulo tambien queda apagado`() {
        val features = parse("""{"data":{"features":{"packages":null,"gift_cards":false,"social":true}}}""").data?.features
        assertEquals(ModuleFeatures(social = true), features)
    }

    @Test fun `sin features o sin data no hay interruptores y todo se trata como apagado`() {
        assertNull(parse("""{"data":{"nombre":"UrbanBlade"}}""").data?.features)
        assertNull(parse("""{}""").data)
        assertEquals(ModuleFeatures(), ModuleFeatures())
        assertFalse(ModuleFeatures().allowsRoute("social"))
    }

    @Test fun `una ruta apagada no se permite y el resto siempre`() {
        val off = ModuleFeatures()
        listOf("social", "moderation", "blocked_authors", "campaigns", "raffles", "gift_cards").forEach {
            assertFalse("$it debería estar bloqueada", off.allowsRoute(it))
        }
        // Membresía y puntos no tienen interruptor; tampoco el resto de la app.
        listOf("home", "wallet", "offers_admin", "reviews", "payments", "orders", "appointments", "catalog", null).forEach {
            assertTrue("$it debería estar permitida", off.allowsRoute(it))
        }
    }

    @Test fun `cada interruptor abre solo sus rutas`() {
        assertTrue(ModuleFeatures(social = true).allowsRoute("social"))
        assertTrue(ModuleFeatures(social = true).allowsRoute("moderation"))
        assertTrue(ModuleFeatures(social = true).allowsRoute("blocked_authors"))
        assertFalse(ModuleFeatures(social = true).allowsRoute("campaigns"))
        assertTrue(ModuleFeatures(campaigns = true).allowsRoute("campaigns"))
        assertFalse(ModuleFeatures(campaigns = true).allowsRoute("raffles"))
        assertTrue(ModuleFeatures(raffles = true).allowsRoute("raffles"))
        assertTrue(ModuleFeatures(giftCards = true).allowsRoute("gift_cards"))
        assertFalse(ModuleFeatures(giftCards = true, packages = true, referrals = true).allowsRoute("social"))
    }

    @Test fun `la ruta con parametros se evalua por su nombre`() {
        assertFalse(ModuleFeatures().allowsRoute("social?work=1"))
        assertTrue(ModuleFeatures(social = true).allowsRoute("social?work=1"))
    }

    @Test fun `la tienda arranca apagada y no enciende nada si la peticion falla`() = runTest {
        assertEquals(ModuleFeatures(), ModuleFeaturesStore.current)
        ModuleFeaturesStore.refresh { throw java.io.IOException("sin red") }
        assertEquals(ModuleFeatures(), ModuleFeaturesStore.current)
    }

    @Test fun `una respuesta valida enciende los modulos y un fallo posterior los apaga`() = runTest {
        ModuleFeaturesStore.refresh { ModuleFeatures(social = true, raffles = true) }
        assertTrue(ModuleFeaturesStore.features.value.social)
        assertTrue(ModuleFeaturesStore.current.raffles)
        assertFalse(ModuleFeaturesStore.current.packages)
        ModuleFeaturesStore.refresh { throw java.io.IOException("sin red") }
        assertEquals(ModuleFeatures(), ModuleFeaturesStore.current)
    }

    @Test fun `una respuesta sin features apaga todo`() = runTest {
        ModuleFeaturesStore.refresh { ModuleFeatures(social = true) }
        ModuleFeaturesStore.refresh { null }
        assertEquals(ModuleFeatures(), ModuleFeaturesStore.current)
    }

    @Test fun `la app pide la ficha publica de la barberia y lee data features`() = runTest {
        val server = MockWebServer()
        server.start()
        try {
            val api = Retrofit.Builder().baseUrl(server.url("/api/v1/"))
                .addConverterFactory(GsonConverterFactory.create(ApiGson.instance)).build().create(UrbanBladeApi::class.java)
            server.enqueue(MockResponse().setBody("""{"data":{"features":{"gift_cards":true}}}"""))
            val features = api.barbershop().data?.features
            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/api/v1/barbershop", request.path)
            assertNotNull(features)
            assertEquals(ModuleFeatures(giftCards = true), features)
        } finally {
            server.shutdown()
        }
    }
}
