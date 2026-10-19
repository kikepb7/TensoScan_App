# 000 — Auditoría del estado actual (App Android)

> Estado: ✅ Redactada · Rama base analizada: `master` @ `662895e`
> Objetivo: inventariar **todo** lo que debe cambiar y por qué, y enlazar cada hallazgo con la
> spec o tarea que lo resuelve. Las rutas son relativas a la raíz del repositorio.

Severidad: 🔴 Crítico (bug funcional, seguridad o datos de salud) · 🟠 Alto (bloquea la feature o
viola la constitución) · 🟡 Medio (mantenibilidad o UX) · ⚪ Bajo (estilo o limpieza).

## 1. Resumen ejecutivo

1. **El flujo de reconocimiento actual no sirve para tiempo real.** La cámara solo hace fotos y
   las guarda en DCIM (`CameraViewModel.kt:35`). El reconocimiento se lanza desde *otra pantalla*,
   eligiendo la imagen de la galería y subiéndola con un timeout de 7 s
   (`SummaryViewModel.kt:103-140`). El resultado se guarda en el servidor **sin que el usuario lo
   confirme**, lo que incumple C-SAFE-01.
2. **La arquitectura no cumple las skills.** Se usa Koin en lugar de Hilt, `:domain` depende de
   Koin, no existe la capa `presentation` (los ViewModels están en `:ui`), conviven tres modelos de
   error (`Either<String,…>`, `Result` y excepciones), hay ViewModels que reciben `Context`,
   `Activity` o `LifecycleCameraController`, y no hay ningún test real.
3. **Hay problemas de seguridad y privacidad.** El token está en `SharedPreferences` en claro con
   `allowBackup=true`, se permite tráfico en claro, se pide `RECORD_AUDIO` sin necesidad y las
   fotos del tensiómetro acaban en la galería pública.
4. **El toolchain es inestable.** AGP `8.9.0-alpha07`, CameraX `1.5.0-alpha03`, KSP `2.0.20`
   frente a Kotlin `2.1.10` (incompatibles), artefactos multiplataforma de JetBrains para
   Navigation y ViewModel en una app solo Android, dos stacks HTTP (Ktor sin usar y Retrofit) y dos
   serializadores (Gson y kotlinx).

## 2. Hallazgos

### 2.1 Arquitectura y DI

| ID | Sev. | Ubicación | Hallazgo | Impacto | Resuelve |
|----|------|-----------|----------|---------|----------|
| AUD-A-01 | 🟠 | `*/build.gradle.kts`, `app/.../di/DiConfigurator.kt`, `*/di/*Module.kt` | Se usa Koin; la skill exige Hilt con inyección por constructor | Incumple C-ARCH-06; sin validación del grafo en compilación | A-001 · T-A001-04/05 |
| AUD-A-02 | 🟠 | `domain/build.gradle.kts:27`, `domain/.../di/DomainModule.kt` | `:domain` depende de Koin y contiene un módulo de DI | Rompe C-ARCH-02 (dominio puro) | T-A001-05 |
| AUD-A-03 | 🟠 | `ui/.../feature/*/*ViewModel.kt` | No existe el módulo `presentation`; los ViewModels y sus estados viven en `:ui` | Incumple C-ARCH-01 | T-A001-03 |
| AUD-A-04 | 🟠 | `ui/.../camera/CameraViewModel.kt:34`, `PermissionViewModel.kt:19-26`, `SummaryViewModel.kt:103,146` | Los ViewModels reciben `LifecycleCameraController`, `Activity`, `Context` o `Uri` y hacen E/S de ficheros | Fugas de memoria, no se pueden testear y se mezclan capas | A-002 (cámara) · A-003 (Summary) |
| AUD-A-05 | 🟡 | `data/.../common/Extensions.kt`, `data/.../measurements/utils/MeasurementMapper.kt` | Mappers duplicados (`dtoToMeasurementModel` y `toDomain`) dentro de un `Extensions.kt` cajón de sastre | Ambigüedad y divergencia | T-A001-07 |
| AUD-A-06 | 🟡 | `domain/.../camera/model/FailureDomain.kt` | El error global vive en el paquete `camera` y lo usan todas las features | Acoplamiento accidental | T-A001-06 |
| AUD-A-07 | 🟡 | `ui/.../utils/`, `ui/.../common/extensions/` | Carpetas `utils` y `extensions` como cajón de sastre (permisos, PDF, colores, ficheros) | Incumple C-ARCH-04 | A-003 |
| AUD-A-08 | ⚪ | `*.compose.kt` | Los nombres de fichero no coinciden con la clase y los composables se llaman `*View`/`*ScreenView` | Incumple las convenciones de nombre | A-003 |
| AUD-A-09 | ⚪ | Todo el proyecto | Visibilidad `public` en todo | Incumple C-ARCH-05 | Transversal |
| AUD-A-10 | 🟡 | `app/build.gradle.kts:10,14` | `namespace` y `applicationId` `com.example.tensoscan`: Google Play rechaza `com.example` | No se puede publicar | Pregunta abierta A-001 |

