## Context

PR #80 changed `MemoriesScreen` from a single memory list into a two-tab page:

- **Route**: in-progress and completed memory cards, create-memory CTA, card menu, delete dialog.
- **Bookmark**: search field and a list of saved spots rendered by `SimpleBookmarkCard`.

The implementation is still local/mock-backed:

- Route tab reads `MockData.memories` and splits by `data.MemoryStatus`.
- Bookmark tab keeps `savedSpotIds = mutableStateListOf(...)` inside the Composable and filters `MockData.spots`.
- Bookmark search state, selected tab, action-menu state, and delete-dialog state are all local `remember` state.
- `onSpotClick` is wired from navigation to `MemoryPhotoSelectionDestination(spotId)`, but that destination expects a `memoryId`; bookmark card taps should open spot detail instead.
- The memory menu has inert Edit / Duplicate / Share items; Delete only dismisses UI.
- Completed cards still render a dead detail arrow and fake likes/comments sourced only from mock data.
- The screen still uses `selectedLanguage` plus `_zh` string lookups.

The change #7 memory wizard already provides the Room-backed `MemoryRepository`, the `FilePhoto` Composable, and memoryId-keyed wizard destinations. The content pipeline already provides persisted `Spot` records, but `ContentRepository` currently exposes only `routes()` and `spot(id)`, not all spots as a Flow. User prefs already persist route bookmark IDs; this change adds the equivalent spot bookmark store for the Memories Bookmark tab.

## Goals / Non-Goals

**Goals:**

1. Preserve the PR #80 `Route / Bookmark` tab structure.
2. Route tab renders live Room memories via `MemoriesViewModel`.
3. Bookmark tab renders persisted saved spots via `ContentRepository.spots()` + `UserPreferencesRepository.bookmarkedSpotIds`.
4. Bookmark tab search filters saved spots by locale-resolved title.
5. Bookmark card taps navigate to `SpotDetailDestination(spotId)`.
6. Wire all four memory-card menu actions: Edit, Delete, Duplicate, Share.
7. Draft resume from the in-progress card body.
8. Remove the dangling completed-card detail arrow and dead social stats.
9. Migrate the surface off `selectedLanguage` / `_zh` lookups.
10. Keep legacy `MockData` intact for coexistence, while stopping `MemoriesScreen` from consuming it.

**Non-Goals:**

- A memory detail screen.
- A rendered composite memory image for Share; Share exports the underlying photo files.
- Social engagement counts.
- A new spot-bookmark toggle UI. This change creates the persisted spot-bookmark source consumed by the tab, but adding bookmark/unbookmark controls to spot screens is a separate UX change.
- Batch actions or multi-select on either tab.

## Decisions

### D1. One `MemoriesViewModel` owns both tabs

`MemoriesViewModel` combines:

- `memoryRepo.observeByStatus(IN_PROGRESS)`
- `memoryRepo.observeByStatus(COMPLETED)`
- `contentRepo.spots()`
- `prefsRepo.bookmarkedSpotIds`
- a local MutableStateFlow for UI state (`selectedTab`, `bookmarkSearchQuery`, `activeMenuMemoryId`, `showDeleteDialog`)

This keeps the two PR #80 tabs in one surface-level state object and lets tests assert tab/search/menu/dialog behavior without instrumented Compose tests.

```kotlin
data class MemoriesState(
    val selectedTab: MemoriesTab = MemoriesTab.ROUTE,
    val inProgress: List<MemoryCard> = emptyList(),
    val completed: List<MemoryCard> = emptyList(),
    val bookmarkedSpots: List<BookmarkSpotCard> = emptyList(),
    val bookmarkSearchQuery: String = "",
    val isLoading: Boolean = true,
    val activeMenuMemoryId: String? = null,
    val showDeleteDialog: Boolean = false
)

enum class MemoriesTab { ROUTE, BOOKMARK }
```

The Composable renders the tab selector from `state.selectedTab` and sends `TabSelected(...)` intents. The tab labels use `stringResource(R.string.memories_route_tab)` and `stringResource(R.string.memories_bookmark_tab)`.

### D2. Route-tab memory cards use Room DTOs

`MemoryCard` is derived from the Room domain model:

```kotlin
data class MemoryCard(
    val id: String,
    val title: String,
    val coverRelativePath: String?,
    val status: String,
    val dateLabel: String,
    val draftProgress: DraftProgress
)

data class DraftProgress(val current: Int, val total: Int)
```

`coverRelativePath` is the first photo path or null. The screen renders covers through change #7's `FilePhoto(relativePath, filesDir)`, not `painterResource`. For in-progress rows, `draftProgress` is derived as `1..3 / 3`: draft exists, has at least one photo, has non-blank insights.

### D3. Bookmark-tab spot cards use content DTOs

