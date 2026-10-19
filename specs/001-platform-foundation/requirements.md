# A-001 — Fundación de plataforma · Requisitos

> Estado: 📝 Pendiente de aprobación · Tipo: Refactor técnico · Depende de: 000
> Resuelve: AUD-A-01…03, 05, 06, 11, 12, 37…40, 48…54 (completos) y prepara el resto.

## 1. Contexto y motivación

La feature de captura en tiempo real (A-002) añade el subsistema más complejo de la app: cámara,
inferencia, concurrencia, persistencia offline y sincronización. Construirla sobre la base
actual obligaría a:

- escribirla con Koin y migrarla después a Hilt, que es lo que exige la skill;
- meter sus ViewModels en `:ui`, porque no existe `:presentation`;
- usar un toolchain alfa con KSP incompatible, lo que impide añadir Hilt y Room por KSP;
- convivir con tres modelos de error;
- hacerlo sin tests.

**Decisión de secuenciación**: primero se hace una fundación mínima y transversal, y después la
feature, que ya nace con la arquitectura final. La migración de las pantallas existentes al patrón
UDF completo **no** se hace aquí (va en A-003), para no bloquear la feature. Lo que sí se migra
aquí es la DI de *todo* el proyecto, porque Koin y Hilt no deben convivir (ver design ADR-A001-02).

## 2. Historias de usuario (técnicas)

- **HU-A001-1**: Como desarrollador, quiero un toolchain estable y coherente para que el proyecto
  compile de forma reproducible en local y en CI.
- **HU-A001-2**: Como desarrollador, quiero que las capas sean módulos Gradle con dependencias
  que el compilador haga cumplir, para que una violación de capa no compile.
- **HU-A001-3**: Como desarrollador, quiero inyección con Hilt verificada en compilación para
  detectar dependencias rotas antes de ejecutar.
- **HU-A001-4**: Como desarrollador, quiero un modelo de error único y tipado para que la UI pueda
  mostrar mensajes precisos.
- **HU-A001-5**: Como usuario, quiero que mi sesión y mis datos viajen y se guarden de forma
  segura.
- **HU-A001-6**: Como desarrollador, quiero infraestructura de tests y análisis estático en CI para
  refactorizar con seguridad.

## 3. Requisitos (EARS)

### Build y toolchain
- **REQ-A001-01**: El proyecto DEBERÁ usar versiones **estables** de AGP, Kotlin, KSP (alineada
  con Kotlin), Compose BOM, CameraX, Navigation, Lifecycle, Room, Hilt, Retrofit, OkHttp y
  kotlinx-serialization, todas declaradas en `gradle/libs.versions.toml`.
- **REQ-A001-02**: El proyecto NO DEBERÁ usar `kapt`. Todo procesamiento de anotaciones (Room y
  Hilt) DEBERÁ hacerse con KSP.
- **REQ-A001-03**: El proyecto DEBERÁ compilar con JDK 17 y `jvmTarget = 17`.
- **REQ-A001-04**: El proyecto DEBERÁ tener un único stack HTTP (Retrofit + OkHttp) y un único
  serializador (kotlinx-serialization). Ktor y Gson DEBERÁN eliminarse.
- **REQ-A001-05**: La navegación DEBERÁ usar `androidx.navigation:navigation-compose` con rutas
  tipadas (`@Serializable`) y la ViewModel API de AndroidX (`hiltViewModel()`), no los artefactos
  multiplataforma de JetBrains.
- **REQ-A001-06**: El build *release* DEBERÁ tener `isMinifyEnabled = true` y
  `isShrinkResources = true`, con reglas R8 para kotlinx-serialization, Retrofit, LiteRT y ML Kit.

### Arquitectura y módulos
- **REQ-A001-07**: El proyecto DEBERÁ tener los módulos `:domain` (JVM), `:data`,
  `:presentation`, `:ui` y `:app`, con las dependencias de la tabla C-ARCH-01 de la constitución.
- **REQ-A001-08**: `:domain` NO DEBERÁ depender de ningún framework de DI ni de Android.
- **REQ-A001-09**: `:data` NO DEBERÁ depender de Compose, Material ni CameraX-view.
- **REQ-A001-10**: CUANDO un módulo intente depender de otro no permitido, el build DEBERÁ fallar
  (verificación automática en CI con una tarea Gradle o una regla de detekt).

