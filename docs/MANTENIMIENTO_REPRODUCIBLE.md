# Mantenimiento reproducible de Android

El proyecto activo es Android nativo: Kotlin, Jetpack Compose y API Laravel.
Este mantenimiento conserva las versiones del build: AGP 9.4.1, Kotlin 2.2.10,
Gradle 9.6.0 y JDK JetBrains 21 para Gradle; bytecode Java/Kotlin 17 y SDK 36.
No confundirlo con el antiguo cliente Expo.

## Primera entrega

- Gradle Wrapper completo, distribución y JAR cotejados con checksums oficiales.
- CI usa el wrapper, valida sus JAR y fija Actions por commit completo.
- Runner Ubuntu 24.04 explícito y token de checkout sin persistencia.
- Se retira el workflow de ejemplo de saludo; Android CI ya admite ejecución manual.
- Se conservan lint, suite JVM, control de pruebas omitidas, cobertura y build R8.
- Se recuperan la licencia GPL-3.0-only y las políticas ya preparadas en la rama docs/licencia-gpl3; no habían llegado a main.

No se actualizan librerías de la app sin una validación separada de compatibilidad.
No se incluyen google-services.json, local.properties, keystores ni credenciales.

## Validación local

Ejecutar los comandos del README desde el checkout que se está revisando. Mantener
separados los resultados de unit tests, lint, compilación debug y comprobación TLS
en vivo. El build no implica instalar el APK, publicar un AAB o actualizar Play.
No ejecutar scripts de creación de keystore o publicación como preparación del entorno.

En una prueba contra la API local de Barber, usar una variante de auditoría separada
(UI_AUDIT=true) y comprobar su URL efectiva antes de instalar. No sobreescribir la
sesión de la app habitual ni utilizar cuentas de producción para una prueba sintética.

## Siguientes entregas

1. Revisar la paridad funcional por rol; el [contraste inicial de rutas](AUDITORIA_CONTRATOS_API.md) no encontró rutas cliente ausentes tras revisar el POST multipart de inventario.
2. Revisar e integrar por separado los cambios de interfaz que ya existen en el checkout original.
3. Evaluar dependencias y documentación por grupos, con pruebas y lint antes de subir.
4. Verificar la app en emulador o dispositivo contra la API local aislada antes de una entrega.

La firma de publicación y las acciones de Play Console requieren autorización
específica; este mantenimiento no publica ni despliega la app.

## Certificado de la API

La comprobación en vivo usa /up y no sigue redirecciones. La raíz / redirige al
frontend y acoplaba una prueba del certificado de la API a la disponibilidad de
otro servicio. Se conservan los pins de producción, la validación TLS y el rechazo
del pin falso; la respuesta correcta debe ser 200 y permanecer en el host de la API.
