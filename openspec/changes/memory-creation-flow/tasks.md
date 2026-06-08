## 1. Gradle catalog + Room/KSP

- [ ] 1.1 In `frontend/mobile/gradle/libs.versions.toml` `[versions]` add `room` (pin **≥ 2.7.0** — `2.6.x` deprecated `fallbackToDestructiveMigration()`; `2.7.x` uses `fallbackToDestructiveMigration(dropAllTables = true)` which is what this change targets) and `ksp` (must match the Kotlin 2.2.10 version — typically `2.2.10-x.y.z`). Look up exact latest stable on Maven Central / Context7 at implementation time
- [ ] 1.2 In `[libraries]` add `androidx-room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }`, `androidx-room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }`, `androidx-room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }`, `androidx-room-testing = { module = "androidx.room:room-testing", version.ref = "room" }`
- [ ] 1.3 In `[plugins]` add `ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }`
- [ ] 1.4 In `frontend/mobile/app/build.gradle.kts` `plugins { }`, add `alias(libs.plugins.ksp)`
- [ ] 1.5 In `dependencies { }`, add `implementation(libs.androidx.room.runtime)`, `implementation(libs.androidx.room.ktx)`, `ksp(libs.androidx.room.compiler)`, `testImplementation(libs.androidx.room.testing)`
- [ ] 1.6 Verify `activity-compose` version supports `PickMultipleVisualMedia` (`androidx.activity:activity-compose:1.8+`). The catalog has `activityCompose = "1.13.0"` — confirmed sufficient. No bump needed
- [ ] 1.7 Run `cd frontend/mobile && ./gradlew :app:dependencies | rg room` to confirm Room artifacts resolve

## 2. String resources

- [ ] 2.1 Add to `res/values/strings.xml`:
  ```xml
  <string name="memory_reflection_mood_label">How did you feel?</string>
  <string name="memory_reflection_insights_label">What did you learn?</string>
  <string name="memory_reflection_feedback_label">Looking back…</string>
  <string name="template_old_street">Old Street Journal</string>
  <string name="template_old_street_desc">Magazine-style layout journaling old streets and daily life.</string>
  <string name="template_city_walk">City Walk Map</string>
  <string name="template_city_walk_desc">Record your day trip with maps, location markers, and photo cards.</string>
  <string name="template_taiwan_pop">Taiwan Pop Collage</string>
  <string name="template_taiwan_pop_desc">Youthful collage with stickers, handwritten text, and photo cards.</string>
  <string name="template_heritage_arch">Heritage Architecture Archive</string>
  <string name="template_heritage_arch_desc">Exhibition-catalogue layout for historical buildings and annotations.</string>
  ```
- [ ] 2.2 Add the same keys to `res/values-zh/strings.xml` with Chinese translations sourced from the existing `MockData.templates[i].titleZh`/`descriptionZh` values

## 3. Room layer

- [ ] 3.1 Create `data/memory/MemoryStatus.kt`: `object MemoryStatus { const val IN_PROGRESS = "IN_PROGRESS"; const val COMPLETED = "COMPLETED" }`
- [ ] 3.2 Create `data/memory/MemoryEntity.kt` with `@Entity(tableName = "memories")` exactly matching design D1 (15 columns)
- [ ] 3.3 Create `data/memory/MemoryDao.kt` with `@Upsert suspend fun upsert(memory: MemoryEntity)`, `@Query("DELETE FROM memories WHERE id = :id") suspend fun delete(id: String)`, `@Query("SELECT * FROM memories WHERE id = :id") suspend fun getOnce(id: String): MemoryEntity?` (single suspend lookup for read-modify-write paths in `addPhoto` / `cancelDraftIfInProgress` — Flow.first() against `Flow<MemoryEntity?>` has ambiguous semantics for the missing-row case), `@Query("SELECT * FROM memories WHERE id = :id") fun observe(id: String): Flow<MemoryEntity?>` (Flow for UI consumers wanting live updates), `@Query("SELECT * FROM memories ORDER BY updatedAt DESC") fun observeAll(): Flow<List<MemoryEntity>>`, `@Query("SELECT * FROM memories WHERE status = :status ORDER BY updatedAt DESC") fun observeByStatus(status: String): Flow<List<MemoryEntity>>`, `@Query("SELECT * FROM memories WHERE status = 'IN_PROGRESS' AND updatedAt < :cutoff") suspend fun findStaleInProgress(cutoff: Long): List<MemoryEntity>`
- [ ] 3.4 Create `data/memory/MemoryDatabase.kt`: `@Database(entities = [MemoryEntity::class], version = 1, exportSchema = false) abstract class MemoryDatabase : RoomDatabase() { abstract fun memoryDao(): MemoryDao }`
- [ ] 3.5 Create `data/memory/MemoryDomainTypes.kt`: `data class Memory(...)` per design D2 schema; `data class TemplateSlot(...)`

