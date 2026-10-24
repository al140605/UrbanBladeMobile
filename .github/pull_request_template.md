## Qué cambia y por qué

<!-- Una o dos frases. Enlaza la tarea o historia (por ejemplo T077 / HT-26). -->

## Lista de verificación del revisor (T077)

Marca cada punto antes de aprobar. Si no aplica, escribe «N/A» y por qué.

### Estándares
- [ ] El cambio hace una sola cosa y el título del PR la describe (en español, sin créditos de herramientas).
- [ ] El código Kotlin sigue el estilo del archivo que toca y los colores salen de `UrbanColors`, no de valores sueltos.
- [ ] No quedan código muerto, `Log.d` de depuración ni comentarios sobrantes.
- [ ] Los textos de la interfaz están en español, sin faltas, y los botones tienen descripción de accesibilidad.

### Seguridad y privacidad
- [ ] No hay secretos, tokens, contraseñas ni correos reales en el código, los recursos ni los datos de prueba.
- [ ] Ninguna clave se escribe en el código: salen de `local.properties` o de `-P`.
- [ ] Un permiso nuevo del `AndroidManifest.xml` está justificado y declarado en la ficha de Play.
- [ ] Si cambia qué datos se recogen o comparten, se actualizó «Seguridad de los datos» y el aviso de privacidad.

### Pruebas
- [ ] Hay pruebas nuevas o actualizadas que fallan sin el cambio (ViewModel o Compose UI Test).
- [ ] `gradle :app:testDebugUnitTest` y `gradle :app:lintDebug` pasaron y el CI está en verde.
- [ ] Probé el caso feliz y al menos un caso de error (sin red, sesión vencida, pantalla vacía).

### Documentación
- [ ] El README o la documentación afectada está actualizada.

### Publicación
- [ ] Si toca `versionCode` o `versionName`, coincide con lo que se sube a Play.
- [ ] El build de release (R8) compila: se revisó la regla de `proguard-rules.pro` si se agregó un DTO.

## Cómo lo probé

<!-- Comandos ejecutados y resultado, o capturas si es un cambio visual. -->
