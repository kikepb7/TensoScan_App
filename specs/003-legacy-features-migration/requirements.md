# A-003 — Migración de features existentes · Requisitos

> Estado: 📝 Pendiente de aprobación · Tipo: Refactor + correcciones de producto
> Depende de: A-001 (y, para el historial, de A-002 fases C y D) · Backend B-001 (auth v1)
> Resuelve: AUD-A-04 (Summary), 07, 08, 09, 11, 15…22, 42…47 y el *baseline* de detekt

## 1. Contexto

Tras A-001 las features legacy compilan con Hilt y en sus módulos correctos, pero siguen con el
patrón antiguo: estado con flags, navegación por heurística, errores como `String`, textos
literales, `CoroutineScope` en composables y llamadas a rutas no versionadas. Además, la
clasificación de la tensión es clínicamente incorrecta (AUD-A-43) y el informe se pasa como HTML
en un argumento de navegación (AUD-A-17) con JS activado (AUD-A-46).

## 2. Historias de usuario

- **HU-A003-1**: Como usuario con sesión iniciada, quiero que la app abra directamente mi
  historial.
- **HU-A003-2**: Como usuario, quiero mensajes de error claros al iniciar sesión o registrarme
  (credenciales incorrectas, email ya registrado, sin conexión).
- **HU-A003-3**: Como usuario, quiero ver mi historial con un gráfico de evolución y una
  clasificación orientativa correcta de cada lectura.
- **HU-A003-4**: Como usuario, quiero exportar mi historial en PDF y compartirlo (por ejemplo con
  mi médico).
- **HU-A003-5**: Como usuario, quiero que, si caduca la sesión, se me lleve al login con un aviso y
  sin perder mis mediciones pendientes.

## 3. Requisitos (EARS)

### Sesión y arranque
- **REQ-A003-01**: Al arrancar, la app DEBERÁ mantener la *splash* MIENTRAS se resuelve
  `SessionState`, y DEBERÁ navegar al historial si es `Authenticated` o al login en otro caso.
- **REQ-A003-02**: CUANDO `SessionState` pase a `Expired`, la app DEBERÁ navegar al login,
  limpiar el back stack y mostrar «Tu sesión ha caducado. Vuelve a iniciar sesión».

### Login y registro
- **REQ-A003-03**: El login DEBERÁ usar `POST /v1/auth/login` y guardar el access token y el
  refresh token.
- **REQ-A003-04**: El login DEBERÁ navegar al historial **solo** por un evento explícito de éxito.
- **REQ-A003-05**: El login DEBERÁ distinguir y mostrar: credenciales incorrectas, sin conexión,
  demasiados intentos (429) y error del servidor.
- **REQ-A003-06**: El registro DEBERÁ validar en local: nombre y apellidos no vacíos, formato de
  email, contraseña de 8 a 128 caracteres y confirmación igual. Los errores se mostrarán bajo cada
  campo.
- **REQ-A003-07**: El registro DEBERÁ mostrar «Este email ya está registrado» ante un 409 y, tras
  registrarse, DEBERÁ volver al login con el email ya relleno y un mensaje de éxito.

### Historial
- **REQ-A003-08**: El historial DEBERÁ tener los estados: carga inicial, vacío (con la llamada a la
  acción «Nueva medición»), contenido y error de la carga inicial sin datos locales (con
  «Reintentar»). Si falla un refresco con datos locales, DEBERÁ conservar los datos y mostrar un
  snackbar.
- **REQ-A003-09**: El historial DEBERÁ permitir refrescar tirando hacia abajo (*pull-to-refresh*).
- **REQ-A003-10**: Cada elemento DEBERÁ mostrar SYS/DIA mmHg, el pulso, la fecha y hora locales,
  la clasificación orientativa (REQ-A003-12) y el estado de sincronización (de A-002). La
  «confianza» del modelo NO DEBERÁ mostrarse al usuario.
- **REQ-A003-11**: Borrar DEBERÁ pedir confirmación (o permitir deshacer con un snackbar durante
  5 s) antes de ejecutar el borrado de A-002.
- **REQ-A003-12**: La clasificación DEBERÁ calcularla `BloodPressureClassifier` en `:domain`
  usando **SYS y DIA** (gana la categoría más alta de las dos), con la guía definida en el ADR
  correspondiente. La UI DEBERÁ indicar que es orientativa y que una sola lectura no diagnostica.
