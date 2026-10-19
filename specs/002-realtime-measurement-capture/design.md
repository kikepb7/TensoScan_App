# A-002 — Captura de medición en tiempo real · Diseño

> Estado: 📝 Pendiente de aprobación · Requisitos: [requirements.md](requirements.md)

## 1. Visión general

```
┌──────────────────────────── :ui ────────────────────────────┐
│ ScanScreen                                                  │
│  ├─ ScanCameraPreview (PreviewView + LifecycleCameraController)
│  │     ImageAnalysis.Analyzer (executor "scan-analysis")    │
│  │        ImageProxy ──► ImageProxyFrameConverter ──► FrameImage (ROI vertical, ≤640 px)
│  │                                         │                │
│  └─ ScanOverlay (Canvas: recuadro, cajas, progreso)         │
└────────────────────────────────────────────┼────────────────┘
                                             ▼  onFrame(frame) [síncrono, hilo de análisis]
┌──────────────────────── :presentation ─────────────────────┐
│ ScanViewModel ── RecognizeFrameUseCase ── ScanSession ──► uiState (StateFlow)
└────────────────────────────────────────────┼────────────────┘
                                             ▼
┌────────────────────────── :domain ─────────────────────────┐
│ RecognizeFrameUseCase                                       │
│   ├─ DigitDetector.detect(frame)      (interfaz)            │
│   ├─ DisplayLayoutParser.parse(...)   (reading-rules §4)    │
│   └─ FrameExposureEvaluator.evaluate(frame)                 │
│ ScanSession ── ReadingStabilizer ── ReadingValidator        │
└────────────────────────────────────────────┼────────────────┘
                                             ▲ implementa
┌─────────────────────────── :data ──────────────────────────┐
│ FallbackDigitDetector(primary, fallback)                    │
│   ├─ LiteRtDigitDetector   (modelo propio, contrato B-003)  │
│   └─ MlKitDigitDetector    (ML Kit Text Recognition v2)     │
└─────────────────────────────────────────────────────────────┘
```

Tras la captura: `ReviewMeasurementScreen` → `SaveMeasurementUseCase` → Room (`PENDING`) →
`MeasurementSyncScheduler` (WorkManager) → `SyncMeasurementsWorker` → `POST /v1/measurements`.

## 2. Decisiones de arquitectura (ADR)

### ADR-A002-01 — Reconocimiento en el dispositivo (y no en el servidor)
- **Contexto**: el reconocimiento en servidor tarda (subida + inferencia), no funciona offline,
  envía imágenes de salud a un tercero y no permite feedback en vivo.
- **Decisión**: toda la inferencia ocurre en el dispositivo y el backend solo recibe valores
  confirmados.
- **Consecuencias**: el modelo viaja en el APK (≤ 6 MB), el rendimiento depende del dispositivo
  (se mitiga con RNF y consenso adaptativo) y el servidor deja de necesitar TensorFlow (B-002).

### ADR-A002-02 — Detectar glifos + parser determinista (y no clasificación fija ni OCR genérico)
- **Alternativas evaluadas**:

  | Opción | Problema | Veredicto |
  |--------|----------|-----------|
  | Recortes en coordenadas fijas + clasificador 28×28 (lo actual) | Solo vale para un modelo y un encuadre perfecto; no sabe de dígitos apagados | ❌ |
  | OCR genérico (ML Kit) como solución final | Los 7 segmentos tienen huecos que el OCR genérico confunde (1/7, 0/8, 5/S) | ⚠️ Solo como arranque o respaldo |
  | Modelo extremo a extremo que devuelva directamente SYS/DIA/PUL | Necesita muchos datos por layout y no se puede explicar ni testear por partes | ❌ |
  | **Detector de glifos + parser de layout con reglas** | Independiente del modelo de tensiómetro, testeable con casos dorados, el modelo es pequeño | ✅ |

- **Consecuencia**: la lógica de «qué número es cada valor» es código Kotlin puro con tests
  (`DisplayLayoutParser`), compartido por contrato con el harness de evaluación del backend.

### ADR-A002-03 — `DigitDetector` como puerto con dos adaptadores y respaldo
- **Contexto**: el modelo propio (B-003) necesita datos reales y entrenamiento, y no debe
  bloquear la UX.
- **Decisión**: interfaz `DigitDetector` en `:domain` con dos implementaciones en `:data`:
  - `MlKitDigitDetector`: ML Kit Text Recognition v2 (empaquetado, offline), cuyos símbolos se
    mapean a `DisplaySymbol` con una tabla de confusiones típicas de 7 segmentos (§9.1);
  - `LiteRtDigitDetector`: modelo propio según `digit-detector-model.md`.

  `FallbackDigitDetector(primary, fallback)` es el que se inyecta. La elección del primario la
  marca `BuildConfig.DIGIT_DETECTOR_PRIMARY` (`mlkit` en las fases A–D y `litert` en la fase E).
- **Consecuencia**: toda la feature (UI, consenso, revisión, guardado y sincronización) se
  construye y valida antes de que exista el modelo propio. Cambiar de detector es una línea de
  configuración. El respaldo cubre REQ-A002-24.

### ADR-A002-04 — La ROI es el recuadro guía; sin detector de display en v1
- **Contexto**: detectar el contorno del display añade un segundo modelo y la homografía.
- **Decisión**: el usuario encuadra el display en un recuadro guía fijo y solo se procesa esa
  región. El aumento de datos de B-003 cubre perspectivas de ±25°.
- **Revisión**: si la validación de campo (§12) muestra más de un 5 % de fallos atribuibles a la
  inclinación, se abre una spec v2 con un detector de 4 esquinas.

### ADR-A002-05 — Consenso temporal con votación por campo
- **Alternativas**:
  - Exigir N frames idénticos consecutivos: un solo frame malo reinicia la cuenta; en la
    práctica no se estabiliza nunca con reflejos.
  - Votar la tripleta completa: un pulso que parpadea impide capturar SYS y DIA, que ya son
    estables.
  - **Votación independiente por campo en una ventana temporal** ✅: es robusta frente a frames
    malos sueltos, permite la captura parcial por timeout (REQ-A002-23) y se adapta a los fps
    del dispositivo.

### ADR-A002-06 — Análisis síncrono en el executor de cámara con copia acotada
- **Decisión**: el analizador convierte el `ImageProxy` en un `FrameImage` (ROI vertical
  reescalada a ≤ 640 px por el lado mayor, ARGB `IntArray` de un **buffer reutilizado**), cierra
  el `ImageProxy` y llama a `viewModel.onFrame(frame)` de forma **síncrona** en el mismo hilo.
- **Motivos**: con `KEEP_ONLY_LATEST`, mientras se procesa un frame CameraX descarta los demás, lo
  que da *backpressure* natural sin colas. La copia de la ROI escalada pesa ~1,6 MB como máximo y
  se reutiliza (sin presión sobre el GC). El dominio recibe un tipo JVM puro.
- **Restricción documentada**: `FrameImage` **solo es válido durante la llamada** a `onFrame`.
  Ningún componente puede retenerlo.

