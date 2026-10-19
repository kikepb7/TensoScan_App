# A-001 — Fundación de plataforma · Diseño

> Estado: 📝 Pendiente de aprobación · Requisitos: [requirements.md](requirements.md)

## 1. Módulos objetivo

```
:app ──────────────┬──► :ui ──► :presentation ──► :domain
  (Application,    │                                 ▲
   MainActivity,   ├──► :data ───────────────────────┘
   di/*Module.kt,  │
   NavHost raíz)   └──► :presentation, :domain (para cablear)

:core:testing (solo testImplementation): MainDispatcherExtension, builders de fixtures
```

| Módulo | Plugins | Dependencias principales |
|--------|---------|--------------------------|
| `:domain` | `kotlin("jvm")` | coroutines-core |
| `:data` | android-library, ksp, hilt, kotlin-serialization | Retrofit, OkHttp, serialization-converter, Room (KSP), DataStore, WorkManager y `androidx.hilt:hilt-work` (en A-002, para `@HiltWorker`), LiteRT y ML Kit (en A-002) |
| `:presentation` | android-library, ksp, hilt, kotlin-serialization | lifecycle-viewmodel-ktx, lifecycle-viewmodel-savedstate, navigation-common (solo para `toRoute`) y `hilt-android` (`@HiltViewModel` necesita el procesador de Hilt en el módulo que lo declara) |
| `:ui` | android-library, compose | compose-bom, material3, navigation-compose, hilt-navigation-compose, camera-view y camera-lifecycle (en A-002), lifecycle-runtime-compose |
| `:app` | android-application, hilt, ksp | todos los módulos, hilt-android, hilt-work (en A-002) |

**Por qué las rutas de navegación viven en `:presentation`**: los ViewModels leen sus argumentos
con `savedStateHandle.toRoute<ReviewMeasurementRoute>()`, así que las clases de ruta tienen que
ser visibles desde `:presentation`. `:ui` las usa para declarar el `NavHost`. Es la opción que
mantiene la dirección `ui → presentation`.

### 1.1 Verificación de fronteras (REQ-A001-10)

Una tarea Gradle `verifyModuleBoundaries` en el `build.gradle.kts` raíz recorre las
configuraciones `implementation` y `api` de cada proyecto y falla si encuentra un borde que no
está en la lista blanca:

```kotlin
val allowed = mapOf(
    ":domain" to setOf(),
    ":data" to setOf(":domain"),
    ":presentation" to setOf(":domain"),
    ":ui" to setOf(":presentation", ":domain"),
    ":app" to setOf(":ui", ":presentation", ":data", ":domain"),
)
```
CI ejecuta `./gradlew verifyModuleBoundaries` antes que los tests.

*Alternativa descartada*: plugins de terceros de grafos de módulos. Añaden una dependencia para
algo que se resuelve en unas 20 líneas.

## 2. Toolchain (T-A001-01)

Política: en el momento de implementar se elige **la última versión estable** de cada librería y
se registra en el PR. Restricciones:

- La versión de KSP debe corresponder a la versión de Kotlin elegida (`<kotlin>-<ksp>`).
- La Compose BOM debe ser compatible con la versión del plugin de Compose de Kotlin.
- CameraX debe ser estable y tener `camera-mlkit-vision` disponible en la misma versión.
- Se eliminan del catálogo: `ktor-*`, `gsonConverter`, `koin-*`, los artefactos
  `org.jetbrains.androidx.*`, `kotlinx-coroutines` 1.6.1 (se sube), `coil-network-ktor` y
  `tensorflowlite` (se reintroduce como LiteRT en A-002).
- Se añaden: `hilt-android`, `hilt-compiler`, `androidx-hilt-navigation-compose`,
  `retrofit-converter-kotlinx-serialization`, `okhttp-logging-interceptor` (solo debug),
  `datastore-preferences`, `junit-jupiter`, `mockk`, `turbine`, `coroutines-test`, el plugin
  `android-junit5` y el plugin `detekt`.

## 3. DI con Hilt (T-A001-04/05)

