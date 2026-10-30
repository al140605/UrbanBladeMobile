# Seguridad de la documentación

Revisión editorial: 30 de octubre de 2026. Alcance: documentación mantenida de Android y capturas seleccionadas para su README. No incluye cambios de cuentas, secretos, servicios, datos reales ni distribución.

## Material permitido

Usar ejemplos ficticios, hosts de desarrollo y propiedades sin valores reales. La clave publicable de Stripe y un identificador OAuth son configuración cliente; no autorizan a incorporar claves secretas, credenciales de MongoDB o claves privadas de firma.

Mantener fuera de Git `local.properties`, `app/google-services.json`, keystores, contraseñas, exportaciones de usuarios, dumps y sesiones. Antes de añadir archivos, comprobar el diff y las rutas candidatas; no incorporar todo el workspace.

## Capturas y logs

Las dos capturas del README se inspeccionaron visualmente: acceso sin credenciales introducidas y reserva como invitado con servicio sintético. Consultar [procedencia y límites](CAPTURAS.md). No se acredita una revisión de privacidad de todas las imágenes históricas del repositorio.

Revisar notificaciones del sistema, correos, nombres, identificadores, comprobantes, headers Authorization, tokens y enlaces de sesión antes de compartir una captura, Logcat o reporte de fallos. No convertir una captura con datos reales en material de portada por su apariencia visual.

## Verificación y límites

La revisión local de esta renovación utilizó Gitleaks 8.30.1 con reglas predeterminadas, salida redactada y sin añadir excepciones. Los escaneos del directorio realizados durante la renovación y la revisión final previa a preparar el PR dieron cero hallazgos.

No se enviaron archivos a un servicio externo de análisis. El resultado no certifica ausencia absoluta de secretos ni cubre OCR de imágenes, validez de credenciales, configuración de proveedores o pruebas de penetración. La evidencia de este escaneo del directorio no equivale a revisar todo el historial Git.

Si aparece una posible exposición, usar el [canal privado de seguridad](../SECURITY.md), describir ubicación y tipo sin copiar el valor y coordinar revocación o rotación con el responsable. Borrar el valor actual no lo elimina del historial; no reescribir Git ni probar credenciales por iniciativa documental.
