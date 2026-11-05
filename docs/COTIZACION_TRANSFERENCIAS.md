# Cotización de transferencias

La aplicación consulta el importe al backend; no reproduce las reglas de
membresía o lealtad. La cotización incluye servicio con descuentos, propina,
total y moneda MXN. Una transferencia sigue pendiente hasta revisar el comprobante.

## Contrato

`GET /api/v1/payments/transfer-info` conserva la respuesta bancaria sin parámetros.
Para cotizar acepta `appointment_id` o `service_id`, exclusivamente uno, y
`propina` no negativa. El contexto de servicio permite consultar antes de reservar
y no crea una cita. El cliente solo puede consultar sus propias citas.

La respuesta añade `data.cotizacion` con `appointment_id`, `service_id`,
`monto_servicio`, `propina`, `total` y `moneda`. El servidor rechaza citas no
cobrables o con pago activo. Consultar no crea pagos ni operaciones en Stripe.

## Comportamiento de Android

Cambiar cita, servicio o propina descarta el importe anterior y cancela la consulta
pendiente. Una respuesta tardía no sustituye la consulta nueva. La interfaz valida
contexto, moneda e importes antes de presentar el total como confirmado.

Los fallos de red se muestran como errores recuperables, separados de una cuenta
bancaria sin configurar. Una API antigua sin cotización conserva el aviso de
importe estimado; no habilita el envío del comprobante como si hubiera confirmado
el importe. Puntos y tarjetas regalo pertenecen al flujo de tarjeta.

La cotización refleja las reglas actuales; no bloquea precios ni garantiza una
vigencia. El backend vuelve a calcular al recibir el comprobante. Los productos
para recoger se pagan por separado y no forman parte de esta cotización.

## Entrega coordinada

El contrato del backend debe publicarse antes de liberar esta versión de Android.
La implementación del servidor se mantiene local en `feat/cotizacion-transferencia`;
esta actualización no autoriza despliegues, PR ni fusiones.

Las pruebas cubren propiedad de citas, descuentos, propina, permisos, ausencia de
escrituras al consultar, coincidencia con el registro del comprobante, respuestas
tardías y presentación de importes confirmados frente a estimados.
