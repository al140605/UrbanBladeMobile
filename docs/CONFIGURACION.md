# Configuración de entornos

Fuente de verdad: `app/build.gradle.kts`, manifiestos por variante y configuración de Gradle. Esta guía documenta el comportamiento existente; no contiene credenciales.

## Propiedades privadas

Las propiedades Gradle `-P` tienen prioridad sobre `local.properties`. Usar el archivo local para evitar que valores sensibles queden en el historial del shell. No añadirlo a Git.

| Propiedad | Finalidad |
|---|---|
| `sdk.dir` | Ruta local del SDK de Android |
| `DEBUG_API_BASE_URL` | API local; emulador por defecto `http://10.0.2.2:8000/api/v1/` |
| `STAGING_API_BASE_URL` | API remota HTTPS; confirmar el entorno efectivo |
| `RELEASE_API_BASE_URL` | API HTTPS de la distribución |
| `GOOGLE_CLIENT_ID` | Cliente web OAuth que verifica la API |
| `STRIPE_PUBLISHABLE_KEY` | Clave publicable correspondiente a la cuenta y modo del servidor |
| `UPLOAD_STORE_FILE` | Ruta al keystore de subida, fuera del repositorio |
| `UPLOAD_STORE_PASSWORD`, `UPLOAD_KEY_ALIAS`, `UPLOAD_KEY_PASSWORD` | Configuración privada de firma |
| `ALLOW_TEST_PAYMENTS` | Excepción explícita para bundles de prueba con Stripe; no equivale a pagos reales |

Los placeholders permiten ciertas compilaciones sin servicios configurados; no constituyen una configuración funcional de release. Nunca incorporar claves secretas Stripe ni credenciales de servidor al APK.

## Backend local y dispositivo

Preparar la API con su propio procedimiento y confirmar el puerto y la base de datos efectiva. El cliente no requiere acceso directo a MongoDB. En emulador, `10.0.2.2` representa el host; ajustar el puerto si la API local no expone 8000.

En dispositivo USB, configurar:

```properties
DEBUG_API_BASE_URL=http://127.0.0.1:8000/api/v1/
```

Y aplicar el reenvío con el ADB del SDK:

```powershell
adb reverse tcp:8000 tcp:8000
```

No cambiar el manifiesto principal para habilitar tráfico HTTP. El permiso de cleartext es específico de debug. Para auditoría aislada, `-PUI_AUDIT=true` añade el sufijo `.audit` a debug y evita aplicar los plugins Firebase. Confirmar variante, URL y datos antes de instalar; no sustituir la app habitual durante una revisión documental.

## Firebase y OAuth

`app/google-services.json` se proporciona fuera de Git. Cuando existe y no está activo `UI_AUDIT`, Gradle aplica Google Services, Crashlytics y Performance. Los identificadores OAuth y certificados de firma deben coincidir con la configuración del proyecto. Compilar sin el archivo no prueba recepción de notificaciones ni autenticación Google.

## Release y distribución

`assembleRelease` comprueba compilación y R8; no demuestra firma, pagos ni disponibilidad en Play. `bundleRelease` tiene guardas adicionales para URL HTTPS, OAuth, Stripe y firma. La excepción `ALLOW_TEST_PAYMENTS=true` permite una clave publicable de prueba válida y debe mantenerse identificada como tal.

La configuración de publicación requiere revisión independiente. Consultar el [procedimiento](PUBLICAR_PLAY_STORE.md) y contrastar cualquier estado histórico con Play Console antes de una entrega.
