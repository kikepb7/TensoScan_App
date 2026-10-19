# Constitución técnica — TensoScan App

Principios **no negociables** para todo el código de la app. Concretan las skills del proyecto
(`.claude/skills/android-architecture`, `android-patterns` y `android-testing`) para el caso de
TensoScan. Si una spec contradice esta constitución, se modifica primero la constitución con una
justificación explícita.

## 1. Arquitectura (skill `android-architecture`)

**C-ARCH-01. Dirección de dependencias**: `ui → presentation → domain ← data`, y `di` cablea.

| Módulo Gradle | Contenido | Puede depender de |
|---------------|-----------|-------------------|
| `:domain` | Kotlin/JVM puro: modelos, interfaces de repositorio y detector, casos de uso, reglas (validador, parser, estabilizador, clasificador) | `kotlinx-coroutines-core` |
| `:data` | Retrofit, Room, DataStore, WorkManager, LiteRT, ML Kit, implementaciones de repositorio, DTO y mappers | `:domain` |
| `:presentation` | ViewModels, `*ScreenUiState`, `*UiState`, `*UiAction`, `*UiEvent`, mappers y parsers de pantalla | `:domain`, AndroidX Lifecycle |
| `:ui` | Composables (Screen y Content), componentes, tema, previews, integración de CameraX | `:presentation` |
| `:app` | `Application`, `MainActivity`, navegación raíz y `di/` con todos los módulos Hilt | todos |

**C-ARCH-02.** `:domain` no importa Android, Compose, Retrofit, Room, Hilt, Koin ni `javax.inject`.
Los casos de uso se proporcionan con `@Provides` en `app/di/UseCaseModule.kt`.

**C-ARCH-03.** `:presentation` no conoce DTO, Retrofit, Room ni CameraX.

**C-ARCH-04.** La estructura es feature-first dentro de cada módulo (`.../measurement/`,
`.../scan/`, `.../auth/`...). No se crean carpetas `utils`, `helpers` ni `common` como cajón de
sastre, y no se usan los nombres `Manager`, `Helper`, `Processor` ni `Utils`.

**C-ARCH-05.** La visibilidad es `internal` por defecto. Solo se hace `public` lo que otro módulo
necesita.

**C-ARCH-06. DI**: Hilt con inyección por constructor. `@Binds` para interfaz → implementación y
`@Provides` cuando hay lógica de construcción. Se usa `javax.inject.Inject`. Quedan prohibidos los
*service locators* y los singletons manuales.

**C-ARCH-07.** Una sola `Activity` (`@AndroidEntryPoint`), Navigation Compose con rutas tipadas
`@Serializable` y sin fragments.

## 2. Patrones (skill `android-patterns`)

**C-PAT-01. UDF**: `UiAction → onAction() → estado interno → UiState → Composable`. Los efectos de
una sola vez salen como `UiEvent` por un `SharedFlow`.

**C-PAT-02.** Cada pantalla expone:
- `uiState: StateFlow<XScreenUiState>`, construido con `stateIn(WhileSubscribed(5_000))` cuando
  procede de flows;
- `uiEvents: SharedFlow<XUiEvent>`;
- `fun onAction(action: XUiAction)` como **única** entrada de acciones del usuario.

**C-PAT-03.** Los estados se modelan con `sealed interface` y no con varios booleanos. El
contenido que se pinta va en un `data class` anotado con `@Immutable`.

**C-PAT-04.** Los ViewModels coordinan; no formatean textos, no parsean respuestas y no contienen
reglas de negocio. Esas tareas van en clases con nombre propio: `*Parser`, `*Mapper`,
`*Formatter`, `*Validator` y `*Classifier`.

**C-PAT-05. Errores**: se usa `Result<T>` con fallos tipados (`sealed class DomainError :
Exception`). No se lanzan excepciones a través de flows cuando basta con un resultado
recuperable, y los errores de bajo nivel se traducen en el límite de cada capa.

**C-PAT-06.** Los repositorios exponen modelos de dominio y `Flow`, nunca DTO.

