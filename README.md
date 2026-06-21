# Odoo Tasks

An Android client for managing project tasks in an [Odoo](https://www.odoo.com/) ERP
instance. The app authenticates against an Odoo server over JSON-RPC and lets the user
view, create, and update the status of their project tasks from their phone.

Built with **Kotlin**, **Jetpack Compose**, and a strict **Clean Architecture + MVI**
setup.

---

## Features

- **Login** against an Odoo database with username/password (credentials persisted for
  auto-login on next launch).
- **Task list** with pull-to-refresh, a personalised greeting, and per-task status chips.
- **Swipe to delete** — remove a task by swiping its row.
- **Create task** — title, description, and a due-date picker.
- **Update task status** — move a task between the project's shared stages.
- **Account** — view and update the logged-in user's account name, plus logout.

---

## Project setup

### Prerequisites

- Android Studio (Ladybug or newer) / JDK 11+
- An accessible Odoo server and a database you can log into.

### 1. Clone & open

```bash
git clone <repo-url>
cd "practical Test - Android"
```

Open the folder in Android Studio and let it sync Gradle.

### 2. Configure your Odoo instance

The Odoo server URL and default database are read from `local.properties` (which is **not**
committed) and exposed to the app through `BuildConfig`. Add these lines to
`local.properties` in the project root:

```properties
ODOO_BASE_URL=https://your-instance.odoo.com/
ODOO_DATABASE=your_database_name
```

If omitted, the build falls back to the defaults declared in
[app/build.gradle.kts](app/build.gradle.kts) (`buildConfigField "API_BASE_URL"` /
`"ODOO_DATABASE"`). The database can also be typed on the login screen.

> The URL is used as the Retrofit base URL; the app calls the standard Odoo `/jsonrpc`
> endpoint.

### 3. Build & run

```bash
./gradlew installDebug      # build + install on a connected device/emulator
# or
./gradlew assembleDebug     # produce app/build/outputs/apk/debug/app-debug.apk
```

> **Build note:** D8 dexing can be heap-hungry on the default JVM. If a build OOMs, run
> with a larger heap:
>
> ```bash
> ./gradlew assembleDebug -Dorg.gradle.jvmargs="-Xmx4g -XX:MaxMetaspaceSize=1g"
> ```

---

## Architecture

The project follows **Clean Architecture** with three layers and the **MVI**
(Model–View–Intent) pattern in the presentation layer. Dependencies point inward — the
`domain` layer is pure Kotlin with no Android or framework dependencies.

```
┌──────────────────────────────────────────────────────────────┐
│  PRESENTATION  (feature/*)                                     │
│  Compose Screen  ──events──▶  ViewModel : MviViewModel         │
│        ▲                          │  updateState / sendEffect  │
│        └────── State / Effect ────┘                            │
└───────────────────────────────┬──────────────────────────────┘
                                 │ calls
┌───────────────────────────────▼──────────────────────────────┐
│  DOMAIN  (domain/*)            UseCases                        │
│  Models · Repository interfaces · UseCases (business rules,    │
│  validation) — no Android dependencies                        │
└───────────────────────────────┬──────────────────────────────┘
                                 │ implemented by
┌───────────────────────────────▼──────────────────────────────┐
│  DATA  (data/*)               RepositoryImpl                   │
│   ├─ remote:  Retrofit OdooApiService (JSON-RPC) + DTOs        │
│   ├─ local:   Room (TaskEntity/DAO) — cache & source of truth  │
│   └─ prefs:   DataStore SessionDataStore (saved credentials)   │
└──────────────────────────────────────────────────────────────┘
```

### Layer responsibilities

- **Presentation (`feature/`)** — One package per feature (`auth`, `tasks`,
  `task_detail`, `task_create`, `account`). Each has a `Contract` (State / Event /
  Effect), a `ViewModel` (extends a small `MviViewModel` base in `core/mvi`), and a
  Compose `Screen`. The screen renders state and emits events; the ViewModel reduces
  events into new state and one-shot effects (navigation, snackbars).
- **Domain (`domain/`)** — Plain Kotlin models (`Task`, `Stage`, `Project`,
  `OdooUser`), repository **interfaces**, and **use cases** that hold the business rules
  and input validation. This layer is framework-agnostic and the easiest to unit-test.
- **Data (`data/`)** — Repository **implementations** that coordinate three sources:
  - `remote/` — a single Retrofit `OdooApiService` hitting the Odoo `/jsonrpc`
    endpoint, with DTOs mapped to domain models.
  - `local/` — a Room database that caches tasks. Room is treated as the **single
    source of truth**: the task list is observed as a `Flow`, so any write (refresh,
    status update) flows straight back to the UI.
  - `prefs/` — a DataStore (`SessionDataStore`) that persists the session
    (database/username/password/uid) for auto-login.

### Patterns & decisions

- **MVI** gives each screen a single immutable state object and a unidirectional data
  flow, which makes the UI predictable and effects (navigation/snackbars) explicit.
- **Dependency Injection with Hilt** wires repositories, use cases, network, and
  database across the layers without manual plumbing.
- **Reactive cache** — the tasks screen subscribes to Room via `Flow`, so updating a
  task's stage on the detail screen reflects on the list automatically on return.

---

## Libraries used

| Library                                      | Why it's used                                                                                                                              |
| -------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------ |
| **Jetpack Compose** (+ Material 3)           | Declarative UI toolkit — the entire UI is Compose, with Material 3 components (TopAppBar, PullToRefresh, ExposedDropdownMenu, DatePicker). |
| **Navigation Compose**                       | Type-safe, single-activity navigation between the login, list, detail, create, and account screens.                                        |
| **Hilt** (Dagger)                            | Compile-time dependency injection. Provides ViewModels, repositories, use cases, Retrofit, and Room with minimal boilerplate.              |
| **Retrofit** + **Gson converter**            | HTTP client for the Odoo JSON-RPC API. A single endpoint sends typed request bodies and parses dynamic responses.                          |
| **OkHttp** + **logging-interceptor**         | Underlying HTTP engine; the logging interceptor makes JSON-RPC traffic inspectable during development.                                     |
| **Room**                                     | Local SQLite persistence for the task cache, used as the single source of truth and exposed as Kotlin `Flow`.                              |
| **DataStore (Preferences)**                  | Lightweight, async key-value storage for the user session/credentials (modern replacement for SharedPreferences).                          |
| **Kotlin Coroutines / Flow**                 | Asynchronous work and reactive streams throughout (suspend functions in use cases, `Flow` from Room).                                      |
| **AndroidX Lifecycle (ViewModel + Compose)** | Lifecycle-aware ViewModels and `collectAsStateWithLifecycle` for safe state collection in Compose.                                         |
| **Material Icons Extended**                  | Icon set used across screens (deadline, status, navigation icons).                                                                         |
| **JUnit · Truth · Turbine · MockK · coroutines-test** | JVM unit-test toolchain — Truth for readable assertions, Turbine for `Flow`/effect streams, MockK for the Odoo JSON-RPC arg-building tests, coroutines-test for deterministic `runTest`. |
| **Espresso · Compose UI Test · Room-testing · Hilt-android-testing** | Instrumented testing — Compose UI tests, in-memory Room DAO tests, and Hilt-injected navigation/E2E flows.                  |

Versions are centralised in [gradle/libs.versions.toml](gradle/libs.versions.toml).

---

## Testing

The Clean Architecture + MVI layering makes the app highly testable: pure domain use cases,
fakeable repository **interfaces**, ViewModels with deterministic state/effect streams, and a
single Room DAO. The suite is built as a test pyramid — heavy on fast JVM unit tests, with a
thin layer of instrumented Room + Compose UI tests on top.

| Layer                           | Source set         | Tooling                                  |
| ------------------------------- | ------------------ | ---------------------------------------- |
| Domain use cases · DTO mappers  | `src/test` (JVM)   | JUnit, Truth, MockK                      |
| ViewModel (MVI) state/effects   | `src/test` (JVM)   | Turbine, coroutines-test, `MainDispatcherRule` |
| Room DAO                        | `src/androidTest`  | room-testing (in-memory DB)              |
| Compose UI · navigation / E2E   | `src/androidTest`  | Compose UI Test, Hilt-android-testing    |

Shared test infrastructure lives in `src/test/.../util/` — a `MainDispatcherRule` (swaps
`Dispatchers.Main` for a `StandardTestDispatcher`), domain-model **builders**, and hand-written
**fakes** of the three repository interfaces, reused across the unit-test suites. Hilt-backed
instrumented tests run via a custom `HiltTestRunner` (`HiltTestApplication`).

```bash
./gradlew :app:testDebugUnitTest            # fast JVM unit tests
./gradlew :app:connectedDebugAndroidTest    # instrumented tests (device/emulator required)
```

---

## Assumptions & limitations

- **Offline synchronization is not implemented in this version.** Room is used as a
  read cache and single source of truth, but there is no offline write queue or
  conflict resolution — creating/updating tasks requires connectivity.
- **Credentials are stored locally in DataStore** for auto-login. They are not
  encrypted; this is acceptable for a demo/test app but would warrant
  `EncryptedSharedPreferences`/Keystore in production.
- **New tasks are created in the first available project**, since the UI has no project
  picker.
- **Stage list is filtered to shared project stages** (`user_id = false`) so per-user
  personal Odoo stages don't appear in the status dropdown.
- The app targets a single Odoo database/server configured at build time (overridable on
  the login screen); multi-server switching is out of scope.
- `minSdk = 26`, `targetSdk = 36`.

---

## Project structure

```
com.example.odootask
├── core/mvi/            # MVI base classes (MviViewModel, Contract, CollectEffect)
├── domain/
│   ├── model/           # Task, Stage, Project, OdooUser
│   ├── repository/      # Auth/Task/Project repository interfaces
│   └── usecase/         # auth/* and task/* use cases (business rules + validation)
├── data/
│   ├── remote/          # OdooApiService + JSON-RPC DTOs
│   ├── local/           # Room: entity, dao, database
│   ├── prefs/           # SessionDataStore (DataStore)
│   ├── repository/      # Repository implementations
│   └── di/              # Hilt modules (Network, Database, Repository)
├── feature/
│   ├── auth/            # Login
│   ├── tasks/           # Task list
│   ├── task_detail/     # Update task status
│   ├── task_create/     # Create task
│   └── account/         # Account / logout
└── ui/
    ├── navigation/      # AppNavHost, Routes, SessionViewModel
    └── theme/           # Theme, Color, Type

src/test/com.example.odootask/        # JVM unit tests
└── util/                # MainDispatcherRule, builders, repository fakes
src/androidTest/com.example.odootask/ # instrumented tests
└── util/                # HiltTestRunner
```
