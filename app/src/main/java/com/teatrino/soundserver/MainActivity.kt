package com.teatrino.soundserver

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.teatrino.soundserver.ui.paneles.PanelListasEspectaculos
import com.teatrino.soundserver.ui.paneles.PanelServidor
import com.teatrino.soundserver.ui.paneles.PanelSonidosSesion
import com.teatrino.soundserver.ui.theme.TeatrinoSoundServerTheme

enum class Pantallas(val titulo: String, val indicador: String) {
    Servidor("Servidor", ""),
    Sonidos("Sonidos", ""),
    Listas("Listas", ""),
    Config("Botones", "")
}

class MainActivity : ComponentActivity() {
    companion object {
        var servidorEncendido by mutableStateOf(false)
        var ipServidor by mutableStateOf("Desconocida")
        val listaSonidosSesion = mutableStateListOf<Sonido>()
        val listaObrasGuardadas = mutableStateListOf<String>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Cargar índice desde SharedPreferences al iniciar la app
        val ordenInicial = PersistenceUtils.obtenerNombresObrasOrdenadas(this)
        listaObrasGuardadas.clear()
        listaObrasGuardadas.addAll(ordenInicial)

        setContent {
            TeatrinoSoundServerTheme {
                var pantallaActual by remember { mutableStateOf(Pantallas.Servidor) }

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            Pantallas.entries.forEach { pantalla ->
                                NavigationBarItem(
                                    icon = { Text(pantalla.indicador, fontSize = 24.sp) },
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
                                    val intent =
                                        Intent(this@MainActivity, TeatrinoService::class.java)
                                    if (encender) startService(intent) else stopService(intent)
                                }
                            )

                            Pantallas.Sonidos -> PanelSonidosSesion()
                            Pantallas.Listas -> PanelListasEspectaculos(
                                onObraCargada = {
                                    pantallaActual = Pantallas.Sonidos
                                } // Auto-navegación reactiva
                            )

                            Pantallas.Config -> Text(
                                "Hola Mundo: Panel de Personalización de Botones",
                                fontSize = 24.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
