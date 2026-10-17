package com.teatrino.soundserver

import java.net.NetworkInterface
import java.net.Inet4Address
import java.util.Collections

/**
 * Función utilitaria para escanear las interfaces de red del celular
 * y extraer la dirección IPv4 local activa.
 */
fun obtenerIpLocal(): String? {
    try {
        // 1. Obtenemos todas las interfaces de red (Wi-Fi, Datos, Hotspot, Loopback...)
        val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())

        // 2. Recorremos cada interfaz encontrada
        for (interfaz in interfaces) {
            // Saltamos interfaces que estén apagadas o sean de bucle local (localhost / 127.0.0.1)
            if (!interfaz.isUp || interfaz.isLoopback) continue

            // 3. Revisamos las direcciones asignadas a esta interfaz
            val direcciones = Collections.list(interfaz.inetAddresses)
            for (direccion in direcciones) {
                // Filtramos para quedarnos estrictamente con una dirección IPv4 (ej. 192.168.1.5)
                if (!direccion.isLoopbackAddress && direccion is Inet4Address) {
                    val ip = direccion.hostAddress

                    // Opcional: Si necesitas dar prioridad al Hotspot/Wi-Fi del celular,
                    // sus nombres suelen empezar con "wlan", "ap" o "rndis".
                    return ip
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    // Si el celular no está conectado a ninguna red (ni Wi-Fi ni Hotspot), devolverá null
    return null
}
