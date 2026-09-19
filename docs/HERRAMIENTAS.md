# Herramientas recomendadas — Android Studio + diseño

Investigación 2026-09-11 de plugins de Android Studio y librerías de referencia para
Kotlin + Jetpack Compose + Material 3. **Nada de esto está instalado ni integrado al
proyecto** — son sugerencias para revisar e instalar manualmente desde la IDE
(`File → Settings → Plugins → Marketplace`), o repos para consultar como referencia de
componentes, no para clonar dentro de este proyecto sin revisión.

## Plugins de Android Studio (JetBrains Marketplace)

### Diseño / Compose

- **[Compose Hammer](https://plugins.jetbrains.com/plugin/21912-compose-hammer)** —
  constructor visual de UI para Jetpack Compose + Material 3: arrastra componentes
  (59+ de Material 3) y genera el `@Composable` correspondiente. Útil para armar
  pantallas nuevas más rápido sin escribir el boilerplate de layout a mano.
- Helpers de **Compose Multipreview** (vienen incluidos en Android Studio moderno, no
  requieren plugin aparte) — generan automáticamente `@Preview` en varios tamaños de
  pantalla/tema a la vez, útil dado que la app soporta 5 roles distintos con UI propia.

### Productividad / calidad de código

- **[JSON To Kotlin Class](https://plugins.jetbrains.com/plugin/9960-json-to-kotlin-class-jsontokotlinclass-)**
  — genera `data class` de Kotlin a partir de una respuesta JSON pegada directamente.
  Con un backend Laravel que responde JSON real, esto ahorra tiempo modelando los DTOs
  de Retrofit para cada endpoint nuevo.
- **[ADB Idea](https://plugins.jetbrains.com/plugin/7380-adb-idea)** — atajos para
  limpiar datos de la app, desinstalar, forzar detener, etc. sin salir de la IDE ni
  escribir comandos `adb` a mano.
- **[Detekt](https://plugins.jetbrains.com/plugin/10761-detekt)** — linter estático de
  Kotlin (detecta code smells, complejidad, posibles leaks de coroutines). Útil para
  mantener consistencia si dos personas escriben Kotlin en el mismo repo.
- **[Key Promoter X](https://plugins.jetbrains.com/plugin/9792-key-promoter-x)** —
  sugiere el atajo de teclado cada vez que usas el mouse para una acción que tiene uno;
  acelera la curva de aprendizaje del IDE.
- **[Rainbow Brackets](https://plugins.jetbrains.com/plugin/10080-rainbow-brackets)** —
  colorea pares de llaves/paréntesis anidados; ayuda a leer Composables anidados
  profundamente (común en Compose).

Ninguno de estos requiere cambios en `build.gradle.kts` — son plugins de la IDE, no
dependencias del proyecto.

## Librerías de referencia (Compose + Material 3) — para consultar, no para copiar directo

Repos open-source con catálogos de componentes Material 3 en Compose, útiles como
referencia de patrones/composición al construir pantallas nuevas:

- **[meticha/material-3-expressive-catalog](https://github.com/meticha/material-3-expressive-catalog)**
  — catálogo de componentes Material 3 "expressive" (el estilo más reciente de Google,
  2026) en Compose.
- **[muhammedeminalan/ComposeMaterialDesign](https://github.com/muhammedeminalan/ComposeMaterialDesign)**
  — showcase completo de Material Design 3 en Compose.
- **[Dinesh2510/Jetpack-Compose-UI-Components-Material-3](https://github.com/Dinesh2510/Jetpack-Compose-UI-Components-Material-3)**
  — implementación de componentes Material (botones, cards, switches, Scaffold) como
  funciones composables independientes, fácil de leer una por una.

Antes de traer código de cualquiera de estos al proyecto: revisar la licencia del repo,
y adaptar el componente al sistema de theming/colores propio de UrbanBlade en vez de
copiar el tema de ejemplo tal cual.

## Fuentes

- [Best Android Studio Plugins 2025 — Medium](https://medium.com/@androidlab/hot-plugins-for-android-developers-2025-productivity-boosting-tools-for-android-studio-9d6441cb8bdc)
- [Compose Hammer — JetBrains Marketplace](https://plugins.jetbrains.com/plugin/21912-compose-hammer)
- [Material Design 3 for Jetpack Compose — m3.material.io](https://m3.material.io/develop/android/jetpack-compose)
- [Top tier plugins for Android developer in 2026 — Medium](https://medium.com/@dmitrij.maksimow/top-plugins-you-must-use-as-android-developer-d1a15025da09)
- [10 Best Android Studio Plugins for Productivity in 2026 — ajmani.dev](https://ajmani.dev/best-android-studio-plugins-for-productivity/)
