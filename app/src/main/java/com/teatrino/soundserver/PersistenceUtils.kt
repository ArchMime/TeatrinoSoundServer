package com.teatrino.soundserver

import android.content.Context
import android.net.Uri
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.Serializable

@Serializable
data class ListaObraGuardada(
    val nombreObra: String,
    val sonidos: List<SonidoPersistente>
)

@Serializable
data class SonidoPersistente(
    val id: String,
    val nombreBoton: String,
    val nombreArchivo: String,
    val uriString: String
)

object PersistenceUtils {
    private const val PREFS_NAME = "TeatrinoListasPrefs"

    /**
     * Guarda la sesión activa de sonidos en el almacenamiento interno del celular como JSON.
     */
    fun guardarLista(contexto: Context, nombreObra: String, listaSonidos: List<Sonido>) {
        try {
            val prefs = contexto.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            // Simplificamos la lista para persistirla
            val sonidosPersistentes = listaSonidos.map {
                SonidoPersistente(it.id, it.nombreBoton, it.nombreArchivo, it.uriString)
            }
            val obraData = ListaObraGuardada(nombreObra, sonidosPersistentes)
            val jsonString = Json.encodeToString(obraData)

            prefs.edit().putString(nombreObra, jsonString).apply()
            println("💾 [PERSISTENCIA] Obra '$nombreObra' guardada con éxito.")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Obtiene los nombres de todas las obras guardadas por el usuario.
     */
    fun obtenerNombresObras(contexto: Context): List<String> {
        val prefs = contexto.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.all.keys.toList()
    }

    /**
     * Recupera una lista guardada y VERIFICA en microsegundos si cada archivo sigue existiendo.
     */
    fun cargarLista(contexto: Context, nombreObra: String): List<Sonido> {
        val listaResultado = mutableListOf<Sonido>()
        try {
            val prefs = contexto.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonString = prefs.getString(nombreObra, null) ?: return emptyList()

            val obraData = Json.decodeFromString<ListaObraGuardada>(jsonString)

            for (sp in obraData.sonidos) {
                // VERIFICACIÓN DE SEGURIDAD PREVENTIVA: ¿El archivo sigue vivo en el celular?
                var archivoExiste = false
                try {
                    contexto.contentResolver.openInputStream(Uri.parse(sp.uriString))?.use {
                        archivoExiste = true // El archivo se abrió con éxito
                    }
                } catch (e: Exception) {
                    // Si el archivo fue borrado o movido, Android arroja una excepción
                    archivoExiste = false
                }

                listaResultado.add(
                    Sonido(
                        id = sp.id,
                        nombreBoton = sp.nombreBoton,
                        nombreArchivo = sp.nombreArchivo,
                        uriString = sp.uriString,
                        existe = archivoExiste // Si da falso, la fila se pintará de rojo automáticamente
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return listaResultado
    }

    /**
     * Elimina una obra guardada del almacenamiento de las listas.
     */
    fun eliminarObra(contexto: Context, nombreObra: String) {
        val prefs = contexto.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(nombreObra).apply()
    }
}
