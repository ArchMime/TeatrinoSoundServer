package com.teatrino.soundserver

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

/**
 * Convierte un String de texto (la URL) en una imagen de mapa de bits (Bitmap)
 * procesando la matriz de forma 100% offline.
 */
fun generarQrBitmap(contenido: String, tamaño: Int = 512): Bitmap? {
    return try {
        // 1. El escritor de ZXing calcula la matriz binaria del código QR
        val matrizBits = QRCodeWriter().encode(
            contenido,
            BarcodeFormat.QR_CODE,
            tamaño,
            tamaño
        )

        // 2. Creamos un mapa de bits vacío del tamaño solicitado
        val bitmap = Bitmap.createBitmap(tamaño, tamaño, Bitmap.Config.ARGB_8888)

        // 3. Recorremos la matriz fila por fila, columna por columna
        for (x in 0 until tamaño) {
            for (y in 0 until tamaño) {
                // Si la posición de la matriz tiene un bit activo, pintamos negro. Si no, blanco.
                val color = if (matrizBits.get(x, y)) {
                    android.graphics.Color.BLACK
                } else {
                    android.graphics.Color.WHITE
                }
                bitmap.setPixel(x, y, color)
            }
        }
        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
