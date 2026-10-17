package com.teatrino.soundserver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import io.ktor.server.engine.EmbeddedServer
import kotlin.concurrent.thread

class TeatrinoService : Service() {

    private var servidorWeb: EmbeddedServer<*, *>? = null
    private val CHANNEL_ID = "TeatrinoServerChannel"

    // ALMACÉN ESTÁTICO DE INYECCIÓN: Expone el contexto de Android de forma limpia a Ktor
    companion object {
        var contextoAplicacion: Context? = null
    }

    override fun onCreate() {
        super.onCreate()
        // Capturamos el contexto real de la app en cuanto Android inicializa el servicio
        contextoAplicacion = applicationContext
        crearCanalDeNotificacion()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notificacion = construirNotificacion()

        // Verificación de simetría obligatoria para Android 14+ (SDK 34 a 37)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notificacion, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(1, notificacion)
        }

        thread {
            try {
                if (servidorWeb == null) {
                    val ipDetectada = obtenerIpLocal() ?: "127.0.0.1"
                    MainActivity.ipServidor = ipDetectada

                    servidorWeb = iniciarServidorWeb()
                    servidorWeb?.start(wait = true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        thread {
            try {
                servidorWeb?.stop(500, 1000)
                servidorWeb = null
                MainActivity.ipServidor = "Desconocida"
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun crearCanalDeNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CHANNEL_ID,
                "Servidor de Audio Teatrino",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(canal)
        }
    }

    private fun construirNotificacion(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Teatrino Sound Server")
            .setContentText("El servidor inalámbrico está encendido y listo.")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .build()
    }
}
