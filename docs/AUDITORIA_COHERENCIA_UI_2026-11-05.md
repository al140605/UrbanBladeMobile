# Auditoría de coherencia visual y de interacción

5 de noviembre de 2026 · TT32 / HT-12, rama `feat/mejoras-interfaz-mobile`.
Alcance: las pantallas de los cinco roles. Es un diagnóstico y un plan; no cambia código.

## Resumen

El kit existe (más de sesenta componentes `Urban*`), los colores y los tamaños de
texto salen del tema y casi no hay valores fijos. La incoherencia no viene de valores
sueltos: viene de que **cada pantalla vuelve a decidir cómo ensamblar** el kit.
El resultado es un producto con identidad común, pero sin un ritmo, un encabezado, un
vocabulario ni un componente de estado únicos. Eso es lo que separa «correcto» de «premium».

## Evidencia

Mediciones sobre `app/src/main/java/com/urbanblade/mobile/ui` (4 de noviembre de 2026).

| Hallazgo | Medición |
| --- | --- |
| Sin escala de espaciado ni de radios | Catorce valores de `dp` escritos a mano en pantallas; los más usados son 8 (161), 10 (114), 12 (109), 14 (99) y 18 (98), casi equivalentes entre sí |
| Dos andamios de pantalla | `UrbanModuleScreen` (eyebrow, título, atrás y actualizar ya resueltos) solo lo usan 6 pantallas; las demás recomponen `Scaffold` + `UrbanTopBar` + `UrbanPageHeader` + `UrbanRefreshButton` a mano |
| Seis variantes de tarjeta | `UrbanCard`, `UrbanPremiumCard`, `UrbanHeroCard`, `UrbanStatCard`, `UrbanMetricCard`, `UrbanAnalyticsCard`; el criterio para elegir una no está escrito |
| Cuatro tipos de pastilla | `UrbanStatusPill`, `UrbanTonePill`, `UrbanRolePill`, `UrbanTrendPill` |
| Sin botón terciario compartido | 80 `TextButton` de Material con estilo local frente a 38 `UrbanPrimaryButton` y 53 `UrbanOutlineButton` |
| Estados vacíos dispersos | 57 textos de vacío escritos a mano; `UrbanEmptyState`, `UrbanInlineEmpty` y `UrbanListFeedback` conviven |

## Incoherencias visibles por rol

Inicios (capturas de QA del 4 de noviembre con cuentas ficticias):

- Cliente: encabezado dentro de una tarjeta con borde, con actualizar, campana y avatar.
  Personal: encabezado plano con etiqueta de rol, saludo en serif, fecha y avatar, y sin campana.
- Resumen numérico: administración usa dos fichas por fila; recepción, una tarjeta con dos
  datos; barbero, una tarjeta de próxima cita; ingeniería, una lista vertical Hoy / Creados / Eliminados.
- La mascota solo aparece en el inicio del cliente.
- Barra inferior: cliente cuatro destinos (Reservar), personal cuatro (Citas, Más),
  ingeniería tres. El personal no tiene un atajo equivalente a Reservar.

Vocabulario del menú «Más» frente al título de cada pantalla:

| Menú | Título de la pantalla | Etiqueta superior de la pantalla |
| --- | --- | --- |
| Pagos | Cobros y pagos | OPERACIÓN |
| Datos del negocio | Reportes | ANÁLISIS |
| Logs | Logs de actividad | ANÁLISIS |
| KPIs operativos | Métricas del negocio | ANÁLISIS |
| Roles y accesos | Usuarios y personal | ADMINISTRACIÓN |
| Servicios y salud | Estado del sistema | SISTEMA |
| Resumen de turno | Cierre de hoy | CORTE DE CAJA |

Los grupos del menú son GENERAL, OPERACIÓN, ANÁLISIS, ADMIN y SISTEMA; las etiquetas de
las pantallas añaden CALIDAD, EQUIPO, CATÁLOGO, MARKETING, BENEFICIOS, AGENDA y BARBERO.
Un mismo módulo se llama de dos maneras y se agrupa de dos maneras.

## Principios propuestos

1. Una sola pantalla de módulo: eyebrow, título, subtítulo, atrás, actualizar, estados y contenido.
2. Un ritmo: escala de espaciado (4, 8, 12, 16, 24, 32) y de radios, sin valores intermedios.
3. Una familia de tarjetas con variantes nombradas y una regla escrita para elegir.
4. Un único vocabulario: el nombre del menú es el título de la pantalla y la etiqueta es el grupo del menú.
5. Un único componente de carga, error con reintento, vacío confirmado y datos anteriores.
6. Un único encabezado de inicio con variantes por rol, no cinco construcciones distintas.
7. Lo «premium» se mide: movimiento, retroalimentación táctil, esqueletos y microcopy con el mismo criterio en todos los roles.

## Plan por incrementos

Cada incremento termina con pruebas, lint, APK `.audit` y recorrido físico registrado.

| # | Incremento | Qué se ve | Alcance |
| --- | --- | --- | --- |
| 1 | Escala de espaciado y radios, y migración de todos los módulos a `UrbanModuleScreen` | Mismo encabezado, mismo actualizar y mismo ritmo en más de 25 pantallas | Interno + visible |
| 2 | Vocabulario: menú, título y etiqueta alineados | Sin nombres distintos para el mismo módulo | Textos |
| 3 | Estados únicos (`UrbanListFeedback` en todas las listas) y botón terciario compartido | Carga, error y vacío idénticos | Visible |
| 4 | Una familia de tarjetas y de pastillas | Menos variantes, regla escrita | Interno + visible |
| 5 | Encabezado y resumen de inicio unificados por rol | Los cinco inicios se sienten de la misma app | Visible |
| 6 | Capa premium: movimiento, háptica, esqueletos y microcopy | Percepción de calidad | Visible |

No cambian rutas, permisos ni contratos del backend. Los incrementos 1 a 3 son los de mayor
efecto por esfuerzo y no requieren decisiones de diseño nuevas, porque `UrbanModuleScreen`
y `UrbanListFeedback` ya existen.

## Pendiente de esta auditoría

Solo se compararon visualmente los cinco inicios; los módulos se revisaron en el teléfono por
texto y comportamiento, no con capturas comparadas. Falta una hoja comparativa de módulos,
los cuatro temas, 320 dp y TalkBack antes de dar por cerrado cada incremento.