Add:

```kotlin
fun ContentRepository.spots(): Flow<List<Spot>>
```

`BookmarkSpotCard` is locale- and resource-resolved in the VM:

```kotlin
data class BookmarkSpotCard(
    val id: String,
    val title: String,
    val heroDrawableRes: Int
)
```

The VM filters all content spots to `spot.id in prefsRepo.bookmarkedSpotIds`, preserves content-pipeline source order, maps to `BookmarkSpotCard`, then applies `bookmarkSearchQuery` against `title` case-insensitively. `MemoriesScreen` no longer imports `MockData.spots` or owns a hard-coded `savedSpotIds`.

### D4. Spot bookmark IDs live in DataStore

Extend prefs:

```kotlin
val bookmarkedSpotIds: Flow<Set<String>>
suspend fun setBookmarkedSpotIds(set: Set<String>)
```

`UserPrefsKeys` adds `BOOKMARKED_SPOTS = stringSetPreferencesKey("saved_spot_ids")`. `DataStoreUserPreferencesRepository` returns `emptySet()` when absent. This mirrors `bookmarkedRouteIds` and keeps future spot-save UI from needing a second persistence mechanism.

### D5. Bookmark card taps open spot detail

`MemoriesEffect` includes:

```kotlin
data class NavigateToSpot(val spotId: String) : MemoriesEffect
```

`MyAppNavigation` handles it with `backStack.add(SpotDetailDestination(spotId))`. It must not pass the spot id into `MemoryPhotoSelectionDestination`, because that destination is memoryId-keyed.

### D6. Memory actions reuse wizard and repository boundaries

Edit/resume:

| Source | Destination |
| --- | --- |
| In-progress card body | `MemoryPhotoSelectionDestination(memoryId)` |
| In-progress menu Edit | `MemoryPhotoSelectionDestination(memoryId)` |
| Completed menu Edit | `MemoryEditDestination(memoryId)` |

Delete adds `MemoryRepository.deleteMemory(memoryId)` and deletes any status row plus `filesDir/memories/<memoryId>/`, guarded by the same UUID and path-containment checks change #7 established.

Duplicate adds `MemoryRepository.duplicateMemory(memoryId): Result<String>`, deep-copying photo files into `memories/<newId>/photo_<i>.jpg`, preserving status, and best-effort cleaning the new directory on failure.

Share uses a `FileProvider` and emits `ShareMemory(relativePaths, title)` from the VM. The Composable resolves content URIs and launches `ACTION_SEND` or `ACTION_SEND_MULTIPLE`. Zero-photo memories do not emit a share effect; the menu item is disabled.

### D7. Completed-card cleanup

The completed-card detail arrow is removed because no memory-detail destination exists. The likes/comments row is removed because Room `Memory` has no engagement fields and the app has no backend/account model. Rendering permanent fake counts would be misleading.

### D8. Locale migration follows prior screen rewrites

`MemoriesScreen(selectedLanguage: String, ...)` becomes `MemoriesScreen(viewModel: MemoriesViewModel, ...)`. Chrome text uses `stringResource(R.string.X)` with AppCompat locale. The ViewModel pre-resolves dynamic card titles using the injected `localeProvider()` where source data is localized (`Spot`), and passes already-stored memory titles through unchanged.

Add bilingual string resources for the PR #80 tab/search labels:

- `memories_route_tab`
- `memories_bookmark_tab`
- `memories_search_bookmarks`
- `memories_choose_bookmarks`
- `memories_empty_bookmarks`

## Migration Plan

1. Add `bookmarkedSpotIds` / `setBookmarkedSpotIds` to prefs and DataStore keys.
2. Add `ContentRepository.spots(): Flow<List<Spot>>`.
3. Add `deleteMemory` and `duplicateMemory` to the memory repository.
4. Add FileProvider manifest + `res/xml/file_paths.xml`.
5. Create `ui/memory/library/` DTO/state/intent/effect/VM/factory files.
6. Rewrite `MemoriesScreen` to render Route and Bookmark tabs from VM state.
7. Wire `MyAppNavigation` to build `MemoriesViewModel` and handle wizard/share/spot effects.
8. Add focused repository and VM tests.
9. Verify build, unit tests, source-grep checks, and manual emulator smoke.

## Risks / Trade-offs

- The Bookmark tab can be empty until another UX writes `bookmarkedSpotIds`. This is preferable to keeping hidden hard-coded demo IDs in a persisted app surface.
- Removing likes/comments visibly departs from the mock, but it matches the no-backend architecture.
- Sharing raw photos, not a styled memory composite, may be less polished; it is honest for the current editor scope.
- `ContentRepository.spots()` exposes all spots for the first time. It follows the existing `routes()` shape and emits the loaded snapshot once, so blast radius is small.
