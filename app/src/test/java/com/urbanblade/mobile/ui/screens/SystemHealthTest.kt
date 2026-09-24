package com.urbanblade.mobile.ui.screens

import com.urbanblade.mobile.data.model.SystemQueueStatus
import com.urbanblade.mobile.data.model.SystemScheduledTask
import com.urbanblade.mobile.data.model.SystemServiceStatus
import com.urbanblade.mobile.data.model.SystemStatusResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemHealthTest {

    private val up = SystemServiceStatus("up", 12)
    private fun status(
        database: SystemServiceStatus = up,
        redis: SystemServiceStatus = up,
        failed: Int = 0,
        tasks: List<SystemScheduledTask> = emptyList()
    ) = SystemStatusResponse(database = database, redis = redis, queue = SystemQueueStatus("redis", 0, failed), scheduledTasks = tasks)

    @Test
    fun `todo arriba y sin fallos es OK`() {
        val health = systemHealth(status(tasks = listOf(SystemScheduledTask("backup", status = "success"))))
        assertEquals(HealthLevel.OK, health.level)
        assertEquals("Todo funciona con normalidad", health.headline)
        assertTrue(health.issues.isEmpty())
    }

    @Test
    fun `un servicio caido gana a cualquier advertencia y va primero`() {
        val health = systemHealth(status(database = SystemServiceStatus("down", null, "timeout"), failed = 3))
        assertEquals(HealthLevel.DOWN, health.level)
        assertEquals("Hay servicios caídos", health.headline)
        assertEquals("La base de datos no responde", health.issues.first().title)
        assertEquals("timeout", health.issues.first().detail)
    }

    @Test
    fun `trabajos fallidos y tareas fallidas son advertencias`() {
        val health = systemHealth(status(failed = 1, tasks = listOf(SystemScheduledTask("reminders", status = "failed", error = "SMTP"))))
        assertEquals(HealthLevel.WARNING, health.level)
        assertEquals("Funciona, con 2 puntos por revisar", health.headline)
        assertEquals(listOf("1 trabajo fallido en la cola", "Falló la tarea reminders"), health.issues.map { it.title })
    }

    @Test
    fun `latencia alta o estado desconocido tambien se revisan`() {
        val slow = systemHealth(status(redis = SystemServiceStatus("up", 1500)))
        assertEquals("Redis (caché, sesiones y colas) responde lento", slow.issues.single().title)
        assertEquals("1500 ms", slow.issues.single().detail)

        val unknown = systemHealth(status(database = SystemServiceStatus(null)))
        assertEquals("No se pudo verificar la base de datos", unknown.issues.single().title)
        assertEquals(HealthLevel.WARNING, unknown.level)
    }
}
