## ADDED Requirements

### Requirement: Memories library renders live Room data, not MockData

`MemoriesScreen` SHALL render its in-progress and completed sections from `MemoryRepository.observeByStatus("IN_PROGRESS")` and `observeByStatus("COMPLETED")` (both exposed by change #7), surfaced through a `MemoriesViewModel`. The screen MUST NOT read `MockData.memories`.

#### Scenario: Completed memories from Room appear in the completed section
- **WHEN** the Room DB contains one `COMPLETED` memory and the `MemoriesViewModel` state is collected
- **THEN** `state.completed` contains exactly one `MemoryCard` whose `id`, `title`, and `coverRelativePath` come from that Room row, AND `state.inProgress` is empty

#### Scenario: In-progress and completed rows land in their own sections
- **WHEN** the DB contains two `IN_PROGRESS` rows and three `COMPLETED` rows
- **THEN** `state.inProgress.size == 2` AND `state.completed.size == 3`, each ordered by the Room query's `updatedAt DESC`

#### Scenario: MemoriesScreen no longer reads MockData
- **WHEN** the source of `MemoriesScreen.kt` is inspected after this change lands
- **THEN** the file does not reference `MockData.memories` AND `grep -nF 'MockData.memories' MemoriesScreen.kt` returns zero matches

### Requirement: Each card exposes a cover photo, locale-resolved title, and date

`MemoriesViewModel` SHALL map each `Memory` to a `MemoryCard(id, title, coverRelativePath, status, dateLabel, draftProgress)`. `coverRelativePath` SHALL be the first element of `photoRelativePaths` or `null` when empty. Card covers SHALL be rendered through change #7's `FilePhoto(relativePath, filesDir)` Composable, NOT `painterResource`.

#### Scenario: Cover path is the first photo
- **WHEN** a `Memory` has `photoRelativePaths = ["memories/<id>/photo_0.jpg", "memories/<id>/photo_1.jpg"]`
- **THEN** its `MemoryCard.coverRelativePath == "memories/<id>/photo_0.jpg"`

#### Scenario: A memory with no photos has a null cover
- **WHEN** a `Memory` has `photoRelativePaths = []`
- **THEN** its `MemoryCard.coverRelativePath == null`, AND the card renders the `FilePhoto` placeholder rather than crashing

#### Scenario: Completed card date is derived from updatedAt
- **WHEN** a `COMPLETED` memory's `updatedAt` is a known epoch-millis value
- **THEN** the `MemoryCard.dateLabel` is a non-empty string formatted from that timestamp (the screen passes it to `R.string.memories_updated_on`)

### Requirement: In-progress draft progress is derived from the memory's content

For an `IN_PROGRESS` memory, `MemoriesViewModel` SHALL compute `draftProgress` as a `current / total` pair where `total == 3` and `current` increments for (1) the draft existing, (2) having ≥1 photo, and (3) having non-blank `userInsights`. The progress bar on the in-progress card SHALL render from this derived value, NOT from any stored counter.

#### Scenario: A bare draft reports 1 of 3
- **WHEN** an `IN_PROGRESS` memory has `photoRelativePaths = []` and `userInsights = ""`
- **THEN** its `MemoryCard.draftProgress == DraftProgress(current = 1, total = 3)`

#### Scenario: A draft with photos and insights reports 3 of 3
- **WHEN** an `IN_PROGRESS` memory has ≥1 photo and non-blank `userInsights`
- **THEN** its `MemoryCard.draftProgress == DraftProgress(current = 3, total = 3)`

### Requirement: Draft resume and Edit re-enter the change #7 wizard by memoryId

Tapping the body of an `IN_PROGRESS` card ("Continue Editing") OR its menu "Edit Memory" SHALL emit an effect navigating to `MemoryPhotoSelectionDestination(memoryId)`. The menu "Edit Memory" on a `COMPLETED` card SHALL emit an effect navigating to `MemoryEditDestination(memoryId)`. No change #7 wizard code is modified.

#### Scenario: Continue Editing resumes an in-progress draft at the photo step
- **WHEN** the VM receives `MemoriesIntent.ContinueEditingClicked(id)` for an `IN_PROGRESS` memory
- **THEN** the effects Flow emits exactly one `MemoriesEffect.NavigateToWizard(memoryId = id, entry = PHOTO_SELECTION)`

#### Scenario: Edit on a completed memory opens the edit step
- **WHEN** the VM receives `MemoriesIntent.EditClicked(id)` for a `COMPLETED` memory
- **THEN** the effects Flow emits exactly one `MemoriesEffect.NavigateToWizard(memoryId = id, entry = EDIT)`

#### Scenario: Backing out of a re-edited completed memory does not delete it
- **WHEN** a completed memory is reopened via Edit and the wizard VM's `onCleared()` later fires without a save
- **THEN** `MemoryRepository.cancelDraftIfInProgress(memoryId)` no-ops (status is `COMPLETED`), AND the memory row and its photos still exist — relying on change #7's existing guard, with no code change here

### Requirement: Delete removes the memory and its photos regardless of status

`MemoryRepository.deleteMemory(memoryId)` SHALL delete the DB row AND recursively delete `filesDir/memories/<memoryId>/` for a memory of ANY status, guarded by the same UUID-shape check and `startsWith(filesDir/memories)` containment check change #7 established. The library's `DeleteConfirmationDialog` confirm button SHALL invoke it.

#### Scenario: Deleting a completed memory removes row and files
- **WHEN** a test creates a `COMPLETED` memory with one photo file on disk and calls `deleteMemory(memoryId)`
- **THEN** the DB row no longer exists, AND `filesDir/memories/<memoryId>/` does not exist on disk

#### Scenario: Deleting an in-progress draft also works
- **WHEN** a test calls `deleteMemory(memoryId)` against an `IN_PROGRESS` row
- **THEN** the row and its photo directory are both removed (unlike `cancelDraftIfInProgress`, status is not a precondition)

#### Scenario: Invalid memoryId is a guarded no-op
- **WHEN** a test calls `deleteMemory("../etc")`
- **THEN** no directory outside `filesDir/memories/` is touched AND no exception escapes (the UUID guard short-circuits)

#### Scenario: Confirm dialog drives the delete
- **WHEN** the VM receives `MemoriesIntent.DeleteClicked(id)` then `MemoriesIntent.DeleteConfirmed`
- **THEN** `MemoryRepository.deleteMemory(id)` is invoked exactly once, AND the menu/dialog UI state resets (`activeMenuMemoryId == null`, `showDeleteDialog == false`)

#### Scenario: Cancelling the dialog deletes nothing
- **WHEN** the VM receives `MemoriesIntent.DeleteClicked(id)` then `MemoriesIntent.DeleteCancelled`
- **THEN** `MemoryRepository.deleteMemory(...)` is never invoked, AND `showDeleteDialog == false`

### Requirement: Duplicate deep-copies the memory and its photo files

`MemoryRepository.duplicateMemory(memoryId)` SHALL create a new row with a fresh UUID, copy every photo file from `filesDir/memories/<src>/` into `filesDir/memories/<newId>/` with re-sequenced `photo_<i>.jpg` names, set `createdAt = updatedAt = now`, preserve `status`, and return the new id wrapped in `Result`.

#### Scenario: Duplicating a 2-photo completed memory copies files and row
- **WHEN** a test duplicates a `COMPLETED` memory with two photo files
- **THEN** a new row exists with a different `id` and `status == "COMPLETED"`, AND `filesDir/memories/<newId>/photo_0.jpg` and `photo_1.jpg` both exist and are byte-identical to the source files, AND the new row's persisted `photoLocalPaths` JSON decodes to the two new relative paths

#### Scenario: The new memoryId differs from the source
- **WHEN** `duplicateMemory(memoryId)` succeeds
- **THEN** the returned id is a UUID-shape string not equal to `memoryId`

#### Scenario: Duplicate appears in the library automatically
- **WHEN** the VM receives `MemoriesIntent.DuplicateClicked(id)` and the repository succeeds
- **THEN** `MemoryRepository.duplicateMemory(id)` is invoked exactly once, AND the new card surfaces through the observed Flow with no explicit refresh

### Requirement: Share exports the memory's photos via FileProvider

`MemoriesViewModel` SHALL emit `MemoriesEffect.ShareMemory(relativePaths, title)` for the tapped memory; the Composable collector SHALL build content URIs via `FileProvider.getUriForFile(..., "${packageName}.fileprovider", ...)` and launch an `ACTION_SEND` (single photo) or `ACTION_SEND_MULTIPLE` (multiple) chooser with `FLAG_GRANT_READ_URI_PERMISSION` and the title as `EXTRA_TEXT`. A `FileProvider` exposing `filesDir/memories/` SHALL be declared in `AndroidManifest.xml`.

#### Scenario: Share emits an effect carrying the memory's photo paths
- **WHEN** the VM receives `MemoriesIntent.ShareClicked(id)` for a memory with two photos titled "Tainan Day"
- **THEN** the effects Flow emits exactly one `MemoriesEffect.ShareMemory(relativePaths = [the two paths], title = "Tainan Day")` — no Android `Intent` is constructed in the VM

#### Scenario: Sharing a memory with no photos is disabled
- **WHEN** a card's `coverRelativePath == null` (zero photos)
- **THEN** the Share menu item is disabled / its intent is not emitted (no empty `ACTION_SEND`)

#### Scenario: FileProvider is declared with a files-path for memories
- **WHEN** the repo is inspected after this change lands
- **THEN** `AndroidManifest.xml` declares a `<provider>` for `androidx.core.content.FileProvider` with authority `${applicationId}.fileprovider`, AND `res/xml/file_paths.xml` contains a `<files-path … path="memories/" />` entry

### Requirement: The dangling detail arrow is removed from completed cards

`CompletedMemoryCard` SHALL NOT render the detail arrow `Box` that previously held an empty `clickable { /* Detail */ }` (`MemoriesScreen.kt:490-507`). No memory-detail destination exists in the mobile-direct app.

#### Scenario: No arrow_right detail affordance remains on the card
- **WHEN** the source of `MemoriesScreen.kt` is inspected after this change lands
- **THEN** `CompletedMemoryCard` contains no `clickable { /* Detail */ }` block and no `R.drawable.arrow_right` reference for a bottom-end detail button

### Requirement: Dead social stats are removed from completed cards

The likes/comments `Row` on `CompletedMemoryCard` (`MemoriesScreen.kt:445-473`) SHALL be removed, because the no-backend single-device architecture has no source for engagement counts and the Room `Memory` model carries no `likes`/`comments` fields.

#### Scenario: Completed card no longer references likes or comments
- **WHEN** the source of `MemoriesScreen.kt` is inspected after this change lands
- **THEN** `CompletedMemoryCard` contains no reference to `memory.likes` or `memory.comments` and no `UntitledIcons.CommentIcon` / `SavedFilled` engagement row

### Requirement: Memories surface is migrated off selectedLanguage and _zh lookups

`MemoriesScreen` and its card composables SHALL NOT declare a `selectedLanguage: String` parameter and SHALL NOT use any `R.string.X_zh` runtime lookup. Chrome text MUST be accessed via `stringResource(R.string.X)` resolved by the AppCompat application locale.

#### Scenario: Screen signature drops selectedLanguage and _zh lookups
- **WHEN** the source of `MemoriesScreen.kt` is inspected after this change lands
- **THEN** the `MemoriesScreen` Composable signature has no `selectedLanguage` parameter, AND `grep -nE 'R\.string\.\w+_zh' MemoriesScreen.kt` returns zero matches

### Requirement: MockData consumers continue to compile

This change SHALL NOT delete or modify `MockData.memories`, `MockData.templates`, `MockData.routes`, or `MockData.spots`, nor the `data.MemoryData` / `data.MemoryStatus` types they use.

#### Scenario: Build succeeds with MockData intact
- **WHEN** `./gradlew :app:assembleDebug` is run after this change lands
- **THEN** the build succeeds, AND `data.MockData.memories` and `data.MockData.templates` references elsewhere still compile
