# A-002 — Captura de medición en tiempo real · Requisitos

> Estado: 📝 Pendiente de aprobación · Tipo: Feature
> Depende de: A-001 · Backend B-002 (sincronización) · Backend B-003 (modelo propio, fase E)
> Contratos: [`api-v1.md`](../contracts/api-v1.md), [`reading-rules.md`](../contracts/reading-rules.md),
> [`digit-detector-model.md`](../contracts/digit-detector-model.md)
> Resuelve: AUD-A-04 (cámara), 13, 14, 23…36, 41, 55

## 1. Contexto

Hoy, registrar una medición exige: hacer una foto (que acaba en la galería pública), ir al
historial, elegir esa foto de la galería, esperar hasta 7 s a que el servidor la procese con un
modelo poco fiable, y el resultado se guarda **sin que el usuario lo confirme**. El reconocimiento
falla con frecuencia (ver la auditoría del backend AUD-B-01…08) y no hay forma de corregirlo.

## 2. Objetivo y métricas de éxito

**Objetivo**: registrar una medición fiable en **menos de 10 segundos** desde que el usuario
pulsa «Nueva medición», sin enviar imágenes a ningún servidor y siempre con confirmación humana.

| Métrica | Objetivo v1 | Cómo se mide |
|---------|-------------|--------------|
| Tiempo hasta la lectura estable (p50), en buenas condiciones de luz | ≤ 3 s | Protocolo de validación de campo (design §12) |
| Tiempo total hasta guardar (p50) | ≤ 10 s | Ídem |
| Lecturas propuestas correctas sin edición (modelo propio, set real) | ≥ 95 % | Ídem + harness B-003 |
| Lecturas propuestas correctas sin edición (fase ML Kit) | Se mide; es el *baseline* | Ídem |
| Lecturas incorrectas guardadas sin que el usuario lo note | 0 (la confirmación es obligatoria) | Por diseño |
| Imágenes enviadas a servidor | 0 | Por diseño, verificado por inspección de tráfico |

## 3. Historias de usuario

- **HU-A002-1**: Como usuario, quiero iniciar una medición nueva con un toque desde el historial
  o desde la barra inferior.
- **HU-A002-2**: Como usuario, quiero apuntar con la cámara al display y ver en tiempo real que la
  app reconoce los números, para saber si lo estoy haciendo bien.
- **HU-A002-3**: Como usuario, quiero que la app capture sola cuando la lectura es estable, sin
  tener que pulsar un botón ni hacer fotos.
- **HU-A002-4**: Como usuario, quiero revisar los valores propuestos y corregir cualquiera antes
  de guardar.
- **HU-A002-5**: Como usuario, quiero introducir la medición a mano si la cámara no me funciona.
- **HU-A002-6**: Como usuario, quiero que la medición se guarde aunque no tenga conexión y que se
  sincronice sola después.
- **HU-A002-7**: Como usuario, quiero que la app me avise si los valores parecen erróneos o
  peligrosos.
- **HU-A002-8**: Como usuario con baja visión, quiero que el lector de pantalla me indique qué
  está pasando y qué valores se han leído.

## 4. Flujo principal

```
Historial ──[+ Nueva medición]──► Escaneo (cámara + recuadro guía)
                                   │  detecta dígitos en vivo, muestra SYS/DIA/PUL y el progreso
                                   │  lectura estable ──► vibración + transición
                                   ▼
                                 Revisión (valores editables, fecha y hora)
                                   │  [Confirmar] ──► guardado local (pendiente) ──► Historial + snackbar
                                   │  [Repetir]   ──► Escaneo
                                   │  [Descartar] ──► Historial
                                   ▼
                       Sincronización en segundo plano ──► estado «sincronizada» en el historial
```
Ruta alternativa: desde el escaneo, el botón «Introducir a mano» abre la revisión con los campos
vacíos (`source = manual`).

## 5. Requisitos (EARS)

### 5.1 Entrada y permisos
- **REQ-A002-01**: El historial DEBERÁ mostrar una acción principal «Nueva medición» (FAB), y la
  pestaña «Cámara» de la barra inferior DEBERÁ abrir la misma pantalla de escaneo.
- **REQ-A002-02**: CUANDO se abra el escaneo sin el permiso `CAMERA`, la app DEBERÁ pedirlo con el
  diálogo del sistema. Si el sistema indica que hay que mostrar una justificación, se mostrará
  antes una explicación propia.
