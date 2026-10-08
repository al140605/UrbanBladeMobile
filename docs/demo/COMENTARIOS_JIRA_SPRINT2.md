# Comentarios para Jira — Sprint 2 (corte 7-oct-2026)

Pegar un comentario por tarea. Solo se marca **Completada** lo que tiene evidencia; lo demás queda como está.
(No tengo acceso a Jira desde esta sesión: estos textos son para copiar y pegar.)

## Completadas (con evidencia)

**T142 — Push FCM en la app Android** (Luis)
> Completada. Integración con Firebase Cloud Messaging (`UrbanMessagingService`, canal «citas», permiso `POST_NOTIFICATIONS`). Verificado el 7-oct en SM-S731B (Android 16, build staging): la app abre sin fallos y registra el servicio. Evidencia: capturas en `docs/demo`.

**T143 — Tokens de dispositivo y recordatorios push** (Alan)
> Completada. Backend registra tokens Android y genera recordatorios; en la app las citas muestran «Recordatorio por enviar». Pendiente de ensayo de extremo a extremo antes de la demo.

**TT01 — Vigencia del token y `expires_at`** (Alan)
> Completada. Login, registro y Google emiten `expires_at`; la app muestra «Tu sesión venció» al expirar.

**TT02 — Pruebas PHPUnit de token vencido** (Miguel)
> Completada. Cubierto en `ApiTokenSecurityTest` y `CleanExpiredTokensCommandTest`.

**TT38 — Mapa 3D del sistema (`/system`, three.js)** (Luis)
> Completada. Mapa 3D alimentado por los datos del admin; se mostrará en la demo.

## Avance parcial

**TT03 — Aviso de sesión expirada** (Elías)
> En progreso. Android listo (banner en la pantalla de bienvenida). Falta el aviso en la web.

**T112–T116 — Terraform** (Alan, María, Luis, Elías)
> En progreso (inicio de Terraform, según calendario del Sprint 2). Cada integrante actualizará su tarea con el enlace al código cuando lo suba al repositorio. T114 (variables, outputs y `terraform.tfvars`) — Luis: iniciada.

## Pendientes (sin cambios)

- **TT04** (Alan): forgot-password responde distinto si el correo existe (200) o no (400); debe dar mensaje genérico.
- **TT05** (María): el token de recuperación vence en 60 min (`config/auth.php`); debe ser 15 min.
- **TT06** (Miguel): pruebas de recuperación.
- **T145 / T084** (Miguel): pruebas instrumentadas y en dispositivos; no existe `androidTest`.
- **TT35** (Elías): matriz RACI y plan de comunicación.
