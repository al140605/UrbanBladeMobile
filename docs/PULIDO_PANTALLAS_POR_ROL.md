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

## Incremento de unificación de módulos

Se reutiliza `UrbanRefreshButton` en quince pantallas: clientes, inventario,
movimientos, lista de espera, pedidos, cobros, corte, servicios, barberos,
membresías/paquetes, campañas, sorteos, reseñas, usuarios y perfil público del
barbero. Conserva los filtros al actualizar y deshabilita el control durante
carga y, donde existe ese estado, guardado, envío o procesamiento de una fila.
No cambia rutas, permisos ni operaciones del servidor.

`UrbanSectionTitle` comparte el criterio adaptable del encabezado principal:
acciones debajo del título a menos de 360 dp o con fuente desde 130 %, semántica
de encabezado y objetivo táctil mínimo de 48 dp.

Reseñas, bitácora y sorteos reutilizan `urbanListFeedback` para distinguir:
primera consulta, error inicial con reintento, datos anteriores tras un fallo,
actualización, vacío confirmado y contenido disponible. Los errores de guardar
o ejecutar operaciones mantienen sus avisos específicos; no se presentan como
fallos de consulta.

Sus ViewModels cancelan la consulta anterior y comprueban que siga activa antes
de publicar datos o errores. Una finalización cancelada no apaga el indicador de
la nueva consulta. Sorteos ya no presenta estadísticas cero antes de recibir una
respuesta válida.

### Validación de este incremento

- 446 pruebas Android, sin fallos, errores u omitidas; lint y APK debug correctos.
- Regresiones de respuestas tardías, cambios de filtro, errores y reintento;
  componentes y acciones alcanzables a 320 dp con fuente al 200 %.
- Cinco GET a la API QA local: dos filtros de reseñas, dos eventos de bitácora y
  sorteos. Respondieron 200 con administrador o ingeniero según el módulo.
- `git diff --check` y enlaces documentales correctos. Capturas o sesiones de QA
  no se incorporan al repositorio.

El teléfono no estaba conectado: no se instaló esta APK ni se certifica su
recorrido visual físico. No se repitió release/R8 para este cambio de interfaz;
los controles del incremento de seguridad continúan en el CI. Pendientes:
recorridos completos, formularios, acciones de creación y validación en los cuatro
temas y TalkBack. No se abre PR ni se despliega.

### Recorrido físico del incremento de unificación (4 de noviembre de 2026)

Entorno: Samsung SM-S731B con la variante `.audit` 3.0.0 (APK debug compilada el
4 de noviembre a las 21:36), API local por `adb reverse` al puerto 8001 y base
`urbanblade_dev` de `mongo-dev`. Se usaron cuentas ficticias de los cinco roles;
las contraseñas permanecen fuera del repositorio. La app habitual no se reemplazó.

| Rol | Pantallas revisadas | Resultado |
| --- | --- | --- |
| Administración | Pagos, pedidos, clientes, inventario, lista de espera, bitácora, campañas, sorteos, usuarios, barberos, servicios, reseñas, membresías | Trece módulos con el control de actualización compartido; sin errores tras actualizar |
| Recepción | Cobros, pedidos, clientes, inventario, movimientos, lista de espera, tarjetas regalo, corte | Siete con actualización compartida; tarjetas regalo es un formulario de venta y consulta y no la lleva |
| Barbero | Mi agenda, portafolio, horario | Cargan sin errores; portafolio y horario no tienen listado que actualizar |
| Ingeniería | Reportes, bitácora, métricas, insights, estado del sistema | Cinco módulos con actualización y sin errores |
| Cliente | Inicio, Mis citas, hoja de pago con transferencia | La cotización mostró servicio con descuento 95.00, propina 15.00 y total 110.00 |

El menú «Más» de cada rol coincide con la matriz: recepción sin análisis ni
administración e ingeniería sin operación.

Límites: solo se consultó y se actualizó. No se crearon, editaron ni borraron
datos, no se confirmó ningún cobro ni se subió un comprobante, y la base local no
registró pagos nuevos. El registro del dispositivo no mostró cierres inesperados.
No acredita formularios, acciones de creación, cuatro temas, 320 dp, TalkBack,
cuentas con varios roles ni el perfil público del barbero; siguen pendientes.

### Incremento 1 de coherencia: escala de espaciado y andamio único (5 de noviembre de 2026)

Plan en [la auditoría de coherencia](AUDITORIA_COHERENCIA_UI_2026-11-05.md). `UrbanSpace`
define la retícula de 4 dp y un margen lateral único de 16 dp, aplicado a 40 márgenes de
18 dp. `UrbanModuleScreen` admite acciones extra, botón flotante, estado de lista y un bloque
fijo; diecisiete módulos lo usan en lugar de recomponer `Scaffold`, barra y encabezado. En el
cierre de caja el encabezado ya no queda debajo de los avisos. No cambian rutas, permisos ni
contratos del servidor. Quedan fuera, a propósito: tienda con barra inferior, reserva, chat,
calendario de citas y los detalles de cliente y barbero, que no son módulos de lista.