### 2.2 Patrones, estado y errores

| ID | Sev. | Ubicación | Hallazgo | Impacto | Resuelve |
|----|------|-----------|----------|---------|----------|
| AUD-A-11 | 🟠 | `domain/.../common/Either.kt`, `LoginRepository.kt:9-11`, `CameraRepository.kt` | Tres modelos de error: `Either<String,…>`, `Either<FailureDomain,…>`, `Result<Unit>` y excepciones lanzadas (`login`, `getCurrentUser`) | Errores sin tipar y estados de UI imprecisos | T-A001-06 · A-003 |
| AUD-A-12 | 🔴 | `data/.../common/Extensions.kt:16` | HTTP 400 se traduce a `UnknownHostError` | El usuario recibe un mensaje falso | T-A001-06 |
| AUD-A-13 | 🔴 | `data/.../MeasurementRemoteDataSource.kt:16-17` | La llamada Retrofit está **fuera** del `try`; además `errorBody().toString()` (líneas 26 y 34) no lee el cuerpo | Sin red, la excepción se escapa y se pierde el error | A-002 · T-A002-28 |
| AUD-A-14 | 🟠 | `data/.../MeasurementsRepositoryImpl.kt:22-47` | Se capturan excepciones dentro de `flow {}` envolviendo otro flow, y si falla el fetch se reemite el dato local en silencio | Error invisible y violación de la transparencia de excepciones | A-002 · T-A002-26 · T-A002-30 |
| AUD-A-15 | 🟡 | `ui/.../login/LoginScreen.compose.kt:60-66` | Se navega al historial con una heurística (`!isLoading && error == null && email.isNotBlank()`) en lugar de con un evento de éxito | Navegación frágil | A-003 · T-A003-02 |
| AUD-A-16 | 🟡 | `ui/.../summary/SummaryScreen.compose.kt:147`, `camera/CameraScreen.compose.kt:137` | `CoroutineScope(Dispatchers.Main).launch` dentro de composables | Fugas y trabajo huérfano | A-002 · A-003 |
| AUD-A-17 | 🟡 | `ui/.../summary/SummaryScreen.compose.kt:101-104`, `NavigationWrapper.kt:56` | El HTML completo del informe se pasa como **argumento de navegación** codificado en la URI | Límite de tamaño del back stack (`TransactionTooLargeException`) | A-003 · T-A003-05 |
| AUD-A-18 | 🟡 | `ui/.../summary/SummaryScreen.compose.kt:95`, `NavigationWrapper.kt:61-72` | La pantalla de historial recibe una lista `PredictionUiModel` vía JSON en la ruta, que nunca se rellena | Código muerto y confuso | A-003 |
| AUD-A-19 | 🟡 | `ui/.../summary/SummaryViewModel.kt:154-160` | Estado con flags y nulables (`errorMessage`, `pdfData: ByteArray?` en un data class sin `equals`) | Re-renders y eventos repetidos | A-003 |
| AUD-A-20 | 🟡 | `ui/.../user/UserViewModel.kt:23,39` | Carga en `init` y el error solo se registra con `Log.e`; la UI no se entera | Pantalla vacía sin explicación | A-003 |
| AUD-A-21 | 🟡 | `ui/.../register/RegisterViewModel.kt:27,32` | La acción `Submit` no hace nada (`it.copy()`) y `registerUser()` es pública fuera de `onAction` | Incumple C-PAT-02 | A-003 |
| AUD-A-22 | ⚪ | `domain/.../chatbot/model/ChatbotModel.kt:5`, `LoginRepository.kt:10` | Erratas en API pública: `conntent`, `registerUSer` | Legibilidad | A-003 |

### 2.3 Cámara y reconocimiento

