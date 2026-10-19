# A-002 — Captura de medición en tiempo real · Tareas

> Requisito previo: A-001 completada (al menos T-A001-01…09 y T-A001-11).
> Cada tarea cumple la DoD de la constitución. **TDD**: en las tareas de `:domain` los tests se
> escriben primero, a partir de los casos dorados y de los escenarios de design §12.
> Commit: `T-A002-xx | <descripción en inglés>`.

## Fase A — Limpieza y núcleo de dominio (puro, sin Android)

- [x] **T-A002-01 · Retirar la captura legacy**
  - Eliminar: guardado en DCIM (`CameraRepositoryImpl.takePhoto/saveMedia/saveVideo`),
    `SavePhotoUseCase`, `recordVideo`, el botón de vídeo, el botón de galería sin efecto, el cambio
    a la cámara frontal, `bitmapToFile`, `PermissionManager`, `PermissionViewModel`, `Constants`
    y el permiso `RECORD_AUDIO` (de los dos manifiestos).
  - La pestaña «Cámara» muestra temporalmente un *placeholder* «Próximamente» hasta T-A002-19.
  - Hallazgos: AUD-A-24, 25, 26, 27, 28, 30, 41 · Req.: REQ-A002-05, 06, 25.
- [x] **T-A002-02 · Modelos de dominio de medición**
  - `measurement/model/*` (design §4). `MeasurementModel` (legacy, con `String`) convive marcado
    `@Deprecated` hasta T-A002-31.
  - Hallazgos: AUD-A-32.
- [x] **T-A002-03 · `ReadingLimits` + `ReadingValidator`**
  - Tarea Gradle `syncContractFixtures` (design §12.1).
  - Tests: todos los `validation_cases`, más `IN_FUTURE` y `TOO_OLD` con un `Clock` falso.
  - Req.: REQ-A002-28 (lógica), contrato `reading-rules.md` §2–3.
- [x] **T-A002-04 · Modelos de dominio de escaneo** (`scan/model/*` y `error/DetectorError`).
- [x] **T-A002-05 · Interfaz `DigitDetector`** (sin implementación).
- [x] **T-A002-06 · `NumberGrouper`** (pasos 1–3 de reading-rules §4.3).
  - Tests: agrupación de glifos, hueco y solape límite, altura dispar, clasificación de los
    grupos (error, midiendo, hora y número).
- [x] **T-A002-07 · `DisplayLayoutParser`**
  - Tests: **todos** los `parser_cases` (`@TestFactory`) más `primaryHeight`.
  - Req.: REQ-A002-14.
- [x] **T-A002-08 · `ReadingStabilizer`**
  - Tests: escenarios de design §12.1.
  - Req.: REQ-A002-16, 17.
- [x] **T-A002-09 · `FrameExposureEvaluator`**
  - Req.: REQ-A002-21.
- [x] **T-A002-10 · `RecognizeFrameUseCase` + `PrepareDetectorUseCase`**
  - Req.: REQ-A002-13, 14.
- [x] **T-A002-11 · `ScanSession`**
  - Pistas, temporizadores, comandos y congelado tras la captura (design §6.5).
  - Req.: REQ-A002-09, 19, 20, 22, 23.

## Fase B — Escaneo en vivo con el detector ML Kit

- [x] **T-A002-12 · `MlKitDigitDetector` + `MlKitSymbolMapper`**
  - Nota de implementación: se usa el nivel `Text.Symbol` de ML Kit (caja y confianza por carácter); compila con `text-recognition:16.0.1`.
  - Dependencia de ML Kit empaquetado y reglas R8.
  - Tests del mapper (tabla §9.1).
  - Req.: REQ-A002-13.
- [x] **T-A002-13 · `FallbackDigitDetector` + `DetectorModule`**
  - Nota de implementación: `FallbackDigitDetector` y `UnavailableDigitDetector` viven en `:domain` (Kotlin puro, testeables en JVM); `DetectorModule` está en `:data` (visibilidad `internal`, `ViewModelComponent`) y `ScanModule` en `:app`. No hay flag `BuildConfig`: el primario se cambia en `DetectorModule`.
  - Flag `BuildConfig.DIGIT_DETECTOR_PRIMARY = "mlkit"`. Sin LiteRT todavía, el respaldo es un
    `UnavailableDigitDetector` (devuelve `ModelMissing`).
  - Tests del cambio por 5 fallos y de `prepare` con primario y respaldo.
  - Req.: REQ-A002-24.
