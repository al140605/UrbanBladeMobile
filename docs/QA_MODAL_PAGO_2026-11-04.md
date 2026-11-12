# Refinamiento del modal de pago y eliminación de Link

Revisión: 4 de noviembre de 2026. Rama: `feat/mejoras-interfaz-mobile`.

## Cambios

La confirmación deja el AlertDialog genérico y reutiliza
`UrbanPaymentReviewDialog`, componente compartido del patrón Android. Tiene
encabezado del comercio, total real destacado, bloque de tarjeta seleccionada y
acciones verticales para cambiar tarjeta, pagar y cancelar. Los colores proceden
de UrbanColors y el contenido admite desplazamiento y fuente grande.

La configuración compartida de Stripe usa
`PaymentSheet.LinkConfiguration.Display.Never`. Se elimina Link del selector y de
su registro opcional sin cambiar claves, permisos o la configuración global de la
cuenta Stripe. Esto aplica a las pantallas que utilizan el FlowController compartido;
los formularios propios de reserva y membresía no añadían Link.

Se conserva el orden tarjeta, revisión, Pagar, huella/PIN y confirmación Stripe.
La app sigue obteniendo el importe del PaymentIntent creado por el servidor.

## Verificación automatizada

`testDebugUnitTest`, `lintDebug` y `assembleDebug` con `UI_AUDIT=true` y API local
finalizaron correctamente. Resultado: 413 pruebas, cero fallos, errores u omisiones.
Se actualizaron los casos de revisión para comprobar importe y acciones alcanzables
con desplazamiento a 320 dp y fuente al 200 %. Las pruebas de bloqueo durante la
autorización y de cancelación/cambio de tarjeta continúan pasando.

Las dos copias de la skill de patrones documentan el componente compartido.
`git diff --check` no detectó problemas. Las pruebas no certifican TalkBack ni una
matriz física completa de temas, versiones y fabricantes.

## Dispositivo y API QA

Se instaló solo `com.urbanblade.mobile.audit` en el Samsung SM-S731B, conservando
la app habitual y los datos. Se comprobó en el formulario oficial Stripe que no
aparece texto, botón ni opción de registro de Link; continúa disponible la tarjeta.
Con datos ficticios se llegó al modal, cuyo importe y controles resultaron visibles.
La captura se revisó visualmente y permanece privada fuera de Git.

Durante una pausa de la sesión, el propietario probó el botón Pagar y confirmó
que autorizó con huella/PIN. Una consulta posterior a Mongo local identificó un
único pago nuevo por $95.00, con tarjeta, estado verificado y referencia Stripe.
La cita conservó su estado confirmada. Ese cobro se atribuye a la prueba del
propietario, no a una cancelación realizada por el agente.

Después se hizo una prueba independiente: nueva línea base, otra cita QA pendiente,
tarjeta ficticia, revisión y Cancelar. La lectura posterior confirmó cero pagos
adicionales. No se pulsó Pagar durante esa segunda prueba.

Al retomar se restableció el túnel USB local y `/up` respondió 200. La revisión de
salud Docker del contenedor seguía señalando unhealthy; esa señal del contenedor no
se equipara con el resultado HTTP o con una garantía de disponibilidad continua.

## Límites y publicación

Estos resultados corresponden al sandbox y a la base `urbanblade_dev`. No se
movió dinero real ni se modificó producción. No se añadieron dependencias.
Credenciales, referencias de cobros, comprobantes y capturas permanecen fuera de Git.

El cambio se conserva en la misma rama de mejoras, sin PR, merge, despliegue o
publicación en Play. Siguen pendientes la cotización exacta de transferencia desde
servidor y la recuperación completa durante el pago.

Referencia: [Display.Never de Stripe Android](https://stripe.dev/stripe-android/paymentsheet/com.stripe.android.paymentsheet/-payment-sheet/-link-configuration/-display/-never/index.html).
