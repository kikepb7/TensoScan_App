# TensoScan — Pasos restantes hasta una prueba real en un dispositivo

> Estado a 2026-10-19. El código de la etapa 1 está terminado y verificado por CI en las ramas
> `recognition-refactor-feature` de `TensoScan_App` y `TensoScan_Backend`. Lo que falta es poner el
> sistema en marcha con un móvil real, medir cuánto acierta y, después, entrenar el modelo propio.
>
> Este documento se divide en **tres bloques**. El **A** es lo mínimo para probar el escaneo en
> tu móvil hoy; el **B** mide el acierto con tus fotos; el **C** produce el modelo propio.

## Resumen en una tabla

| # | Qué | Quién | Necesita | Estado |
|---|-----|-------|----------|--------|
| A1 | Arrancar el backend en tu ordenador | Tú | Docker (o `uv` + MongoDB) | Pendiente |
| A2 | Conseguir el APK de debug | Tú | Acceso a GitHub Actions (o Android Studio) | Pendiente |
| A3 | Conectar el móvil con el backend | Tú | Cable USB + `adb` | Pendiente |
| A4 | Probar el escaneo con el Medisana | Tú | A1 + A2 + A3 | Pendiente |
| B1 | Verificar las etiquetas de las fotos | Tú | La tabla de 53 lecturas | **Bloquea B y C** |
| B2 | Herramienta de volcado de detecciones | Claude | — | **No existe aún** |
| B3 | Línea base de ML Kit con tus fotos | Tú + Claude | B1 + B2 + móvil | Pendiente |
| C1 | Decidir las correcciones del parser (F1–F3) | Tú | — | ✅ Hecho (contrato 1.1.0) |
| C2 | Dataset sintético | Claude | C1 | Pendiente |
| C3 | Entrenar con GPU | Tú | Colab o Kaggle | Pendiente |
| C4 | Evaluar, exportar y publicar el modelo `beta` (incluye terminar `release`) | Claude | C3 + B1 | Pendiente |
| C5 | Integrar el modelo y activar `litert` | Claude | C4 | Pendiente |
| C6 | Validación de campo en el móvil | Tú + Claude | C5 | Pendiente |

---

## Bloque A — Probar el escaneo en tu móvil (con ML Kit)

El detector inicial es **ML Kit**, que ya viene en la app. No hace falta el modelo propio para
esta prueba. Lo que sí hace falta es un **backend en marcha**: la app abre en el login y no se
puede entrar sin él.

### A1. Arrancar el backend

Desde la carpeta de `TensoScan_Backend`:

```bash
cp .env.example .env
# Edita .env: TENSOSCAN_SECRET_KEY (≥ 32 caracteres aleatorios) y las credenciales de Mongo
docker compose up --build        # API en http://127.0.0.1:8000, documentación en /docs
```

Sin Docker: `uv sync --dev` y `uv run uvicorn app.main:create_app --factory --reload`, con un MongoDB
accesible en `TENSOSCAN_MONGODB_URL`. El servicio **no arranca sin `TENSOSCAN_SECRET_KEY`**.

Comprueba que funciona abriendo `http://127.0.0.1:8000/docs`.

### A2. Conseguir el APK de debug

Dos opciones:

- **Sin instalar nada**: en GitHub, repo `kikepb7/TensoScan_App` → pestaña **Actions** → último run
  verde de la rama `recognition-refactor-feature` → artefacto **`tensoscan-debug-apk`**. Descárgalo y
  descomprímelo.
- **Con Android Studio**: abre el proyecto y ejecuta `./gradlew assembleDebug`
  (el APK queda en `app/build/outputs/apk/debug/`).

Instálalo en el móvil: `adb install -r app-debug.apk` (con la depuración USB activada) o copia el
fichero y ábrelo desde el móvil.

### A3. Conectar el móvil con el backend (importante)

La configuración de red de **debug** solo permite tráfico sin cifrar hacia `10.0.2.2` (el emulador)
y `localhost`. **Un móvil real no puede usar `http://<IP-del-ordenador>:8000/`**: Android lo bloquea.
La forma más sencilla es redirigir el puerto por USB:

```bash
adb reverse tcp:8000 tcp:8000
```

Con eso el móvil ve el backend como `http://localhost:8000/`. Después hay que compilar la app con esa
URL, porque el valor por defecto es `10.0.2.2` (solo emulador). En `local.properties` del proyecto
Android añade:

```properties
tensoscan.apiBaseUrl=http://localhost:8000/
```

y vuelve a generar el APK **con Android Studio o `./gradlew assembleDebug` en tu ordenador** (el APK
de CI usa el valor por defecto, así que con él solo funcionaría un emulador).

> Alternativas si `adb reverse` no te sirve: un túnel HTTPS (por ejemplo `cloudflared` o `ngrok`)
> con `tensoscan.apiBaseUrl=https://<tu-túnel>/`, o añadir la IP de tu ordenador a
> `app/src/debug/res/xml/network_security_config.xml` (cambio de código de una línea; pídemelo).

