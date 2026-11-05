# Comparativa de experiencia: citas y pagos móviles

Revisión: 4 de noviembre de 2026. Código UrbanBlade inspeccionado: `0de90ea`.

## Método y alcance

Se contrastaron flujos documentados por Fresha, Booksy y Square, investigación
publicada de checkout de Baymard y el código actual de UrbanBlade. No se iniciaron
sesiones ni se hicieron reservas o pagos en competidores. Sus guías describen
funciones y recorridos; no demuestran por sí solas mayor satisfacción o conversión.
Las políticas y opciones pueden variar por país, proveedor, plan y configuración.
No se hicieron entrevistas nuevas ni una muestra representativa de clientes.

Las recomendaciones siguientes son inferencias de producto aplicadas a nuestros
hallazgos, no funciones copiadas ni garantías estadísticas de mejora.

## Qué aporta cada referencia

| Referencia primaria | Patrón documentado | Aplicación propuesta |
| --- | --- | --- |
| [Fresha: recorrido de reserva](https://www.fresha.com/help-center/knowledge-base/online-profile/101646-learn-how-clients-book-appointments-online) | Servicio, profesional, disponibilidad, revisión de fecha/hora/precio y confirmación. | Conservar el asistente y revisar que la última pantalla reúna la información necesaria sin regresar a otros pasos. |
| [Fresha: políticas de pago](https://www.fresha.com/help-center/knowledge-base/payments/613-payments-policies-overview) | Diferencia depósito y captura de tarjeta; guardar una tarjeta no implica cobrar en ese momento. | Distinguir visualmente reserva, depósito, comprobante en revisión y pago completo. No etiquetar todos como Pagado. |
| [Booksy: reagendar](https://help.booksy.com/hc/en-gb/articles/21616159780242-How-can-I-reschedule-an-appointment) | Gestión desde Citas condicionada por la política del negocio y acceso al contacto cuando no es posible. | Explicar la restricción y la siguiente acción sin obligar a descubrirla tras un error. No copiar su mecanismo de cancelar y crear otra cita. |
| [Booksy: falta de disponibilidad](https://help.booksy.com/hc/en-gb/articles/21618163992594-Why-is-there-no-availability-with-my-desired-service-provider) | Explica la disponibilidad limitada y ofrece aviso/lista de espera cuando está habilitada. | Reforzar la lista de espera existente y las alternativas de día o profesional, sin prometer una reserva automática. |
| [Square: cancelación y prepago](https://squareup.com/help/us/en/article/5493-set-a-custom-cancellation-policy-with-square-appointments) | Ventanas de cancelación, condiciones de prepago y recordatorio de la política. | Mostrar las condiciones reales de UrbanBlade antes de consentir o cancelar, con datos del servidor. |
| [Square: avisos y recordatorios](https://squareup.com/help/us/en/article/6729-customer-confirmations-with-square-appointments) | Avisos diferenciados de solicitud, confirmación, cambios y cancelación. | Una notificación debe explicar qué cambió y abrir la cita correspondiente. Verificar entrega real de los canales ya integrados. |
| [Baymard: Payment UX](https://baymard.com/blog/payment-ux) | Resumen accesible, precio transparente, acción explícita y recuperación de errores sin rehacer todo el formulario. | Consolidar el flujo final y corregir primero importes inciertos y recuperación, antes de añadir decoración o métodos de pago. |

## Lo que ya existe en UrbanBlade

- Reserva por pasos, selección de profesional, disponibilidad y revisión final.
- Favorito, repetición de visita y lista de espera: no proponerlos como funciones nuevas.
- Mis citas con acciones de pagar, reagendar y cancelar; la autoridad del permiso sigue en API.
- Tarjeta nueva mediante selección Stripe, revisión del importe real y huella/PIN solo al pulsar Pagar.
- Modal refinado y Link desactivado; efectivo y transferencia se conservan.
- Cobro sandbox normal y 3DS, rechazo, pago autorizado por propietario y cancelación de revisión con evidencia previa. Ver [QA de pagos](QA_PAGOS_2026-11-04.md), [autorización final](INVESTIGACION_UX_PAGO_FINAL_2026-11-04.md) y [modal](QA_MODAL_PAGO_2026-11-04.md).

La comparación no justifica reemplazar el diseño aprobado del cliente y barbero,
las imágenes, los cuatro temas ni los componentes compartidos.

## Hallazgos concretos del código

1. `TransferAccount` en `BookingPayment.kt` muestra un importe estimado y exige
   confirmar descuentos con la barbería. La tarjeta ya presenta el importe del
   PaymentIntent: falta una cotización autoritativa equivalente para transferencia.
2. En `ClientAppointmentsScreen.kt`, Por pagar usa «Citas atendidas que aún no se
   cobran». En las pruebas físicas se mostraron allí citas confirmadas pendientes
   de pago. El texto presupone un servicio atendido que no se deduce del cobro.
3. `confirmAppointmentPayment` consulta el servidor varias veces y después avisa
   que sigue confirmando. Es una recuperación parcial; falta demostrar el recorrido
   tras recreación, muerte del proceso o pérdida de respuesta sin duplicar operaciones.
4. El consentimiento por inasistencia del último paso dice «un porcentaje» y que
   puede quedar un adeudo. Conviene presentar el porcentaje/plazo reales o su
   explicación contextual, sin inventarlos ni fijarlos en Android.
5. `joinWaitlist`, su gestión y la acción cuando no hay horarios ya existen. El
   trabajo debe centrarse en comprensión, estados y entrega real del aviso.

## Prioridad y criterios de aceptación

| ID | Prioridad | Mejora | Dependencia | Criterio verificable |
| --- | --- | --- | --- | --- |
| UX-01 | P0 | Total exacto para transferencia y desglose de descuentos/propina | Contrato de cotización backend y Android | Misma cita y opciones producen el mismo total por método; la UI no calcula descuentos y no presenta estimaciones como instrucciones definitivas de transferencia. |
| UX-02 | P0 | Separar estado de cita y estado del dinero | Datos/API existentes y presentación | Confirmada no significa atendida; depósito no significa pago total; pendiente de verificación no significa pagado. Ajustar el texto de Por pagar y probar combinaciones. |
| UX-03 | P0 | Recuperar un pago interrumpido | Reconciliación backend, ciclo de vida Android e idempotencia | Tras rotar, cerrar proceso o perder red, consultar la operación existente antes de reintentar; no crear otra cita ni un segundo cobro. No persistir CVC, PIN ni autorización biométrica reutilizable. |
| UX-04 | P1 | Transferencia con estado y siguiente acción | Campos y permisos del backend | Mostrar comprobante recibido/en revisión/aprobado/rechazado; explicar el motivo y permitir reenvío válido. Cancelar o reenviar no duplica un pago activo. |
| UX-05 | P1 | Reagendar/cancelar con condiciones claras | Política del servidor | Antes de confirmar se ve el plazo aplicable y si hay consecuencia económica; si no es elegible, explicar por qué y ofrecer el contacto disponible. No cambiar reglas por imitar a otra plataforma. |
| UX-06 | P1 | Próxima visita y repetir reserva sin fricción | Funciones existentes de citas, favorito y disponibilidad | Una acción principal por estado; repetir preselecciona datos pero vuelve a validar horario, precio y permisos actuales. |
| UX-07 | P1 | Sin horarios con alternativas y lista de espera clara | Disponibilidad, lista de espera y notificaciones existentes | Se puede cambiar fecha/profesional o pedir aviso; se distingue estar en lista de tener una cita. Mensaje y destino de notificación se validan con roles reales. |
| UX-08 | P2 | Accesibilidad de los recorridos completos | Componentes y dispositivo | TalkBack, 320 dp, fuente grande, teclado y cuatro temas; todo botón crítico alcanzable y con estado comprensible. Una prueba aislada de componente no certifica el recorrido completo. |

## Orden recomendado y cómo medirlo

Primero UX-01, UX-02 y UX-03: afectan dinero y confianza. Después UX-04 y UX-05,
seguido de UX-06 y UX-07. UX-08 se comprueba en cada cambio, sin dejar accesibilidad
para una última corrección estética.

Para cada incremento, registrar tareas completadas, pasos abandonados, errores,
reintentos que terminan correctamente y confusión observada sobre importe/estado.
No fijar una promesa de conversión ni porcentajes sin una línea base. Si se añade
telemetría, definir consentimiento, minimización y eventos sin datos de tarjeta,
credenciales o información personal innecesaria; este informe no la habilita.

En una siguiente ronda de pruebas con clientes reales, proponer tareas concretas:
reservar con su profesional habitual, encontrar un horario alternativo, pagar,
reintentar después de una desconexión y reagendar antes/fuera del plazo. No tratar
las pruebas de QA del desarrollador como investigación representativa de clientes.

## Límites de esta entrega

Este incremento documenta investigación y prioridades; no modifica código de
aplicación, políticas comerciales, infraestructura ni producción. No se ejecutan
cobros, suscripciones o reservas en plataformas de terceros. La rama conserva el
límite de no abrir PR ni fusionar hasta cubrir los cambios acordados.