- [x] **T-A002-14 · Contratos de presentación del escaneo + `ScanUiStateMapper`**
  - Nota de implementación: los tipos son `public` porque `:presentation` y `:ui` son módulos distintos; las rutas tipadas (`ScanRoute`, `ReviewMeasurementRoute`) viven en `:presentation` para que los ViewModels las emitan y las lean de `SavedStateHandle`.
  - Tests del mapper (roles de las cajas, «—» y guía).
- [x] **T-A002-15 · `ScanViewModel`**
  - Permisos, `prepare`, `onFrame` y cola de comandos, captura, inactividad y «Usar lo leído».
  - Tests con Turbine (design §12.1).
  - Req.: REQ-A002-02…04, 09, 10, 16…24, 44.
- [x] **T-A002-16 · `RoiGeometry` + recuadro guía en dp**
  - Sustituye `CameraUserAssistantView` (píxeles fijos).
  - Tests JVM de las transformaciones.
  - Hallazgos: AUD-A-29 · Req.: REQ-A002-12.
- [x] **T-A002-17 · `ScanCameraPreview` + `ImageProxyFrameConverter`**
  - CameraX `IMAGE_ANALYSIS`, RGBA_8888, `KEEP_ONLY_LATEST`, executor dedicado, linterna,
    enfoque y medición en la ROI, ciclo de vida y errores de cámara.
  - Hallazgos: AUD-A-23 · Req.: REQ-A002-06…11.
- [x] **T-A002-18 · `ScanScreen` / `ScanContent` y componentes**
  - Overlay, panel de valores en vivo, banner de guía, contenido de permisos y no disponible,
    acciones de timeout, haptic, `LiveRegion`, bloqueo vertical y `@Preview` de **cada** estado de
    `ScanScreenUiState`.
  - Req.: REQ-A002-15, 18, 44, 45.
- [x] **T-A002-19 · Navegación**
  - `ScanRoute` y `ReviewMeasurementRoute` (tipadas). FAB «Nueva medición» en el historial y
    pestaña «Cámara» → `ScanRoute`.
  - Req.: REQ-A002-01.
- [ ] **🔎 Punto de control 1**: demo en dispositivo del escaneo en vivo con ML Kit. Se revisa la
  UX con el usuario antes de continuar.

## Fase C — Revisión y guardado local

- [x] **T-A002-20 · `ReadingInputParser`** (con tests).
- [x] **T-A002-21 · Casos de uso de guardado**
  - `ValidateReadingUseCase`, `CheckDuplicateMeasurementUseCase`, `SaveMeasurementUseCase`
    (`editedFields`) e interfaces `MeasurementRepository` y `MeasurementSyncScheduler`.
  - Req.: REQ-A002-30, 31, 34.
- [x] **T-A002-22 · `ReviewMeasurementViewModel` + contratos + mapper**
  - Req.: REQ-A002-26…33, 35.
- [x] **T-A002-23 · `ReviewMeasurementScreen` / `Content` y componentes**
  - Campos, diálogo de avisos, diálogo de descarte, selector de fecha y hora, y previews.
  - Test de Compose en `androidTest`.
  - Req.: REQ-A002-26…30, 32, 45.
- [x] **T-A002-24 · Room v2**
  - `MeasurementEntity`, DAO reactivo, `Migration1To2` (ADR-A002-11), esquema exportado y
    `MigrationTestHelper`. Eliminar `fallbackToDestructiveMigration`.
  - Hallazgos: AUD-A-33, 34, 35, 36.
- [x] **T-A002-25 · `MeasurementLocalDataSource` + `MeasurementEntityMapper`** (con tests).
- [x] **T-A002-26 · `MeasurementRepositoryImpl`** (parte local: `observe`, `save`, `findAround`,
  `clearLocal`, `observeUnsyncedCount`) y un `NoOpMeasurementSyncScheduler` provisional.
  - Hallazgos: AUD-A-14 (parcial).
- [ ] **🔎 Punto de control 2**: flujo completo escaneo → revisión → guardado local → historial
  (con el historial legacy leyendo de Room v2 a través de un adaptador temporal).

