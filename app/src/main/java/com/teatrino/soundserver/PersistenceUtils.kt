package com.teatrino.soundserver

import android.content.Context
import android.net.Uri
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.Serializable

@Serializable
data class ListaObraGuardada (
    val nombreObra: String,
    val sonidos: List<SonidoPersistente>
)

@Serializable
data class SonidoPersistente (
    val id: String,
    val nombreBoton: String,
    val nombreArchivo: String,
    val uriString: String
)

object PersistenceUtils {
    private const val PREFS_NAME = "TeatrinoListasPrefs"
    private const val KEY_ORDEN = "__orden_obras__"

    /**
     * Guarda o actualiza la sesión activa en su propia llave JSON.
     * Si es una obra nueva, la registra en el índice de orden global.
     */
    fun guardarLista(contexto: Context, nombreObra: String, listaSonidos: List<Sonido>) {
        try {
            val prefs = contexto.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            // 1. Serializar y guardar la obra de forma independiente
            val sonidosPersistentes = listaSonidos.map {
                SonidoPersistente(it.id, it.nombreBoton, it.nombreArchivo, it.uriString)
            }
            val obraData = ListaObraGuardada(nombreObra, sonidosPersistentes)
            val jsonString = Json.encodeToString(obraData)

            val editor = prefs.edit()
            editor.putString(nombreObra, jsonString)

            // 2. Actualizar el índice de orden si es una obra nueva
            val ordenActual = obtenerOrdenObras(contexto).toMutableList()
            if (!ordenActual.contains(nombreObra)) {
                ordenActual.add(nombreObra)
                editor.putString(KEY_ORDEN, Json.encodeToString(ordenActual))
            }

            editor.apply()
            println(" [PERSISTENCIA] Obra '$nombreObra' guardada con éxito.")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Devuelve el orden de los nombres desde la llave administrativa.
     */
    fun obtenerOrdenObras(contexto: Context): List<String> {
        return try {
            val prefs = contexto.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonString = prefs.getString(KEY_ORDEN, null) ?: return emptyList()
            Json.decodeFromString<List<String>>(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Sobrescribe el orden global (se usa al presionar subir/bajar en la interfaz).
     */
    fun guardarOrdenObras(contexto: Context, nuevoOrden: List<String>) {
        try {
            val prefs = contexto.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_ORDEN, Json.encodeToString(nuevoOrden)).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Devuelve los nombres de las obras respetando el orden del índice administrativo.
     * Si hay llaves huérfanas por algún motivo, las sincroniza.
     */
    fun obtenerNombresObrasOrdenadas(contexto: Context): List<String> {
        val prefs = contexto.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val todasLasLlaves = prefs.all.keys.toMutableSet()
        todasLasLlaves.remove(KEY_ORDEN) // Excluir la llave administrativa

        val ordenRegistrado = obtenerOrdenObras(contexto)

        // Filtrar el orden para asegurar que las obras aún existan en SharedPreferences
        val resultadoFiltrado = ordenRegistrado.filter { todasLasLlaves.contains(it) }.toMutableList()

        // Si hay llaves reales que no estaban en el índice de orden, las añade al final (seguridad de datos)
        todasLasLlaves.forEach { llave ->
            if (!resultadoFiltrado.contains(llave)) {
                resultadoFiltrado.add(llave)
            }
        }

        // Si el estado cambió, re-guardamos el índice limpio
        if (resultadoFiltrado != ordenRegistrado) {
            guardarOrdenObras(contexto, resultadoFiltrado)
        }

        return resultadoFiltrado
    }

    /**
     * Recupera una lista guardada y verifica en microsegundos si cada archivo sigue existiendo.
     */
    fun cargarLista(contexto: Context, nombreObra: String): List<Sonido> {
        val listaResultado = mutableListOf<Sonido>()
        try {
            val prefs = contexto.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonString = prefs.getString(nombreObra, null) ?: return emptyList()
            val obraData = Json.decodeFromString<ListaObraGuardada>(jsonString)

            for (sp in obraData.sonidos) {
                var archivoExiste = false
                try {
                    contexto.contentResolver.openInputStream(Uri.parse(sp.uriString))?.use {
                        archivoExiste = true
                    }
                } catch (e: Exception) {
                    archivoExiste = false
                }

                listaResultado.add(
                    Sonido(
                        id = sp.id,
                        nombreBoton = sp.nombreBoton,
                        nombreArchivo = sp.nombreArchivo,
                        uriString = sp.uriString,
                        existe = archivoExiste
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return listaResultado
    }

    /**
     * Elimina la obra de su llave individual y la remueve del índice de orden.
     */
    fun eliminarObra(contexto: Context, nombreObra: String) {
        try {
            val prefs = contexto.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()

            editor.remove(nombreObra)

            val ordenActual = obtenerOrdenObras(contexto).toMutableList()
            if (ordenActual.remove(nombreObra)) {
                editor.putString(KEY_ORDEN, Json.encodeToString(ordenActual))
            }

            editor.apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Renombra la llave de la obra y actualiza su posición en el índice.
     */
    fun renombrarObra(contexto: Context, nombreAntiguo: String, nombreNuevo: String) {
        if (nombreAntiguo == nombreNuevo || nombreNuevo.isBlank()) return
        try {
            val prefs = contexto.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonString = prefs.getString(nombreAntiguo, null) ?: return

            val editor = prefs.edit()

            // 1. Guardar los datos exactos bajo la nueva llave
            editor.putString(nombreNuevo, jsonString)
            // 2. Eliminar la llave vieja
            editor.remove(nombreAntiguo)

            // 3. Actualizar el nombre dentro del índice de orden manteniendo la misma posición
            val ordenActual = obtenerOrdenObras(contexto).toMutableList()
            val indice = ordenActual.indexOf(nombreAntiguo)
            if (indice != -1) {
                ordenActual[indice] = nombreNuevo
                editor.putString(KEY_ORDEN, Json.encodeToString(ordenActual))
            }

            editor.apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
