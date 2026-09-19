# Inventario de pantallas y componentes — base para mockups en Figma

Extraído directamente del código Compose ya construido (`ui/theme/Theme.kt`,
`ui/components/UrbanComponents.kt`, `ui/navigation/UrbanBladeRoot.kt`,
`ui/screens/*.kt`, commit `e1533b9` "interfaz completa de UrbanBlade Mobile").
El objetivo es que los mockups en Figma documenten y refinen lo que ya existe en
código — mismo tema **"Sastrería Nocturna"** que usa `frontend-urban` — no que
empiecen de cero con otra dirección visual.

## 1. Tokens de diseño (ya en código, portar tal cual a Figma)

### Color — dark theme fijo (no hay modo claro)

| Token | Hex | Uso |
|---|---|---|
| Background | `#0A0A0A` | fondo base, con gradiente sutil a `#080808`/`#0E0D09` |
| Surface | `#111111` | superficies elevadas |
| Card | `#161616` | tarjetas estándar (`UrbanCard`) |
| CardAlt | `#1F1F1F` | variante de tarjeta |
| Line | `#2C2C2C` | bordes/separadores |
| **Gold** | `#D4AF37` | color de marca/acento — CTAs, iconos activos, precios destacados |
| GoldDim | `#A8842C` | estado disabled del dorado |
| Ink | `#F2F2F2` | texto principal |
| Muted | `#9C9C9C` | texto secundario |
| Success | `#4BB983` | estados: completada, confirmada, entregado, verificado |
| Warning | `#E2A84D` | estados: pendiente, en proceso |
| Danger | `#E36D6D` | estados: cancelada, rechazado, error |
| Info | `#72A5D8` | estados neutros/informativos |

### Tipografía

- **Serif Bold** (display/headline) — `displaySmall` 36sp, `headlineLarge` 31sp,
  `headlineMedium` 27sp. Es la fuente "editorial" de títulos grandes, mismo espíritu
  que el branding web.
- **Sans-serif** (todo lo demás) — `titleLarge` 20sp Bold, `titleMedium` 16sp
  SemiBold, `bodyLarge` 16sp, `bodyMedium` 14sp, `bodySmall` 12sp, `labelLarge`/
  `labelMedium` 14sp/12sp Bold para pills y botones.

### Formas

`RoundedCornerShape`: extraSmall 8dp, small 12dp, medium 18dp, large 24dp,
extraLarge 30dp. Las tarjetas premium (`UrbanPremiumCard`) usan `large` (24dp); las
tarjetas normales (`UrbanCard`) usan `medium` (18dp); botones 15dp.

## 2. Sistema de componentes (17 componentes reutilizables — replicar como componentes de Figma con variantes)

| Componente | Qué es | Notas de diseño |
|---|---|---|
| `UrbanBladeBackground` | Fondo con gradiente vertical de 3 tonos | Contenedor raíz de cada pantalla |
| `UrbanBrandMark` | Logo: ícono tijera en caja dorada + wordmark "URBANBLADE / BARBERSHOP · MOBILE" | Variante `compact` (34dp) para topbars |
| `UrbanPageHeader` | Encabezado de pantalla: eyebrow + título (headlineMedium) + subtítulo + slot `trailing` | Patrón repetido en casi todas las pantallas |
| `UrbanSectionTitle` | Título de sección con acción opcional a la derecha | Usado para agrupar listas (ej. módulos por rol) |
| `UrbanPremiumCard` | Tarjeta con gradiente diagonal `#1B1B1B→#0D0D0D` + borde 1dp | Para KPIs y accesos rápidos — más "premium" que `UrbanCard` |
| `UrbanCard` | Tarjeta plana color `Card` + borde `Line` | Uso general (listas, módulos) |
| `UrbanMetricCard` | Icono en badge dorado + valor grande + label | KPIs del dashboard |
| `UrbanQuickAction` | Icono en badge dorado sólido + título + subtítulo + chevron | Accesos rápidos tipo lista-tarjeta |
| `UrbanStatusPill` | Pill de estado con color semántico automático (success/danger/warning/info según texto) | Estados de citas/pagos/pedidos |
| `UrbanRolePill` | Pill dorada para mostrar el rol del usuario | Perfil, "Más herramientas" |
| `UrbanAvatar` | Círculo con iniciales, fondo dorado translúcido | Placeholder de foto de perfil |
| `UrbanPrimaryButton` | Botón sólido dorado, 52dp alto, soporta ícono + loading | CTA principal por pantalla |
| `UrbanOutlineButton` | Botón outline, 50dp alto | Acción secundaria |
| `UrbanErrorBanner` | Banner rojo translúcido con ícono de error | Errores de formulario/red |
| `UrbanInfoBanner` | Banner dorado translúcido con ícono informativo | Avisos neutros |
| `UrbanEmptyState` | Ícono circular + título + subtítulo + acción opcional | Listas vacías |
| `UrbanTopBar` | AppBar con back opcional + acciones | Pantallas secundarias (no root del bottom nav) |
| `UrbanFieldLabel` / `UrbanKeyValue` | Label de campo en mayúsculas / fila label-valor | Formularios y resúmenes |