### ADR-A001-01 — Hilt en lugar de Koin
- **Contexto**: la skill `android-architecture` exige Hilt. El proyecto usa Koin.
- **Decisión**: migrar todo a Hilt.
- **Motivos**: el grafo se verifica en compilación, la integración con ViewModel, Navigation y
  WorkManager (`@HiltWorker`) es de primera clase, y el proyecto queda alineado con el estándar
  del equipo.
- **Coste**: unos 7 ViewModels, 6 repositorios y 11 casos de uso. Es una migración mecánica.

### ADR-A001-02 — Migración «big bang» de la DI (y no incremental)
- **Alternativa A (descartada)**: convivencia temporal Koin + Hilt. Habría dos grafos, dos
  inicializaciones y riesgo de instancias duplicadas de Room o OkHttp (dos bases de datos sobre el
  mismo fichero = corrupción).
- **Alternativa B (elegida)**: un único PR que sustituye Koin por Hilt en todas las features sin
  tocar su lógica. El tamaño es asumible y el comportamiento se verifica con el smoke test (§9).

### ADR-A001-03 — El dominio no lleva anotaciones `@Inject`
- **Contexto**: la constitución C-ARCH-02 prohíbe APIs de DI en `:domain`, y `javax.inject` es una
  API de DI aunque sea un estándar.
- **Decisión**: los casos de uso son clases Kotlin puras y se proporcionan con `@Provides` en
  `UseCaseModule` (`@InstallIn(SingletonComponent::class)` y **sin** *scope*, es decir, una
  instancia nueva en cada inyección). Se instalan en `SingletonComponent` y no en
  `ViewModelComponent` porque algunos casos de uso los necesitan también los `Worker` de
  WorkManager y el `Application`, que no ven el grafo de ViewModel. Las reglas puras
  (validador, parser, etc.) se proporcionan igual. Solo lo que tiene estado por pantalla
  (`ScanSession`, el detector) usa `ViewModelComponent` + `@ViewModelScoped` (A-002).
- **Coste**: unas pocas líneas de boilerplate por caso de uso. **Beneficio**: el dominio es
  portable y su `build.gradle.kts` lo garantiza.

### 3.1 Módulos Hilt

```kotlin
// app/src/main/kotlin/com/tensoscan/di/
@Module @InstallIn(SingletonComponent::class)
internal object NetworkModule {
    @Provides @Singleton fun json(): Json = Json { ignoreUnknownKeys = true; explicitNulls = false }
    @Provides @Singleton fun okHttp(auth: AuthInterceptor, authenticator: TokenAuthenticator): OkHttpClient
    @Provides @Singleton @AuthenticatedApi fun retrofit(client: OkHttpClient, json: Json): Retrofit
    @Provides @Singleton @PublicApi fun publicRetrofit(json: Json): Retrofit  // sin authenticator (login, refresh)
    @Provides fun authApi(@PublicApi r: Retrofit): AuthApi = r.create()
    // ...
}
@Module @InstallIn(SingletonComponent::class) internal abstract class RepositoryModule {
    @Binds abstract fun measurementRepository(impl: MeasurementRepositoryImpl): MeasurementRepository
    // ...
}
// :domain  →  interface AppDispatchers { val io: CoroutineDispatcher; val default: CoroutineDispatcher; val main: CoroutineDispatcher }
@Module @InstallIn(SingletonComponent::class) internal object DispatcherModule {
    @Provides @Singleton fun dispatchers(): AppDispatchers = object : AppDispatchers {
        override val io = Dispatchers.IO
        override val default = Dispatchers.Default
        override val main = Dispatchers.Main
    }
}
// Tests: TestAppDispatchers(StandardTestDispatcher()) en :core:testing. Sin calificadores, así que
// :domain, :presentation y :data comparten el mismo tipo sin duplicar anotaciones.
```

Las implementaciones de `:data` llevan `@Inject constructor`. `:data` puede depender de
`javax.inject`; la restricción solo aplica a `:domain`.

## 4. Modelo de errores (T-A001-06)

### ADR-A001-04 — `Result<T>` + `DomainError` en lugar de `Either`
- **Contexto**: hoy conviven `Either<String,…>`, `Either<FailureDomain,…>`, `Result` y
  excepciones. La skill pide `Result<T>` y errores de dominio explícitos.
- **Decisión**: `Result<T>` de Kotlin, con fallos **siempre** de tipo `DomainError` (una
  subclase de `Exception` sellada). La extensión `Result<T>.domainErrorOrNull()` hace el
  *smart-cast* en presentación.
