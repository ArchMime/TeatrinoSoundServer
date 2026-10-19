let socket = null;
let reconexiónTimer = null;
let sentinelWakeLock = null;

// --- 1. MOTOR DE COMUNICACIONES INALÁMBRICAS (WEBSOCKETS) ---
function conectarServidor() {
    const hostLocal = window.location.host;
    const indicadorVisual = document.getElementById("txtEstado");

    socket = new WebSocket("ws://" + hostLocal + "/socket");

    socket.onopen = function() {
        console.log("[RED] Canal de audio abierto con el celular.");
        indicadorVisual.textContent = "● EN LÍNEA";
        indicadorVisual.className = "estado-conexion conectado";

        if (reconexiónTimer) {
            clearTimeout(reconexiónTimer);
            reconexiónTimer = null;
        }
    };

    socket.onmessage = function(evento) {
        try {
            const paquete = JSON.parse(evento.data);
            // Captura la estructura RespuestaBotonera enviada desde la app
            if (paquete.sonidos) {
                dibujarBotonera(paquete.sonidos);
            }
        } catch (err) {
            console.error("Fallo al decodificar ráfaga JSON:", err);
        }
    };

    socket.onclose = function() {
        console.warn("[RED] Conexión perdida. Activando protocolo de rescate...");
        indicadorVisual.textContent = "○ DESCONECTADO (Reintentando...)";
        indicadorVisual.className = "estado-conexion";

        limpiarBotoneraVisual();
        forzarReconexion();
    };

    socket.onerror = function() {
        socket.close(); // Forzamos gatillo unificado de cierre
    };
}

function forzarReconexion() {
    if (reconexiónTimer) return;
    reconexiónTimer = setTimeout(() => {
        reconexiónTimer = null;
        conectarServidor();
    }, 2000); // Reintenta sin descanso cada 2 segundos
}

function enviarComando(accion, idSonido, valor = "") {
    if (socket && socket.readyState === WebSocket.OPEN) {
        const comando = JSON.stringify({
            accion: accion,
                    id: idSonido,
                    valor: valor
        });
        socket.send(comando);
    }
}

function transmitirVolumen(volumenDecimal) {
    enviarComando("VOLUME", "0", volumenDecimal);
}

// --- 2. RENDERIZADO REACTIVO DE LA GRID ---
function dibujarBotonera(listaSonidos) {
    const grid = document.getElementById("contenedorGrid");
    grid.innerHTML = ""; // Limpieza de seguridad

    listaSonidos.forEach(sonido => {
        const tarjeta = document.createElement("div");
        tarjeta.className = "tarjeta-sonido";
        tarjeta.style.cursor = "pointer";

        // Evento asíncrono atado al ID del sonido nativo
        tarjeta.onclick = () => enviarComando("PLAY", sonido.id);

        const txtNombre = document.createElement("div");
        txtNombre.className = "nombre-boton";
        txtNombre.textContent = sonido.nombreBoton;

        const txtArchivo = document.createElement("div");
        txtArchivo.className = "nombre-archivo";
        txtArchivo.textContent = sonido.nombreArchivo;

        tarjeta.appendChild(txtNombre);
        tarjeta.appendChild(txtArchivo);
        grid.appendChild(tarjeta);
    });
}

function limpiarBotoneraVisual() {
    document.getElementById("contenedorGrid").innerHTML = "";
}

// --- 3. BLINDAJE FISICO DE LA TABLET (FULLSCREEN & KEEP-AWAKE) ---
async function engranarModoFuncion() {
    // A. Desplegar Pantalla Completa Pura
    try {
        const raiz = document.documentElement;
        if (raiz.requestFullscreen) {
            await raiz.requestFullscreen();
        } else if (raiz.webkitRequestFullscreen) { /* iOS/Safari */
            await raiz.webkitRequestFullscreen();
        }
    } catch (err) {
        console.warn("Fallo al encajar Fullscreen:", err);
    }

    // B. Secuestrar el modo reposo de la pantalla
    await adquirirFrenoSuspenso();

    // Ocultar el botón para limpiar la consola de control
    document.getElementById("btnModoFuncion").style.display = "none";
}

async function adquirirFrenoSuspenso() {
    if ('wakeLock' in navigator) {
        try {
            sentinelWakeLock = await navigator.wakeLock.request('screen');
            console.log("[WAKE-LOCK] Candado adquirido. Pantalla despierta indefinidamente.");

            sentinelWakeLock.addEventListener('release', () => {
                console.log("[WAKE-LOCK] Candado liberado por el sistema.");
                sentinelWakeLock = null;
            });
        } catch (err) {
            console.error("Error al inyectar WakeLock:", err.message);
        }
    } else {
        console.warn("Navegador antiguo sin soporte WakeLock API.");
    }
}

// Si el operador minimiza por error y vuelve, re-enganchamos el candado de luz
document.addEventListener('visibilitychange', async () => {
    if (sentinelWakeLock !== null && document.visibilityState === 'visible') {
        await adquirirFrenoSuspenso();
    }
});

// Lanzamiento del ciclo de vida del cliente al cargar
window.addEventListener("DOMContentLoaded", conectarServidor);
