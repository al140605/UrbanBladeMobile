# Decisiones técnicas

Registro de decisiones existentes contrastado con el código el 30 de octubre de 2026. No representa una migración de arquitectura ni cambios de comportamiento.

| Decisión | Motivo | Consecuencia |
|---|---|---|
| Kotlin y Compose, cliente nativo | Mantener navegación e integración Android en el proyecto activo | El antiguo cliente Expo no es la referencia de implementación |
| MVVM dentro del módulo app | Separar representación, estado y acceso remoto | La separación es por paquetes; no se afirma una arquitectura multimódulo |
| Autorización final en Laravel | El cliente puede manipularse | El servidor verifica permisos y reglas aunque Android oculte acciones |
| Token cifrado antes de DataStore | Proteger la persistencia local de la sesión | AES-GCM y Keystore requieren contemplar pérdida o invalidación de claves |
| Configuración por variante | Diferenciar desarrollo local y distribución | Confirmar API y datos efectivos; staging no garantiza aislamiento |
| Wrapper con checksum | Reproducir la versión de Gradle en local y CI | Mantener distribución, JAR y scripts como una unidad al actualizar |
| CodeQL con build explícito | El autobuild anterior agotó el heap de Java | Debug con dos workers y Kotlin en proceso; R8 se comprueba en Android CI |
| Prueba TLS en `/up` sin redirecciones | La raíz de API redirigía al frontend | El chequeo permanece en la API y conserva el rechazo del pin falso |
| Firebase opcional para compilación | Verificar código sin archivos privados | Un build sin configuración no acredita push ni telemetría |

## Cambios futuros

Una decisión nueva debe indicar contexto, alternativas relevantes, solución elegida, consecuencias y evidencia de validación. Mantener decisiones de publicación, configuración de pagos y permisos separadas de una limpieza editorial. La documentación no autoriza modificar servicios o datos reales.
