# Android MVI + Clean Architecture Boilerplate

Copy this folder, rename `com.example.app` everywhere, and you have a fully wired project.

---

## What's included

| Layer | Files |
|---|---|
| MVI core | `core/mvi/MviViewModel`, `MviContract`, `CollectEffect` |
| Domain | `domain/model/Item`, `domain/repository/ItemRepository`, `domain/usecase/GetItemsUseCase` |
| Data | `data/repository/ItemRepositoryImpl`, `data/network/ApiService`, `data/network/dto/ItemDto` |
| Local DB | `data/local/AppDatabase`, `data/local/entity/ItemEntity`, `data/local/dao/ItemDao`, `data/local/migration/Migrations` |
| DI | `data/di/NetworkModule`, `data/di/DatabaseModule`, `data/di/RepositoryModule` |
| Feature | `feature/items/ItemsContract`, `ItemsViewModel`, `ItemsScreen` |
| UI | `ui/navigation/AppNavHost`, `ui/navigation/Routes`, `ui/theme/AppTheme` |
| App | `MainActivity`, `MyApplication` |

---

## Quick-start checklist

1. **Rename package**: replace `com.example.app` with your package in all files and `app/build.gradle.kts`.
2. **Set `applicationId`** in `app/build.gradle.kts`.
3. **Set `rootProject.name`** in `settings.gradle.kts`.
4. **Add `API_BASE_URL`** to `BuildConfig` (see `app/build.gradle.kts` comment).
5. **Run** `./gradlew installDebug` — the project compiles and shows a list screen backed by Room.

---

## Architecture diagram

```
UI (Composable Screen)
    │  collectAsStateWithLifecycle()
    │  CollectEffect { effect -> ... }
    ▼
ViewModel : MviViewModel<State, Event, Effect>
    │  onEvent(event)  →  updateState { ... }  /  sendEffect(...)
    ▼
Use Case  (optional thin layer, great for reuse / testing)
    ▼
Repository Interface  (domain — no Android deps)
    ▼
RepositoryImpl  (data — Room + Retrofit)
    ├── Local: Room DAO  →  Flow<List<Entity>>
    └── Remote: Retrofit ApiService
```

---

## MVI pattern

### Contract file (`{Feature}Contract.kt`)
```kotlin
data class FooUiState(val items: List<Item> = emptyList()) : MviUiState

sealed interface FooUiEvent : MviUiEvent {
    data object Appeared : FooUiEvent
    data class ItemClicked(val id: String) : FooUiEvent
}

sealed interface FooUiEffect : MviUiEffect {
    data class NavigateToDetail(val id: String) : FooUiEffect
    data class ShowSnackbar(val message: String) : FooUiEffect
}
```

### ViewModel
```kotlin
@HiltViewModel
class FooViewModel @Inject constructor(
    private val getItems: GetItemsUseCase,
) : MviViewModel<FooUiState, FooUiEvent, FooUiEffect>(FooUiState()) {

    init {
        getItems()
            .onEach { items -> updateState { copy(items = items) } }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: FooUiEvent) = when (event) {
        FooUiEvent.Appeared -> { /* trigger side effects on screen appear */ }
        is FooUiEvent.ItemClicked -> sendEffect(FooUiEffect.NavigateToDetail(event.id))
    }
}
```

### Screen
```kotlin
@Composable
fun FooScreen(viewModel: FooViewModel = hiltViewModel(), onNavigate: (String) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.onEvent(FooUiEvent.Appeared) }

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is FooUiEffect.NavigateToDetail -> onNavigate(effect.id)
            is FooUiEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
        }
    }
    // ... Compose UI using state
}
```

---

## Adding a new feature

1. Create `feature/{name}/{Name}Contract.kt` — define `UiState`, `UiEvent`, `UiEffect`.
2. Create `feature/{name}/{Name}ViewModel.kt` — extend `MviViewModel`, inject use cases.
3. Create `feature/{name}/{Name}Screen.kt` — collect state, collect effects, render UI.
4. Add a `composable(Routes.NAME) { ... }` entry in `AppNavHost`.

## Adding a new API endpoint

1. Add method to `ApiService`.
2. Add DTO in `data/network/dto/`.
3. Implement mapping in `ItemRepositoryImpl` (or a new repo if it's a different domain).
4. Add a method to the repository interface + impl.
5. Create a use case in `domain/usecase/` if it will be called from multiple ViewModels.

## Adding a new Room table

1. Create `data/local/entity/{Foo}Entity.kt` with `@Entity`.
2. Create `data/local/dao/{Foo}Dao.kt` with `@Dao`.
3. Add entity to `@Database(entities = [...])` in `AppDatabase`.
4. **Bump `AppDatabase.VERSION`** by 1.
5. Write a `MIGRATION_X_Y` in `Migrations.kt` and register it in `DatabaseModule`.
6. Provide the DAO from `DatabaseModule`.

---

## Library versions (`gradle/libs.versions.toml`)

| Library | Version |
|---|---|
| AGP | 9.2.1 |
| Kotlin | 2.3.20 |
| KSP | 2.3.6 |
| Compose BOM | 2026.03.01 |
| Lifecycle | 2.10.0 |
| Navigation Compose | 2.9.7 |
| Hilt | 2.59.2 |
| Hilt Navigation Compose | 1.3.0 |
| Retrofit | 3.0.0 |
| OkHttp | 5.3.2 |
| Room | 2.8.4 |
| DataStore | 1.2.1 |
| Coroutines | 1.10.2 |

---

## Folder structure

```
app/src/main/java/com/example/app/
├── core/
│   └── mvi/
│       ├── MviContract.kt        ← MviUiState, MviUiEvent, MviUiEffect interfaces
│       ├── MviViewModel.kt       ← Base ViewModel: state, effect, updateState, sendEffect
│       └── CollectEffect.kt      ← Composable helper to collect one-time effects
├── data/
│   ├── di/
│   │   ├── NetworkModule.kt      ← OkHttp, Retrofit, ApiService singletons
│   │   ├── DatabaseModule.kt     ← Room database + DAO providers
│   │   └── RepositoryModule.kt   ← @Binds interface → impl
│   ├── local/
│   │   ├── AppDatabase.kt
│   │   ├── dao/ItemDao.kt
│   │   ├── entity/ItemEntity.kt
│   │   └── migration/Migrations.kt
│   ├── network/
│   │   ├── ApiService.kt
│   │   └── dto/ItemDto.kt
│   └── repository/
│       └── ItemRepositoryImpl.kt
├── domain/
│   ├── model/Item.kt
│   ├── repository/ItemRepository.kt   ← interface (no Android deps)
│   └── usecase/GetItemsUseCase.kt
├── feature/
│   └── items/
│       ├── ItemsContract.kt      ← UiState, UiEvent, UiEffect
│       ├── ItemsViewModel.kt
│       └── ItemsScreen.kt
├── ui/
│   ├── navigation/
│   │   ├── AppNavHost.kt
│   │   └── Routes.kt
│   └── theme/
│       ├── Color.kt
│       └── Theme.kt
├── MainActivity.kt
└── MyApplication.kt
```
