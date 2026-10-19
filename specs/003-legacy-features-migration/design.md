# A-003 — Migración de features existentes · Diseño

> Estado: 📝 Pendiente de aprobación · Requisitos: [requirements.md](requirements.md)

## 1. Patrón común (plantilla para cada feature)

```
domain/<feature>/      model, repository (interfaz), usecase
data/<feature>/        remote/<Feature>Api + dto, mapper/<X>Mapper, <Feature>RepositoryImpl
presentation/<feature>/<Feature>ViewModel, <Feature>ScreenUiState, <Feature>UiState,
                       <Feature>UiAction, <Feature>UiEvent, mapper/<Feature>UiStateMapper
ui/<feature>/          <Feature>Screen (recoge estado y eventos), <Feature>Content (sin estado, con @Preview),
                       components/*
```
Los errores llegan como `DomainError` y `presentation/<feature>/mapper` los traduce a enums de UI
(`LoginErrorUi.INVALID_CREDENTIALS`...). `:ui` los resuelve con `stringResource`.

## 2. Arranque y sesión (T-A003-01)

- `MainActivity`: `installSplashScreen().setKeepOnScreenCondition { viewModel.isResolving }`.
- `AppViewModel` (`:presentation/app`) observa `SessionRepository.observeSession()` y expone
  `startDestination` y el evento `NavigateToLoginExpired`.
- El `NavHost` raíz recibe `startDestination` (`HistoryRoute` o `LoginRoute`). Ante `Expired`, se
  navega con `popUpTo(0) { inclusive = true }` y se muestra un snackbar.
- `LoginRoute(prefilledEmail: String? = null, reason: LoginReason? = null)` para cubrir
  REQ-A003-02 y REQ-A003-07.

## 3. Login y registro (T-A003-02/03)

```kotlin
internal sealed interface LoginScreenUiState { data class Editing(val content: LoginUiState) : …; data class Submitting(val content: LoginUiState) : … }
internal data class LoginUiState(val email: String, val password: String, val error: LoginErrorUi?, val isSubmitEnabled: Boolean, val notice: LoginNoticeUi?)
internal sealed interface LoginUiAction { EmailChanged, PasswordChanged, SubmitClicked, RegisterClicked }
internal sealed interface LoginUiEvent { NavigateToHistory, NavigateToRegister }
```
- `LoginUseCase.login(email, password): Result<Unit>`. El repositorio guarda los tokens y el
  propietario de los datos locales (A-002 §9.5), y programa la sincronización.
- Mapeo de errores: `Unauthorized` (`INVALID_CREDENTIALS`) → `INVALID_CREDENTIALS`, `Network` →
  `NO_CONNECTION`, 429 → `TOO_MANY_ATTEMPTS`, el resto → `GENERIC`.
- Registro: `RegistrationFormValidator` (`:domain/auth`) con `EmailValidator` (regex simple +
  `trim`) y `PasswordPolicy` (8–128). Errores por campo; el 409 → error en el email.

## 4. Historial (T-A003-04…07)

### 4.1 Estado
```kotlin
internal sealed interface HistoryScreenUiState {
    data object Loading : HistoryScreenUiState
    data object Empty : HistoryScreenUiState
    data class Content(val content: HistoryUiState) : HistoryScreenUiState
    data object Error : HistoryScreenUiState
}
internal data class HistoryUiState(
    val items: List<MeasurementItemUiState>,       // key = clientId
    val chart: TrendChartUiState,
    val isRefreshing: Boolean,
    val isExporting: Boolean,
    val pendingDeletion: MeasurementItemUiState?,  // diálogo de confirmación
)
internal data class MeasurementItemUiState(
    val id: String, val pressureText: String /* "128/82" */, val pulseText: String,
    val dateTimeText: String, val category: BloodPressureCategoryUi, val sync: SyncBadgeUi,
)
```
- `uiState = combine(observeMeasurementsUseCase.observe(), refreshState, exportState, deletionState)
  → HistoryUiStateMapper → stateIn(WhileSubscribed(5_000), Loading)`.
- Se refresca al entrar y con *pull-to-refresh*. Un fallo con datos → evento `ShowRefreshError`.
  Un fallo sin datos (Room vacío y primera carga) → `Error`.
- `HistoryUiStateMapper` usa `MeasurementDateFormatter` (zona del sistema, formato corto
  localizado) y `BloodPressureClassifier`.

### 4.2 ADR-A003-01 — Guía de clasificación
- **Contexto**: la clasificación actual usa solo la sistólica y umbrales de consulta (120/140),
  con etiquetas alarmistas. Las mediciones de TensoScan son **domiciliarias**, y las guías fijan
  umbrales domiciliarios más bajos que los de consulta (≥ 135/85 frente a ≥ 140/90).
