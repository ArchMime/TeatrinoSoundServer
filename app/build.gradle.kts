plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    kotlin("plugin.serialization") version "2.0.21" // O la versión de Kotlin que use tu proyecto

}

android {
    namespace = "com.teatrino.soundserver"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.teatrino.soundserver"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    // Versiones modernas y compatibles con tu entorno nativo
    implementation("io.ktor:ktor-server-core-jvm:3.0.0")
    implementation("io.ktor:ktor-server-cio-jvm:3.0.0")
    implementation("io.ktor:ktor-server-websockets-jvm:3.0.0")
    implementation("io.github.g00fy2.quickie:quickie-bundled:1.10.0") // Si quieres escáner a futuro, pero para generar:
    implementation("com.lightspark:compose-qr-code:1.0.1")
// Librería principal de Google para cálculo de matrices de códigos de barra y QR
    implementation("com.google.zxing:core:3.5.3")
// Soporte para serialización JSON en Ktor 3.x
    // Ruta oficial y corregida para serialización JSON en Ktor 3.x
    // Rutas oficiales unificadas con tu ecosistema Ktor JVM 3.x
    implementation("io.ktor:ktor-server-content-negotiation-jvm:3.0.0")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:3.0.0")
    // Soporte oficial de Ktor para servir archivos estáticos (HTML, JS, CSS)
    implementation("io.ktor:ktor-server-html-builder-jvm:3.0.0")

}