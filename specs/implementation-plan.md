# Plan de implementación (App + Backend)

> Estado: 📝 Pendiente de aprobación · Fecha: 2026-10-19
> Decisión del responsable: **primero se completa todo el código; después se pasa a pruebas
> con datos reales y a entrenar el modelo.**

## 1. Decisiones registradas

| Tema | Decisión |
|------|----------|
| `applicationId` / paquete | `com.tensoscan` (renombrado antes de crear código nuevo; T-A001-13) |
| Modelo de negocio | Comercial → solo dependencias de licencia permisiva; se descarta Ultralytics/YOLO (AGPL) |
| Zona horaria | `Europe/Madrid`: migración de fechas legacy y zona por defecto del informe PDF |
| Datos reales | Fotos de 1 tensiómetro con varios valores; se aportan en la etapa 2. Un solo dispositivo → el modelo se publica en canal `beta`; `production` exige ≥ 3 dispositivos (B-003 §1) |
| Reglas de lectura (contrato 1.1.0) | Se aprueba la corrección F1 del parser (permiso para el hueco previo a un «1», con `roi_aspect`); F2: `GROUP_MAX_OVERLAP_FACTOR` 0,30 → 0,45; F3: `dash`/`colon` con altura mínima propia y «midiendo» por recuento de guiones. Se mantienen las cajas ajustadas de la guía de CVAT. Detalle en `contracts/reading-rules.md` §6 |
| Cumplimiento | Datos de salud (RGPD art. 9), supresión de cuenta, consentimiento, valoración regulatoria antes de publicar (constitución C-LEGAL) |

## 2. Cómo se verifica cada parte (limitaciones reales del entorno)

| Parte | Dónde se verifica | Detalle |
|-------|-------------------|---------|
| `:domain` (Kotlin JVM puro) | **Aquí**, con Gradle + JUnit 5 | Parser, validador, estabilizador, sesión, casos de uso |
| Backend (`app/`) y `ml/` | **Aquí**, con `uv` + `pytest` + `ruff` + `mypy` | Los tests de integración necesitan un MongoDB real: si no hay `mongod` en el entorno, se ejecutan en CI con un contenedor de servicio |
| `:data`, `:presentation`, `:ui`, `:app` (Android) | **GitHub Actions** | El entorno no tiene Android SDK (`dl.google.com` está bloqueado). CI compila, ejecuta los tests unitarios y sube el APK *debug* como artefacto; yo leo los logs de CI y corrijo |
| Cámara, rendimiento, UX | **Tu móvil** | Instalas el APK de CI. Puntos de control marcados con 🔎 |
| Entrenamiento del modelo | **Colab / Kaggle** (GPU) | Este entorno tiene 4 CPU y sin GPU: solo hace entrenamientos de humo |

Consecuencia: ninguna tarea Android se marca `[x]` hasta que su CI esté en verde.

## 3. Política de ramas y commits

- Todo el trabajo va en `recognition-refactor-feature` de cada repo (sin ramas intermedias).
- **Un commit por tarea** (`T-A001-03 | Create presentation module...`), `git push` al cerrar cada hito.
- No se abre ningún PR hasta que lo pidas.
- Si una tarea obliga a cambiar una spec, se cambia la spec primero y va en el mismo commit.

## 4. ETAPA 1 — Todo el código

> Orden pensado para que lo más arriesgado (cámara + reconocimiento en vivo) se pueda ver en tu
> móvil lo antes posible y para que el backend llegue cuando la app ya lo necesita.

