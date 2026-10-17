package com.teatrino.soundserver

import kotlinx.serialization.Serializable

/**
 * Molde del mensaje que el celular le envía a la tablet para armar la botonera.
 */
@Serializable
data class RespuestaBotonera(
    val evento: String = "ACTUALIZAR_BOTONERA",
    val sonidos: List<SonidoJson>
)

/**
 * Una versión simplificada de nuestro Sonido sin URIs internas de Android,
 * ideal para enviarla de forma segura a la web.
 */
@Serializable
data class SonidoJson(
    val id: String,
    val nombreBoton: String,
    val nombreArchivo: String
)

/**
 * Molde del mensaje de texto que la tablet le enviará al celular al pulsar un botón.
 */
@Serializable
data class ComandoTablet(
    val accion: String, // "PLAY" o "STOP"
    val id: String,      // El ID único del sonido
    val valor: Float? = null
)
