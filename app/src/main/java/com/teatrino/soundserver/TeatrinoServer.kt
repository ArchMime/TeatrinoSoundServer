package com.teatrino.soundserver

import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticResources
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

class TeatrinoServerConfig {
    companion object {
        var sesionActiva: WebSocketServerSession? = null

        /**
         * Envía la lista de sonidos actual de la sesión a la tablet de forma reactiva y segura.
         */
        suspend fun forzarSincronizacionWeb() {
            val sesion = sesionActiva ?: return
            try {
                // Leemos la lista de sonidos de forma segura aislándola del hilo de Compose
                val copiaEstatica =
                    androidx.compose.runtime.snapshots.Snapshot.withoutReadObservation {
                        MainActivity.listaSonidosSesion.toList()
                    }

                val listaSimplificada = copiaEstatica.map { sonido ->
                    SonidoJson(
                        id = sonido.id,
                        nombreBoton = sonido.nombreBoton,
                        nombreArchivo = sonido.nombreArchivo
                    )
                }

                val paquete = RespuestaBotonera(sonidos = listaSimplificada)
                val jsonString = Json.encodeToString(paquete)

                println("🚀 [LOG-SERVER] Forzando transmisión reactiva hacia la tablet: $jsonString")
                sesion.send(Frame.Text(jsonString))
            } catch (e: Exception) {
                println("❌ [LOG-SERVER] Error al sincronizar vía WebSocket: ${e.message}")
                sesionActiva = null
            }
        }
    }
}

fun iniciarServidorWeb(): EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration> {
    return embeddedServer(CIO, configure = {
        connectors.add(io.ktor.server.engine.EngineConnectorBuilder().apply {
            port = 8080
            host = "0.0.0.0"
        })
        reuseAddress = true
    }) {

        install(WebSockets) {
            pingPeriod = 15.seconds
            timeout = 30.seconds
            maxFrameSize = Long.MAX_VALUE
        }

        routing {
            // Servir los archivos web estáticos (index.html, estilos.css, controlador.js)
            staticResources(remotePath = "/", basePackage = "static")

            webSocket("/socket") {
                println("🔌 [LOG-SERVER] ¡Tablet conectada!")
                TeatrinoServerConfig.sesionActiva = this

                var idSonidoActual: String? = null
                var estaPausado = false

                try {
                    // Forzar sincronización inmediata al conectar para persistencia de la interfaz web
                    TeatrinoServerConfig.forzarSincronizacionWeb()

                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val textoRecibido = frame.readText()
                            try {
                                val comando = Json.decodeFromString<ComandoTablet>(textoRecibido)

                                if (comando.accion == "STOP") {
                                    println("⏹️ [LOG-SERVER] Comando STOP Global recibido")
                                    AudioPlayerManager.detenerAudio()
                                    idSonidoActual = null
                                    estaPausado = false
                                    continue
                                }

                                if (comando.accion == "VOLUME") {
                                    val vol = comando.valor
                                    if (vol != null) {
                                        AudioPlayerManager.ajustarVolumen(vol)
                                    }
                                    continue
                                }

                                if (comando.accion == "PLAY") {
                                    val sonidoEncontrado = MainActivity.listaSoundSesion(comando.id)

                                    if (sonidoEncontrado != null) {
                                        val contextoAndroid = TeatrinoService.contextoAplicacion

                                        if (contextoAndroid != null) {
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
                                                    AudioPlayerManager.reproducirAudio(
                                                        contextoAndroid,
                                                        sonidoEncontrado.uriString
                                                    )
                                                }
                                            } else {
                                                println("🎵 [LOG-SERVER] Cambiando de tema a: ${sonidoEncontrado.nombreArchivo}")
                                                AudioPlayerManager.reproducirAudio(
                                                    contextoAndroid,
                                                    sonidoEncontrado.uriString
                                                )
                                                idSonidoActual = comando.id
                                                estaPausado = false
                                            }
                                        } else {
                                            println("⚠️ [LOG-SERVER] No se puede reproducir: Contexto de Android nulo.")
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                println("❌ [LOG-SERVER] Error al decodificar comando JSON: ${e.message}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("❌ [LOG-SERVER] Excepción en el bucle del WebSocket: ${e.message}")
                    e.printStackTrace()
                } finally {
                    TeatrinoServerConfig.sesionActiva = null
                    println("🔌 [LOG-SERVER] Tablet desconectada. Canal liberado de forma limpia.")
                }
            }
        }
    }
}

// Función de extensión corregida para buscar sonidos en la sesión de forma segura
fun MainActivity.Companion.listaSoundSesion(id: String): Sonido? {
    return listaSonidosSesion.find { it.id == id }
}