- **REQ-A002-03**: SI el usuario deniega el permiso, ENTONCES la pantalla DEBERÁ mostrar el motivo
  y dos acciones: «Conceder permiso» (vuelve a pedirlo) e «Introducir a mano».
- **REQ-A002-04**: SI el permiso está denegado de forma permanente, ENTONCES la acción DEBERÁ ser
  «Abrir ajustes», que lleva a los ajustes de la app. Al volver con el permiso concedido, la
  cámara DEBERÁ iniciarse sin más pasos.
- **REQ-A002-05**: La app NO DEBERÁ pedir `RECORD_AUDIO` ni ningún otro permiso para esta
  feature.

### 5.2 Cámara
- **REQ-A002-06**: La pantalla de escaneo DEBERÁ usar la cámara trasera con los casos de uso
  Preview + ImageAnalysis. No DEBERÁ ofrecer cambio a cámara frontal, ni vídeo, ni selector de
  galería.
- **REQ-A002-07**: El análisis DEBERÁ ejecutarse en un executor dedicado con
  `STRATEGY_KEEP_ONLY_LATEST`, formato RGBA_8888 y una resolución de análisis objetivo de
  1280×720 (o la más cercana soportada).
- **REQ-A002-08**: La pantalla DEBERÁ ofrecer un botón de linterna cuando el dispositivo la
  tenga, y DEBERÁ enfocar y medir la exposición en el centro del recuadro guía al empezar y al
  tocar dentro de él.
- **REQ-A002-09**: CUANDO la app pase a segundo plano, la cámara y el análisis DEBERÁN detenerse.
  CUANDO vuelva, DEBERÁN reanudarse **conservando** el progreso de consenso si han pasado menos de
  5 s, y reiniciándolo en otro caso.
- **REQ-A002-10**: SI la cámara no está disponible o falla al iniciar o durante el uso, ENTONCES la
  pantalla DEBERÁ mostrar un estado de error con «Reintentar» e «Introducir a mano».
- **REQ-A002-11**: La pantalla de escaneo DEBERÁ estar bloqueada en orientación vertical.

### 5.3 Reconocimiento en tiempo real
- **REQ-A002-12**: La app DEBERÁ procesar **solo la región del recuadro guía** (ROI) de cada frame
  analizado.
- **REQ-A002-13**: La app DEBERÁ detectar en la ROI los glifos de 7 segmentos (0–9, `:`, `-`, `E`,
  `r`) mediante un `DigitDetector` que se ejecuta **en el dispositivo**.
- **REQ-A002-14**: La app DEBERÁ convertir las detecciones de cada frame en una lectura
  (SYS/DIA/PUL, posiblemente parcial) con el algoritmo de `reading-rules.md` §4, **sin depender
  del modelo de tensiómetro**.
- **REQ-A002-15**: La app DEBERÁ dibujar en vivo sobre la vista previa las cajas de los glifos
  detectados y mostrar los valores provisionales de SYS, DIA y PUL que vaya leyendo.
- **REQ-A002-16**: La app DEBERÁ considerar una lectura **estable** CUANDO, dentro de la ventana
  temporal de consenso, cada uno de los tres campos tenga un valor mayoritario con soporte
  suficiente (parámetros en design §6.4) **y** la combinación supere la validación de errores de
  `reading-rules.md` §3.1.
- **REQ-A002-17**: MIENTRAS no haya lectura estable, la app DEBERÁ mostrar un indicador de
  progreso de 0 a 100 % que refleje el soporte del campo menos estable.
- **REQ-A002-18**: CUANDO la lectura sea estable, la app DEBERÁ detener el análisis, emitir una
  vibración breve, anunciarlo por accesibilidad y navegar a la revisión con los valores, la
  confianza por campo y la versión del detector.
- **REQ-A002-19**: SI se detecta un código de error del tensiómetro (`E`, `Err`) en 3 frames
  consecutivos o más, ENTONCES la app DEBERÁ indicar «El tensiómetro muestra un error. Repite la
  medición en el aparato» y seguir analizando.
- **REQ-A002-20**: SI se detectan guiones (`---`, medición en curso), ENTONCES la app DEBERÁ
  indicar «Esperando a que el tensiómetro termine».