### ADR-A002-07 — Un solo pipeline para cámara y, en el futuro, galería
- `RecognizeFrameUseCase` es agnóstico de la fuente. Una futura importación desde galería
  convertiría el bitmap en `FrameImage` y llamaría al mismo caso de uso una vez, sin consenso.

### ADR-A002-08 — Offline-first con WorkManager e idempotencia por `client_id`
- **Contexto**: hoy, si falla la red, se pierde la medición. Un reintento ingenuo crea duplicados.
- **Decisión**: Room es la fuente de verdad local. `client_id` (UUID v4) se genera en la app.
  `POST /v1/measurements` es idempotente por `(user, client_id)`. WorkManager garantiza la
  ejecución aunque se cierre la app.
- **Alternativa descartada**: sincronizar en el `viewModelScope` al guardar. Se pierde si el
  usuario sale de la pantalla o mata la app.

### ADR-A002-09 — El borrador viaja en la ruta tipada (y no en un ViewModel compartido)
- **Decisión**: `ReviewMeasurementRoute` (`@Serializable`) lleva los valores propuestos, sus
  confianzas, la versión del detector y la fecha de captura.
- **Motivos**: sobrevive a la muerte del proceso sin código extra (lo restaura el back stack) y
  desacopla las pantallas. El tamaño (unos 10 primitivos) es despreciable.
- **Alternativas descartadas**: un ViewModel con *scope* de grafo (acoplamiento y pérdida al
  morir el proceso) y un repositorio de borradores en memoria (ídem).

### ADR-A002-10 — Una sola pantalla de revisión para captura y entrada manual
- El modo manual es la misma pantalla con los campos vacíos y `source = MANUAL`. Así hay un solo
  punto de validación y menos UI que mantener.

### ADR-A002-11 — Migración Room v1→v2 explícita, que descarta la caché legacy
- **Contexto**: la tabla v1 `measurement_table` solo contiene la **caché** de lo que hay en el
  servidor (no se puede crear nada en local en v1).
- **Decisión**: una `Migration(1, 2)` explícita borra `measurement_table`, crea `measurements` y,
  al arrancar, se lanza un refresh desde el servidor. No se pierde nada porque el servidor tiene
  los datos (B-002 migra sus documentos legacy).
- **A partir de v2**: las filas `PENDING` existen solo en local, así que **toda migración futura es
  explícita y testeada** (`MigrationTestHelper`) y queda prohibido
  `fallbackToDestructiveMigration`.

## 3. Estructura de paquetes

```
domain/src/main/kotlin/com/tensoscan/domain/
├── measurement/
│   ├── model/        BloodPressureReading, Measurement, MeasurementId, MeasurementSource,
│   │                 SyncStatus, RecognitionMetadata, ReadingField, MeasurementDraft
│   ├── validation/   ReadingLimits, ReadingValidator, ReadingValidationResult,
│   │                 ReadingErrorCode, ReadingWarning
│   ├── repository/   MeasurementRepository, MeasurementSyncScheduler
│   └── usecase/      SaveMeasurementUseCase, ValidateReadingUseCase,
│                     CheckDuplicateMeasurementUseCase, ObserveMeasurementsUseCase,
│                     RefreshMeasurementsUseCase, DeleteMeasurementUseCase,
│                     RetryMeasurementSyncUseCase, ObserveUnsyncedCountUseCase
├── error/            DomainError, FieldError, DetectorError (subjerarquías sealed en el mismo paquete)
├── scan/
│   ├── model/        FrameImage, NormalizedBox, DisplaySymbol, SymbolDetection, FrameReading,
│   │                 FrameReadingStatus, ExposureAssessment, FrameRecognition, DetectorInfo,
│   │                 CapturedReading, ScanHint, ScanSessionState
│   ├── detector/     DigitDetector
│   ├── parser/       NumberGrouper, DisplayLayoutParser, LayoutRules (constantes §4.2)
│   ├── exposure/     FrameExposureEvaluator
│   ├── stabilizer/   ReadingStabilizer, StabilizerConfig
│   ├── session/      ScanSession, ScanSessionConfig
│   └── usecase/      RecognizeFrameUseCase, PrepareDetectorUseCase
└── time/             Clock (interfaz; el ScanSession usa timestamps de frame, no el reloj)

data/src/main/kotlin/com/tensoscan/data/
├── scan/
│   ├── mlkit/        MlKitDigitDetector, MlKitSymbolMapper
│   ├── litert/       LiteRtDigitDetector, ModelAssetLoader, ModelSidecar (DTO), LetterboxPreprocessor,
│   │                 DetectionDecoder (decodificado + NMS + deshacer letterbox)
│   └── FallbackDigitDetector
├── measurement/
│   ├── local/        MeasurementDatabase (v2), MeasurementDao, MeasurementEntity, Migration1To2,
│   │                 MeasurementLocalDataSource
│   ├── remote/       MeasurementApi, dto/*, MeasurementRemoteDataSource
│   ├── mapper/       MeasurementEntityMapper, MeasurementDtoMapper
│   ├── sync/         MeasurementSynchronizer, SyncMeasurementsWorker,
│   │                 WorkManagerMeasurementSyncScheduler
│   └── MeasurementRepositoryImpl

presentation/src/main/kotlin/com/tensoscan/presentation/
├── navigation/       ScanRoute, ReviewMeasurementRoute
├── scan/             ScanViewModel, ScanScreenUiState, ScanUiState, ScanUiAction, ScanUiEvent,
│                     mapper/ScanUiStateMapper, model/*
└── review/           ReviewMeasurementViewModel, ReviewMeasurementScreenUiState,
                      ReviewMeasurementUiState, ReviewMeasurementUiAction,
                      ReviewMeasurementUiEvent, parser/ReadingInputParser,
                      mapper/ReviewMeasurementUiStateMapper

ui/src/main/kotlin/com/tensoscan/ui/
├── scan/             ScanScreen, ScanContent, camera/ScanCameraPreview,
│                     camera/ImageProxyFrameConverter, camera/RoiGeometry,
│                     components/ScanOverlay, components/LiveReadingPanel,
│                     components/ScanGuidanceBanner, components/PermissionRequiredContent,
│                     components/ScanUnavailableContent
└── review/           ReviewMeasurementScreen, ReviewMeasurementContent,
                      components/ReadingField, components/ReadingWarningsDialog,
                      components/MeasuredAtPicker
```

## 4. Modelo de dominio

