# Política de seguridad

Esta política cubre la aplicación Android de UrbanBlade.
Los reportes de vulnerabilidades se comunican en privado, no en issues públicos.

## Reportar una vulnerabilidad

Utiliza el [canal privado del equipo UrbanBlade](https://github.com/KikeGonRam/barber/security/advisories/new)
e indica que el componente afectado es `UrbanBladeMobile`.
Se utiliza este canal compartido sin asumir que el repositorio UrbanBladeMobile
tenga habilitado su propio formulario privado.

Incluye el commit o versión afectada, entorno, impacto estimado, pasos mínimos con
datos ficticios y evidencia depurada. Si afecta a varios componentes, descríbelos
en un solo reporte. No publiques exploits, tokens o datos personales.

## Alcance de mantenimiento

Las correcciones se proponen por PR contra `main`.
Identifica el SHA evaluado: una rama local, un resultado de CI y una versión
instalada o distribuida por Google Play
son evidencias diferentes. No se promete un plazo de respuesta, una recompensa
o soporte por versión que el equipo no haya acordado.

El equipo revisará el reporte y coordinará la reproducción, mitigación y divulgación
por el canal privado. Un reporte no autoriza pruebas sobre producción, acceso a
cuentas ajenas, pagos reales, ingeniería social o modificaciones de datos.

## Datos del cliente y firma de la app

- No publicar tokens Bearer, headers Authorization, sesiones, conversaciones,
  datos personales ni comprobantes en logs, capturas o reportes de fallos.
- Mantener fuera de Git local.properties, credenciales de servicios y material
  de firma: keystores, claves privadas y sus contraseñas.
- Las claves secretas de Stripe y las credenciales de AWS/Atlas pertenecen a los
  servicios responsables; no se incorporan al APK.
- La configuración de cliente y los identificadores públicos no sustituyen
  la autorización y las reglas del backend.
- Depurar logcat, informes de fallos y grabaciones antes de compartirlos.
  No usar cuentas reales en fixtures o demostraciones públicas.

## Cambios y pruebas

Laravel conserva la autoridad sobre rol, pertenencia, precios, stock y estados.
Ocultar un botón en Android no constituye una barrera de autorización.

Las pruebas deben usar un entorno aislado y datos ficticios.
Una tarea documental no requiere instalar un APK, probar pagos reales, cambiar
credenciales de firma ni publicar en Play Console.
Un build local o de CI no acredita distribución ni una instalación en usuarios.
Consultar [arquitectura](docs/ARQUITECTURA.md) y el [README](README.md)
para conocer los comandos y límites del proyecto.

## Credenciales expuestas y dependencias

Comunica en privado ubicación y tipo, sin copiar el valor ni probar una credencial
ajena contra el proveedor. El responsable coordinará revocación o rotación y revisión
del uso. Eliminar el archivo actual no elimina su contenido del historial.
No reescribir Git, cambiar cuentas o desactivar controles de forma automática.

Los escáneres y resultados de CI son controles complementarios; no certifican
ausencia de vulnerabilidades. Los componentes de terceros conservan sus avisos y
licencias; los hallazgos se evalúan por alcance e impacto.

## Otras políticas

[Contribución](CONTRIBUTING.md) · [Código de conducta](CODE_OF_CONDUCT.md) ·
[Licencia](LICENSE) · [Alcance de la licencia](LICENSING.md).