| Hito | Contenido | Repo | Tareas | Verificación | Te necesito |
|------|-----------|------|--------|--------------|-------------|
| **H0** | CI mínimo (compila) en los dos repos | App, Backend | Parte de T-A001-09 y T-B001-11 | Primer push en verde | — |
| **H1** | **Fundación de la app**: toolchain estable, `com.tensoscan`, KSP, tests+detekt+CI, módulo `:presentation`, Hilt (adiós Koin), errores tipados, red/sesión cifrada, rutas tipadas, R8 | App | T-A001-09(wf) → 01 → 13 → 02 → 09 → 03 → 11 → 04 → 05 → 06 → 07 → 08 → 10 → 12 → 14 | CI verde; smoke test (design A-001 §9) | — |
| **H2** | **Núcleo de reconocimiento en el dominio** (Kotlin puro): modelos, validador, agrupador, parser, estabilizador, exposición, sesión, casos de uso | App | T-A002-01…11 | **Aquí**: tests con los casos dorados | — |
| **H3** | **Escaneo en vivo con ML Kit**: detector, ViewModel, geometría del recuadro, CameraX, pantalla y overlay, navegación | App | T-A002-12…19 | CI verde + APK en artefacto | 🔎 **CP1**: pruebas el escaneo en tu móvil |
| **H4** | **Revisión, edición y guardado local** (offline-first): validación en vivo, avisos, Room v2 + migración | App | T-A002-20…26 | CI verde + `androidTest` en CI si hay emulador | 🔎 **CP2**: flujo completo escaneo → revisión → guardado |
| **H5** | **Fundación del backend**: settings, factory, errores, capas, PyJWT, Argon2, refresh tokens, rate limit, asistente, alias legacy, supresión de cuenta, Docker, CI | Backend | T-B001-01 → 02 → 03 → 04 → 05 → 06 → 07 → 08 → 09 → 12 → 14 → 10 → 11 → 13 | **Aquí**: ruff + mypy + pytest | — |
| **H6** | **API de mediciones v1**: POST idempotente, listado paginado, borrado, informe PDF, migración m001 (Madrid), convivencia legacy | Backend | T-B002-01…08 | **Aquí** + CI con Mongo | — |
| **H7** | **Sincronización en la app**: API v1, WorkManager, fusión, historial con estados, borrado y logout con avisos | App | T-A002-27…32 | CI + pruebas contra el backend local (MockWebServer y backend real en Docker) | 🔎 **CP3**: decidir dónde se despliega el backend (Q-B001-01) |
| **H8** | **Tooling de ML completo** (sin entrenar): paquete `ml/`, parser Python, generador sintético, ingesta (fotos + `labels.csv`), split, evaluación, entrenamiento de 2 etapas, exportación, `verify`, `release`, modelo *dummy*, cuaderno de Colab | Backend | T-B003-01…05, 06, 07, 09, 10b, 10, 11, 12, 13 (+ `ml-tests` en CI) | **Aquí**: pytest; entrenamiento de humo en CPU | — |
| **H9** | **LiteRT en la app contra el modelo *dummy***: carga y verificación sha256, preprocesado, decodificación, NMS y latencia | App | T-A002-33, 34 (+ T-A002-36 en emulador/CI) | CI verde | — |
| **H10** | **Migración de las demás pantallas** a UDF: arranque con sesión, login, registro (+ consentimiento), historial con gráfico nativo y PDF, clasificador, perfil (+ eliminar cuenta), asistente, barridos de textos/nombres y limpieza | App | T-A003-01…13 | CI verde | — |
| **H11** | **Pase de accesibilidad y documentación** | App | T-A002-37, 40 | TalkBack manual | 🔎 Pruebas rápidas con TalkBack |

**Fin de la etapa 1**: todo el código escrito, compilando y con tests unitarios en verde; la app
funciona de extremo a extremo con ML Kit; el pipeline de ML funciona de extremo a extremo con datos
sintéticos y modelo *dummy*.

## 5. ETAPA 2 — Pruebas con datos reales y entrenamiento

| Paso | Qué ocurre | Quién |
|------|------------|-------|
| **E1** | Aportas las fotos + los valores de cada una (SYS/DIA/PUL). Preparo el `labels.csv`, ejecuto `ingest --preview` y validamos el encuadre juntos (`--roi-mode`) | Tú + yo |
| **E2** | **Línea base de ML Kit**: la app (modo debug) vuelca `detections.jsonl` de tus fotos; `eval` mide el acierto real del detector provisional con la misma métrica que el modelo propio | Yo (tú pasas la app por las fotos) |
| **E3** | Genero el dataset sintético (30.000 + 3.000 imágenes) y reviso contigo la galería de muestras | Yo |
| **E4** | *Spike* de arquitectura (YOLOX-Nano vs NanoDet-Plus) en Colab/Kaggle. Decisión registrada en el ADR | Tú lanzas el cuaderno; yo analizo resultados |
| **E5** | Entrenamiento en 2 etapas: preentrenamiento sintético → *fine-tuning* con tus fotos (pre-etiquetadas, con correcciones tuyas en CVAT) | Tú (GPU) + yo |
| **E6** | `eval` del `.tflite` exportado en tu set real → `verify` → `release --channel beta` | Yo |
| **E7** | Integro el modelo en la app, cambio `DIGIT_DETECTOR_PRIMARY = litert` (T-A002-35), re-ejecuto el benchmark (T-A002-36) | Yo |
| **E8** | **Validación de campo** en tu móvil (T-A002-38/39): tiempo hasta lectura estable, aciertos, decisión sobre ADR-A002-04 (¿hace falta detectar las esquinas?) | Tú + yo |
| **E9** | Iteración: añadir casos al parser/dorados, reajustar umbrales, reentrenar si hace falta | Yo |

## 6. ETAPA 3 — Hacia producción (cuando decidas)

1. Más dispositivos (≥ 3) → reentrenar y `release --channel production`.
2. Valoración regulatoria y RGPD (Q-LEGAL-01), política de privacidad y términos (Q-LEGAL-02),
   alojamiento en la UE, revisión clínica de los textos de aviso y de la tabla de clasificación
   (Q-SAFE-01/02).
3. URL de producción (Q-A001-02), firma de la app y ficha de Google Play (formulario de seguridad de
   datos).
4. Retirada del OCR legacy y de TensorFlow del backend (T-B002-09) cuando la app nueva esté adoptada.

## 7. Pendientes abiertos

| ID | Pendiente | Bloquea |
|----|-----------|---------|
| Q-A001-02 | URL de producción (HTTPS) | Solo la publicación |
| Q-B001-01 | Dónde se despliega el backend (¿Mongo Atlas en la UE?) | Solo el despliegue |
| Q-B003-05 | Número de fotos, sus valores y su encuadre | Etapa 2 (E1) |
| Q-B003-03 | GPU: ¿Colab/Kaggle o una máquina propia? | Etapa 2 (E4) |
| Q-LEGAL-01/02 | Valoración regulatoria; URLs de privacidad y términos | Solo la publicación |
| Q-SAFE-01/02 | Revisión clínica de textos y clasificación | Solo la publicación |