### A4. Qué probar con el Medisana

1. **Registro e inicio de sesión**: crea una cuenta (hay que marcar la casilla de privacidad) e
   inicia sesión. Debe llevarte al historial.
2. **Escaneo en vivo**: pulsa «Cámara», concede el permiso y encaja el display en el recuadro.
   Anota **cuánto tarda en fijar la lectura** y si acierta los tres números.
3. **Casos difíciles**: poca luz, reflejo de una lámpara, móvil algo inclinado, lejos y cerca. Los
   avisos de la pantalla (acércate, reflejos, poca luz) deben aparecer.
4. **Revisión**: cambia un valor a mano y guarda. La medición **nunca se guarda sin tu
   confirmación**. Prueba también un valor extremo (por ejemplo 185/125) y comprueba que pide
   confirmación adicional.
5. **Sin conexión**: activa el modo avión, guarda una medición y comprueba que aparece como
   «pendiente de subir». Al desactivarlo debe subirse sola.
6. **Historial**: gráfico, clasificación orientativa, borrar con confirmación, exportar el PDF y
   compartirlo.
7. **Cerrar sesión y eliminar cuenta**: con mediciones pendientes debe avisar antes de borrar.
8. **Accesibilidad**: con TalkBack activado, recorre el escaneo y la revisión; prueba la fuente
   del sistema al 200 %.

**Qué apuntar** (sirve para el informe de validación): modelo del móvil y versión de Android,
tiempo hasta lectura estable, aciertos sobre 20 intentos por condición de luz, y cualquier fallo
o pantalla que no se entienda.

---

## Bloque B — Medir cuánto acierta ML Kit con tus fotos

Sirve de **línea base**: el modelo propio solo merece la pena si supera estos números.

### B1. Verificar las etiquetas (bloquea B y C)

Claude leyó SYS/DIA/PUL de las 251 fotos a mano, **sin verificación humana**. Hay que confirmarlas:

- Claude te pasará una tabla con las **53 lecturas distintas** y cuántas fotos tiene cada una. Basta
  con que compruebes una foto por lectura.
- **15 fotos están aparte** (`ml/datasets/labels/ds-real-0.1.0.needs-review.csv`) porque el valor no
  era claro o el aparato está girado. Las más importantes: **p055–p057 muestran 177/157/56**; DIA 157
  no es una lectura válida, así que hay que decir qué marcaba realmente el aparato.
- Las fotos están en tu Drive con su nombre original; `photo_map.csv` (en el mismo directorio de
  etiquetas) traduce `pNNN` al nombre original.

### B2. Herramienta de volcado de detecciones (la escribe Claude)

El plan prevé que la app, en modo debug, vuelque un `detections.jsonl` con lo que detecta ML Kit en
cada foto, para medirlo con la misma métrica que el modelo propio. **Esa herramienta todavía no
existe.** Es una pantalla o un botón solo de debug que recorra una carpeta de fotos del móvil, ejecute
el detector sobre el recorte del display y escriba el fichero. Cuando la tengas, se hace así:

1. Copia las fotos al móvil (carpeta `Download/tensoscan-fotos/`).
2. Ejecuta el volcado en la app de debug y copia el `detections.jsonl` al ordenador.

### B3. Evaluar

```bash
cd TensoScan_Backend/ml
export TENSOSCAN_ML_DATA_DIR=/ruta/a/los/datos
uv run tensoscan-ml eval --detector detections.jsonl \
    --manifest $TENSOSCAN_ML_DATA_DIR/real/ds-real-0.1.0/test.jsonl --out report-mlkit
```

El informe da el porcentaje de lecturas completas
acertadas y el de cada campo. Como solo hay un tensiómetro, el resultado vale como **beta**, no
como validación de producción.

> Si prefieres no esperar a B2, el bloque A4 ya te da una medida real aunque más manual: aciertos
> sobre 20 intentos por condición.

---

## Bloque C — Modelo propio (LiteRT)

### C1. Reglas de lectura (hecho: contrato 1.1.0)

Las correcciones F1–F3 ya están aplicadas en el contrato (`specs/contracts/reading-rules.md` §6), en el
parser de Python y en el de la app, con 7 casos dorados nuevos que pasan con el mismo resultado en
los dos lenguajes:

| Id | Qué se hizo |
|----|-------------|
| F1 | Permiso para el hueco previo a un «1» (que depende de la proporción de la ROI, `roi_aspect`) y partición de tandas de 4 a 6 dígitos. Con el generador sintético: fuentes ordinarias 86 % → 99,6 %. |
| F2 | `GROUP_MAX_OVERLAP_FACTOR` 0,30 → 0,45: el acierto con giro y perspectiva se estabiliza desde 0,45. |
| F3 | Se mantienen las cajas ajustadas de la guía de CVAT; guiones y dos puntos tienen altura mínima propia y «midiendo» es «al menos 2 guiones». |

