---
name: urbanblade-mobile-ui-patrones
description: Patrón visual y módulos compartidos de UrbanBladeMobile (Compose). Consultar antes de crear o rediseñar cualquier pantalla de la app (inicio de un rol, módulo, Mi cuenta, estados de vacío/error) para reutilizar UrbanPattern, el kit de cuenta y las mascotas en vez de inventar componentes nuevos.
---

# UrbanBladeMobile — patrón de UI y módulos compartidos

La app sigue **un solo lenguaje visual**: el de la bienvenida, el login, el registro y el
inicio del administrador. Toda pantalla nueva o rediseñada se arma con estas piezas. Si
falta una, se agrega aquí (en `ui/components` o `ui/account`) y se documenta en esta
skill, no se crea una variante local en la pantalla.

Complementa a `urbanblade-mobile-android` (capas, seguridad, build) y a
`urbanblade-historias-sprint` (cada cambio va ligado a una tarea; el trabajo de UI
actual es **TT32, HT-12**).

## 1. Piezas del patrón (`ui/components/UrbanPattern.kt`)

| Pieza | Cuándo usarla |
|---|---|
| `UrbanPageHeader(title, subtitle, eyebrow, trailing)` | Encabezado de toda pantalla raíz (pestaña de la barra inferior). Eyebrow dorado en mayúsculas = rol o área; título serif. |
| `UrbanModuleScreen(eyebrow, title, subtitle, onBack, onRefresh)` | Módulos a los que se entra desde el inicio o "Más" (tienen volver). El título vive en el encabezado, no en la barra. |
| `UrbanHeroCard` + `UrbanHeroLabel` + `UrbanHeroStat` | El dato principal de la pantalla sobre la foto de la barbería con borde dorado. Una por pantalla, arriba. |
| `UrbanAttentionRow(icon, text, subtitle, tone, onClick)` | "Requiere tu atención": algo que revisar, con el tono de su gravedad (Danger, Warning, Success, Gold). Sin `onClick` es informativa. |
| `UrbanStatStrip` | 3–4 números en una sola tarjeta (en vez de varias `UrbanMetricCard` angostas). |
| `UrbanModuleTile` / `UrbanModuleGrid` | Accesos rápidos en dos columnas. |
| `UrbanPillTabs(tabs, selected, onSelect)` | 2 o 3 pestañas en píldora dorada (Mi cuenta, Mis citas del cliente). No usar `FilterChip` sueltos para cambiar de vista. |
| `UrbanMascotState(kind, title, subtitle, actionLabel, onAction)` | Estados vacíos y de error. |

Otras piezas compartidas:

- `UrbanTextField` y `PasswordStrengthMeter` (`UrbanAuthKit.kt`): **todo** campo de
  texto, no solo el de acceso. Etiqueta arriba, error junto al campo.
- `UrbanDateField` (`UrbanDateField.kt`): toda fecha. Entrega ISO `AAAA-MM-DD` y muestra
  "15 de mayo de 1995". `onlyPast = true` para nacimientos. Nunca pedir fechas escritas
  a mano.
- `UrbanPrimaryButton` / `UrbanOutlineButton`: el botón dorado sigue dorado mientras
  carga aunque el formulario esté incompleto (commit ccfb7c8).
- `UrbanFormat`: fechas (`date`, `dateShort`, `dateLong`, `monthYear`), horas y conteos
  ("1 cita" / "3 citas"). No formatear fechas a mano en la pantalla.

## 2. Mi cuenta (`ui/account/`) — compartida por los cinco roles

Diseño elegido por el usuario el 24-sep-2026: **tarjeta de miembro con pestañas (B) +
secciones en acordeón (C)**.

| Archivo | Contenido |
|---|---|
| `AccountKit.kt` | `AccountMemberCard` (tarjeta héroe con foto que se toca para cambiarla), `AccountAccordion` (sección plegable con resumen visible cerrada), `AccountSwitchRow`, `accountRoleLabel()`. |
| `AccountSections.kt` | `PersonalDataForm`, `ChangePasswordForm`, `NotificationChannels`, `ThemePicker`, `AccountNoticeBanner`. |
| `AccountScreen.kt` | La pantalla: pestañas Datos / Seguridad / Ajustes. |
| `ui/viewmodel/AccountViewModel.kt` | Estado único `AccountState`; avisos por sección (`AccountNotice`). |
| `data/model/AccountModels.kt` | Modelos de cuenta (separados de `Models.kt`). |

