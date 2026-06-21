## Why

`memory-library-actions` is change #9, the last entry in the mobile-direct wiring umbrella. After PR #80, `MemoriesScreen` is a two-tab surface: the **Route** tab shows in-progress/completed memories and the **Bookmark** tab shows saved spots with search.

The Route tab still reads `MockData.memories`, so memories the user creates through the change #7 wizard never appear in the library. The Bookmark tab is also local/mock-backed: it keeps a hard-coded `savedSpotIds` list and filters `MockData.spots`, so saved spots are not persisted and cannot follow the content pipeline.

The Route tab's 3-dots menu renders four actions (Edit / Delete / Duplicate / Share) but only Delete is wired to UI, and even that just dismisses the dialog without removing anything. Edit, Duplicate, and Share are inert menu items. Completed cards also carry a dangling detail arrow whose handler is an empty `clickable { /* Detail */ }`, and the whole screen is still on the legacy `selectedLanguage: String` plus `_zh`-suffix lookup that every prior change has been retiring.

This change closes the umbrella by making the Route tab show real Room data, preserving the new Bookmark tab, and making every affordance on the Memories surface either act or be removed.

## What Changes

- Preserve the PR #80 `Route / Bookmark` tab selector. Route is the memory-list tab; Bookmark is the saved-spot tab.
- Replace `MockData.memories` reads on the Route tab with live Room data: `MemoriesScreen` consumes `MemoryRepository.observeByStatus("IN_PROGRESS")` and `observeByStatus("COMPLETED")` through a new `MemoriesViewModel`.
- Replace the Bookmark tab's hard-coded `savedSpotIds` and `MockData.spots` with `ContentRepository.spots()` filtered by a new DataStore-backed `UserPreferencesRepository.bookmarkedSpotIds`, mapped to a `BookmarkSpotCard` DTO and filtered by the tab's search text.
- Wire the 3-dots menu actions on both memory card types:
  - **Edit** re-enters the change #7 wizard keyed by `memoryId`. An `IN_PROGRESS` draft resumes at `MemoryPhotoSelectionDestination(memoryId)`; a `COMPLETED` memory opens at `MemoryEditDestination(memoryId)`.
  - **Delete** wires the existing `DeleteConfirmationDialog` to a new `MemoryRepository.deleteMemory(memoryId)` that removes the row and photo directory regardless of status.
  - **Duplicate** adds `MemoryRepository.duplicateMemory(memoryId)`, deep-copying the row and its photo files into a fresh `memoryId`, preserving `status`.
  - **Share** shares the memory's photos via `Intent.ACTION_SEND` / `ACTION_SEND_MULTIPLE` through a new `FileProvider`.
- **Draft resume**: the `IN_PROGRESS` card body becomes a tappable resume into `MemoryPhotoSelectionDestination(memoryId)`.
- **Bookmark search and navigation**: Bookmark tab search filters saved spots by locale-resolved title; tapping a bookmark emits `NavigateToSpot(spotId)` and `MyAppNavigation` opens `SpotDetailDestination(spotId)`.
- **Remove the dangling arrow**: delete the completed-card detail arrow because no memory-detail destination exists.
- **Drop dead social stats**: remove completed-card likes/comments because the no-backend, single-device architecture has no source for engagement counts.
- Migrate `MemoriesScreen` and its card composables off `selectedLanguage: String` and `R.string.X_zh` lookups to the established VM + AppCompat-locale pattern.
- Add the `MemoriesScreen` Nav3 entry wiring: `MyAppNavigation` constructs the `MemoriesViewModel` with memory repo + content repo + prefs repo and routes effects to the wizard destinations, the share chooser, and spot detail.

## Capabilities

### New Capabilities

- `memory-library-actions`: The Memories surface backed by real state. The `MemoriesViewModel` maps `Memory` domain rows into `IN_PROGRESS` / `COMPLETED` Route-tab card lists, maps bookmarked content spots into Bookmark-tab cards, owns tab/search/menu/dialog UI state, wires the four 3-dots actions, handles draft resume from the card body, adds the `FileProvider` + `file_paths.xml`, and adds the new repository/prefs/content methods needed by this screen. The capability owns the contract that the surface reflects persisted state live and that every affordance either acts or is removed.

### Modified Capabilities

<!-- None. `memory-creation-flow` supplies MemoryRepository / MemoryDatabase / MemoirApplication.memoryRepo / FilePhoto / memoryId-keyed wizard destinations, `language-toggle` supplies AppCompat locale, and the content pipeline supplies persisted spots. This change consumes those foundations and adds only the new methods required by the Memories surface. -->

## Impact

- **Depends on**: change #7 `memory-creation-flow`, change #2 `language-toggle`, and the archived content-pipeline/home-discovery/route-bookmarking foundations. MUST land after those.
- **New files**:
  - `ui/memory/library/MemoriesViewModel.kt`, `MemoriesState.kt`, `MemoriesIntent.kt`, `MemoriesEffect.kt`, `MemoriesViewModelFactory.kt`, `MemoryCard.kt`, `BookmarkSpotCard.kt`.
  - `res/xml/file_paths.xml` (FileProvider config).
  - Tests: `MemoriesViewModelTest.kt`, `MemoryRepositoryActionsTest.kt`.
- **Modified files**:
  - `MemoriesScreen.kt`: VM-driven; PR #80 tab selector preserved; `selectedLanguage`/`_zh` removed; Route cards read `MemoryCard` DTOs and `FilePhoto` covers; Bookmark cards read `BookmarkSpotCard` DTOs and content-pipeline drawable ids; arrow and likes/comments row deleted; menu/dialog/search/tab state wired to intents.
  - `data/memory/MemoryRepository.kt` and `RoomMemoryRepository.kt`: add `deleteMemory(memoryId)` and `duplicateMemory(memoryId): Result<String>`.
  - `data/content/ContentRepository.kt`: add `spots(): Flow<List<Spot>>`.
  - `data/prefs/UserPreferencesRepository.kt`, `DataStoreUserPreferencesRepository.kt`, `UserPrefsKeys.kt`: add DataStore-backed `bookmarkedSpotIds` and `setBookmarkedSpotIds(...)`.
  - `MyAppNavigation.kt`: `Memories` entry builds the `MemoriesViewModel`; routes effects to wizard destinations, the FileProvider share chooser, and `SpotDetailDestination`.
  - `AndroidManifest.xml`: add the `<provider>` for `androidx.core.content.FileProvider`.
- **Dependencies added**: none new (`androidx.core` already provides `FileProvider`).
- **Not changed**: `MockData.memories`, `MockData.templates`, `MockData.routes`, and `MockData.spots` stay intact and compiling, but `MemoriesScreen` no longer consumes `MockData.memories` or `MockData.spots`.
- **Risk**: dropping the likes/comments row is a visible deviation from the mock design; FileProvider share of raw photos (not a rendered composite) is the honest MVP given the editor canvas stays stubbed.
