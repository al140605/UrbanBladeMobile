# QA Android con base local poblada

Fecha: 30 de octubre de 2026. App revisada: commit `395c94c`, rama `feat/mejoras-interfaz-mobile`, paquete `com.urbanblade.mobile.audit`, Samsung SM-S731B. Sin PR, fusión, despliegue ni publicación en Google Play.

## Entorno y datos

Se comprobó la configuración efectiva de `ub-demo-api`: entorno local, host Mongo `mongo-dev` y base `urbanblade_dev`. La API está expuesta en loopback del equipo, puerto 8001, y el teléfono accede por reenvío ADB USB. La app habitual de Play no se sustituyó.

Se guardó un respaldo local antes de la carga. Se conservaron las cuentas, contraseñas y registros anteriores. Los fixtures adicionales usan identificadores QA y usuarios de dominio reservado `example.test`; la generación se ejecutó sin eventos de modelos ni llamadas a proveedores de pago o notificación. Son datos sintéticos para interfaz, no transacciones reales ni una restauración de producción.

| Entidad | Antes | Después |
|---|---:|---:|
| Usuarios | 14 | 136 |
| Clientes | 8 | 128 |
| Barberos | 2 | 3 |
| Citas | 19 | 944 |
| Registros de pago | 14 | 581 |
| Productos | 4 | 36 |
| Pedidos | 0 | 90 |
| Trabajos de portafolio | 0 | 24 |
| Comentarios QA | 0 | 72 |
| Reacciones QA | 0 | 144 |

Las citas incluyen historia, próximas visitas, pendientes, confirmadas, canceladas y no asistencias; los productos incluyen existencias normales, bajas y agotadas. Los pagos sembrados son registros ficticios en efectivo: no representan dinero cobrado. Se ajustaron solamente fixtures que coincidían con citas previas, conservando los registros originales. Revisión final: cero solapamientos de horario, cero duplicados activos de cliente por día y ninguna referencia rota en la carga revisada.

## Recorridos observados en el teléfono

| Rol / caso | Resultado |
|---|---|
| Cliente: acceso e inicio | Inicio con próxima visita, identidad y avisos con datos locales |
| Mis citas: próximas e historial | Tarjetas, importes y estados visibles; listas desplazables |
| Historial: cargar más | La segunda página se añadió conservando la posición de lectura |
| Cuenta y pedidos | Acceso a la lista de pedidos del cliente y sus controles |
| Barbero: inicio | Accesos de trabajo y gráficas con datos |
| Agenda de barbero | 4 citas del día y 24 de la semana; cambio Día/Semana correcto |
| Portafolio | 8 trabajos por barbero, imágenes ilustrativas locales cargadas |
| Horario | Seis días activos, entrada y salida visibles; no se guardaron cambios |
| Administración | Inicio, inventario de 36 productos y lista de 128 clientes cargados |
| Búsqueda de cliente | Un nombre largo se encontró por su identificador de prueba; lista legible |
| Reserva en efectivo | Catálogo, barbero, horario, extras opcionales, revisión y confirmación completados |
| Dos toques de confirmación | Una sola cita nueva verificada en Mongo; ningún pago creado |
| Regreso a Mis citas | La nueva cita pendiente del día a las 12:00 aparece en pantalla |

Se probaron cuentas QA de cliente, barbero y administrador. Las credenciales y tokens quedaron en archivos privados fuera del repositorio y no se incorporan al informe.

## Comprobaciones HTTP

20 solicitudes de lectura contra la API local con tres roles: 18 respuestas 200 y dos 403 esperadas (cliente en horario de barbero; barbero en listado de clientes). Sin respuestas 500. Se comprobó paginación de citas, pedidos, clientes e inventario.

Los tiempos de esa muestra estuvieron aproximadamente entre 265 y 1050 ms en el equipo de desarrollo; no son una prueba de carga ni una garantía de rendimiento. La prueba del teléfono se distingue de esas lecturas HTTP. No se repitieron las pruebas JVM o CI porque esta sesión de QA no modificó código de aplicación.

## Incidencias del entorno resueltas

