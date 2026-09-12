plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.urbanblade.mobile"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.urbanblade.mobile"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "3.0.0"

        buildConfigField(
            "String",
            "API_BASE_URL",
            "\"http://10.0.2.2:8000/api/v1/\""
        )

        // Mismo Web Client ID que ya usa barber (services.google.client_id,
        // ver SocialAuthController) -- Credential Manager lo usa como
        // "serverClientId" para pedir un ID token que el backend pueda
        // verificar contra esa misma audiencia. Placeholder a propósito:
        // reemplazar con el valor real (termina en .apps.googleusercontent.com)
        // antes de probar el login con Google.
        buildConfigField(
            "String",
            "GOOGLE_CLIENT_ID",
            "\"PENDIENTE_CONFIGURAR.apps.googleusercontent.com\""
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
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
}
