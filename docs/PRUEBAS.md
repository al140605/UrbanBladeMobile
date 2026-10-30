# Pruebas y calidad

## Reproducción local

Requisitos: SDK 36, JetBrains JDK 21 y wrapper versionado. Ejecutar desde la raíz del checkout:

```powershell
.\gradlew.bat :app:lintDebug :app:testDebugUnitTest :app:createDebugUnitTestCoverageReport :app:assembleDebug --console=plain
.\gradlew.bat :app:assembleRelease -PRELEASE_API_BASE_URL=https://api.urbanblade.com.mx/api/v1/ --console=plain
```

En Linux y macOS sustituir el ejecutable por `./gradlew`. El segundo comando compila release con R8; no publica ni certifica configuración de servicios o firma.

## Qué se comprueba

| Comprobación | Alcance y límites |
|---|---|
| JVM | Lógica, modelos y componentes; algunos usan Robolectric y recursos Android |
| TLS controlado | Casos de certificados locales y rechazo de pin incorrecto |
| TLS público | API `/up`, respuesta 200, handshake y host esperado sin seguir redirecciones; requiere red |
| Android Lint | Análisis estático de la variante debug |
| JaCoCo | Reporte de cobertura; CI no impone un umbral mínimo |
| Debug y release/R8 | Compilación y reducción; no prueban un recorrido funcional completo |
| CodeQL | Análisis Java/Kotlin con build debug explícito e instrumentado en GitHub |

Los reportes JVM quedan en `app/build/reports/tests/testDebugUnitTest/`; lint en `app/build/reports/lint-results-debug.html`; cobertura en `app/build/reports/coverage/test/debug/`.

No desactivar pinning, saltar pruebas o sustituir resultados ante un timeout de red. Diagnosticar conectividad y repetir el caso sin debilitar la validación.

## Evidencia de referencia

Mantenimiento fusionado el 30 de octubre de 2026, commit `603879cdadea34cbae027e46ada63358ac889a8a`:

- Validación local: 369 pruebas aprobadas, cero errores y cero omisiones.
- [Android CI en main](https://github.com/al140605/UrbanBladeMobile/actions/runs/41016321324): lint, pruebas, control de omisiones, cobertura, debug y release/R8 aprobados.
- [CodeQL en main](https://github.com/al140605/UrbanBladeMobile/actions/runs/41016321297): aprobado.

Estos resultados corresponden a ese commit. No acreditan cambios posteriores, instalación en dispositivo ni distribución en Play.

## Revisión funcional

Usar [verificación manual](VERIFICACION_MANUAL_P0.md) y [contrato de experiencia](UI_UX_CONTRATO_MOVIL.md). Registrar commit, variante, API efectiva, rol, dispositivo, datos de prueba y resultado. Comprobar estados vacíos, errores, reconexión, permisos y accesibilidad. Las capturas históricas ilustran la interfaz, no reemplazan esa ejecución.

## Criterio de entrega

Revisar el diff y secretos, ejecutar las comprobaciones acordes al cambio y abrir PR desde una rama específica. Confirmar checks del SHA revisado antes de fusionar, conservar la rama y verificar las ejecuciones del commit resultante en main. La protección automática de la rama depende de la configuración que aplique el propietario; seguir este procedimiento aunque no esté habilitada.

Para cambios exclusivamente documentales: revisar enlaces, imágenes, referencias, formato y contenido sensible. No generar firmas, instalar APK ni publicar como parte de esa validación.
