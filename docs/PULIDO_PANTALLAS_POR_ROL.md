# Pulido integral por rol

TT32 / HT-12, rama `feat/mejoras-interfaz-mobile`. El alcance incluye toda la
experiencia Android. Se conservan identidad, imágenes, cuatro temas y navegación
aprobada. Sin PR, fusión, publicación ni despliegue.

## Criterios comunes

Revisar jerarquía visual, títulos y acciones, carga inicial, vacío confirmado,
errores, datos anteriores al actualizar, formularios, teclado, navegación,
permisos y actualización posterior a las acciones. Validar 320 dp, fuente grande,
cuatro temas y objetivos táctiles de 48 dp. El backend conserva la autoridad
sobre datos y acciones válidas.

Una ruta existente o un componente compartido no acreditan el recorrido completo.
Las evidencias se registran por incremento. La matriz proviene de `UrbanBladeRoot`,
`MoreScreen`, `AccountScreen` y `UserExperience`; no redefine permisos del servidor.
También queda pendiente revisar cuentas con varios roles.

## Matriz de recorridos

| Rol | Pantallas y recorridos | Revisión restante |
| --- | --- | --- |
| Público | Bienvenida, acceso, registro, recuperación, catálogo | Formularios, teclado, retorno y errores |
| Cliente | Inicio, reserva, Mis citas, historial | Estados, paginación, cancelación, reagendado y recuperación |
| Cliente | Beneficios, membresías, paquetes, tarjetas regalo | Elegibilidad, errores y confirmaciones |
| Cliente | Tienda, carrito, pedidos, facturas | Búsqueda, descarga, detalle y retorno |
| Barbero | Inicio, Mi agenda, servicio activo, ticket | Horarios, conflictos y cambios de estado |
| Barbero | Portafolio y Mi horario | Medios, validaciones y guardado |
| Recepción | Inicio, agenda, clientes | Búsqueda, registro, edición y confirmación |
| Recepción | Lista de espera, inventario, movimientos | Filtros, conflictos y avisos |
| Recepción | Cobros, pedidos, tarjetas regalo, corte | Estados, permisos y resultado de acciones |
| Administración | Inicio y módulos operativos de recepción | Resúmenes y navegación entre módulos |
| Administración | Servicios, barberos, usuarios, configuración | Formularios largos, validaciones y acciones destructivas |
| Administración | Membresías/paquetes, campañas, sorteos, reseñas | Segmentación, estados y confirmaciones |
| Administración / Ingeniería | Reportes, bitácora, métricas, insights, sistema | Rangos, exportación y fallos parciales |
| Personal | Más, catálogo, analítica según acceso existente | Organización, filtros y vuelta al origen |
| Compartido | Cuenta, notificaciones, muro, perfil público, Bladebot | Perfil actualizado, medios, accesibilidad y errores |

## Incremento de estados y encabezados

- Actualización accesible en los cinco inicios. Un fallo de refresco avisa que se
  conserva información anterior.
- Administración distingue pagos pendientes desconocidos de cero confirmado;
  un fallo parcial no permite afirmar «Todo al día».
- Ingeniería no muestra actividad cero antes de consultar la bitácora.
- Cliente distingue la primera consulta de una agenda vacía confirmada.
- Bitácora y reseñas separan carga, error y datos confirmados. Los filtros de
  reseñas se desplazan horizontalmente.
- El encabezado compartido apila acciones bajo el título en ancho estrecho o
  fuente grande. Los módulos reutilizan la actualización accesible.
- Tienda muestra un único vacío. Clientes y sorteos no muestran vacío junto con
  un error de consulta.

No cambian contratos del backend ni permisos. La validación automática y los
recorridos físicos se registran por separado; no certifican todos los módulos.

## Evidencia del incremento

- Suite completa: 431 pruebas, cero fallos, errores u omitidas. Incluye primera
  carga, reintento, datos anteriores, fallo parcial administrativo y encabezado
  con acciones a 320 dp y fuente al 200 %.
- Lint y APK debug correctos en la variante de auditoría. Las aclaraciones de
  etiquetas históricas se comprueban también mediante compilación y lint.
- 17 consultas locales con cinco roles: módulos permitidos respondieron 200 y
  las consultas a usuarios de los cuatro roles no administrativos respondieron
  403. Esto no reemplaza la cobertura de todos los endpoints y operaciones.
- Cinco inicios abiertos y actualizados en el Samsung conectado con cuentas
  ficticias. Se inspeccionaron sus capturas, conservadas fuera de Git. La app
  habitual no se reemplazó; la variante QA quedó con la cuenta de cliente QA.
- Conexión efectiva comprobada: API de loopback, Mongo local `mongo-dev`, base
  `urbanblade_dev`. Se añadieron únicamente dos cuentas ficticias de recepción
  e ingeniería que faltaban. Contraseñas y tokens permanecen fuera del repositorio.

El recorrido físico se limita al inicio y su actualización. No acredita todavía
todos los botones, edición, borrado, cuatro temas ni TalkBack de la matriz.
