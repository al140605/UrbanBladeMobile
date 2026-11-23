package com.urbanblade.mobile.ui.screens

import com.urbanblade.mobile.data.model.AuthUser
import com.urbanblade.mobile.data.model.ModuleFeatures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Los grupos de «Más» no anuncian pestañas de módulos apagados y ya no existe el rol ingeniero. */
class HubEntriesTest {
    private fun user(vararg roles: String) = AuthUser("1", "Ana", "ana@example.test", roles = roles.toList())
    private val off = ModuleFeatures()
    private val on = ModuleFeatures(packages = true, giftCards = true, referrals = true, raffles = true, social = true, campaigns = true)

    @Test fun `el administrador con todo apagado ve Marketing como Membresias y Comunidad solo con resenas`() {
        val entries = hubEntries(user("administrador"), off).associateBy { it.title }
        assertEquals("offers_admin", entries.getValue("Membresías").route)
        assertEquals(1, entries.getValue("Membresías").tabs)
        assertFalse(entries.containsKey("Marketing"))
        assertEquals("reviews", entries.getValue("Comunidad").route)
        assertEquals(1, entries.getValue("Comunidad").tabs)
        assertEquals(3, entries.getValue("Caja").tabs)
    }

    @Test fun `el administrador con todo encendido conserva los grupos completos`() {
        val entries = hubEntries(user("administrador"), on).associateBy { it.title }
        assertEquals("campaigns", entries.getValue("Marketing").route)
        assertEquals(3, entries.getValue("Marketing").tabs)
        assertEquals(3, entries.getValue("Comunidad").tabs)
        assertEquals(4, entries.getValue("Caja").tabs)
    }

    @Test fun `con solo sorteos Marketing abre en sorteos`() {
        val marketing = hubEntries(user("administrador"), ModuleFeatures(raffles = true)).single { it.title == "Marketing" }
        assertEquals("raffles", marketing.route)
        assertEquals(2, marketing.tabs)
    }

    @Test fun `recepcion no ve grupos de administracion`() {
        val titles = hubEntries(user("recepcionista"), on).map { it.title }
        assertTrue("Caja" in titles)
        assertFalse("Marketing" in titles)
        assertFalse("Comunidad" in titles)
    }

    @Test fun `una cuenta que solo tiene el rol ingeniero retirado no recibe ningun grupo de personal`() {
        val entries = hubEntries(user("ingeniero"), on)
        assertEquals(listOf("Análisis"), entries.map { it.title })
        assertEquals("analytics", entries.single().route)
    }
}
