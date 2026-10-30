# Interfaz y experiencia Android

Revisión contra las fuentes versionadas: 30 de octubre de 2026. Esta guía orienta el mantenimiento; las capturas y ejecuciones fechadas conservan su alcance original.

## Temas y componentes

`ui/theme/Theme.kt` define cuatro temas: Sastrería Nocturna (`noir`), Taller de Acero (`acero`), Salón Inglés (`salon`) y Libreta de Barbero (`libreta`). El último utiliza esquema claro; no describir el producto como una interfaz oscura fija.

Los componentes compartidos viven en `ui/components/`. Usar sus tokens y estados al modificar pantallas para evitar colores o comportamientos inconsistentes. Los recursos de mascota se seleccionan por tema en `Theme.kt`.

## Navegación por experiencia

`core/access/UserExperience.kt` resuelve el rol principal en este orden: administrador, recepción, barbero, ingeniero y cliente. El orden de la lista recibida no define la prioridad. Esta selección adapta la interfaz y no concede permisos del backend.

El cliente tiene Inicio, Reservar, Mis citas y Cuenta; las experiencias operativas utilizan sus destinos por rol. Consultar el [contrato de experiencia](UI_UX_CONTRATO_MOVIL.md) para acceso, retorno, reservas, errores y persistencia de formularios.

## Revisión de una pantalla

Comprobar carga, vacío, error y reintento; tamaños de texto y contraste; teclado, navegación Atrás, orientación y recreación. Confirmar que una selección temporal no se presenta como una reserva confirmada y que los mensajes distinguen una sesión vencida de un permiso insuficiente.

Usar el [procedimiento manual](VERIFICACION_MANUAL_P0.md) con API sintética o entorno aislado. No instalar sobre una sesión personal durante una auditoría. Registrar commit, dispositivo, variante y API efectiva cuando se genere evidencia nueva.

## Referencias visuales

La [galería de capturas](CAPTURAS.md) documenta imágenes nativas existentes. Las auditorías históricas y comparaciones ilustran decisiones de su fecha; no sustituyen una prueba del build actual. Los cambios de interfaz pendientes en otros checkouts requieren revisión propia y no forman parte de esta renovación documental.
