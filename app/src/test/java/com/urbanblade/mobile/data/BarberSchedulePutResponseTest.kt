package com.urbanblade.mobile.data

import com.urbanblade.mobile.core.network.ApiGson
import com.urbanblade.mobile.data.model.BarberScheduleResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Al guardar el horario el servidor devuelve los registros crudos (`is_working`), no los normalizados de la
 * consulta (`is_active`). Si la app solo entendiera `is_active`, tras guardar todos los días quedaban en descanso.
 */
class BarberSchedulePutResponseTest {
    private fun parse(json: String) = ApiGson.instance.fromJson(json, BarberScheduleResponse::class.java)

    @Test
    fun `la respuesta de guardar con is_working conserva los dias activos`() {
        val put = """{"message":"Horarios actualizados correctamente","schedules":[
            {"day_of_week":"monday","start_time":"09:00","end_time":"18:00","is_working":true},
            {"day_of_week":"tuesday","start_time":"09:00","end_time":"18:00","is_working":false}]}"""
        val days = parse(put).schedules
        assertEquals(2, days.size)
        assertTrue(days[0].isActive)
        assertEquals(false, days[1].isActive)
    }

    @Test
    fun `la consulta con is_active sigue funcionando`() {
        val get = """{"schedules":[{"day_of_week":"monday","start_time":"09:00","end_time":"18:00","is_active":true}]}"""
        assertTrue(parse(get).schedules.single().isActive)
    }
}
