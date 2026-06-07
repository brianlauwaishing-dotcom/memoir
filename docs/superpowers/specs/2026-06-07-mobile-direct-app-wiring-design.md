# Mobile-Direct App Wiring — Umbrella Architecture Design

**Date**: 2026-06-07
**Authors**: potato + Claude (brainstorming session)
**Status**: Proposed — pending implementation through per-feature OpenSpec changes
**Scope**: Cross-cutting architecture for completing the Memoir Android app's stubbed interactions, replacing `MockData.kt` with bundled content, and adding direct LLM calls — without any backend service.

---

## 1. Context

### 1.1 Where we are

- `frontend/mobile/` is a Jetpack Compose Android app at `com.mcis.memoir`. PR #79 (merged into `origin/main`) added 17 screens and `data.MockData` with bilingual EN/ZH route + spot data.
- All UI navigation is wired (see `references/guideline.md`). Many interactions are stubs (`{還沒有功能}`): MemoryReflection AI generation, MemoryPhotoSelection picker, MemoryEdit canvas, Memories 3-dots menu, Podcast, Camera capture, etc.
- The Spring Boot backend that previously held content + LLM logic has been removed from the worktree. The team has decided the MVP will run **mobile-direct** (no backend service).
- Designer source-of-truth bilingual content lives in `data/tainan_routes.csv` (untracked) and partially in `data.MockData.kt`.

### 1.2 Driving constraints

- **No backend**: APK must function standalone. No microservices, no Content DB, no narrative service.
- **Content lives in `data/tainan-route/`** at the repo root (designer-editable).
- **Mobile calls LLM directly**: AI reflection feature calls DeepSeek (OpenAI-compatible) from the device.
- **Cost**: free / OSS preferred. `CLAUDE.md` blesses Gemini free tier; the team has been using DeepSeek (very cheap pay-as-you-go) — this is a conscious deviation, documented here.
- **Academic / MVP**: reverse-engineerable embedded API key is acceptable for demo. Test pixel-perfection and full E2E are out of scope.

### 1.3 Non-goals

- Spring Boot backend / Python AI services / K8s deployment
- Podcast playback (no audio source material)
- Auth / user accounts (single-device, single-user)
- Cloud sync across devices
- OpenAI / Anthropic providers (cost)

---

## 2. Decomposition into OpenSpec changes

This umbrella governs cross-cutting architecture. Each of the following lands as its own `openspec` change once approved:

