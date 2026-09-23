package com.urbanblade.mobile.data

import com.google.gson.Gson
import com.urbanblade.mobile.data.model.AppointmentsResponse
import com.urbanblade.mobile.data.model.BarbersAdminResponse
import com.urbanblade.mobile.data.model.ClientDetailResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Los nombres de los campos de foto deben coincidir con los que manda el servidor (snake_case). */
class PhotoFieldsParsingTest {

    private val gson = Gson()

    @Test
    fun `la cita trae la foto del cliente y del barbero`() {
        val json = """{"data":[{"id":"1","client":{"id":"c","user":{"name":"Ana","avatar_url":"https://x/a.jpg"}},
            "barber":{"id":"b","slug":"carlos","user":{"name":"Carlos"},"foto_url":"https://x/c.jpg"}}]}"""

        val appt = gson.fromJson(json, AppointmentsResponse::class.java).data.first()

        assertEquals("https://x/a.jpg", appt.client?.user?.avatarUrl)
        assertEquals("https://x/c.jpg", appt.barber?.fotoUrl)
    }

    @Test
    fun `sin foto los campos quedan nulos y no rompen`() {
        val json = """{"data":[{"id":"1","client":{"id":"c","user":{"name":"Ana","avatar_url":null}},"barber":{"id":"b","foto_url":null}}]}"""

        val appt = gson.fromJson(json, AppointmentsResponse::class.java).data.first()

        assertNull(appt.client?.user?.avatarUrl)
        assertNull(appt.barber?.fotoUrl)
    }

    @Test
    fun `el barbero de gestion y el detalle de cliente traen su foto`() {
        val barbers = gson.fromJson(
            """{"data":[{"id":"1","slug":"carlos","foto":"barbers/c.jpg","foto_url":"https://x/c.jpg","user":{"name":"Carlos","email":"c@x.com"}}]}""",
            BarbersAdminResponse::class.java
        )
        val client = gson.fromJson("""{"data":{"id":"9","name":"Ana","avatar_url":"https://x/a.jpg"}}""", ClientDetailResponse::class.java)

        assertEquals("https://x/c.jpg", barbers.data.first().fotoUrl)
        assertEquals("https://x/a.jpg", client.data?.avatarUrl)
    }
}
