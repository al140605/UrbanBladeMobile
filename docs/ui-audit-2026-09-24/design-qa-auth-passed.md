# Design QA — Bienvenida y autenticación

## Evidencia

- Fuente visual: `C:\Users\luis1\.codex\generated_images\01a0d20a-d904-7ee0-bcf4-420c580d829c\exec-47250d1f-6232-4044-b484-34d8ba12bc92.png`
- Implementación final: `docs/ui-audit-2026-09-24/17-welcome-final.png`
- Comparación conjunta final: `docs/ui-audit-2026-09-24/18-welcome-final-comparison.jpg`
- Login real: `docs/ui-audit-2026-09-24/12-login-redesign.png`
- Registro real: `docs/ui-audit-2026-09-24/13-register-redesign.png`
- Dispositivo: Samsung físico `R5GL44QP8TT`, Android, orientación vertical.
- Viewport de app: 360 × 780 dp aproximados; captura nativa 1080 × 2340 px, densidad aproximada 3×.
- Fuente: 853 × 1844 px. Para la comparación conjunta, fuente e implementación se normalizaron visualmente a 540 × 1170 px cada una, sin marco externo.
- Estado: invitado; bienvenida, login y registro sin teclado y sin datos escritos.

## Comparación visual final

La composición conserva la intención elegida: fondo cinematográfico cálido, marca compacta arriba, eyebrow cobre, titular editorial serif, texto secundario sobrio, Bladebot como protagonista y una acción primaria claramente dominante. La implementación adapta el mock al alto real del dispositivo y respeta las barras del sistema.

No fue necesaria una comparación recortada adicional: en la comparación conjunta final se leen con claridad marca, tipografía, mascota, botones y espaciado. Login y Registro se revisaron además en capturas completas independientes porque no existía un mock separado para esos estados.

## Superficies de fidelidad

- Tipografía: jerarquía equivalente al mock; serif de alto contraste para el mensaje principal y sans legible para marca, apoyo y controles. No hay truncamientos ni saltos problemáticos.
- Espaciado y ritmo: márgenes constantes, separación clara entre contenido editorial, mascota y acciones. Registro cabe completo sin recortes en 360 × 780 dp.
- Colores y tokens: negro cálido, blanco suave, grises y cobre mantienen contraste y continuidad. Los estados deshabilitados son visibles sin confundirse con acciones activas.
- Imagen: fotografía local nítida, recorte vertical estable y overlays oscuros suficientes para lectura. Bladebot usa el asset real de la app, sin sustitutos dibujados.
- Copy: bienvenida coincide con la dirección aprobada; Login y Registro tienen mensajes breves, directos y coherentes.
- Accesibilidad y controles: acciones principales tienen superficies amplias; las rutas Bienvenida → Login → Registro se pudieron activar en el teléfono. La burbuja circular visible en la esquina superior derecha pertenece a otra aplicación del dispositivo y no a UrbanBlade.

## Historial de iteraciones

1. Primera captura (`11-welcome-redesign.png`): P2 — Bladebot estaba pequeño y demasiado bajo; el copy secundario se apartaba del visual elegido.
   - Corrección: copy alineado con el mock, mascota aumentada y elevada.
   - Evidencia posterior: `15-welcome-final.png` y `16-welcome-final-comparison.jpg`.
2. Segunda comparación: P2 — la mascota aún perdía demasiada presencia respecto a la fuente.
   - Corrección: tamaño final de 300 dp y separación inferior de 88 dp dentro del área editorial.
   - Evidencia posterior: `17-welcome-final.png` y `18-welcome-final-comparison.jpg`.

## Findings

No quedan diferencias P0, P1 o P2 accionables. La variación residual en el recorte exacto del fondo y la posición de Bladebot es aceptable por la relación de aspecto distinta entre el mock y el dispositivo real.

## Interacciones y estabilidad

- Probado: abrir Bienvenida, entrar a Login y navegar a Registro.
- Probado: composición completa sin teclado; no se enviaron formularios ni se alteraron datos de usuario.
- Compilación: `:app:assembleDebug` exitosa con Gradle 8.13 y JBR 21.
- Instalación: APK instalada correctamente mediante ADB.
- Errores de ejecución: no aparecieron entradas `AndroidRuntime:E` en la revisión posterior al lanzamiento.

## Follow-up polish

- P3: migrar en otra tarea los iconos direccionales deprecados a variantes `AutoMirrored`; no afecta la pantalla actual ni bloquea la entrega.

final result: passed
