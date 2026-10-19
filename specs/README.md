# TensoScan App — Specs (Spec-Driven Development)

Este directorio es la **fuente de verdad** del desarrollo de la rama `recognition-refactor-feature`.
Ningún cambio de código se hace sin una spec aprobada que lo justifique, y cada commit referencia
el identificador de la tarea (`T-xxx`) y de los requisitos (`REQ-xxx`) que implementa.

## 1. Objetivo de la iniciativa

Sustituir el flujo actual de *foto → subida al backend → reconocimiento en servidor* por un flujo
**en tiempo real y en el dispositivo**:

1. El usuario pulsa «Nueva medición» y se abre la cámara.
2. Enfoca el display del tensiómetro dentro del recuadro guía.
3. La app detecta los dígitos en vivo, los superpone en pantalla y, cuando la lectura es estable
   durante varios frames, la captura automáticamente.
4. Se muestra una pantalla de revisión donde el usuario **confirma o corrige** sistólica,
   diastólica y pulso. Se contemplan todos los casos de error (permisos, cámara, modelo,
   lectura imposible, valores no plausibles, guardado y sincronización).
5. La medición se guarda en local al instante (offline-first) y se sincroniza con el backend.

En paralelo, se refactorizan ambos repositorios para seguir las buenas prácticas definidas en
`constitution.md` (skills `android-architecture`, `android-patterns` y `android-testing`).

> **Plan de implementación y decisiones vigentes**: [`implementation-plan.md`](implementation-plan.md).
> Resumen: `applicationId = com.tensoscan` · app comercial · zona horaria Europe/Madrid · primero
> todo el código (etapa 1) y después pruebas con datos reales y entrenamiento (etapa 2).

## 2. Mapa de specs

| ID | Spec | Tipo | Depende de | Estado |
|----|------|------|-----------|--------|
| 000 | [Auditoría del estado actual](000-baseline-audit/audit.md) | Análisis | — | ✅ Redactada |
| 001 | [Fundación de plataforma](001-platform-foundation/requirements.md) | Refactor | 000 | 👀 En revisión |
| 002 | [Captura de medición en tiempo real](002-realtime-measurement-capture/requirements.md) | Feature | 001 · Backend 002 (sync) · Backend 003 (modelo final) | 👀 En revisión |
| 003 | [Migración de features existentes](003-legacy-features-migration/requirements.md) | Refactor | 001 | 👀 En revisión |

Cada spec contiene:

- `requirements.md`: **qué** y **por qué**. Historias de usuario, requisitos en formato EARS con
  ID, criterios de aceptación verificables, requisitos no funcionales, fuera de alcance y preguntas
  abiertas.
- `design.md`: **cómo**. Arquitectura, modelos, flujos, máquinas de estado, catálogo de errores,
  decisiones (ADR) con alternativas descartadas y su motivo, estrategia de test y riesgos.
- `tasks.md`: **en qué orden**. Tareas pequeñas (≤ 1 día), con dependencias, requisitos que cubren
  y Definition of Done.

Contratos compartidos con el backend (copias sincronizadas, la fuente canónica está en
`TensoScan_Backend/specs/contracts/`):

- [`contracts/api-v1.md`](contracts/api-v1.md): API REST v1.
- [`contracts/digit-detector-model.md`](contracts/digit-detector-model.md): contrato de entrada y salida del modelo LiteRT.
- [`contracts/reading-rules.md`](contracts/reading-rules.md): rangos, validación y reglas de
  parseo compartidas, más los casos dorados.

## 3. Hoja de ruta entre repositorios

```
            BACKEND                                   APP
 ┌────────────────────────────┐        ┌────────────────────────────────┐
 │ B-001 Fundación            │        │ A-001 Fundación                │
 │  (config, auth, errores,   │        │  (toolchain, Hilt, módulos,    │
 │   tests, CI, Docker)       │        │   tests, red, errores)         │
 └─────────────┬──────────────┘        └───────────────┬────────────────┘
               │                                       │
 ┌─────────────▼──────────────┐  contrato  ┌───────────▼────────────────┐
 │ B-002 API Measurements v1  │◄──────────►│ A-002 Captura tiempo real  │
 │  (POST idempotente, tipado,│  api-v1    │  Fase A: pipeline + ML Kit │
 │   paginación, informes)    │            │  Fase B: revisión + guardado│
 └─────────────┬──────────────┘            │  Fase C: sync offline-first│
               │                           │  Fase D: modelo propio     │
 ┌─────────────▼──────────────┐  modelo    └───────────▲────────────────┘
 │ B-003 Modelo de dígitos    │──.tflite──────────────────┘
 │  (sintéticos, entrenamiento│  + contrato
 │   evaluación, export)      │
 └────────────────────────────┘            ┌────────────────────────────┐
                                           │ A-003 Migración features    │
                                           │  (login, registro, historial│
                                           │   usuario, chatbot)         │
                                           └────────────────────────────┘
```

**Ruta crítica**: A-001 → A-002 (fases A y B) → B-002 → A-002 (fase C).
El modelo propio (B-003) **no bloquea** el desarrollo de la feature: A-002 empieza con un detector
basado en ML Kit detrás de la misma interfaz `DigitDetector`, y se cambia por el modelo LiteRT con
una sola línea de inyección de dependencias (ver ADR-A002-03).

## 4. Flujo de trabajo SDD con Claude Code

1. **Specify**: se redacta o actualiza `requirements.md` y lo revisa una persona.
2. **Design**: se redacta `design.md`. Las decisiones relevantes van como ADR dentro del propio
   documento.
3. **Tasks**: se desglosa en `tasks.md`.
4. **Implement**: Claude Code implementa **una tarea cada vez**, en el orden indicado:
   - lee `constitution.md` y las skills de `.claude/skills/` antes de escribir código;
   - escribe los tests indicados en la tarea (preferiblemente antes del código);
   - ejecuta `./gradlew testDebugUnitTest detekt` (o lo que indique la tarea);
   - marca la tarea como `[x]` en `tasks.md` y hace commit con el formato
     `T-xxx | <descripción>` (por ejemplo, `T-A002-07 | Implement DisplayLayoutParser`).
5. **Verify**: los criterios de aceptación de `requirements.md` se comprueban en la revisión del PR.
   Si la implementación obliga a cambiar la spec, **primero se actualiza la spec** y después el
   código.

Estados de una spec: `📝 Borrador` → `👀 En revisión` → `✅ Aprobada` → `🚧 En implementación` → `🏁 Completada`.

## 5. Glosario

| Término | Significado |
|---------|-------------|
| SYS / DIA / PUL | Presión sistólica / diastólica (mmHg) / pulso (ppm) |
| ROI | *Region of interest*: el recuadro guía donde el usuario coloca el display |
| Frame | Imagen individual entregada por CameraX `ImageAnalysis` |
| Detección | Caja + clase (dígito o símbolo) + confianza devuelta por el detector |
| Lectura de frame | SYS/DIA/PUL (posiblemente parcial) deducida de las detecciones de un frame |
| Lectura estable | Lectura en la que coinciden suficientes frames recientes (consenso temporal) |
| Borrador | Lectura pendiente de que el usuario la confirme en la pantalla de revisión |
| Offline-first | Se guarda primero en local y se sincroniza en segundo plano |
