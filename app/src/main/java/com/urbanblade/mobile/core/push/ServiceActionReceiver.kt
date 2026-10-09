package com.urbanblade.mobile.core.push

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.urbanblade.mobile.core.appointments.canForceFromBody
import com.urbanblade.mobile.core.network.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import retrofit2.HttpException

/**
 * Botones de la notificación «tu servicio termina en 5 min»: terminar ya o agregar 10/15 min sin abrir la app. Llama a
 * la misma API que la agenda (PATCH status / POST extend) con la sesión guardada y deja el resultado en la propia
 * notificación. Si agregar tiempo choca con la siguiente cita, no fuerza nada: lleva a la agenda para decidir.
 */
class ServiceActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val code = intent.getStringExtra(EXTRA_CODE)?.takeIf { it.isNotBlank() } ?: return
        val action = parseServiceAction(intent.getStringExtra(EXTRA_ACTION)) ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, code.hashCode())
        val app = context.applicationContext
        val pending = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val repo = AppContainer.urbanRepository
                when (action) {
                    ServiceAction.Finish -> {
                        repo.updateAppointmentStatus(code, "completada")
                        PushNotifications.showResult(app, notificationId, "Servicio terminado", "Listo. Se envió el ticket al cliente.")
                    }
                    is ServiceAction.Extend -> {
                        repo.extendAppointment(code, action.minutes, forzar = false)
                        PushNotifications.showResult(app, notificationId, "Tiempo agregado", "Se agregaron ${action.minutes} minutos y avisamos al cliente.")
                    }
                }
            } catch (e: Exception) {
                val body = (e as? HttpException)?.takeIf { it.code() == 422 }?.response()?.errorBody()?.string()
                if (canForceFromBody(body)) {
                    PushNotifications.showResult(
                        app, notificationId, "Choca con la siguiente cita",
                        "Abre tu agenda para decidir si extiendes de todos modos.", openAgenda = true
                    )
                } else {
                    val reason = runCatching { com.google.gson.JsonParser.parseString(body).asJsonObject.get("message").asString }.getOrNull()
                    PushNotifications.showResult(
                        app, notificationId, "No se pudo completar",
                        reason ?: "Abre tu agenda e inténtalo desde ahí.", openAgenda = true
                    )
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val EXTRA_CODE = "appointment_code"
        const val EXTRA_ACTION = "service_action"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }
}
