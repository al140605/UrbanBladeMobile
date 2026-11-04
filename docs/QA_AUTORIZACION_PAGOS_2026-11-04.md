# Autorización local de pagos — 4 de noviembre de 2026

## Función

Se solicita verificar la identidad mediante el bloqueo del dispositivo antes de
abrir el checkout Stripe o confirmar una tarjeta guardada. La integración cubre
Mis citas, la reserva con tarjeta, el primer cobro de membresías y las compras de
beneficios que utilizan PaymentSheet. No altera los pagos en efectivo ni el envío
de comprobantes de transferencia.

Android 11 o superior usa el diálogo nativo de biometría fuerte o credencial del
dispositivo. Android 8–10 usa la confirmación nativa de PIN, patrón o contraseña.
Sin bloqueo seguro, al cancelar o ante un error, no se confirma el cargo.

Ver [arquitectura y límites de confianza](ARQUITECTURA.md#autorización-local-de-pagos-con-tarjeta).
No se recopila ni guarda la huella, el PIN o una autorización reutilizable. La
verificación local no sustituye los permisos del servidor ni Stripe/3DS/webhooks.

## Validación automatizada

Comandos ejecutados con JBR de Android Studio y API QA local:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug -PUI_AUDIT=true -PDEBUG_API_BASE_URL=http://127.0.0.1:8001/api/v1/ --console=plain
```

Resultado: 404 pruebas, cero fallos, errores u omisiones. Lint y compilación audit
finalizaron correctamente. Cinco regresiones nuevas comprueban autorización de un
solo uso, cancelación sin confirmación, doble toque, resultado tardío de un intento
anterior e invalidación al abandonar la pantalla. No se añadieron dependencias.

Las pruebas de la compuerta son de lógica; no prueban por sí mismas el diálogo del
fabricante, sus sensores ni todas las versiones de Android.

## Dispositivo

Se instaló la variante `com.urbanblade.mobile.audit` en el Samsung SM-S731B,
conservando sus datos y la app habitual. La comparación SHA-256 confirmó que el
APK instalado corresponde al generado en esta validación.

- El propietario confirmó que apareció la verificación de huella/PIN y que, tras
  autorizar personalmente, se abrió el formulario Stripe. No se registró cuál de
  esos métodos utilizó ni se solicitaron sus credenciales.
- El propietario realizó después la prueba de cancelación y confirmó que regresó
  al checkout sin abrir Stripe y pudo reintentar. Esa observación física es del
  propietario, distinta de las pruebas automatizadas.
- Una lectura posterior contra `urbanblade_dev` confirmó cero pagos nuevos respecto
  a la línea base de estos ensayos. No se confirmó una tarjeta durante esta prueba.

No se capturaron ni introdujeron PIN, patrones, contraseñas o datos biométricos.
La evidencia técnica privada y la línea base permanecen fuera de Git.

## Pendientes y entrega

No se comprobó físicamente el fallback de Android 8–10, la falta de bloqueo seguro,
el bloqueo temporal del sensor ni toda la matriz de fabricantes. Tampoco se repitió
el pago sandbox exitoso y 3DS después de añadir esta autorización: su evidencia
anterior está en [QA de pagos](QA_PAGOS_2026-11-04.md).

La invalidación al abandonar la pantalla está cubierta por la compuerta; no acredita
por sí sola recuperación completa del proceso Android durante Stripe. Sigue pendiente
la cotización exacta de transferencia desde el servidor.

Se conserva el trabajo en `feat/mejoras-interfaz-mobile`, sin PR, merge, despliegue
ni publicación en Play como parte de este cambio.