## 4. MemoryRepository

- [ ] 4.1 Create `data/memory/MemoryRepository.kt` as an `interface` per the `UserPreferencesRepository`/`ContentAssetLoader` precedent: `startDraft`, `observe`, `observeAll`, `observeByStatus`, `addPhoto`, `removePhoto`, `updateReflection`, `complete`, `cancelDraftIfInProgress`, `fireCancelDraftIfInProgress`, `sweepOrphans`
- [ ] 4.2 Create `data/memory/RoomMemoryRepository.kt` impl with constructor `(private val dao: MemoryDao, private val filesDir: File, private val json: Json, private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO)`
- [ ] 4.3 Implement `private val cleanupScope = CoroutineScope(SupervisorJob() + ioDispatcher)` for fire-and-forget cleanup launches
- [ ] 4.4 Implement `private fun String.isValidUuid(): Boolean = matches(Regex("^[0-9a-fA-F\\-]{36}$"))` — defense-in-depth path safety
- [ ] 4.5 Implement `startDraft(templateId, defaultTitle): String` per design D2 — generate UUID, insert row, return id
- [ ] 4.6 Implement `addPhoto(memoryId, sourceUri, contentResolver): Result<String>` per design D3 — validate UUID, look up current paths, copy file with `contentResolver.openInputStream`+`FileOutputStream`, append relative path, upsert row
- [ ] 4.7 Implement `removePhoto(memoryId, index): Result<Unit>` — UUID check, delete file at `filesDir/memories/<id>/photo_<index>.jpg`, remove from JSON list, upsert
- [ ] 4.8 Implement `updateReflection(memoryId, mood, insights, feedback)` — single `dao.upsert` with bumped `updatedAt`
- [ ] 4.9 Implement `complete(memoryId)` — sets `status = "COMPLETED"`, bumps `updatedAt`
- [ ] 4.10 Implement `cancelDraftIfInProgress(memoryId)` per design D4 — UUID check, only act if status == IN_PROGRESS, recursive delete with `startsWith` containment check, DB delete
- [ ] 4.11 Implement `fireCancelDraftIfInProgress(memoryId) { cleanupScope.launch { cancelDraftIfInProgress(memoryId) } }` — fire-and-forget for VM `onCleared()`
- [ ] 4.12 Implement `sweepOrphans()` per design D5 — query `findStaleInProgress(now - 7 days)`, recursive delete each row's directory + DB row

## 5. Application wiring

