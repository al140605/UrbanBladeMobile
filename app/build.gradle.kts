import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Notificaciones push (T142): el plugin de Google Services solo se aplica si existe
// app/google-services.json (se descarga de la consola de Firebase y no se versiona,
// igual que local.properties). Sin ese archivo la app compila y el push queda apagado.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.isFile) {
        file.inputStream().use(::load)
    }
}

val googleClientId = providers.gradleProperty("GOOGLE_CLIENT_ID").orNull
    ?: localProperties.getProperty("GOOGLE_CLIENT_ID")
    ?: "PENDIENTE_CONFIGURAR.apps.googleusercontent.com"

// Mismo mecanismo que GOOGLE_CLIENT_ID: la URL de producción no se inventa ni se
// versiona -- solo release la necesita, vía local.properties o -P para CI.
val releaseApiBaseUrl = providers.gradleProperty("RELEASE_API_BASE_URL").orNull
    ?: localProperties.getProperty("RELEASE_API_BASE_URL")
    ?: "https://PENDIENTE_CONFIGURAR.example/api/v1/"

// Staging en AWS (CloudFront, HTTPS) detrás del dominio propio. La URL no es un secreto; se puede
// sobreescribir con STAGING_API_BASE_URL (local.properties o -P) si cambia.
val stagingApiBaseUrl = providers.gradleProperty("STAGING_API_BASE_URL").orNull
    ?: localProperties.getProperty("STAGING_API_BASE_URL")
    ?: "https://api.urbanblade.com.mx/api/v1/"
// URL de la API para debug. Por defecto el emulador (10.0.2.2). En un celular fisico
// conectado por USB: DEBUG_API_BASE_URL=http://127.0.0.1:8000/api/v1/ en local.properties
// y ejecutar "adb reverse tcp:8000 tcp:8000" cada vez que conectes el celular.
val debugApiBaseUrl = providers.gradleProperty("DEBUG_API_BASE_URL").orNull
    ?: localProperties.getProperty("DEBUG_API_BASE_URL")
    ?: "http://10.0.2.2:8000/api/v1/"

// Publishable key de Stripe -- es pública por diseño (Stripe la espera embebida
// en apps cliente), pero igual se inyecta sin hardcodear: debe coincidir con la
// misma clave que ya usan barber/frontend-urban, nunca se inventa aquí.
val stripePublishableKey = providers.gradleProperty("STRIPE_PUBLISHABLE_KEY").orNull
    ?: localProperties.getProperty("STRIPE_PUBLISHABLE_KEY")
    ?: "pk_test_PENDIENTE_CONFIGURAR"

android {
    namespace = "com.urbanblade.mobile"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.urbanblade.mobile"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "3.0.0"

        // Credential Manager solicita un ID token para el mismo Web Client ID
        // que Laravel verifica. Se inyecta desde local.properties (ignorado por
        // Git) o -PGOOGLE_CLIENT_ID para CI; nunca se deja en código versionado.
        buildConfigField(
            "String",
            "GOOGLE_CLIENT_ID",
            "\"$googleClientId\""
        )

        // Checkout con Stripe (PaymentSheet) -- ver core/payment/UrbanPaymentSheet.kt.
        // Mismo mecanismo de inyección segura que GOOGLE_CLIENT_ID.
        buildConfigField(
            "String",
            "STRIPE_PUBLISHABLE_KEY",
            "\"$stripePublishableKey\""
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        // API_BASE_URL se define por variante, no en defaultConfig: debug solo habla
        // con el emulador (10.0.2.2, HTTP -- loopback, no es un secreto); release
        // exige HTTPS real, inyectada igual que GOOGLE_CLIENT_ID (nunca hardcodeada).
        debug {
            buildConfigField("String", "API_BASE_URL", "\"$debugApiBaseUrl\"")
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"$releaseApiBaseUrl\"")
        }
        // Build de prueba contra el staging de AWS: se instala igual que debug (mismo
        // applicationId, así el login con Google sigue funcionando), pero habla HTTPS con
        // CloudFront. No usa el manifest de debug, así que NO permite HTTP en claro.
        create("staging") {
            initWith(getByName("debug"))
            buildConfigField("String", "API_BASE_URL", "\"$stagingApiBaseUrl\"")
            matchingFallbacks += listOf("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.02.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.9")
    implementation("androidx.datastore:datastore-preferences:1.1.2")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Pulido visual (2026-09-18). Versiones fijadas a las compatibles con Kotlin 2.1.10,
    // compileSdk 35 y AGP 8.8.2; subirlas exige subir esos tres a la vez.
    implementation("dev.chrisbanes.haze:haze:1.6.10")                       // desenfoque tipo cristal
    implementation("com.valentinilk.shimmer:compose-shimmer:1.3.3")         // esqueletos de carga
    implementation("com.airbnb.android:lottie-compose:6.6.10")              // animaciones

    // Login con Google nativo (Credential Manager) -- ver core/auth/GoogleAuthHelper.kt
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // Checkout con Stripe (PaymentSheet) -- ver core/payment/UrbanPaymentSheet.kt.
    // Fijado a esta versión (no la más reciente): versiones más nuevas jalan
    // transitivos de AndroidX que exigen compileSdk 36 + AGP 8.9.1, y este
    // proyecto está en compileSdk 35 / AGP 8.8.2 -- subir esos dos para una
    // sola dependencia es un cambio de mayor alcance que no correspondía a
    // esta ronda. Revisar si conviene actualizar cuando el proyecto suba de
    // compileSdk por otro motivo.
    implementation("com.stripe:stripe-android:21.19.0")

    // Notificaciones push con Firebase Cloud Messaging (T142) -- ver core/push/.
    // BoM 33.7.0: última línea compatible con compileSdk 35 sin subir AGP.
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-messaging")

    // Pruebas JVM de ViewModels/contrato -- ver app/src/test
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    // Mockito 5.x mockea AuthRepository (clase final) sin necesitar un
    // SessionManager real (que exige un Context de Android/DataStore no
    // disponible en pruebas JVM puras).
    testImplementation("org.mockito:mockito-core:5.14.2")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
}
