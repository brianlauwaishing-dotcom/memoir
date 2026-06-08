## Context

Biggest change in the umbrella by file count and conceptual load: introduces Room, defines the photo file lifecycle, and rewires four screens into a wizard whose state lives in a single Room row. Consumes:
- `tainan-route-content-pipeline` change #1: nothing directly (wizard has no route content reads).
- `language-toggle` change #2: `LocaleController.currentLocale()`, `values-zh/strings.xml`.
- `home-discovery` change #3: MVI template, `TagCatalog` precedent for `TemplateCatalog`, JUnit5+MockK+Turbine stack.
- `route-bookmarking` change #4: factory pattern, `internal` extension cross-package precedent.
- `artifact-discovery-flow` change #5: MVI for screens with no business logic (`SavedViewModel` precedent).
- `artifact-photo-capture` change #6: indirect — camera-captured photos land in MediaStore and are pickable by the system Photo Picker.

**Current state**:
- `MemoryTemplateScreen.kt:48` reads `MockData.templates` directly.
- `MemoryPhotoSelectionScreen.kt:156-168` hardcodes a fake selection of 5 `R.drawable.*` resources when user taps "Add Photos".
- `MemoryEditScreen` + `MemoryReflectionScreen` per `references/guideline.md:16-17` are stubs.
- `MyAppNavigation.kt:33` has `var selectedPhotos by remember { mutableStateOf(listOf<Int>()) }` — lifted state of mock-photo resource ids, threaded through 3 destinations.
- `MyAppNavigation.kt:61,66` destination args use `templateId: String` and `photoResIds: List<Int>` — not `memoryId`.
- No Room in `libs.versions.toml`. No KSP plugin applied.
- No `MemoryRepository`. No `filesDir/memories/...` directory.
- `MemoirApplication.kt` (created by change #1) has `companion object { lateinit var content; lateinit var prefs }` (after change #2 added `prefs`). This change adds `memoryRepo`.

**Constraints**:
- Cannot precede change #1 (depends on `MemoirApplication`).
- Cannot precede change #2 (depends on `LocaleController`, `values-zh/`).
- Should follow change #5 to inherit the latest VM pattern.
- Test stack JUnit5+MockK+Turbine from change #3 carries.
- Room schema must be exact umbrella §7.2 verbatim.
- Photo lifecycle must implement umbrella §7.3 verbatim.

## Goals / Non-Goals

**Goals:**
1. Replace mock photo flow with real `PickMultipleVisualMedia` + eager copy to `filesDir/memories/<memoryId>/photo_<i>.jpg`.
2. Persist memory draft in Room with `status = "IN_PROGRESS"` on template selection; bump to `"COMPLETED"` on Reflection save.
3. Implement abandonment cleanup (`onCleared()` deletes IN_PROGRESS draft + photos) and orphan sweep at app start (7-day cutoff).
4. Migrate all four screens to MVI VMs and drop `selectedLanguage`/`_zh`-suffix.
5. Add the three-field Reflection UI (mood / insights / feedback) per umbrella §5.5.
6. Replace `MockData.templates` reads with `TemplateCatalog` (code-resident).
7. Rewire `MyAppNavigation` to memoryId-keyed navigation per umbrella §7.3.
8. Keep `MockData` compiling (per multi-change coexistence rule).
9. Demonstrate Room in-memory testing via `androidx.room:room-testing`.

**Non-Goals:**
- Real editor (sticker / filter / font / stamp): all stubbed.
- AI text generation (change #8).
- `MemoriesScreen` migration to Room reads (change #9).
- `spotNotes` per-spot text UI.
- `editorState` JSON content.
- Photo reorder.
- Camera-capture integration (the captured photo is reachable via the system photo picker; no special wiring).
- Coil dependency (using `BitmapFactory` + `rememberAsyncImagePainter`-like Composable suffices; Coil deferred until a real perf win is demonstrated).
- Per-locale text storage (single text-blob fields).
- Memory share / duplicate / delete UI (change #9).

## Decisions

### D1. Room schema verbatim umbrella §7.2

```kotlin
@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey val id: String,            // UUID v4
    val templateId: String,
    val routeId: String? = null,            // null = free-form memory; unused in this change
    val title: String,                       // default = template title resolved at creation time per the active locale (D13)
    val status: String,                      // "IN_PROGRESS" / "COMPLETED"
    val createdAt: Long,
    val updatedAt: Long,
    val photoLocalPaths: String,             // JSON List<String>, relative to filesDir
    val spotNotes: String,                   // JSON Map<spotId, note>, "{}" until later change
    val overallMood: String? = null,
    val userInsights: String = "",
    val postTripFeedback: String? = null,
    val generatedReflection: String? = null,
    val editorState: String? = null
)
```

`status` is a `String` not enum — Room ↔ enum codec ceremony isn't worth the safety here. A small `object MemoryStatus { const val IN_PROGRESS = "IN_PROGRESS"; const val COMPLETED = "COMPLETED" }` provides type-checked access at call sites.

Database is `@Database(entities = [MemoryEntity::class], version = 1, exportSchema = false)`. Schema export disabled because we use `destructiveMigration()` per umbrella §7.4.

### D2. Repository unfolds/folds JSON columns at the boundary

```kotlin
data class Memory(
    val id: String,
    val templateId: String,
    val routeId: String?,
    val title: String,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
    val photoRelativePaths: List<String>,
    val spotNotes: Map<String, String>,
    val overallMood: String?,
    val userInsights: String,
    val postTripFeedback: String?,
    val generatedReflection: String?,
    val editorState: String?
)

class MemoryRepository(
    private val dao: MemoryDao,
    private val filesDir: File,
    private val json: Json,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun startDraft(templateId: String, defaultTitle: String): String = withContext(ioDispatcher) {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        dao.upsert(MemoryEntity(
            id = id,
            templateId = templateId,
            routeId = null,
            title = defaultTitle,
            status = MemoryStatus.IN_PROGRESS,
            createdAt = now,
            updatedAt = now,
            photoLocalPaths = "[]",
            spotNotes = "{}"
        ))
        id
    }

    fun observe(id: String): Flow<Memory?> = dao.observe(id).map { it?.toDomain(json) }

    suspend fun addPhoto(memoryId: String, sourceUri: Uri, contentResolver: ContentResolver): Result<String> = …
    suspend fun removePhoto(memoryId: String, index: Int): Result<Unit> = …
    suspend fun updateReflection(memoryId: String, mood: String?, insights: String, feedback: String?) = …
    suspend fun complete(memoryId: String) = …
    suspend fun cancelDraftIfInProgress(memoryId: String) = …
    suspend fun sweepOrphans() = …

    private fun MemoryEntity.toDomain(json: Json): Memory = Memory(
        id = id, templateId = templateId, routeId = routeId, title = title, status = status,
        createdAt = createdAt, updatedAt = updatedAt,
        photoRelativePaths = json.decodeFromString(photoLocalPaths),
        spotNotes = json.decodeFromString(spotNotes),
        overallMood = overallMood, userInsights = userInsights, postTripFeedback = postTripFeedback,
        generatedReflection = generatedReflection, editorState = editorState
    )
}
```

`Json` is shared with `ContentJson` from change #1 — both consumers want `ignoreUnknownKeys = true`.

### D3. `addPhoto` lifecycle

```kotlin
suspend fun addPhoto(
    memoryId: String,
    sourceUri: Uri,
    contentResolver: ContentResolver
): Result<String> = withContext(ioDispatcher) {
    // All validation goes through runCatching so EVERY failure path produces
    // Result.failure(...) per the spec contract (no synchronous throws).
    runCatching {
        require(memoryId.isValidUuid()) { "memoryId must be a UUID" }
        val current = dao.getOnce(memoryId)
            ?: error("memory not found: $memoryId")
        val currentPaths: List<String> = json.decodeFromString(current.photoLocalPaths)
        val index = currentPaths.size
        val rel = "memories/$memoryId/photo_$index.jpg"
        val dest = File(filesDir, rel)
        dest.parentFile?.mkdirs()
        contentResolver.openInputStream(sourceUri).use { input ->
            FileOutputStream(dest).use { output ->
                input?.copyTo(output) ?: error("openInputStream returned null")
            }
        }
        val next = currentPaths + rel
        dao.upsert(current.copy(
            photoLocalPaths = json.encodeToString(next),
            updatedAt = System.currentTimeMillis()
        ))
        rel
    }
}
```

**`dao.getOnce(id)` not `dao.observe(id).first()`**: a `Flow<MemoryEntity?>` from Room can suspend indefinitely depending on subscription model; the read-modify-write path here wants a single suspend lookup. Add `@Query("SELECT * FROM memories WHERE id = :id") suspend fun getOnce(id: String): MemoryEntity?` to `MemoryDao`. The Flow-based `observe(id)` is retained for UI consumers (Photo / Edit / Reflection VMs need live updates).

**Why eager-copy here vs in the VM:**
- Repository owns the storage contract; VM never touches `filesDir` paths.
- Path safety (`isValidUuid` check) is centralized.
- Testable with a temp `filesDir` and a mocked `ContentResolver`.

`isValidUuid()` is a strict `Regex("^[0-9a-fA-F\\-]{36}$")` match. Defense-in-depth against a future bug that constructs a malicious path.

### D4. `cancelDraftIfInProgress` recursive delete with path validation

```kotlin
suspend fun cancelDraftIfInProgress(memoryId: String) = withContext(ioDispatcher) {
    if (!memoryId.isValidUuid()) return@withContext   // silent no-op on bad input — fire-and-forget caller can't react anyway
    val row = dao.getOnce(memoryId) ?: return@withContext
    if (row.status != MemoryStatus.IN_PROGRESS) return@withContext
    val dir = File(filesDir, "memories/$memoryId")
    if (dir.exists() && dir.startsWith(File(filesDir, "memories"))) {
        dir.deleteRecursively()
    }
    dao.delete(memoryId)
}
```

The `startsWith(File(filesDir, "memories"))` check is the redundant safety: combined with the UUID regex, a malicious `memoryId` can't escape the `memories/` subdirectory.

### D5. Orphan sweep on app start

```kotlin
suspend fun sweepOrphans() = withContext(ioDispatcher) {
    val cutoff = System.currentTimeMillis() - SEVEN_DAYS_MS
    val orphans = dao.findStaleInProgress(cutoff)
    for (row in orphans) {
        val dir = File(filesDir, "memories/${row.id}")
        if (dir.exists() && dir.startsWith(File(filesDir, "memories"))) {
            dir.deleteRecursively()
        }
        dao.delete(row.id)
    }
}

@Query("SELECT * FROM memories WHERE status = 'IN_PROGRESS' AND updatedAt < :cutoff")
suspend fun findStaleInProgress(cutoff: Long): List<MemoryEntity>
```

`SEVEN_DAYS_MS = 7L * 24 * 60 * 60 * 1000`. Hardcoded constant — making it configurable adds complexity for no demonstrated benefit.

`MemoirApplication.onCreate()` invokes `appScope.launch { memoryRepo.sweepOrphans() }` — fire-and-forget on the supervisor scope; if it fails, the app continues.

### D6. ViewModel `onCleared()` cleanup uses a Repository-owned supervisor scope

`viewModelScope` is cancelled before `onCleared()` runs, so suspending Repository calls launched from `onCleared()` won't complete. Instead, `MemoryRepository` exposes a fire-and-forget API:

```kotlin
class MemoryRepository(...) {
    private val cleanupScope = CoroutineScope(SupervisorJob() + ioDispatcher)

    fun fireCancelDraftIfInProgress(memoryId: String) {
        cleanupScope.launch { cancelDraftIfInProgress(memoryId) }
    }
}
```

VM `onCleared()`:
```kotlin
override fun onCleared() {
    super.onCleared()
    if (!completionConfirmed) {
        repo.fireCancelDraftIfInProgress(memoryId)
    }
}
```

The `completionConfirmed` flag is set to `true` inside `ReflectionViewModel.SaveClicked` reducer right before navigation. Without this flag, every wizard back-out triggers cleanup; with it, only abandonment paths clean up.

**Why a Repository-owned scope vs `GlobalScope.launch`:**
- `GlobalScope` is discouraged in modern Kotlin guidance (lint warning, test pollution).
- A named supervisor scope owned by the Repository is testable (inject a `TestScope` in tests; capture launches with `runCurrent()`).

### D7. `PickMultipleVisualMedia` integration

```kotlin
@Composable
fun MemoryPhotoSelectionScreen(viewModel: PhotoSelectionViewModel, ...) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.onIntent(PhotoSelectionIntent.PhotosPicked(uris))
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { e -> when (e) {
            is PhotoSelectionEffect.LaunchPicker -> launcher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
            is PhotoSelectionEffect.NavigateToEdit -> onNavigateToEdit(e.memoryId)
        }}
    }

    // AddPhotosBox onClick = { viewModel.onIntent(PhotoSelectionIntent.AddPhotosClicked) }
    // VM emits LaunchPicker effect; LaunchedEffect handles it
}
```

**Why launcher in Composable scope, effect in VM:**
- `rememberLauncherForActivityResult` MUST live in Composable scope (it ties to the Activity result registry).
- VM cannot directly launch the picker — but it can emit an effect saying "please launch the picker", which the Composable's effect collector turns into the actual `launcher.launch(...)` call.
- Trade-off: one indirection. Worth it because the VM stays testable without `rememberLauncherForActivityResult` mocking.

The 5-cap is documented in `PickMultipleVisualMedia(maxItems = 5)`. The VM also defends-in-depth: `PhotosPicked` reducer takes only the first `5 - currentPhotoCount` uris if the user somehow picks more.

### D8. `TemplateCatalog`

```kotlin
object TemplateCatalog {
    val all: List<Template> = listOf(
        Template("old_street", R.string.template_old_street, R.string.template_old_street_desc,
                 R.drawable.memory_templete_1, R.drawable.memory_templete_1_mask,
                 slots = listOf(/* same 5 slots as MockData.templates[0] */)),
        // ... 3 more
    )
    val ids: Set<String> = all.map { it.id }.toSet()
    fun byId(id: String): Template? = all.firstOrNull { it.id == id }
}

data class Template(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    @DrawableRes val imageRes: Int,
    @DrawableRes val maskRes: Int,
    val slots: List<TemplateSlot>
)

data class TemplateSlot(val x: Float, val y: Float, val width: Float, val height: Float, val rotation: Float)
```

Strings `template_old_street`, `template_city_walk`, `template_taiwan_pop`, `template_heritage_arch` + their `_desc` siblings + `values-zh/` mirrors added. Existing `MockData.templates` is NOT deleted — coexistence.

### D9. Wizard navigation contract

```kotlin
data object MemoryTemplateDestination
data class  MemoryPhotoSelectionDestination(val memoryId: String)    // was templateId
data class  MemoryEditDestination(val memoryId: String)                 // was (templateId, photoResIds)
data class  MemoryReflectionDestination(val memoryId: String)           // was data object
```

The `MyAppNavigation` Compose state `var selectedPhotos by remember { mutableStateOf(listOf<Int>()) }` is DELETED. Photos live in Room.

### D10. Photo rendering in `MemoryEditScreen` without Coil

```kotlin
@Composable
fun FilePhoto(relativePath: String, filesDir: File, modifier: Modifier = Modifier) {
    val bitmap = remember(relativePath) {
        val f = File(filesDir, relativePath)
        if (f.exists()) BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap() else null
    }
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop)
    } else {
        Box(modifier.background(Color.LightGray))
    }
}
```

**Why not Coil:**
- Coil adds ~250 KB to the APK + a dependency to manage.
- Edit screen shows ≤5 photos at known small sizes (template slots are ~200dp×200dp).
- `BitmapFactory.decodeFile` synchronous decode on Compose's main-thread `remember` block is acceptable at this scale; if profiling shows jank, switch to Coil in a follow-up.

If a stale path appears (photo file deleted out-of-band), the gray placeholder shows. Acceptable degradation.

### D11. ReflectionViewModel save flow

```kotlin
class ReflectionViewModel(
    private val memoryId: String,
    private val repo: MemoryRepository
) : ViewModel() {

    private var completionConfirmed = false

    fun onIntent(intent: ReflectionIntent) {
        when (intent) {
            is ReflectionIntent.MoodChanged    -> _state.update { it.copy(overallMood = intent.text) }
            is ReflectionIntent.InsightsChanged -> _state.update { it.copy(userInsights = intent.text) }
            is ReflectionIntent.FeedbackChanged -> _state.update { it.copy(postTripFeedback = intent.text) }
            ReflectionIntent.SaveClicked         -> viewModelScope.launch {
                _state.update { it.copy(isSaving = true) }
                val s = _state.value
                runCatching {
                    repo.updateReflection(memoryId, s.overallMood.ifBlank { null }, s.userInsights, s.postTripFeedback.ifBlank { null })
                    repo.complete(memoryId)
                }.fold(
                    onSuccess = {
                        completionConfirmed = true
                        _effects.send(ReflectionEffect.NavigateToMemoriesList)
                    },
                    onFailure = { e ->
                        _state.update { it.copy(isSaving = false, error = e.message) }
                    }
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (!completionConfirmed) repo.fireCancelDraftIfInProgress(memoryId)
    }
}
```

The `EditViewModel` and `PhotoSelectionViewModel` have the same `onCleared()` shape (without `completionConfirmed` since they never complete — only Reflection does).

**Why mood/feedback `.ifBlank { null }`:**
- Empty strings vs null carry different semantics. The schema marks `overallMood` / `postTripFeedback` as nullable; blank input is treated as "user skipped this field", not "user typed empty".

### D12. Test approach

Six new tests:
- `MemoryRepositoryTest` — Room in-memory DB (`Room.inMemoryDatabaseBuilder(...)`), real `filesDir` via `Files.createTempDirectory()`. Covers: `startDraft` creates row; `addPhoto` copies file and updates row; `cancelDraftIfInProgress` deletes row + dir; `cancelDraftIfInProgress` no-op on COMPLETED row; `sweepOrphans` deletes only stale IN_PROGRESS.
- `PhotoLifecycleTest` — focused on the file-copy + rollback paths.
- `OrphanSweepTest` — focused on the 7-day cutoff math.
- Four VM tests — JUnit5+MockK+Turbine, mock `MemoryRepository` interface.
- `TemplateCatalogTest` — assert no duplicate ids, all string/drawable refs resolve via Kotlin reflection (same as `TagCatalog`).

`MemoryRepository` is declared as an `interface` with `DataStoreMemoryRepository` (wait, name is wrong — let's call it `RoomMemoryRepository`) implementing. Same `internal` extension pattern as `Route.toCard`.

### D13. Title default at draft creation

`startDraft(templateId, defaultTitle)` — `defaultTitle` is the template's locale-resolved title at the time of creation. This means a memory created under en will have an English title even if the user later switches locale; acceptable. Title editing UI is deferred to change #9.

## Risks / Trade-offs

- **`onCleared()` race with Reflection save**: if the user taps Save and the system reclaims the VM before `completionConfirmed = true` is set (extremely rare; Save coroutine takes microseconds), cleanup would delete the just-completed memory. Mitigation: set `completionConfirmed = true` BEFORE the `complete()` call returns, by wrapping in a synchronous check.
- **`PickMultipleVisualMedia` Uri lifespan**: documented as short-lived; eager-copy mitigates. If the user backgrounds the app DURING the copy, the InputStream may close. Repository's `Result.failure` path handles this; VM shows error.
- **Recursive `deleteRecursively()` on a malicious path**: UUID regex + `startsWith(filesDir/memories)` are belt-and-suspenders.
- **Room schema v1 with `destructiveMigration()`**: pre-public-MVP wipes any prior test data. Acceptable.
- **`MemoryRepository.cleanupScope` lifetime is process-wide**: a SupervisorJob never cancelled. Memory leak risk is theoretical (no captured Activity refs). Acceptable.
- **`BitmapFactory.decodeFile` blocks Compose recomposition**: at ~5 small photos, the decode is fast. If it shows up, swap to a coroutine-loaded ImageBitmap state.
- **Multi-wizard concurrent draft drafts**: if user starts a draft, backs out, starts another draft, the first draft's cleanup fires after the second draft is in flight. Each draft has a distinct memoryId so they don't collide.
- **`MemoriesScreen` still reads `MockData.memories`** until change #9 lands. The Memories list won't show real Room data until then; verify with the implementing PR that this gap is documented in the PR description.

## Migration Plan

1. Add Room + KSP dependencies + `activity-compose` BOM update.
2. Create `MemoryEntity` / `MemoryStatus` / `MemoryDao` / `MemoryDatabase`.
3. Create `MemoryRepository` interface + `RoomMemoryRepository` impl + `Memory` domain type.
4. Wire `MemoirApplication.onCreate()` to construct `memoryRepo` and invoke `sweepOrphans()`.
5. Create `TemplateCatalog` + 4 new strings + values-zh mirrors.
6. Create 4 wizard VMs + 4 factories + state/intent/effect files.
7. Rewrite the 4 wizard screens.
8. Rewrite the 4 MyAppNavigation entries; delete `selectedPhotos` lifted state; change destination signatures.
9. Tests.
10. Emulator smoke.

**Rollback**: revert the change commit. `MockData.templates` reads return. Room database file remains on device but ignored (next test rerun clears via `pm clear`).

## Open Questions

- **Should `MemoryRepository` interface live in the same module as the impl, or in a separate `data-memory` module?** No multi-module split today; keep in `:app`. Re-evaluate if Compose modules emerge.
- **Should photo paths store the full absolute path instead of relative?** Per umbrella §7.3 last paragraph, relative paths are preferred (testability + backup/restore). Stick with relative.
- **Should `MemoriesScreen` consume `MemoryRepository.observeAll()` Flow in this change, even though change #9 owns the UI?** No — keeps the file diff scoped. The Flow is exposed (`observeAll(status): Flow<List<Memory>>`); change #9 wires the consumer.
- **What happens if the user switches locale mid-wizard?** The memory's `title` was resolved at creation time and persisted; subsequent locale switches don't retroactively change it. Acceptable for MVP.
- **Default title format**: the template's locale-resolved title alone, OR concatenated with a date? Pick just the title for now (`title = template.title[locale]`); change #9 may add editing UI.
