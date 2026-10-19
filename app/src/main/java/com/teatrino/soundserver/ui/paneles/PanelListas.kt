package com.teatrino.soundserver.ui.paneles

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
import com.teatrino.soundserver.TeatrinoServerConfig
import kotlinx.coroutines.launch

@Composable
fun PanelListasEspectaculos(onObraCargada: () -> Unit) {
    val contexto = LocalContext.current
    val obras = MainActivity.listaObrasGuardadas
    val scope = rememberCoroutineScope()

    var mostrarDialogoRenombrarObra by remember { mutableStateOf(false) }
    var obraAEditarNombre by remember { mutableStateOf("") }
    var textoNuevoNombreObra by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Espectáculos Guardados",
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (obras.isEmpty()) {
            Box(modifier = Modifier
                .fillMaxSize()
                .weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = "No hay obras guardadas aún.\nConfigura sonidos y presiona 'Guardar'.",
                    color = MaterialTheme.colorScheme.outline,
                    fontSize = 16.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(obras) { indice, nombreObra ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = {
                                    if (indice > 0) {
                                        obras.removeAt(indice).also { obras.add(indice - 1, it) }
                                        PersistenceUtils.guardarOrdenObras(contexto, obras.toList())
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
                                    if (indice < obras.size - 1) {
                                        obras.removeAt(indice).also { obras.add(indice + 1, it) }
                                        PersistenceUtils.guardarOrdenObras(contexto, obras.toList())
                                    }
                                },
                                enabled = indice < obras.size - 1
                            ) {
                                Text(
                                    "▼",
                                    fontSize = 18.sp,
                                    color = if (indice < obras.size - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        Column(modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)) {
                            Text(
                                text = nombreObra,
                                fontSize = 18.sp,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = {
                                val sonidosCargados =
                                    PersistenceUtils.cargarLista(contexto, nombreObra)
                                MainActivity.listaSonidosSesion.clear()
                                MainActivity.listaSonidosSesion.addAll(sonidosCargados)

                                scope.launch { TeatrinoServerConfig.forzarSincronizacionWeb() }
                                onObraCargada()
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .height(32.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Cargar", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                obraAEditarNombre = nombreObra
                                textoNuevoNombreObra = nombreObra
                                mostrarDialogoRenombrarObra = true
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .height(32.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("Renombrar", fontSize = 11.sp)
                        }

                        IconButton(onClick = {
                            PersistenceUtils.eliminarObra(contexto, nombreObra)
                            obras.removeAt(indice)
                        }) {
                            Text("✕", fontSize = 20.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    if (mostrarDialogoRenombrarObra) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoRenombrarObra = false },
            title = { Text("Renombrar Espectáculo") },
            text = {
                Column {
                    Text(
                        "Introduce el nuevo nombre para este espectáculo:",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    TextField(
                        value = textoNuevoNombreObra,
                        onValueChange = { textoNuevoNombreObra = it },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (textoNuevoNombreObra.isNotBlank() && obraAEditarNombre.isNotBlank()) {
                        val nombreLimpioNuevo = textoNuevoNombreObra.trim()
                        PersistenceUtils.renombrarObra(
                            contexto,
                            obraAEditarNombre,
                            nombreLimpioNuevo
                        )

                        val ordenActualizado =
                            PersistenceUtils.obtenerNombresObrasOrdenadas(contexto)
                        obras.clear()
                        obras.addAll(ordenActualizado)

                        mostrarDialogoRenombrarObra = false
                    }
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    mostrarDialogoRenombrarObra = false
                }) { Text("Cancelar") }
            }
        )
    }
}
