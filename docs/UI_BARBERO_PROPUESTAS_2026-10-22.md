# TT32 / HT-12 — Propuestas de vistas del barbero

Actualización del 30 de octubre de 2026: mejoras conservadas en `feat/mejoras-interfaz-mobile` e integración local de `main` publicado, commit `3173e34839517145c1708f86f56320da0c6e9f1a`, sin conflictos. Validación de la combinación: lintDebug, testDebugUnitTest y assembleDebug aprobados; 369 pruebas, cero fallos, errores u omisiones. No se instaló el APK, no se subió esta rama y no se repitió la revisión visual en dispositivo. Las observaciones de octubre 22 que siguen son históricas.


Revisión del código: 22 de octubre de 2026. Opción A elegida e implementada sobre las pantallas existentes; ver validación y límites al final.

## Alcance revisado

- Inicio: `BarberHomeScreen.kt`. Siguiente cita o cita en proceso, pendientes por confirmar, gráficas y accesos de trabajo. Actualmente las gráficas aparecen antes de los accesos.
- Mi agenda: `BarberAgendaScreen.kt`. Periodo, fecha, resumen, filtros y acciones por estado. El resumen ocupa espacio antes de la lista.
- Portafolio: `BarberPortfolioScreen.kt`. Resumen, publicación, trabajos y eliminación confirmada. Conservar fotos y videos existentes.
- Mi horario: `AdminModuleScreens.kt`, `BarberScheduleScreen`. Días activos, entrada/salida, validación y guardado.
- Cuenta: kit compartido de personal. Conservar datos, seguridad y ajustes, sin nivel/puntos exclusivos de cliente.

## A — Clara y compacta

Encabezados bajos, próxima cita en tarjeta compacta, resúmenes en franjas y accesos de trabajo antes de gráficas. Agenda con mayor espacio para citas. Portafolio con métricas discretas y horario con filas alineadas.

## B — Visual y enfocada

Misma estructura y funciones, con la foto actual de barbería en la tarjeta principal, mayor énfasis en cliente/hora/estado y acentos de estado moderados. Accesos operativos antes de gráficas. Los módulos conservan encabezados compactos.

## Invariantes

- Conservar imágenes, rutas y permisos existentes; los espacios de fotos en las propuestas representan los medios actuales.
- Conservar navegación raíz Inicio / Citas / Más / Cuenta; agenda, portafolio y horario siguen como módulos con volver.
- No alterar reglas de inicio, cobro, confirmación, cancelación, inasistencia ni extensión. El servidor determina si se puede iniciar un servicio.
- Cuenta del personal conserva su comportamiento; bienvenida y cambios del cliente permanecen intactos.
- Datos de las imágenes ficticios. Revisión de código, sin validación visual del rol en teléfono en esta etapa.
- Sin merge a principal. La implementación corresponde a la opción A elegida.

## Lectura de los mockups

Los mockups generados son referencias de jerarquía, espaciado y color. Cualquier etiqueta o control adicional dibujado por el generador (por ejemplo Guardados, Reprogramar o secciones extra de Cuenta) no forma parte del alcance: se mantienen exclusivamente las funciones existentes descritas arriba. La variante B inicial con textos en portugués se descarta; usar únicamente su versión corregida en español.

## Opción elegida e implementación

El usuario eligió A, aplicada sobre las vistas existentes sin reemplazar imágenes.

- Inicio: encabezado y siguiente cita más compactos; accesos de trabajo antes de ambas gráficas existentes.
- Agenda: pestañas Día/Semana compartidas, resumen más bajo; filtros, estados y acciones conservados.
- Portafolio y Horario: menos altura en sus resúmenes, sin cambiar medios ni formularios.
- Cuenta: tarjeta compacta y datos inicialmente plegados únicamente para barbero (además del cliente ya ajustado). Los otros roles conservan su presentación.

Validación: assembleDebug, testDebugUnitTest y lintDebug con UI_AUDIT=true: BUILD SUCCESSFUL. 347 pruebas, cero fallos, errores u omitidas. Lint completado con avisos existentes. git diff --check correcto. Sin cambios en recursos de imagen ni bienvenida respecto de ba4893c.

Auditoría visual en SM_S731B, aplicación com.urbanblade.mobile.audit y API sintética local: Inicio, Agenda, Portafolio, Horario y Cuenta. Agenda/Portafolio sin contenido y Horario sin días activos; no se valida aquí la apariencia con citas/trabajos cargados, gráficas ni horarios activos. No se guardaron datos ni ejecutaron acciones sobre citas.

Capturas en C:/Users/luis1/.codex/visualizations/2026/10/21/01a1606a-701b-7bc2-93b8-e62d55991a67/: barber-home-A.png, barber-agenda-A.png, barber-portfolio-A.png, barber-schedule-A.png, barber-account-A.png.

Cambios locales en docs/play-store; sin nuevo commit, push ni merge a principal.

