<div align="center">

<img src="https://raw.githubusercontent.com/KikeGonRam/frontend_Urbanblade/main/docs/assets/landing.png" alt="UrbanBlade" width="100%"/>

<br/>

# ✂️ UrbanBlade Mobile

### La experiencia UrbanBlade, ahora en Android

**Kotlin · Jetpack Compose · MVVM · Retrofit · Laravel API**

<br/>

[![Android](https://img.shields.io/badge/Android-Native-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)
[![Laravel](https://img.shields.io/badge/API-Laravel_13-FF2D20?style=for-the-badge&logo=laravel&logoColor=white)](https://laravel.com/)
[![MVVM](https://img.shields.io/badge/Arquitectura-MVVM-D4AF37?style=for-the-badge)](#-arquitectura)
[![Status](https://img.shields.io/badge/Estado-En_desarrollo-C1703D?style=for-the-badge)](#-estado-del-proyecto)

<br/>

[**🌐 Frontend Web**](https://github.com/KikeGonRam/frontend_Urbanblade)
&nbsp;&nbsp;•&nbsp;&nbsp;
[**⚙️ Backend API**](https://github.com/KikeGonRam/barber)
&nbsp;&nbsp;•&nbsp;&nbsp;
**📱 Android**

</div>

---

## ✨ Identidad visual UrbanBlade

La aplicación móvil toma como referencia el mismo lenguaje visual del frontend web: superficies oscuras, acentos metálicos, tarjetas premium, jerarquía clara y una estética inspirada en barbería clásica contemporánea.

<table>
<tr>
<td align="center" width="25%">

### 🌙 Sastrería Nocturna
![Noir](https://img.shields.io/badge/NEGRO-0A0A0A?style=for-the-badge&labelColor=161616)
![Oro](https://img.shields.io/badge/ORO-D4AF37?style=for-the-badge&labelColor=D4AF37)

**Negro + oro**

Elegante, sobrio y premium.

</td>
<td align="center" width="25%">

### ⚙️ Taller de Acero
![Grafito](https://img.shields.io/badge/GRAFITO-111317?style=for-the-badge&labelColor=1A1D22)
![Cobre](https://img.shields.io/badge/COBRE-C1703D?style=for-the-badge&labelColor=C1703D)

**Grafito + cobre**

Industrial y moderno.

</td>
<td align="center" width="25%">

### 🎩 Salón Inglés
![Verde](https://img.shields.io/badge/VERDE-0B1210?style=for-the-badge&labelColor=141C19)
![Latón](https://img.shields.io/badge/LATÓN-C9A24A?style=for-the-badge&labelColor=C9A24A)

**Verde inglés + latón**

Clásico y distinguido.

</td>
<td align="center" width="25%">

### 📒 Libreta de Barbero
![Marfil](https://img.shields.io/badge/MARFIL-F3EDE0?style=for-the-badge&labelColor=FFF8E8)
![Oro](https://img.shields.io/badge/ORO-B8860B?style=for-the-badge&labelColor=B8860B)

**Marfil + tinta + oro**

Claro, editorial y artesanal.

</td>
</tr>
</table>

> La web de UrbanBlade maneja estos cuatro temas completos mediante tokens de color. La app Android mantiene la misma dirección visual y puede evolucionar hacia paridad temática completa.

---

## 📱 ¿Qué es UrbanBlade Mobile?

**UrbanBlade Mobile** es la aplicación Android nativa del ecosistema UrbanBlade. Consume directamente la API de Laravel y adapta su navegación, pantallas y acciones al rol autenticado.

La app busca llevar al teléfono la misma experiencia funcional que ya existe en web:

**reservas · agenda · tienda · pedidos · pagos · clientes · inventario · reportes · notificaciones · analítica**

> **Principio de seguridad:** Android presenta la interfaz; Laravel conserva la autoridad sobre permisos, precios, stock, estados y reglas de negocio.

---

## 🖼️ Referencia visual del ecosistema

<table>
<tr>
<td width="50%" align="center">
<img src="https://raw.githubusercontent.com/KikeGonRam/frontend_Urbanblade/main/docs/assets/login.png" alt="Login UrbanBlade" width="100%"/>
<br/>
<sub><b>Acceso UrbanBlade</b> — estética oscura, acentos dorados y jerarquía limpia.</sub>
</td>
<td width="50%" align="center">
<img src="https://raw.githubusercontent.com/KikeGonRam/frontend_Urbanblade/main/docs/assets/dashboard-admin.png" alt="Dashboard UrbanBlade" width="100%"/>
<br/>
<sub><b>Dashboard administrativo</b> — tarjetas, KPIs y navegación modular.</sub>
</td>
</tr>
<tr>
<td width="50%" align="center">
<img src="https://raw.githubusercontent.com/KikeGonRam/frontend_Urbanblade/main/docs/assets/dashboard-cliente.png" alt="Dashboard cliente" width="100%"/>
<br/>
<sub><b>Experiencia cliente</b> — próxima cita, membresía y acciones rápidas.</sub>
</td>
<td width="50%" align="center">
<img src="https://raw.githubusercontent.com/KikeGonRam/frontend_Urbanblade/main/docs/assets/booking-modal-cliente.png" alt="Reserva UrbanBlade" width="100%"/>
<br/>
<sub><b>Reserva</b> — disponibilidad real según barbero, servicio y fecha.</sub>
</td>
</tr>
</table>

---

## ⚡ Funcionalidades principales

| | Módulo | Funcionalidad |
|:--:|---|---|
| 🔐 | **Autenticación** | Login, registro, recuperación y sesión persistente |
| 🏠 | **Dashboard** | Contenido dinámico según rol |
| 📅 | **Citas** | Consulta, creación, disponibilidad y cancelación |
| ✂️ | **Catálogo** | Servicios y barberos |
| 🛍️ | **Tienda** | Productos, carrito y checkout |
| 📦 | **Pedidos** | Historial, cancelación y entrega |
| 💳 | **Pagos** | Facturas, historial y transferencias |
| 🔔 | **Notificaciones** | Consulta y marcado como leído |
| 👤 | **Perfil** | Datos personales y sesión |
| 👥 | **Clientes** | Gestión para recepción y administración |
| 📦 | **Inventario** | Productos, movimientos y alertas |
| 📊 | **Analítica** | KPIs, reportes e insights |
| 🤖 | **Bladebot** | Asistente del ecosistema UrbanBlade |

---

## 🧭 Experiencia por rol

<table>
<tr>
<td width="33%" valign="top">

### 👤 Cliente
- Reservar citas
- Ver disponibilidad
- Tienda y carrito
- Pedidos
- Pagos y facturas
- Perfil
- Notificaciones

</td>
<td width="33%" valign="top">

### 🧾 Recepcionista
- Agenda operativa
- Gestión de clientes
- Pedidos
- Cobros
- Transferencias
- Inventario
- Corte de caja

</td>
<td width="33%" valign="top">

### ✂️ Barbero
- Agenda personal
- Citas asignadas
- Perfil profesional
- Portafolio
- Horario de trabajo

</td>
</tr>
<tr>
<td width="50%" valign="top" colspan="2">

### 🛡️ Administrador
- Control operativo completo
- Usuarios y configuración
- Clientes e inventario
- Campañas y sorteos
- Reportes y logs
- Métricas e insights
- Estado del sistema

</td>
<td width="50%" valign="top">

### 🧑‍💻 Ingeniero
- Solo lectura
- Reportes
- Logs
- Métricas
- Insights
- Estado del sistema

</td>
</tr>
</table>

> La interfaz oculta o muestra acciones según el rol, pero **Laravel siempre realiza la autorización real**.

---

## 🧰 Stack tecnológico

<div align="center">

| Capa | Tecnología |
|---|---|
| 📱 **Aplicación** | Kotlin + Jetpack Compose |
| 🎨 **UI** | Material 3 |
| 🧭 **Navegación** | Navigation Compose |
| 🏗️ **Arquitectura** | MVVM |
| 🌐 **Networking** | Retrofit + OkHttp |
| 🔐 **Sesión** | DataStore + Bearer Token |
| 🖼️ **Imágenes** | Coil |
| ⚙️ **Backend** | Laravel 13 |
| 🗄️ **Datos** | MongoDB + Redis |
| 💳 **Pagos** | Stripe |

</div>

---

## 🏗️ Arquitectura

```text
┌─────────────────────────────────────┐
│          URBANBLADE MOBILE          │
│        Jetpack Compose UI           │
└─────────────────┬───────────────────┘
                  │
                  ▼
┌─────────────────────────────────────┐
│             ViewModels              │
│       Estado + lógica de UI         │
└─────────────────┬───────────────────┘
                  │
                  ▼
┌─────────────────────────────────────┐
│            Repositories             │
│      Acceso y coordinación data     │
└──────────────┬──────────────┬───────┘
               │              │
               ▼              ▼
      ┌──────────────┐  ┌───────────────┐
      │  DataStore   │  │ Retrofit +    │
      │ Bearer Token │  │    OkHttp     │
      └──────────────┘  └───────┬───────┘
                                │
                                ▼
                    ┌─────────────────────┐
                    │ Laravel API /api/v1 │
                    └──────────┬──────────┘
                               │
                 ┌─────────────┼─────────────┐
                 ▼             ▼             ▼
             MongoDB         Redis         Stripe
```

---

## 🚀 Puesta en marcha

### 1. Requisitos

Necesitas:

- **Android Studio**
- **Android SDK API 35**
- JDK compatible con Android Studio/Gradle
- **Docker Desktop**
- Backend `barber`
- Emulador Android o dispositivo físico

### 2. Abrir la app

```text
Android Studio
→ File
→ Open
→ UrbanBladeMobile
```

Espera la sincronización de Gradle.

```text
BUILD SUCCESSFUL
```

### 3. Levantar el backend

Desde el repositorio `barber`:

```powershell
docker compose up -d --build
```

Prueba:

```text
http://localhost:8000/api/v1/services
```

---

## 🌐 Conexión con la API

La URL se encuentra en:

```text
app/build.gradle.kts
```

`API_BASE_URL` ahora se define por variante dentro de `buildTypes` (antes vivía en
`defaultConfig`, compartido sin querer entre debug y release):

### 🖥️ Emulador

```kotlin
buildTypes {
    debug {
        buildConfigField(
            "String",
            "API_BASE_URL",
            "\"http://10.0.2.2:8000/api/v1/\""
        )
    }
}
```

### Google Sign-In nativo

Antes de ejecutar la app, agrega el mismo **Web Client ID** que Laravel usa
para verificar tokens a tu `local.properties` (ese archivo está ignorado por
Git):

```properties
GOOGLE_CLIENT_ID=tu-web-client-id.apps.googleusercontent.com
```

También puede inyectarse en CI con `-PGOOGLE_CLIENT_ID=...`. No copies
`GOOGLE_CLIENT_SECRET` a Android: el secreto pertenece exclusivamente al
backend. Si el inicio de sesión muestra un error de desarrollador, confirma
en Google Cloud la configuración OAuth para el paquete Android y la huella del
certificado de firma de la variante que estás probando.

### 📱 Dispositivo físico

El celular y la computadora deben estar en la misma Wi-Fi.

En Windows:

```powershell
ipconfig
```

Ejemplo:

```text
IPv4: 192.168.100.11
```

Configura (dentro del mismo bloque `debug { }` de arriba):

```kotlin
buildConfigField(
    "String",
    "API_BASE_URL",
    "\"http://192.168.100.11:8000/api/v1/\""
)
```

Y desde Chrome en el celular verifica:

```text
http://192.168.100.11:8000/api/v1/services
```

### 🚀 Release

Los builds release **no** usan `10.0.2.2` ni HTTP: exigen una URL HTTPS real, que se
inyecta igual que `GOOGLE_CLIENT_ID` (nunca hardcodeada en el repo). Agrega a tu
`local.properties`:

```properties
RELEASE_API_BASE_URL=https://tu-dominio-real.example/api/v1/
```

O para CI: `-PRELEASE_API_BASE_URL=...`. Sin este valor, un build release compila con
un placeholder obviamente inválido (`https://PENDIENTE_CONFIGURAR.example/api/v1/`)
que falla en tiempo de ejecución en vez de apuntar accidentalmente al emulador o a
HTTP sin cifrar. El cleartext (`usesCleartextTraffic`) también quedó scoped solo a
debug (`app/src/debug/AndroidManifest.xml`) — release usa el valor seguro por defecto
de Android (`false`).

---

## 🔐 Flujo de autenticación

```text
┌──────────────┐
│    Login     │
└──────┬───────┘
       │ POST /auth/login
       ▼
┌──────────────┐
│ Bearer Token │
└──────┬───────┘
       │
       ▼
┌──────────────┐
│  DataStore   │
└──────┬───────┘
       │
       ▼
Authorization: Bearer <token>
```

El interceptor de OkHttp agrega el token automáticamente en las peticiones protegidas.

---

## 🛡️ Seguridad

- 🔒 Las contraseñas **no se almacenan** en Android.
- 🔑 El token se persiste mediante **DataStore**.
- 🧱 Laravel valida nuevamente cada permiso.
- 💰 Android no decide precios finales.
- 📦 Laravel valida stock durante el checkout.
- 📅 La disponibilidad se consulta antes de reservar.
- 💳 Las claves secretas de Stripe viven solamente en servidor.
- 🌐 Producción debe utilizar **HTTPS**.
- 👁️ Ocultar un botón en Android nunca sustituye una autorización del backend.

---

## 💳 Stripe

La aplicación contempla:

```text
POST /api/v1/payments/stripe-intent
```

El backend calcula el monto real antes de crear el `PaymentIntent`.

Para completar el flujo nativo de tarjeta en Android se debe integrar el **Stripe Android SDK** utilizando únicamente una **Publishable Key**.

---

## 📂 Estructura del proyecto

```text
UrbanBladeMobile/
│
├── app/
│   └── src/main/java/com/urbanblade/mobile/
│       ├── data/
│       │   ├── api/
│       │   ├── model/
│       │   ├── repository/
│       │   └── session/
│       │
│       ├── ui/
│       │   ├── navigation/
│       │   ├── screens/
│       │   ├── theme/
│       │   └── viewmodel/
│       │
│       ├── MainActivity.kt
│       └── UrbanBladeApplication.kt
│
├── docs/
│   └── ARQUITECTURA.md
│
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## 🔗 Ecosistema UrbanBlade

<div align="center">

| Proyecto | Tecnología | Repositorio |
|---|---|---|
| 🌐 **UrbanBlade Web** | Nuxt 4 + Vue + Tailwind | [frontend_Urbanblade](https://github.com/KikeGonRam/frontend_Urbanblade) |
| ⚙️ **UrbanBlade API** | Laravel 13 + MongoDB | [barber](https://github.com/KikeGonRam/barber) |
| 📱 **UrbanBlade Mobile** | Kotlin + Compose | **Este proyecto** |

</div>

---

## 📌 Estado del proyecto

```text
╭──────────────────────────────────────────────────────────╮
│                  URBANBLADE MOBILE                       │
├──────────────────────────────────────────────────────────┤
│  ✅ Arquitectura Android base                            │
│  ✅ Autenticación Bearer Token                           │
│  ✅ Navegación por roles                                 │
│  ✅ Dashboard                                            │
│  ✅ Citas y disponibilidad                               │
│  ✅ Catálogo                                             │
│  ✅ Tienda y carrito                                     │
│  ✅ Pedidos                                              │
│  ✅ Pagos e historial                                    │
│  ✅ Perfil                                               │
│  ✅ Notificaciones                                       │
│  ✅ Clientes e inventario                                │
│  ✅ Analítica y reportes                                 │
│  ✅ Herramientas admin / ingeniero                       │
│  🟡 Stripe Android SDK completo                          │
│  🟡 Push notifications                                   │
│  🟡 Pruebas E2E móviles                                  │
│  🟡 Release firmado / Google Play                        │
╰──────────────────────────────────────────────────────────╯
```

---

## 🗺️ Roadmap

- [ ] Paridad visual completa con los 4 temas de UrbanBlade Web
- [ ] Stripe Android SDK
- [ ] Firebase Cloud Messaging
- [ ] Pruebas unitarias
- [ ] Pruebas instrumentadas
- [ ] Soporte offline selectivo
- [ ] APK / AAB firmado
- [ ] CI/CD Android
- [ ] Publicación en Google Play

---

<div align="center">

<br/>

### ✂️ URBAN**BLADE**

**Corta menos pasos. Conecta toda la operación.**

`Web · API · Android`

<br/>

![Noir](https://img.shields.io/badge/Sastrería_Nocturna-0A0A0A?style=flat-square&labelColor=0A0A0A)
![Acero](https://img.shields.io/badge/Taller_de_Acero-C1703D?style=flat-square&labelColor=111317)
![Salon](https://img.shields.io/badge/Salón_Inglés-C9A24A?style=flat-square&labelColor=0B1210)
![Libreta](https://img.shields.io/badge/Libreta_de_Barbero-B8860B?style=flat-square&labelColor=F3EDE0)

<br/>

Desarrollado como parte del ecosistema **UrbanBlade**.

</div>
