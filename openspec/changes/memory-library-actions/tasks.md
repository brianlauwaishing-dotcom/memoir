## 1. Persisted saved spots

- [x] 1.1 In `UserPrefsKeys.kt`, add `BOOKMARKED_SPOTS = stringSetPreferencesKey("saved_spot_ids")`
- [x] 1.2 In `UserPreferencesRepository.kt`, add `val bookmarkedSpotIds: Flow<Set<String>>` and `suspend fun setBookmarkedSpotIds(set: Set<String>)`
- [x] 1.3 In `DataStoreUserPreferencesRepository.kt`, implement `bookmarkedSpotIds` with `emptySet()` fallback and `setBookmarkedSpotIds(...)` mirroring the existing route bookmark implementation
- [x] 1.4 Keep `bookmarkedRouteIds` behavior unchanged

## 2. Content repository spots stream

- [x] 2.1 In `ContentRepository.kt`, add `fun spots(): Flow<List<Spot>> = flow { emit(snapshot.await().spots.values.toList()) }`
- [x] 2.2 Preserve `route(id)`, `routes()`, and `spot(id)` behavior unchanged

## 3. Repository methods for memory actions

- [x] 3.1 In `data/memory/MemoryRepository.kt`, add `suspend fun deleteMemory(memoryId: String)` and `suspend fun duplicateMemory(memoryId: String): Result<String>`
- [x] 3.2 In `RoomMemoryRepository.kt`, implement `deleteMemory` with the existing UUID guard and `startsWith(File(filesDir, "memories"))` containment guard; delete the row and `filesDir/memories/<memoryId>/` regardless of status
- [x] 3.3 In `RoomMemoryRepository.kt`, implement `duplicateMemory` inside `runCatching`: validate UUID, read source row, create a fresh UUID, copy each source photo to `memories/<newId>/photo_<i>.jpg`, upsert a copied row with new id/timestamps/photo paths, preserve status, return new id
- [x] 3.4 On duplicate failure after `newId` allocation, best-effort delete `filesDir/memories/<newId>/` before returning `Result.failure`

## 4. FileProvider for sharing app-private photos

- [x] 4.1 Create `res/xml/file_paths.xml` with a `<files-path name="memories" path="memories/" />`
- [x] 4.2 In `AndroidManifest.xml`, add an `androidx.core.content.FileProvider` under `<application>` with authority `${applicationId}.fileprovider`, `exported=false`, `grantUriPermissions=true`, and metadata pointing at `@xml/file_paths`
- [x] 4.3 Confirm no new dependency is needed because `androidx.core` is already available

## 5. Library MVI scaffolding (`ui/memory/library/`)

- [x] 5.1 Create `MemoryCard.kt`: `MemoryCard(id, title, coverRelativePath, status, dateLabel, draftProgress)` and `DraftProgress(current, total)`
- [x] 5.2 Create `BookmarkSpotCard.kt`: `BookmarkSpotCard(id, title, heroDrawableRes)`
- [x] 5.3 Create `MemoriesState.kt` with `selectedTab`, `inProgress`, `completed`, `bookmarkedSpots`, `bookmarkSearchQuery`, `isLoading`, `activeMenuMemoryId`, `showDeleteDialog`
- [x] 5.4 Create `MemoriesIntent.kt` covering `TabSelected`, `BookmarkSearchChanged`, `BookmarkSpotClicked`, `MoreClicked`, `MenuDismissed`, `ContinueEditingClicked`, `EditClicked`, `DeleteClicked`, `DeleteConfirmed`, `DeleteCancelled`, `DuplicateClicked`, `ShareClicked`, `CreateMemoryClicked`
- [x] 5.5 Create `MemoriesEffect.kt` covering `NavigateToWizard(memoryId, WizardEntry)`, `NavigateToCreate`, `NavigateToSpot(spotId)`, and `ShareMemory(relativePaths, title)`
- [x] 5.6 Create `MemoriesViewModelFactory.kt` closing over `MemoryRepository`, `ContentRepository`, `UserPreferencesRepository`, `Resources`, and `localeProvider`

## 6. MemoriesViewModel

- [x] 6.1 Combine `repo.observeByStatus(IN_PROGRESS)`, `repo.observeByStatus(COMPLETED)`, `contentRepo.spots()`, `prefsRepo.bookmarkedSpotIds`, and UI state into one `StateFlow<MemoriesState>`
- [x] 6.2 Map Room memories to `MemoryCard`: first photo as cover, `updatedAt` formatted into `dateLabel`, and derived `DraftProgress(1..3, 3)`
- [x] 6.3 Map content spots to `BookmarkSpotCard`: filter to bookmarked IDs, preserve content source order, resolve title by `localeProvider()`, resolve `heroImage` through `Resources.getIdentifier`
- [x] 6.4 Apply `bookmarkSearchQuery` after DTO mapping; filter by title contains query, case-insensitive
- [x] 6.5 Keep a latest-memory map for Share to read `photoRelativePaths` without a second repo read
- [x] 6.6 Implement tab/search/bookmark intents: `TabSelected`, `BookmarkSearchChanged`, `BookmarkSpotClicked -> NavigateToSpot(spotId)`
- [x] 6.7 Implement memory navigation intents: in-progress body/Edit -> `PHOTO_SELECTION`; completed Edit -> `EDIT`; Create -> `NavigateToCreate`
- [x] 6.8 Implement delete dialog intents and call `repo.deleteMemory(id)` exactly once on confirm
- [x] 6.9 Implement duplicate and share intents; disable/no-op Share when the selected memory has zero photos

