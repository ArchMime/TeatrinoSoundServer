package com.teatrino.soundserver.ui.paneles

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teatrino.soundserver.MainActivity
import com.teatrino.soundserver.PersistenceUtils
import com.teatrino.soundserver.Sonido
import com.teatrino.soundserver.TeatrinoServerConfig
import kotlinx.coroutines.launch

@Composable
fun PanelSonidosSesion() {
    val contexto = LocalContext.current
    val sonidos = MainActivity.listaSonidosSesion
    val scope = rememberCoroutineScope()

    var mostrarDialogoEdicion by remember { mutableStateOf(false) }
    var sonidoAEditar by remember { mutableStateOf<Sonido?>(null) }
    var textoNuevoNombreBoton by remember { mutableStateOf("") }

    var mostrarDialogoGuardarObra by remember { mutableStateOf(false) }
    var nombreNuevaObra by remember { mutableStateOf("") }

    val selectorAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { uriSegura ->
            var nombreArchivoReal = "Audio_${sonidos.size + 1}.mp3"
            contexto.contentResolver.query(uriSegura, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    nombreArchivoReal = cursor.getString(nameIndex)
                }
            }
            val nuevoSonido = Sonido(
                nombreBoton = (sonidos.size + 1).toString(),
                nombreArchivo = nombreArchivoReal,
                uriString = uriSegura.toString(),
                existe = true
            )
            sonidos.add(nuevoSonido)
            scope.launch { TeatrinoServerConfig.forzarSincronizacionWeb() }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Sonidos de la Sesión",
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Button(
            onClick = { selectorAudioLauncher.launch("audio/*") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text("🎵 Agregar Sonido desde el Celular")
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(sonidos) { indice, sonido ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (sonido.existe) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.errorContainer)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = {
                                if (indice > 0) {
                                    sonidos.removeAt(indice).also { sonidos.add(indice - 1, it) }
                                    scope.launch { TeatrinoServerConfig.forzarSincronizacionWeb() }
                                }
                            },
                            enabled = indice > 0
                        ) {
                            Text(
                                "▲",
                                fontSize = 18.sp,
                                color = if (indice > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }
                        IconButton(
                            onClick = {
                                if (indice < sonidos.size - 1) {
                                    sonidos.removeAt(indice).also { sonidos.add(indice + 1, it) }
                                    scope.launch { TeatrinoServerConfig.forzarSincronizacionWeb() }
                                }
                            },
                            enabled = indice < sonidos.size - 1
                        ) {
                            Text(
                                "▼",
                                fontSize = 18.sp,
                                color = if (indice < sonidos.size - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    Column(modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)) {
                        Text(
                            text = "Botón: ${sonido.nombreBoton}",
                            fontSize = 18.sp,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (sonido.existe) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = sonido.nombreArchivo,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1
                        )
                    }
                    Button(
                        onClick = {
                            sonidoAEditar = sonido
                            textoNuevoNombreBoton = sonido.nombreBoton
                            mostrarDialogoEdicion = true
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .height(32.dp)
                    ) {
                        Text("Modificar", fontSize = 11.sp)
                    }
                    IconButton(onClick = {
                        sonidos.removeAt(indice)
                        scope.launch { TeatrinoServerConfig.forzarSincronizacionWeb() }
                    }) {
                        Text("✕", fontSize = 20.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        if (sonidos.isNotEmpty()) {
            Button(
                onClick = {
                    nombreNuevaObra = ""
                    mostrarDialogoGuardarObra = true
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text("💾 Guardar Lista de Sonidos")
            }
        }
    }

    if (mostrarDialogoEdicion && sonidoAEditar != null) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoEdicion = false },
            title = { Text("Renombrar Botón") },
            text = {
                Column {
                    Text(
                        "Introduce el identificador para la botonera web:",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    TextField(
                        value = textoNuevoNombreBoton,
                        onValueChange = { textoNuevoNombreBoton = it },
                        singleLine = true,
                        placeholder = { Text("Ej: Trueno, Intro, Fx 2") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    val objetivo = sonidoAEditar
                    if (objetivo != null && textoNuevoNombreBoton.isNotBlank()) {
                        val indice = sonidos.indexOfFirst { it.id == objetivo.id }
                        if (indice != -1) {
                            sonidos[indice] =
                                sonidos[indice].copy(nombreBoton = textoNuevoNombreBoton)
                            scope.launch { TeatrinoServerConfig.forzarSincronizacionWeb() }
                        }
                    }
                    mostrarDialogoEdicion = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    mostrarDialogoEdicion = false
                }) { Text("Cancelar") }
            }
        )
    }

    if (mostrarDialogoGuardarObra) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoGuardarObra = false },
            title = { Text("Guardar Espectáculo") },
            text = {
                Column {
                    Text(
                        "Introduce el nombre de la obra teatral:",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    TextField(
                        value = nombreNuevaObra,
                        onValueChange = { nombreNuevaObra = it },
                        singleLine = true,
                        placeholder = { Text("Ej: Caperucita Roja, Hamlet") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (nombreNuevaObra.isNotBlank()) {
                        PersistenceUtils.guardarLista(contexto, nombreNuevaObra.trim(), sonidos)
                        val ordenActualizado =
                            PersistenceUtils.obtenerNombresObrasOrdenadas(contexto)
                        MainActivity.listaObrasGuardadas.clear()
                        MainActivity.listaObrasGuardadas.addAll(ordenActualizado)
                        mostrarDialogoGuardarObra = false
                    }
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    mostrarDialogoGuardarObra = false
                }) { Text("Cancelar") }
            }
        )
    }
}
