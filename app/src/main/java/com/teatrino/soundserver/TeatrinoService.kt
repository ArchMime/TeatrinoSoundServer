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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TeatrinoService : Service() {
    private var servidorWeb: EmbeddedServer<*, *>? = null
    private val CHANNEL_ID = "TeatrinoServerChannel"

    // Ámbito de corrutinas supervisor que protege a Ktor contra el ahorro de energía
    private val servicioJob = SupervisorJob()
    private val scopeServicio = CoroutineScope(Dispatchers.Default + servicioJob)

    companion object {
        var contextoAplicacion: Context? = null
    }

    override fun onCreate() {
        super.onCreate()
        contextoAplicacion = applicationContext
        crearCanalDeNotificacion()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notificacion = construirNotificacion()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                1,
                notificacion,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(1, notificacion)
        }

        // Iniciamos Ktor de forma segura vinculándolo al ciclo de vida del Foreground Service
        scopeServicio.launch {
            try {
                if (servidorWeb == null) {
                    val ipDetectada = obtenerIpLocal() ?: "127.0.0.1"
                    MainActivity.ipServidor = ipDetectada

                    servidorWeb = iniciarServidorWeb()
                    // wait = false evita congelar el hilo. El servidor vive gracias a scopeServicio.
                    servidorWeb?.start(wait = false)
                    println(" [LOG-SERVER] Ktor iniciado con éxito en CoroutineScope.")
                }
            } catch (e: Exception) {
                println(" [LOG-SERVER] Error al arrancar Ktor: ${e.message}")
                e.printStackTrace()
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        // Detiene de golpe todas las corrutinas que use el servicio
        servicioJob.cancel()

        try {
            servidorWeb?.stop(500, 1000)
            servidorWeb = null
            MainActivity.ipServidor = "Desconocida"
            println(" [LOG-SERVER] Servidor Ktor detenido limpiamente.")
        } catch (e: Exception) {
            e.printStackTrace()
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
