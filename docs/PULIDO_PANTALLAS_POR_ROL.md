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

### Incremento 3b: estados únicos de lista, piloto (5 de noviembre de 2026)

Piloto en las dos listas que son consultas puras: movimientos de inventario y lista de espera.
Sus ViewModels registran `loaded` (solo tras una primera consulta correcta), cancelan la consulta
anterior y la paginación en curso, y no publican el resultado de una consulta cancelada, con el
mismo método que reseñas, bitácora y sorteos. Sus pantallas sustituyen la barra de carga, el
aviso de error, el esqueleto y el vacío escritos a mano por `urbanListFeedback`, y el resumen
numérico de movimientos no se muestra hasta tener datos confirmados. Seis pruebas nuevas
(`StaffListLoadStateTest`): primera consulta fallida y reintento, fallo al actualizar con lo
confirmado conservado, y respuesta tardía de un filtro anterior descartada.

Validación: 456 pruebas Android sin fallos, lint y APK de auditoría correctos. En el Samsung
SM-S731B con la variante `.audit`, quitando el túnel USB para provocar un error de red real:
- Primera consulta sin red: lista de espera muestra «No pudimos cargar la lista de espera», el
  motivo («Sin conexión. Revisa tu red e inténtalo de nuevo.») y el botón «Reintentar»; ya no
  aparece un «Nadie en esta lista» falso.
- Tras restaurar la red y pulsar «Reintentar»: «Nadie en esta lista», ya como vacío confirmado.
- Fallo al actualizar con datos ya confirmados: aviso «No pudimos actualizar. La información
  visible corresponde a la última consulta» con «Reintentar actualización».
- Movimientos con red: resumen y vacío confirmado («Sin movimientos»).
La demo no tiene entradas ni movimientos, de modo que la conservación de filas la cubren las
pruebas y no el recorrido físico.

Pendiente: extender el método a las demás listas por tandas. Las que mezclan consultar y guardar
en un mismo campo `error` (cobros, servicios, barberos, campañas, membresías y usuarios)
necesitan primero separar ambos errores en su ViewModel.

### Incremento 3b, tanda 2: clientes, pedidos e inventario (5 de noviembre de 2026)

Los tres ViewModels registran `loaded` (`hasLoaded` en clientes e inventario), cancelan la
consulta anterior y publican el error de la consulta en `listError`, separado de `error`, que
queda para el detalle y para crear, editar, eliminar, entregar, cancelar o registrar un
movimiento. Un fallo al guardar ya no se presenta como «no se pudo cargar», ni una primera
consulta fallida como lista vacía. `ClientsViewModel` recibe ahora el repositorio por
constructor, con el mismo valor por defecto, para poder probarlo. `urbanListFeedback` admite un
vacío con acción opcional: clientes conserva «Registrar cliente». Las tres pantallas sustituyen
sus bloques de carga, error y vacío por `urbanListFeedback` y conservan sus avisos de acción.
Siete pruebas nuevas (`ListErrorSeparationTest`) y cuatro aserciones antiguas actualizadas: un
fallo de consulta ahora está en `listError`, no en `error`.

Validación: 463 pruebas Android sin fallos, lint y APK de auditoría correctos. En el Samsung
SM-S731B con la variante `.audit`, quitando el túnel USB y abriendo cada lista por primera vez:
las tres muestran «No pudimos cargar …», el motivo («Sin conexión. Revisa tu red e inténtalo de
nuevo.») y «Reintentar»; con la red restaurada y tras pulsar «Reintentar» cargan sus datos
(128 clientes, pedidos por entregar, productos con stock bajo). Clientes conserva el buscador y
los filtros durante el error, y los resúmenes de pedidos e inventario no aparecen hasta tener
datos confirmados. Sin cierres en el registro del dispositivo.

Pendiente: cobros, servicios, barberos, campañas, membresías y usuarios.

### Incremento 3b, tanda 3: servicios, barberos y usuarios (5 de noviembre de 2026)