| ID | Sev. | Ubicación | Hallazgo | Impacto | Resuelve |
|----|------|-----------|----------|---------|----------|
| AUD-A-23 | 🟠 | `ui/.../camera/CameraScreen.compose.kt:68` | `IMAGE_CAPTURE or VIDEO_CAPTURE`: no hay `IMAGE_ANALYSIS` | No hay tiempo real | A-002 |
| AUD-A-24 | 🔴 | `ui/.../camera/CameraViewModel.kt:52-61`, `data/.../CameraRepositoryImpl.kt:32-47` | Cada foto se guarda en `DCIM` (galería pública) | Exposición de datos de salud (C-PRIV-02) | A-002 · T-A002-01 |
| AUD-A-25 | 🟡 | `ui/.../camera/CameraScreen.compose.kt:104-112` | El botón «galería» lanza `ACTION_PICK` sin manejar el resultado | Botón que no hace nada | A-002 (se elimina) |
| AUD-A-26 | 🟡 | `ui/.../camera/CameraScreen.compose.kt:118-125`, `CameraViewModel.kt:70-73` | Botón de vídeo sin implementar que pide permiso de micrófono | Funcionalidad falsa y permiso innecesario | A-002 (se elimina) |
| AUD-A-27 | 🟡 | `ui/.../camera/CameraScreen.compose.kt:145-153` | Permite cambiar a la cámara frontal | Sin sentido para leer un display | A-002 (se elimina) |
| AUD-A-28 | 🟡 | `ui/.../camera/CameraScreen.compose.kt:72-74,127-139` | La foto se dispara aunque el permiso se esté pidiendo en ese momento; el «flash» negro es un `delay(5)` | Condición de carrera | A-002 |
| AUD-A-29 | 🟡 | `ui/.../common/components/CameraUserAssistantComponent.compose.kt:30-31` | Recuadro guía con tamaño fijo en **píxeles** (390×490 px) | Recuadro distinto según la densidad de pantalla; no se puede mapear a la imagen | A-002 · T-A002-16 |
| AUD-A-30 | ⚪ | `ui/.../common/extensions/Extensions.kt:41` | JPEG calidad 100 en caché | Ficheros enormes | A-002 (se elimina) |
| AUD-A-31 | ⚪ | `gradle/libs.versions.toml` (`tensorflowLite`) | TFLite declarado y no usado | Ruido | A-002 · T-A002-33 |

### 2.4 Datos, persistencia y red

| ID | Sev. | Ubicación | Hallazgo | Impacto | Resuelve |
|----|------|-----------|----------|---------|----------|
| AUD-A-32 | 🟠 | `domain/.../measurements/model/MeasurementModel.kt` | Los valores de presión, pulso, confianza y fecha son `String` | No se puede validar, ordenar ni graficar con fiabilidad | A-002 · T-A002-02 |
| AUD-A-33 | 🟠 | `data/.../dao/MeasurementDao.kt:11` | `ORDER BY timestamp` sobre un `String` | El orden depende del formato | A-002 · T-A002-24 |
| AUD-A-34 | 🟡 | `data/.../dao/MeasurementDao.kt:15,24` | `findMeasurementById(Int)` con PK `String` | Consulta que nunca coincide | A-002 · T-A002-24 |
| AUD-A-35 | 🟡 | `data/.../dao/MeasurementDao.kt` | DAO no reactivo (sin `Flow`) y `clear + insert` sin transacción | Parpadeos y pérdida de datos si falla a mitad | A-002 · T-A002-24 |
| AUD-A-36 | 🟠 | `data/.../di/DataModule.kt:52-54` | `fallbackToDestructiveMigration(false)` y BD v1 sin esquema exportado | Hoy es una caché; con datos pendientes de sincronizar, una migración destructiva **perdería mediciones** | A-002 · T-A002-24 |
| AUD-A-37 | 🟠 | `data/.../di/DataModule.kt:43-44` | `baseUrl` fija a `10.0.2.2` (emulador) | No funciona en un dispositivo ni en *release* | T-A001-08 |
| AUD-A-38 | 🟡 | `data/.../*DataSource.kt`, `*Service.kt` | Cabecera `Authorization` construida a mano en cada llamada (también con `"Bearer null"`) | Duplicación; sin gestión de 401 ni refresh | T-A001-08 |
| AUD-A-39 | 🔴 | `data/.../local/TokenManager.kt:8-9`, `AndroidManifest.xml:17` | JWT en `SharedPreferences` en claro con `allowBackup="true"` y reglas de backup vacías | El token se copia en los backups (C-PRIV-03) | T-A001-10 |
| AUD-A-40 | 🔴 | `app/src/main/AndroidManifest.xml:18` | `usesCleartextTraffic="true"` global | Credenciales y datos de salud en claro (C-PRIV-04) | T-A001-08 |
| AUD-A-41 | 🟡 | `app/src/main/AndroidManifest.xml:12`, `data/src/main/AndroidManifest.xml:7`, `ui/.../utils/Constants.kt:8` | Permiso `RECORD_AUDIO` | Permiso innecesario (C-PRIV-05) | T-A002-01 |
| AUD-A-42 | 🟡 | `data/.../LoginRepositoryImpl.kt:38` | `throw Exception("Token not found")` | Error genérico | A-003 |

