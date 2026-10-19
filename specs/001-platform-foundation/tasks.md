# A-001 — Fundación de plataforma · Tareas

> Orden obligatorio salvo que se indique que dos tareas son paralelizables. Cada tarea es un PR o
> commit atómico que cumple la Definition of Done de la constitución (§7).
> Formato de commit: `T-A001-xx | <descripción en inglés>`.

> **Orden de ejecución real**: T-A001-09 *(solo el workflow mínimo de CI, para verificar todo lo
> demás)* → T-A001-01 → **T-A001-13** → T-A001-02 → T-A001-09 *(resto)* → T-A001-03 →
> T-A001-11 → T-A001-04 → T-A001-05 → T-A001-06 → T-A001-07 → T-A001-08 → T-A001-10 →
> T-A001-12 → T-A001-14. (Los números no reflejan el orden.)

## Fase 1 — Toolchain

- [ ] **T-A001-01 · Subir el toolchain a versiones estables**
  - Actualizar `libs.versions.toml`: AGP, Kotlin, KSP alineado, Compose BOM, CameraX estable,
    `androidx.navigation:navigation-compose`, `androidx.lifecycle:*`, coroutines, Room y Retrofit.
  - Eliminar `ktor-*`, `coil-network-ktor`, `org.jetbrains.androidx.*` y `tensorflowlite`.
    Mantener Koin y Gson **temporalmente** (se eliminan en T-A001-05 y T-A001-08).
  - Pasar a JDK 17 y `jvmTarget = 17` en todos los módulos.
  - Requisitos: REQ-A001-01, 03, 05 (artefactos) · Hallazgos: AUD-A-48, 50, 54.
  - DoD: `assembleDebug` en verde y smoke test 1–6 de design §9.
- [ ] **T-A001-02 · Room de kapt a KSP; eliminar kapt**
  - Sustituir `kapt(libs.room.compiler)` por `ksp(...)`, activar `room { schemaDirectory(...) }`
    y exportar el esquema v1 actual a `data/schemas/` (es la base de la migración v2 de A-002).
  - Quitar `kotlin("kapt")` de todos los módulos (incluido `:domain`, que no lo usa).
  - Requisitos: REQ-A001-02 · Hallazgos: AUD-A-49.
  - DoD: `data/schemas/.../1.json` versionado y build en verde.

- [ ] **T-A001-13 · Renombrar `com.example.*` → `com.tensoscan.*`** (decisión del usuario)
  - Design §5.4. Se ejecuta justo después de T-A001-01 y antes de T-A001-03.
  - Requisitos: REQ-A001-30 · Hallazgos: AUD-A-10.
  - DoD: `grep -r "com.example"` sin resultados y `assembleDebug` en verde.

## Fase 2 — Calidad (va pronto para dar red de seguridad al resto)

- [ ] **T-A001-09 · Infraestructura de tests, detekt y CI**
  - Plugin `android-junit5`, JUnit Jupiter, MockK, Turbine y coroutines-test en todos los módulos.
  - Crear `:core:testing` con `MainDispatcherExtension`.
  - *Orden*: esta tarea cubre los módulos que existen en este momento (`:domain`, `:data`, `:ui`,
    `:app`). `:presentation` recibe la misma configuración al crearse en T-A001-03.
  - Borrar `ExampleUnitTest` y `ExampleInstrumentedTest`.
  - Configurar detekt + formatting (`config/detekt/detekt.yml`) y generar el *baseline*.
  - Quitar `.github` de `.gitignore` y crear `.github/workflows/android-ci.yml` y
    `.github/pull_request_template.md` (secciones: Spec/Tareas, Cambios, Tests, Checklist DoD).
  - Primer test real: `HttpErrorMapper` todavía no existe, así que se empieza por un test de
    caracterización del mapper actual `FailureDto.toFailureDomain()` que **documente** el bug del
    400 (test marcado como «comportamiento actual», que se invierte en T-A001-06).
  - Requisitos: REQ-A001-25…28 · Hallazgos: AUD-A-52, 53, 54.
  - DoD: CI en verde en el PR.

## Fase 3 — Módulos y DI

- [ ] **T-A001-03 · Crear `:presentation` y limpiar dependencias de capa**
  - Nuevo módulo `:presentation`. Mover **sin cambiar lógica** los `*ViewModel.kt` y sus clases de
    estado de `ui/feature/*` a `presentation/<feature>/`. Mover `PredictionUiModel` y los modelos
    no visuales a `presentation/<feature>/model`.
  - `:ui` depende de `:presentation`. Quitar de `:data` Compose, Material, activity-compose y
    CameraX.
  - Añadir la tarea `verifyModuleBoundaries` (design §1.1) y llamarla en CI.
  - Requisitos: REQ-A001-07, 09, 10 · Hallazgos: AUD-A-03, 51.
  - DoD: build en verde y CA-A001-03 verificado (con un PR de prueba o un test de la tarea).
- [ ] **T-A001-04 · Introducir Hilt**
  - Aplicar los plugins de Hilt y KSP en `:app`, `:presentation` y `:data`.
  - `@HiltAndroidApp` en `TensoScanApplication` y `@AndroidEntryPoint` en `MainActivity`.
  - Crear `app/di/` con `NetworkModule`, `DatabaseModule`, `RepositoryModule`, `UseCaseModule` y
    `DispatcherModule` (design §3.1). Poner `@Inject constructor` en las implementaciones de `:data`.
  - `@HiltViewModel` + `@Inject constructor` en todos los ViewModels, y `hiltViewModel()` en las
    pantallas en lugar de `koinViewModel()`.
  - `PermissionManager` (clase sin estado) deja de inyectarse; se elimina en A-002.
  - Requisitos: REQ-A001-11, 12 · Hallazgos: AUD-A-01.
