# Estado verificado de Google Play

Consulta directa del panel de UrbanBlade en Play Console: **5 de noviembre de 2026**. Paquete `com.urbanblade.mobile`. Esta revisión fue de solo lectura y no publicó versiones ni modificó configuración.

| Requisito o estado | Resultado visible |
|---|---|
| Publicar una versión de prueba cerrada | Completado |
| Al menos 12 verificadores inscritos | Completado |
| Ejecutar la prueba cerrada con 12 verificadores durante al menos 14 días | Completado |
| Solicitar acceso a producción | **Botón habilitado** (aún sin enviar) |
| Producción | Inactiva |

Los tres requisitos de la prueba cerrada figuran tachados como cumplidos y el botón «Solicitar acceso a producción» ya está habilitado. Antes, el 30 de octubre, el panel mostraba 8 de 14 días y el botón deshabilitado.

**Qué significa y qué no:** Google habilitó la solicitud; todavía **no** hay autorización de producción ni versión publicada. Falta que el titular de la cuenta abra la solicitud, responda el cuestionario sobre la prueba con evidencia real (qué se probó, qué se corrigió, cómo se reclutó a los verificadores) y la envíe. Después Google la revisa. Esta consulta no envía la solicitud y no acredita aprobación.

**Solicitud enviada (6 de noviembre de 2026, 1:59 a. m.):** el titular autorizó el envío y se contestó el cuestionario de acceso a producción en sus tres pasos (acerca de la prueba cerrada, de la app y del nivel de preparación). El panel muestra «Recibimos tu solicitud de acceso a producción» y estima una revisión de 7 días o menos, con respuesta por correo al propietario de la cuenta. Producción sigue **inactiva**: enviar la solicitud no publica la app. Las respuestas declararon, con datos reales, el reclutamiento por el grupo del curso, 12 verificadores, el uso casi completo de las funciones y una semana más de mejoras antes de lanzar.

Pendiente antes de publicar, según la ficha y las decisiones ya documentadas: sustituir los videos de terceros del muro por trabajos propios, confirmar el aviso de proyecto académico con Stripe en modo prueba y subir el `versionCode` en cada versión nueva.

Los registros anteriores permanecen en la [bitácora histórica](BITACORA_CONSOLA.md). Los datos de testers, cuentas de revisión y credenciales se mantienen fuera del repositorio. La distribución de Play es independiente de los checks y la fusión de un PR documental.
