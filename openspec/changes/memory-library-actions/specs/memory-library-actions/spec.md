## ADDED Requirements

### Requirement: Memories surface preserves Route and Bookmark tabs

`MemoriesScreen` SHALL render the PR #80 two-tab structure. The Route tab SHALL contain the memory library list and create-memory CTA. The Bookmark tab SHALL contain saved spot search and saved spot cards. The selected tab SHALL be driven by `MemoriesViewModel` state, not local Composable-only state.

#### Scenario: Route tab renders memory sections
- **WHEN** `MemoriesState.selectedTab == ROUTE`
- **THEN** the screen renders the in-progress/completed memory sections and the create-memory CTA

#### Scenario: Bookmark tab renders saved spot search
- **WHEN** `MemoriesState.selectedTab == BOOKMARK`
- **THEN** the screen renders a bookmark search field and the saved spot list from `state.bookmarkedSpots`

#### Scenario: Tab selection is state-driven
- **WHEN** the user taps the Bookmark tab
- **THEN** the screen dispatches `MemoriesIntent.TabSelected(BOOKMARK)` and the next state emission has `selectedTab == BOOKMARK`

### Requirement: Route tab renders live Room data, not MockData

`MemoriesScreen` SHALL render its Route-tab in-progress and completed sections from `MemoryRepository.observeByStatus("IN_PROGRESS")` and `observeByStatus("COMPLETED")`, surfaced through `MemoriesViewModel`. The screen MUST NOT read `MockData.memories`.

#### Scenario: Completed memories from Room appear in the completed section
- **WHEN** the Room DB contains one `COMPLETED` memory and the `MemoriesViewModel` state is collected
- **THEN** `state.completed` contains exactly one `MemoryCard` whose `id`, `title`, and `coverRelativePath` come from that Room row, AND `state.inProgress` is empty

#### Scenario: In-progress and completed rows land in their own sections
- **WHEN** the DB contains two `IN_PROGRESS` rows and three `COMPLETED` rows
- **THEN** `state.inProgress.size == 2` AND `state.completed.size == 3`, each ordered by the Room query's `updatedAt DESC`

#### Scenario: MemoriesScreen no longer reads MockData memories
- **WHEN** the source of `MemoriesScreen.kt` is inspected after this change lands
- **THEN** the file does not reference `MockData.memories`

### Requirement: Memory cards expose cover photo, title, date, and derived draft progress

`MemoriesViewModel` SHALL map each `Memory` to a `MemoryCard(id, title, coverRelativePath, status, dateLabel, draftProgress)`. `coverRelativePath` SHALL be the first element of `photoRelativePaths` or `null` when empty. Card covers SHALL be rendered through change #7's `FilePhoto(relativePath, filesDir)` Composable, NOT `painterResource`.

For an `IN_PROGRESS` memory, `draftProgress.total == 3` and `draftProgress.current` SHALL increment for (1) the draft existing, (2) having at least one photo, and (3) having non-blank `userInsights`.

#### Scenario: Cover path is the first photo
- **WHEN** a `Memory` has `photoRelativePaths = ["memories/<id>/photo_0.jpg", "memories/<id>/photo_1.jpg"]`
- **THEN** its `MemoryCard.coverRelativePath == "memories/<id>/photo_0.jpg"`

#### Scenario: A memory with no photos has a null cover
- **WHEN** a `Memory` has `photoRelativePaths = []`
- **THEN** its `MemoryCard.coverRelativePath == null`, AND the card renders a placeholder rather than crashing

#### Scenario: Completed card date is derived from updatedAt
- **WHEN** a `COMPLETED` memory's `updatedAt` is a known epoch-millis value
- **THEN** the `MemoryCard.dateLabel` is a non-empty string formatted from that timestamp

#### Scenario: A bare draft reports 1 of 3
- **WHEN** an `IN_PROGRESS` memory has `photoRelativePaths = []` and `userInsights = ""`
- **THEN** its `MemoryCard.draftProgress == DraftProgress(current = 1, total = 3)`

#### Scenario: A draft with photos and insights reports 3 of 3
- **WHEN** an `IN_PROGRESS` memory has at least one photo and non-blank `userInsights`
- **THEN** its `MemoryCard.draftProgress == DraftProgress(current = 3, total = 3)`

