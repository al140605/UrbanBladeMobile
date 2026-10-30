# Documentación de Android

Estado de distribución consultado el 30 de octubre de 2026: [verificación directa de Play Console](play-store/ESTADO.md).


Revisión documental: 30 de octubre de 2026.

La documentación operativa se mantiene junto al código. Los resultados fechados se conservan como evidencia y no sustituyen una comprobación del commit que se va a entregar.

## Guías vigentes

| Documento | Propósito |
|---|---|
| [README del proyecto](../README.md) | Alcance, entorno y primera compilación |
| [Configuración](CONFIGURACION.md) | Variantes, API, sesión de pruebas e integraciones |
| [Arquitectura](ARQUITECTURA.md) | Responsabilidades, datos y límites de confianza |
| [Decisiones técnicas](DECISIONES.md) | Motivos y consecuencias de las decisiones existentes |
| [Pruebas](PRUEBAS.md) | Comandos, evidencia y criterios de entrega |
| [CI/CD](CI_CD.md) | Workflows, artefactos y distribución |
| [Seguridad documental](SEGURIDAD_DOCUMENTACION.md) | Secretos, capturas, logs y límites de revisión |
| [Interfaz](INTERFAZ.md) | Temas, roles y revisión de pantallas |
| [Herramientas](HERRAMIENTAS.md) | Entorno y diagnóstico utilizados |
| [Capturas móviles](CAPTURAS.md) | Procedencia y selección de imágenes |
| [Mantenimiento reproducible](MANTENIMIENTO_REPRODUCIBLE.md) | Wrapper, TLS y compilación de CodeQL |
| [Contratos API](AUDITORIA_CONTRATOS_API.md) | Auditoría inicial de rutas y sus límites |
| [Contrato de experiencia](UI_UX_CONTRATO_MOVIL.md) | Comportamiento esperado de la interfaz |
| [Módulo remoto](INTEGRACION_MODULO_REMOTO.md) | Integración de campañas y validaciones |
| [Verificación manual](VERIFICACION_MANUAL_P0.md) | Casos de revisión funcional |
| [Publicación](PUBLICAR_PLAY_STORE.md) | Preparación técnica y referencia a registros de Play |

## Evidencia y referencias históricas

`ui-audit-2026-09-24/`, `ui-audit-2026-10-11/` y [validación de interfaz](UI_UX_VALIDACION_2026-10-11.md) documentan revisiones fechadas. Las propuestas y comparaciones no representan necesariamente la versión publicada.

[Documentación de demostraciones](demo/GUION_Y_CHECKLIST_DEMO_SPRINT2.md), [registro de Play Console](play-store/README.md), [inventario de diseño](INVENTARIO_FIGMA.md), [rediseño inicial](FRONTEND_REDESIGN.md) y [QA de diseño](../design-qa.md) se conservan como contexto histórico. No utilizar cifras de testers, estados de revisión o configuraciones antiguas como confirmación actual.

## Mantenimiento editorial

Usar títulos descriptivos, lenguaje directo y tablas cuando faciliten comparar. No usar emojis ni imágenes del cliente web para ilustrar Android. Antes de añadir capturas, revisar datos personales, notificaciones, correos, tokens y elementos superpuestos del sistema. Mantener enlaces relativos y registrar fecha, entorno y alcance de cada evidencia.

Actualizar una guía cuando cambia su implementación; conservar procedimientos de publicación y evidencias de decisiones. La información que envejece debe llevar fecha y alcance, evitando presentar planes como capacidades entregadas.

## Políticas del proyecto

[Seguridad](../SECURITY.md) · [Contribución](../CONTRIBUTING.md) · [Código de conducta](../CODE_OF_CONDUCT.md) · [GPL-3.0-only](../LICENSE) · [Alcance de licencia](../LICENSING.md).

## Trabajo en curso

[Plan de mejoras UI/UX y compatibilidad con API](PLAN_MEJORAS_UI_UX.md): referencias, prioridades, contratos observados y criterios previos al PR. Las propuestas no se consideran implementadas.