Los tres ViewModels registran `loaded`, cancelan la consulta anterior y la paginación en curso, y
publican el error de la consulta en `listError`, separado de `error`, que queda para guardar,
activar, editar y eliminar. Las pantallas conservan su barra de progreso mientras guardan, dejan
la consulta a `urbanListFeedback` y mantienen el error de guardar dentro de su hoja o formulario.
Servicios conserva «Nuevo servicio» en el primer uso; usuarios pasa del vacío con icono al
mismo vacío con mascota que el resto. Nueve pruebas nuevas (`AdminListLoadStateTest`): primera
consulta fallida y reintento, un fallo de acción que no se presenta como fallo de consulta, y la
respuesta tardía de un filtro anterior descartada. Las pruebas existentes de estos ViewModels
solo comprobaban `error` para fallos de acciones, que siguen en `error`.

Validación: 472 pruebas Android sin fallos, lint y APK de auditoría correctos. En el Samsung
SM-S731B con la variante `.audit`, quitando el túnel USB y abriendo cada lista por primera vez:
las tres muestran «No pudimos cargar …», el motivo y «Reintentar»; con la red restaurada y tras
pulsar «Reintentar» cargan sus datos. Sin cierres. Un fallo al guardar o eliminar no se
provocó en el teléfono (sería destructivo); lo cubren las pruebas.

Pendiente: cobros, campañas y membresías y paquetes.

### Incremento 3b, tanda 4: campañas y membresías y paquetes (5 de noviembre de 2026)

Ambos ViewModels registran `loaded`, cancelan la consulta anterior y publican el error de la
consulta en `listError`, separado de `error`, que queda para enviar una campaña y para guardar,
activar o desactivar un plan o un paquete. En membresías, `runCatching` del selector de servicios
también captura las cancelaciones, de modo que se comprueba `ensureActive()` tras cada paso para
que una consulta cancelada no publique su resultado. Campañas conserva su barra de progreso
mientras envía; membresías usa `urbanListFeedback` una vez por pestaña y conserva «Nuevo plan» y
«Nuevo paquete» en el vacío. Seis pruebas nuevas (`PromoListLoadStateTest`).

Validación: 478 pruebas Android sin fallos, lint y APK de auditoría correctos. En el Samsung
SM-S731B con la variante `.audit`, quitando el túnel USB y abriendo cada pantalla por primera vez:
las dos muestran «No pudimos cargar …», el motivo y «Reintentar»; con la red restaurada y tras
pulsar «Reintentar», campañas muestra sus segmentos y el vacío confirmado («Sin campañas
todavía») y membresías muestra el plan existente. Sin cierres. Un fallo al enviar, guardar o
desactivar no se provocó en el teléfono; lo cubren las pruebas.

Pendiente: cobros.

### Incremento 3b, tanda 5: cobros y pagos (5 de noviembre de 2026)

Hallazgo previo al cambio: las listas de comprobantes por revisar, anticipos y adeudos se
pedían con `runCatching { … }.getOrDefault(lo anterior)`. Si esa consulta fallaba en la primera
carga, el valor anterior era una lista vacía y la pantalla decía «Todo al día» aunque hubiera
comprobantes pendientes sin consultar; además `runCatching` también capturaba las cancelaciones.
Una prueba existente fijaba como deseado que el historial se mostrara aunque fallaran los
anticipos; esa decisión se mantiene.