### Requirement: Bookmark tab renders persisted saved spots, not hard-coded MockData spots

The Bookmark tab SHALL render `BookmarkSpotCard` rows derived from `ContentRepository.spots()` filtered by `UserPreferencesRepository.bookmarkedSpotIds`. `MemoriesScreen` MUST NOT keep a hard-coded `savedSpotIds` list and MUST NOT read `MockData.spots`.

#### Scenario: No saved spots renders an empty bookmark list
- **WHEN** `ContentRepository.spots()` emits three spots and `bookmarkedSpotIds` emits `emptySet()`
- **THEN** `state.bookmarkedSpots` is empty

#### Scenario: Saved spots preserve content order
- **WHEN** `ContentRepository.spots()` emits `[A, B, C]` and `bookmarkedSpotIds` emits `setOf("C", "A")`
- **THEN** `state.bookmarkedSpots.map { it.id } == ["A", "C"]`

#### Scenario: Bookmark spot cards are locale and resource resolved
- **WHEN** a bookmarked `Spot` has a localized title and `heroImage = "grand_mazu"`
- **THEN** its `BookmarkSpotCard.title` is resolved using the injected locale AND `heroDrawableRes` is resolved with `Resources.getIdentifier("grand_mazu", "drawable", packageName)`

#### Scenario: MemoriesScreen no longer reads MockData spots
- **WHEN** the source of `MemoriesScreen.kt` is inspected after this change lands
- **THEN** the file contains no `MockData.spots` reference and no `savedSpotIds` state

### Requirement: Spot bookmark IDs are persisted in UserPreferencesRepository

`UserPreferencesRepository` SHALL expose `bookmarkedSpotIds: Flow<Set<String>>` and `setBookmarkedSpotIds(set)`. `DataStoreUserPreferencesRepository` SHALL persist them under a `saved_spot_ids` string-set key and default to `emptySet()` when absent.

#### Scenario: Spot bookmark IDs default to empty
- **WHEN** DataStore has no `saved_spot_ids` value
- **THEN** `bookmarkedSpotIds.first() == emptySet()`

#### Scenario: Spot bookmark IDs persist
- **WHEN** `setBookmarkedSpotIds(setOf("grand_mazu"))` is called
- **THEN** the next `bookmarkedSpotIds` emission contains exactly `"grand_mazu"`

#### Scenario: Route bookmark persistence is unchanged
- **WHEN** route bookmark tests are run after this change
- **THEN** `bookmarkedRouteIds` and `setBookmarkedRouteIds(...)` keep their existing behavior

### Requirement: Bookmark search filters saved spots by title

The Bookmark tab search field SHALL be driven by `MemoriesState.bookmarkSearchQuery`. Updating it SHALL dispatch `MemoriesIntent.BookmarkSearchChanged(query)`. Filtering SHALL happen after saved-spot DTO mapping and SHALL match `BookmarkSpotCard.title` case-insensitively.

#### Scenario: Empty query shows all saved spots
- **WHEN** two saved spot cards exist and `bookmarkSearchQuery == ""`
- **THEN** both cards appear in `state.bookmarkedSpots`

#### Scenario: Query filters by title
- **WHEN** saved spot cards are titled `"Grand Mazu Temple"` and `"Grand Wumiao Temple"`
- **AND** the VM receives `BookmarkSearchChanged("wumiao")`
- **THEN** the next state contains only the `"Grand Wumiao Temple"` card

### Requirement: Bookmark card taps navigate to spot detail

Tapping a Bookmark tab card SHALL emit `MemoriesEffect.NavigateToSpot(spotId)`. `MyAppNavigation` SHALL handle that effect by adding `SpotDetailDestination(spotId)`. Bookmark card taps MUST NOT navigate to `MemoryPhotoSelectionDestination`.

#### Scenario: Bookmark card click emits spot navigation
- **WHEN** the VM receives `MemoriesIntent.BookmarkSpotClicked("grand_mazu")`
- **THEN** the effects Flow emits exactly one `MemoriesEffect.NavigateToSpot("grand_mazu")`