- [ ] 5.1 In `MemoirApplication.kt`, add `companion object { ... lateinit var memoryRepo: MemoryRepository private set }` alongside existing `content` and `prefs` fields
- [ ] 5.2 In `onCreate()`, build the Room DB via `val database = Room.databaseBuilder(applicationContext, MemoryDatabase::class.java, "memoir.db").fallbackToDestructiveMigration(dropAllTables = true).build()` — Room 2.7+ API. The boolean parameter MUST be present; the no-arg `fallbackToDestructiveMigration()` form is deprecated in 2.6+ and removed in 2.7+
- [ ] 5.3 Construct `memoryRepo = RoomMemoryRepository(dao = database.memoryDao(), filesDir = applicationContext.filesDir, json = ContentJson)` — reuse `ContentJson` from change #1 (`Json { ignoreUnknownKeys = true; explicitNulls = false }`)
- [ ] 5.4 Invoke `appScope.launch { memoryRepo.sweepOrphans() }` on the same `appScope` used for `LocaleController.reconcileAtStartup` and content loading — fire-and-forget at app start

## 6. TemplateCatalog

- [ ] 6.1 Create `ui/memory/template/Template.kt`: `data class Template(val id: String, @StringRes val titleRes: Int, @StringRes val descriptionRes: Int, @DrawableRes val imageRes: Int, @DrawableRes val maskRes: Int, val slots: List<TemplateSlot>)`
- [ ] 6.2 Create `ui/memory/template/TemplateCatalog.kt`:
  ```kotlin
  object TemplateCatalog {
      val all: List<Template> = listOf(
          Template("old_street", R.string.template_old_street, R.string.template_old_street_desc,
                   R.drawable.memory_templete_1, R.drawable.memory_templete_1_mask,
                   slots = listOf(/* 5 slots copied verbatim from MockData.templates[0].slots */)),
          Template("city_walk", R.string.template_city_walk, R.string.template_city_walk_desc,
                   R.drawable.memory_templete_2, R.drawable.memory_templete_2_mask,
                   slots = listOf(/* 5 slots from MockData.templates[1] */)),
          Template("taiwan_pop", R.string.template_taiwan_pop, R.string.template_taiwan_pop_desc,
                   R.drawable.memory_templete_3, R.drawable.memory_templete_3_mask,
                   slots = listOf(/* 5 slots from MockData.templates[2] */)),
          Template("heritage_arch", R.string.template_heritage_arch, R.string.template_heritage_arch_desc,
                   R.drawable.memory_templete_4, R.drawable.memory_templete_4_mask,
                   slots = listOf(/* 5 slots from MockData.templates[3] */))
      )
      val ids: Set<String> = all.map { it.id }.toSet()
      fun byId(id: String): Template? = all.firstOrNull { it.id == id }
  }
  ```
  The `slots` lists are mechanical copies from `MockData.kt:246-301` — preserve `TemplateSlot(x, y, width, height, rotation)` values verbatim

## 7. MemoryTemplateViewModel + screen

- [ ] 7.1 Create `ui/memory/template/MemoryTemplateState.kt` with `data class TemplateCard(id, title, description, imageRes, maskRes)` DTO + `MemoryTemplateState(templates: List<TemplateCard> = emptyList(), isLoading: Boolean = true)`
- [ ] 7.2 Create `MemoryTemplateIntent.kt`: `sealed interface MemoryTemplateIntent { data class TemplateClicked(val templateId: String) : MemoryTemplateIntent }`
- [ ] 7.3 Create `MemoryTemplateEffect.kt`: `sealed interface MemoryTemplateEffect { data class NavigateToPhotoSelection(val memoryId: String) : MemoryTemplateEffect }`
- [ ] 7.4 Create `MemoryTemplateViewModel.kt` with constructor `(private val repo: MemoryRepository, private val resources: Resources, private val localeProvider: () -> Locale)`
- [ ] 7.5 In `init`: build `state.templates` from `TemplateCatalog.all.map { TemplateCard(id = it.id, title = resources.getString(it.titleRes), description = resources.getString(it.descriptionRes), imageRes = it.imageRes, maskRes = it.maskRes) }` — locale resolved via the active `Configuration` because `resources.getString(@StringRes)` honors `values-zh/`
- [ ] 7.6 Implement `onIntent(TemplateClicked(templateId))`: `viewModelScope.launch { val title = resources.getString(TemplateCatalog.byId(templateId)!!.titleRes); val memoryId = repo.startDraft(templateId, title); _effects.send(NavigateToPhotoSelection(memoryId)) }`
- [ ] 7.7 Create `MemoryTemplateViewModelFactory.kt`
- [ ] 7.8 Rewrite `MemoryTemplateScreen.kt`: drop `selectedLanguage` parameter; consume `viewModel.state`; render `state.templates` instead of `MockData.templates`; use `stringResource(R.string.memory_flow_choose_template)` (no `_zh` branch); collect effects in `LaunchedEffect(viewModel)` → `onNavigateToPhotoSelection(memoryId)`