- El reenvío USB había desaparecido. La app mostró el estado sin conexión; al restablecer el puerto 8001 y reintentar recuperó la sesión y los datos.
- El servidor local de API devolvía HTML para una ruta de archivo de portafolio. Las imágenes ficticias se sirven ahora desde un directorio local dedicado, puerto 18766, reenviado al teléfono por USB. Solo contiene una imagen ilustrativa reutilizada en los fixtures; no expone credenciales ni respaldos. No se cambió el almacenamiento de producción.
- La coherencia de fecha se comprobó sobre fechas normalizadas del modelo Mongo; una comparación directa contra un string no sustituye ese control.

No se detectó `FATAL EXCEPTION` en el proceso Android consultado. Esto no acredita ausencia de todos los errores ni estabilidad prolongada.

## Hallazgos y siguientes comprobaciones

1. En el listado de clientes, «Última visita» puede mostrar una cita futura. `ClientsScreens.kt` presenta `lastAppointment` del listado; `ClientAdminController::enrichFromBatch` escoge la cita más reciente sin filtrar completadas, mientras el detalle sí filtra visitas realizadas. Corregir y probar ese contrato de forma coordinada; este informe no modifica Barber.
2. Algunos resúmenes muestran inicialmente cero mientras cargan. Evaluar si necesitan un estado de carga explícito para evitar confundirlo con un resultado vacío.
3. Completar accesibilidad: ancho 320 dp, fuente 200 %, TalkBack y los cuatro temas. Esta revisión física no cubre esa matriz.
4. Completar recuperación de tarjeta y transferencia en un entorno de proveedor de pruebas confirmado. Esta sesión solo creó la reserva en efectivo; no valida cobro, webhook, comprobantes ni recreación durante Stripe.
5. No se certifica producción, rendimiento bajo concurrencia o compatibilidad entre versiones desplegadas a partir de estos resultados locales.

## Continuar la prueba

Mantener Docker y el USB conectados. Si se pierde el enlace, reenviar los puertos 8001 (API) y 18766 (imágenes). El servidor de imágenes solo escucha en loopback y sus archivos están separados de los accesos privados. No copiar respaldos, contraseñas o sesiones en Git ni importar los fixtures a un entorno operativo.


## Seguimiento del 4 de noviembre de 2026

La rama Android diferencia ahora la carga inicial de una respuesta vacía confirmada
mediante estado explícito en inventario, agenda y portafolio. El inicio del barbero
usa ese mismo estado: no presenta «sin citas» antes de recibir la primera respuesta.
Un error inicial no confirma cifras en cero; al fallar una actualización se conserva
el último resultado obtenido. Los errores siguen ofreciendo las acciones existentes
de actualización o reintento.

El resumen de inventario reutiliza `UrbanStatStrip`: las métricas se apilan en ancho
reducido o con fuente grande, conservando etiquetas e importes. Se elimina el
componente anterior que truncaba los valores a una sola línea. La advertencia de
reposición mantiene su color semántico.

Se añaden pruebas de estado para inventario, agenda y portafolio, y una comprobación
Compose del resumen a 320 dp con fuente al 200 %. Esto no sustituye TalkBack ni
una revisión física de los cuatro temas.

El código actual de Barber ya filtra las visitas completadas y ocurridas en el
listado de clientes e incluye las dos regresiones de ese contrato. No se modificó
ese repositorio en este seguimiento. Docker no estaba activo y ADB no detectaba
un dispositivo conectado: no se repitieron las pruebas integradas ni se instaló
esta versión en el teléfono. Los pagos y webhooks continúan pendientes de un
entorno de proveedor de pruebas confirmado. No se modificó producción.

Validación Android del seguimiento: `:app:testDebugUnitTest :app:lintDebug
:app:assembleDebug` finalizó correctamente. Se contabilizaron 399 pruebas, sin
fallos, errores ni omisiones; las seis nuevas comprobaciones forman parte de esa
suite. `git diff --check` no detectó problemas. El APK generado es de desarrollo;
no se instaló ni publicó en este seguimiento.