### Inyección de dependencias
- **REQ-A001-11**: La app DEBERÁ usar Hilt (`@HiltAndroidApp`, `@AndroidEntryPoint`,
  `@HiltViewModel`) con inyección por constructor y `@Binds` para interfaces.
- **REQ-A001-12**: Los módulos Hilt DEBERÁN vivir en `app/src/main/.../di/`, separados por
  responsabilidad: `NetworkModule`, `DatabaseModule`, `DataStoreModule`, `RepositoryModule`,
  `UseCaseModule`, `DispatcherModule` y `DetectorModule` (este último en A-002).
- **REQ-A001-13**: Koin DEBERÁ eliminarse por completo: dependencias, módulos e imports.

### Errores y concurrencia
- **REQ-A001-14**: El dominio DEBERÁ definir una jerarquía `sealed class DomainError :
  Exception()` con al menos `Network`, `Unauthorized`, `SessionExpired`, `NotFound`,
  `Conflict`, `Validation(details)`, `Server`, `Storage` y `Unknown`.
- **REQ-A001-15**: Las operaciones que pueden fallar DEBERÁN devolver `Result<T>` cuyo fallo es un
  `DomainError`. `Either` DEBERÁ eliminarse al terminar A-003; mientras tanto el código nuevo NO
  DEBERÁ usarlo.
- **REQ-A001-16**: `:data` DEBERÁ traducir las respuestas HTTP con un único `HttpErrorMapper`:
  400 y 422 → `Validation`, 401 → `Unauthorized` o `SessionExpired` según
  `error.code`, 404 → `NotFound`, 409 → `Conflict`, 5xx → `Server`, `IOException` → `Network`.
- **REQ-A001-17**: Los dispatchers DEBERÁN inyectarse con los calificadores `@IoDispatcher` y
  `@DefaultDispatcher`.

### Red y sesión
- **REQ-A001-18**: La URL base DEBERÁ venir de `BuildConfig.API_BASE_URL`, configurable por
  *build type* (debug → emulador o `local.properties`; release → HTTPS de producción).
- **REQ-A001-19**: Un `AuthInterceptor` DEBERÁ añadir `Authorization: Bearer` a las peticiones
  autenticadas. Los servicios Retrofit NO DEBERÁN recibir la cabecera como parámetro.
- **REQ-A001-20**: CUANDO el servidor responda `401 TOKEN_EXPIRED`, un `TokenAuthenticator`
  DEBERÁ intentar **una única vez** `POST /v1/auth/refresh` (serializado entre peticiones
  concurrentes) y reintentar la petición original. SI el refresh falla, ENTONCES la sesión DEBERÁ
  invalidarse y emitirse `SessionState.Expired`.
  *Nota*: el endpoint de refresh llega con B-001. Hasta entonces el authenticator invalida la
  sesión directamente (es un comportamiento tras flag).
- **REQ-A001-21**: Los tokens DEBERÁN guardarse en DataStore cifrados con una clave AES-GCM del
  Android Keystore. CUANDO exista un token en las `SharedPreferences` legacy (`auth_prefs`), la app
  DEBERÁ migrarlo una vez y borrar el original.
- **REQ-A001-22**: Los datos de sesión DEBERÁN excluirse del backup en la nube y de la
  transferencia entre dispositivos (`dataExtractionRules` y `fullBackupContent`).
- **REQ-A001-23**: El tráfico en claro DEBERÁ estar prohibido en *release*. Una
  `network_security_config` de *debug* PODRÁ permitir `10.0.2.2` y `localhost`.
- **REQ-A001-24**: El dominio DEBERÁ exponer `SessionRepository.observeSession(): Flow<SessionState>`
  (`Authenticated`, `Unauthenticated` o `Expired`) como única fuente de verdad de la sesión.

### Calidad
- **REQ-A001-25**: Todos los módulos DEBERÁN ejecutar tests con JUnit 5. Los módulos Android lo
  harán con el plugin `de.mannodermaus.android-junit5`. Las dependencias de test serán MockK,
  Turbine y `kotlinx-coroutines-test`.
- **REQ-A001-26**: DEBERÁ existir una extensión JUnit 5 `MainDispatcherExtension` reutilizable
  (en un módulo `:core:testing` o en `testFixtures`).