## Fase D — Sincronización con el backend (requiere B-002 desplegado en un entorno accesible)

- [x] **T-A002-27 · `MeasurementApi` v1 + DTO + `MeasurementDtoMapper`**
  - Según `contracts/api-v1.md` §3. Tests de serialización con JSON de ejemplo del contrato.
- [x] **T-A002-28 · `MeasurementRemoteDataSource` v1**
  - `suspendRunCatching` + `HttpErrorMapper`, paginación por cursor.
  - Tests con MockWebServer.
  - Hallazgos: AUD-A-13.
- [x] **T-A002-29 · `MeasurementSynchronizer` + `SyncMeasurementsWorker` + `WorkManagerMeasurementSyncScheduler`**
  - `HiltWorkerFactory` en `Application` (`Configuration.Provider`) y desactivación del
    inicializador por defecto en el manifiesto.
  - Programación al arrancar y tras el login.
  - Tests de la tabla §9.5.
  - Req.: REQ-A002-36…40.
- [x] **T-A002-30 · `refresh()` con fusión + propietario de los datos locales**
  - Hallazgos: AUD-A-14 · Req.: REQ-A002-42, §9.5.
- [x] **T-A002-31 · Integración con el historial**
  - `ObserveMeasurementsUseCase` tipado, indicadores `PENDING` / `SYNC_FAILED`, «Reintentar»
    (`RetryMeasurementSyncUseCase`) y «Eliminar».
  - Retirar `MeasurementModel` legacy, `UploadImageUseCase`, `ImageService` y el flujo de
    subida de la galería en el historial.
  - Req.: REQ-A002-38, 42.
- [x] **T-A002-32 · Borrado *offline-aware* + aviso de logout con pendientes**
  - Req.: REQ-A002-41, 43.

## Fase E — Modelo propio

> **Reparto por etapas** (`implementation-plan.md`): T-A002-33, 34 y 36 pertenecen a la **etapa 1** y
> se completan contra el modelo *dummy* de B-003 (T-B003-10b). T-A002-35 y T-A002-39 pertenecen a la
> **etapa 2** y requieren el modelo entrenado.

- [x] **T-A002-33 · `LiteRtDigitDetector`**
  - Dependencias LiteRT, `ModelAssetLoader` (sha256 y versión de contrato),
    `LetterboxPreprocessor`, delegate GPU con *fallback* a CPU y calentamiento.
  - Modelo en `data/src/main/assets/models/`.
  - Hallazgos: AUD-A-31 · Req.: REQ-A002-13, 24.
- [x] **T-A002-34 · `DetectionDecoder`** (umbral, NMS y deshacer letterbox), con tests de tensores
  sintéticos.
- [ ] **T-A002-35 · (Etapa 2) Cambiar el primario a `litert`**
  - El respaldo pasa a ser `MlKitDigitDetector`.
  - Re-ejecutar la matriz de validación de campo.
- [ ] **T-A002-36 · Benchmark instrumentado** (`LiteRtDigitDetectorBenchmark`)
  - Req.: RNF-A002-03.

## Fase F — Endurecimiento y cierre

- [ ] **T-A002-37 · Pase de accesibilidad**
  - TalkBack en los dos flujos, fuente al 200 %, contraste y objetivos de 48 dp.
  - Req.: REQ-A002-44, 45.
- [ ] **T-A002-38 · Pase de rendimiento**
  - fps efectivos, memoria, tamaño de APK y tiempo de arranque del detector en el dispositivo de
    referencia y en gama baja. Anotarlo en `validation-report.md`.
  - Req.: RNF-A002-01…08.
- [ ] **T-A002-39 · (Etapa 2) Validación de campo** (design §12.3) y `validation-report.md`, con la decisión
  documentada sobre ADR-A002-04.
- [ ] **T-A002-40 · Documentación**
  - README (flujo nuevo, reconocimiento en el dispositivo y privacidad) y capturas.
  - Hallazgos: AUD-A-55.

## Cierre de la spec
- [ ] CA-A002-01…22 verificados (manual o automáticamente) y registrados en el PR.
- [ ] Hallazgos resueltos: AUD-A-04 (cámara), 13, 14, 23…36, 41, 55.