- **Decisión (propuesta, pendiente de Q-SAFE-02)**: categorías de la guía ESC 2024 con umbrales
  domiciliarios. La categoría final es la **más alta** que resulte de SYS o de DIA:

  | Categoría | SYS (mmHg) | DIA (mmHg) | Etiqueta | Color (rol del tema) |
  |-----------|-----------|------------|----------|----------------------|
  | `NON_ELEVATED` | < 120 | < 70 | «No elevada» | `success` |
  | `ELEVATED` | 120–134 | 70–84 | «Elevada» | `warning` |
  | `HYPERTENSION_RANGE` | ≥ 135 | ≥ 85 | «En rango de hipertensión» | `error` |

  Además, si se cumple `VERY_HIGH_PRESSURE` (reading-rules §3.2) se muestra el icono de aviso y el
  texto de seguridad al abrir el detalle.
- **Alternativas**: AHA/ACC 2017 (cuatro categorías, umbrales de consulta) y ESH 2023 (seis
  grados, umbrales de consulta). Se descartan como opción por defecto porque son umbrales de
  consulta. El clasificador es una estrategia intercambiable (`BloodPressureGuideline`) para poder
  cambiar de guía sin tocar la UI.
- **Texto fijo** bajo el gráfico: «Clasificación orientativa basada en lecturas domiciliarias
  individuales. El diagnóstico requiere varias mediciones y la valoración de un profesional».
- ⚠️ Las cifras de la tabla las **debe validar un profesional sanitario** antes de publicar.

### 4.3 Gráfico (T-A003-05)
- Se reutiliza y rehace `SummaryGraphComponent` como `TrendChart` en Canvas (sin librería
  externa), con tres series, ejes con unidades, selector de rango (7 días, 30 días, todo) y una
  descripción accesible que resuma mínimo, máximo y media de cada serie.
- Se elimina `MeasurementsWebViewScreen`, la ruta `webview/{htmlContent}` y la llamada a
  `/html`.

### 4.4 Exportación PDF (T-A003-06)
- `ExportMeasurementsReportUseCase.export(range): Result<ReportFile>` (`ReportFile(path,
  mimeType)` es un modelo de dominio y el *path* es una cadena opaca).
- `:data` descarga en `cacheDir/reports/` con `@Streaming` y escritura en `IoDispatcher`.
- `:ui` recibe `HistoryUiEvent.ShareReport(path)` y construye el `Intent.ACTION_SEND` con
  `FileProvider` (`file_paths.xml` → `cache-path name="reports"`).
- Se eliminan `savePdfToStorage` y `openPdfFile` (`filesDir` fijo y `Toast` literal).

## 5. Usuario y asistente (T-A003-08/09)
- Usuario: `ProfileScreenUiState` (`Loading`, `Content` o `Error`). La carga se dispara al
  suscribirse (`stateIn` de un flow que la ejecuta), no en `init`. «Cerrar sesión» →
  `LogoutUseCase` (avisa de los pendientes: diálogo de A-002) → evento `NavigateToLogin`.
- Asistente: `AssistantViewModel` con la lista de mensajes `(id, role, text, status)`. El
  indicador de escritura es un **estado** (no un mensaje falso `"..."`) y un error marca el
  último mensaje del usuario como fallido con «Reintentar». Se corrige `conntent` → `content`.

## 6. Barridos transversales (T-A003-10…12)
- **Textos**: `strings.xml` (es) para todos los literales de `BottomBarItem`,
  `CameraUserAssistantComponent`, las etiquetas de estado y los `Toast`. Se revisan los
  `contentDescription`.
- **Nombres**: `*.compose.kt` → `<Nombre>.kt`; `*ScreenView` → `*Screen`; `*View` →
  componentes con nombre de sustantivo (`MeasurementCard`, `PulseLoadingIndicator`...).
- **Visibilidad**: `internal` por defecto en `:presentation` y `:ui`.
- **Código muerto**: `Either`, `FailureDomain`, `PredictionUiModel`, `UploadErrorModel`,
  `sharedViewModel`, la ruta `Summary` con JSON, `utils/` y `common/extensions/`.
- **detekt**: se vacía el *baseline*.

## 7. Pruebas
- ViewModels: Login (éxito → evento; cada error → `LoginErrorUi`), Register (validación por
  campo y 409), History (estados `Loading`/`Empty`/`Content`/`Error`, error de refresco con datos
  → evento, borrado y exportación), Profile y Assistant.
- Dominio: `BloodPressureClassifier` (`@TestFactory` con los límites de cada categoría y los
  casos «gana la más alta»), `RegistrationFormValidator`, `EmailValidator` y `PasswordPolicy`.
- Mappers: `HistoryUiStateMapper` (formato y categorías), `LoginErrorUiMapper`.