- **REQ-A001-27**: detekt (con reglas de formateo) DEBERÁ ejecutarse en CI y fallar ante una
  violación nueva. Se permite un *baseline* para el código legacy pendiente de A-003.
- **REQ-A001-28**: DEBERÁ existir un workflow de GitHub Actions que, en cada PR, ejecute
  `detekt`, `testDebugUnitTest` y `assembleDebug`. `.github` DEBERÁ dejar de estar en
  `.gitignore`.
- **REQ-A001-29**: El comportamiento visible de las features existentes (login, registro,
  historial, usuario y chatbot) NO DEBERÁ cambiar en esta spec, salvo las correcciones de bugs
  enumeradas (mapeo de 400 y cabecera `Bearer null`).

## 4. Criterios de aceptación

| ID | Dado / Cuando / Entonces | Verifica |
|----|--------------------------|----------|
| CA-A001-01 | Dado un checkout limpio, cuando se ejecuta `./gradlew assembleDebug testDebugUnitTest detekt` con JDK 17, entonces termina en verde sin warnings de versiones alfa | REQ-01…04, 25, 27 |
| CA-A001-02 | Dado `:domain`, cuando se inspecciona su `build.gradle.kts`, entonces solo depende de `kotlinx-coroutines-core` (y de dependencias de test) | REQ-08 |
| CA-A001-03 | Dado que alguien añade `implementation(project(":data"))` en `:presentation`, cuando se ejecuta CI, entonces el build falla | REQ-10 |
| CA-A001-04 | Dado el código fuente, cuando se busca `koin` o `kapt`, entonces no hay resultados | REQ-02, 13 |
| CA-A001-05 | Dado un servidor que responde 400, cuando el repositorio mapea el error, entonces devuelve `DomainError.Validation` (no `UnknownHostError`) | REQ-16 |
| CA-A001-06 | Dado un usuario con sesión iniciada en la versión anterior, cuando actualiza la app, entonces sigue con la sesión iniciada y `auth_prefs` ya no contiene el token | REQ-21 |
| CA-A001-07 | Dado un APK *release*, cuando intenta una petición `http://`, entonces el sistema la bloquea | REQ-23 |
| CA-A001-08 | Dado un backup en la nube, cuando se restaura en otro dispositivo, entonces no se restaura el token | REQ-22 |
| CA-A001-09 | Dado un 401 con `TOKEN_EXPIRED` y un refresh válido, cuando dos peticiones fallan a la vez, entonces solo se hace **un** refresh y ambas se reintentan | REQ-20 |
| CA-A001-10 | Dado el login, el registro, el historial, el usuario y el chatbot, cuando se ejecuta el smoke test manual (`design.md` §9), entonces se comportan igual que en `master` | REQ-29 |

## 5. Requisitos no funcionales

- **RNF-A001-01**: El build incremental de un cambio en `:presentation` no DEBERÍA superar el
  tiempo actual (se mide antes y después y se anota en el PR).
- **RNF-A001-02**: El APK *release* DEBERÍA reducir su tamaño respecto al actual gracias a R8 y
  a la eliminación de Ktor y Gson.

## 6. Fuera de alcance

- Migrar las pantallas existentes a `UiState`/`UiAction`/`UiEvent` (va en A-003).
- Cambiar los endpoints a `/v1` en las features legacy (va en A-003; A-002 usa `/v1` desde el
  principio).
- Rediseño visual.

## 7. Preguntas abiertas

| ID | Pregunta | Propuesta por defecto |
|----|----------|------------------------|
| Q-A001-01 | ¿Cambiamos `applicationId`/namespace de `com.example.tensoscan` a uno propio (por ejemplo `dev.enriquepalma.tensoscan`)? Google Play no admite `com.example`. Cambiarlo más tarde implica una app nueva en Play | Cambiarlo ahora (T-A001-13). **Necesita el nombre definitivo** |
| Q-A001-02 | ¿Cuál es la URL del backend de producción (HTTPS)? | `BuildConfig` con un placeholder hasta tenerla |
| Q-A001-03 | ¿Mantenemos minSdk 28? | Sí: cubre ~95 % de dispositivos activos y LiteRT y ML Kit lo soportan |