### C2. Dataset sintético (Claude, ~15 min)

```bash
cd TensoScan_Backend/ml
uv run tensoscan-ml synth --train 30000 --val 3000 --test 500
```

Genera 30.000 imágenes de entrenamiento y 3.000 de validación con etiquetas exactas, y una galería
de muestra para revisar el aspecto. Conviene mirarla juntos: si las pantallas sintéticas no se
parecen a un Medisana, el entrenamiento no transfiere.

### C3. Entrenar (tú, con GPU)

El entrenamiento completo necesita GPU y tarda **1–3 horas**. Lo más sencillo:

1. Abre `TensoScan_Backend/ml/notebooks/train_colab.ipynb` en **Google Colab** (o Kaggle) con GPU.
2. Monta tu Drive; el cuaderno clona el repo, genera o lee el dataset sintético y entrena en dos
   etapas: preentrenamiento sintético y *fine-tuning* con tus fotos.
3. Descarga el `.tflite` y el `.json` (sidecar) resultantes.

Para la segunda etapa hacen falta fotos **con cajas** de los dígitos. Claude las pre-etiqueta con el
modelo y tú las corriges con CVAT siguiendo `ml/docs/cvat-annotation-guide.md`; sin ellas, esa etapa se
omite con un aviso.

### C4. Evaluar, verificar y publicar el modelo `beta`

```bash
uv run tensoscan-ml eval   --detector tflite --model dist/digits-X.tflite --manifest .../test.jsonl
uv run tensoscan-ml verify --model dist/digits-X.tflite --golden synthetic
```

> `release --channel beta|production` (T-B003-13) **todavía no está implementado**: hoy es un
> esqueleto que termina con error. Claude lo completa antes de publicar el primer modelo; hasta
> entonces el sidecar y la comprobación de umbrales se hacen con `verify` y el informe de `eval`.

Criterio: el modelo solo se publica si mejora la línea base de ML Kit **en fotos que no ha visto**
(conjunto de test, 12 lecturas). Con un solo tensiómetro solo se admite el canal `beta`; **`production`
necesita al menos 3 modelos de tensiómetro**.

### C5. Integrar el modelo y activarlo (Claude)

1. Copiar el `.tflite` y su `.json` a `app/src/debug/assets/models/` (el modelo falso actual
   `digits-0.0.0-dummy-fixed.*` hay que retirarlo para que no se cargue en su lugar).
2. Poner `tensoscan.detector=litert` en `local.properties`.
3. Ejecutar el benchmark de latencia (`LiteRtDigitDetectorBenchmark`) en un dispositivo:
   objetivo **p95 ≤ 60 ms**.
4. Si todo va bien, cambiar el primario por defecto a `litert` (T-A002-35). **ML Kit queda como
   respaldo automático** si el modelo falta o es incompatible.

### C6. Validación de campo (tú + Claude)

Repite el bloque A4 con el modelo propio, anota los mismos números y compáralos con los de ML Kit.
Se decide entonces si hace falta detectar las esquinas del display (ADR-A002-04). El resultado se
escribe en `specs/002-realtime-measurement-capture/validation-report.md`.

---

## Pendientes que no bloquean la prueba, pero sí la publicación

| Qué | Para qué |
|-----|----------|
| Test de migración de Room (`data/schemas`) | Necesita el SDK de Android para generar el esquema |
| Revisión clínica de umbrales y textos de aviso (Q-SAFE-01/02) | La app clasifica con ESC en medidas domiciliarias; lo debe validar un profesional |
| URL de producción con HTTPS (Q-A001-02) y dónde se despliega el backend (Q-B001-01) | Release usa un dominio de ejemplo |
| URLs de política de privacidad y términos (Q-LEGAL-02), valoración regulatoria (Q-LEGAL-01) | Es una app que trata datos de salud |
| Firma de la app y ficha de Google Play | Publicación |
| Retirar el OCR antiguo y TensorFlow del backend (T-B002-09) | Cuando la app nueva esté adoptada |

## Dónde está cada cosa

| Qué | Dónde |
|-----|-------|
| Código de la app | `TensoScan_App` · rama `recognition-refactor-feature` |
| Backend y paquete de ML | `TensoScan_Backend` · rama `recognition-refactor-feature` (`ml/` para el ML) |
| Especificaciones y tareas | `specs/` en cada repo |
| Guía de captura de fotos y vídeos | `TensoScan_Backend/ml/docs/capture-protocol.md` |
| Guía de anotación en CVAT | `TensoScan_Backend/ml/docs/cvat-annotation-guide.md` |
| Cuaderno de entrenamiento | `TensoScan_Backend/ml/notebooks/train_colab.ipynb` |
| Manifiesto y etiquetas de tus fotos | `TensoScan_Backend/ml/datasets/` |
| Las fotos | Tu Drive; copia temporal en el contenedor (se pierde al reciclarse) |