**C-PAT-07.** Los casos de uso siguen la forma `[Verbo][Sujeto]UseCase` con un método `fun verbo(...)`,
por ejemplo `saveMeasurementUseCase.save(...)`.

**C-PAT-08. Compose**: separación Screen (recoge estado y efectos) / Content (sin estado, con
preview). No se pasa el ViewModel a componentes anidados, las listas perezosas llevan claves
estables y nunca se usa `CoroutineScope(...)` dentro de un composable.

## 3. Testing (skill `android-testing`)

**C-TEST-01.** JUnit 5, MockK, Turbine y `kotlinx-coroutines-test` con `runTest`.

**C-TEST-02.** Los tests tienen nombres con backticks de la forma
`` `When <condición> then <resultado>` ``, el sujeto se llama `sut` y el cuerpo tiene cuatro
bloques (crear, stub, ejecutar, comprobar) con las variables `expected` y `actual`.

**C-TEST-03.** Se usan tests parametrizados o `@TestFactory` cuando hay tablas de casos. Los casos
dorados de `contracts/reading-rules.md` se ejecutan como tests dinámicos.

**C-TEST-04. Cobertura mínima por PR**: todo caso de uso, validador, parser, mapper, estabilizador y
ViewModel nuevo o modificado tiene tests unitarios. Los ViewModels se prueban a través de
`onAction`, `uiState` y `uiEvents`.

**C-TEST-05.** Los dispatchers se inyectan (`@IoDispatcher` y `@DefaultDispatcher`) y no se escribe
`Dispatchers.IO` directamente en el código de producción.

## 4. Seguridad del paciente y producto sanitario

**C-SAFE-01.** **Ninguna lectura se guarda sin confirmación explícita del usuario.** El
reconocimiento solo propone valores.

**C-SAFE-02.** La app **no diagnostica**. Las clasificaciones son informativas, citan la guía
clínica usada e incluyen un aviso para consultar a un profesional.

**C-SAFE-03.** Los valores fuera de rango fisiológico se rechazan. Los valores extremos pero
posibles (por ejemplo SYS ≥ 180) necesitan una confirmación adicional y muestran una recomendación
de seguridad.

**C-SAFE-04.** Las reglas de validación son **las mismas** en app y backend
(`contracts/reading-rules.md`).

## 5. Privacidad y seguridad

**C-PRIV-01.** Los frames de cámara **nunca** se guardan en disco ni salen del dispositivo. La
inferencia se hace siempre en el dispositivo.

**C-PRIV-02.** No se guardan fotos en la galería pública (DCIM).

**C-PRIV-03.** Los tokens se guardan cifrados (DataStore + Android Keystore), quedan excluidos del
backup y no aparecen nunca en los logs.

**C-PRIV-04.** No se permite tráfico en claro en *release*. La `network_security_config` solo
autoriza `10.0.2.2` en *debug*.

**C-PRIV-05.** Solo se piden los permisos estrictamente necesarios: `CAMERA` (se elimina
`RECORD_AUDIO`).

## 6. Rendimiento y accesibilidad

**C-PERF-01.** El hilo principal no hace inferencia ni E/S. El análisis de cámara va en un
executor dedicado con `STRATEGY_KEEP_ONLY_LATEST`.

**C-A11Y-01.** Los textos están en `strings.xml` (español por defecto) y no hay cadenas
literales en el código. Todo `contentDescription` es correcto, los cambios de estado relevantes se
anuncian con `LiveRegion` y los objetivos táctiles miden al menos 48 dp.

## 7. Definition of Done (aplica a cada tarea)

- [ ] El código cumple esta constitución (arquitectura, patrones, nombres y visibilidad).
- [ ] Los tests unitarios de la tarea están escritos y pasan: `./gradlew testDebugUnitTest`.
- [ ] El análisis estático pasa: `./gradlew detekt` (desde T-A001-09).
- [ ] La app compila: `./gradlew assembleDebug`.
- [ ] Cada composable nuevo tiene `@Preview`.
- [ ] La tarea está marcada como `[x]` en `tasks.md` y el commit lleva el formato `T-xxx | ...`.
- [ ] Si la tarea cambia un contrato, la copia del otro repositorio está actualizada.