`PaymentsStaffViewModel` registra `loaded`, cancela la consulta anterior y la paginación en curso,
y publica el error de la consulta del historial y de «cargar más» en `listError`, separado de
`error`, que queda para aprobar, rechazar, cobrar, condonar y abrir un comprobante. Las tres
listas de revisión se piden con una función propia que no se traga las cancelaciones. Si alguna
falla, el historial se muestra igual, se conserva la lista anterior y se publica un aviso de
fallo parcial (`reviewListsError`) que nombra cuáles fallaron; `reviewUnknown` marca el fallo de
comprobantes o anticipos, que es lo que impide afirmar que todo está al día. Un fallo solo de
adeudos avisa, pero no vuelve desconocido el subtítulo. La pantalla dice «Consulta los
comprobantes por revisar y el historial» antes de la primera consulta, «No pudimos confirmar
los comprobantes por revisar» si fallaron y «Todo al día» solo cuando es cierto; muestra el
aviso parcial con «Reintentar»; no muestra «Cobrado hoy» hasta tener datos confirmados; y usa
`urbanListFeedback` para el historial. La barra de progreso queda para aprobar, rechazar y
cobrar. Siete pruebas nuevas (`PaymentsListLoadStateTest`) y refuerzo de la existente de fallo
parcial.

Validación: 485 pruebas Android sin fallos, lint y APK de auditoría correctos. En el Samsung
SM-S731B con la variante `.audit`, quitando el túnel USB y abriendo la pantalla por primera vez:
muestra «No pudimos cargar los pagos», el motivo y «Reintentar», y el subtítulo no dice «Todo al
día»; con la red restaurada y tras «Reintentar» carga el historial y el subtítulo dice «Todo al
día» (en la demo no hay comprobantes pendientes). El fallo parcial no se pudo aislar en el
teléfono, porque cortar la red falla todas las consultas a la vez: lo cubren las pruebas. Sin
cierres.

Con esta tanda las doce listas de administración y operación usan los estados únicos.

### Incremento 4 de coherencia: una ficha de KPI y una pastilla (5 de noviembre de 2026)

