package com.teatrino.soundserver

import android.content.Intent
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teatrino.soundserver.ui.theme.TeatrinoSoundServerTheme
import kotlinx.coroutines.launch

enum class Pantallas(val titulo: String, val indicador: String) {
    Servidor("Servidor", ""),
    Sonidos("Sonidos", " "),
    Listas("Listas", " "),
    Config("Botones", "")
}

class MainActivity : ComponentActivity() {
    companion object {
        var servidorEncendido by mutableStateOf(false)
        var ipServidor by mutableStateOf("Desconocida")
        val listaSonidosSesion = mutableStateListOf<Sonido>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TeatrinoSoundServerTheme {
                var pantallaActual by remember { mutableStateOf(Pantallas.Servidor) }
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            Pantallas.entries.forEach { pantalla ->
                                NavigationBarItem(
                                    icon = { Text(pantalla.indicador, fontSize = 20.sp) },
                                    label = { Text(pantalla.titulo) },
                                    selected = pantallaActual == pantalla,
                                    onClick = { pantallaActual = pantalla }
                                )
                            }
                        }
                    }
                ) { paddingInterno ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingInterno),
                        contentAlignment = Alignment.Center
                    ) {
                        when (pantallaActual) {
                            Pantallas.Servidor -> PanelServidor(
                                encendido = servidorEncendido,
                                ip = ipServidor,
                                onCambioEstado = { encender ->
                                    servidorEncendido = encender
                                    val intent = Intent(this@MainActivity, TeatrinoService::class.java)
                                    if (encender) startService(intent) else stopService(intent)
                                }
                            )
                            Pantallas.Sonidos -> PanelSonidosSesion()
                            Pantallas.Listas -> Text("Hola Mundo: Panel de Listas (Espectáculos)", fontSize = 24.sp)
                            Pantallas.Config -> Text("Hola Mundo: Panel de Personalización de Botones", fontSize = 24.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PanelServidor(encendido: Boolean, ip: String, onCambioEstado: (Boolean) -> Unit = {}) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Teatrino Sound Server", fontSize = 28.sp, modifier = Modifier.padding(bottom = 16.dp))

        Text(
            text = if (encendido) "SERVIDOR: ENCENDIDO" else "SERVIDOR: APAGADO",
            fontSize = 20.sp,
            color = if (encendido) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (encendido) {
            val urlCompleta = "http://$ip:8080"

            Text(
                text = "URL de conexión:\n$urlCompleta",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 16.dp),
                style = MaterialTheme.typography.bodyMedium
            )

            val qrBitmap = remember(urlCompleta) { generarQrBitmap(urlCompleta, tamaño = 400) }

            qrBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Código QR de conexión",
                    modifier = Modifier
                        .size(200.dp)
                        .padding(bottom = 16.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.height(32.dp))
        }

        Switch(
            checked = encendido,
            onCheckedChange = onCambioEstado,
            modifier = Modifier.size(60.dp)
        )
    }
}

@Composable
fun PanelSonidosSesion() {
    val contexto = LocalContext.current
    val sonidos = MainActivity.listaSonidosSesion
    val scope = rememberCoroutineScope()

    var mostrarDialogo by remember { mutableStateOf(false) }
    var sonidoAEditar by remember { mutableStateOf<Sonido?>(null) }
    var textoNuevoNombre by remember { mutableStateOf("") }

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

            scope.launch {
                TeatrinoServerConfig.forzarSincronizacionWeb()
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Sonidos de la Sesión", fontSize = 24.sp, modifier = Modifier.padding(bottom = 16.dp))

        Button(
            onClick = { selectorAudioLauncher.launch("audio/*") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Text("➕ Agregar Sonido desde el Celular")
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(sonidos) { indice, sonido ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (sonido.existe) MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.errorContainer
                        )
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Controles para alterar el orden de la botonera
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
                            Text("▲", fontSize = 18.sp, color = if (indice > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
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
                            Text("▼", fontSize = 18.sp, color = if (indice < sonidos.size - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                        }
                    }

                    // 2. Despliegue de los nombres informativos
                    Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
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
// 3. Botón Modificar para abrir el Popup
                    Button(
                        onClick = {
                            sonidoAEditar = sonido
                            textoNuevoNombre = sonido.nombreBoton
                            mostrarDialogo = true
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.padding(end = 4.dp).height(32.dp)
                    ) {
                        Text("Modificar", fontSize = 11.sp)
                    }
// 4. Botón Eliminar
                    IconButton(onClick = {
                        sonidos.removeAt(indice)
                        scope.launch {
                            TeatrinoServerConfig.forzarSincronizacionWeb()
                        }
                    }) {
                        Text("✕", fontSize = 20.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        if (sonidos.isNotEmpty()) {
            Button(
                onClick = { /* TODO: Guardar lista de la obra persistente */ },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                Text("💾 Guardar Lista de Sonidos")
            }
        }
    }
// VENTANA MODAL EMERGENTE (DIÁLOGO DE EDICIÓN)
    if (mostrarDialogo && sonidoAEditar != null) {
        AlertDialog(
            onDismissRequest = { mostrarDialogo = false },
            title = { Text("Renombrar Botón") },
            text = {
                Column {
                    Text("Introduce el nombre identificador para la botonera web:", fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                    TextField(
                        value = textoNuevoNombre,
                        onValueChange = { textoNuevoNombre = it },
                        singleLine = true,
                        placeholder = { Text("Ej: Trueno, Intro, Fx 2") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val objetivo = sonidoAEditar
                        if (objetivo != null && textoNuevoNombre.isNotBlank()) {
                            val indice = sonidos.indexOfFirst { it.id == objetivo.id }
                            if (indice != -1) {
                                sonidos[indice] = sonidos[indice].copy(nombreBoton = textoNuevoNombre)
                                scope.launch {
                                    TeatrinoServerConfig.forzarSincronizacionWeb()
                                }
                            }
                        }
                        mostrarDialogo = false
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogo = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
