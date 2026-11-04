# Experiencia de pago y verificación final

Revisión: 4 de noviembre de 2026. Alcance: autorización local de tarjeta en Android.

## Evidencia y criterio

| Fuente primaria | Hallazgo útil | Aplicación en UrbanBlade |
| --- | --- | --- |
| [Baymard: Payment UX](https://baymard.com/blog/payment-ux) | La investigación de checkout recomienda un total visible y una revisión clara después de introducir el método, sin confundir captura con compra. | Introducir la tarjeta no cobra; la revisión identifica tarjeta e importe real y ofrece Pagar, Cambiar tarjeta y Cancelar. |
| [Google Play: verificación de compras](https://support.google.com/googleplay/answer/1626831?hl=es) | Distingue la verificación del comprador de la verificación del método de pago; el comportamiento varía por dispositivo, territorio y ajustes. | La huella/PIN de UrbanBlade y el desafío 3DS del banco son pasos diferentes. |
| [Apple: compras con Apple Pay](https://support.apple.com/guide/iphone/make-purchases-with-apple-pay-iphbd4cf42b4/27/ios/27) | Documenta una acción explícita del usuario para autorizar la compra con el dispositivo. | Pedir verificación como consecuencia de una acción de pagar, no para abrir el formulario. |
| [Android: diálogo biométrico](https://developer.android.com/identity/sign-in/biometric-auth) | Recomienda confirmación explícita para acciones sensibles como compras y permite credencial del dispositivo. | Mantener el diálogo del sistema y una autorización de un solo uso para la operación pendiente. |
| [Stripe Android: FlowController](https://stripe.dev/stripe-android/paymentsheet/com.stripe.android.paymentsheet/-payment-sheet/-flow-controller/index.html) | Separa configurar, elegir método y confirmar el pago. | Usar selección de tarjeta sin cobro y llamar confirm solo después de Pagar y de autorizar. |

Estas fuentes no demuestran que todas las aplicaciones usen el mismo recorrido.
Baymard aporta investigación publicada de usuarios de checkout; Google, Apple,
Android y Stripe documentan sus propios productos. No se hicieron entrevistas nuevas
ni un estudio representativo de usuarios de UrbanBlade. La observación directa del
propietario mostró que la solicitud anterior aparecía demasiado pronto; esa es la
corrección concreta, no una afirmación general sobre todas las plataformas móviles.

## Flujo resultante

1. El usuario solicita pagar su cita o beneficio.
2. La app consulta el importe del PaymentIntent que creó el backend; no decide el precio.
3. Stripe recoge o selecciona la tarjeta, sin confirmar el cobro.
4. UrbanBlade muestra tarjeta e importe real en MXN para revisar.
5. El usuario pulsa Pagar con el importe visible.
6. Android pide huella compatible o credencial de bloqueo.
7. Solo la autorización positiva permite confirmar en Stripe; puede seguir un desafío 3DS.
8. El backend concilia por webhook y la app consulta el estado real.

Cancelar la selección no cobra. Cancelar huella/PIN conserva la revisión para
reintentar; cambiar la tarjeta vuelve a la selección. Un doble toque no debe
confirmar dos veces. Los callbacks tardíos de consulta no reabren un pago cancelado.
La autorización y los datos de revisión no se persisten como permiso reutilizable.

Las tarjetas guardadas que ya se confirman desde un botón Pagar y las reservas o
membresías con formulario propio conservan su verificación inmediatamente anterior
a la confirmación del SDK. La separación con FlowController corrige específicamente
las pantallas que antes solicitaban huella antes de abrir PaymentSheet.

## Mejoras siguientes

- Cotización exacta de transferencia proporcionada por backend, conservando descuentos y propinas.
- Recuperación al recrear actividad o proceso durante selección, autenticación y confirmación.
- Matriz de accesibilidad de la revisión: 320 dp, fuente grande, TalkBack y cuatro temas.
- Mensajes que diferencien rechazo, cancelación, confirmación pendiente y pago verificado.

La investigación no autoriza producción, una publicación en Play ni cargos reales.

## Validación de esta implementación

La suite definitiva ejecutó 413 pruebas: cero fallos, errores u omisiones.
`testDebugUnitTest`, `lintDebug` y `assembleDebug` finalizaron correctamente con
variante audit y API local. Se añadieron seis casos de transición de pago y tres
de revisión Compose, incluido ancho 320 dp con fuente al 200 %. Esa prueba no
certifica TalkBack ni todos los tamaños, temas o dispositivos.

En el Samsung se comprobó que el selector Stripe abre sin verificación local y
presenta Continuar. Con datos ficticios se llegó a la revisión que muestra tarjeta
terminada en 4242, total $95.00 MXN y acciones Pagar, Cambiar tarjeta y Cancelar.
Una lectura de Mongo QA confirmó cero pagos nuevos al llegar a esa revisión.
La firma del webhook local permaneció configurada y su listener activo.

La autenticación local y el cobro posterior se verifican aparte de esa selección;
no deben inferirse de que el formulario o la revisión aparezcan correctamente.
Las capturas y referencias privadas no se publican en Git.

### Confirmación física del pago final

El propietario confirmó que el diálogo de huella/PIN apareció al pulsar el botón
final Pagar y lo autorizó personalmente. No se solicitaron ni observaron sus
credenciales. Una lectura posterior de Mongo QA confirmó exactamente un pago nuevo
con tarjeta, estado verificado e importe $95.00. La cita continuó confirmada.
Se observó el regreso a la app y el mensaje de pago resuelto. El listener registró
la tercera respuesta HTTP 200 de esta serie de ensayos sandbox, adicional a los dos
pagos normales/3DS documentados previamente.

Así se distingue selección sin cobro, autorización del propietario y conciliación
real por webhook. No se creó un registro manual para simular el resultado del SDK.
No se movió dinero real ni se modificó producción. Esta prueba física cubre el
recorrido de tarjeta nueva de una cita; no certifica todas las compras de beneficios,
tarjetas guardadas, fabricantes o recuperación de proceso.

La cancelación biométrica y el cambio de tarjeta de esta revisión están cubiertos
por las nuevas pruebas de estado/interfaz; no se atribuye aquí una nueva ejecución
física de esas variantes. El informe de autorización anterior conserva su fecha y
su evidencia histórica, y enlaza esta corrección del momento de la verificación.