Validación: 450 pruebas Android, sin fallos, errores ni omitidas; lint y APK de auditoría
correctos. Recorrido físico en el Samsung SM-S731B con la variante `.audit`, API local y
cuentas ficticias: dieciséis módulos de administración capturados en vertical (cobros,
pedidos, clientes, inventario, movimientos, lista de espera, tarjetas regalo, cierre de caja,
barberos, servicios, campañas, membresías, sorteos, reseñas, usuarios y configuración). Todos
muestran la misma barra superior, el mismo encabezado y el mismo margen. Las capturas se
conservan fuera de Git.

Hallazgo durante el recorrido: Configuración mostraba «No se pudo cargar la configuración»
cuando el negocio no tiene redes sociales, porque Laravel devuelve `redes_sociales` como `[]`
y el modelo esperaba un objeto. No procede de este incremento. Se corrige con un lector
tolerante (`[]` y `null` equivalen a «sin redes») y cuatro pruebas de lectura; con la
corrección la pantalla carga.

Pendiente: el botón flotante de campañas y servicios se superpone al estado vacío o a la
última tarjeta cuando la pantalla no se desplaza (incremento 3); buscadores, filtros y
resúmenes numéricos siguen con formas distintas (incrementos 3 y 4); cuatro temas, 320 dp y
TalkBack no se revisaron para este incremento.

### Incremento 2 de coherencia: vocabulario único (5 de noviembre de 2026)

Regla aplicada: el título del menú «Más» es el título de la pantalla, y la etiqueta superior
de cada pantalla es el grupo del menú. Los módulos de administración pasan a ADMINISTRACIÓN
(antes siete etiquetas distintas y el grupo ADMIN); «Analítica» pasa de GENERAL a ANÁLISIS;
«Pagos» se llama «Cobros y pagos»; «Logs» se llama «Bitácora»; «Métricas», «Usuarios» y «Corte
de caja» usan el mismo título en el menú y en la pantalla; «Muro de Inspiración» también.
Los atajos de recepción e ingeniería usan los mismos nombres. Solo cambian textos: no cambian
rutas, permisos, orden de las secciones ni contratos del servidor. La tabla de vocabulario de
la auditoría se corrigió, porque la primera versión confundía subtítulos con títulos.

Validación: 450 pruebas Android sin fallos, lint y APK de auditoría correctos. En el Samsung
SM-S731B con la variante `.audit`, el administrador abrió los veintitrés módulos de «Más» y
comparó el título y la etiqueta de cada pantalla con el menú: veintidós coinciden; la
excepción prevista es «Muro de Inspiración», que conserva su etiqueta porque también lo usa
el cliente. Los atajos de recepción (Citas, Cobros y pagos) e ingeniería (Bitácora, Estado del
sistema) se leyeron en pantalla. Sin cierres inesperados en el registro del dispositivo.

Pendiente: revisar los cuatro temas, 320 dp y TalkBack con los nombres nuevos; los nombres
internos de rutas (`logs`, `payments`) no cambian a propósito.

### Incremento 3 de coherencia: botón flotante y acción terciaria únicos (5 de noviembre de 2026)

`UrbanFab` sustituye a los cuatro botones flotantes copiados (Citas, Campañas, Membresías y
paquetes, Servicios): mismo color y forma, y se contrae a solo el icono en cuanto la lista se
desplaza, de modo que no tapa la última tarjeta; el icono conserva el nombre de la acción para
los lectores de pantalla. `UrbanTextButton` sustituye a los 24 `TextButton` que no son de
diálogo y garantiza un objetivo táctil de 48 dp; los de `confirmButton` y `dismissButton`
conservan el de Material. Solo cambia la presentación: no cambian rutas, permisos ni contratos.

Validación: 450 pruebas Android sin fallos, lint y APK de auditoría correctos. En el Samsung
SM-S731B con la variante `.audit`: en Servicios el botón aparece expandido arriba y contraído
tras desplazar, con la última tarjeta completa. No se recorrieron todas las pantallas que usan
`UrbanTextButton`; el cambio es de presentación y lo cubren la compilación y las pruebas.

Alcance que no entra, a propósito: unificar la carga y el error de las listas con
`urbanListFeedback`. Ninguno de los once ViewModels de lista registra si la primera consulta
terminó (`loaded`) y varios comparten un solo campo `error` entre consultar y guardar, de modo
que un fallo al guardar se mostraría como fallo de consulta. Exige cambios en la capa de datos
con pruebas y queda como incremento 3b.
