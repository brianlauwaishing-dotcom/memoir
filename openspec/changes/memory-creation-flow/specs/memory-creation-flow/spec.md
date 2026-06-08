## ADDED Requirements

### Requirement: Memory persistence uses Room with the umbrella §7.2 schema

`com.mcis.memoir.data.memory.MemoryEntity` SHALL declare exactly the fields enumerated in umbrella §7.2 (`id`, `templateId`, `routeId`, `title`, `status`, `createdAt`, `updatedAt`, `photoLocalPaths`, `spotNotes`, `overallMood`, `userInsights`, `postTripFeedback`, `generatedReflection`, `editorState`). `status` SHALL be a String with the only two valid values `"IN_PROGRESS"` and `"COMPLETED"`, available via `MemoryStatus` constants. The Room `@Database` MUST be version 1 with `exportSchema = false` and use destructive migration for any future schema change during MVP.

#### Scenario: Inserting and reading back a draft preserves all fields
- **WHEN** a test inserts a `MemoryEntity` with all fields populated and queries it back via `MemoryDao.observe(id).first()`
- **THEN** every field round-trips byte-identical, AND the inserted row appears in `observeAll().first()` and in `observeByStatus("IN_PROGRESS").first()`

#### Scenario: Status field accepts only IN_PROGRESS or COMPLETED
- **WHEN** code references `MemoryStatus.IN_PROGRESS` and `MemoryStatus.COMPLETED`
- **THEN** both resolve to string constants `"IN_PROGRESS"` and `"COMPLETED"` respectively, AND no other status value is written by this change

### Requirement: Template selection assigns a memoryId and creates an IN_PROGRESS draft

`MemoryRepository.startDraft(templateId, defaultTitle)` SHALL generate a fresh UUID v4, insert a new `MemoryEntity` with `status = "IN_PROGRESS"`, `createdAt = updatedAt = System.currentTimeMillis()`, `photoLocalPaths = "[]"`, `spotNotes = "{}"`, and return the assigned `memoryId`. `MemoryTemplateViewModel.TemplateClicked` SHALL invoke `startDraft(...)` and emit `NavigateToPhotoSelection(memoryId)`.

#### Scenario: Tapping a template creates a draft row
- **WHEN** the user taps the "Old Street Journal" template on `MemoryTemplateScreen`
- **THEN** within one VM coroutine tick, `MemoryRepository.startDraft("old_street", localeResolvedTitle)` is invoked exactly once, AND a new row appears in the Room DB with the returned `memoryId` and `status = "IN_PROGRESS"`, AND the effects Flow emits `MemoryTemplateEffect.NavigateToPhotoSelection(memoryId)`

#### Scenario: memoryId is a UUID-shape string parseable by java.util.UUID
- **WHEN** `startDraft(...)` is invoked
- **THEN** the returned id is a 36-character UUID-shape string (regex `^[0-9a-fA-F\\-]{36}$`) AND `UUID.fromString(id)` does not throw — the production implementation MUST use `UUID.randomUUID()` (which generates a v4 UUID), but the spec asserts only the parseable-shape contract because the path-safety check in `addPhoto`/`cancelDraftIfInProgress` is regex-based and version-agnostic

### Requirement: PhotoSelectionViewModel launches PickMultipleVisualMedia capped at 5

`PhotoSelectionViewModel.AddPhotosClicked` SHALL emit `PhotoSelectionEffect.LaunchPicker(maxItems = 5)`. The Composable collector SHALL invoke `ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)` via `rememberLauncherForActivityResult`. The reducer for `PhotosPicked(uris)` SHALL clip the input list to `(5 - currentPhotoCount)` entries before invoking `MemoryRepository.addPhoto(...)` for each.

#### Scenario: AddPhotos click emits a LaunchPicker effect
- **WHEN** the screen state has `photoPaths.size == 0` and the user taps the Add Photos box
- **THEN** the effects Flow emits exactly one `PhotoSelectionEffect.LaunchPicker(maxItems = 5)`