| # | Change | Scope |
|---|---|---|
| 1 | `tainan-route-content-pipeline` | `MockData.kt` → `data/tainan-route/` assets + `ContentRepository` |
| 2 | `language-toggle` | LanguageSelection persists choice + flips app locale |
| 3 | `home-discovery` | HomeScreen search + filter + CultureInterest affects ranking |
| 4 | `route-bookmarking` | RouteDetail bookmark + SavedScreen renders real bookmarks |
| 5 | `artifact-discovery-flow` | SpotIntro → ArtifactDiscovery → ArtifactDetail with real multi-spot data |
| 6 | `artifact-photo-capture` | CameraPreview `onCapture` writes to MediaStore |
| 7 | `memory-creation-flow` | Template → Photo picker → Edit → Reflection wizard, end-to-end |
| 8 | `ai-reflection-generation` | DeepSeek call + retry / error UX (decoupled from #7 via interface) |
| 9 | `memory-library-actions` | Memories 3-dots (edit/delete/duplicate/share); remove dangling arrow; draft resume |

**Deferred**: Podcast (no audio assets).

**Suggested build order**:
```
0 (this umbrella, decisions only)
→ 1 (content pipeline) + 2 (language toggle)  ← parallel
→ 3 (home) + 4 (bookmark)                     ← parallel
→ 5 (artifact discovery)
→ 6 (camera capture)
→ 7 (memory wizard) ─→ 8 (AI reflection slots in)
→ 9 (memory library)
```

---

## 3. Module / package structure

Single-module app. Inside `frontend/mobile/app/src/main/java/com/mcis/memoir/`:

```
com/mcis/memoir/
├── MemoirApplication.kt        # Application subclass; Koin startKoin
├── MainActivity.kt
├── MyAppNavigation.kt          # Nav3 type-safe routes (rewritten)
├── di/
│   ├── AppModule.kt            # application-scope singletons
│   ├── DataModule.kt           # repositories / DataStore / Room
│   ├── NetworkModule.kt        # OpenAI client (host = DeepSeek)
│   └── ViewModelModule.kt      # viewModel {} definitions
├── data/
│   ├── content/                # replaces MockData
│   │   ├── ContentRepository.kt
│   │   ├── ContentAssetLoader.kt
│   │   └── model/
│   │       ├── LocalizedText.kt
│   │       ├── Route.kt
│   │       ├── Spot.kt
│   │       ├── Artifact.kt
│   │       └── PhotographyTip.kt
│   ├── memory/
│   │   ├── MemoryEntity.kt
│   │   ├── MemoryDao.kt
│   │   ├── MemoryDatabase.kt
│   │   └── MemoryRepository.kt
│   ├── bookmark/
│   │   └── BookmarkRepository.kt
│   ├── prefs/
│   │   └── UserPreferencesRepository.kt
│   └── llm/
│       ├── ReflectionClient.kt
│       └── PromptBuilder.kt
├── ui/
│   ├── screens/                # all existing screens move here; one Screen + one ViewModel per file pair
│   ├── components/
│   ├── icons/
│   └── theme/
└── domain/                     # cross-screen use cases only; expected to stay small or empty for MVP
```

**Migration moves** (in change #1):
- Existing root-package screen files (`HomeScreen.kt`, `RouteDetailScreen.kt`, etc.) → `ui/screens/`
- `data.MockData` → deleted
- `data.PreferenceManager` → replaced by `data.prefs.UserPreferencesRepository` (DataStore-backed)
- `data.RouteData.kt` → split into `data.content.model.*`

---

## 4. Data layer

### 4.1 `data/tainan-route/` layout (repo root)

```
data/tainan-route/
├── routes/
│   ├── sounds_of_temple.json
│   ├── sea_protection.json
│   ├── colonial_architecture.json
│   ├── brick_arches.json
│   └── faith_hidden.json
├── spots/
│   ├── grand_mazu.json
│   ├── grand_wumiao.json
│   ├── anping_kaitai.json
│   └── …
└── index.json
```

`index.json` lists every route id and every spot id so the loader knows what to scan without enumerating files at runtime (AssetManager listing is awkward).

### 4.2 JSON schemas

`routes/<id>.json`:
```json
{
  "id": "sounds_of_temple",
  "title":       {"en": "...", "zh": "..."},
  "category":    {"en": "Temples & Folk Beliefs", "zh": "宗教信仰"},
  "heroImage":   "sounds_of_temple",
  "description": {"en": "...", "zh": "..."},
  "tags":        ["temples", "folk-belief"],
  "journey": [
    {"order": 1, "spotId": "grand_mazu"},
    {"order": 2, "spotId": "grand_wumiao"},
    {"order": 3, "spotId": "heaven_temple"}
  ]
}
```

`spots/<id>.json`:
```json
{
  "id": "grand_mazu",
  "title":                  {"en": "...", "zh": "大天后宮"},
  "heroImage":              "grand_mazu_temple",
  "duration":               {"en": "20–30 mins", "zh": "20–30 分鐘"},
  "whyItMatters":           {"en": "...", "zh": "..."},
  "historicalContext":      {"en": "...", "zh": "..."},
  "architecturalFeatures":  {"en": "...", "zh": "..."},
  "modernUse":              {"en": "...", "zh": "..."},
  "facts": {
    "en": ["Main deity: Mazu", "..."],
    "zh": ["主祀：媽祖", "..."]
  },
  "photographyTips": [
    {"id": 1, "description": {"en": "...", "zh": "..."}, "image": "tip1"}
  ],
  "artifacts": [
    {"id": 1, "title": {...}, "description": {...}, "image": "dragon_pillar"}
  ]
}
```

`heroImage` / `image` values are **drawable resource names** (no extension); the loader resolves them via `Resources.getIdentifier(name, "drawable", pkg)`. Images stay in `app/src/main/res/drawable/` and benefit from the R class.

### 4.3 Gradle wiring

`frontend/mobile/app/build.gradle.kts`:

```kotlin
android {
    sourceSets["main"].assets.srcDirs(
        "src/main/assets",
        rootProject.file("../../data")   // repo-root data/ is bundled as assets
    )
}
```

Result: `data/tainan-route/...` ends up at `assets/tainan-route/...` inside the APK.

### 4.4 Kotlin models

```kotlin
@Serializable
data class LocalizedText(val en: String, val zh: String) {
    operator fun get(locale: Locale): String =
        if (locale.language == "zh") zh else en
}

@Serializable
data class Route(
    val id: String,
    val title: LocalizedText,
    val category: LocalizedText,
    val heroImage: String,
    val description: LocalizedText,
    val tags: List<String> = emptyList(),
    val journey: List<JourneyStop>
)

@Serializable data class JourneyStop(val order: Int, val spotId: String)

@Serializable
data class Spot(
    val id: String,
    val title: LocalizedText,
    val heroImage: String,
    val duration: LocalizedText,
    val whyItMatters: LocalizedText,
    val historicalContext: LocalizedText,
    val architecturalFeatures: LocalizedText,
    val modernUse: LocalizedText,
    val facts: LocalizedFacts,
    val photographyTips: List<PhotographyTip>,
    val artifacts: List<Artifact>
)

@Serializable data class LocalizedFacts(val en: List<String>, val zh: List<String>)
@Serializable data class PhotographyTip(val id: Int, val description: LocalizedText, val image: String)
@Serializable data class Artifact(val id: Int, val title: LocalizedText, val description: LocalizedText, val image: String)
```

### 4.5 Loader + Repository

```kotlin
class ContentAssetLoader(
    private val assets: AssetManager,
    private val json: Json
) {
    suspend fun load(): ContentSnapshot = withContext(Dispatchers.IO) {
        val index = assets.open("tainan-route/index.json").reader().use {
            json.decodeFromString<Index>(it.readText())
        }
        val routes = index.routes.map { id -> readJson<Route>("tainan-route/routes/$id.json") }
        val spots  = index.spots.map  { id -> readJson<Spot>( "tainan-route/spots/$id.json")  }
        ContentSnapshot(routes.associateBy { it.id }, spots.associateBy { it.id })
    }
    @Serializable private data class Index(val routes: List<String>, val spots: List<String>)
}

class ContentRepository(loader: ContentAssetLoader, scope: CoroutineScope) {
    private val snapshot: Deferred<ContentSnapshot> = scope.async(Dispatchers.IO) { loader.load() }

    // Content is immutable for the process lifetime, so this flow emits exactly once
    // then completes. `combine` callers must keep this in mind (it pairs fine with a
    // long-lived flow like a DataStore observation — the combined flow stays open as
    // long as at least one source flow is open).
    fun routes(): Flow<List<Route>> = flow { emit(snapshot.await().routes.values.toList()) }
    suspend fun route(id: String): Route? = snapshot.await().routes[id]
    suspend fun spot(id: String): Spot? = snapshot.await().spots[id]
}
```

`ContentRepository` is an application-scoped singleton; loading happens once on first access.

---

## 5. LLM client (DeepSeek)

### 5.1 Dependencies

`libs.versions.toml`:
```toml
[versions]
openai-kotlin = "4.0.1"
ktor = "3.0.0"

[libraries]
openai-client    = { module = "com.aallam.openai:openai-client", version.ref = "openai-kotlin" }
ktor-client-cio  = { module = "io.ktor:ktor-client-cio",         version.ref = "ktor"          }
```

### 5.2 API key (BuildConfig + local.properties)

`local.properties` (gitignored, already exists):
```
DEEPSEEK_API_KEY=sk-xxxxxxxxx
```

`app/build.gradle.kts`:
```kotlin
val localProps = java.util.Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use(::load)
}

android {
    defaultConfig {
        buildConfigField(
            "String",
            "DEEPSEEK_API_KEY",
            "\"${localProps["DEEPSEEK_API_KEY"] ?: ""}\""
        )
    }
    buildFeatures.buildConfig = true
}
```

**Risk acknowledgement**: APK ships with the key embedded. Decompile-recoverable. Acceptable for an academic demo where the APK is not publicly distributed; if the demo build leaks, rotate the key. Long-term mitigation (proxy server) is explicitly deferred.

### 5.3 Client wrapper

```kotlin
val networkModule = module {
    single {
        OpenAI(
            token = BuildConfig.DEEPSEEK_API_KEY,
            host  = OpenAIHost(baseUrl = "https://api.deepseek.com/v1/"),
            timeout = Timeout(socket = 60.seconds)
        )
    }
    single { ReflectionClient(get()) }
}

class ReflectionClient(private val openAI: OpenAI) {
    suspend fun generateReflection(input: JourneyReflectionInput): Result<String> = runCatching {
        val req = ChatCompletionRequest(
            model       = ModelId("deepseek-chat"),
            messages    = PromptBuilder.build(input),
            temperature = 0.8,
            maxTokens   = 500
        )
        openAI.chatCompletion(req).choices.first().message.content.orEmpty()
    }
}
```

### 5.4 Reflection input schema

The reflection is **journey-level** and the output target is **a social-media-ready caption** (Instagram / Threads), not a per-spot note.

```kotlin
data class JourneyReflectionInput(
    val locale: Locale,
    val routeId: String,
    val spotEntries: List<SpotEntry>,    // visited spots in order
    val overallMood: String?,             // user's overall feeling
    val userInsights: String,              // user's reflection text (心得)
    val postTripFeedback: String?,        // optional follow-up reflection (後續反饋)
    val templateStyle: String              // "postcard" / "diary" / "story"
)

data class SpotEntry(
    val spotId: String,
    val userNote: String?,                 // per-spot note / mood
    val photoCount: Int                    // hint to AI about depth at this stop
)
```

The full prompt template, temperature tuning, and token budgeting are finalized in change `ai-reflection-generation`, not here. This umbrella locks only the interface and the error model.

### 5.5 Error model

```kotlin
when (val r = client.generateReflection(input)) {
    is Result.Success -> // render r.value
    is Result.Failure -> when (val e = r.exceptionOrNull()) {
        is IOException                                -> // "Network error, retry"
        is OpenAIException -> when (e.statusCode) {
            401 -> // "API key invalid — contact dev"
            429 -> // "Rate limited — wait 30s"
            else -> // "Service unavailable"
        }
        else                                          -> // "Unexpected error"
    }
}
```

---

## 6. State management (MVI)

### 6.1 Per-screen triad

```kotlin
// State — immutable rendering data
data class HomeState(
    val cards: List<RouteCard> = emptyList(),
    val isLoading: Boolean = false,
    val query: String = "",
    val activeTags: Set<String> = emptySet(),
    val error: String? = null
)

// Intent — every action the UI can fire
sealed interface HomeIntent {
    data object Refresh : HomeIntent
    data class SearchChanged(val q: String) : HomeIntent
    data class FilterTagToggled(val tagId: String) : HomeIntent
    data class CardClicked(val routeId: String) : HomeIntent
    data class BookmarkToggled(val routeId: String) : HomeIntent
}

// Effect — one-shot side effects (not replayed on rotation)
sealed interface HomeEffect {
    data class NavigateToRoute(val routeId: String) : HomeEffect
    data class ShowSnackbar(val msg: String) : HomeEffect
}
```

### 6.2 ViewModel pattern

```kotlin
class HomeViewModel(
    private val contentRepo: ContentRepository,
    private val prefsRepo: UserPreferencesRepository,
    private val bookmarkRepo: BookmarkRepository
) : ViewModel() {

    private val _state   = MutableStateFlow(HomeState(isLoading = true))
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val _effects = Channel<HomeEffect>(Channel.BUFFERED)
    val effects: Flow<HomeEffect> = _effects.receiveAsFlow()

    init { observeContent() }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.SearchChanged    -> _state.update { it.copy(query = intent.q) }
            is HomeIntent.FilterTagToggled -> _state.update { it.toggleTag(intent.tagId) }
            is HomeIntent.CardClicked      -> emit(HomeEffect.NavigateToRoute(intent.routeId))
            is HomeIntent.BookmarkToggled  -> viewModelScope.launch { bookmarkRepo.toggle(intent.routeId) }
            HomeIntent.Refresh             -> observeContent()
        }
    }

    private fun emit(e: HomeEffect) { viewModelScope.launch { _effects.send(e) } }
    private fun observeContent() { /* combine repos → _state */ }
}

// State extension lives next to the State definition
private fun HomeState.toggleTag(tagId: String): HomeState =
    copy(activeTags = if (tagId in activeTags) activeTags - tagId else activeTags + tagId)
```

### 6.3 Compose binding

```kotlin
@Composable
fun HomeScreen(
    onNavigateToRoute: (String) -> Unit,
    snackbar: SnackbarHostState,
    viewModel: HomeViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { e ->
            when (e) {
                is HomeEffect.NavigateToRoute -> onNavigateToRoute(e.routeId)
                is HomeEffect.ShowSnackbar    -> snackbar.showSnackbar(e.msg)
            }
        }
    }
    HomeContent(state = state, onIntent = viewModel::onIntent)
}
```

### 6.4 Rules

- UI → ViewModel has **exactly one entry**: `onIntent(intent)`.
- State is read-only from UI (`collectAsStateWithLifecycle()`).
- Effects use a `Channel`-backed `Flow`, never `StateFlow` (don't replay navigation on rotation).
- Reducer logic lives entirely in ViewModel — Screens are dumb.
- **No MVI library** (Orbit, MVIKotlin). Hand-rolled is sufficient at this scale.

### 6.5 Optional base class

If 3+ ViewModels accrue noticeable boilerplate, extract:
```kotlin
abstract class MviViewModel<I, S, E>(initial: S) : ViewModel() {
    private val _state   = MutableStateFlow(initial); val state: StateFlow<S> = _state
    private val _effects = Channel<E>(Channel.BUFFERED); val effects = _effects.receiveAsFlow()
    fun updateState(f: (S) -> S) = _state.update(f)
    fun emitEffect(e: E) = viewModelScope.launch { _effects.send(e) }
    abstract fun onIntent(intent: I)
}
```
**Defer extraction** until the boilerplate is visible across multiple changes.

---

## 7. Persistence

### 7.1 DataStore Preferences

For simple key-value state observable as a Flow:

```kotlin
private val Context.dataStore by preferencesDataStore(name = "user_prefs")

class UserPreferencesRepository(private val context: Context) {
    private object Keys {
        val LANGUAGE          = stringPreferencesKey("language")            // "en" / "zh"
        val INTERESTS         = stringSetPreferencesKey("interests")
        val ONBOARDING_DONE   = booleanPreferencesKey("onboarding_done")
    }

    val language: Flow<String>                     = context.dataStore.data.map { it[Keys.LANGUAGE] ?: defaultLanguage() }
    val selectedInterests: Flow<Set<String>>      = context.dataStore.data.map { it[Keys.INTERESTS].orEmpty() }
    val onboardingDone: Flow<Boolean>             = context.dataStore.data.map { it[Keys.ONBOARDING_DONE] ?: false }

    suspend fun setLanguage(lang: String)          { context.dataStore.edit { it[Keys.LANGUAGE] = lang } }
    suspend fun setInterests(s: Set<String>)       { context.dataStore.edit { it[Keys.INTERESTS] = s } }
    suspend fun markOnboardingDone()               { context.dataStore.edit { it[Keys.ONBOARDING_DONE] = true } }
}

class BookmarkRepository(private val context: Context) {
    private val key = stringSetPreferencesKey("bookmarked_routes")
    val bookmarkedRouteIds: Flow<Set<String>> = context.dataStore.data.map { it[key].orEmpty() }
    suspend fun toggle(routeId: String) {
        context.dataStore.edit { p ->
            val cur = p[key].orEmpty().toMutableSet()
            if (!cur.add(routeId)) cur.remove(routeId)
            p[key] = cur
        }
    }
}
```

### 7.2 Room — Memories

```kotlin
@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey val id: String,                  // UUID v4
    val templateId: String,                       // "postcard" / "diary" / ...
    val routeId: String?,                          // null = free-form memory
    val title: String,
    val status: String,                            // "IN_PROGRESS" / "COMPLETED"
    val createdAt: Long,
    val updatedAt: Long,
    val photoLocalPaths: String,                   // JSON List<String>
    val spotNotes: String,                         // JSON Map<spotId, note>
    val overallMood: String?,
    val userInsights: String,
    val postTripFeedback: String?,
    val generatedReflection: String?,              // last LLM output (cacheable)
    val editorState: String?                       // JSON: sticker placements / crop / filter
)

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE status = :status ORDER BY updatedAt DESC")
    fun observeByStatus(status: String): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE id = :id")
    fun observe(id: String): Flow<MemoryEntity?>

    @Upsert suspend fun upsert(memory: MemoryEntity)
    @Query("DELETE FROM memories WHERE id = :id") suspend fun delete(id: String)
}

@Database(entities = [MemoryEntity::class], version = 1)
abstract class MemoryDatabase : RoomDatabase() { abstract fun memoryDao(): MemoryDao }
```

`MemoryRepository` owns the DAO and adapts to/from a domain `Memory` model (unfolding JSON columns).

### 7.3 Photo storage policy

When the user picks a photo via the system picker, copy it to **app-private storage** immediately at selection time (eager copy):

```
context.filesDir/memories/<memoryId>/photo_<index>.jpg
```

**Lifecycle**:
- A `Memory` row is created in Room with `status = IN_PROGRESS` when the user enters `MemoryTemplateScreen` and picks a template. The `memoryId` is assigned at that point.
- Photos are copied to `filesDir/memories/<memoryId>/` as the user selects them in `MemoryPhotoSelectionScreen`.
- If the user abandons the wizard (back-stack out, or explicit cancel from `MemoryEditScreen`), `MemoryRepository.delete(memoryId)` removes the DB row AND deletes `filesDir/memories/<memoryId>/` recursively. This cleanup runs in the wizard ViewModel's `onCleared()` if `status` is still `IN_PROGRESS` AND the user did not explicitly save.
- When the user completes `MemoryReflectionScreen` and saves, `status` transitions to `COMPLETED`.

Pros: photos cannot disappear when the user deletes them from the gallery; no `takePersistableUriPermission` lifecycle to manage.
Cons: storage cost; small risk of orphaned directories if the app process dies mid-wizard. Acceptable for MVP — a startup-time scan can sweep orphans (rows with status `IN_PROGRESS` older than 7 days are auto-deleted along with their dirs).

DB stores absolute file paths (`photoLocalPaths` JSON). UI loads via Coil from file path.

### 7.4 Migration

- Start at schema v1.
- Add-only column changes → Room AutoMigration.
- Breaking changes during MVP → `destructiveMigration()` (user has no precious data).

---

## 8. i18n

### 8.1 Two layers

| Layer | Mechanism |
|---|---|
| Chrome text (buttons, labels, placeholders) | Existing `res/values/strings.xml` + `res/values-zh/strings.xml`, accessed via `stringResource(R.string.X)`. |
| Content text (spot stories, route descriptions, photo tips) | Loaded from JSON, modeled as `LocalizedText`, accessed via `text[currentLocale]`. |

### 8.2 Language toggle

```kotlin
// LanguageViewModel.onLanguageChosen
viewModelScope.launch {
    prefsRepo.setLanguage(lang)                                                    // persist
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang)) // apply
}
```

`AppCompatDelegate.setApplicationLocales`:
- Triggers Activity re-creation automatically.
- Updates `LocalConfiguration.current.locales[0]` → both `stringResource()` and `LocalizedText[locale]` pick up correctly.
- Persists across reboots without us writing anything.

`AndroidManifest.xml`:
```xml
<application android:localeConfig="@xml/locales_config" ...>
```
`res/xml/locales_config.xml`:
```xml
<locale-config xmlns:android="http://schemas.android.com/apk/res/android">
    <locale android:name="en" />
    <locale android:name="zh" />
</locale-config>
```

---

## 9. Navigation (Nav3 type-safe routes)

`build.gradle.kts` already brings in `androidx.navigation3.ui`. Rewrite `MyAppNavigation.kt`:

```kotlin
@Serializable data object Splash
@Serializable data object Welcome
@Serializable data object LanguageSelection
@Serializable data object CultureInterest
@Serializable data object Home
@Serializable data object Saved
@Serializable data object Memories
@Serializable data class  RouteDetail(val routeId: String)
@Serializable data class  SpotIntro(val spotId: String)
@Serializable data class  SpotDetail(val spotId: String)
@Serializable data class  ArtifactDiscovery(val spotId: String, val artifactId: Int)
@Serializable data class  ArtifactDetail(val spotId: String, val artifactId: Int)
@Serializable data object CameraPreview
@Serializable data object MemoryTemplate
@Serializable data class  MemoryPhotoSelection(val templateId: String)
@Serializable data class  MemoryEdit(val memoryId: String)
@Serializable data class  MemoryReflection(val memoryId: String)
```

`MyAppNavigation` uses `NavDisplay` + `entryProvider { entry<RouteDetail> { args -> RouteDetailScreen(args.routeId, …) } }`.

**ID-only navigation**: never pass Parcelable domain objects through navigation. Screens receive an ID and ask their ViewModel to resolve via `ContentRepository`. Koin gets the ID through `SavedStateHandle`:

```kotlin
viewModel { (handle: SavedStateHandle) ->
    RouteDetailViewModel(
        routeId      = handle.toRoute<RouteDetail>().routeId,
        contentRepo  = get(),
        bookmarkRepo = get()
    )
}
```

---

## 10. Testing approach

| Layer | Tools | Scope | Priority |
|---|---|---|---|
| Unit — Repository | JUnit5, MockK, Turbine | Flow emissions, edge cases (empty, duplicate, parse failure) | **Required** |
| Unit — ViewModel | JUnit5, MockK, Turbine, `MainDispatcherRule` | Intent → State reducer, Effect emission, Repo wiring | **Required** |
| Unit — LLM client | MockK on `OpenAI` interface | `Result` wrapping, error categorization. No real network. | **Required** |
| Instrumented — Room | Room in-memory DB, AndroidJUnit4 | DAO queries, migration sanity | Required |
| UI — Compose | `createComposeRule()` | Happy-path screen render + click → effect | Sample coverage; don't chase 100% |
| E2E | — | — | **Skipped** (replaced by human demo) |
| Screenshot diff | — | — | **Skipped** |

**Real DeepSeek calls are never made in tests** to avoid bills and flakiness.

---

## 11. Build & CI

### 11.1 Build types

```kotlin
buildTypes {
    debug   { applicationIdSuffix = ".debug" }   // dev + prod APK can coexist on a device
    release { isMinifyEnabled = true; … }
}
```

No flavor split (no staging vs prod). Both build types read `DEEPSEEK_API_KEY` from `local.properties`.

### 11.2 GitHub Actions

`.github/workflows/mobile-ci.yml` (new):
```yaml
- run: echo "DEEPSEEK_API_KEY=${{ secrets.DEEPSEEK_API_KEY }}" >> frontend/mobile/local.properties
- run: cd frontend/mobile && ./gradlew :app:lintDebug :app:testDebugUnitTest :app:assembleDebug
```

The CI key may be a separate restricted DeepSeek key with low rate limit — sufficient for build smoke and not enough to run up bills.

---

## 12. Open risks & deferred work

| Risk / Item | Decision | Trigger to revisit |
|---|---|---|
| Embedded API key | Acceptable for academic demo | If demo APK leaks publicly, rotate; if app enters real distribution, add proxy |
| `destructiveMigration` for Room | Acceptable while no user owns precious data | Before first external release |
| No streaming for LLM output | Single-shot completion; UX is "Generating…" spinner | If perceived latency hurts demo |
| Podcast feature | Deferred — no audio assets | If team produces / sources audio |
| MVI base class | Hand-roll per VM until ≥ 3 VMs duplicate | When boilerplate becomes visible in 3rd VM |
| Real DeepSeek calls in CI | Mocked | Only revisit if integration regressions appear |

---

## 13. Glossary

- **Content** — text and image data describing routes, spots, artifacts (build-time, designer-authored).
- **Memory** — a user-authored journal entry combining photos, a template, per-spot notes, and an LLM-generated caption.
- **Reflection** — the LLM-generated, share-ready caption produced from a complete journey.
- **Artifact** — a specific cultural detail at a spot (e.g., dragon pillar, door-god painting).
- **Spot** — a place on a route (e.g., Grand Mazu Temple).
- **Route** — an ordered sequence of spots with a theme (e.g., "Sounds of Temple Tainan").

---

## 14. Approval & next step

Once this umbrella is signed off, the implementation proceeds change-by-change through OpenSpec (`openspec propose ...`), starting with change #1 (`tainan-route-content-pipeline`). Each change carries its own delta specs and tasks; this umbrella is referenced from each as the binding architectural context.
