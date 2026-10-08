# Notificaciones push — pruebas y evidencia (Sprint 2, T142/T143)

Fecha: 7-oct-2026 · Dispositivo: Samsung SM-S731B, Android 16 · Build: `staging` · Proyecto Firebase: `barber-c6b3a`

## 1. Qué cambió
| Capa | Cambio |
|---|---|
| Backend `barber` | Todos los avisos al cliente y al personal pueden llegar por push (web + Android), no solo las citas. 14 tipos clasificados en 6 canales y con pantalla destino (`app/Services/Push/PushRouting.php`). El texto de pagos ahora dice «Pago recibido» con el monto. |
| Android | 6 canales de notificación (Citas, Pagos, Pedidos, Beneficios, Promociones, Operación) con importancia propia. Color dorado, agrupadas por canal, versión pública en pantalla de bloqueo, y **al tocar el aviso se abre la pantalla correspondiente** (lista cerrada de rutas). «Atrás» regresa a Inicio. |
| Requisito | El usuario debe tener activado el canal **push** en sus preferencias (por defecto está apagado). |

## 2. Tipos de aviso → canal → pantalla
| Tipo | Canal | Pantalla |
|---|---|---|
| Cita (creación, confirmación, cancelación, recordatorio) | citas | Citas |
| Solicitud de reseña | citas | Citas |
| Pago recibido / comprobante de transferencia | pagos | Pagos |
| Pedido entregado / vencido | pedidos | Pedidos |
| Nivel sube/baja, puntos vencidos, sorteo, cumpleaños | fidelidad | Wallet |
| Promoción (solo con consentimiento) | promociones | Notificaciones |
| Servicio excedido (personal) | operacion | Citas |
| Inventario bajo (personal) | operacion | Inventario |

## 3. Pruebas automáticas
| Prueba | Resultado |
|---|---|
| Backend: `PushAllCasesTest` (13 tipos × 3 comprobaciones + consentimiento + monto + fallback) junto con `AppointmentNotificationChannelsTest`, `FcmPushServiceTest` y `PushTestEndpointTest`, vía `test.ps1` | **51 pruebas pasan** (148 aserciones) |
| Android: `PushContentTest` (texto, canal, ruta permitida, canal desconocido, lista de canales = backend, importancia) | Pasan |
| Android: suite unitaria completa `testDebugUnitTest` | Pasa |

Comandos: `./test.ps1 --filter "PushAllCasesTest|AppointmentNotificationChannelsTest|FcmPushServiceTest|PushTestEndpointTest"` (en `barber`) y `gradle :app:testDebugUnitTest` (en `UrbanBladeMobile`).

## 4. Pruebas en el celular (Firebase Console → mensaje de prueba)
Enviadas solo al token del celular de prueba (no a la base de usuarios).

| # | Escenario | Resultado | Evidencia |
|---|---|---|---|
| 1 | App en segundo plano, canal `pagos`, `route=payments` | ✅ Llegó «Pago recibido» en el canal **pagos** (importancia alta). Al tocarla abrió **Cobros y pagos**. | `push_pagos_bandeja.png`, `push_pagos_destino.png` |
| 2 | App abierta, canal `pedidos`, `route=orders` | ✅ Notificación creada por la app en canal **pedidos**, color dorado, categoría evento, agrupada, con versión pública. Al tocarla abrió **Pedidos**. | `push_pedidos_destino.png` |
| 3 | «Atrás» tras abrir dos avisos seguidos | ✅ Regresa a Inicio (antes volvía al aviso anterior; corregido). | — |
| 4 | Los 6 canales existen en el teléfono con la importancia esperada (4=alta: citas, pagos, operación; 3=normal: pedidos, beneficios, promociones) | ✅ | `dumpsys notification` |