- **REQ-A003-13**: El gráfico de evolución DEBERÁ ser nativo (Compose) y mostrar SYS, DIA y pulso de
  las últimas N mediciones o del rango elegido. La vista HTML/WebView DEBERÁ eliminarse.
- **REQ-A003-14**: «Exportar PDF» DEBERÁ descargar `GET /v1/measurements/report?format=pdf&tz=…`
  a la caché de la app y abrir el *share sheet* del sistema (vía `FileProvider`). SI falla,
  ENTONCES DEBERÁ mostrarse un error con «Reintentar».

### Usuario y asistente
- **REQ-A003-15**: La pantalla de usuario DEBERÁ mostrar los estados de carga, contenido y error
  (con «Reintentar») de `GET /v1/users/me`.
- **REQ-A003-16**: Cerrar sesión DEBERÁ llamar a `POST /v1/auth/logout` en el mejor esfuerzo,
  limpiar la sesión y los datos locales (con el aviso de pendientes de A-002, REQ-A002-41) y
  navegar al login.
- **REQ-A003-17**: El asistente DEBERÁ usar `POST /v1/assistant/messages`, mostrar un aviso fijo
  («El asistente no sustituye el consejo médico»), un indicador de escritura, y errores con
  reintento del último mensaje. El historial enviado DEBERÁ limitarse a los últimos 20 mensajes.

### Calidad transversal
- **REQ-A003-18**: Todas las pantallas DEBERÁN seguir C-PAT-01…08 (Screen y Content, `UiState`,
  `UiAction`, `UiEvent` y previews).
- **REQ-A003-19**: El código NO DEBERÁ contener textos visibles literales ni `contentDescription`
  incorrectos.
- **REQ-A003-20**: Los ficheros y símbolos DEBERÁN seguir las convenciones de nombre de la skill
  (`LoginScreen.kt`, `LoginViewModel`, `LoginScreenUiState`...), con visibilidad `internal`.
- **REQ-A003-21**: Al terminar, `Either`, `FailureDomain`, `PredictionUiModel`, `utils/` y
  `common/extensions/` NO DEBERÁN existir y el *baseline* de detekt DEBERÁ estar vacío.

## 4. Criterios de aceptación

| ID | Escenario | Req. |
|----|-----------|------|
| CA-A003-01 | Dada una sesión válida, cuando abro la app, entonces veo el historial sin pasar por el login | 01 |
| CA-A003-02 | Dado un refresh token inválido, cuando una petición devuelve 401, entonces acabo en el login con el aviso y mis mediciones `PENDING` siguen en Room | 02 |
| CA-A003-03 | Dadas credenciales incorrectas, cuando inicio sesión, entonces veo «Email o contraseña incorrectos» y no navego | 04, 05 |
| CA-A003-04 | Dado un email ya registrado, cuando me registro, entonces veo el error bajo el campo email | 07 |
| CA-A003-05 | Dado 135/82, cuando veo el historial, entonces la clasificación corresponde a la tabla del ADR (y no solo a la sistólica) | 12 |
| CA-A003-06 | Dado el historial, cuando pulso «Exportar PDF», entonces se abre el *share sheet* con `tensoscan-report-*.pdf` | 14 |
| CA-A003-07 | Dado el código, cuando busco `WebView`, `Either` o `CoroutineScope(Dispatchers`, entonces no hay resultados | 13, 18, 21 |
| CA-A003-08 | Dado TalkBack, cuando recorro la barra inferior, entonces cada icono se anuncia correctamente | 19 |

## 5. Fuera de alcance
- Recuperación de contraseña y verificación de email.
- Rediseño visual completo.
- Filtros avanzados del historial (más allá del rango temporal del gráfico).

## 6. Preguntas abiertas

| ID | Pregunta | Propuesta por defecto |
|----|----------|------------------------|
| Q-SAFE-02 | ¿Qué guía clínica usa la clasificación? | ESC 2024 con umbrales **domiciliarios** (ver design ADR-A003-01), validado por un profesional |
| Q-A003-01 | ¿Se mantiene el asistente (chatbot)? Tiene implicaciones de coste y de seguridad clínica | Mantener, con aviso y límites |
