## Why

Umbrella §2 line 7 is the longest single line in the wiring spec: "Template → Photo picker → Edit → Reflection wizard, end-to-end". The four screens it covers are the most-stubbed surfaces in the worktree: `MemoryTemplateScreen` lists templates from `MockData` and threads a `templateId: String` forward; `MemoryPhotoSelectionScreen` does not invoke the system photo picker at all — its `Add Photos` button at `MemoryPhotoSelectionScreen.kt:156-168` hardcodes a list of five `R.drawable.*` resource ids to fake a selection; `MemoryEditScreen` and `MemoryReflectionScreen` per `references/guideline.md:16-17` are explicitly marked `{還沒有功能}`. There is no `Memory` persistence today — no Room, no DataStore wrapper, no file system storage of photos.

This change is the load-bearing build: it introduces Room, wires the photo picker, establishes the photo lifecycle (umbrella §7.3 in full), defines the wizard's data flow as `memoryId`-keyed instead of `templateId`-keyed (umbrella §7.3 third bullet), and lands the four ViewModels with the umbrella §6 MVI shape. AI text generation is deferred to change #8 (`ai-reflection-generation`) per the umbrella's explicit decoupling — this change ships a Reflection screen with mood / insights / follow-up text fields persisted to Room, and a `Save` button that completes the memory in `COMPLETED` status without calling any LLM.

## What Changes

- Add Room (`androidx.room:room-runtime`, `room-ktx`, Room KSP compiler) to the Gradle catalog.
- Introduce `data/memory/MemoryEntity.kt` mirroring umbrella §7.2 schema verbatim: `id` (UUID v4), `templateId`, `routeId?`, `title`, `status` ("IN_PROGRESS" / "COMPLETED"), `createdAt`, `updatedAt`, `photoLocalPaths` (JSON `List<String>`, relative to `context.filesDir`), `spotNotes` (JSON `Map<spotId, note>`, unused by this change — set to `"{}"` — reserved for a future change), `overallMood`, `userInsights`, `postTripFeedback`, `generatedReflection`, `editorState` (JSON, unused by this change — set to `null`).
- Introduce `MemoryDao` (umbrella §7.2 verbatim), `MemoryDatabase` (Room `@Database(version = 1)`), `MemoryRepository` with the JSON unfold/fold layer.
- Introduce a code-resident `TemplateCatalog` object (same precedent as `TagCatalog` in `home-discovery`) declaring the four templates from `MockData.templates` as `Template(id, titleRes, descriptionRes, imageRes, maskRes, slots)`. `MockData.templates` STAYS as a separate constant for now — same coexistence rule as for routes/spots. The `MemoryTemplateScreen` reads `TemplateCatalog.all`, NOT `MockData.templates`.
- Photo lifecycle per umbrella §7.3:
  - **memoryId assignment**: `MemoryTemplateScreen.onTemplateSelect(templateId)` invokes `MemoryRepository.startDraft(templateId)` which creates a new Room row with `id = UUID.randomUUID().toString()`, `status = "IN_PROGRESS"`, `createdAt = updatedAt = now`, then returns the `memoryId`. Navigation: `MemoryPhotoSelectionDestination(memoryId)` (renamed from `templateId`).
  - **Photo picker**: `MemoryPhotoSelectionScreen.AddPhotosBox` click launches `ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)` from `androidx.activity:activity-compose`. No CAMERA or READ_EXTERNAL_STORAGE permission required (Photo Picker is permission-less since AndroidX backport).
  - **Eager copy**: each picked `Uri` is copied to `context.filesDir/memories/<memoryId>/photo_<index>.jpg` (overwriting existing index slot if user re-picks). The Repository's `addPhoto(memoryId, sourceUri)` does the file copy + DB row update inside a `withContext(Dispatchers.IO)` block. `photoLocalPaths` stores relative paths like `"memories/<memoryId>/photo_0.jpg"`.
  - **Abandonment cleanup**: each wizard ViewModel (`PhotoSelectionViewModel`, `EditViewModel`, `ReflectionViewModel`) overrides `onCleared()` to call `MemoryRepository.cancelDraftIfInProgress(memoryId)` — which checks the DB row's `status`, and if still `"IN_PROGRESS"`, deletes the row AND recursively deletes `context.filesDir/memories/<memoryId>/`. Only the `ReflectionViewModel.Save` path bumps status to `"COMPLETED"` BEFORE `onCleared()` fires, so completed memories survive.
  - **Orphan sweep**: `MemoryRepository.sweepOrphans()` runs once on `MemoirApplication.onCreate()`'s application-scope coroutine: deletes every `MemoryEntity` row where `status == "IN_PROGRESS" AND updatedAt < now - 7 days` plus its `filesDir/memories/<id>/` directory. Acts as the failsafe for crash-survivors.