Qué cambia por rol (y nada más):

- **Cliente**: la tarjeta muestra nivel y puntos (`dashboard.loyalty`) y su descuento
  activo; Datos incluye teléfono, cumpleaños y sexo; Ajustes incluye Bladebot.
- **Personal** (admin, recepción, barbero, ingeniero): la tarjeta muestra "Miembro desde"
  y si el correo está verificado; Datos es solo nombre y correo (el backend ignora los
  campos de cliente para ellos).

Endpoints: `GET/PUT profile`, `POST profile/avatar` (multipart `avatar`, JPG/PNG/WebP
≤ 4 MB), `PUT profile/password`, `GET/PATCH notifications/preferences` (el PATCH hace
merge: mandar solo el canal que cambió). Tras cambiar nombre, correo o foto se llama
`AuthViewModel.refreshUser()` para que el resto de la app lo vea sin cerrar sesión.

Avisos: la app muestra solo los canales que hoy entregan algo (**en la app, correo,
promociones**). `sms`, `whatsapp` y `push` existen en el backend pero no se ofrecen
hasta que funcionen de punta a punta (push llega con T143).

Pendiente de decidir (no implementado): barbero favorito (`PUT profile/favorite-barber`
existe, pero `GET profile` no devuelve el actual) y eliminar cuenta (`DELETE profile`,
irreversible: requiere confirmación del PO).

Para agregar una sección nueva a Mi cuenta: un `AccountAccordion` en la pestaña que le
corresponda, su formulario en `AccountSections.kt` y su `AccountSection` para los avisos.

## 3. Mis citas del cliente

`ClientAppointmentsScreen.kt` (solo para quien es únicamente cliente; el personal usa
`AppointmentsScreen.kt`). Pestañas Próximas / Historial; la siguiente cita va en la
tarjeta héroe con Pagar, Reagendar y Cancelar a la vista. Reutiliza `RescheduleSheet`,
`CheckoutSheet`, `WaitlistCard` y `AppointmentDateRail` de `AppointmentsScreen.kt`
(son `internal` para eso): no duplicarlas. "Nueva" va en el encabezado, nunca como botón
flotante sobre la lista.

## 4. Navegación del cliente (propuesta A, 25-sep-2026)

Cuatro pestañas: **Inicio · Reservar · Mis citas · Cuenta**. Reglas que la sostienen:

- El título de cada pantalla es el nombre de su pestaña (Reservar, Mis citas, Cuenta).
- Un solo verbo para reservar: «Reservar» / «Reservar cita» / «Reservar de nuevo». Nada de
  «Nueva», «Agendar» ni «Explorar».
- Cada cosa en un solo lugar: Beneficios (antes Wallet), Mis facturas y Mis pedidos en Cuenta
  → «Tu actividad»; Muro y Tienda en Inicio → «Descubre»; Bladebot en Inicio.
- Pantallas que no son pestaña llevan `UrbanTopBar("", onBack)`.
- Reservar son cuatro pasos (Servicio · Horario · Extras · Pago) con el total de la visita fijo
  abajo. Los productos de Extras se pagan aparte en el salón (pedido ligado a la cita). El
  método de pago se elige con tres mosaicos (efectivo, transferencia, tarjeta) y debajo solo
  el detalle del elegido. El estado del asistente va en `rememberSaveable`.
- Comprobantes (Mis facturas): se **descargan** con `core/media/ReceiptDownloads` a
  Descargas/UrbanBlade y se abren en el visor de PDF. Nunca abrir la liga firmada de S3 en
  el navegador.

## 5. Mascotas y tono

- **Bruno**: errores y datos técnicos (`UrbanStateKind.ERROR`).
- **Nava**: vacíos y "todavía no hay nada" (`UrbanStateKind.EMPTY`).
- **Bladebot**: bienvenida, espera, éxito y ayuda (asistente).
- No agregar mascotas nuevas ni dibujarlas; usar los assets de `res/drawable`.