```kotlin
// ---------- measurement ----------
@JvmInline value class MeasurementId(val value: String)          // UUID v4 (= client_id)
data class BloodPressureReading(val systolic: Int, val diastolic: Int, val pulse: Int)
enum class ReadingField { SYSTOLIC, DIASTOLIC, PULSE }
enum class MeasurementSource { CAMERA, MANUAL }
sealed interface SyncStatus {
    data object Pending : SyncStatus
    data object Synced : SyncStatus
    data class Failed(val reason: String) : SyncStatus      // código del servidor
}
data class RecognitionMetadata(
    val confidence: Float,                 // media de los campos propuestos
    val detectorVersion: String,           // "mlkit-text-v2" | "digits-1.0.0"
    val editedFields: Set<ReadingField>,
)
data class Measurement(
    val id: MeasurementId,
    val remoteId: String?,
    val reading: BloodPressureReading,
    val measuredAt: Instant,
    val createdAt: Instant,
    val source: MeasurementSource,
    val recognition: RecognitionMetadata?,  // != null ⇔ source == CAMERA
    val syncStatus: SyncStatus,
)
/** Entrada sin validar desde la revisión. */
data class MeasurementDraft(
    val systolic: Int?, val diastolic: Int?, val pulse: Int?,
    val measuredAt: Instant,
    val source: MeasurementSource,
    val proposal: CapturedReading?,        // lo que propuso el escaneo (para calcular editedFields)
)

// ---------- validation (reading-rules §2-3) ----------
enum class ReadingErrorCode { REQUIRED, OUT_OF_RANGE, SYSTOLIC_NOT_GREATER_THAN_DIASTOLIC, IN_FUTURE, TOO_OLD }
enum class ReadingWarning { VERY_HIGH_PRESSURE, LOW_PRESSURE, UNUSUAL_PULSE, UNUSUAL_PULSE_PRESSURE, POSSIBLE_DUPLICATE }
data class ReadingFieldError(val field: ReadingField?, val code: ReadingErrorCode) // field null ⇒ measuredAt
data class ReadingValidationResult(val errors: List<ReadingFieldError>, val warnings: List<ReadingWarning>)

// ---------- scan ----------
/** ROI vertical, ARGB_8888 empaquetado. Solo es válido durante onFrame (ADR-A002-06). */
class FrameImage(val width: Int, val height: Int, val argb: IntArray, val timestampMillis: Long)
data class NormalizedBox(val left: Float, val top: Float, val right: Float, val bottom: Float)
enum class DisplaySymbol { DIGIT_0, DIGIT_1, /*…*/ DIGIT_9, COLON, DASH, LETTER_E, LETTER_R }
data class SymbolDetection(val symbol: DisplaySymbol, val box: NormalizedBox, val confidence: Float)
enum class FrameReadingStatus { COMPLETE, PARTIAL, NO_READING, MEASURING, DEVICE_ERROR }
data class FieldValue(val value: Int, val confidence: Float, val box: NormalizedBox)
data class FrameReading(
    val status: FrameReadingStatus,
    val systolic: FieldValue?, val diastolic: FieldValue?, val pulse: FieldValue?,
    val primaryHeight: Float?,              // altura relativa del número más alto (pista MOVE_CLOSER)
)
data class ExposureAssessment(val meanLuminance: Float, val saturatedRatio: Float)
data class FrameRecognition(
    val timestampMillis: Long,
    val detections: List<SymbolDetection>,
    val reading: FrameReading,
    val exposure: ExposureAssessment,
)
data class DetectorInfo(val id: String, val version: String)   // "litert" / "1.0.0"
data class CapturedReading(
    val systolic: Int?, val diastolic: Int?, val pulse: Int?,
    val confidences: Map<ReadingField, Float>,
    val detector: DetectorInfo,
    val capturedAtMillis: Long,
)
enum class ScanHint { POINT_AT_DISPLAY, MOVE_CLOSER, LOW_LIGHT, GLARE, DEVICE_SHOWS_ERROR, DEVICE_MEASURING }
sealed interface ScanSessionState {
    data class Scanning(
        val provisional: Map<ReadingField, Int>,   // moda actual por campo (para la UI)
        val progress: Float,                       // 0..1
        val hint: ScanHint?,
        val isTimedOut: Boolean,
        val stableFields: Map<ReadingField, Int>,  // campos ya estables (para «Usar lo leído»)
    ) : ScanSessionState
    data class Captured(val reading: CapturedReading) : ScanSessionState
}
```

```kotlin
interface DigitDetector {
    /** Carga el modelo. Idempotente. Se llama fuera del hilo principal. */
    suspend fun prepare(): Result<DetectorInfo>
    /** Bloqueante. SOLO en el hilo de análisis. Requiere prepare() con éxito. */
    fun detect(frame: FrameImage): Result<List<SymbolDetection>>
    fun release()
}
sealed class DetectorError : DomainError() {
    data object ModelMissing : DetectorError()
    data object ModelIncompatible : DetectorError()   // contract_version o sha256
    data class RuntimeFailure(override val cause: Throwable?) : DetectorError()
}
```
*(Kotlin exige que las subclases directas de una clase `sealed` estén en el **mismo paquete y
módulo**. Por eso `DetectorError` se declara en `…domain.error` (fichero `DetectorError.kt`)
junto a `DomainError`, y no en `scan/detector/`, donde solo vive la interfaz `DigitDetector`.)*

## 5. Pipeline por frame

```
[analysis thread]
ImageProxy (RGBA_8888, rotationDegrees)
  └─ ImageProxyFrameConverter.convert(proxy, roiGeometry) → FrameImage
       1. toBitmap() (reusa el bitmap de destino cuando la API lo permite)
       2. Matrix: recorte ROI (coordenadas del sensor) + rotación a vertical + escala a ≤640 px
          en una sola Bitmap.createBitmap(src, x, y, w, h, matrix, filter=true)
       3. bitmap.getPixels(reusableIntArray) → FrameImage(w, h, argb, timestamp)
  └─ proxy.close()
  └─ ScanViewModel.onFrame(frame)
       ├─ drain(commandQueue)  → reset/pausa pedidos desde el hilo principal
       ├─ RecognizeFrameUseCase.recognize(frame): FrameRecognition
       │    ├─ detector.detect(frame)  (Result; si falla → reading NO_READING, contador de fallos)
       │    ├─ DisplayLayoutParser.parse(detections)
       │    └─ FrameExposureEvaluator.evaluate(frame)  (muestreo 1 de cada 8 px)
       ├─ ScanSession.onFrame(recognition): ScanSessionState
       └─ _sessionState.value = state  (MutableStateFlow; seguro entre hilos)
[main]
uiState = combine(permission, cameraStatus, detectorStatus, sessionState, torch, overlay) → ScanScreenUiState
```

## 6. Algoritmos

### 6.1 Geometría del recuadro guía (`RoiGeometry`, en `:ui`, puro y testeable)
- La ROI se define en **coordenadas normalizadas de la imagen vertical** de análisis: centrada
  en horizontal, con su centro vertical en el 45 % de la altura (algo por encima del centro, para
  dejar sitio al panel inferior), ancho = 0,80·W y alto = min(1,25·ancho, 0,60·H) en píxeles.
- Preview e ImageAnalysis se configuran con la **misma** estrategia de relación de aspecto
  (16:9 con *fallback* automático) y `PreviewView.ScaleType.FILL_CENTER`.
- Imagen → vista: `scale = max(viewW/imgW, viewH/imgH)`, `offset = ((viewW − imgW·scale)/2,
  (viewH − imgH·scale)/2)`. El overlay pinta el recuadro y las cajas aplicando esta
  transformación, así que lo que ve el usuario coincide **exactamente** con lo que se analiza.
- Vertical → sensor (para recortar antes de rotar): se rota el rectángulo con
  `-rotationDegrees` alrededor del centro de la imagen.
