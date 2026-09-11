# UrbanBlade Mobile — Android Studio

Aplicación Android nativa para el ecosistema **UrbanBlade**, construida sobre los contratos reales de:

- `KikeGonRam/frontend_Urbanblade` — referencia funcional/UX.
- `KikeGonRam/barber` — API Laravel 13 + MongoDB + Redis.

## Stack

- Kotlin
- Jetpack Compose + Material 3
- MVVM
- Navigation Compose
- Retrofit + OkHttp + Gson
- DataStore para Bearer Token
- Coil para imágenes

## Funciones implementadas

### Todos los usuarios autenticados
- Login, registro, recuperación de contraseña y restauración de sesión.
- Dashboard por rol.
- Perfil y cambio de datos.
- Catálogo de servicios/barberos.
- Analítica.
- Notificaciones.
- Muro social.
- Bladebot.

### Cliente
- Mis citas.
- Reservar citas usando `/availability/slots`.
- Cancelar citas.
- Tienda de productos.
- Carrito local seguro: al backend solo se envían `product_id` + `cantidad`.
- Crear/cancelar pedidos.
- Historial de pagos/facturas.

### Recepcionista
- Citas y operación.
- Pedidos: cancelar y entregar con efectivo/tarjeta/transferencia.
- Pagos y transferencias pendientes: aprobar/rechazar.
- Clientes.
- Inventario.
- Movimientos / bajo stock mediante API.
- Corte de caja.

### Barbero
- Agenda propia.
- Portafolio.
- Horario.
- Perfil y dashboard.

### Administrador
- Todo lo operativo permitido por API.
- Clientes, inventario, pagos, pedidos.
- Reportes y logs.
- Campañas, sorteos y reseñas.
- Usuarios y configuración.
- Métricas administrativas.
- Insights/predicciones.
- Estado del sistema.

### Ingeniero
- Vista de solo lectura para reportes, logs, métricas, insights y estado del sistema.
- La app no expone acciones destructivas para este rol.

## URL del backend

Para el emulador de Android Studio la app usa por defecto:

```text
http://10.0.2.2:8000/api/v1/
```

`10.0.2.2` apunta al `localhost` de tu computadora desde el emulador Android.

Para un celular físico cambia `API_BASE_URL` en `app/build.gradle.kts` por la IP LAN o dominio HTTPS del backend. Ejemplo:

```kotlin
buildConfigField("String", "API_BASE_URL", "\"https://api.tudominio.com/api/v1/\"")
```

## Abrir

1. Descomprime el proyecto.
2. Android Studio → **Open** → selecciona `UrbanBladeMobile`.
3. Espera la sincronización Gradle.
4. Levanta `barber` en el puerto 8000.
5. Ejecuta un emulador Android.
6. Run `app`.

## Seguridad

- El token se guarda con DataStore y se adjunta como `Authorization: Bearer`.
- El backend sigue siendo la autoridad de permisos y roles.
- La app nunca envía precios de productos para checkout; Laravel recalcula precio/stock.
- La disponibilidad de citas se obtiene del servidor antes de reservar.
- No se confía en controles visuales de la app como autorización real.

## Estado de Stripe

El contrato `/payments/stripe-intent` está modelado en la capa de red. El historial de pagos y la revisión de transferencias funcionan contra la API. Para capturar tarjeta directamente dentro de Android debe configurarse el SDK oficial de Stripe con la clave publicable del entorno; no se incrusta ninguna clave secreta en el proyecto.

## Arquitectura

```text
Compose UI
   ↓
ViewModels
   ↓
Repositories
   ↓
Retrofit / OkHttp
   ↓
Laravel /api/v1
   ↓
MongoDB + Redis + Stripe
```

Consulta `docs/ARQUITECTURA.md` para más detalle.
