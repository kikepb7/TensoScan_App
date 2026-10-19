# A-003 — Migración de features existentes · Tareas

> Requisito previo: A-001. T-A003-04…07 requieren A-002 fases C y D. T-A003-01…03 y 08…09
> requieren los endpoints `/v1/auth/*`, `/v1/users/me` y `/v1/assistant/*` de B-001 y B-002.
> Se puede intercalar con A-002 (las features son independientes).
> Commit: `T-A003-xx | <descripción en inglés>`.

- [x] **T-A003-01 · Arranque según la sesión y manejo de la expiración**
  - `AppViewModel`, *splash* condicionada, `startDestination` dinámico y el evento de sesión
    expirada.
  - Req.: REQ-A003-01, 02 · Hallazgos: AUD-A-45.
- [x] **T-A003-02 · Login a UDF + `/v1/auth/login`**
  - `LoginViewModel` con `onAction`, un evento de navegación, errores tipados, el modelo de tokens
    con refresh, y `REFRESH_TOKEN_ENABLED = true` (A-001).
  - Req.: REQ-A003-03…05 · Hallazgos: AUD-A-15, 42.
- [x] **T-A003-03 · Registro a UDF + validación local + `/v1/auth/register`**
  - Req.: REQ-A003-06, 07 · Hallazgos: AUD-A-21, 22 (`registerUSer`).
- [x] **T-A003-04 · Historial a UDF**
  - Estados, *pull-to-refresh*, confirmación de borrado e indicadores de sincronización.
  - Sustituye `SummaryViewModel` y `SummaryScreen`; elimina el `CoroutineScope` en el composable.
  - Req.: REQ-A003-08…11 · Hallazgos: AUD-A-04 (Summary), 16, 18, 19, 47.
- [x] **T-A003-05 · Gráfico nativo y eliminación del WebView**
  - Req.: REQ-A003-13 · Hallazgos: AUD-A-17, 46.
- [x] **T-A003-06 · Exportar y compartir el PDF**
  - Req.: REQ-A003-14.
- [x] **T-A003-07 · `BloodPressureClassifier` + UI de categoría + texto orientativo**
  - Bloqueada para publicar (no para desarrollar) por Q-SAFE-02.
  - Req.: REQ-A003-12 · Hallazgos: AUD-A-43.
- [x] **T-A003-08 · Perfil y logout**
  - `/v1/users/me` y `/v1/auth/logout` con aviso de pendientes.
  - Req.: REQ-A003-15, 16 · Hallazgos: AUD-A-20.
- [x] **T-A003-09 · Asistente a UDF + `/v1/assistant/messages` + aviso**
  - Req.: REQ-A003-17 · Hallazgos: AUD-A-22 (`conntent`).
- [x] **T-A003-10 · Barrido de textos y accesibilidad**
  - Req.: REQ-A003-19 · Hallazgos: AUD-A-44.
- [x] **T-A003-11 · Barrido de nombres, ficheros y visibilidad**
  - Req.: REQ-A003-20 · Hallazgos: AUD-A-07, 08, 09.
- [x] **T-A003-12 · Eliminar el código muerto y vaciar el *baseline* de detekt**
  - Req.: REQ-A003-21 · Hallazgos: AUD-A-11 (cierre).

- [x] **T-A003-13 · Eliminar cuenta + consentimiento de privacidad en el registro**
  - Req.: REQ-A003-22, 23 · Requiere `DELETE /v1/users/me` (B-001 T-B001-14).

## Cierre de la spec
- [ ] CA-A003-01…08 verificados.
- [ ] Coordinar con el backend la retirada de las rutas legacy (contrato api-v1 §6).