- Tests: rotaciones 0/90/180/270 y relaciones de aspecto de vista 9:16, 9:19,5 y 9:20.

### 6.2 Preprocesado LiteRT (`LetterboxPreprocessor`)
Según `digit-detector-model.md` §3: escalar `FrameImage` con bilineal hasta que el lado mayor mida
320, centrar, rellenar con 114/255 y escribir RGB float32 en un `ByteBuffer` directo reutilizado
(`1·320·320·3·4` bytes). Se guardan `scale` y `pad` para deshacer el letterbox en el
`DetectionDecoder`.

### 6.3 Parser
Implementación literal de `reading-rules.md` §4.3 en `NumberGrouper` (pasos 1–3) y
`DisplayLayoutParser` (pasos 4–6). Las constantes viven en `LayoutRules`. **Ninguna otra clase
puede reinterpretar el layout.**

### 6.4 `ReadingStabilizer` (votación por campo)

| Parámetro (`StabilizerConfig`) | Valor | Motivo |
|--------------------------------|-------|--------|
| `windowMillis` | 1500 | Cubre 15 frames a 10 fps o 6 a 4 fps |
| `minVotes(framesInWindow)` | `clamp(round(0,5·framesInWindow), 3, 6)` | Se adapta a los fps (RNF-A002-02) |
| `minAgreement` | 0,70 | Proporción de la moda sobre los frames en los que el campo aparece |
| `minMeanConfidence` | 0,55 | Confianza media de los votos de la moda |
| `resetAfterEmptyMillis` | 1000 | Si no hay números durante 1 s, se vacía la ventana (el usuario se ha movido) |

Por cada `FrameRecognition`:
1. Se descarta el frame si `status ∈ {DEVICE_ERROR, MEASURING}`; cuenta como «vacío» a efectos
   de reinicio.
2. Se filtran los campos individualmente **inválidos** (`OUT_OF_RANGE`) y, si `SYS ≤ DIA`, se
   descartan ambos de ese frame.
3. Se añaden los votos `(timestamp, campo, valor, confianza)` y se expulsan los anteriores a
   `t − windowMillis`.
4. Para cada campo: `moda`, `votosModa`, `presentes`. Es estable si `votosModa ≥ minVotes`,
   `votosModa/presentes ≥ minAgreement` y la confianza media es suficiente.
5. `progress = mínimo, sobre los tres campos, de min(1, votosModa/minVotes)`.
6. `Captured` cuando los tres campos son estables **y** la tripleta pasa
   `ReadingValidator.errors` (sin contar `REQUIRED`).

### 6.5 `ScanSession` (estado y temporizadores)
- El tiempo es el `timestampMillis` del frame. No se usa el reloj de pared, así que los tests son
  deterministas.
- `startedAt` = primer frame. `lastNumbersAt` = último frame con al menos un número.
- Prioridad de pistas: `DEVICE_SHOWS_ERROR` (≥ 3 frames consecutivos con `DEVICE_ERROR`) >
  `DEVICE_MEASURING` > `LOW_LIGHT` > `GLARE` > `MOVE_CLOSER` (`primaryHeight < 0,15` durante
  1 s) > `POINT_AT_DISPLAY` (sin números durante ≥ 6 s, REQ-A002-22).
- `isTimedOut` = `t − startedAt ≥ 20 s` sin captura (REQ-A002-23). No detiene el análisis.
- Comandos (desde el hilo principal, vía cola): `Reset` (reintentar o repetir), `Pause` (segundo
  plano: guarda `pausedAt`) y `Resume` (si `pausa > 5 s` → `Reset`, REQ-A002-09).
- Tras `Captured`, la sesión queda congelada (ignora frames) hasta que llegue un `Reset`.

### 6.6 `FrameExposureEvaluator`
Muestreo de 1 de cada 8 píxeles en cada eje. `Y = 0,2126R + 0,7152G + 0,0722B` normalizada.
`LOW_LIGHT` si `mean(Y) < 0,15` y `GLARE` si `ratio(Y > 0,98) > 0,06`, ambas mantenidas durante
≥ 1 s (con histéresis para evitar parpadeo). Los umbrales se ajustan en la validación de campo y
se registran en el informe.

## 7. Presentación

### 7.1 Escaneo

```kotlin
@Immutable internal sealed interface ScanScreenUiState {
    data object CheckingPermission : ScanScreenUiState
    data class PermissionRequired(val isPermanentlyDenied: Boolean) : ScanScreenUiState
    data object Preparing : ScanScreenUiState                      // cargando el detector
    data class Scanning(val content: ScanUiState) : ScanScreenUiState
    data class Unavailable(val reason: ScanUnavailableReason) : ScanScreenUiState
}
internal enum class ScanUnavailableReason { CAMERA_ERROR, RECOGNITION_UNAVAILABLE }

@Immutable internal data class ScanUiState(
    val boxes: List<DetectionBoxUiState>,         // coordenadas ROI normalizadas + rol (SYS/DIA/PUL/otro)
    val systolic: LiveValueUiState, val diastolic: LiveValueUiState, val pulse: LiveValueUiState,
    val progress: Float,
    val guidance: ScanGuidance,                   // enum → string resource en :ui
    val torch: TorchUiState,                      // UNAVAILABLE | OFF | ON
    val timeoutActions: TimeoutActionsUiState?,   // null hasta los 20 s
    val isPaused: Boolean,                        // RNF-A002-08 (60 s sin interacción)
    val accessibilityAnnouncement: ScanAnnouncement?, // limitado a 1 cada 2 s (REQ-A002-44)
)

internal sealed interface ScanUiAction {
    data class PermissionResult(val granted: Boolean, val shouldShowRationale: Boolean) : ScanUiAction
    data object GrantPermissionClicked : ScanUiAction
    data object OpenSettingsClicked : ScanUiAction
    data object ScreenResumed : ScanUiAction
    data object ScreenPaused : ScanUiAction
    data object CameraStarted : ScanUiAction
    data class CameraFailed(val cause: CameraFailureCause) : ScanUiAction
    data class TorchAvailabilityChanged(val available: Boolean) : ScanUiAction
    data object ToggleTorchClicked : ScanUiAction
    data object RetryClicked : ScanUiAction
    data object UsePartialReadingClicked : ScanUiAction
    data object EnterManuallyClicked : ScanUiAction
    data object ContinueScanningClicked : ScanUiAction   // salir de la pausa
    data object CloseClicked : ScanUiAction
}
internal sealed interface ScanUiEvent {
    data object RequestCameraPermission : ScanUiEvent
    data object OpenAppSettings : ScanUiEvent
    data class NavigateToReview(val route: ReviewMeasurementRoute) : ScanUiEvent
    data object PerformCaptureHaptic : ScanUiEvent
    data object NavigateBack : ScanUiEvent
}
```

`ScanViewModel` (`@HiltViewModel internal`):
- Dependencias: `PrepareDetectorUseCase`, `RecognizeFrameUseCase`, `ScanSession` (instancia
  nueva por ViewModel), `ScanUiStateMapper` y `AppDispatchers` (`default`) para `prepare`.
