# 📲 TensoScan — AI-powered Blood Pressure Monitor Companion

<p align="center">
  <img src="assets/tensoscan_logo_400.png" alt="TensoScan Hero Banner" width="200"/>
</p>

> **Turn your traditional blood pressure monitor into a smart health tracker**
>
> TensoScan automatically scans and recognizes the digits on your monitor's screen using AI. It saves each reading and builds a personal history to help you track your blood pressure over time

---

<p align="center">
  <img src="assets/demo-tensoscan.gif" alt="TensoScan Demo" width="300"/>
</p>

---

## 🚀 Key Features

- 📷 **Live, on-device digit recognition**: point the camera at the monitor's display and the reading is
  detected frame by frame; nothing is saved until you confirm it (and camera frames never leave the phone)
- ✏️ **Review and correct** systolic, diastolic and pulse before saving, with warnings for unusual values
- 💾 **Offline-first**: every measurement is stored in Room immediately and uploaded in the background
  (WorkManager) when there is a connection
- 🧾 **History** with an evolution chart, an informative classification of each reading and PDF export
- 🔐 Account with refresh-token sessions, account deletion and an assistant that answers general questions
- ♿ Accessible: TalkBack announcements while scanning, text alternatives for the chart, no colour-only meaning

---

## 🧱 Architecture

`ui → presentation → domain ← data`, with `app` wiring everything together with Hilt.

| Module | Contents |
|--------|----------|
| `:domain` | Pure Kotlin: models, use cases, the recognition core (layout parser, stabiliser, session), validation rules |
| `:data` | Room, Retrofit/OkHttp, WorkManager sync, ML Kit and LiteRT detectors, session storage |
| `:presentation` | ViewModels with unidirectional data flow (`UiState` / `UiAction` / `UiEvent`) |
| `:ui` | Jetpack Compose screens, CameraX preview and permissions |
| `:app` | `Application`, `MainActivity`, Hilt modules |
| `:core:testing` | Test helpers shared by the modules |

The rules are enforced by `./gradlew verifyArchitecture` (module dependencies, forbidden imports,
no `runCatching`, no direct `Dispatchers.*`).

The project follows **Spec-Driven Development**: the source of truth is [`specs/`](specs/README.md)
(requirements, design and tasks) and the API contract shared with the backend lives in
[`specs/contracts/`](specs/contracts/).

---

## 🛠️ Build and verify

```bash
./gradlew verifyArchitecture spotlessCheck test testDebugUnitTest assembleDebug
```

---

🧰 Tech Stack
| Category          | Technology                                   |
|------------------|-----------------------------------------------|
| 🖼️ UI             | Jetpack Compose, Navigation (type-safe routes) |
| 🤖 Language        | Kotlin, Coroutines, Flow                     |
| 🏗️ Architecture   | MVVM / UDF + Clean Architecture               |
| 🔍 Vision AI       | CameraX + ML Kit text recognition (LiteRT detector ready for the in-house model) |
| 🌐 Networking      | Retrofit + OkHttp + kotlinx.serialization    |
| 💾 Storage         | Room, DataStore                               |
| 🔄 Background      | WorkManager                                   |
| 💉 DI              | Hilt                                          |
| 🧪 Testing         | JUnit 5, MockK, Turbine, MockWebServer        |

---

🧠 Artificial Intelligence
- The first detector runs ML Kit text recognition on the cropped display area and a deterministic layout
  parser turns the symbols into systolic / diastolic / pulse
- A custom LiteRT digit detector can replace it behind the same `DigitDetector` interface; its model contract
  is in [`specs/contracts/digit-detector-model.md`](specs/contracts/digit-detector-model.md)

---

📄 License
This project is licensed under the MIT License

---