## 8. PhotoSelectionViewModel + screen

- [ ] 8.1 Create `ui/memory/photo/PhotoSelectionState.kt`: `data class PhotoSelectionState(val isLoading: Boolean = true, val memoryId: String? = null, val photoPaths: List<String> = emptyList(), val maxPhotos: Int = 5, val error: String? = null)`
- [ ] 8.2 Create `PhotoSelectionIntent.kt`: `sealed interface PhotoSelectionIntent { data object AddPhotosClicked; data class PhotosPicked(val uris: List<Uri>); data class PhotoRemoved(val index: Int); data object NextClicked }`
- [ ] 8.3 Create `PhotoSelectionEffect.kt`: `sealed interface PhotoSelectionEffect { data class LaunchPicker(val maxItems: Int); data class NavigateToEdit(val memoryId: String) }`
- [ ] 8.4 Create `PhotoSelectionViewModel.kt` with constructor `(private val memoryId: String, private val repo: MemoryRepository, private val contentResolver: ContentResolver)`
- [ ] 8.5 In `init`: `viewModelScope.launch { repo.observe(memoryId).collect { memory -> _state.update { it.copy(isLoading = false, memoryId = memory?.id, photoPaths = memory?.photoRelativePaths.orEmpty()) } } }`
- [ ] 8.6 Implement `onIntent`: `AddPhotosClicked` → emit `LaunchPicker(maxItems = 5 - _state.value.photoPaths.size)`; `PhotosPicked(uris)` → clip uris to available slots, launch coroutine to `repo.addPhoto(memoryId, uri, contentResolver)` for each; `PhotoRemoved(index)` → call `repo.removePhoto(memoryId, index)`; `NextClicked` → emit `NavigateToEdit(memoryId)` only if `_state.value.photoPaths.isNotEmpty()`
- [ ] 8.7 Override `onCleared()` to call `repo.fireCancelDraftIfInProgress(memoryId)` — abandonment cleanup
- [ ] 8.8 Create `PhotoSelectionViewModelFactory.kt`
- [ ] 8.9 Rewrite `MemoryPhotoSelectionScreen.kt`: drop `selectedLanguage`/`templateId`/`initialPhotos`/`onPhotosChange` parameters; use `val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)) { uris -> if (uris.isNotEmpty()) viewModel.onIntent(PhotoSelectionIntent.PhotosPicked(uris)) }`; collect effects to invoke `launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))` on `LaunchPicker` AND `onNavigateToEdit(memoryId)` on `NavigateToEdit`; render photos via `FilePhoto(relativePath, filesDir)` Composable defined in `ui/memory/components/FilePhoto.kt`; "Add Photos" click → `viewModel.onIntent(AddPhotosClicked)`; Next button click → `viewModel.onIntent(NextClicked)`
- [ ] 8.10 Delete the hardcoded `R.drawable.sounds_of_temple` / `sea_protection` / etc. fake-photo block at the existing `MemoryPhotoSelectionScreen.kt:156-168`

## 9. EditViewModel + screen