- `init`: no hace nada pesado. `prepare()` se lanza al recibir `PermissionResult(granted=true)`, en
  paralelo con el arranque de la cámara (RNF-A002-07).
- `onFrame(frame)`: es público, **no** es una acción de usuario (C-PAT-02 aplica a acciones de
  usuario). Se ignora si el estado no es `Scanning` o si está en pausa.
- Al pasar a `Captured`: emite `PerformCaptureHaptic` y luego `NavigateToReview`, y deja de
  procesar frames.
- La inactividad (60 s sin un `onAction`) pone `isPaused = true` y la UI desvincula el análisis.
- `ScanUiStateMapper` traduce `ScanSessionState` y las detecciones a `ScanUiState` (roles de las
  cajas, `LiveValueUiState` con valor o «—», guía). Nada de texto: solo enums que `:ui` resuelve a
  `stringResource`.

### 7.2 Revisión

```kotlin
@Serializable internal data class ReviewMeasurementRoute(
    val systolic: Int? = null, val diastolic: Int? = null, val pulse: Int? = null,
    val systolicConfidence: Float? = null, val diastolicConfidence: Float? = null, val pulseConfidence: Float? = null,
    val detectorId: String? = null, val detectorVersion: String? = null,
    val capturedAtEpochMillis: Long? = null,
    val source: MeasurementSourceArg,              // CAMERA | MANUAL
)

@Immutable internal sealed interface ReviewMeasurementScreenUiState {
    data class Editing(val content: ReviewMeasurementUiState) : ReviewMeasurementScreenUiState
    data class Saving(val content: ReviewMeasurementUiState) : ReviewMeasurementScreenUiState
}
@Immutable internal data class ReviewMeasurementUiState(
    val systolic: ReadingFieldUiState, val diastolic: ReadingFieldUiState, val pulse: ReadingFieldUiState,
    val measuredAt: MeasuredAtUiState,              // texto formateado + epoch
    val measuredAtError: MeasuredAtErrorUi?,
    val isConfirmEnabled: Boolean,
    val warningsDialog: WarningsDialogUiState?,     // != null ⇒ el diálogo es visible
    val isDiscardDialogVisible: Boolean,
    val source: MeasurementSourceUi,
)
@Immutable internal data class ReadingFieldUiState(
    val rawValue: String, val error: ReadingFieldErrorUi?, val needsCheck: Boolean,
)
internal sealed interface ReviewMeasurementUiAction {
    data class SystolicChanged(val value: String) : ReviewMeasurementUiAction
    data class DiastolicChanged(val value: String) : ReviewMeasurementUiAction
    data class PulseChanged(val value: String) : ReviewMeasurementUiAction
    data class MeasuredAtChanged(val epochMillis: Long) : ReviewMeasurementUiAction
    data object ConfirmClicked : ReviewMeasurementUiAction
    data object SaveAnywayClicked : ReviewMeasurementUiAction
    data object ReviewWarningsClicked : ReviewMeasurementUiAction
    data object RetakeClicked : ReviewMeasurementUiAction
    data object DiscardClicked : ReviewMeasurementUiAction
    data object DiscardConfirmed : ReviewMeasurementUiAction
    data object DiscardCancelled : ReviewMeasurementUiAction
}
internal sealed interface ReviewMeasurementUiEvent {
    data object NavigateToHistoryAfterSave : ReviewMeasurementUiEvent  // la UI muestra el snackbar «Medición guardada»
    data object NavigateToHistoryAfterDiscard : ReviewMeasurementUiEvent
    data object NavigateBackToScan : ReviewMeasurementUiEvent
    data object ShowSaveError : ReviewMeasurementUiEvent               // snackbar con «Reintentar» → ConfirmClicked
}
```

`ReviewMeasurementViewModel`:
- El estado interno (texto bruto y `measuredAt`) se guarda en `SavedStateHandle`
  (`getStateFlow`), lo que da REQ-A002-33.
- `ReadingInputParser.parse(raw): ParsedInput` (filtra los caracteres que no son dígitos y
  limita a 3) devuelve `Int?`.
- Derivación: `combine(raw...) → ValidateReadingUseCase.validate(draft) →
  ReviewMeasurementUiStateMapper.map(...)`, expuesta con `stateIn(WhileSubscribed(5_000))`.
- `ConfirmClicked`:
  1. recalcula los errores (deben estar vacíos);
  2. `CheckDuplicateMeasurementUseCase` añade `POSSIBLE_DUPLICATE`;
  3. si hay avisos → `warningsDialog`; si no →
  4. `save()` → `Saving` → `SaveMeasurementUseCase.save(draft)`:
     - éxito → `NavigateToHistoryAfterSave`;
     - `DomainError.Storage` → `Editing` + `ShowSaveError`;
     - `DomainError.Validation` (no debería ocurrir, pero el dominio revalida) → `Editing`.
- `editedFields` lo calcula el **dominio** (`SaveMeasurementUseCase`) comparando el borrador con
  `proposal`. No lo calcula el ViewModel.
- `needsCheck` = el campo vino vacío en la propuesta, o su confianza es < 0,70 y el usuario aún no
  lo ha editado.

## 8. UI

### 8.1 Escaneo (vertical)

```
┌──────────────────────────────┐
│ ✕                       🔦   │  ← cerrar · linterna (si la hay)
│ ┌──────────────────────────┐ │
│ │░░░░░░ vista previa ░░░░░░│ │  oscurecido fuera de la ROI
│ │░░ ┏━━━━━━━━━━━━━━━━━┓ ░░░│ │
│ │░░ ┃ [1][2][8]  ←cajas┃ ░░░│ │  recuadro guía (borde con progreso)
│ │░░ ┃    [8][2]       ┃ ░░░│ │
│ │░░ ┃        [6][7]   ┃ ░░░│ │
│ │░░ ┗━━━━━━━━━━━━━━━━━┛ ░░░│ │
│ └──────────────────────────┘ │
│  «Mantén el móvil quieto»    │  ← ScanGuidanceBanner (LiveRegion)
│  SYS 128   DIA 82   PUL —    │  ← LiveReadingPanel
│  [ Introducir a mano ]       │  (siempre visible, secundario)
│  [ Usar lo leído ] (≥ 20 s)  │
└──────────────────────────────┘
```
- El borde del recuadro funciona como anillo de progreso (gris → color primario según `progress`).
- Cajas: SYS, DIA y PUL con colores distintos del tema y una etiqueta pequeña; el resto en gris.
- Al capturar: destello en el borde + haptic `CONFIRM` y la navegación.
- `ScanCameraPreview` es **el único** composable que conoce CameraX: crea
  `LifecycleCameraController(context)`, `setEnabledUseCases(IMAGE_ANALYSIS)`, el analizador con
  un `Executors.newSingleThreadExecutor` (nombre `scan-analysis`, cerrado en `onDispose`),
  `enableTorch(state)`, `TorchState` observado → `TorchAvailabilityChanged` y tap-to-focus
  limitado a la ROI. Los errores de `bindToLifecycle` y de `CameraState` → `CameraFailed`.
