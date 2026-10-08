# Demo Sprint 2 — Urban Blade (8 de octubre de 2026)

Sprint 2 «Android completo e inicio de Terraform» (28-sep al 9-oct). Presenta: Luis Enrique González Ramírez (Full Stack / Mobile).
Duración sugerida: **8–10 min**. Backend: **staging** (`https://api.urbanblade.com.mx`), verificado el 7-oct (servicios y barberos responden 200).

## 1. Checklist de la noche anterior
Estado verificado el 7-oct-2026 en un Samsung SM-S731B (Android 16), build `staging`:

| # | Verificación | Estado |
|---|---|---|
| 1 | La app compila y se instala (`installStaging`) con SDK 36 | ✅ Hecho |
| 2 | La app abre sin fallos (logcat limpio) | ✅ Hecho |
| 3 | La sesión de Recepción carga Inicio, Citas, Más y Cuenta con datos reales | ✅ Hecho (capturas en esta carpeta) |
| 4 | API de staging responde (`/services`, `/barbers`) | ✅ Hecho |
| 5 | Pruebas unitarias JVM pasan | ✅ Hecho |
| 6 | Celular cargado (≥ 80 %) y **modo No molestar apagado** (hoy estaba en silencio, y no se oiría el push) | ⬜ Tuyo |
| 7 | Wi-Fi del salón probado o **hotspot del celular** como plan B | ⬜ Tuyo |
| 8 | Cerrar Spotify y otras apps (en las capturas aparece una pestaña de música) | ⬜ Tuyo |
| 9 | Activar «Mostrar toques» y poner brillo alto | ⬜ Tuyo |
| 10 | Push probado desde Firebase (citas, pagos y pedidos llegaron y abren su pantalla) | ✅ Hecho 7-oct; repetir una vez antes de entrar |
| 10b | Chrome con la consola de Firebase abierta y sesión iniciada; token del celular visible en el diálogo de prueba | ⬜ Tuyo |
| 11 | Plan B: video o capturas de cada paso si falla la red (se pueden grabar con la grabación de pantalla del celular) | ⬜ Tuyo |
| 12 | Web abierta en `/system` (mapa 3D) en el navegador de la laptop | ⬜ Tuyo |
| 13 | Tener a mano un segundo usuario (cliente) por si piden ver otro rol | ⬜ Tuyo |

Comando para reinstalar la app si hace falta: `gradle :app:installStaging` desde `UrbanBladeMobile`.

## 2. Guion (paso a paso)

**0:00 – Apertura (30 s)**
> «Urban Blade es una plataforma de barberías: API Laravel + MongoDB, web en Nuxt y app Android nativa en Kotlin con Compose. En este sprint cerramos la parte Android y arrancamos infraestructura como código.»

**0:30 – App Android: Inicio de Recepción (1 min)**
- Mostrar *Hola, kike* → tarjeta «Hoy en la barbería» (citas, cobrado, clientes nuevos).
- Señalar el **resumen automático del día** y «Requiere tu atención» (6 productos con stock bajo).
- Idea clave: *la pantalla cambia según el rol*.

**1:30 – Citas (1.5 min)**
- Pestaña **Citas**: filtros (Requieren atención, Hoy, Próximas, Todas) y vista Lista / Calendario.
- Mostrar una cita *Confirmada* con el estado «Recordatorio por enviar».
- Idea clave: **T142/T143**, esos recordatorios salen como notificación push (Firebase Cloud Messaging).

**3:00 – Notificaciones push (2 min)** — ya ensayado el 7-oct (ver `PRUEBAS_NOTIFICACIONES_PUSH.md`)
- En Chrome (laptop) abrir la consola de Firebase ya preparada: 6 borradores DEMO 1–6. Seguir `GUIA_FIREBASE_DEMO.md` (⋮ → Editar → Enviar mensaje de prueba → marcar el token → Probar).
- Enviar **DEMO 2 (Pago recibido)** con la app en segundo plano, mostrar la notificación en el celular y tocarla: abre Pagos. Luego **DEMO 3 (Pedido)** con la app abierta.
- Mensaje clave: 14 tipos de aviso en 6 canales; cada uno abre su pantalla; en pantalla de bloqueo no se ve el contenido.
- Si falla: mostrar el video grabado. No improvisar.

