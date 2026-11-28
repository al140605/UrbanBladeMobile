package com.urbanblade.mobile.t053

import com.urbanblade.mobile.ui.components.UrbanFormat
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * T053 (HT-19): cambio con una prueba FALLIDA A PROPÓSITO para comprobar que el pipeline queda en rojo y no deja
 * integrar el cambio (RNF-19). NO FUSIONAR: esta rama solo existe para la prueba y se borra al terminar.
 */
class T053PruebaFallidaTest {
    @Test
    fun `espera un formato de hora que la app no usa`() {
        // La app muestra «7:30 PM»; la prueba exige el formato de 24 horas para fallar.
        assertEquals("19:30", UrbanFormat.time("19:30"))
    }
}