- Bloqueo de orientación: `LockScreenOrientation(PORTRAIT)` como `DisposableEffect` en
  `ScanScreen` (REQ-A002-11).

### 8.2 Revisión

```
┌──────────────────────────────┐
│ ←  Revisa la medición        │
│  Sistólica   [ 128 ] mmHg    │
│  Diastólica  [  82 ] mmHg    │
│  Pulso       [  6_ ] ppm  ⚠ «Comprueba este valor»
│  Fecha  19/10/2026 08:15 ✎   │
│                              │
│ [ Repetir ]  [ Confirmar ]   │
│        Descartar             │
└──────────────────────────────┘
```
- `OutlinedTextField` con `KeyboardType.NumberPassword` (solo dígitos y sin sugerencias), una
  `supportingText` para el error o la pista, e `imeAction` Next/Done.
- `ReadingWarningsDialog`: lista de avisos. `VERY_HIGH_PRESSURE` va primero y con estilo de
  advertencia. Botones «Revisar» y «Guardar igualmente».
- Fecha: `DatePicker` + `TimePicker` de Material 3 limitados a «ahora + 5 min».

## 9. Datos

### 9.1 `MlKitDigitDetector`
- Dependencia `com.google.mlkit:text-recognition` (modelo latino **empaquetado**, funciona
  offline desde la instalación).
- `detect`: `FrameImage → Bitmap` (buffer reutilizado) → `InputImage.fromBitmap(bmp, 0)` →
  `Tasks.await(recognizer.process(image), 500 ms)` (está permitido fuera del hilo principal) →
  `MlKitSymbolMapper`.
- `MlKitSymbolMapper` (puro, recibe `List<OcrSymbol(text, box, confidence?)>`):

  | Texto ML Kit | `DisplaySymbol` |
  |--------------|-----------------|
  | `0`–`9` | dígito |
  | `O`, `o`, `D`, `Q` | `DIGIT_0` |
  | `l`, `I`, `\|`, `i`, `!` | `DIGIT_1` |
  | `Z`, `z` | `DIGIT_2` |
  | `S`, `s` | `DIGIT_5` |
  | `b`, `G` | `DIGIT_6` |
  | `B` | `DIGIT_8` |
  | `g`, `q` | `DIGIT_9` |
  | `:` | `COLON` |
  | `-`, `–`, `_` | `DASH` |
  | `E` | `LETTER_E` |
  | `r` | `LETTER_R` |
  | otros | se descarta |

  La confianza es la `confidence` del símbolo si ML Kit la da, y 0,80 en caso contrario. Las cajas
  se normalizan por el tamaño del frame.
- `DetectorInfo("mlkit-text", "v2")` → `model_version = "mlkit-text-v2"`.
- **A verificar en T-A002-12**: que la versión estable de `text-recognition` expone `Text.Symbol` con
  caja (y `confidence`). Si no, se usa el nivel `Element` y se reparte la caja por caracteres
  proporcionalmente a su anchura. La decisión se anota aquí.

### 9.2 `LiteRtDigitDetector`
- Dependencias: LiteRT (`com.google.ai.edge.litert`) y su delegate GPU, en sus versiones estables.
- `prepare()` (en `AppDispatchers.default`):
  1. `ModelAssetLoader` lee `assets/models/digits-<v>.tflite` y su `.json`;
  2. verifica `sha256` y que `contract_version` tenga el mismo *major*;
  3. crea el `Interpreter`: GPU delegate si está soportado y, si falla al inicializar, CPU
     XNNPACK con 4 hilos;
  4. hace una inferencia de calentamiento.

  El resultado es `DetectorError.ModelMissing`, `ModelIncompatible` o `RuntimeFailure`.
- `detect`: `LetterboxPreprocessor` → `interpreter.run` → `DetectionDecoder` (umbral, NMS
  agnóstico, máximo 32 detecciones, deshacer el letterbox). El intérprete queda **confinado** al
  hilo de análisis.
- `DetectionDecoder` es Kotlin puro, así que se testea con tensores sintéticos.

### 9.3 `FallbackDigitDetector`
- `prepare()`: prepara el primario y, si falla, registra la causa y prepara el respaldo. Si
  también falla → `DetectorError` (y la UI pasa a `RECOGNITION_UNAVAILABLE`).
- `detect()`: delega en el activo. Con 5 fallos consecutivos del primario en tiempo de ejecución,
  cambia al respaldo para el resto de la sesión.
- `DetectorModule` (`app/di`): `@Provides @Primary`/`@Fallback` según
  `BuildConfig.DIGIT_DETECTOR_PRIMARY`, y `@Binds DigitDetector ← FallbackDigitDetector`, con
  *scope* `ViewModelScoped`. Se libera en `onCleared` mediante `PrepareDetectorUseCase.release()`.

### 9.4 Room v2

```kotlin
@Entity(tableName = "measurements", indices = [Index("measured_at"), Index(value = ["remote_id"], unique = true)])
internal data class MeasurementEntity(
    @PrimaryKey @ColumnInfo("client_id") val clientId: String,
    @ColumnInfo("remote_id") val remoteId: String?,
    val systolic: Int, val diastolic: Int, val pulse: Int,
    @ColumnInfo("measured_at") val measuredAtEpochMillis: Long,
    @ColumnInfo("created_at") val createdAtEpochMillis: Long,
    val source: String,                          // CAMERA | MANUAL
    val confidence: Float?, @ColumnInfo("detector_version") val detectorVersion: String?,
    @ColumnInfo("edited_fields") val editedFields: String,   // "SYSTOLIC,PULSE" (TypeConverter)
    @ColumnInfo("sync_status") val syncStatus: String,       // PENDING | SYNCED | SYNC_FAILED
    @ColumnInfo("sync_error") val syncError: String?,
)
```
DAO:
- `observeAll(): Flow<List<MeasurementEntity>>` (`ORDER BY measured_at DESC`);
- `findBetween(from, to)`;
- `findPending()`;
- `upsert`;
- `updateSyncStatus(clientId, status, remoteId, error)`;
- `deleteByClientId`;
- `deleteSyncedNotIn(remoteIds)`;
- `@Transaction mergeRemote(items)`;
- `deleteAll`.

`Migration1To2` según ADR-A002-11, más `MigrationTestHelper` en `androidTest`.

### 9.5 Repositorio y sincronización

```kotlin
interface MeasurementRepository {                       // :domain
    fun observeMeasurements(): Flow<List<Measurement>>
    fun observeUnsyncedCount(): Flow<Int>
    suspend fun findAround(instant: Instant, window: Duration): Result<List<Measurement>>
    suspend fun save(measurement: Measurement): Result<Unit>
    suspend fun refresh(): Result<Unit>
    suspend fun delete(id: MeasurementId): Result<Unit>
    suspend fun markForRetry(id: MeasurementId): Result<Unit>
    suspend fun clearLocal(): Result<Unit>
}
interface MeasurementSyncScheduler { fun schedule() }   // :domain
```

