# Mejoras de UI/UX y compatibilidad con la API

Revisión: 30 de octubre de 2026. Trabajo asociado a TT32 / HT-12.
Rama de trabajo: `feat/mejoras-interfaz-mobile`. No abrir PR hasta completar los cambios y su validación; no fusionar ni publicar en Google Play durante esta fase.

## Punto de partida

La rama conserva el rediseño editorial, la opción 2 del cliente y la opción A del barbero. Integra main publicado `3173e34839517145c1708f86f56320da0c6e9f1a`; la combinación `100ac0ca76182b9d4b4b20bbe0224abaf10e8035` pasó lint, build debug y 369 pruebas JVM sin errores u omisiones. Esta evidencia no reemplaza la revisión visual e integración de los próximos cambios.

Conservar imágenes, cuatro temas, componentes compartidos y navegación vigente. Una mejora compacta debe mantener legibilidad, objetivos táctiles y todas las funciones existentes. No añadir dependencias por imitar una referencia.

## Referencias consultadas

| Fuente primaria | Qué aporta | Aplicación propuesta |
|---|---|---|
| [Jetsnack y muestras oficiales Compose](https://github.com/android/compose-samples) | Sistema de diseño propio, catálogo y composiciones | Jerarquía de catálogo, separación de contenido y controles; conservar marca e imágenes UrbanBlade |
| [Reply](https://github.com/android/compose-samples/tree/main/Reply) | Navegación y diseño adaptable | Estudiar el comportamiento en anchos mayores sin cambiar destinos por rol |
| [Now in Android](https://github.com/android/nowinandroid) | Componentes, pruebas y datos de demostración separados de red | Estados reproducibles para auditoría; una captura sintética no acredita integración |
| [Accesibilidad Compose](https://developer.android.com/develop/ui/compose/accessibility/api-defaults) | Semántica y espacio táctil mínimo de 48 dp | Revisar acciones compactas, TalkBack y superposición de zonas táctiles |
| [Arquitectura Compose](https://developer.android.com/develop/ui/compose/architecture) | Estado descendente y eventos ascendentes | Conservar ViewModel y repositorio; evitar llamadas HTTP desde componentes visuales |
| [Navegación adaptable](https://developer.android.com/develop/adaptive-apps/guides/build-adaptive-navigation) | Barra inferior o lateral según espacio disponible | Evaluación posterior al recorrido principal de teléfono; no requiere migración inmediata |

Son referencias para decisiones, no material para copiar pantallas, recursos o identidad visual. El diseño de una app de noticias o correo no acredita un flujo de reservas o pagos. Si se reutiliza código de terceros, revisar su licencia y avisos por separado.

## Orden de trabajo propuesto

| Prioridad | Área | Mejora propuesta | Criterio de aceptación |
|---|---|---|---|
| P0 | Reserva | Resumen claro de servicio, profesional, fecha, hora e importe estimado; progreso y retorno coherentes | No confirmar con horarios de otra consulta; doble toque no duplica reserva; error conserva contexto |
| P0 | Reserva y pago | Distinguir cita creada de pago confirmado y ofrecer recuperación de la cita existente | Si falla o se cancela el pago después de crear, no volver a crear la cita para reintentar |
| P0 | Mis citas | Priorizar próxima visita, acciones válidas y mensajes de estado/pago | No inferir deuda solo por estado; revisar listas vacías, nombres largos y paginación |
| P0 | Accesibilidad | Revisar componentes compartidos y formularios en cuatro temas | Controles alcanzables, espacio táctil de 48 dp, fuente 200 %, ancho 320 dp y teclado sin tapar acciones |
| P1 | Barbero | Consolidar opción A: siguiente servicio y accesos operativos antes de gráficas | Revisar con citas y trabajos cargados, horario activo y estados vacíos; conservar medios |
| P1 | Cuenta y Beneficios | Resumir identidad y actividad; distinguir información de acciones | Cambios de perfil se reflejan al volver; un beneficio visible no promete elegibilidad no confirmada |
| P2 | Adaptación | Evaluar lista/detalle y navegación lateral en pantallas amplias | Mismos destinos, selección y permisos al cambiar tamaño; teléfono primero |

Presentar las variantes visuales antes de implementar una propuesta nueva. Las opciones ya aprobadas del cliente y barbero siguen siendo la base. No introducir animaciones decorativas en tareas frecuentes.

## Contraste estático con el backend

Fuentes locales: Android en la rama indicada y Barber en `a38eb33c61056a42ec952dfc91c6b19af9f612b5`. Se leyeron rutas, modelos y controladores; no se invocaron operaciones de negocio ni se modificó Barber. Los archivos locales no versionados de otro trabajo quedan fuera de esta revisión. No se afirma que este SHA sea la versión desplegada del servidor.

| Flujo | Contrato observado | Regla para la mejora |
|---|---|---|
| Horarios | `GET availability/slots`: `barber_id`, `service_id`, `date`; fecha desde hoy, IDs existentes | Conservar carga/error/vacío separados y selección ligada a servicio, barbero y fecha; el servidor valida al reservar |
| Crear cita | `POST appointments`; `AppointmentRequest` usa `barber_id`, `service_id`, `fecha`, `hora_inicio` y opciones existentes | No añadir campos ni totales inventados. El móvil ya contempla `pagar_ahora`, propina y productos; verificar cada opción en integración |
| Pagar al reservar | `POST appointments/{code}/deposit/stripe-intent` y carga de comprobante | No sustituirlo indiscriminadamente por el cobro de citas completadas. La reserva se crea antes del pago |
| Cobro de cita | `POST payments/stripe-intent` valida rol/pertenencia, que la cita esté en un estado cobrable (`confirmada`, `en_proceso` o `completada`; nunca `pendiente`) y que no tenga ningún pago registrado; calcula importe en servidor | La UI no decide precio final. Mostrar «Pagar cita» solo si el servidor marca `is_chargeable` y no hay `has_payment`, que es la misma regla (verificado el 30 de octubre de 2026 contra `PaymentController` y `AppointmentResource` en `a38eb33`; el servidor ya tiene pruebas de ambos rechazos) |
| Próximas e historial | `GET appointments` admite `page`, `per_page`, `desde`, `hasta`; Android tiene esos parámetros | Contrastar orden, varias páginas y límites de fechas antes de declarar que «no hay citas» |
| Estado de pago | Modelo Android recibe `pago_resuelto`; Mis citas usa `pagoResuelto` | Conservar la semántica del servidor; no deducir deuda únicamente de `confirmada` o `completada` |
| Barbero | Rutas `barber/agenda`, `barber/portfolio` y `barber/schedule` bajo middleware de barbero | Conservar permisos y propiedad de recursos; un cambio visual no amplía acceso |
| Perfil y favorito | `GET/PUT profile` y `PUT profile/favorite-barber` | Mostrar lo confirmado por la API y refrescar el usuario tras una actualización |

Fuentes en Android: `core/network/UrbanBladeApi.kt`, `data/model/Models.kt`, `ui/viewmodel/UrbanViewModels.kt` y `ui/screens/ClientAppointmentsScreen.kt`. Fuentes Barber: `routes/api.php`, `Api/Appointment/AvailabilityController.php`, `Api/Appointment/AppointmentController.php` y `Api/Payment/PaymentController.php` bajo `app/Http/Controllers/`.

El [inventario anterior de rutas](AUDITORIA_CONTRATOS_API.md) sigue siendo evidencia de su fecha. Una coincidencia de método y ruta no prueba payload, permisos, concurrencia ni funcionamiento con el backend desplegado.

## Validación antes del PR

1. Recorrido visual con datos sintéticos: 0, 1 y muchos elementos; nombres largos; cuatro temas; 320 dp y fuente 200 %. Reutilizar la variante `.audit` y conservar separada la sesión habitual.
2. Recorridos con API local aislada y usuarios ficticios por rol: carga, 401/403/422/429/503, timeout, doble toque, cambio de servicio durante consulta y reintento.
3. Reserva en efectivo; cancelación o fallo de pago con cita ya creada; cobro/deposito correspondiente al estado. No usar dinero, cuentas o comprobantes reales.
4. Confirmar actualización posterior a reservar, cancelar, reagendar y editar perfil. Revisar paginación y el límite de fecha del negocio frente al dispositivo.
5. Pruebas JVM pertinentes, lint y builds; registrar commit, API y datos efectivos. Agregar pruebas de regresión para errores reproducidos y corregidos.
6. Registrar evidencia y pendientes resueltos; solo entonces abrir PR cuando el alcance acordado esté cubierto. Subir avances a esta rama no equivale a aprobación del producto ni distribución.

Estado actual: investigación y contraste estático completados; primer incremento de reserva implementado, descrito abajo. La integración funcional con API local y la revisión visual siguen pendientes.


## Primer incremento: reserva y resultado de pago

Implementado el 30 de octubre de 2026, sin modificar rutas, payloads o reglas del backend:

- La reserva toma el bloqueo antes de iniciar su corrutina: dos eventos consecutivos no lanzan dos solicitudes de creación.
- La misma instancia del flujo no vuelve a reservar mientras espera Stripe ni tras haber creado una cita y finalizado el resultado de pago. La salida sigue siendo Mis citas, donde se recupera el pago existente.
- El resultado final se expone como estado del ViewModel, además de la confirmación guardada de la pantalla. Una composición nueva puede observarlo cuando el ViewModel sobrevivió a la recreación; no depende únicamente del callback de la composición anterior.
- Los callbacks repetidos o ajenos a un pago pendiente no sobrescriben el primer resultado ni repiten la finalización.
- Un rechazo de creación por validación permite corregir y volver a intentar; no se bloquea un formulario cuya cita no se creó.

Se reprodujeron primero tres fallos con pruebas JVM: creación duplicada, nueva reserva después de cancelar el pago y resultado sobrescrito por callback repetido. Después se verificaron también el fallo al iniciar el intento, el rechazo 422 recuperable y el reenvío tras éxito.

El estado «flujo finalizado» significa que existe una cita y terminó este paso del cliente; no acredita un cobro confirmado. Se conserva la confirmación del servidor y la separación entre depósito al reservar y cobro de cita completada.

### Límites de este incremento

El bloqueo es local al ViewModel, no una clave de idempotencia del servidor. La muerte del proceso y una respuesta perdida después de crear en Laravel requieren reconciliación del lado servidor antes de automatizar reintentos; no se garantiza deduplicación entre procesos o dispositivos. No se modificó Barber ni se enviaron reservas, comprobantes o pagos reales. Pendientes: recreación durante el pago en dispositivo, recorridos de recuperación en Mis citas e integración con API local aislada.

### Validación local del incremento

`lintDebug`, `testDebugUnitTest` y `assembleDebug`: BUILD SUCCESSFUL. Suite completa: 375 pruebas, cero fallos, errores u omisiones; incluye seis regresiones nuevas de reserva. Gitleaks sobre los cuatro archivos cambiados: cero hallazgos. Enlaces documentales y `git diff --check`: correctos. No se ejecutó CI, build release, instalación en dispositivo ni prueba funcional con API real para este incremento.

## Accesibilidad (30 de octubre de 2026)

Revisión estática de las pantallas principales y pruebas automáticas con Robolectric. No sustituye a TalkBack en el teléfono.

- **Contraste:** los cuatro temas cumplen AA (4,5:1) en todas las combinaciones de texto, acento y estados sobre fondo, superficie y tarjetas; el valor más bajo medido es 4,8:1. `ThemeContrastTest` lo vigila, ahora también para los estados (éxito, aviso, error, información) en los cuatro temas.
- **320 dp y fuente al 200 %:** `AccesibilidadEscalaTest` ejecuta los pasos de servicio y de resumen de la reserva con ese peor caso: la información sigue en pantalla, se puede elegir y el área táctil es de al menos 48 dp.
- **Cambios:** valoración del chatbot extraída a `ChatRatingButtons` con botones de 48 dp (`ChatRatingButtonsTest`), botón de moderación del muro de 48 dp y botón «Reservar» del chatbot a dos líneas para que el precio no se corte con fuente grande.
- **Hallazgos que no son defecto:** un `IconButton` con `Modifier.size` menor ya tenía un área táctil efectiva de 48 dp, porque Compose la amplía; los 48 dp explícitos evitan depender de eso. Los chips de filtro de Material miden 38 dp con fuente al 200 % por diseño y también se amplían al tocar.
- **Pendiente:** recorrido con TalkBack en el teléfono, revisión visual de Mis citas, Cuenta, barbero y administración en los cuatro temas, y comprobar que el teclado no tape acciones en los formularios.

Validación local: `lintDebug`, `assembleDebug` y 381 pruebas JVM, sin fallos ni omisiones.

## QA con datos poblados

El [informe del 30 de octubre](QA_DATOS_POBLADOS_2026-10-30.md) registra las pruebas físicas con cliente, barbero y administración, la reserva local en efectivo y el doble toque sin duplicado. La recuperación de tarjeta/transferencia y la matriz completa de accesibilidad siguen pendientes.
