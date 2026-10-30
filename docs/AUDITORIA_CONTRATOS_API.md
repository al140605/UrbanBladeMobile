# Contraste de rutas Android / backend

Base Android revisada: origin/main 37bfa25. Fuente: UrbanBladeApi.kt.
Backend: rutas api/v1 obtenidas de la API local aislada de Barber mediante
route:list --json; no se invocaron operaciones de negocio.

Se compararon 146 combinaciones únicas de método y ruta de Retrofit con 210
combinaciones de la API local. Se normalizaron nombres de parámetros y se
excluyó HEAD del inventario del servidor.

- 145 coincidencias directas de método y ruta.
- 1 adaptación intencional: actualización de inventario como POST multipart
  con el campo _method=PUT en UrbanRepository.updateProduct. El backend expone
  PUT inventory/products/{product}; Laravel interpreta la sobreescritura del método.
- No se identificó una ruta cliente ausente después de revisar esa adaptación.

Este contraste solo comprueba correspondencia de rutas. No prueba payloads,
reglas por rol, autorización de recursos, pagos, notificaciones ni los flujos
de interfaz. Tampoco declara cubiertas todas las capacidades del backend por
el cliente móvil. La siguiente validación debe recorrer esos contratos con
cuentas ficticias y una variante de auditoría contra la API local.