Textos: español de México, frases cortas, sin tecnicismos ("No pudimos abrir tu
cuenta", no "Error 500"). En un 422 mostrar el `message` del servidor
(`HttpException.serverMessage()`), que ya explica qué falló.

## 6. Eyebrow por rol

`Administrador`, `Recepción`, `Barbero`, `Ingeniero`, `Cliente` (usar
`accountRoleLabel(user.roles)`; si alguien tiene varios roles gana el de más
responsabilidad).

## 7. Lecciones aprendidas (no repetirlas)

1. **Leer el backend antes de escribir un texto o un dato.** Varias veces un mensaje
   prometía algo que la API no hacía (p. ej. "pagos pendientes" eran citas completadas
   sin cobro; la lista de espera avisa a todos, no solo al primero).
2. **Nunca inventar datos en la UI.** Si la API no lo da, no se muestra o se pide el
   endpoint (cambio aditivo en `barber`, con prueba).
3. **Probar cada botón hasta el final en el S25** (`R5GL44QP8TT`), con la cuenta del
   rol correcto. Una pantalla que compila no está terminada.
4. **No tocar trabajo ajeno sin commit.** Antes de editar, `git status`; si el archivo
   tiene cambios de otra persona, poner lo nuevo en un archivo aparte (así nació
   `AccountModels.kt`).
5. **La tarjeta héroe sigue el tema**: su velo usa `UrbanColors.Background` (crema en
   "Libreta"), así que dentro de ella se usan `UrbanColors.Ink` / `Muted` como en cualquier
   tarjeta. Nunca colores fijos claros ni un velo negro fijo: en el tema claro el texto se
   perdía.
6. **Un botón flotante no debe tapar acciones** de la última tarjeta: dejar
   `contentPadding` inferior suficiente.
7. **Un botón que cambia de función en el mismo lugar** («Continuar» → «Reservar») se
   activa un momento después de cambiar, para que un doble toque no confirme nada.
8. **Formularios de Stripe** (`CardMultilineWidget`) heredan colores del tema oscuro: fijar
   texto, pista y acento (`styleCardWidget`) o salen blancos sobre blanco.
9. **Validar** con `gradle :app:testDebugUnitTest` e instalar con `:app:installDebug`.
   La IA no hace commit ni push; entrega el mensaje en español con el ID de la tarea y
   sin línea `Co-Authored-By`.

## 8. Kit de controles, dashboards y gráficas (plan del 26-sep-2026)

Decisión del usuario: **kit propio** inspirado en shadcn/ui, Untitled UI y Tremor, igual
que en la web (`frontend-urban/.claude/skills/urbanblade-ui-kit`, mismo catálogo y mismas
fases). Sin librerías de UI nuevas; todo con `UrbanColors` y Material 3.

**Regla de oro:** el kit cambia cómo se ve, nunca lo que se manda. `BookingPayMethod`
(`EFECTIVO`, `TRANSFERENCIA`, `TARJETA`), los `Boolean` de activo/inactivo y los
request de Retrofit quedan idénticos.

| Pieza (`ui/components/UrbanControls.kt`) | Fase | Reemplaza |
|---|---|---|
| `UrbanSwitch` / `UrbanSwitchRow(title, subtitle, checked, onCheckedChange)` | Hecho (26-sep) | Los 9 `Switch` sueltos: `AccountKit`, `AdminModuleScreens`, `BarbersAdminScreen` (2), `OffersAdminScreen` (2), `ServicesAdminScreen` (2), `SettingsScreen`. Fila completa tocable, pista dorada encendida, `Role.Switch` para TalkBack. |
| `UrbanChoiceTiles(options, selected, onSelect)` con `UrbanChoice(value, title, subtitle, icon)` | Hecho (26-sep) | Los mosaicos de la "Propuesta A" (25-sep) de `BookingPayment` pasaron al kit sin cambiar su aspecto, y ahora también se usan al pagar una cita en `AppointmentsScreen` (antes eran dos botones sin estado seleccionado). Los `FilterChip` de propina y filtros se quedan (son filtros, no métodos). |
| `UrbanStatCard` con variación | 2 | KPI de los inicios por rol (valor + "+12 % vs mes pasado"). |
| `UrbanCharts` pulido | 2 | Mismo archivo: menos cuadrícula, barras redondeadas, colores del tema, estado vacío. Nada de datos inventados. |
| Tabla/lista compacta | 3 | Listas de pagos, inventario y clientes: fila con badge de estado y acción al final. |

Validar igual que siempre (sección 7, punto 9) y revisar cada pantalla tocada en el S25
en al menos un tema oscuro y en "Libreta".
