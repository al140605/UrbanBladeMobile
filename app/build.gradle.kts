import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
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
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8000/api/v1/\"")
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"$releaseApiBaseUrl\"")
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

    // Login con Google nativo (Credential Manager) -- ver core/auth/GoogleAuthHelper.kt
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

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
