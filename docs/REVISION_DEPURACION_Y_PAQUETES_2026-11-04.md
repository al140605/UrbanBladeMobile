# Revisión de depuración y paquetes Android

TT32 / HT-12. Rama `feat/mejoras-interfaz-mobile`. Revisión de código propio,
configuración, dependencias de ejecución, archivos versionables e historial y APKs
locales. Sin PR, fusión a main, distribución ni cambios en producción.

## Hallazgos corregidos

| Hallazgo | Corrección |
| --- | --- |
| El cliente HTTP registraba URLs con nivel BASIC en debug | Se retiraron el interceptor y su dependencia en todas las variantes |
| Staging heredaba la capacidad de depuración de debug | Staging y release declaran `isDebuggable = false`; debug conserva las pruebas locales |
| El bloqueo de HTTP de las variantes distribuibles dependía del valor por defecto | Manifiesto principal con `usesCleartextTraffic=false`; excepción solo en el manifiesto debug |
| Una excepción inesperada podía mostrar su mensaje interno en cualquier pantalla | Mensaje de interfaz de cada operación; se mantienen los errores de negocio HTTP tratados explícitamente |
| Los fallos del SDK podían mostrar detalles internos | Mensajes de pago claros sin concatenar la excepción; la reserva existente conserva su recuperación |
| Los logs locales se ignoraban por una regla global del equipo | Exclusiones dentro del repositorio para logs, volcados y material privado/de firma |
| La rama no contenía el escaneo de secretos publicado | Se integró desde origin/main el commit `095f168`, sin modificar main |

No se encontraron llamadas manuales a `Log.*`, `println`, `printStackTrace`,
`StrictMode` o depuración de WebView en el código de aplicación revisado.
Release usa R8 para retirar llamadas verbose, debug e info a Logcat; conserva
warnings y errores operativos. Las dependencias de terceros pueden producir sus
propios mensajes: esto no afirma que todo Logcat del dispositivo esté vacío.

## Controles automáticos

- `verifyDistributionDependencies`: rechaza herramientas de pruebas e inspección
  en staging/release (logging-interceptor, MockWebServer, tooling interactivo,
  JUnit, Mockito, Robolectric, Flipper, Stetho, Chucker y LeakCanary).
- `scripts/security/check_android_package.py`: revisa el manifiesto empaquetado,
  BuildConfig, endpoint HTTPS y los APK declarados en los metadatos de salida.
  Rechaza depuración, testOnly, perfilado shell, permisos diagnósticos restringidos,
  archivos privados y patrones de secretos. No imprime valores detectados.
- Android CI compila staging y release/R8 y ejecuta ambos controles, además de
  lint y pruebas existentes. El workflow de Gitleaks conserva el historial completo.

```powershell
.\gradlew.bat :app:assembleDebug :app:assembleStaging :app:assembleRelease -PUI_AUDIT=true -PRELEASE_API_BASE_URL=https://api.urbanblade.com.mx/api/v1/
python -m unittest discover -s scripts/security -p 'test_*.py'
.\gradlew.bat :app:verifyDistributionDependencies
python scripts/security/check_android_package.py --variant staging
python scripts/security/check_android_package.py --variant release
```

Compilar las variantes antes de revisar sus paquetes. La guarda de publicación
`bundleRelease` para entorno, Firebase y firma continúa siendo un control distinto.

## Evidencia local

- 435 pruebas Android: cero fallos, errores u omitidas.
- Siete pruebas del verificador: paquetes válidos, flags inseguros, permiso de
  logs, archivos privados, secretos entre bloques y endpoint no configurado.
- Lint, debug, staging y release/R8: correctos. Los builds de verificación usaron
  `UI_AUDIT=true`; no subieron mappings ni habilitaron plugins Firebase.
- Classpaths de staging/release: sin las herramientas de pruebas o inspección
  bloqueadas. El SDK y la instrumentación reales deben comprobarse además antes
  de una distribución autorizada.
- Ambos APK pasaron el verificador. `aapt2` confirmó directamente en sus
  manifiestos binarios: no depurables y sin tráfico HTTP permitido.
- Gitleaks 8.30.1 oficial con SHA-256 verificado: cero hallazgos en el historial
  y en una copia de los archivos versionables, incluidos los cambios nuevos.

El teléfono se desconectó durante la compilación. La instalación de esta APK y
la comprobación nueva de Logcat no se completaron; no se confunden con la revisión
física de los cinco roles del incremento anterior. Los reportes permanecen fuera
de Git y no contienen valores expuestos en la documentación.

## Límites y referencias

Los escaneos por patrones complementan la revisión; no certifican ausencia de toda
credencial o vulnerabilidad. Crashlytics, métricas y bitácora de negocio tienen un
propósito operativo distinto de las trazas de desarrollo; no se eliminan por el
nombre de una clase o por contener la palabra debug. No se borraron archivos,
reescribió historial ni rotaron credenciales.

Criterios de Android: [aplicaciones depurables](https://developer.android.com/privacy-and-security/risks/android-debuggable)
y [exposición de información en logs](https://developer.android.com/privacy-and-security/risks/log-info-disclosure).
