# Vista pública editorial B — TT32 / HT-12

Dirección visual elegida por el usuario y ajustada en dispositivo el 22-oct-2026.

## Resultado
- Bienvenida con fotografía vertical a pantalla completa, sin franjas ni acceso Ver servicios. Barbero y cliente visibles; marca arriba; título, descripción y panel de acciones en la parte inferior.
- Panel de bienvenida con borde sutil, Crear mi cuenta como acción principal e Iniciar sesión como secundaria.
- Acceso, registro y recuperación con cabecera fotográfica, marca UB sin recuadro y formulario sobre un panel opaco. Con teclado se oculta la foto y la acción principal permanece fuera del contenido desplazable.
- Catálogo invitado con cabecera compacta, aviso del requisito de cuenta, buscador/filtros y tarjetas con imagen, precio y duración. El catálogo autenticado conserva su presentación anterior.

## Componentes y recursos
UrbanPublicHero y UrbanPublicPanel viven en ui/components/UrbanPublicEditorial.kt. Campos, botones y estados reutilizan el kit del proyecto. Los paneles usan UrbanColors; el texto sobre fotografía mantiene un velo oscuro para su legibilidad.

public_barber_editorial.png es fotografía decorativa generada para la propuesta; public_welcome_portrait.png adapta esa escena a formato vertical. No representan personal real ni una sede verificada, y no reemplazan las imágenes de servicios/barberos de la API. urbanblade_brand_symbol.xml reutiliza los trazos UB originales sin el margen del icono adaptativo.

## Validación
- assembleDebug, testDebugUnitTest y lintDebug completados durante el rediseño: 347 pruebas, 0 fallos, 0 errores y 0 omitidas. Lint terminó con advertencias; no se declara libre de advertencias.
- Los ajustes visuales posteriores se compilaron e instalaron en com.urbanblade.mobile.audit, junto a la versión de Play, sin reemplazarla.
- Verificación en Samsung SM-S731B: bienvenida, catálogo con datos sintéticos, selección de servicio hacia acceso, búsqueda sin resultados/limpieza, acceso con teclado, registro paso 1/desplazamiento y recuperación sin envío.
- Captura final de bienvenida: welcome-fullscreen-portrait.png, en las visualizaciones del chat. Foto a pantalla completa y ambos rostros libres de la marca; acciones inferiores alcanzables.

## Límites
No se validaron registro paso 2 en dispositivo, OAuth, envío real de recuperación, recorrido autenticado, todos los temas, fuente ampliada ni TalkBack en esta revisión. No se crearon cuentas, reservas ni cobros reales. Sin cambios de endpoints, precios, lógica de autenticación, consentimiento, versionCode o firma de publicación.

El commit conserva únicamente el rediseño público aprobado. Las vistas autenticadas del cliente se diseñan después mediante propuestas visuales. No fusionar con main hasta terminar y aprobar el rediseño completo.