#### Scenario: Navigation opens spot detail
- **WHEN** `MyAppNavigation` receives `NavigateToSpot("grand_mazu")`
- **THEN** it adds `SpotDetailDestination("grand_mazu")` to the back stack

### Requirement: Draft resume and Edit re-enter the change #7 wizard by memoryId

Tapping the body of an `IN_PROGRESS` card OR its menu "Edit" SHALL emit an effect navigating to `MemoryPhotoSelectionDestination(memoryId)`. The menu "Edit" on a `COMPLETED` card SHALL emit an effect navigating to `MemoryEditDestination(memoryId)`.

#### Scenario: Continue Editing resumes an in-progress draft at the photo step
- **WHEN** the VM receives `MemoriesIntent.ContinueEditingClicked(id)` for an `IN_PROGRESS` memory
- **THEN** the effects Flow emits exactly one `MemoriesEffect.NavigateToWizard(memoryId = id, entry = PHOTO_SELECTION)`

#### Scenario: Edit on a completed memory opens the edit step
- **WHEN** the VM receives `MemoriesIntent.EditClicked(id)` for a `COMPLETED` memory
- **THEN** the effects Flow emits exactly one `MemoriesEffect.NavigateToWizard(memoryId = id, entry = EDIT)`

#### Scenario: Backing out of a re-edited completed memory does not delete it
- **WHEN** a completed memory is reopened via Edit and the wizard VM's `onCleared()` later fires without a save
- **THEN** `MemoryRepository.cancelDraftIfInProgress(memoryId)` no-ops because status is `COMPLETED`

### Requirement: Delete removes the memory and its photos regardless of status

`MemoryRepository.deleteMemory(memoryId)` SHALL delete the DB row AND recursively delete `filesDir/memories/<memoryId>/` for a memory of ANY status, guarded by the same UUID-shape check and `startsWith(filesDir/memories)` containment check change #7 established. The library's `DeleteConfirmationDialog` confirm button SHALL invoke it.

#### Scenario: Deleting a completed memory removes row and files
- **WHEN** a test creates a `COMPLETED` memory with one photo file on disk and calls `deleteMemory(memoryId)`
- **THEN** the DB row no longer exists, AND `filesDir/memories/<memoryId>/` does not exist on disk

#### Scenario: Deleting an in-progress draft also works
- **WHEN** a test calls `deleteMemory(memoryId)` against an `IN_PROGRESS` row
- **THEN** the row and its photo directory are both removed

#### Scenario: Invalid memoryId is a guarded no-op
- **WHEN** a test calls `deleteMemory("../etc")`
- **THEN** no directory outside `filesDir/memories/` is touched AND no exception escapes

#### Scenario: Confirm dialog drives the delete
- **WHEN** the VM receives `MemoriesIntent.DeleteClicked(id)` then `MemoriesIntent.DeleteConfirmed`
- **THEN** `MemoryRepository.deleteMemory(id)` is invoked exactly once, AND the menu/dialog UI state resets

#### Scenario: Cancelling the dialog deletes nothing
- **WHEN** the VM receives `MemoriesIntent.DeleteClicked(id)` then `MemoriesIntent.DeleteCancelled`
- **THEN** `MemoryRepository.deleteMemory(...)` is never invoked, AND `showDeleteDialog == false`

### Requirement: Duplicate deep-copies the memory and its photo files

`MemoryRepository.duplicateMemory(memoryId)` SHALL create a new row with a fresh UUID, copy every photo file from `filesDir/memories/<src>/` into `filesDir/memories/<newId>/` with re-sequenced `photo_<i>.jpg` names, set `createdAt = updatedAt = now`, preserve `status`, and return the new id wrapped in `Result`.

#### Scenario: Duplicating a 2-photo completed memory copies files and row
- **WHEN** a test duplicates a `COMPLETED` memory with two photo files
- **THEN** a new row exists with a different `id` and `status == "COMPLETED"`, AND both copied files exist and are byte-identical to the source files

#### Scenario: The new memoryId differs from the source
- **WHEN** `duplicateMemory(memoryId)` succeeds
- **THEN** the returned id is a UUID-shape string not equal to `memoryId`