**Cobro con tarjeta (1 min)** — cuenta de **cliente**: Reservar → elegir servicio, día y hora → paso 4 **Pago → Tarjeta**, deslizar el carrusel 3D (Visa plata, segunda tarjeta y «Otra tarjeta») y en «Otra tarjeta» escribir el **nombre del titular** y los primeros dígitos: la tarjeta se dibuja en vivo (marca, número, titular y vencimiento). **No presiones «Reservar»** si no quieres crear una cita. Si quieres cobrar de verdad, usa la tarjeta de prueba de Stripe `4242 4242 4242 4242`, cualquier fecha futura y CVC.

**4:00 – Más herramientas (1.5 min)**
- Módulos: Servicios y barberos, Notificaciones, Analítica, Muro de Inspiración, **Bladebot** (asistente con IA), Pagos.
- Abrir **Bladebot** y hacer una pregunta corta («¿Qué servicios tienen?»).

**5:30 – Cuenta y seguridad (1 min)**
- Cuenta → Datos / **Seguridad** / Ajustes. Correo verificado.
- Idea clave: **TT01**, los tokens ahora expiran (`expires_at`) y, al vencer, la app muestra «Tu sesión venció. Inicia sesión de nuevo» (pantalla de bienvenida).
- Además: el token de sesión no se respalda en la nube (reglas de backup), y el release usa R8.

**6:30 – Web: mapa 3D del sistema (1 min)**
- Abrir `/system` en la laptop: mapa 3D con three.js alimentado por los datos del admin (**TT38**).

**7:30 – Infraestructura y siguiente paso (1 min)**
- Terraform: estructura, proveedores AWS/Atlas, variables y outputs, estado remoto en S3 — **en curso** (T112–T116).
- Publicación en Google Play: la app ya apunta a API 36, firma de release y guarda de publicación; la ruta está en `docs/PUBLICAR_PLAY_STORE.md`.

**8:30 – Cierre y preguntas.**

## 3. Estado real del Sprint 2 (para responder preguntas)

| Tarea | Responsable | Estado |
|---|---|---|
| T142 Push FCM en Android | Luis | Completada |
| T143 Tokens de dispositivo y recordatorios | Alan | Completada |
| TT01 `expires_at` en tokens | Alan | Completada |
| TT02 Pruebas de token vencido | Miguel | Completada |
| TT38 Mapa 3D `/system` | Luis | Completada |
| T112–T116 Terraform (estructura, proveedores, variables/outputs, estado remoto, workspaces) | Alan, María, Luis, Elías | **En curso** |
| TT03 Aviso de sesión expirada | Elías | Android listo; falta la web |
| TT04 Mensaje genérico en forgot-password | Alan | Pendiente |
| TT05 Token de recuperación de 15 min | María | Pendiente |
| TT06 Pruebas de recuperación | Miguel | Pendiente |
| T145 Pruebas instrumentadas Android | Miguel | Pendiente |
| T084 Pruebas en dispositivos | Miguel | Pendiente |
| TT35 Matriz RACI y comunicación | Elías | Pendiente |

## 4. Preguntas probables
- **¿Por qué no está T145?** Falta la carpeta `androidTest`; hay pruebas JVM de ViewModels y contrato que sí pasan.
- **¿Qué pasa si se roba un token?** Expira (TT01); falta endurecer la recuperación de contraseña (TT04/TT05).
- **¿Cuánto cuesta publicar?** $25 USD, pago único; sin comisión de Google (los cobros son por Stripe).

## 5. Detalles menores vistos en el celular
- En **Citas**, el botón flotante «Nueva» queda parcialmente tapado por la mascota de Bladebot (esquina inferior derecha). No afecta la demo; evitar tocar ahí o mover la mascota.
- El estado «Recordatorio por enviar» sigue visible en citas futuras: es lo esperado hasta que se dispare el recordatorio.
