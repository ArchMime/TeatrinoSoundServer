package com.teatrino.soundserver

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri

object AudioPlayerManager {
    private var mediaPlayer: MediaPlayer? = null

    // NUEVO: Almacenamos el volumen actual en memoria (por defecto al máximo = 1.0f)
    private var volumenActual = 1.0f

    fun estaSonando(): Boolean {
        return try {
            mediaPlayer?.isPlaying ?: false
        } catch (e: Exception) {
            false
        }
    }

    fun reproducirAudio(contexto: Context, uriString: String) {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(contexto, Uri.parse(uriString))
                prepare()

                // NUEVO: Le inyectamos el volumen recordado antes de arrancar
                setVolume(volumenActual, volumenActual)
                start()
            }
            println("▶️ [MEDIA-PLAYER] Reproduciendo con volumen: $volumenActual")
        } catch (e: Exception) {
            println("❌ [MEDIA-PLAYER] Error al reproducir: ${e.message}")
            e.printStackTrace()
        }
    }

    // NUEVO: Ajusta el volumen dinámicamente en vivo
    fun ajustarVolumen(nuevoVolumen: Float) {
        try {
            // Aseguramos que el valor esté estrictamente acotado entre 0.0 y 1.0
            volumenActual = nuevoVolumen.coerceIn(0.0f, 1.0f)

            // Si hay un audio sonando en este instante, le alteramos la ganancia por hardware de inmediato
            mediaPlayer?.setVolume(volumenActual, volumenActual)
            println("🔊 [MEDIA-PLAYER] Volumen ajustado a: $volumenActual")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun reanudarAudio() {
        try {
            mediaPlayer?.start()
            println("▶️ [MEDIA-PLAYER] Audio reanudado.")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun pausarAudio() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                println("⏸️ [MEDIA-PLAYER] Audio pausado.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun detenerAudio() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            println("⏹️ [MEDIA-PLAYER] Sonido detenido por completo.")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