Regla de uso en [la auditoría](AUDITORIA_COHERENCIA_UI_2026-11-05.md#regla-de-uso-de-tarjetas-y-pastillas).
`UrbanMetricCard` se retira y sus ocho usos (Campañas, detalle de cliente y Dashboard) pasan a
`UrbanStatCard`, la ficha plana con la etiqueta arriba que admite tendencia. `UrbanRolePill` y
`SimpleStatusPill` delegan en `UrbanTonePill`, que gana un color de texto opcional (el rol conserva
su dorado legible), de modo que hay una sola implementación del tinte, el borde y el texto para 16
usos. El relleno de `UrbanPremiumCard` pasa de 18 a 16 dp (`UrbanSpace.Md`). La medición previa se
corrigió: eran tres superficies con roles distintos y dos fichas de KPI duplicadas, no seis tipos
de tarjeta; y el contraste de `SimpleStatusPill` ya pasaba AA en Libreta (5,2 a 5,6), de modo que
la fusión es de mantenimiento y no de accesibilidad.

Validación: 488 pruebas Android sin fallos, lint y APK de auditoría correctos. En el Samsung
SM-S731B con la variante `.audit`, en el tema «Sastrería Nocturna» y en «Libreta de Barbero» (el
único claro): fichas de KPI en Campañas y en el detalle de cliente, pastillas «Stock bajo» en
Inventario, roles en Usuarios e inicio del administrador con las tarjetas destacadas. Las fichas
planas se leen limpias en ambos temas y «Stock bajo» mantiene buen contraste en el claro. El tema
de la variante de auditoría se devolvió a «Sastrería Nocturna». No se revisaron «Taller de Acero» ni
«Salón Inglés».

Hallazgo durante el recorrido, previo al incremento: el detalle de cliente mostraba «No se pudo
cargar el cliente». El backend resuelve `admin/clients/{client}` por slug (el modelo `Client` usa
`HasSlug`) y la app llamaba con el id, de modo que el detalle, la edición y la baja respondían 404;
ya ocurría antes de esta rama. La lista navega ahora con `ClientRow.routeKey()` (el slug, con el id
como respaldo) y se añaden tres pruebas. Con el cambio el detalle carga y muestra sus seis fichas.

Pendiente: el botón flotante de «Nueva campaña» sigue tapando parte del texto del estado vacío
mientras la lista está en lo alto; al desplazar se contrae y deja de tapar.

## Incremento 5: inicios unificados (5 de noviembre de 2026)

- `UrbanHomeHeader` (etiqueta de rol, «Hola, nombre», fecha, actualizar y avatar) sustituye las cuatro
  construcciones a mano de Administración, Recepción, Ingeniería y Barbero; el barbero deja de usar el
  tamaño `compact`.
- `UrbanHeroSummary` sustituye los cuatro bloques de resumen copiados (con tendencia y pie opcionales,
  tono por dato y clic opcional). El Cliente conserva su saludo dentro de `UrbanHeroCard`, con la campana.
- Las acciones rápidas de Administración pasan a `UrbanModuleGrid` y `HomeTile` se elimina.
- Ritmo único de 16 dp (`UrbanSpace.Md`) arriba y entre bloques en los cinco inicios.
- Validación: lint, pruebas unitarias y APK `.audit` en verde. Recorrido físico (tema oscuro) con las
  cuatro cuentas demo de personal: mismo encabezado, misma tarjeta de resumen y mismo ritmo.
- Pendiente: revisar Libreta, los otros dos temas y 320 dp.
- Ajuste tras la primera revisión: el resumen ocupaba demasiado. Ahora la cifra es más pequeña, la tendencia
  va a su lado, los datos secundarios van en una fila sin cajas de icono ni divisor, y el encabezado ya no
  apila actualizar y avatar bajo la fecha salvo por debajo de 300 dp (antes 360 dp, que en un teléfono de
  385 dp con márgenes los apilaba). Cada inicio recupera unos 75 dp de alto.

## Bienvenida, acceso, registro y recuperación (5 de noviembre de 2026)

Sin cambiar ninguna fotografía. Solo composición:

- Bienvenida: marca más grande con la leyenda al lado, velo superior más firme, «gran corte.» en dorado,
  tres beneficios en una línea y los botones sin el recuadro doble.
- Acceso, registro y recuperación: ahora siguen el patrón de la bienvenida. Fotografía a pantalla completa
  con velo, la misma marca oficial arriba (antes usaban otra variante del logotipo), el título sobre la foto y
  el formulario en un panel opaco abajo. Con el teclado abierto la foto y el título ceden el espacio y el pie
  queda fijo. La mascota de error o éxito va junto al título.
- Registro: el avance es una barra de dos segmentos (con descripción para TalkBack).
- Pendiente: probar Libreta, el teclado abierto y el estado «correo enviado» en el teléfono.

## Cuenta, perfil y accesibilidad (5 de noviembre de 2026)

Misma pantalla para los cinco roles; lo que cambia es solo lo que cada rol tiene.

- Tarjeta de miembro única y compacta (antes administración, recepción e ingeniería usaban una grande y
  barbero y cliente una compacta): foto de 64 dp, nombre, correo, rol y dos datos en una fila sin cajas.
- Todas las secciones empiezan cerradas, también para el personal, y el margen es el de los demás inicios
  (16 dp).
- «Legal y privacidad» pasa de dos botones de texto sueltos a dos filas del mismo grupo de acciones que usa
  el cliente (Beneficios, pedidos, facturas).
- Accesibilidad: el título de cada pantalla (`UrbanPageHeader`) es un encabezado para TalkBack; la foto de
  perfil es un botón con descripción; el selector de tema es un grupo de selección; los avisos de guardado y
  error son regiones vivas; las filas con interruptor leen título y descripción (antes la descripción
  reemplazaba al texto y se perdía el subtítulo); la etiqueta seleccionada de los chips usa el dorado de texto
  (legible en Libreta).
- Pendiente: recorrer con TalkBack en el teléfono, revisar Seguridad y Ajustes en Libreta y 320 dp.

## Verificación de temas (5 de noviembre de 2026)

Recorrido físico con la cuenta demo de administrador: bienvenida, acceso, registro, recuperación, inicio y
Cuenta > Ajustes en los cuatro temas.

- Taller de Acero y Salón Inglés: sin defectos visibles.
- Libreta: «Iniciar sesión» de la bienvenida se pintaba en dorado oscuro sobre la fotografía oscura y no se
  leía. `UrbanOutlineButton` tiene ahora `onPhoto`, que en un tema claro usa dorado claro; en los temas
  oscuros la bienvenida no cambia.
- Pendiente: 320 dp y fuente grande (el teléfono se desconectó al cambiar la densidad), teclado abierto en
  acceso, estado «correo enviado», Seguridad y Ajustes en Libreta, TalkBack real.

## Notificaciones: bandeja y destino de los push (5 de noviembre de 2026)

Hecho sin teléfono (compila, lint y pruebas en verde); falta el recorrido físico.

- Bandeja nueva (`NotificationsScreen.kt`): «Nuevas» (todo lo no leído) arriba y lo leído por antigüedad
  (Hoy, Ayer, Esta semana, Anteriores); filtro Todas / No leídas; una fila por aviso con ícono por tipo,
  tiempo relativo («Hace 5 min», «Ayer», «20 oct»), fondo dorado tenue y punto si no se ha leído; menú por
  aviso (marcar como leída, eliminar) y las mismas acciones para TalkBack; «Marcar todas como leídas» como
  botón de texto. Eliminar usa `DELETE /notifications/{id}`, que el backend ya tenía.
- Destino del toque: la bandeja y el push comparten `routeForNotificationType`. Se añaden `service_ending`
  (agenda del barbero), `barber_review_flagged` (reseñas) y `barber_performance_report` (reportes). Si un
  push llega con la ruta genérica «notifications» pero con un `type` que la app conoce, abre la pantalla del
  tipo en vez de la bandeja (`pushRoute`).
- Límite conocido: el push abre la **pantalla** del módulo, no el elemento. El push no trae el id de la cita
  ni del pedido (`AppointmentNotification::toWebPush` no lo incluye y `PushesToDevices` solo añade
  tipo, canal y ruta), y la app no tiene una vista de detalle por cita o pedido a la que enlazar. Abrir el
  elemento exacto necesita un cambio en el backend (ids en el push) y una pantalla de destino en la app.
- Además: se quitan tres espaciadores finales de 80 dp en Campañas, Ofertas y Servicios, que se sumaban a los
  88 dp que el andamio ya reserva bajo el botón flotante.

## Muro de Inspiración en la barra del cliente (5 de noviembre de 2026)

- La barra inferior del cliente pasa de cuatro a cinco destinos: Inicio, Reservar, Mis citas, Muro y
  Cuenta (la etiqueta «Inspiración» se cortaba a 320 dp, «Inspiraci…»; «Muro» cabe). El personal no cambia: lo sigue teniendo en «Más» y el barbero en su inicio.
- Como es una pestaña, el muro no muestra flecha de volver para el cliente (`onBack` ahora puede ser nulo en
  `UrbanModuleScreen` y `SocialFeedScreen`). Con cinco destinos las etiquetas usan una talla menos.
- Verificado en el teléfono a 320 dp (cliente demo): la barra completa se lee. Pendiente: fuente grande y el
  regreso desde el atajo del inicio del cliente.

## Aviso al reportante (5 de noviembre de 2026)

- Backend (rama `feat/aviso-reportante`, commit local sin subir): al resolver los reportes de un comentario,
  cada reportante recibe un aviso una sola vez («eliminamos el comentario» o «cumple con las reglas, se
  conserva»), sin revelar al autor. Canal de Android nuevo «comunidad», tipo `report_resolved`, ruta `social`.
- App: `PushChannel.COMUNIDAD`, `report_resolved` → pantalla `social`, ícono propio en la bandeja y pruebas.
- Pendiente: el backend debe publicarse por PR y desplegarse antes de que el aviso llegue; verificar en el
  teléfono cuando esté desplegado.
