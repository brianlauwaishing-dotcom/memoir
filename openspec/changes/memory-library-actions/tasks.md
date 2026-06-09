## 1. Repository methods (delete + duplicate)

- [ ] 1.1 In `data/memory/MemoryRepository.kt` (the interface from change #7), add `suspend fun deleteMemory(memoryId: String)` and `suspend fun duplicateMemory(memoryId: String): Result<String>`
- [ ] 1.2 In `RoomMemoryRepository.kt`, implement `deleteMemory` per design D4: reuse the existing `String.isValidUuid()` guard; if invalid, return early; else recursively delete `File(filesDir, "memories/$memoryId")` only when it `startsWith(File(filesDir, "memories"))`, then `dao.delete(memoryId)`. Status is NOT a precondition (distinct from `cancelDraftIfInProgress`)
- [ ] 1.3 In `RoomMemoryRepository.kt`, implement `duplicateMemory` per design D5 inside `runCatching`: require valid UUID; `dao.getOnce(memoryId)` or error; generate `newId = UUID.randomUUID().toString()`; decode `src.photoLocalPaths`; copy each `File(filesDir, rel)` to `memories/$newId/photo_$i.jpg` (`parentFile?.mkdirs()`, `copyTo(overwrite = true)`); `dao.upsert(src.copy(id = newId, createdAt = now, updatedAt = now, photoLocalPaths = json.encodeToString(newPaths)))`; return `newId`. On failure, best-effort `File(filesDir, "memories/$newId").deleteRecursively()` before the `Result.failure` propagates (design Risks)
- [ ] 1.4 Confirm `observeByStatus(status): Flow<List<Memory>>` is already exposed by change #7's interface (it is) — no change needed; this change only consumes it

## 2. FileProvider for sharing app-private photos

- [ ] 2.1 Create `res/xml/file_paths.xml` with `<paths><files-path name="memories" path="memories/" /></paths>`
- [ ] 2.2 In `AndroidManifest.xml`, add inside `<application>` a `<provider android:name="androidx.core.content.FileProvider" android:authorities="${applicationId}.fileprovider" android:exported="false" android:grantUriPermissions="true">` with the `<meta-data android:name="android.support.FILE_PROVIDER_PATHS" android:resource="@xml/file_paths" />` child (design D7)
- [ ] 2.3 Confirm `androidx.core` (providing `androidx.core.content.FileProvider`) is already a dependency — no catalog change expected

## 3. Library MVI scaffolding (`ui/memory/library/`)

- [ ] 3.1 Create `MemoryCard.kt`: `data class MemoryCard(val id: String, val title: String, val coverRelativePath: String?, val status: String, val dateLabel: String, val draftProgress: DraftProgress)` and `data class DraftProgress(val current: Int, val total: Int)`
- [ ] 3.2 Create `MemoriesState.kt`: `data class MemoriesState(val inProgress: List<MemoryCard> = emptyList(), val completed: List<MemoryCard> = emptyList(), val isLoading: Boolean = true, val activeMenuMemoryId: String? = null, val showDeleteDialog: Boolean = false)`
- [ ] 3.3 Create `MemoriesIntent.kt` sealed interface: `MoreClicked(id)`, `MenuDismissed`, `ContinueEditingClicked(id)`, `EditClicked(id)`, `DeleteClicked(id)`, `DeleteConfirmed`, `DeleteCancelled`, `DuplicateClicked(id)`, `ShareClicked(id)`, `CreateMemoryClicked`
- [ ] 3.4 Create `MemoriesEffect.kt` sealed interface: `NavigateToWizard(memoryId: String, entry: WizardEntry)` with `enum class WizardEntry { PHOTO_SELECTION, EDIT }`; `ShareMemory(relativePaths: List<String>, title: String)`; `NavigateToCreate`
- [ ] 3.5 Create `MemoriesViewModelFactory.kt` mirroring the change #7 factory pattern, closing over `MemoryRepository` (from `MemoirApplication.memoryRepo`)

## 4. MemoriesViewModel

- [ ] 4.1 Create `MemoriesViewModel.kt` per design D1: `combine(repo.observeByStatus(IN_PROGRESS), repo.observeByStatus(COMPLETED)) { … }` mapping each `Memory` via `toCard()`, `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MemoriesState(isLoading = true))`. Merge the menu/dialog UI state (`activeMenuMemoryId`, `showDeleteDialog`) held in a second `MutableStateFlow` into the exposed `state`. Also retain the latest emitted `Map<String, Memory>` (id → domain row) in a VM field for `ShareClicked` to read photo paths from — avoids a second repo read
- [ ] 4.2 Implement `Memory.toCard()`: `coverRelativePath = photoRelativePaths.firstOrNull()`; `dateLabel` formatted from `updatedAt` (e.g. `SimpleDateFormat("yyyy/MM/dd")` or `DateTimeFormatter`); `draftProgress = draftProgress()`
- [ ] 4.3 Implement `Memory.draftProgress()` per design D2: start at 1, +1 if `photoRelativePaths.isNotEmpty()`, +1 if `userInsights.isNotBlank()`, `total = 3`
- [ ] 4.4 Implement intent handling: `MoreClicked` → set `activeMenuMemoryId`; `MenuDismissed` → clear it; `ContinueEditingClicked(id)` and `EditClicked(id)` on an IN_PROGRESS card → emit `NavigateToWizard(id, PHOTO_SELECTION)`; `EditClicked(id)` on a COMPLETED card → emit `NavigateToWizard(id, EDIT)` (look up the card's status from current state)
- [ ] 4.5 Implement `DeleteClicked(id)` → set `showDeleteDialog = true` (keep `activeMenuMemoryId = id`); `DeleteConfirmed` → `viewModelScope.launch { repo.deleteMemory(id) }` then reset `activeMenuMemoryId = null`, `showDeleteDialog = false`; `DeleteCancelled` → `showDeleteDialog = false`
- [ ] 4.6 Implement `DuplicateClicked(id)` → `viewModelScope.launch { repo.duplicateMemory(id) }`, clear menu; on `Result.failure` set a transient error (optional toast effect) — list updates via the observed Flow
- [ ] 4.7 Implement `ShareClicked(id)` → look up the card; if `coverRelativePath == null` do nothing; else read the memory's `photoRelativePaths` + `title` from the retained `Map<String, Memory>` (task 4.1) and emit `ShareMemory(relativePaths, title)`. Clear the menu. (Do NOT add a one-shot repo read — the VM already holds the full domain rows from the observed Flow)
- [ ] 4.8 Implement `CreateMemoryClicked` → emit `NavigateToCreate`

## 5. Rewrite MemoriesScreen (VM-driven, cleanup)

- [ ] 5.1 Change the signature to `MemoriesScreen(viewModel: MemoriesViewModel, onNavigateToHome, onNavigateToSaved, onNavigateToMemories, onCreateMemoryClick, onNavigateToWizard: (String, WizardEntry) -> Unit, modifier)` — remove `selectedLanguage`. Collect `state` with `collectAsStateWithLifecycle()`
- [ ] 5.2 Remove `isChinese` and replace every `if (isChinese) R.string.X_zh else R.string.X` with `stringResource(R.string.X)` (values-zh already mirrors all `memories_*`, `cancel_button`, `delete_button` keys)
- [ ] 5.3 Render `state.inProgress` / `state.completed` lists of `MemoryCard`. Render an empty-state (just the create button visible) when both are empty (design Risks: empty-state)
- [ ] 5.4 Rewrite `InProgressMemoryCard` to take a `MemoryCard`: cover via `FilePhoto(card.coverRelativePath, filesDir)` (change #7) with placeholder when null; progress bar from `card.draftProgress`; whole card body `clickable { viewModel.onIntent(ContinueEditingClicked(card.id)) }`; more button → `MoreClicked(card.id)`
- [ ] 5.5 Rewrite `CompletedMemoryCard` to take a `MemoryCard`: cover via `FilePhoto`; date via `stringResource(R.string.memories_updated_on, card.dateLabel)`; more button → `MoreClicked(card.id)`. **DELETE** the bottom-end arrow `Box` (old `:490-507`) and **DELETE** the likes/comments `Row` (old `:445-473`) per spec + design D6
- [ ] 5.6 Wire `MemoryActionMenu` to pass all four handlers: `onEditClick`, `onDeleteClick`, `onDuplicateClick`, `onShareClick`, each dispatching the matching intent for `state.activeMenuMemoryId`. Disable the Share item when the active card has `coverRelativePath == null`
- [ ] 5.7 Wire `DeleteConfirmationDialog`: show when `state.showDeleteDialog`; `onCancel` → `DeleteCancelled`; `onDelete` → `DeleteConfirmed`
- [ ] 5.8 Drive the menu overlay visibility from `state.activeMenuMemoryId` + `MenuDismissed` instead of local `remember` state. Remove the now-unused `MockData` / `MemoryData` / `data.MemoryStatus` imports; import `MemoryStatus` constants from `data/memory`
- [ ] 5.9 Collect `viewModel.effects` in a `LaunchedEffect`: `NavigateToWizard` → `onNavigateToWizard(memoryId, entry)`; `NavigateToCreate` → `onCreateMemoryClick()`; `ShareMemory` → build `FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(context.filesDir, rel))` for each path and launch `ACTION_SEND` / `ACTION_SEND_MULTIPLE` chooser with `FLAG_GRANT_READ_URI_PERMISSION` + `EXTRA_TEXT = title` (design D7)

## 6. Navigation wiring

- [ ] 6.1 In `MyAppNavigation.kt`, the `Memories` entry constructs `MemoriesViewModel` via its Factory (from `MemoirApplication.memoryRepo`) and passes `onNavigateToWizard = { memoryId, entry -> backStack.add(when (entry) { PHOTO_SELECTION -> MemoryPhotoSelectionDestination(memoryId); EDIT -> MemoryEditDestination(memoryId) }) }`
- [ ] 6.2 Keep `onCreateMemoryClick` pointing at the existing `MemoryTemplateDestination` entry (unchanged from change #7)
- [ ] 6.3 Confirm the bottom-nav `Memories` tab still routes here and that `MemoriesScreen` no longer receives a `selectedLanguage` argument anywhere it is constructed

## 7. Tests (JUnit5 + MockK + Turbine)

- [ ] 7.1 `MemoryRepositoryActionsTest.kt` (Room in-memory + temp `filesDir`, same harness as change #7's `MemoryRepositoryTest`): `deleteMemory` removes a COMPLETED row + its dir; `deleteMemory` works on IN_PROGRESS; `deleteMemory("../etc")` is a guarded no-op
- [ ] 7.2 In the same test, duplicate paths: `duplicateMemory` of a 2-photo COMPLETED memory creates a new id, copies both files byte-identically into `memories/<newId>/`, preserves `status`, and the new `photoLocalPaths` lists the re-sequenced paths
- [ ] 7.3 `MemoriesViewModelTest.kt`: `combine` mapping puts IN_PROGRESS rows in `inProgress` and COMPLETED in `completed`; `coverRelativePath` is the first photo / null when empty; `draftProgress` is `1/3` for a bare draft and `3/3` with photos+insights
- [ ] 7.4 In the same VM test: `ContinueEditingClicked` and IN_PROGRESS `EditClicked` emit `NavigateToWizard(_, PHOTO_SELECTION)`; COMPLETED `EditClicked` emits `NavigateToWizard(_, EDIT)`
- [ ] 7.5 In the same VM test: `DeleteClicked` → `DeleteConfirmed` calls `repo.deleteMemory` once and resets dialog state; `DeleteCancelled` never calls it; `DuplicateClicked` calls `repo.duplicateMemory` once
- [ ] 7.6 In the same VM test: `ShareClicked` on a memory with photos emits exactly one `ShareMemory(relativePaths, title)`; `ShareClicked` on a zero-photo card emits no effect

## 8. Verification

- [ ] 8.1 `cd frontend/mobile && ./gradlew :app:assembleDebug` succeeds with `MockData` intact (spec: MockData consumers compile)
- [ ] 8.2 `./gradlew :app:testDebugUnitTest` passes for the two new test files
- [ ] 8.3 `grep -nE 'R\.string\.\w+_zh' MemoriesScreen.kt` and `grep -nF 'MockData.memories' MemoriesScreen.kt` both return zero matches; confirm no `clickable { /* Detail */ }` and no `memory.likes`/`memory.comments` remain
- [ ] 8.4 Emulator smoke: create a memory via the wizard → it appears in the library; resume a draft via "Continue Editing"; Edit a completed memory; Duplicate (a copy appears); Delete (confirm dialog → card disappears); Share (chooser opens with the photo(s)). Confirm with the reviewer the likes/comments removal (design D6)
