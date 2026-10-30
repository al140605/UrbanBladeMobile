# Integración continua y distribución

Revisión documental: 30 de octubre de 2026. Fuentes: [Android CI](../.github/workflows/android-ci.yml) y [CodeQL](../.github/workflows/codeql.yml).

## Workflows existentes

| Workflow | Disparadores | Controles |
|---|---|---|
| Android CI | Pull request a main, push a main y ejecución manual | Wrapper, lint, pruebas JVM, cero omisiones, JaCoCo, debug y release/R8 |
| CodeQL Advanced | Pull request a main, push a main y calendario semanal | Análisis Java/Kotlin con compilación debug explícita |

Ambos usan Ubuntu 24.04, JetBrains JDK 21 y Actions fijadas a commits completos. El checkout no conserva credenciales Git. El wrapper fija Gradle 9.6.0 y verifica el checksum de la distribución.

Android CI tiene un límite de 40 minutos y cancela ejecuciones anteriores del mismo grupo de referencia. CodeQL tiene un límite de 35 minutos; compila sin build cache, con dos workers y Kotlin en el proceso de Gradle. No deducir un resultado de CodeQL a partir de un build local sin instrumentación.

## Artefactos y dependencias

Android CI publica `reportes-android` con lint, resultados de pruebas y cobertura durante 14 días. El paso usa `always()`: la presencia de un artefacto no implica que el workflow haya pasado. JaCoCo informa cobertura sin imponer umbral mínimo.

La suite no requiere credenciales de firma ni Firebase. Su caso TLS público sí requiere conectividad a la API. El build release configura la URL pública para compilar con R8; no ejecuta una prueba funcional de producción.

## Flujo de cambios

1. Trabajar en una rama específica y preservar cambios ajenos.
2. Revisar diff, documentación, imágenes y secretos; ejecutar las comprobaciones pertinentes.
3. Abrir un PR a main con alcance, motivo, evidencia y límites.
4. Confirmar los checks del SHA exacto; si cambia el código o la base, reevaluar la validación.
5. Fusionar mediante merge commit cuando los checks estén en verde.
6. Comprobar las ejecuciones del commit resultante en main y conservar la rama.

La revisión previa encontró main sin protección y la cuenta de trabajo sin permisos administrativos. Esta guía describe el flujo acordado, no afirma que GitHub lo imponga. El propietario debe configurar PR obligatorio, checks, conversaciones resueltas y rechazo de push forzado o borrado. La documentación no cambia esos permisos.

## Distribución

Estos workflows no instalan APK, generan una publicación firmada ni suben versiones a Google Play. Compilar, analizar, fusionar y distribuir son acciones diferentes.

Antes de una publicación autorizada, seguir [publicación](PUBLICAR_PLAY_STORE.md), verificar el entorno y conservar acceso al artefacto anterior. Revisar también cualquier servicio externo conectado al repositorio: la ausencia de un paso de despliegue en YAML no permite asegurar que un push nunca active integraciones externas.
