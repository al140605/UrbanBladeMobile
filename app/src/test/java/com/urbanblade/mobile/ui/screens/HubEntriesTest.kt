package com.urbanblade.mobile.ui.screens

import com.urbanblade.mobile.data.model.AuthUser
import com.urbanblade.mobile.data.model.ModuleFeatures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Los grupos de «Más» son los mismos, y en el mismo orden, que las secciones del menú web (rediseño de
 * navegación, nov-2026); no anuncian pestañas de módulos apagados y ya no existe el rol ingeniero.
 */
class HubEntriesTest {
    private fun user(vararg roles: String) = AuthUser("1", "Ana", "ana@example.test", roles = roles.toList())
    private val off = ModuleFeatures()
    private val on = ModuleFeatures(packages = true, giftCards = true, referrals = true, raffles = true, social = true, campaigns = true)

    @Test fun `el administrador con todo apagado ve los grupos de la web en el mismo orden`() {
        val entries = hubEntries(user("administrador"), off)
        assertEquals(
            listOf("Clientes", "Caja", "Inventario", "Equipo y catálogo", "Análisis", "Sistema"),
            entries.map { it.title }
        )
        val byTitle = entries.associateBy { it.title }
        // Reseñas dentro de Clientes; membresías dentro de Equipo y catálogo; bitácora dentro de Sistema.
        assertEquals(3, byTitle.getValue("Clientes").tabs)
        assertEquals("barbers_admin", byTitle.getValue("Equipo y catálogo").route)
        assertEquals(4, byTitle.getValue("Equipo y catálogo").tabs)
        assertEquals(4, byTitle.getValue("Análisis").tabs)
        assertEquals(3, byTitle.getValue("Sistema").tabs)
        assertEquals(3, byTitle.getValue("Caja").tabs)
    }

    @Test fun `ya no hay grupos sueltos de membresias, comunidad ni ajustes`() {
        val titles = hubEntries(user("administrador"), on).map { it.title }
        assertFalse("Membresías" in titles)
        assertFalse("Comunidad" in titles)
        assertFalse("Ajustes" in titles)
    }

    @Test fun `el administrador con todo encendido suma Marketing y la moderacion en Clientes`() {
        val entries = hubEntries(user("administrador"), on).associateBy { it.title }
        assertEquals("campaigns", entries.getValue("Marketing").route)
        assertEquals(2, entries.getValue("Marketing").tabs)
        assertEquals(5, entries.getValue("Clientes").tabs)
        assertEquals(4, entries.getValue("Caja").tabs)
    }

    @Test fun `con solo sorteos Marketing abre en sorteos`() {
        val marketing = hubEntries(user("administrador"), ModuleFeatures(raffles = true)).single { it.title == "Marketing" }
        assertEquals("raffles", marketing.route)
        assertEquals(1, marketing.tabs)
    }

    @Test fun `recepcion ve sus grupos sin los de administracion`() {
        val titles = hubEntries(user("recepcionista"), on).map { it.title }
        assertEquals(listOf("Clientes", "Caja", "Inventario", "Análisis"), titles)
        assertFalse("Marketing" in titles)
        assertFalse("Sistema" in titles)
        assertTrue(hubEntries(user("recepcionista"), on).single { it.title == "Clientes" }.tabs == 2)
    }

    @Test fun `una cuenta que solo tiene el rol ingeniero retirado no recibe ningun grupo de personal`() {
        val entries = hubEntries(user("ingeniero"), on)
        assertEquals(listOf("Análisis"), entries.map { it.title })
        assertEquals("analytics", entries.single().route)
    }
}
