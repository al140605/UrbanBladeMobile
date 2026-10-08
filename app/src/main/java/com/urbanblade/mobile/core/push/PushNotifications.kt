package com.urbanblade.mobile.core.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.urbanblade.mobile.MainActivity
import com.urbanblade.mobile.R
import com.urbanblade.mobile.data.repository.UrbanRepository
import com.urbanblade.mobile.ui.theme.UrbanColors
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Notificaciones push con Firebase Cloud Messaging (T142, HU-16).
 *
 * El token del dispositivo se registra en barber con POST /profile/push-token;
 * el envío de los recordatorios desde el backend es T143. Si la app se compiló
 * sin app/google-services.json, Firebase no se inicializa y todo esto queda
 * apagado sin romper nada (isAvailable() == false).
 */
object PushNotifications {

    fun isAvailable(context: Context): Boolean = FirebaseApp.getApps(context).isNotEmpty()

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Crea (o actualiza) todos los canales; se llama al arrancar para que existan antes del primer aviso. */
    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        PushChannel.entries.forEach { c ->
            val importance = if (c.important) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT
            manager.createNotificationChannel(
                NotificationChannel(c.id, c.label, importance).apply { description = c.description }
            )
        }
    }

    /** Obtiene el token FCM actual y lo registra en barber. Devuelve false si no se pudo. */
    suspend fun registerCurrentToken(context: Context, repository: UrbanRepository): Boolean {
        if (!isAvailable(context)) return false
        val token = currentToken() ?: return false
        return try {
            repository.savePushToken(token)
            true
        } catch (_: Exception) {
            false // Se reintenta en el siguiente inicio de sesión o en onNewToken.
        }
    }

    /**
     * Al cerrar sesión, el teléfono borra su token de Firebase: la cuenta anterior deja de recibir
     * avisos en este equipo y la siguiente que entre registra uno nuevo. Si Firebase no está
     * configurado o falla, no bloquea el cierre de sesión.
     */
    suspend fun forgetDeviceToken(context: Context) {
        if (!isAvailable(context)) return
        suspendCancellableCoroutine { cont ->
            FirebaseMessaging.getInstance().deleteToken().addOnCompleteListener { cont.resume(Unit) }
        }
    }

    private suspend fun currentToken(): String? = suspendCancellableCoroutine { cont ->
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            cont.resume(if (task.isSuccessful) task.result else null)
        }
    }

    fun show(context: Context, content: PushContent) {
        if (!canNotify(context)) return
        ensureChannels(context)
        val openApp = PendingIntent.getActivity(
            context, content.channel.ordinal,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(PushDeepLink.EXTRA_ROUTE, content.route),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, content.channel.id)
            .setSmallIcon(R.drawable.ic_stat_urbanblade)
            .setColor(UrbanColors.Gold.toArgb())
            .setContentTitle(content.title)
            .setContentText(content.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content.body))
            .setPriority(if (content.channel.important) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(if (content.channel == PushChannel.PROMOCIONES) NotificationCompat.CATEGORY_PROMO else NotificationCompat.CATEGORY_EVENT)
            // Se agrupan por canal para que varios avisos no llenen la bandeja.
            .setGroup("urbanblade_${content.channel.id}")
            // En pantalla de bloqueo solo se ve "UrbanBlade", no los datos de la cita (checklist CN-098).
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(
                NotificationCompat.Builder(context, content.channel.id)
                    .setSmallIcon(R.drawable.ic_stat_urbanblade)
                    .setColor(UrbanColors.Gold.toArgb())
                    .setContentTitle("UrbanBlade")
                    .setContentText("Tienes un aviso nuevo")
                    .build()
            )
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
        } catch (_: SecurityException) {
            // El usuario retiró el permiso entre canNotify() y notify().
        }
    }
}
