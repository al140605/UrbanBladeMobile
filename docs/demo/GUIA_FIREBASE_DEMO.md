# Guía: ejecutar las notificaciones push en la demo (Firebase Console)

Proyecto Firebase **barber-c6b3a** · Lista: https://console.firebase.google.com/project/barber-c6b3a/messaging
La consola ya tiene **6 borradores** listos. No se envía nada a usuarios: cada uno se manda como *mensaje de prueba* solo al celular de la demo.

## Los 6 borradores
| Borrador (nombre interno) | Título que se ve | Canal | Abre al tocar | Usar con cuenta |
|---|---|---|---|---|
| DEMO 1 - Recordatorio de cita (citas) | Recordatorio de tu cita | citas | Citas | Cliente |
| DEMO 2 - Pago recibido (pagos) | Pago recibido | pagos | Pagos | Cliente o Recepción |
| DEMO 3 - Pedido entregado (pedidos) | Pedido entregado | pedidos | Pedidos | Cliente o Recepción |
| DEMO 4 - Subes de nivel (fidelidad) | ¡Subiste a Oro! | fidelidad | Wallet / Beneficios | Cliente |
| DEMO 5 - Promocion (promociones) | 2x1 en cortes este viernes | promociones | Notificaciones | Cliente |
| DEMO 6 - Inventario bajo - personal (operacion) | Inventario bajo | operacion | Inventario | **Recepción/Admin** |

En la lista los borradores se muestran por su título (no por el nombre interno). El orden de la lista es el inverso: el más nuevo (Inventario bajo) arriba.

## Pasos para enviar uno (30 segundos)
1. Celular conectado a internet, app UrbanBlade instalada y con sesión iniciada (da igual si está abierta o en segundo plano). **Apaga «No molestar»**.
2. En la lista de campañas, en la fila del borrador: menú **⋮ → Editar**.
3. Botón azul **«Enviar mensaje de prueba»** (arriba a la derecha, bajo «Vista previa»).
4. En el diálogo, bajo «Usados recientemente» está el token del celular. **Marca la casilla y verifica que quede con palomita azul** (a veces el primer clic solo enfoca; si «Probar» sigue gris, vuelve a hacer clic).
5. **Probar**. En 2–5 segundos llega la notificación. Tócala: la app abre la pantalla indicada en la tabla.

## Si no llega
- «No molestar» o modo ahorro de batería extremo: apágalos.
- La casilla del token estaba sin marcar (paso 4).
- El token cambia si **desinstalas** la app o borras sus datos. Para obtener el nuevo (build staging/debug), con el celular por USB:
  ```bash
  adb shell run-as com.urbanblade.mobile cat shared_prefs/com.google.android.gms.appid.xml
  ```
  El valor largo después de `"token":"` es el token. Pégalo en el diálogo («Agrega un ID de instalación…») y presiona Enter. Reinstalar con `installStaging` (sin desinstalar) conserva el token.
- Permiso de notificaciones: Ajustes del celular → Apps → UrbanBlade → Notificaciones activadas (Android 13+).

## Qué se muestra
- Con la app **abierta**, la propia app crea el aviso (color dorado, agrupado por canal).
- Con la app **cerrada o en segundo plano**, Android lo muestra con el canal que indica Firebase (`citas`, `pagos`, …).
- En la pantalla de bloqueo solo se ve «UrbanBlade · Tienes un aviso nuevo» (privacidad).

## Importante para la presentación
- Los borradores simulan lo que enviará el backend real. Los push **del backend** (cuando alguien paga o se entrega un pedido) dependen de desplegar `barber` a staging y de que el usuario tenga **push activado** en sus preferencias (por defecto está apagado).
- Mantén la consola de Firebase abierta en una pestaña de Chrome con tu sesión iniciada antes de entrar al salón.

---

# Notificaciones en la **web local** (Nuxt + barber en Docker)

Los borradores de Firebase **solo llegan al celular Android**. En la web las notificaciones usan Web Push (VAPID) y las manda el backend.

## Qué tiene que estar activo
1. `docker compose up -d worker` en `barber`: **el worker de colas**. Estuvo apagado 12 días y sin él ningún aviso en cola sale (push, bandeja ni correo). Tras reiniciar la PC hay que volver a encenderlo. El `scheduler` se dejó apagado a propósito (manda recordatorios reales desde la base de Atlas).
2. Nuxt en `http://localhost:3000` (`npm run dev` en `frontend-urban`, o la vista previa «frontend-urban-dev»).
3. Sesión iniciada en la web y notificaciones permitidas en el navegador: interruptor de push en el encabezado o en *Notificaciones*, y aceptar el permiso de Chrome. Windows no debe estar en «No molestar» / asistente de concentración.
4. Desde esta versión, al suscribir el navegador **se enciende el canal push** automáticamente si la persona nunca eligió (antes quedaba apagado y el servidor descartaba el aviso).

## Probar un aviso sin crear datos reales
```bash
docker exec barber-app php artisan urbanblade:demo-push correo@dominio.com --tipo=pago
```
Tipos: `cita`, `pago`, `pedido`, `nivel`, `sorteo`, `cumpleanos`, `promocion`, `inventario`. Manda solo push (sin correo ni bandeja), muestra una tabla de diagnóstico (VAPID, suscripciones web, Firebase, token del teléfono) e indica qué falta si no llega.

## Límites locales
- El backend local **no tiene credenciales de Firebase** (`FIREBASE_PROJECT_ID` y `FIREBASE_CREDENTIALS_PATH` vacíos): desde local no se puede mandar push al teléfono; para eso están los borradores de Firebase, o staging si ya las tiene.
- El push web necesita el navegador abierto (puede estar minimizado) y conectado a internet.
