package com.teatrino.soundserver

import java.util.UUID

/**
 * El molde de datos para cada botón de sonido en la sesión activa.
 */
data class Sonido(
    // Identificador único (se genera automáticamente como un String único)
    val id: String = UUID.randomUUID().toString(),

    // El nombre que el titiritero le da al botón (ej. "Btn 2" o "Trueno")
    var nombreBoton: String,

    // El nombre real del archivo en el teléfono (ej. "efecto_321.mp3")
    val nombreArchivo: String,

    // El identificador seguro de Android (la ruta interna del archivo)
    val uriString: String,

    // Bandera lógica para pintar el botón en rojo si el archivo desaparece
    var existe: Boolean = true
)
