package com.urbanblade.mobile.core.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.urbanblade.mobile.core.network.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Recibe los mensajes de Firebase Cloud Messaging (T142). */
class UrbanMessagingService : FirebaseMessagingService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Firebase rotó el token: se vuelve a registrar si hay sesión iniciada. */
    override fun onNewToken(token: String) {
        scope.launch {
            if (AppContainer.sessionManager.currentToken().isNullOrBlank()) return@launch
            try { AppContainer.urbanRepository.savePushToken(token) } catch (_: Exception) { }
        }
    }

    /** Con la app en primer plano Firebase no muestra la notificación: se muestra aquí. */
    override fun onMessageReceived(message: RemoteMessage) {
        pushContent(message.notification?.title, message.notification?.body, message.data)
            ?.let { PushNotifications.show(applicationContext, it) }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
