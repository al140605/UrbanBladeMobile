package com.urbanblade.mobile.data

import com.google.gson.Gson
import com.urbanblade.mobile.data.model.BarbershopSettingResponse
import com.urbanblade.mobile.data.model.RedesSociales
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** `GET settings` no debe romper la pantalla de Configuración según cómo llegue `redes_sociales`. */
class SettingsParsingTest {

    private val gson = Gson()

    private fun settings(redes: String) =
        """{"data":{"id":"1","nombre":"Negocio de prueba","maintenance_mode":false,"redes_sociales":$redes,
            "datos_bancarios":{"banco":"Banco","clabe":"000000000000000000"}}}"""

    @Test
    fun `un arreglo vacio de redes sociales se lee como sin redes`() {
        val setting = gson.fromJson(settings("[]"), BarbershopSettingResponse::class.java).data

        assertEquals("Negocio de prueba", setting.nombre)
        assertEquals(RedesSociales(), setting.redesSociales)
        assertEquals("Banco", setting.datosBancarios.banco)
    }

    @Test
    fun `las redes sociales nulas se leen como sin redes`() {
        val setting = gson.fromJson(settings("null"), BarbershopSettingResponse::class.java).data

        assertEquals(RedesSociales(), setting.redesSociales)
    }

    @Test
    fun `las redes sociales guardadas se leen una por una`() {
        val setting = gson.fromJson(
            settings("""{"instagram":"@barberia","facebook":null,"tiktok":"@barberia.tt","youtube":"ignorada"}"""),
            BarbershopSettingResponse::class.java
        ).data

        assertEquals("@barberia", setting.redesSociales.instagram)
        assertNull(setting.redesSociales.facebook)
        assertEquals("@barberia.tt", setting.redesSociales.tiktok)
    }

    @Test
    fun `escribir y leer las redes sociales conserva sus valores`() {
        val redes = RedesSociales(instagram = "@barberia", tiktok = "@barberia.tt")

        val ida = gson.toJson(redes)
        val vuelta = gson.fromJson(ida, RedesSociales::class.java)

        assertEquals(redes, vuelta)
    }
}