- **Alternativas descartadas**:
  - Mantener `Either`: es propio del proyecto, no es estándar y la skill no lo recomienda.
  - Arrow: añade una dependencia grande para el tamaño del proyecto.
- **Riesgo**: `runCatching` captura `CancellationException`. **Regla**: está prohibido usar
  `runCatching` en código de producción. Se usa el helper `suspendRunCatching` de `:data`, que
  relanza la cancelación y mapea el resto con `HttpErrorMapper` o `StorageErrorMapper`.

```kotlin
// :domain
sealed class DomainError(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
    data object Network : DomainError()
    data object Unauthorized : DomainError()
    data object SessionExpired : DomainError()
    data object NotFound : DomainError()
    data class Conflict(val reason: String? = null) : DomainError()
    data class Validation(val details: List<FieldError>) : DomainError()
    data object Server : DomainError()
    data class Storage(override val cause: Throwable?) : DomainError()
    data class Unknown(override val cause: Throwable?) : DomainError()
}
data class FieldError(val field: String, val code: String)
```

```kotlin
// :data
internal class HttpErrorMapper @Inject constructor(private val json: Json) {
    fun map(throwable: Throwable): DomainError = when (throwable) {
        is DomainError -> throwable
        is HttpException -> fromResponse(throwable.code(), throwable.response()?.errorBody()?.string())
        is IOException -> DomainError.Network
        is SerializationException -> DomainError.Server
        else -> DomainError.Unknown(throwable)
    }
    // 400/422 → Validation(details del sobre de error), 401 → TOKEN_EXPIRED ? SessionExpired : Unauthorized,
    // 403/404 → NotFound, 409 → Conflict(code), 5xx → Server
}
```

## 5. Red y sesión (T-A001-08, T-A001-10)

### 5.1 Flujo de autenticación

```
Retrofit(@AuthenticatedApi) ─► OkHttp
                                ├─ AuthInterceptor: añade Bearer <access> si hay sesión
                                └─ TokenAuthenticator (401):
                                     ├─ si es la 2ª vez para esta petición → null (abandona)
                                     ├─ synchronized(lock): si otro hilo ya refrescó → reintenta con el token nuevo
                                     ├─ POST /v1/auth/refresh vía @PublicApi (sin authenticator → sin recursión)
                                     ├─ éxito → guarda tokens → reintenta
                                     └─ fallo → sessionStore.expire() → null
```

`SessionState.Expired` llega por `SessionRepository.observeSession()`. `MainActivity` (A-003) lo
observa y navega al login con un mensaje. Las mediciones pendientes **no** se pierden (A-002).

### 5.2 Almacenamiento cifrado

### ADR-A001-05 — DataStore + Keystore (AES-GCM) en lugar de EncryptedSharedPreferences
- **Contexto**: `androidx.security:security-crypto` (EncryptedSharedPreferences) está
  **deprecado**. Las `SharedPreferences` en claro incumplen C-PRIV-03.
- **Decisión**: `SessionLocalDataSource` sobre `DataStore<Preferences>`. Los valores se cifran con
  `TokenCipher` (AES/GCM/NoPadding, clave de 256 bits en `AndroidKeyStore` con alias
  `tensoscan_session_key`, IV aleatorio de 12 bytes antepuesto al texto cifrado y Base64).
- **Fallo de descifrado** (clave invalidada, por ejemplo al cambiar el bloqueo de pantalla en
  algunos OEM): se borra la sesión y se emite `Unauthenticated`. El usuario vuelve a iniciar
  sesión, que es el mal menor.
- **Migración**: un `DataMigration` de DataStore lee `auth_prefs/access_token`, lo cifra, lo
  guarda y borra la preferencia antigua.

### 5.3 Configuración de red

```xml
<!-- app/src/main/res/xml/network_security_config.xml (release) -->
<network-security-config><base-config cleartextTrafficPermitted="false"/></network-security-config>
<!-- app/src/debug/res/xml/network_security_config.xml -->
<network-security-config>
  <domain-config cleartextTrafficPermitted="true">
    <domain includeSubdomains="false">10.0.2.2</domain><domain>localhost</domain>
  </domain-config>
</network-security-config>
```
`API_BASE_URL`: en *debug* se lee de `local.properties` (`tensoscan.apiBaseUrl`, por defecto
`http://10.0.2.2:8000/`). En *release* es la URL HTTPS de producción (Q-A001-02).

