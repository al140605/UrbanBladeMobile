# Herramientas de desarrollo

## Entorno utilizado

- Android Studio para abrir el proyecto, sincronizar Gradle, inspeccionar Compose y ejecutar en Android.
- SDK de Android 36 y herramientas de plataforma, incluido ADB.
- JetBrains JDK 21 para Gradle; bytecode Java/Kotlin 17.
- Gradle Wrapper versionado para los mismos comandos en local y CI.

Consultar [configuración](CONFIGURACION.md) y [pruebas](PRUEBAS.md). Docker pertenece al entorno del backend y no es requisito para compilar el cliente Android.

## Calidad y diagnóstico

Android Lint, las pruebas JVM, Robolectric, JaCoCo y CodeQL forman parte de la verificación existente. Usar Logcat y el inspector de Android Studio para diagnóstico local; retirar tokens, correos y datos personales antes de compartir logs.

Las recomendaciones antiguas de plugins de diseño no constituyen dependencias del proyecto. No se exige instalar extensiones adicionales ni copiar componentes externos para completar la compilación documentada.