- Migrate the four wizard screens off `selectedLanguage: String` + `_zh`-suffix:
  - `MemoryTemplateScreen` → `MemoryTemplateViewModel(repo, templateCatalog)` → state `{ templates: List<TemplateCard>, isLoading }`, intent `TemplateClicked(templateId)`, effect `NavigateToPhotoSelection(memoryId)`. `TemplateCard` is a DTO `(id, title, description, imageRes, maskRes)` pre-resolved to current locale.
  - `MemoryPhotoSelectionScreen` → `PhotoSelectionViewModel(repo, memoryId, locale)` → state `{ photoPaths: List<String>, maxPhotos: Int = 5, isLoading, error }`, intent `PhotosPicked(uris: List<Uri>)`, `PhotoRemoved(index: Int)`, `NextClicked`, effect `NavigateToEdit(memoryId)`, `LaunchPicker(maxItems: Int)`. The screen renders selected photos by reading from `filesDir` via Coil/Painter — uses `File("$filesDir/$relativePath")`.
  - `MemoryEditScreen` → `EditViewModel(repo, memoryId, locale)` → state `{ templateId, photoPaths: List<String>, templateMaskRes, templateSlots: List<TemplateSlot>, isLoading }`, intent `SaveClicked`, effect `NavigateToReflection(memoryId)`. The UI renders photos positioned at `template.slots[i]` coordinates over the template mask drawable. No actual editing affordances (move / filter / sticker / stamp / font) are shipped — those stay as dead UI per scope cap.
  - `MemoryReflectionScreen` → `ReflectionViewModel(repo, memoryId, locale)` → state `{ overallMood: String, userInsights: String, postTripFeedback: String, isSaving, error }`, intent `MoodChanged(text)`, `InsightsChanged(text)`, `FeedbackChanged(text)`, `SaveClicked`, effect `NavigateToMemoriesList`. `SaveClicked` writes the three text fields, sets `status = "COMPLETED"`, bumps `updatedAt`, and emits the navigation effect. No LLM call.
- Add UI for the three reflection text fields (replacing the existing single `My Reflection` field):
  - `memory_reflection_mood_label` ("How did you feel?" / "你的心情如何？")
  - `memory_reflection_insights_label` ("What did you learn?" / "你學到了什麼？") — replaces the current `memory_reflection_my_reflection`
  - `memory_reflection_feedback_label` ("Looking back..." / "回顧旅程...")
  - The "Polish with AI" button (`memory_reflection_polish_ai`) stays in the UI but its click is a no-op + `Log.w("memory-creation-flow", "AI polish coming in change #8")` until `ai-reflection-generation` lands.
- Rewrite `MyAppNavigation`'s four wizard destination entries:
  - Change `MemoryPhotoSelectionDestination(val templateId: String)` → `MemoryPhotoSelectionDestination(val memoryId: String)`.
  - Change `MemoryEditDestination(val templateId: String, val photoResIds: List<Int>)` → `MemoryEditDestination(val memoryId: String)`.
  - Change `MemoryReflectionDestination` → `MemoryReflectionDestination(val memoryId: String)`.
  - Delete `var selectedPhotos by remember { mutableStateOf(listOf<Int>()) }` Compose state at the top of `MyAppNavigation()` — no longer needed; photos live in Room.
  - Each entry constructs its VM via a Factory (same pattern as previous changes).
- **Out of scope** (these stay stubs / dead UI):
  - Memory editor canvas (move / filter / sticker / font / stamp buttons). UI rendered but inert.
  - "Add to Landmarks" button on Reflection screen.
  - "Polish with AI" actual call — change #8.
  - `MemoriesScreen` real listing — change #9 (`memory-library-actions`).
  - Coil dependency: photo rendering in this change uses raw `BitmapFactory.decodeFile(...)` wrapped in `painterResource`-like Composable helper, OR continues to use the existing painter approach for now. (Decision punted to design D-photo-rendering.)
  - Photo reorder via drag-and-drop (the existing `memory_flow_hold_to_reorder` label stays but reorder is no-op — per umbrella §2 scope).
  - `spotNotes` per-spot text — schema present in `MemoryEntity` but no UI to populate; reserved for a future scope.
  - `editorState` JSON — schema present, set to null.
  - Memory bookmark / share / duplicate — change #9.
  - Multi-locale data input (text fields accept any locale — no per-language storage).

## Capabilities

### New Capabilities
- `memory-creation-flow`: The four-screen wizard from Template selection to Reflection save, plus the Room-backed `Memory` persistence, the `filesDir`-backed photo storage with eager-copy/cancel-cleanup/orphan-sweep lifecycle, the `TemplateCatalog`, and the four ViewModels. Owns the contract that a `memoryId` is assigned at template selection and survives backward+forward navigation across the wizard.