- [ ] **T-A001-05 · Eliminar Koin y purificar `:domain`**
  - Borrar `DiConfigurator.kt`, `DataModule.kt`, `DomainModule.kt` y `UiModule.kt`, y todas las
    dependencias e imports de Koin.
  - Quitar de `:domain` los plugins `kapt` y `kotlinxSerialization` y la dependencia de Koin.
  - Requisitos: REQ-A001-08, 13 · Hallazgos: AUD-A-01, 02.
  - DoD: CA-A001-02 y CA-A001-04; smoke test completo.
- [ ] **T-A001-11 · Dispatchers inyectados**
  - Interfaz `AppDispatchers` en `:domain` (`io`, `default`, `main`), `DispatcherModule` en
    `app/di` y `TestAppDispatchers` en `:core:testing`. Sustituir cada `Dispatchers.IO` /
    `Dispatchers.Default` del código de producción.
  - Requisitos: REQ-A001-17.

## Fase 4 — Errores, red y sesión

- [ ] **T-A001-06 · Modelo de errores**
  - `DomainError` y `FieldError` en `:domain/error/`. Helper `suspendRunCatching` y
    `HttpErrorMapper` en `:data/network/`.
  - Regla detekt que prohíbe `runCatching`. Invertir el test de caracterización de T-A001-09
    (ahora 400 → `Validation`).
  - Mover `FailureDomain` a `:domain/error/` como alias temporal *deprecated* hacia `DomainError`
    para no romper las features legacy, que lo eliminan en A-003.
  - Tests: `HttpErrorMapperTest` (parametrizado: 400, 401 + `TOKEN_EXPIRED`, 401, 403, 404, 409,
    422 con `details`, 500, 503, `IOException`, `SerializationException` y `CancellationException`
    relanzada).
  - Requisitos: REQ-A001-14…16 · Hallazgos: AUD-A-06, 11 (parcial), 12.
- [ ] **T-A001-07 · Mappers con nombre propio**
  - Sustituir `data/common/Extensions.kt` y `feature/*/utils/*Mapper.kt` por clases
    `internal class <Source>Mapper @Inject constructor()` dentro de `data/<feature>/mapper/`.
    Eliminar los duplicados.
  - Tests de mapper con valores exactos.
  - Hallazgos: AUD-A-05.
- [ ] **T-A001-08 · Red: BuildConfig, interceptor, authenticator, kotlinx-serialization y seguridad de transporte**
  - `buildConfigField("String", "API_BASE_URL", ...)` por *build type*, leyendo
    `local.properties` en debug.
  - `AuthInterceptor` y `TokenAuthenticator` (design §5.1) detrás del flag
    `BuildConfig.REFRESH_TOKEN_ENABLED`, que vale `false` hasta que exista B-001.
  - Quitar el parámetro `@Header("Authorization")` de los servicios.
  - Pasar de Gson a `retrofit2-kotlinx-serialization-converter` y anotar los DTO con
    `@Serializable`/`@SerialName`. Eliminar Gson.
  - `network_security_config` de release y de debug; quitar `usesCleartextTraffic`.
  - Tests: `AuthInterceptorTest` (con y sin sesión) y `TokenAuthenticatorTest` (un solo refresh
    con peticiones concurrentes, abandono en el segundo intento y expiración), con MockWebServer.
  - Requisitos: REQ-A001-04, 18…20, 23 · Hallazgos: AUD-A-37, 38, 40, 50.
- [ ] **T-A001-10 · Sesión cifrada**
  - `TokenCipher` (Keystore AES-GCM), `SessionLocalDataSource` (DataStore), `DataMigration`
    desde `auth_prefs`, `SessionRepository` en `:domain` con `observeSession()` y su
    implementación.
  - Reglas de backup y de extracción que excluyen el DataStore de sesión.
  - Sustituir `TokenManager` en todos sus usos.
  - Tests: `SessionRepositoryImplTest` (con un `TokenCipher` falso), test de la migración (con
    DataStore en un directorio temporal) y un test instrumentado de `TokenCipher` en
    `androidTest` (necesita el Keystore real).
  - Requisitos: REQ-A001-21, 22, 24 · Hallazgos: AUD-A-39.

## Fase 5 — Cierre

- [ ] **T-A001-12 · Navegación tipada (esqueleto)**
  - Sustituir `Routes` (strings) por objetos `@Serializable` en `presentation/navigation/`, y
    `NavHost` con `composable<LoginRoute>`, etc. Las rutas del WebView y de `Summary` con
    argumentos JSON se mantienen sin cambios (se rehacen en A-003).
  - Requisitos: REQ-A001-05.
- [ ] **T-A001-14 · Release hardening**
  - `isMinifyEnabled` e `isShrinkResources` en release y reglas R8.
  - Smoke test sobre el APK release.
  - Requisitos: REQ-A001-06 · Hallazgos: AUD-A-54.

## Cierre de la spec

- [ ] Todos los CA-A001-xx verificados y anotados en el PR final.
- [ ] Hallazgos resueltos: AUD-A-01, 02, 03, 05, 06, 10, 12, 37, 38, 39, 40, 48, 49, 50, 51, 52,
  53, 54.
