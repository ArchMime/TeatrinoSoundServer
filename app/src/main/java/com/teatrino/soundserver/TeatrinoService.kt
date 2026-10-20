package com.teatrino.soundserver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import io.ktor.server.engine.EmbeddedServer

class TeatrinoService : Service() {
    companion object {
        var contextoAplicacion: Context? = null
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private var server: EmbeddedServer<*, *>? = null

    override fun onCreate() {
        super.onCreate()
        contextoAplicacion = applicationContext

        // 1. Evita que el CPU se duerma
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Teatrino::ServerLock")
        wakeLock?.acquire()

        // 2. Evita que el WiFi se corte al bloquear el dispositivo
        val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "Teatrino::WifiLock")
        wifiLock?.acquire()

        // 3. Levantar la notificación de primer plano obligatoria
        startForeground(1, crearNotificacion())

        // 4. Arrancar el servidor web Ktor de forma asíncrona
        try {
            server = iniciarServidorWeb()
            server?.start(wait = false)

            // Sincronizar el estado global nativo al encender con éxito
            MainActivity.servidorEncendido = true
            MainActivity.ipServidor = NetworkUtils.obtenerIp(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY // Si el sistema destruye el servicio por memoria, lo recrea automáticamente
    }

    override fun onDestroy() {
        // Detener el servidor web de forma limpia y liberar puertos
        try {
            server?.stop(1000, 2000)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Liberar candados de hardware de inmediato
        wakeLock?.let { if (it.isHeld) it.release() }
        wifiLock?.let { if (it.isHeld) it.release() }

        // Sincronizar estado global al apagar: Informar a la interfaz que el servidor cayó
        MainActivity.servidorEncendido = false
        MainActivity.ipServidor = "Desconocida"

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun crearNotificacion(): Notification {
        val channelId = "teatrino_server"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Servidor Teatrino",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val ipActual = NetworkUtils.obtenerIp(this)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Teatrino activo")
            .setContentText("Servidor en $ipActual:8080")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()
    }
}
