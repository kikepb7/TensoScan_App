# TensoScan App — Guía para Claude Code

Este repositorio se desarrolla con **Spec-Driven Development**. La fuente de verdad es `specs/`.

## Antes de escribir código
1. Lee `specs/README.md` (mapa de specs y flujo) y `specs/constitution.md` (reglas no negociables).
2. Carga las skills del proyecto que apliquen:
   - `android-architecture`: dónde va cada cosa, nombres, Hilt y visibilidad;
   - `android-patterns`: UDF, ViewModel, flows, errores y Compose;
   - `android-testing`: JUnit 5, MockK, Turbine, `sut`, `expected`/`actual`.
3. Localiza la tarea en el `tasks.md` de la spec activa y lee los `REQ-*` y las secciones de
   `design.md` que referencia.

## Al implementar
- **Una tarea cada vez**, en el orden de `tasks.md`. No implementes nada que no esté en una spec.
  Si falta algo, propón primero el cambio de spec.
- Escribe los tests indicados en la tarea (en `:domain`, antes que el código).
- Los contratos compartidos con el backend están en `specs/contracts/`. **No los edites aquí**:
  la fuente canónica es `TensoScan_Backend/specs/contracts/`.
- Los casos dorados (`reading-golden-cases.json`) se consumen desde los tests vía
  `syncContractFixtures` y no se copian a mano.

## Verificación (Definition of Done)
```bash
./gradlew verifyModuleBoundaries detekt testDebugUnitTest assembleDebug
```
- Marca la tarea con `[x]` en `tasks.md`.
- Commit: `T-<spec>-<nn> | <imperative description in English>` (por ejemplo,
  `T-A002-07 | Implement DisplayLayoutParser`).

## Recordatorios críticos
- Ninguna lectura se guarda sin confirmación del usuario (C-SAFE-01).
- Los frames de cámara no salen del dispositivo ni se escriben en disco (C-PRIV-01).
- `:domain` es Kotlin puro: nada de Android, Hilt ni `javax.inject`.
- No uses `runCatching` en producción (captura la cancelación); usa `suspendRunCatching`.