#### Scenario: User picks 3 photos with 2 already selected — only 2 are accepted
- **WHEN** the screen state has `photoPaths.size == 3` and the picker returns 4 Uris
- **THEN** `MemoryRepository.addPhoto(memoryId, uri, ...)` is invoked exactly 2 times (clipped to `5 - 3 = 2`), AND the state's `photoPaths.size == 5` after

<!-- The PickVisualMediaRequest media-type assertion is verifiable only via an instrumented Compose UI test against the launcher — not realistic in this change's JVM-only unit test scope. The contract is enforced at code-review time on the Composable's `launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))` call site; no scenario tests it. -->

#### Scenario: Picker launcher uses ImageOnly media type (code-review contract)
- **WHEN** the source of `MemoryPhotoSelectionScreen.kt` is inspected after this change lands
- **THEN** the file contains exactly one `launcher.launch(PickVisualMediaRequest(...))` call site whose argument literal is `ActivityResultContracts.PickVisualMedia.ImageOnly` — verifiable by `grep -nF 'PickVisualMedia.ImageOnly' MemoryPhotoSelectionScreen.kt` returning a non-empty match

### Requirement: Photos are eager-copied to filesDir at picker selection time

`MemoryRepository.addPhoto(memoryId, sourceUri, contentResolver)` SHALL copy the source Uri's bytes to `filesDir/memories/<memoryId>/photo_<n>.jpg` where `<n>` is the next free index, AND update the row's `photoLocalPaths` JSON with the relative path `"memories/<memoryId>/photo_<n>.jpg"`, AND bump `updatedAt`. The copy MUST be performed on `Dispatchers.IO`.

#### Scenario: First photo lands at photo_0.jpg
- **WHEN** the test calls `addPhoto(memoryId, sourceUri, contentResolver)` against an empty draft and `sourceUri` resolves to 100 bytes of JPEG data
- **THEN** the file at `filesDir/memories/<memoryId>/photo_0.jpg` exists and is 100 bytes, AND the row's `photoLocalPaths` JSON parses to `["memories/<memoryId>/photo_0.jpg"]`

#### Scenario: Second photo lands at photo_1.jpg
- **WHEN** the test calls `addPhoto(...)` a second time against the same draft
- **THEN** `filesDir/memories/<memoryId>/photo_1.jpg` exists, AND the row's `photoLocalPaths` parses to a 2-element list with both relative paths in order

#### Scenario: Invalid memoryId is rejected via Result.failure (no synchronous throw)
- **WHEN** the test calls `addPhoto("../etc/passwd", sourceUri, contentResolver)`
- **THEN** the call returns `Result.failure(cause)` where `cause is IllegalArgumentException` (validation runs inside `runCatching { ... }` so callers always observe a `Result`, never a thrown exception), AND no file is written to `filesDir`, AND no DB row is updated

### Requirement: Abandonment cleanup deletes IN_PROGRESS draft and its photo directory

`MemoryRepository.cancelDraftIfInProgress(memoryId)` SHALL delete the DB row AND recursively delete `filesDir/memories/<memoryId>/` if and only if the row's `status == "IN_PROGRESS"`. The recursive delete MUST be guarded by both a UUID-shape check and a `startsWith(filesDir/memories)` containment check.

#### Scenario: Cancel of IN_PROGRESS draft removes row and files
- **WHEN** a test creates a draft, calls `addPhoto(...)` once (so 1 file exists), then calls `cancelDraftIfInProgress(memoryId)`
- **THEN** the DB row no longer exists, AND `filesDir/memories/<memoryId>/` does not exist on disk

#### Scenario: Cancel of COMPLETED memory is a no-op
- **WHEN** a test calls `cancelDraftIfInProgress(memoryId)` against a row whose `status == "COMPLETED"`
- **THEN** the DB row still exists, AND any files under `filesDir/memories/<memoryId>/` still exist

