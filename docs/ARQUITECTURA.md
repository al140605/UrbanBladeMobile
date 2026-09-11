# Arquitectura — UrbanBlade Mobile

## Flujo

```text
┌───────────────────────────────────┐
│ Android / Jetpack Compose         │
│ Screens + Navigation por rol      │
└────────────────┬──────────────────┘
                 │ StateFlow
┌────────────────▼──────────────────┐
│ ViewModels                         │
│ Auth / Citas / Store / Pedidos    │
│ Pagos / Módulos / Chatbot         │
└────────────────┬──────────────────┘
                 │
┌────────────────▼──────────────────┐
│ Repositories                       │
│ Reglas de interacción con API     │
└────────────────┬──────────────────┘
                 │
┌────────────────▼──────────────────┐
│ Retrofit + OkHttp                  │
│ Authorization: Bearer <token>     │
└────────────────┬──────────────────┘
                 │ HTTPS / JSON
┌────────────────▼──────────────────┐
│ Laravel 13 — /api/v1              │
│ mobile.auth + roles + permisos    │
└─────────┬───────────────┬─────────┘
          │               │
      MongoDB           Redis
          │               │
          └──── Stripe / servicios externos
```

## Separación de responsabilidades

- **Screens**: representación visual, inputs y navegación.
- **ViewModels**: estado de pantalla, loading/error y ejecución de casos de uso.
- **Repositories**: punto de acceso al dominio remoto.
- **UrbanBladeApi**: contrato HTTP con Laravel.
- **SessionManager**: persistencia del Bearer token.
- **Laravel**: validación, autorización, precios, stock, estados de citas y reglas de negocio.

## Roles

| Rol | Android |
|---|---|
| cliente | citas, tienda, pedidos, pagos, perfil, social |
| recepcionista | operación, clientes, inventario, pagos, pedidos, caja |
| barbero | agenda, portafolio, horario, perfil |
| administrador | operación + gestión + análisis + sistema |
| ingeniero | lectura de reportes/logs/métricas/sistema |

La visibilidad de UI reduce ruido, pero **no reemplaza** los middleware de Laravel.

## Pedidos

El carrito existe en memoria del cliente Android. En checkout se manda:

```json
{
  "items": [
    { "product_id": "...", "cantidad": 2 }
  ]
}
```

No se manda precio; Laravel vuelve a consultar el producto y valida stock.

## Citas

Antes de crear una cita se consulta:

```text
GET availability/slots?barber_id=...&service_id=...&date=YYYY-MM-DD
```

Después se crea con:

```text
POST appointments
```

El backend conserva la validación final ante condiciones de carrera.

## Producción

- Cambiar la URL base a HTTPS.
- Desactivar tráfico cleartext en `AndroidManifest.xml`.
- Configurar certificado y dominio reales.
- Integrar FCM si se desea push nativo además del backend actual.
- Configurar Stripe Android con clave publicable; nunca incluir secret keys.