- [ ] 9.1 Create `ui/memory/edit/EditState.kt`: `data class EditState(val isLoading: Boolean = true, val memoryId: String? = null, val templateId: String? = null, val templateMaskRes: Int = 0, val templateSlots: List<TemplateSlot> = emptyList(), val photoPaths: List<String> = emptyList(), val error: String? = null)`
- [ ] 9.2 Create `EditIntent.kt`: `sealed interface EditIntent { data object SaveClicked : EditIntent }`
- [ ] 9.3 Create `EditEffect.kt`: `sealed interface EditEffect { data class NavigateToReflection(val memoryId: String) : EditEffect }`
- [ ] 9.4 Create `EditViewModel.kt` with constructor `(private val memoryId: String, private val repo: MemoryRepository)`
- [ ] 9.5 In `init`: collect `repo.observe(memoryId)`; populate `_state` with the template's `maskRes` + `slots` resolved via `TemplateCatalog.byId(memory.templateId)`
- [ ] 9.6 Implement `onIntent(SaveClicked)`: emit `NavigateToReflection(memoryId)` — no DB write in this change (editor state stays null)
- [ ] 9.7 Override `onCleared()` to call `repo.fireCancelDraftIfInProgress(memoryId)`
- [ ] 9.8 Create `EditViewModelFactory.kt`
- [ ] 9.9 Rewrite `MemoryEditScreen.kt`: drop `selectedLanguage`/`templateId`/`photoResIds`/`onSaveClick` legacy parameters; render `state.templateMaskRes` as a base layer, then render each `state.photoPaths[i]` positioned at `state.templateSlots[i]` (x, y, width, height fractions of the container size) via `FilePhoto`; render the existing UI affordances (Move / Filters / Stickers / Fonts / Stamps buttons) but each is a no-op + `Log.w("memory-creation-flow", "<name> not yet implemented")` — they survive as dead UI per scope cap; Save button → `viewModel.onIntent(EditIntent.SaveClicked)`

## 10. ReflectionViewModel + screen