#### Scenario: VM onCleared() triggers cleanup unless completionConfirmed
- **WHEN** a `PhotoSelectionViewModel` is created against a draft and then `viewModel.onCleared()` fires (e.g. by the navigator popping the screen)
- **THEN** `MemoryRepository.fireCancelDraftIfInProgress(memoryId)` is invoked exactly once

#### Scenario: Reflection save sets completionConfirmed before navigation
- **WHEN** a `ReflectionViewModel` processes `SaveClicked` successfully and then `onCleared()` fires
- **THEN** `fireCancelDraftIfInProgress(memoryId)` is NOT invoked from `onCleared()` — the just-completed memory is preserved

### Requirement: Startup orphan sweep removes stale IN_PROGRESS drafts

`MemoryRepository.sweepOrphans()` SHALL be invoked once per `MemoirApplication.onCreate()`. It SHALL delete every `MemoryEntity` row where `status == "IN_PROGRESS" AND updatedAt < now - 7 days`, plus the corresponding `filesDir/memories/<id>/` directory.

#### Scenario: Stale IN_PROGRESS row is swept
- **WHEN** a test inserts a draft with `updatedAt = System.currentTimeMillis() - 8 * 24 * 60 * 60 * 1000` and calls `sweepOrphans()`
- **THEN** the DB row no longer exists, AND its photo directory no longer exists

#### Scenario: Recent IN_PROGRESS row is preserved
- **WHEN** a test inserts a draft with `updatedAt = System.currentTimeMillis() - 1 * 60 * 60 * 1000` (1 hour old) and calls `sweepOrphans()`
- **THEN** the DB row still exists

#### Scenario: COMPLETED row is never swept regardless of age
- **WHEN** a test inserts a 30-day-old row with `status == "COMPLETED"` and calls `sweepOrphans()`
- **THEN** the row still exists

### Requirement: Reflection save persists three text fields and bumps status to COMPLETED

`ReflectionViewModel.SaveClicked` SHALL invoke `MemoryRepository.updateReflection(memoryId, mood, insights, feedback)` followed by `MemoryRepository.complete(memoryId)`. Empty / blank `mood` and `feedback` SHALL be persisted as `null` (not the empty string); empty `insights` SHALL be persisted as the empty string (per umbrella §5.5 — insights is required, the others are optional).

#### Scenario: SaveClicked persists all three fields and completes the draft
- **WHEN** the screen state is `ReflectionState(overallMood = "excited", userInsights = "...", postTripFeedback = "next time...", isSaving = false)` and the VM receives `SaveClicked`
- **THEN** `updateReflection(memoryId, "excited", "...", "next time...")` is invoked exactly once, AND `complete(memoryId)` is invoked exactly once, AND the row's `status` becomes `"COMPLETED"`, AND the effects Flow emits `NavigateToMemoriesList`

#### Scenario: Empty mood and feedback are stored as null
- **WHEN** the state is `ReflectionState(overallMood = "", userInsights = "some text", postTripFeedback = "")` and `SaveClicked` is fired
- **THEN** `updateReflection(memoryId, null, "some text", null)` is invoked — empty strings become null because they semantically mean "user skipped this field"

#### Scenario: No LLM call is made
- **WHEN** the user taps the "Polish with AI" button or the "Save" button on `MemoryReflectionScreen`
- **THEN** no network request goes out via any HTTP client, AND no DeepSeek API key is read from `BuildConfig`, AND the `generatedReflection` field of the saved row remains `null`

### Requirement: TemplateCatalog is the single source of truth for memory templates in this change

`com.mcis.memoir.ui.memory.template.TemplateCatalog` SHALL be a code-resident object declaring the four templates (Old Street, City Walk, Taiwan Pop, Heritage Architecture) with `@StringRes titleRes` / `descriptionRes`, `@DrawableRes imageRes` / `maskRes`, and a list of `TemplateSlot` rectangles per template. `MemoryTemplateScreen` MUST consume `TemplateCatalog.all` — NOT `MockData.templates`.