| 5 | Borrador **DEMO 1 (Cita)** guardado en Firebase y enviado como mensaje de prueba, app en segundo plano | ✅ Llegó en el canal **citas** (importancia alta). Flujo «abrir borrador → Probar» validado (ver `GUIA_FIREBASE_DEMO.md`). | — |
| 6 | Bandeja de avisos de la app (campana): tocar «Pedido cancelado» | ✅ Marca el aviso como leído **y abre Mis pedidos**; al volver, el aviso queda en gris (28 sin leer). Cada aviso con destino muestra una flecha. 14 tipos → pantalla (prueba unitaria `NotificationRouteTest`). | — |

## 5. Cobro con tarjeta — diseño (cliente, reserva → paso Pago)
Revisado en el celular con sesión de cliente, sin confirmar ninguna reserva.

| Cambio | Detalle | Evidencia |
|---|---|---|
| Carrusel de tarjetas unificado | Las tarjetas guardadas en **reserva y membresía** ahora se eligen con el carrusel 3D (antes eran filas con botón). «Otra tarjeta» es la última página y abre el formulario de datos. | `ui_checkout_tarjeta_1.png` (Visa, 12/56), `_2.png` (Visa, 02/42), `_3.png` (Otra tarjeta) |
| Cobro de citas (`CheckoutSheet`) | La hoja se **desplaza** (antes podía cortar el botón de pagar en pantallas chicas), muestra **Total estimado**, y «gift card / puntos» quedan tras el enlace «¿Tienes gift card o puntos?». | Código; no hubo cita pagable para verlo en vivo |
| Tarjeta de prueba | La hoja de Stripe en modo prueba acepta `4242 4242 4242 4242`; la escribe el usuario (no se probó el cobro real). | — |

## 6. Pendiente / límites
- Los canales **citas, fidelidad, promociones y operación** están cubiertos por pruebas unitarias; no se enviaron como push real uno por uno.
- Los push **generados por el backend** (no por Firebase Console) llegarán a staging cuando se despliegue `barber`; hay que activar `push` en las cuentas de la demo.
- No se confirmó un **pago real** con Stripe (lo hace el usuario con la tarjeta de prueba) ni se vio `CheckoutSheet` en vivo: la cuenta de cliente no tenía ninguna cita cobrable.
- Los dos usuarios de prueba están en staging con datos reales de demo; no se creó ni modificó ninguna cita, pago ni pedido.

## 7. Correo del pago (backend)
El correo «Tu comprobante de pago» ahora adjunta **el comprobante y la factura** en PDF (antes solo la factura). Pruebas: `PaymentReceiptEmailTest` (con y sin comprobante). Solo se envía si el cliente tiene el correo activado en sus preferencias.

## 8. Incidente 7-oct: cancelación sin aviso (causa raíz y corrección)
**Síntoma:** se canceló la cita del jue 22-oct (Combo Corte + Barba); la cita quedó cancelada pero no llegó ningún aviso ni push (celular ni web).

**Evidencia:** en la base solo existen, a las 22:34:11, dos avisos «No se pudo reembolsar un depósito automáticamente» (personal). No existe ningún aviso de cancelación para cliente, barbero ni personal, y la cita tiene `cancellation_notified_at = null`. El cliente sí tiene push activado y token de teléfono; el worker de staging sí procesa avisos (22:39 llegaron las reservas).

**Causa raíz (en el código):** de las 5 rutas que cancelan una cita, solo `DELETE /appointments/{code}` (cliente desde su app) llamaba a `AppointmentNotifier::cancelled()`. Las otras cuatro cambiaban la cita sin avisar:
| Ruta | Quién la usa | Antes | Ahora |
|---|---|---|---|
| `PATCH /appointments/{code}/status` → cancelada | agenda web y app del personal | sin aviso (`statusChanged()` ignora «cancelada») | avisa |
| `PUT /appointments/{code}` con estado cancelada | edición completa (admin/recepción) | sin aviso | avisa |
| `POST /appointments/{code}/manage/cancel` | enlace del recordatorio | sin aviso | avisa |
| `DELETE /appointments/{code}` | cliente (o admin) | avisaba, pero después del reembolso | avisa antes del reembolso |
Además el barbero leía «X canceló su cita» aunque la hubiera cancelado el negocio; ahora lee «Se canceló tu cita con X».