## 7. Rewrite MemoriesScreen

- [x] 7.1 Change signature to `MemoriesScreen(viewModel: MemoriesViewModel, onNavigateToHome, onNavigateToSaved, onNavigateToMemories, onCreateMemoryClick, onNavigateToWizard, onNavigateToSpot, modifier)`; remove `selectedLanguage`
- [x] 7.2 Collect state with `collectAsStateWithLifecycle()`
- [x] 7.3 Preserve the PR #80 tab selector visually; drive selected tab from `state.selectedTab`
- [x] 7.4 Replace all `_zh` runtime branches with `stringResource(R.string.X)` and AppCompat locale
- [x] 7.5 Route tab renders `state.inProgress` / `state.completed`; the create-memory CTA remains visible
- [x] 7.6 In-progress cards render `MemoryCard`, use `FilePhoto`, derive progress from `card.draftProgress`, and dispatch `ContinueEditingClicked(card.id)` from the card body
- [x] 7.7 Completed cards render `MemoryCard`, use `FilePhoto`, remove the bottom-end detail arrow, and remove the likes/comments row
- [x] 7.8 Bookmark tab renders search from `state.bookmarkSearchQuery`, dispatches `BookmarkSearchChanged`, and renders `state.bookmarkedSpots` through a `BookmarkSpotCard` Composable
- [x] 7.9 Bookmark card clicks dispatch `BookmarkSpotClicked(spot.id)`; no bookmark click should call `MemoryPhotoSelectionDestination`
- [x] 7.10 Wire all four menu handlers to intents and disable Share for zero-photo memories
- [x] 7.11 Show `DeleteConfirmationDialog` from `state.showDeleteDialog`
- [x] 7.12 Collect effects in `LaunchedEffect`: wizard/create/spot callbacks plus FileProvider share chooser construction
- [x] 7.13 Remove `MockData`, `MemoryData`, old `data.MemoryStatus`, and hard-coded `savedSpotIds` imports/usages from `MemoriesScreen.kt`

## 8. Navigation wiring

- [x] 8.1 In `MyAppNavigation.kt`, the `Memories` entry constructs `MemoriesViewModel` via factory using `MemoirApplication.memoryRepo`, `MemoirApplication.content`, `MemoirApplication.prefs`, resources, and `LocaleController.currentLocale()`
- [x] 8.2 Handle `NavigateToWizard` by adding `MemoryPhotoSelectionDestination(memoryId)` or `MemoryEditDestination(memoryId)`
- [x] 8.3 Keep `NavigateToCreate` / `onCreateMemoryClick` pointing at `MemoryTemplateDestination`
- [x] 8.4 Handle `NavigateToSpot(spotId)` by adding `SpotDetailDestination(spotId)`
- [x] 8.5 Confirm `MemoriesScreen` is no longer constructed with `selectedLanguage`

## 9. Strings

- [x] 9.1 Add bilingual string entries for `memories_route_tab`, `memories_bookmark_tab`, `memories_search_bookmarks`, `memories_choose_bookmarks`, and `memories_empty_bookmarks`
- [x] 9.2 Keep existing `memories_*`, `cancel_button`, and `delete_button` strings intact

## 10. Tests

- [x] 10.1 `MemoryRepositoryActionsTest`: delete removes completed row + dir; delete removes in-progress row + dir; invalid id is a guarded no-op
- [x] 10.2 `MemoryRepositoryActionsTest`: duplicate copies two photo files byte-identically, preserves status, assigns new UUID and new photo paths
- [x] 10.3 `MemoriesViewModelTest`: state separates in-progress/completed memories and maps cover/date/progress
- [x] 10.4 `MemoriesViewModelTest`: bookmarked spots are filtered by `bookmarkedSpotIds`, locale-resolved, source-order preserved, and filtered by search query
- [x] 10.5 `MemoriesViewModelTest`: tab selection and bookmark search update state without effects
- [x] 10.6 `MemoriesViewModelTest`: bookmark card click emits `NavigateToSpot(spotId)`
- [x] 10.7 `MemoriesViewModelTest`: Continue/Edit/Delete/Duplicate/Share intents emit or call the expected collaborators
- [x] 10.8 `DataStoreUserPreferencesRepositoryTest` or existing prefs test: `bookmarkedSpotIds` persists and defaults to empty

## 11. Verification

- [x] 11.1 `cd frontend/mobile && ./gradlew :app:assembleDebug`
- [x] 11.2 `cd frontend/mobile && ./gradlew :app:testDebugUnitTest`
- [x] 11.3 Source checks: no `MockData.memories`, `MockData.spots`, `savedSpotIds`, `R.string.*_zh`, `clickable { /* Detail */ }`, `memory.likes`, or `memory.comments` remain in `MemoriesScreen.kt`
- [x] 11.4 Source checks: `MyAppNavigation.kt` routes bookmark spot clicks to `SpotDetailDestination`, not `MemoryPhotoSelectionDestination`
- [ ] 11.5 Emulator smoke: Route tab shows created memories; resume/edit/duplicate/delete/share work; Bookmark tab search filters saved spots; bookmark card tap opens spot detail
