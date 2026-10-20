package com.teatrino.soundserver

import android.content.Context
import android.net.wifi.WifiManager
import java.net.NetworkInterface

object NetworkUtils {
    fun obtenerIp(context: Context): String {
        // Método 1: WifiManager (el que funciona en moto g35)
        try {
            val wifiManager =
                context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val ipInt = wifiManager.connectionInfo.ipAddress
            if (ipInt != 0) {
                return String.format(
                    "%d.%d.%d.%d",
                    ipInt and 0xff,
                    ipInt shr 8 and 0xff,
                    ipInt shr 16 and 0xff,
                    ipInt shr 24 and 0xff
                )
            }
        } catch (e: Exception) {
        }

        // Método 2: Fallback por interfaces
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (!iface.isUp || iface.isLoopback) continue
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        val host = addr.hostAddress ?: continue
                        if (host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith(
                                "172."
                            )
                        ) {
                            return host
                        }
                    }
                }
            }
        } catch (e: Exception) {
        }

        return "Desconocida"
    }
}