**Corrección:** todas las rutas llaman a `cancelled()` justo después de guardar el estado y **antes** del reembolso y de la lista de espera, para que un fallo de Stripe no pueda tapar el aviso.

**Pruebas (`AppointmentCancelNotificationsTest`, 8):** cliente, barbero y personal avisados por cada ruta, también con el reembolso fallando, y el push por FCM + web para quien lo tiene activado. **Se comprobó que 4 de ellas fallan con el código anterior** (agenda, agenda con reembolso fallido, edición y enlace) y pasan con la corrección. Suite completa del backend: **789 pruebas pasan**.

**Para que surta efecto en staging hay que desplegar `barber`.** Aparte, el reembolso de ese depósito falló en Stripe (`pi_3UJD…` sigue «verificado»): hay que revisarlo a mano en el panel de Stripe.

**Pendiente de verificar en staging:** que tenga `FIREBASE_PROJECT_ID` / `FIREBASE_CREDENTIALS_PATH` (sin ellos no hay push al teléfono aunque el aviso se cree). Para la web, el usuario debe tener una suscripción de navegador (el cliente de esta cita tiene 0).

## 9. «Agregar tarjeta»: nombre del titular y tarjeta en vivo (8-oct)
**Qué cambió (app Android, reserva y membresía):**
- Campo obligatorio **«Nombre del titular»** (mayúsculas automáticas, solo letras, apóstrofo/guion/punto, máx. 26; pide nombre y apellido).
- La tarjeta «Otra tarjeta» del carrusel (o sola, si no hay tarjetas guardadas) **se dibuja mientras se escribe**: detecta la marca (Visa, Mastercard, Amex…), cambia de color, muestra el número agrupado y completado con puntos, el titular y el vencimiento.
- El nombre se manda a Stripe como dato de facturación de la tarjeta; el backend ahora devuelve `holder` en `GET /payments/cards` y las tarjetas guardadas lo muestran. Las guardadas antes no lo tienen y siguen mostrando la marca.
- Con marca aún desconocida ya no se rotula «UNKNOWN».

**Verificado en el celular** (cliente, reserva → Pago → Tarjeta → «Otra tarjeta»; sin confirmar nada): escribir «luis gonzalez» muestra `TITULAR LUIS GONZALEZ`; los 6 primeros dígitos `424242` pintan la tarjeta **Visa en plata** con `4242 42•• •••• ••••` y `VENCE MM/AA`. Evidencia: `ui_tarjeta_en_vivo.png`. No se escribió ni confirmó ninguna tarjeta completa.

**Pruebas:** `CardHolderTest` (9, Android), `StripeCardPresenterTest` (3, backend: titular sí, número/correo/huella nunca, limpieza y recorte).

## 10. Facturas por correo (backend)
| Compra | Correo | Adjuntos |
|---|---|---|
| Pago de cita | «Tu comprobante de pago» | comprobante **y** factura (antes solo factura) |
| Pedido de la tienda | «Tu pedido … fue entregado» | comprobante del pedido (nuevo; mismo PDF que se descarga en la app) |
| Membresía (cada cobro mensual) | «Tu factura de la membresía …» (nuevo) | factura; una sola vez por factura de Stripe |
La factura de membresía también genera el aviso en la bandeja y el push (canal pagos → pantalla Pagos). Pruebas: `InvoiceEmailsTest` (5), `PaymentReceiptEmailTest` (2).

## 11. Verificación de calidad
Backend: **800 pruebas pasan** (2966 aserciones); Pint **PASS**; Larastan de todo el proyecto **sin errores** (se bajaron 4 contadores del baseline y se quitó 1 entrada que ya no aplica). Android: pruebas unitarias completas pasan.
**Requiere desplegar `barber` a staging** (cancelaciones, push de todos los tipos, titular de la tarjeta, correos).