- `SaveMeasurementUseCase.save(draft)`:
  1. valida con `ReadingValidator` (errores → `DomainError.Validation`);
  2. calcula `editedFields`;
  3. construye `Measurement(id = UUID.randomUUID(), syncStatus = Pending, ...)`;
  4. `repository.save`;
  5. `scheduler.schedule()`.
- `WorkManagerMeasurementSyncScheduler.schedule()`: `enqueueUniqueWork("measurement-sync",
  ExistingWorkPolicy.KEEP, OneTimeWorkRequest<SyncMeasurementsWorker>)` con
  `NetworkType.CONNECTED` y backoff exponencial de 30 s. También se llama al arrancar la app si
  `unsyncedCount > 0` y tras cada login.
- `SyncMeasurementsWorker` (`@HiltWorker`) → `MeasurementSynchronizer.syncPending()`:

  | Respuesta | Acción local | Resultado del worker |
  |-----------|--------------|----------------------|
  | 201 / 200 | `SYNCED` + `remote_id` | continúa con la siguiente |
  | 422 `MEASUREMENT_INVALID` | `SYNC_FAILED("MEASUREMENT_INVALID")` | continúa |
  | 409 `CLIENT_ID_CONFLICT` | `SYNC_FAILED("CLIENT_ID_CONFLICT")` | continúa |
  | `Network` / 5xx | ninguna | `Result.retry()` (para el lote) |
  | `SessionExpired` / `Unauthorized` | ninguna | `Result.failure()`; se reprograma en el próximo login |

- `refresh()`: pagina `GET /v1/measurements` hasta `next_cursor == null` (máximo 20 páginas de
  200) y llama a `mergeRemote` en una transacción:
  - por cada elemento remoto se hace upsert por `client_id` con `SYNCED`;
  - las filas locales `SYNCED` cuyo `remote_id` no aparezca se borran (se eliminaron en otro
    dispositivo);
  - las filas `PENDING` y `SYNC_FAILED` **no se tocan**.
- **Propietario de los datos locales**: se guarda `measurements_owner_user_id` en DataStore. Si al
  iniciar sesión el usuario es distinto, se llama a `clearLocal()` antes del refresh. Esto evita
  que los datos se mezclen entre cuentas.
- `delete(id)`: si `remoteId == null` → borrado local; si no → `DELETE /v1/measurements/{remoteId}`
  y después el borrado local (un 404 se trata como éxito). Si falla → `Result.failure`.

## 10. Catálogo de errores y casos límite

| ID | Situación | Detección | Comportamiento / UI | Recuperación | Req. |
|----|-----------|-----------|---------------------|--------------|------|
| ERR-SCAN-01 | Permiso denegado | `PermissionResult(false, rationale=true)` | `PermissionRequired(false)`: motivo + «Conceder permiso» / «Introducir a mano» | Volver a pedir | 03 |
| ERR-SCAN-02 | Permiso denegado de forma permanente | `granted=false && !rationale` tras haberlo pedido | `PermissionRequired(true)`: «Abrir ajustes» | `ScreenResumed` vuelve a comprobar | 04 |
| ERR-SCAN-03 | Sin cámara trasera o fallo de `bindToLifecycle` | Excepción o `CameraState.error` | `Unavailable(CAMERA_ERROR)` | «Reintentar» → volver a vincular | 10 |
| ERR-SCAN-04 | Cámara ocupada por otra app | `CameraState.ERROR_CAMERA_IN_USE` | Ídem, con un texto específico | Ídem | 10 |
| ERR-SCAN-05 | Modelo propio ausente, corrupto o incompatible | `DetectorError` en `prepare` | Transparente: se usa el respaldo y se registra la causa | — | 24 |
| ERR-SCAN-06 | Ningún detector disponible | Fallan los dos `prepare` | `Unavailable(RECOGNITION_UNAVAILABLE)` + «Introducir a mano» | — | 24 |
| ERR-SCAN-07 | Fallos de inferencia repetidos | 5 `detect` fallidos seguidos | Cambio al respaldo | — | 24 |
| ERR-SCAN-08 | Sin display en la ROI | 6 s sin números | Pista `POINT_AT_DISPLAY` | — | 22 |
| ERR-SCAN-09 | Display demasiado lejos | `primaryHeight < 0,15` durante 1 s | Pista `MOVE_CLOSER` | — | 22 |
| ERR-SCAN-10 | Poca luz | `mean(Y) < 0,15` durante 1 s | Pista `LOW_LIGHT` y el botón de linterna resaltado | — | 21 |
| ERR-SCAN-11 | Reflejos | `ratio(Y>0,98) > 0,06` durante 1 s | Pista `GLARE` | — | 21 |
| ERR-SCAN-12 | El tensiómetro muestra un error | ≥ 3 frames con `DEVICE_ERROR` | Pista `DEVICE_SHOWS_ERROR` | — | 19 |
| ERR-SCAN-13 | El tensiómetro está midiendo | `MEASURING` | Pista `DEVICE_MEASURING` | — | 20 |
| ERR-SCAN-14 | Lectura incoherente (SYS ≤ DIA, fuera de rango) | Filtro del estabilizador | Sigue escaneando sin mostrar error (es ruido) | — | 16 |
| ERR-SCAN-15 | Sin estabilidad a los 20 s | `isTimedOut` | «Usar lo leído» (si hay algún campo estable) + «Introducir a mano» | — | 23 |
| ERR-SCAN-16 | App en segundo plano | `ScreenPaused` | Se desvincula el análisis; `Resume` en ≤ 5 s conserva el progreso, si no se reinicia | — | 09 |
| ERR-SCAN-17 | Inactividad de 60 s | Temporizador del ViewModel | `isPaused` + «Continuar» | — | RNF-08 |
| ERR-SCAN-18 | Frecuencia baja (< 4 fps) | Medida en la ventana del estabilizador | `minVotes` adaptativo y aviso en el log (sin UI) | — | RNF-02 |
| ERR-REV-01 | Campo vacío | `REQUIRED` | Error bajo el campo y «Confirmar» deshabilitado | Escribir | 28, 29 |
| ERR-REV-02 | Fuera de rango | `OUT_OF_RANGE` | Error con el rango permitido | Corregir | 28 |
| ERR-REV-03 | SYS ≤ DIA | Código correspondiente | Error bajo SYS | Corregir | 28 |
| ERR-REV-04 | Fecha futura | `IN_FUTURE` | Error bajo la fecha (el selector ya lo impide) | Corregir | 28 |
| ERR-REV-05 | Valores extremos o inusuales | Avisos §3.2 | Diálogo de confirmación con consejo de seguridad | «Guardar igualmente» / «Revisar» | 30 |
| ERR-REV-06 | Posible duplicado | `POSSIBLE_DUPLICATE` | Diálogo | Ídem | 30 |
| ERR-REV-07 | Descartar con cambios | Edición ≠ propuesta | Diálogo de descarte | Confirmar o cancelar | 32 |
| ERR-REV-08 | Muerte del proceso | — | Se restaura desde la ruta + `SavedStateHandle` | — | 33 |
| ERR-SAVE-01 | Fallo de Room (disco lleno, I/O) | `DomainError.Storage` | Se queda en la revisión con un snackbar «No se pudo guardar» + «Reintentar» | Reintentar | 35 |
| ERR-SYNC-01 | Sin red | Restricción de WorkManager / `Network` | `PENDING` (icono discreto en el historial) | Automática | 39 |
| ERR-SYNC-02 | 5xx o timeout | `Server` / `Network` | `PENDING` | Backoff | 39 |
| ERR-SYNC-03 | 422 / 409 | Tabla §9.5 | `SYNC_FAILED` visible + «Reintentar» / «Eliminar» | Manual | 38 |
| ERR-SYNC-04 | Sesión expirada | `SessionExpired` | Se detiene y no se pierde nada | Al volver a iniciar sesión | 40 |
| ERR-SYNC-05 | Reintento tras un timeout ya guardado | 200 idempotente | `SYNCED` sin duplicado | — | 37 |
| ERR-SYNC-06 | Logout con pendientes | `unsyncedCount > 0` | Diálogo de aviso | Cancelar o confirmar | 41 |
| ERR-SYNC-07 | Otro usuario inicia sesión | El propietario no coincide | `clearLocal` antes del refresh | — | §9.5 |
| ERR-DEL-01 | Borrar una medición sincronizada sin red | `Network` | La medición sigue visible + snackbar de error | Reintentar | 43 |

