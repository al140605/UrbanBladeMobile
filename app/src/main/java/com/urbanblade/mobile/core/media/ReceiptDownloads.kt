package com.urbanblade.mobile.core.media

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat

/**
 * Comprobantes en PDF: se descargan al teléfono en vez de abrir la liga firmada en el navegador
 * (así el cliente no ve la dirección de S3). Quedan en Descargas/UrbanBlade y, al terminar, se
 * abren en el visor de PDF del teléfono; si no hay visor, la notificación del sistema los abre.
 */
object ReceiptDownloads {

    /** "Comprobante cita 28 sep" -> "Comprobante-cita-28-sep.pdf" (sin caracteres que rompan el nombre). */
    fun fileName(label: String): String =
        label.trim().replace(Regex("[^A-Za-z0-9áéíóúñÁÉÍÓÚÑ._-]+"), "-").trim('-').ifBlank { "Comprobante" } + ".pdf"

    /** Encola la descarga y devuelve su id, o null si el sistema no la aceptó. */
    fun enqueue(context: Context, url: String, fileName: String): Long? = runCatching {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(fileName)
            .setDescription("Comprobante de UrbanBlade")
            .setMimeType("application/pdf")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        // Android 10+ permite escribir en Descargas sin permisos; antes se usa la carpeta de la app.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "UrbanBlade/$fileName")
        } else {
            request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
        }
        manager(context).enqueue(request)
    }.getOrNull()

    /**
     * Escucha el fin de las descargas: [onFinished] recibe el id y si terminó bien. Hay que llamar a
     * la función devuelta para dejar de escuchar.
     */
    fun listen(context: Context, onFinished: (id: Long, ok: Boolean) -> Unit): () -> Unit {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (id >= 0) onFinished(id, succeeded(ctx, id))
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_EXPORTED
        )
        return { runCatching { context.unregisterReceiver(receiver) } }
    }

    /** Abre el PDF descargado en el visor del teléfono. Devuelve false si no hay app para abrirlo. */
    fun open(context: Context, id: Long): Boolean = runCatching {
        val uri = manager(context).getUriForDownloadedFile(id) ?: return false
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/pdf")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        true
    }.getOrDefault(false)

    private fun succeeded(context: Context, id: Long): Boolean = runCatching {
        manager(context).query(DownloadManager.Query().setFilterById(id)).use { c ->
            c.moveToFirst() && c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL
        }
    }.getOrDefault(false)

    private fun manager(context: Context) = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
}
