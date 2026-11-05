# QA de cotización de transferencias

## Alcance

Android: rama `feat/mejoras-interfaz-mobile`, variante `com.urbanblade.mobile.audit`.
Servidor: cambio local en `feat/cotizacion-transferencia`, API QA aislada. No se
publica el backend ni se abre PR con esta comprobación.

## Comprobaciones

- Backend: 82 pruebas de pagos, 291 assertions, mediante `test.ps1` y Mongo de
  pruebas. Incluyen cotización ajena denegada, cita no cobrable, pago ya existente,
  contexto inválido, propina negativa y coincidencia con el registro del recibo.
- API QA: servicio de 100, descuento aplicado a 95, propina 10, total 105 MXN.
  La consulta no genera un pago ni una reserva.
- Android: pruebas de modelo, cambios de contexto, respuesta tardía, error de red
  y componente que usa el importe del servidor en lugar del estimado local.
- Regresión de centavos: servicio 142.50 y propina 15 muestran 157.50, sin
  redondear el total a pesos enteros.
- Resultado final de Android: 421 pruebas, cero fallos y cero omitidas; lint y
  compilación debug correctos. APK de auditoría instalada en el Samsung conectado.
- Dispositivo físico: servicio con descuento 142.50; al seleccionar 10% de
  propina aparece 157.50 como total a transferir. No se envió un comprobante ni
  se ejecutó un cobro. Captura conservada fuera de Git como evidencia privada.

## Límites

La cotización no bloquea el precio; el servidor valida nuevamente al recibir el
comprobante. Esta revisión no ejecuta transferencias bancarias reales y no
certifica recuperación de proceso, todos los temas, TalkBack ni fuentes grandes.
La revisión completa de estados de cita/pago y recuperación de pagos continúa
como trabajo separado del plan UX.