- **REQ-A002-21**: SI la luminancia media de la ROI es baja (umbral en design §6.6), ENTONCES la
  app DEBERÁ sugerir encender la linterna. SI hay una proporción alta de píxeles saturados,
  ENTONCES DEBERÁ sugerir «Inclina el móvil para evitar reflejos».
- **REQ-A002-22**: SI pasan 6 s sin ningún número candidato, ENTONCES la app DEBERÁ mostrar la
  ayuda «Coloca el display dentro del recuadro y acércate».
- **REQ-A002-23**: SI pasan 20 s sin lectura estable, ENTONCES la app DEBERÁ ofrecer, sin
  detener el análisis, las acciones «Usar lo leído» (abre la revisión con los campos que sí se
  hayan estabilizado y el resto vacío) e «Introducir a mano».
- **REQ-A002-24**: SI el detector no puede cargarse (modelo ausente, corrupto, incompatible o con
  error del runtime), ENTONCES la app DEBERÁ usar el detector de respaldo. Si tampoco hay respaldo,
  DEBERÁ mostrar «El reconocimiento no está disponible en este dispositivo» con «Introducir a
  mano».
- **REQ-A002-25**: Los frames NO DEBERÁN guardarse en disco ni transmitirse. Solo se conservan en
  memoria mientras se procesan.

### 5.4 Revisión y edición
- **REQ-A002-26**: La pantalla de revisión DEBERÁ mostrar tres campos numéricos editables (SYS,
  DIA y PUL), con su unidad, rellenados con los valores propuestos, y la fecha y hora de la
  medición (por defecto, el momento de la captura) también editables.
- **REQ-A002-27**: Los campos cuya confianza esté por debajo de 0,70, o que estén vacíos porque no
  se leyeron, DEBERÁN resaltarse con el texto «Comprueba este valor».
- **REQ-A002-28**: Los campos DEBERÁN aceptar solo dígitos (máximo 3) con el teclado numérico, y
  validar con `reading-rules.md` §3.1 mientras el usuario escribe. Cada error se muestra **debajo
  del campo afectado**.
- **REQ-A002-29**: El botón «Confirmar» DEBERÁ estar deshabilitado MIENTRAS haya algún error.
- **REQ-A002-30**: CUANDO el usuario pulse «Confirmar» y existan avisos (`reading-rules.md` §3.2,
  incluido `POSSIBLE_DUPLICATE`), la app DEBERÁ mostrar un diálogo con todos ellos y las opciones
  «Guardar igualmente» y «Revisar». `VERY_HIGH_PRESSURE` DEBERÁ incluir el consejo de seguridad.
- **REQ-A002-31**: La app DEBERÁ registrar qué campos modificó el usuario respecto a la propuesta
  (`edited_fields`).
- **REQ-A002-32**: «Repetir» DEBERÁ volver al escaneo con el consenso reiniciado. «Descartar»
  DEBERÁ volver al historial **sin guardar**. Si ha habido ediciones, se pedirá antes
  confirmación de descarte.
- **REQ-A002-33**: El estado de la revisión (valores escritos y fecha) DEBERÁ sobrevivir a un
  cambio de configuración y a la muerte del proceso.

### 5.5 Guardado y sincronización (offline-first)
- **REQ-A002-34**: CUANDO el usuario confirme, la app DEBERÁ guardar la medición en la base de
  datos local con estado `PENDING` y un `client_id` UUID v4, volver al historial y mostrar «Medición
  guardada».
- **REQ-A002-35**: SI el guardado local falla, ENTONCES la app DEBERÁ mantener al usuario en la
  revisión con sus datos intactos y mostrar «No se pudo guardar. Inténtalo de nuevo» con
  «Reintentar».
- **REQ-A002-36**: Tras guardar, la app DEBERÁ programar una sincronización en segundo plano
  (WorkManager, restricción de red conectada, backoff exponencial que empieza en 30 s) que envíe
  cada medición `PENDING` a `POST /v1/measurements`.
- **REQ-A002-37**: CUANDO el servidor responda 201 o 200 (reintento idempotente), la medición
  DEBERÁ pasar a `SYNCED` y guardar el `id` remoto.
- **REQ-A002-38**: SI el servidor responde 422 o 409 (`CLIENT_ID_CONFLICT`), ENTONCES la medición
  DEBERÁ pasar a `SYNC_FAILED` con el motivo, **sin reintentar automáticamente**, y el historial
  DEBERÁ mostrarlo con las acciones «Reintentar» y «Eliminar».