## 11. Concurrencia y ciclo de vida

| Elemento | Hilo o *scope* | Notas |
|----------|----------------|-------|
| `ImageProxyFrameConverter`, `RecognizeFrameUseCase`, `ScanSession`, `DigitDetector.detect` | Hilo `scan-analysis` (único) | Confinamiento: sin locks |
| Comandos al `ScanSession` (reset, pausa, reanudación) | Principal → `ConcurrentLinkedQueue` | Se drena al inicio de `onFrame` |
| `_sessionState` | Escritura en análisis, lectura en principal | `MutableStateFlow` es seguro entre hilos |
| `DigitDetector.prepare` | `AppDispatchers.default` en `viewModelScope` | Se cancela con el ViewModel |
| `DigitDetector.release` | `onCleared` | Se encola en el executor de análisis para no liberar durante una inferencia |
| Room, Retrofit | `AppDispatchers.io` (Room y Retrofit gestionan sus hilos) | — |
| Sincronización | WorkManager | Sobrevive al proceso |

## 12. Estrategia de pruebas

### 12.1 Unitarias (JUnit 5, MockK, Turbine)

| Componente | Casos clave |
|------------|-------------|
| `DisplayLayoutParser` / `NumberGrouper` | **Todos** los `parser_cases` de `reading-golden-cases.json` (`@TestFactory`), más casos propios de bordes |
| `ReadingValidator` | **Todos** los `validation_cases` + fechas (`IN_FUTURE`, `TOO_OLD`) |
| `ReadingStabilizer` | Secuencias: estable a 10 fps, estable a 4 fps, ruido 1 de cada 4, pulso que parpadea, `SYS ≤ DIA` intermitente, reinicio tras 1 s vacío y confianza baja |
| `ScanSession` | Prioridad de pistas, timeout a 20 s, `DEVICE_ERROR` ≥ 3, pausa < 5 s y > 5 s, congelado tras captura |
| `FrameExposureEvaluator` | `IntArray` sintéticos: negro, blanco saturado, gris medio e histéresis |
| `RecognizeFrameUseCase` | Detector que falla → `NO_READING`; composición correcta |
| `SaveMeasurementUseCase` | `editedFields`, revalidación y llamada al scheduler |
| `CheckDuplicateMeasurementUseCase` | Ventana de ±5 min, valores distintos |
| `MlKitSymbolMapper` | Tabla §9.1 completa (parametrizado) |
| `DetectionDecoder` | Umbral, NMS (dos cajas IoU 0,6 → 1), deshacer letterbox con relleno horizontal y vertical |
| `RoiGeometry` (`:ui`, JVM) | Rotaciones y relaciones de aspecto (§6.1) |
| `ReadingInputParser` | `""`, `"12a"`, `"0128"`, `"999"` |
| `ScanViewModel` | Flujo de permisos (los 3 casos), `prepare` falla → `Unavailable`, captura → secuencia de eventos haptic + navegación, «Usar lo leído», pausa por inactividad |
| `ReviewMeasurementViewModel` | Prellenado, validación en vivo, diálogo de avisos, guardado OK y KO, restauración desde `SavedStateHandle`, descarte con y sin cambios |
| `MeasurementSynchronizer` | Tabla §9.5 completa con MockWebServer y un DAO falso |
| `MeasurementRepositoryImpl.refresh` | Fusión: no toca `PENDING`, borra `SYNCED` ausentes y hace upsert |

Los casos dorados se copian de `specs/contracts/` a `domain/src/test/resources/contracts/`
mediante la tarea Gradle `syncContractFixtures`, que es dependencia de `test`. Así **nunca** se
duplican a mano.

### 12.2 Instrumentadas (`androidTest`)
- `Migration1To2Test` y `MeasurementDaoTest` (Room en memoria).
- `ReviewMeasurementScreenTest` (Compose): error en vivo, botón deshabilitado y diálogo.
- `LiteRtDigitDetectorBenchmark`: latencia p50 y p95 sobre 50 imágenes del set de test de B-003,
  con aserción de RNF-A002-03.

### 12.3 Validación de campo (bloqueante para cerrar la spec)
- **Matriz**: ≥ 3 modelos de tensiómetro × 2 móviles (referencia y gama baja) × 5 condiciones (luz
  buena, luz tenue, reflejo, inclinación de ~20° y distancia lejana) × 10 lecturas, para cada
  detector primario (ML Kit y LiteRT).
- **Registro por intento**: tiempo hasta la lectura estable, lectura propuesta, lectura real,
  campos editados y pistas mostradas.
- **Resultado**: `specs/002-realtime-measurement-capture/validation-report.md` con las métricas
  §2 de requisitos y la decisión sobre ADR-A002-04.

## 13. Riesgos

| Riesgo | Prob. | Impacto | Mitigación |
|--------|-------|---------|------------|
| ML Kit lee mal los 7 segmentos | A | M | Es solo para arrancar; tabla de confusiones, consenso y revisión obligatoria; el modelo propio en la fase E |
| Layouts de display no cubiertos por el parser | M | M | Revisión obligatoria, casos dorados ampliables y validación de campo con ≥ 3 modelos |
| Rendimiento insuficiente en gama baja | M | M | ROI escalada a ≤ 640 px, consenso adaptativo, CPU XNNPACK y medición en T-A002-38 |
| Diferencias de CameraX entre fabricantes (aspecto, rotación) | M | M | `RoiGeometry` testeada y prueba en ≥ 2 fabricantes |
| El usuario no mantiene el móvil quieto | M | B | Pistas, ventana de 1,5 s y tolerancia del 30 % de votos discrepantes |
| Pérdida de pendientes por un bug de migración | B | A | ADR-A002-11, tests de migración y prohibición del modo destructivo |
| Textos clínicos inadecuados | B | A | Q-SAFE-01: revisión profesional antes de publicar |