#### Scenario: Duplicate appears in the library automatically
- **WHEN** the VM receives `MemoriesIntent.DuplicateClicked(id)` and the repository succeeds
- **THEN** `MemoryRepository.duplicateMemory(id)` is invoked exactly once, AND the new card surfaces through the observed Flow with no explicit refresh

### Requirement: Share exports the memory's photos via FileProvider

`MemoriesViewModel` SHALL emit `MemoriesEffect.ShareMemory(relativePaths, title)` for the tapped memory. The Composable collector SHALL build content URIs via `FileProvider.getUriForFile(..., "${packageName}.fileprovider", ...)` and launch an `ACTION_SEND` (single photo) or `ACTION_SEND_MULTIPLE` (multiple photos) chooser with `FLAG_GRANT_READ_URI_PERMISSION` and the title as `EXTRA_TEXT`. A `FileProvider` exposing `filesDir/memories/` SHALL be declared in `AndroidManifest.xml`.

#### Scenario: Share emits an effect carrying the memory's photo paths
- **WHEN** the VM receives `MemoriesIntent.ShareClicked(id)` for a memory with two photos titled `"Tainan Day"`
- **THEN** the effects Flow emits exactly one `MemoriesEffect.ShareMemory(relativePaths = [the two paths], title = "Tainan Day")`

#### Scenario: Sharing a memory with no photos is disabled
- **WHEN** a card's `coverRelativePath == null`
- **THEN** the Share menu item is disabled or its intent emits no effect

#### Scenario: FileProvider is declared with a files-path for memories
- **WHEN** the repo is inspected after this change lands
- **THEN** `AndroidManifest.xml` declares an `androidx.core.content.FileProvider` with authority `${applicationId}.fileprovider`, AND `res/xml/file_paths.xml` contains a `files-path` for `memories/`

### Requirement: The dangling detail arrow is removed from completed cards

`CompletedMemoryCard` SHALL NOT render the detail arrow `Box` that previously held an empty `clickable { /* Detail */ }`. No memory-detail destination exists in the mobile-direct app.

#### Scenario: No arrow_right detail affordance remains on the card
- **WHEN** the source of `MemoriesScreen.kt` is inspected after this change lands
- **THEN** `CompletedMemoryCard` contains no `clickable { /* Detail */ }` block and no `R.drawable.arrow_right` reference for a bottom-end detail button

### Requirement: Dead social stats are removed from completed cards

The likes/comments row on `CompletedMemoryCard` SHALL be removed, because the no-backend single-device architecture has no source for engagement counts and the Room `Memory` model carries no `likes`/`comments` fields.

#### Scenario: Completed card no longer references likes or comments
- **WHEN** the source of `MemoriesScreen.kt` is inspected after this change lands
- **THEN** `CompletedMemoryCard` contains no reference to `memory.likes` or `memory.comments` and no engagement-count row

### Requirement: Memories surface is migrated off selectedLanguage and _zh lookups

`MemoriesScreen` and its card composables SHALL NOT declare a `selectedLanguage: String` parameter and SHALL NOT use any `R.string.X_zh` runtime lookup. Chrome text MUST be accessed via `stringResource(R.string.X)` resolved by the AppCompat application locale.

#### Scenario: Screen signature drops selectedLanguage and _zh lookups
- **WHEN** the source of `MemoriesScreen.kt` is inspected after this change lands
- **THEN** the `MemoriesScreen` Composable signature has no `selectedLanguage` parameter, AND no `R.string.*_zh` lookup remains

#### Scenario: New tab/search strings have bilingual coverage
- **WHEN** string resources are inspected after this change lands
- **THEN** `memories_route_tab`, `memories_bookmark_tab`, `memories_search_bookmarks`, `memories_choose_bookmarks`, and `memories_empty_bookmarks` exist in both `values/strings.xml` and `values-zh/strings.xml`

### Requirement: MockData consumers continue to compile

This change SHALL NOT delete or modify `MockData.memories`, `MockData.templates`, `MockData.routes`, or `MockData.spots`, nor the legacy data types they use.

#### Scenario: Build succeeds with MockData intact
- **WHEN** `./gradlew :app:assembleDebug` is run after this change lands
- **THEN** the build succeeds, AND legacy `data.MockData` references outside `MemoriesScreen` still compile