- [ ] 10.1 Create `ui/memory/reflection/ReflectionState.kt`: `data class ReflectionState(val isLoading: Boolean = true, val overallMood: String = "", val userInsights: String = "", val postTripFeedback: String = "", val isSaving: Boolean = false, val error: String? = null)`
- [ ] 10.2 Create `ReflectionIntent.kt`: `sealed interface ReflectionIntent { data class MoodChanged(val text: String); data class InsightsChanged(val text: String); data class FeedbackChanged(val text: String); data object SaveClicked }`
- [ ] 10.3 Create `ReflectionEffect.kt`: `sealed interface ReflectionEffect { data object NavigateToMemoriesList; data class ShowError(val msg: String) }`
- [ ] 10.4 Create `ReflectionViewModel.kt` per design D11 with `private var completionConfirmed = false` flag; `init` block collects current memory and prefills text fields if non-empty (for "Continue Editing" support if change #9 later wires it)
- [ ] 10.5 Implement `onIntent` reducer: text-change intents update state; `SaveClicked` writes via `repo.updateReflection(memoryId, mood.ifBlank { null }, insights, feedback.ifBlank { null })` then `repo.complete(memoryId)` then sets `completionConfirmed = true` then emits `NavigateToMemoriesList`; failure → `ShowError`
- [ ] 10.6 Override `onCleared()`: `if (!completionConfirmed) repo.fireCancelDraftIfInProgress(memoryId)`
- [ ] 10.7 Create `ReflectionViewModelFactory.kt`
- [ ] 10.8 Rewrite `MemoryReflectionScreen.kt`: drop `selectedLanguage`/`onNextClick` legacy parameter; add three `OutlinedTextField` (or styled `BasicTextField`) blocks for the three fields with labels `stringResource(R.string.memory_reflection_mood_label)` / `_insights_label` / `_feedback_label`; the "Polish with AI" button is a no-op + `Log.w("memory-creation-flow", "AI polish coming in change #8")`; the "Add to landmarks" button is a no-op (deferred); Save button → `viewModel.onIntent(SaveClicked)`; collect effects for navigation

## 11. FilePhoto helper Composable

- [ ] 11.1 Create `ui/memory/components/FilePhoto.kt`:
  ```kotlin
  @Composable
  fun FilePhoto(relativePath: String, filesDir: File, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop) {
      val bitmap = remember(relativePath) {
          val f = File(filesDir, relativePath)
          if (f.exists()) BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap() else null
      }
      if (bitmap != null) {
          Image(bitmap = bitmap, contentDescription = null, modifier = modifier, contentScale = contentScale)
      } else {
          Box(modifier.background(Color.LightGray))
      }
  }
  ```
- [ ] 11.2 Note: `BitmapFactory.decodeFile` blocks the main thread during initial decode; acceptable at ≤5 small photos per design D10. If profiling shows jank in a later change, swap to Coil

## 12. MyAppNavigation rewrite

- [ ] 12.1 Change `data class MemoryPhotoSelectionDestination(val templateId: String)` → `data class MemoryPhotoSelectionDestination(val memoryId: String)`
- [ ] 12.2 Change `data class MemoryEditDestination(val templateId: String, val photoResIds: List<Int>)` → `data class MemoryEditDestination(val memoryId: String)`
- [ ] 12.3 Change `data object MemoryReflectionDestination` → `data class MemoryReflectionDestination(val memoryId: String)`
- [ ] 12.4 Delete `var selectedPhotos by remember { mutableStateOf(listOf<Int>()) }` Compose state at the top of `MyAppNavigation()`
- [ ] 12.5 Rewrite the `MemoryTemplateDestination` entry: construct `vm = viewModel(factory = MemoryTemplateViewModelFactory(...))`; pass `onNavigateToPhotoSelection = { memoryId -> backStack.add(MemoryPhotoSelectionDestination(memoryId)) }` via `LaunchedEffect(vm) { vm.effects.collect { e -> when (e) { is NavigateToPhotoSelection -> backStack.add(MemoryPhotoSelectionDestination(e.memoryId)) } } }`
- [ ] 12.6 Rewrite the `MemoryPhotoSelectionDestination` entry: factory closes over `key.memoryId`; `viewModel(key = key.memoryId, factory = PhotoSelectionViewModelFactory(memoryId = key.memoryId, repo = MemoirApplication.memoryRepo, contentResolver = ctx.contentResolver))`; effect collector handles `LaunchPicker` (invokes picker launcher) and `NavigateToEdit` (adds `MemoryEditDestination(memoryId)` to backStack)
- [ ] 12.7 Rewrite the `MemoryEditDestination` entry similarly with `key.memoryId`
- [ ] 12.8 Rewrite the `MemoryReflectionDestination` entry similarly; `NavigateToMemoriesList` effect clears the backstack to MemoriesDestination
- [ ] 12.9 Verify no other destination entries break (`selectedLanguage` threading to other still-legacy screens continues to compile)

## 13. Tests

- [ ] 13.1 Create `MemoryRepositoryTest.kt` as an **instrumented test** under `frontend/mobile/app/src/androidTest/java/com/mcis/memoir/data/memory/` (NOT a JVM unit test). Build real Room in-memory DB via `Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, MemoryDatabase::class.java).allowMainThreadQueries().build()`; real `filesDir = Files.createTempDirectory("memoir-test").toFile()`. The fake-DAO compromise was rejected because it loses real Room `@Upsert` / `@Query` coverage — the spec scenarios assert real Room behavior. Use `@RunWith(AndroidJUnit4::class)` and `kotlinx-coroutines-test`'s `runTest`
- [ ] 13.2 Test cases for `MemoryRepositoryTest`: `startDraft` creates row with `UUID.fromString(id)` parseable + IN_PROGRESS status; `addPhoto` writes file to expected path AND updates `photoLocalPaths` JSON; `addPhoto` with invalid memoryId returns `Result.failure(IllegalArgumentException)` (NOT a synchronous throw — validation is inside `runCatching`); `removePhoto` removes file + JSON entry; `cancelDraftIfInProgress` on IN_PROGRESS deletes row + dir; `cancelDraftIfInProgress` on COMPLETED is no-op; `cancelDraftIfInProgress` on invalid UUID is silent no-op (does NOT throw); `sweepOrphans` deletes old IN_PROGRESS + dir, preserves recent + COMPLETED
- [ ] 13.2a No corresponding JVM unit test — Room behavior coverage lives only at the instrumented layer. The four VM tests (13.3-13.6) test against `mockk<MemoryRepository>()` and remain JVM-only
- [ ] 13.3 Create `MemoryTemplateViewModelTest.kt`: assert `TemplateClicked` invokes `startDraft` + emits effect; mock `MemoryRepository.startDraft(...)` to return a known UUID; verify exact effect emission
- [ ] 13.4 Create `PhotoSelectionViewModelTest.kt`: assert `AddPhotosClicked` emits `LaunchPicker(maxItems)` where maxItems reflects available slots; assert `PhotosPicked` clipping to 5 - currentCount works; assert `PhotoRemoved` calls `removePhoto`; assert `NextClicked` does NOT emit when photoPaths is empty; assert `onCleared` calls `fireCancelDraftIfInProgress`
- [ ] 13.5 Create `EditViewModelTest.kt`: assert init populates template slots + photos from collected Memory; assert `SaveClicked` emits `NavigateToReflection`; assert `onCleared` calls cleanup
- [ ] 13.6 Create `ReflectionViewModelTest.kt`: assert text-change intents update state without effects; assert `SaveClicked` calls `updateReflection` with `mood.ifBlank { null }` AND `complete(memoryId)` AND emits `NavigateToMemoriesList`; assert `completionConfirmed` prevents `onCleared` cleanup after successful save; assert failure path emits `ShowError`
- [ ] 13.7 Create `TemplateCatalogTest.kt`: assert 4 entries, no duplicate ids, every `@StringRes` and `@DrawableRes` is a non-zero compile-time constant (just referencing them is enough)
- [ ] 13.8 Run `cd frontend/mobile && ./gradlew :app:testDebugUnitTest`; assert full suite passes

## 14. Verification gate

- [ ] 14.1 `cd frontend/mobile && ./gradlew :app:assembleDebug` succeeds (Room KSP processor generates DAO impl successfully)
- [ ] 14.2 `cd frontend/mobile && ./gradlew :app:testDebugUnitTest` passes (full suite from changes #1-#7)
- [ ] 14.3 `openspec validate memory-creation-flow --strict` reports zero issues
- [ ] 14.4 Emulator smoke (en): home → Memories → Create Memory → pick Old Street template → PhotoSelection → tap Add Photos → system picker opens → pick 3 photos → 3 photos render with numbered overlays → Next → MemoryEdit shows 3 photos in template layout → Save → MemoryReflection → fill mood + insights + feedback → Save → returns to Memories (Memories still shows MockData until change #9; but inspect Room via `adb shell run-as com.mcis.memoir cat /data/data/com.mcis.memoir/databases/memoir.db` to confirm a COMPLETED row exists)
- [ ] 14.5 Emulator smoke (zh): toggle locale → relaunch → repeat 14.4 → all chrome text renders in Chinese
- [ ] 14.6 Emulator smoke (abandonment): start Create Memory flow → pick template → pick 2 photos → press back arrow all the way to home → confirm `filesDir/memories/<memoryId>/` is gone (`adb shell run-as com.mcis.memoir ls files/memories/`) and Room row is gone
- [ ] 14.7 Emulator smoke (orphan): manually set a Room row's `updatedAt` to 8 days ago via `adb shell` sqlite, kill + relaunch app → confirm sweepOrphans deleted the row + dir on next launch
- [ ] 14.8 Record Koin-change follow-up obligation: "Koin change MUST delete the 4 wizard ViewModel factories + `MemoryRepository.memoryRepo` `lateinit` field and inject via `koinViewModel { parametersOf(memoryId) }`"