- **REQ-A002-39**: SI el error es de red o 5xx, ENTONCES la medición DEBERÁ seguir en `PENDING` y
  el trabajo se reintentará con backoff.
- **REQ-A002-40**: SI la sesión ha expirado (401 tras el intento de refresh), ENTONCES la
  sincronización DEBERÁ detenerse **sin perder** mediciones. Las pendientes se envían cuando el
  usuario vuelve a iniciar sesión **con la misma cuenta**.
- **REQ-A002-41**: CUANDO el usuario cierre sesión con mediciones `PENDING` o `SYNC_FAILED`, la
  app DEBERÁ avisar de que se perderán y pedir confirmación.
- **REQ-A002-42**: El historial DEBERÁ mostrar las mediciones locales y las remotas fusionadas
  (por `client_id` o por `id` remoto), ordenadas por `measured_at` descendente, con un indicador
  discreto para `PENDING` y uno visible para `SYNC_FAILED`.
- **REQ-A002-43**: CUANDO el usuario elimine una medición `PENDING` o `SYNC_FAILED`, DEBERÁ
  borrarse solo en local. Si está `SYNCED`, DEBERÁ borrarse en remoto y luego en local. SI el borrado
  remoto falla, ENTONCES la medición DEBERÁ permanecer visible y mostrarse un error.

### 5.6 Accesibilidad
- **REQ-A002-44**: Los cambios de estado del escaneo (guía, valores leídos y captura) DEBERÁN
  anunciarse mediante `LiveRegion` educada, con un máximo de 1 anuncio cada 2 s para no saturar.
- **REQ-A002-45**: Todos los controles DEBERÁN tener `contentDescription` y un objetivo táctil de
  al menos 48 dp, y los textos DEBERÁN escalar con el tamaño de fuente del sistema hasta 200 % sin
  solaparse en la revisión.

## 6. Criterios de aceptación

| ID | Escenario (Dado / Cuando / Entonces) | Req. |
|----|--------------------------------------|------|
| CA-A002-01 | Dado el historial, cuando pulso «Nueva medición», entonces se abre el escaneo con la vista previa en menos de 1 s (dispositivo de referencia) | 01, 06 |
| CA-A002-02 | Dado que no he concedido el permiso, cuando lo deniego, entonces veo el motivo, «Conceder permiso» e «Introducir a mano» | 02, 03 |
| CA-A002-03 | Dado el permiso denegado permanentemente, cuando pulso «Abrir ajustes», lo concedo y vuelvo, entonces la cámara arranca | 04 |
| CA-A002-04 | Dado un display que muestra 128/82/67 con buena luz, cuando lo encuadro, entonces veo cajas sobre los dígitos y valores provisionales, y en ≤ 3 s (p50) vibra y se abre la revisión con 128, 82 y 67 | 13–18 |
| CA-A002-05 | Dado un display con hora y fecha visibles, cuando se estabiliza, entonces la hora y la fecha no se proponen como valores | 14 |
| CA-A002-06 | Dado un display con «E1», cuando lo encuadro, entonces veo el aviso de error del tensiómetro y no se navega a la revisión | 19 |
| CA-A002-07 | Dado un display con «---», cuando lo encuadro, entonces veo «Esperando a que el tensiómetro termine» | 20 |
| CA-A002-08 | Dado poca luz, cuando la luminancia baja del umbral, entonces se sugiere la linterna y el botón está accesible | 08, 21 |
| CA-A002-09 | Dado que solo se estabilizan SYS y DIA, cuando pasan 20 s, entonces aparece «Usar lo leído», y al pulsarlo la revisión muestra SYS y DIA con PUL vacío y resaltado | 23, 27 |
| CA-A002-10 | Dado un modelo LiteRT corrupto, cuando abro el escaneo, entonces funciona con el detector de respaldo y queda registrada la causa | 24 |
| CA-A002-11 | Dada la revisión, cuando escribo SYS=80 y DIA=82, entonces aparece el error bajo SYS y «Confirmar» se deshabilita | 28, 29 |
| CA-A002-12 | Dada la revisión con 185/100/80, cuando pulso «Confirmar», entonces aparece el diálogo con el aviso y el consejo de seguridad, y «Guardar igualmente» guarda | 30 |
| CA-A002-13 | Dada una medición idéntica guardada hace 2 minutos, cuando confirmo la misma lectura, entonces aparece el aviso `POSSIBLE_DUPLICATE` | 30 |
| CA-A002-14 | Dada la revisión con PUL editado de 76 a 67, cuando guardo, entonces el registro local y el enviado llevan `edited_fields = ["pulse"]` | 31 |
| CA-A002-15 | Dada la revisión con valores editados, cuando giro el dispositivo o el sistema mata el proceso y vuelvo, entonces mis valores siguen ahí | 33 |
| CA-A002-16 | Dado el modo avión, cuando confirmo una medición, entonces aparece en el historial como pendiente, y al recuperar la red pasa a sincronizada sin intervención | 34, 36, 37, 42 |
| CA-A002-17 | Dado un servidor que responde 422, cuando se sincroniza, entonces la medición aparece como «No sincronizada» con «Reintentar» y «Eliminar», y no se reintenta sola | 38 |
| CA-A002-18 | Dado que la sincronización se reintenta tras un timeout en el que el servidor sí guardó, cuando reenvía, entonces el servidor responde 200 y no hay duplicado | 37 |
| CA-A002-19 | Dadas mediciones pendientes, cuando cierro sesión, entonces se me avisa y puedo cancelar | 41 |
| CA-A002-20 | Dado TalkBack activo, cuando se estabiliza una lectura, entonces se anuncia «Lectura capturada: 128, 82, pulso 67» | 44 |
| CA-A002-21 | Dado el tráfico de red durante un escaneo completo, cuando se inspecciona, entonces no hay ninguna petición con imágenes | 25 |
| CA-A002-22 | Dado el escaneo, cuando envío la app a segundo plano 2 s y vuelvo, entonces el progreso se conserva; con 10 s, se reinicia | 09 |