### 2.5 UI, contenido y producto

| ID | Sev. | Ubicación | Hallazgo | Impacto | Resuelve |
|----|------|-----------|----------|---------|----------|
| AUD-A-43 | 🔴 | `ui/.../common/extensions/Extensions.kt:19-30` | Clasificación basada **solo en la sistólica**, con etiquetas «Peligro»/«Alerta» y sin guía clínica | Información sanitaria incorrecta o alarmista (C-SAFE-02) | A-003 · T-A003-07 |
| AUD-A-44 | 🟡 | `BottomBarItem.kt:23-63`, `CameraUserAssistantComponent.compose.kt:65`, `Extensions.kt:27-29,71` | Textos y `contentDescription` literales; el icono de cámara se describe como «User icon» | i18n y accesibilidad (C-A11Y-01) | A-003 |
| AUD-A-45 | 🟡 | `NavigationWrapper.kt:32` | Siempre arranca en Login aunque haya sesión | UX | A-003 · T-A003-01 |
| AUD-A-46 | 🟡 | `common/components/SummaryMeasurementsWebView.compose.kt:15` | WebView con JS activado que carga HTML del servidor; ese HTML incluye datos sin escapar (ver backend AUD-B-21) | Riesgo de XSS | A-003 · T-A003-05 |
| AUD-A-47 | ⚪ | `SummaryCardListItemComponent.compose.kt`, `PredictionUiModel.kt` | Se muestra la «confianza» del modelo al usuario final | Dato técnico sin valor para el usuario | A-003 |

### 2.6 Build, toolchain y calidad

| ID | Sev. | Ubicación | Hallazgo | Impacto | Resuelve |
|----|------|-----------|----------|---------|----------|
| AUD-A-48 | 🟠 | `gradle/libs.versions.toml` | AGP `8.9.0-alpha07`, CameraX `1.5.0-alpha03`, Navigation `2.8.0-alpha10`, Koin VM `1.2.0-Beta4`, Coil `3.0.0-alpha08` | Builds inestables | T-A001-01 |
| AUD-A-49 | 🟠 | `gradle/libs.versions.toml` (`ksp = 2.0.20`, `kotlin = 2.1.10`) | Versión de KSP incompatible con Kotlin; Room se procesa con **kapt** | Build lento y bloquea Hilt + KSP | T-A001-01/02 |
| AUD-A-50 | 🟡 | `gradle/libs.versions.toml`, `data/build.gradle.kts:67-75` | Ktor (sin usar) junto a Retrofit; Gson junto a kotlinx.serialization; `org.jetbrains.androidx.navigation` y `lifecycle-viewmodel-compose` multiplataforma | Peso, confusión y APIs alfa | T-A001-01 |
| AUD-A-51 | 🟡 | `data/build.gradle.kts:39-54` | `:data` depende de Compose, Material3, activity-compose y CameraX | Fuga de capas | T-A001-03 |
| AUD-A-52 | 🟠 | `*/src/test/.../ExampleUnitTest.kt` | No hay tests reales; solo JUnit 4 | Sin red de seguridad para refactorizar | T-A001-09 |
| AUD-A-53 | 🟡 | `.gitignore:16` | `.github` ignorado | No se puede versionar CI ni plantillas de PR | T-A001-09 |
| AUD-A-54 | ⚪ | `*/build.gradle.kts` | Java 11, `isMinifyEnabled=false` en *release*, sin detekt/ktlint | Calidad y tamaño de APK | T-A001-01/09 |
| AUD-A-55 | ⚪ | `README.md` | Dice «Custom OCR model … on-device», lo que hoy es falso | Documentación engañosa | T-A002-40 |

## 3. Lo que se conserva

- La separación en módulos Gradle `domain`/`data`/`ui`/`app` como punto de partida (se añade
  `presentation`).
- Room como persistencia local, Retrofit como cliente HTTP, Compose + Material 3 y el tema actual
  (`ui/theme`).
- Las pantallas y features de negocio: login, registro, historial, usuario y chatbot (se migran en A-003).
- `CameraUserAssistantView` como punto de partida visual del recuadro guía (se rehace en dp y con
  coordenadas normalizadas).

## 4. Trazabilidad

Cada hallazgo `AUD-A-xx` aparece en la columna «Resuelve» de su spec. Al cerrar una spec, su
`tasks.md` enumera los hallazgos que quedan resueltos.
