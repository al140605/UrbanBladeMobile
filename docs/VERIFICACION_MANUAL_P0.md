# Verificación manual P0 — códigos de error y red lenta

Checklist para ejecutar en Android Studio con emulador (este entorno de desarrollo
asistido no tiene `adb`/emulador disponible, así que estos pasos no fueron ejecutados
por el agente — quedan pendientes de que el equipo los corra).

Cómo simular cada código sin depender de que Laravel lo produzca naturalmente: usar el
proxy de Charles/mitmproxy, o temporalmente devolver el código deseado desde un
endpoint de prueba en `barber` (revertir después), o desconectar la red a mitad de una
petición para el caso de "red lenta"/timeout.

## Login (`LoginScreen`, `AuthViewModel.login`)

| Código | Cómo forzarlo | Resultado esperado en la UI |
|---|---|---|
| 401 | credenciales incorrectas reales | "Credenciales incorrectas o sesión vencida." |
| 403 | usuario sin verificar correo (ver `EnsureEmailIsVerified` en `barber`) | "Debes verificar tu correo para iniciar sesión." |
| 422 | enviar email vacío/formato inválido | "Revisa los datos capturados." |
| 429 | 6+ intentos fallidos seguidos (throttle de Laravel) | "Demasiados intentos. Intenta de nuevo más tarde." |
| 503 | `php artisan down` en `barber` | "UrbanBlade está en mantenimiento." |
| Red lenta | Network Link Conditioner / throttling en el emulador a <50kbps | El botón muestra el spinner de `busy` sin trabarse; tras el timeout de OkHttp (20s conexión / 30s lectura, `AppContainer.kt`) debe mostrar un mensaje de error, no quedarse cargando indefinidamente |

## Registro (`RegisterScreen`, `AuthViewModel.register`)

- 422 con email ya registrado → mensaje del backend visible vía `UrbanErrorBanner`.
- 429 tras múltiples registros seguidos → mismo mensaje de "Demasiados intentos".

## Google Sign-In (`GoogleAuthHelper`, ambas pantallas)

- Cancelar el selector de cuentas → "Selecciona una cuenta de Google para continuar o inicia con correo."
- Sin Google Play Services / cuenta no disponible → "No pudimos abrir Google. Revisa tu conexión o intenta de nuevo."
- Requiere `GOOGLE_CLIENT_ID` real en `local.properties` para probar el flujo completo (ver README).

## Creación de cita (`BookingScreen`, `BookingViewModel.create`)

- 422 si el horario ya no está disponible (carrera con otro cliente) → error visible antes de "confirmar" cambios de estado en la UI.
- 503 con `php artisan down` → error de mantenimiento visible.

## Carga de dashboard (`DashboardScreen`)

- Red lenta → el `busy`/`loading` state no debe dejar la pantalla en blanco indefinidamente; confirmar que aparece un estado de carga/skeleton, no un crash.
- 401 con token expirado/revocado → la app debe regresar a `AuthState.Guest` (ver `AuthRepository.restoreSession`, que limpia la sesión en 401) y mostrar la pantalla de login, no un error genérico.

## Insets/teclado (P0 #1, complementa las pruebas automatizadas)

Confirmar en un dispositivo/emulador con teclado en pantalla que ningún campo queda
tapado al enfocarlo, en particular:

- `ClientsScreens.kt` → diálogo "Nuevo cliente" (4 campos, el de contraseña es el más bajo)
- `InventoryScreens.kt` → diálogo "Nuevo/editar producto" (7 campos, el peor caso)
- `UsersScreen.kt` → diálogo de usuario del sistema
- `CampaignsScreen.kt` → diálogo "Nueva campaña" (el campo "Mensaje" es multilínea)
- `BarberPortfolioScreen.kt` → diálogo "Subir trabajo"
- `ReportsScreen.kt` → campos de rango de fechas
- Pantallas ya cubiertas por `AuthShell` (login/registro/recuperar contraseña) — confirmar que siguen funcionando igual después de este cambio, no solo las nuevas.

## Rebanada de cliente, parte 1 — navegación, catálogo/reserva, citas, wallet

Igual que arriba: sin `adb`/emulador en este entorno, ninguno de estos flujos fue
ejecutado por el agente. Checklist para Android Studio.

### Navegación por rol

- Iniciar sesión como `cliente`: la barra inferior debe mostrar Inicio, Explorar, Mis
  citas, Wallet, Perfil (5 items).
- Iniciar sesión como `administrador`/`recepcionista`/`barbero`: la barra debe seguir
  igual que antes (Inicio, Citas, Más, Perfil) — confirmar que NO cambió para estos roles.

### Catálogo como invitado + selección preservada

- Cerrar sesión (o instalar limpio). La app debe abrir directo en el catálogo (no en
  login), con el aviso "Explora como invitado…" visible.
- Tocar un servicio específico → debe pedir iniciar sesión/crear cuenta.
- Iniciar sesión (o registrarse) → debe navegar directo al wizard de reserva con ese
  servicio ya preseleccionado en el paso 1 (confirmar que `PendingBooking` se consumió
  correctamente y no persiste en una sesión posterior).
- Repetir tocando un barbero específico en vez de un servicio.
- Desde el catálogo de invitado, botón "Ya tengo cuenta, iniciar sesión" debe llevar a
  login sin pasar por ninguna selección pendiente.

### Wizard visual de reserva

- Los 4 pasos (servicio → profesional → calendario → revisión) deben verse uno a la vez,
  con la barra de progreso avanzando y "Atrás" funcionando en cada paso.
- Elegir un día sin horarios disponibles → debe aparecer el banner "Sin horarios este
  día" con el botón "Unirme a la lista de espera"; confirmar que el botón desaparece
  (o cambia a "ya anotado") tras usarlo.
- Confirmar una cita real de punta a punta y verificar que aparece en "Mis citas".

### Reagendar, cancelar por política, lista de espera

- En "Mis citas", una cita `pendiente`/`confirmada` y futura debe mostrar "Reagendar" y
  "Cancelar cita"; una `completada`/`cancelada` no debe mostrar ninguno de los dos.
- Reagendar: la hoja debe precargar el barbero actual de la cita, permitir cambiar
  fecha/hora, y mostrar el error real del servidor si la cita ya no es gestionable.
- Cancelar una cita dentro de la ventana de política (< horas configuradas en
  `BarbershopSetting.politica_cancelacion`) → debe mostrar el mensaje exacto de Laravel
  ("Solo puedes cancelar con al menos N horas de anticipación."), no un mensaje genérico.
- Confirmar que "Mi lista de espera" aparece en Mis citas solo para el rol cliente, y que
  "Salir" realmente quita la entrada tras recargar.

### Wallet

- Cargar la pestaña Wallet como cliente sin membresía/paquetes/gift cards/referidos
  reales todavía — debe mostrar los estados vacíos ("Sin membresía activa", etc.) sin
  errores, no una pantalla en blanco o un crash.
- Con datos reales de prueba (crear un paquete/membresía/gift card manualmente en
  `barber` para un cliente de prueba), confirmar que los montos, usos restantes y fechas
  se ven correctos.
- "Cancelar membresía" → confirmar el diálogo, verificar que el estado pasa a
  "se cancelará al finalizar el periodo" sin quitar el acceso inmediato.
- Botón de copiar código de referido → confirmar que efectivamente copia al portapapeles.