## 7. Requisitos no funcionales

| ID | Requisito | Objetivo |
|----|-----------|----------|
| RNF-A002-01 | Frecuencia de análisis efectiva (dispositivo de referencia) | ≥ 10 fps |
| RNF-A002-02 | Frecuencia de análisis efectiva (gama baja, Android 9) | ≥ 4 fps (el consenso se adapta, ver design §6.4) |
| RNF-A002-03 | Latencia de `recognize(frame)` (detector + parser), p95 | ≤ 60 ms |
| RNF-A002-04 | Fluidez de la vista previa y del overlay | Sin *jank* perceptible: el overlay se recompone ≤ 15 veces/s |
| RNF-A002-05 | Memoria adicional durante el escaneo | ≤ 80 MB |
| RNF-A002-06 | Tamaño añadido al APK (modelo + runtime) | ≤ 12 MB |
| RNF-A002-07 | Tiempo de arranque del detector (primera inferencia lista) | ≤ 800 ms, cargado en segundo plano al entrar |
| RNF-A002-08 | Consumo | El análisis se detiene al capturar, en segundo plano y tras 60 s sin interacción (pausa con «Continuar») |
| RNF-A002-09 | Funcionamiento offline | El escaneo, la revisión y el guardado funcionan sin red |

## 8. Fuera de alcance (v1)

- Importar una foto desde la galería (puede añadirse después reutilizando el mismo pipeline con un
  único frame; ver ADR-A002-07).
- Detector de las esquinas del display y corrección de perspectiva (v2 si la validación de
  campo muestra errores por inclinación).
- Descarga remota de modelos (v1 incluye el modelo en el APK).
- Lectura por Bluetooth de tensiómetros conectados.
- Contribución de imágenes para reentrenamiento (requiere consentimiento y una spec de
  privacidad propia).
- Más de un usuario o perfil por dispositivo.

## 9. Preguntas abiertas

| ID | Pregunta | Propuesta por defecto |
|----|----------|------------------------|
| Q-A002-01 | ¿La pestaña «Cámara» de la barra inferior se mantiene, o se sustituye solo por el FAB? | Mantener las dos entradas |
| Q-SAFE-01 | ¿Quién valida los textos de aviso clínico (`VERY_HIGH_PRESSURE`...)? | Un profesional sanitario antes de publicar en producción |
| Q-A002-02 | ¿Qué tensiómetros reales hay disponibles para la validación de campo? (mínimo 3 modelos distintos) | Necesita respuesta del usuario |
| Q-A002-03 | ¿Se acepta guardar con PUL vacío? | **No**: PUL es obligatorio (coherente con el contrato). Si no se leyó, el usuario lo escribe |
