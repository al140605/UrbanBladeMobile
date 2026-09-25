# Design QA — Citas de administrador

## Evidencia

- Fuente visual seleccionada: `C:\Users\luis1\.codex\generated_images\01a0d20a-d904-7ee0-bcf4-420c580d829c\exec-66a626d9-1827-4b8d-8ce6-2eb4e7ed632a.png`.
- Fuente: 853 × 1844 px; objetivo móvil 390 × 844 dp.
- Implementación final: `docs/ui-audit-2026-09-24/admin-appointments-final-v2.png`.
- Implementación: 1080 × 2340 px; pantalla aproximada 360 × 780 dp a densidad 3x, incluyendo UI del sistema.
- Comparación normalizada: `docs/ui-audit-2026-09-24/admin-appointments-final-comparison.jpg`; ambos lados a 540 × 1170 px.
- Estado: administrador autenticado, tema oscuro y datos reales del entorno conectado.

## Comparación e iteraciones

### Primera implementación

- [P2] La sección redundante “Agenda del negocio” desplazaba la decisión prioritaria fuera del primer bloque visible.
- [P2] Todas las pendientes se dibujaban como tarjetas destacadas y la agenda perdía densidad.
- Corrección: se eliminó el encabezado redundante, se dejó una sola decisión destacada y el resto pasó a “Después”.

### Segunda implementación

- [P2] La tarjeta prioritaria y las filas seguían más altas que la referencia; las horas de la lista podían partirse en dos líneas.
- Corrección: se redujo espaciado, se alineó el botón con los datos, se aplicó hora de 24 h y se añadió fecha corta a cada fila para distinguir días reales.

### Resultado final

- Tipografía: conserva la jerarquía serif/sans de UrbanBlade; nombres largos usan elipsis para no romper la tarjeta.
- Espaciado: prioridad y lista compacta caben en el primer viewport; no hay controles persistentes ocultos.
- Color: fondo carbón, cobre/dorado, estados semánticos y contraste coinciden con la dirección seleccionada.
- Imágenes e iconos: la pantalla no requiere imágenes; usa exclusivamente iconos Material existentes.
- Contenido: filtros, conteos, nombres, estados y horarios provienen del entorno real. El estado de recordatorio usa indicadores reales del API cuando el backend actualizado esté desplegado.
- Diferencias aceptables: la fuente usa datos de muestra y mayor densidad; la implementación adapta nombres reales largos y fechas de varios días. La cápsula de Spotify pertenece al sistema, no a la app.
- No quedan hallazgos P0, P1 ni P2.

## Interacciones verificadas

- Navegación Inicio → Citas.
- Cambio Lista → Calendario y regreso a Lista.
- Menú “Más acciones” de la cita prioritaria; muestra Cancelar sin ejecutar cambios de datos.
- “Nueva cita” y “Confirmar” conservaron sus callbacks existentes; no se ejecutaron para evitar modificar datos reales.
- APK instalada en Samsung `R5GL44QP8TT`.

## Seguimiento P3

- El último chip queda parcialmente visible en pantallas de 360 dp para comunicar que la fila es desplazable horizontalmente.
- El calendario conserva las tarjetas anteriores; puede recibir una pasada visual propia cuando sea su turno como submódulo.

final result: passed
