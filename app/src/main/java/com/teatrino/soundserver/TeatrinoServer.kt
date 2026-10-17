package com.teatrino.soundserver

import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.routing.routing
import io.ktor.server.http.content.staticResources
import io.ktor.server.application.install
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send
import io.ktor.server.websocket.WebSocketServerSession
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class TeatrinoServerConfig {
    companion object {
        var sesionActiva: WebSocketServerSession? = null

        /**
         * Envía la lista de sonidos actual de la sesión a la tablet de forma reactiva.
         */
        suspend fun forzarSincronizacionWeb() {
            val sesion = sesionActiva ?: return
            try {
                // Copia segura de la lista de Compose libre de bloqueos de hilos
                val copiaEstatica = androidx.compose.runtime.snapshots.Snapshot.withoutReadObservation {
                    MainActivity.listaSonidosSesion.toList()
                }

                val listaSimplificada = copiaEstatica.map { sonido ->
                    SonidoJson(id = sonido.id, nombreBoton = sonido.nombreBoton, nombreArchivo = sonido.nombreArchivo)
                }

                val paquete = RespuestaBotonera(sonidos = listaSimplificada)
                val jsonString = Json.encodeToString(paquete)

                println("🚀 [LOG-SERVER] Forzando transmisión reactiva: $jsonString")
                sesion.send(Frame.Text(jsonString))
            } catch (e: Exception) {
                println("❌ [LOG-SERVER] Error al sincronizar vía WebSocket: ${e.message}")
            }
        }
    }
}

fun iniciarServidorWeb(): EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration> {
    return embeddedServer(CIO, port = 8080, host = "0.0.0.0") {
        install(WebSockets)

        routing {
            staticResources(remotePath = "/", basePackage = "static")

            webSocket("/socket") {
                println("🔌 [LOG-SERVER] ¡Tablet conectada!")
                TeatrinoServerConfig.sesionActiva = this

                // VARIABLES DE MEMORIA DE LA SESIÓN DE AUDIO
                var idSonidoActual: String? = null
                var estaPausado = false

                try {
                    TeatrinoServerConfig.forzarSincronizacionWeb()

                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val textoRecibido = frame.readText()
                            try {
                                val comando = Json.decodeFromString<ComandoTablet>(textoRecibido)

                                // CASO 1: COMANDO DE STOP GENERAL
                                if (comando.accion == "STOP") {
                                    println("⏹️ [LOG-SERVER] Comando STOP Global recibido")
                                    AudioPlayerManager.detenerAudio()
                                    idSonidoActual = null
                                    estaPausado = false
                                    continue
                                }

                                // NUEVO - CASO 2: COMANDO DE VOLUMEN INALÁMBRICO
                                if (comando.accion == "VOLUME") {
                                    val vol = comando.valor
                                    if (vol != null) {
                                        AudioPlayerManager.ajustarVolumen(vol)
                                    }
                                    continue
                                }

                                // CASO 2: COMANDO DE DISPARO / PAUSA (PLAY)
                                if (comando.accion == "PLAY") {
                                    val sonidoEncontrado = MainActivity.listaSoundSesion(comando.id)

                                    if (sonidoEncontrado != null) {
                                        // Leemos el contexto inyectado desde el Foreground Service de forma segura
                                        val contextoAndroid = TeatrinoService.contextoAplicacion

                                        if (contextoAndroid != null) {
                                            // ¿Es el mismo sonido que ya está interactuando?
                                            if (idSonidoActual == comando.id) {
                                                if (AudioPlayerManager.estaSonando()) {
                                                    AudioPlayerManager.pausarAudio()
                                                    estaPausado = true
                                                    println("⏸️ [LOG-SERVER] Pausando en vivo: ${sonidoEncontrado.nombreArchivo}")
                                                } else if (estaPausado) {
                                                    AudioPlayerManager.reanudarAudio()
                                                    estaPausado = false
                                                    println("▶️ [LOG-SERVER] Reanudando: ${sonidoEncontrado.nombreArchivo}")
                                                } else {
                                                    // Fallback por si terminó solo: arranca de cero
                                                    AudioPlayerManager.reproducirAudio(contextoAndroid, sonidoEncontrado.uriString)
                                                }
                                            } else {
                                                // Es un sonido NUEVO o DIFERENTE: arranca de cero de inmediato
                                                println("🎵 [LOG-SERVER] Cambiando de tema a: ${sonidoEncontrado.nombreArchivo}")
                                                AudioPlayerManager.reproducirAudio(contextoAndroid, sonidoEncontrado.uriString)
                                                idSonidoActual = comando.id
                                                estaPausado = false
                                            }
                                        } else {
                                            println("⚠️ [LOG-SERVER] No se puede reproducir: Contexto de Android nulo.")
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                println("❌ [LOG-SERVER] Error al decodificar: ${e.message}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    TeatrinoServerConfig.sesionActiva = null
                    println("🔌 [LOG-SERVER] Tablet desconectada")
                }
            }
        }
    }
}

fun MainActivity.Companion.listaSoundSesion(id: String): Sonido? {
    return listaSonidosSesion.find { it.id == id }
}