## 3. Navegación

**Bottom nav** (raíz, varía por rol):
`Inicio` → siempre · `Citas` → oculto solo si el usuario es *exclusivamente* ingeniero
· `Tienda` → solo si tiene rol `cliente` · `Más` → siempre · `Perfil` → siempre.

**"Más" (`MoreScreen`)** es el hub de módulos secundarios, agrupados por sección y
filtrados por rol — este es el mapa completo de superficie de la app:

| Sección | Módulos | Visible para |
|---|---|---|
| GENERAL | Servicios y barberos, Notificaciones, Analítica, Muro social, Bladebot | todos |
| OPERACIÓN | Pagos, Pedidos | staff (admin/recepción) **o** cliente |
| OPERACIÓN | Clientes, Inventario, Corte de caja | staff (admin/recepción) |
| BARBERO | Mi agenda, Portafolio, Mi horario | barbero |
| ANÁLISIS | Reportes, Logs, Métricas, Insights IA | admin **o** ingeniero |
| ADMIN | Campañas, Sorteos, Reseñas, Usuarios, Configuración | admin |
| SISTEMA | Estado del sistema | admin **o** ingeniero |

## 4. Inventario de pantallas por prioridad de mockup

Orden sugerido para Figma: primero el flujo que más usuarios tocan (cliente), luego
el que reduce el mayor punto de fricción operativa (staff), al final lo administrativo
(menor frecuencia de uso, ya tiene un patrón repetible una vez definido el primero).

### Fase 1 — Onboarding y cliente (mockear primero)

1. **Login** (`AuthScreens.kt`) — email + password, error banner, link a registro/olvidé contraseña.
2. **Registro** — datos básicos de cuenta.
3. **Recuperar contraseña**.
4. **Dashboard / Inicio** (`DashboardScreen.kt`) — header con avatar, fila de `UrbanMetricCard` (KPIs), accesos rápidos (`UrbanQuickAction`) a Citas/Reservar.
5. **Catálogo** (`CatalogScreen.kt`) — servicios y barberos con imagen.
6. **Reserva guiada** (`BookingScreen.kt`) — flujo por pasos (servicio → barbero → horario → confirmación).
7. **Mis citas** (`AppointmentsScreen.kt`) — resumen + tarjetas de agenda con `UrbanStatusPill`.
8. **Tienda** / **Pedidos** — catálogo de productos + carrito + historial.
9. **Perfil** (`ProfileScreen.kt`) — datos de cuenta, rol, logout.

### Fase 2 — Operación de staff (recepción/administrador)

10. **Clientes** — atención y gestión (mismo criterio "por acción, no por grupo" que ya se implementó en la web, ver `barber` commit `ffd6814`).
11. **Inventario** — stock y movimientos.
12. **Corte de caja** — resumen de turno (mismo modelo de dos fuentes de ingreso que `barber`: pagos verificados + pedidos entregados).
13. **Pagos** — historial y comprobantes.

### Fase 3 — Barbero

14. **Mi agenda**, **Portafolio**, **Mi horario**.

### Fase 4 — Administración / análisis (patrón repetible: `GenericModuleScreen`)

15. **Reportes**, **Logs**, **Métricas**, **Insights IA**, **Campañas**, **Sorteos**,
    **Reseñas**, **Usuarios**, **Configuración**, **Estado del sistema** — en código
    hoy comparten una sola pantalla genérica (`GenericModuleScreen`); en Figma alcanza
    con **un mockup de plantilla** + 2-3 variantes de contenido, no 10 pantallas
    distintas desde cero.
16. **Notificaciones**, **Analítica**, **Muro social**, **Bladebot** (chat) — generales,
    accesibles a todos los roles desde "Más".

## 5. Qué falta decidir antes de empezar en Figma

- **Fotografía real**: catálogo, portafolio y muro social necesitan placeholders de
  imagen con criterio (barbería/cortes), no solo rectángulos grises — define si se usan
  fotos de stock con licencia o assets del negocio real.
- **Iconografía**: el código usa Material Icons por defecto (`Icons.Default.*`). Si el
  mockup quiere una iconografía más "premium"/custom, es una decisión de diseño nueva,
  no algo que ya exista.
- **Tamaño de frame**: Pixel 10 Pro (el emulador que están usando) — conviene fijar un
  frame de referencia (ej. 412×917 dp / 1080×2400 px @ 3x) para que las medidas del
  mockup mapeen directo a dp en Compose.

## Próximo paso

Con el conector de Figma autorizado, se arma un archivo con: página de tokens (0),
página de componentes (1), y una página por fase de pantallas (2-5), reutilizando los
componentes de la página 1 en vez de reconstruir cada tarjeta/botón por pantalla.