Backup: `android:allowBackup="true"` se mantiene para las mediciones locales (útil para el
usuario), pero `data_extraction_rules.xml` y `backup_rules.xml` **excluyen** el fichero DataStore
de sesión. Las claves del Keystore no se respaldan nunca, así que un token cifrado restaurado sería
indescifrable de todos modos: la exclusión evita un estado incoherente.

## 5.4 Renombrado de paquetes (T-A001-13)

`com.example.{app→tensoscan,domain,data,ui}` → `com.tensoscan.*`. Pasos: `git mv` de los
directorios de fuentes, sustitución de `package` e `import`, `namespace`/`applicationId` en cada
`build.gradle.kts`, `android:name` del manifiesto, `FileProvider` authorities (se derivan de
`${applicationId}`), nombre del tema y reglas de ProGuard. Se hace **antes** de crear
`:presentation` para no mover dos veces. Verificación: `grep -r "com.example"` sin resultados.
La sesión guardada en el dispositivo se pierde al cambiar de `applicationId` (es otra app para el
sistema); como todavía no hay usuarios en producción, no hace falta migrarla.

## 6. Testing (T-A001-09)

- El plugin `de.mannodermaus.android-junit5` se aplica en `:data`, `:presentation` y `:ui`.
  `:domain` usa `useJUnitPlatform()`.
- `:core:testing` contiene `MainDispatcherExtension` (`Dispatchers.setMain(StandardTestDispatcher())`)
  y *builders* de fixtures del dominio (`aMeasurement()`, `aReading()`).
- Convenciones: las de la skill `android-testing` (`sut`, `expected`/`actual`, cuatro bloques y
  nombres con backticks).

### 6.1 Análisis estático
- detekt con `detekt-formatting` (ktlint) y reglas habilitadas para: `ForbiddenImport` (`org.koin`,
  `kotlinx.coroutines.GlobalScope`), `ForbiddenMethodCall` (`kotlin.runCatching` en el *source
  set* principal y `android.util.Log` fuera de un `Logger` dedicado), `UndocumentedPublicClass`
  desactivada, `MagicNumber` solo en `:domain` y `:presentation`.
- *Baseline* generado al final de T-A001-09 para el código legacy. A-003 lo va vaciando.

### 6.2 CI (`.github/workflows/android-ci.yml`)
```
on: pull_request, push (main/master)
jobs: build → setup-java 17 → gradle cache → verifyModuleBoundaries → detekt → testDebugUnitTest → assembleDebug
artefactos: informes de test y de detekt
```

## 7. Logging

`Logger` es una interfaz en `:domain`. Su implementación en `:data` usa `android.util.Log` en debug
y no registra nada en release (o envía a Crashlytics en el futuro). Está prohibido registrar
tokens, emails o valores de medición.

## 8. Riesgos

| Riesgo | Prob. | Impacto | Mitigación |
|--------|-------|---------|------------|
| La migración a Hilt rompe una pantalla en tiempo de ejecución | M | M | El grafo se verifica en compilación y se hace smoke test manual (§9) |
| R8 rompe la serialización en release | M | A | Reglas oficiales de kotlinx-serialization y Retrofit, más smoke test del APK release |
| Fallos del Keystore en algunos OEM | B | M | Fallback a cerrar sesión (§5.2) y log del error |
| Subir AGP o Kotlin rompe la compilación de Compose | M | B | Se sube en un PR aislado (T-A001-01) antes de cualquier otra cosa |

## 9. Smoke test manual (CA-A001-10)

1. Registro de un usuario nuevo → vuelve al login.
2. Login → historial con mediciones.
3. Borrar una medición.
4. Abrir el informe y descargar el PDF.
5. Usuario → datos visibles → cerrar sesión → login.
6. Chatbot → enviar un mensaje → respuesta.
7. Matar la app y reabrirla: la sesión se conserva.
8. Actualizar desde la versión de `master` con la sesión iniciada: la sesión se conserva (migración).
