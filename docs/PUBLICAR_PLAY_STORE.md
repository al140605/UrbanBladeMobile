# Publicar UrbanBlade en Google Play — checklist

Costo: **$25 USD**, pago único (cuenta de Play Console). Sin comisión de Google: los cobros son servicios y productos físicos vía Stripe, no Play Billing.

## 1. Ya resuelto en el código (2026-10-06)
- [x] `compileSdk` y `targetSdk` en **36** (Play lo exige en apps nuevas desde 31-ago-2026). Pruebas unitarias pasan.
- [x] `compileSdk` 36 compila con AGP 8.13.2 (el del repo). Si se migra a AGP 9, hay que subir `firebase-perf` a 2.0.2: la 2.0.1 no carga con AGP 9.
- [x] Firma de release por variables (`UPLOAD_*`), sin keystore dentro del repo.
- [x] **Guarda de release**: `bundleRelease` (el AAB para Play) falla con mensaje claro si falta algo de la sección 2; `assembleRelease` del CI no se ve afectado.
- [x] Token de sesión excluido de backup y device-transfer; R8 y shrinkResources activos; `google-services.json` presente.
- [x] Página de privacidad en la web (`frontend-urban/app/pages/privacidad.vue`).

## 2. Pendiente tuyo antes de generar el AAB
Agregar a `local.properties` (no se versiona):

```
RELEASE_API_BASE_URL=https://api.urbanblade.com.mx/api/v1/
GOOGLE_CLIENT_ID=<Web Client ID de producción>
STRIPE_PUBLISHABLE_KEY=pk_live_...        # hoy es pk_test_
UPLOAD_STORE_FILE=C:\\ruta\\fuera\\del\\repo\\urbanblade-upload.jks
UPLOAD_STORE_PASSWORD=...
UPLOAD_KEY_ALIAS=upload
UPLOAD_KEY_PASSWORD=...
```

Crear el keystore (respaldarlo en 2 lugares; sin él no se puede actualizar la app):

```
keytool -genkeypair -v -keystore urbanblade-upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
```

Generar el AAB: `gradle :app:bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`.

## 3. Login con Google en producción
Con Play App Signing, Google re-firma la app. Después de subir el primer AAB, copiar la huella **SHA-1 de "Firma de la app"** (Play Console → Integridad de la app) y registrarla en Firebase (proyecto `barber-c6b3a`) y en el cliente Android de Google Cloud. Sin esto el login falla solo en la versión de Play.

## 4. Play Console
- [ ] Cuenta creada y verificación de identidad completa.
- [ ] Ficha: nombre, descripción corta/larga, ícono 512×512, gráfico 1024×500, ≥2 capturas de teléfono.
- [ ] URL de política de privacidad (`/privacidad` en el dominio público).
- [ ] Seguridad de los datos: declarar Firebase Analytics, Crashlytics, Performance, FCM, Stripe, login con Google, datos de cuenta/citas.
- [ ] Eliminación de cuenta: URL web además del flujo en la app.
- [ ] Clasificación de contenido y público objetivo (no dirigido a menores).
- [ ] Acceso a la app: usuario demo para el revisor.
- [ ] Prueba cerrada: **12 testers, 14 días seguidos** (cuenta personal) antes de pedir producción.

## 5. Antes de enviar
- [ ] Rotar credenciales de `_seguridad/` (`barber/docs/PLAN_ROTACION_CREDENCIALES.md`).
- [ ] Probar el AAB en un teléfono real contra la API de producción (login, reserva, pago, push).
- [ ] Subir `versionCode` en cada nueva versión.
