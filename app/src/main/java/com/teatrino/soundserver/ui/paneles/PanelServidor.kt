package com.teatrino.soundserver.ui.paneles

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teatrino.soundserver.generarQrBitmap

@Composable
fun PanelServidor(encendido: Boolean, ip: String, onCambioEstado: (Boolean) -> Unit) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Teatrino Sound Server",
            fontSize = 28.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = if (encendido) "SERVIDOR: ENCENDIDO" else "SERVIDOR: APAGADO",
            fontSize = 20.sp,
            color = if (encendido) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (encendido && ip != "Desconocida") {
            val urlCompleta = "http://$ip:8080"
            Text(
                text = "URL de conexión:\n$urlCompleta",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 16.dp),
                style = MaterialTheme.typography.bodyMedium
            )

            // Generación offline nativa recordada
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
            onCheckedChange = { nuevoEstado ->
                if (nuevoEstado) {
                    // 1. Validar el permiso de batería antes de encender formalmente el interruptor
                    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                    if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
                        val intent =
                            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                        context.startActivity(intent)
                        return@Switch
                    }
                    onCambioEstado(true)
                } else {
                    onCambioEstado(false)
                }
            },
            modifier = Modifier.size(60.dp)
        )
    }
}