### Modified Capabilities
<!-- None — `content-pipeline`, `language-toggle`, `home-discovery`, `route-bookmarking`, `artifact-discovery-flow`, `artifact-photo-capture` are all foundations consumed unchanged. -->

## Impact

- **New files** (count: ~24):
  - `data/memory/MemoryEntity.kt`, `MemoryDao.kt`, `MemoryDatabase.kt`, `MemoryRepository.kt`, `MemoryStatus.kt` (enum or constants)
  - `data/memory/MemoryDomainTypes.kt` (the `Memory` domain model + `Template` + `TemplateSlot`)
  - `ui/memory/template/TemplateCatalog.kt`, `MemoryTemplateState.kt`, `MemoryTemplateIntent.kt`, `MemoryTemplateEffect.kt`, `MemoryTemplateViewModel.kt`, `MemoryTemplateViewModelFactory.kt`
  - `ui/memory/photo/PhotoSelectionState.kt`, `PhotoSelectionIntent.kt`, `PhotoSelectionEffect.kt`, `PhotoSelectionViewModel.kt`, `PhotoSelectionViewModelFactory.kt`
  - `ui/memory/edit/EditState.kt`, `EditIntent.kt`, `EditEffect.kt`, `EditViewModel.kt`, `EditViewModelFactory.kt`
  - `ui/memory/reflection/ReflectionState.kt`, `ReflectionIntent.kt`, `ReflectionEffect.kt`, `ReflectionViewModel.kt`, `ReflectionViewModelFactory.kt`
  - Tests (8 test files): `MemoryRepositoryTest.kt` (Room in-memory), `MemoryTemplateViewModelTest.kt`, `PhotoSelectionViewModelTest.kt`, `EditViewModelTest.kt`, `ReflectionViewModelTest.kt`, `PhotoLifecycleTest.kt` (eager copy + cleanup paths), `OrphanSweepTest.kt`, `TemplateCatalogTest.kt`
- **Modified files**:
  - `MemoryTemplateScreen.kt`, `MemoryPhotoSelectionScreen.kt`, `MemoryEditScreen.kt`, `MemoryReflectionScreen.kt` — all rewritten as VM-driven Composables, `selectedLanguage`/`_zh` removed.
  - `MyAppNavigation.kt` — four wizard entries rewritten; `selectedPhotos` lifted state deleted; destination signatures changed.
  - `MemoirApplication.kt` — adds `lateinit var memoryRepo: MemoryRepository` to the companion object; constructs in `onCreate()`; invokes `memoryRepo.sweepOrphans()` on the application scope.
  - `gradle/libs.versions.toml` — add `room`, `roomKtx`, `roomCompiler`, `ksp` plugin, `androidx-activity-compose` (the BackHandler/picker need newer activity-compose; verify version).
  - `app/build.gradle.kts` — apply `alias(libs.plugins.ksp)`, add Room implementation + KSP processor + Room testing.
  - `res/values/strings.xml` + `res/values-zh/strings.xml` — add `memory_reflection_mood_label`, `memory_reflection_insights_label`, `memory_reflection_feedback_label`.
- **Dependencies added**: Room (runtime + ktx + compiler via KSP), KSP plugin.
- **Risk acknowledgements**:
  - Room schema v1 with `destructiveMigration()`: any future schema change wipes user memories. Acceptable per umbrella §12 (MVP risk).
  - PickMultipleVisualMedia returns `Uri`s that require `ContentResolver.openInputStream`; on some devices the Uri is short-lived. Eager-copy at selection time mitigates.
  - File deletion in `cancelDraftIfInProgress` is recursive — bug could delete unrelated files. Guard with strict path validation (`memoryId` must be valid UUID).
  - Photo decoding in `MemoryEditScreen` uses raw file paths — if a photo's path becomes stale (e.g. after a sweep), the Composable shows a placeholder.
  - `onCleared()` cleanup runs on a non-coroutine context (synchronous Room calls would be wrong). Use `GlobalScope.launch` carefully OR a dedicated supervisor scope owned by `MemoryRepository`.
  - Backstack pop from Reflection → back-arrow leaves the memory `IN_PROGRESS`; user can resume from `MemoriesScreen` ("Continue Editing") — which change #9 wires.
- **Not changed**:
  - `MockData.templates` — coexists with `TemplateCatalog`.
  - `MockData.memories` — replaced by Room reads; the old mock list stays but `MemoriesScreen` (which still reads it) is owned by change #9.
  - Camera capture flow (change #6) and its captured Uri are NOT integrated into the photo picker — the picker reads from system MediaStore via `PickMultipleVisualMedia`, which surfaces camera-captured photos because they live in `Pictures/Memoir/`.
  - Other screens' files.