#### Scenario: TemplateCatalog has four distinct templates
- **WHEN** the test inspects `TemplateCatalog.all`
- **THEN** the list has exactly 4 entries, AND `TemplateCatalog.ids.size == 4` (no duplicate ids)

#### Scenario: MemoryTemplateScreen does not import MockData
- **WHEN** the source of `MemoryTemplateScreen.kt` is inspected after this change lands
- **THEN** the file does not import `com.mcis.memoir.data.MockData` AND its function signature has no `selectedLanguage` parameter

### Requirement: Wizard navigation is keyed by memoryId throughout

`MyAppNavigation` SHALL declare the wizard destinations as: `MemoryTemplateDestination` (no args), `MemoryPhotoSelectionDestination(memoryId: String)`, `MemoryEditDestination(memoryId: String)`, `MemoryReflectionDestination(memoryId: String)`. The legacy `templateId: String` and `photoResIds: List<Int>` args SHALL be removed. The `var selectedPhotos by remember { mutableStateOf(listOf<Int>()) }` lifted Compose state in `MyAppNavigation` SHALL be deleted.

#### Scenario: Navigation arguments are memoryId only
- **WHEN** the source of `MyAppNavigation.kt` is inspected after this change lands
- **THEN** `MemoryPhotoSelectionDestination`, `MemoryEditDestination`, and `MemoryReflectionDestination` each declare exactly one `memoryId: String` argument, AND no destination declares `templateId` or `photoResIds`, AND no `selectedPhotos` remember-state exists

#### Scenario: Each wizard entry constructs its VM via a Factory closing over memoryId
- **WHEN** the source of `MyAppNavigation.kt` is inspected after this change lands
- **THEN** each wizard destination entry's `viewModel(...)` call passes `factory = …Factory(memoryId = key.memoryId, ...)` with `viewModel(key = key.memoryId, ...)` distinguishing instances per memoryId

### Requirement: Wizard screens are migrated off selectedLanguage and _zh-suffix lookups

`MemoryTemplateScreen`, `MemoryPhotoSelectionScreen`, `MemoryEditScreen`, and `MemoryReflectionScreen` SHALL NOT declare `selectedLanguage: String` parameters. They SHALL NOT call `stringResource(R.string.X_zh)` or use any runtime `_zh`-suffix lookup. All chrome text MUST be accessed via `stringResource(R.string.X)`.

#### Scenario: All four wizard signatures drop selectedLanguage
- **WHEN** the sources are inspected after this change lands
- **THEN** the four wizard Composable function signatures have no `selectedLanguage` parameter, AND `grep -nE 'R\.string\.\w+_zh' MemoryTemplateScreen.kt MemoryPhotoSelectionScreen.kt MemoryEditScreen.kt MemoryReflectionScreen.kt` returns zero matches

### Requirement: New string resources for the three Reflection fields

`memory_reflection_mood_label`, `memory_reflection_insights_label`, `memory_reflection_feedback_label` SHALL exist in both `res/values/strings.xml` and `res/values-zh/strings.xml`.

#### Scenario: New strings have bilingual coverage
- **WHEN** the repo is inspected after this change lands
- **THEN** `grep -l "memory_reflection_mood_label" res/values/strings.xml res/values-zh/strings.xml` returns both files, AND the same for the insights and feedback labels

### Requirement: MockData consumers continue to compile

`MockData.templates`, `MockData.memories`, `MockData.routes`, `MockData.spots` SHALL NOT be deleted or modified by this change. Any screen still importing `data.MockData` (e.g. `MemoriesScreen` until change #9 migrates it) MUST continue to compile.

#### Scenario: Build after this change has MockData intact
- **WHEN** `./gradlew :app:assembleDebug` is run after this change lands
- **THEN** the build succeeds, AND `data.MockData.templates` / `.memories` / `.routes` / `.spots` references compile unchanged
