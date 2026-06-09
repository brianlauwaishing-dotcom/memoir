## Why

`RouteDetailScreen` and `SavedScreen` are the two surfaces in umbrella §2 line "RouteDetail bookmark + SavedScreen renders real bookmarks". Today both screens still read `data.MockData.routes` directly, both still take `selectedLanguage: String` and mix `stringResource(R.string.X_zh)` with hard-coded Kotlin string literals (`"取消收藏"`, `"Saved"`, `"我的旅遊計畫"`, `"My travel plan"`, `"目前還沒有收藏的路徑"`, `"No saved routes yet"` — none of which live in `strings.xml`), and the bookmark state itself lives as a `Set<String>` in `MyAppNavigation` that gets written through to `data.PreferenceManager` via direct field assignment.

`language-toggle` (change #2) already folded `bookmarkedRouteIds: Flow<Set<String>>` + `suspend fun setBookmarkedRouteIds(...)` into `UserPreferencesRepository` so this change has no DataStore plumbing to add. `tainan-route-content-pipeline` (change #1) is the read path. `home-discovery` (change #3) established the MVI template. This change ports both bookmark surfaces onto those rails.

## What Changes

- `RouteDetailScreen` is rewritten:
  - Signature drops `selectedLanguage: String`, `isSaved: Boolean`, `onToggleSave: (String) -> Unit`. New signature: `(viewModel: RouteDetailViewModel, onBackClick: () -> Unit, onNavigateToSaved/Memories: () -> Unit, onSpotClick: (String) -> Unit)`.
  - Reads from `RouteDetailViewModel.state: StateFlow<RouteDetailState>` (cards + `isSaved` + `isLoading` + `error`).
  - Fires `RouteDetailIntent.BookmarkToggled` when the user taps the Save button. Reducer calls `UserPreferencesRepository.setBookmarkedRouteIds(...)` with the toggled set.
  - All chrome text via `stringResource(R.string.X)` against the canonical (non-`_zh`) ids; `values-zh/strings.xml` resolves the Chinese variant per change #2.
  - The fallback / hard-coded literals `"取消收藏"` and `"Saved"` move into `strings.xml` as a new pair `route_detail_save_place_saved` (with the `_zh` mirror entry in `values-zh/` only, since this change uses the canonical-id pattern from the start).
- `RouteDetailViewModel(contentRepo, prefsRepo, resources, localeProvider, routeId)` exposes:
  - `state: StateFlow<RouteDetailState>` built from `combine(contentRepo.route(routeId)-as-flow, prefsRepo.bookmarkedRouteIds) { route, bookmarks -> ... }`.
  - `effects: Flow<RouteDetailEffect>` for one-shot side effects (none in this change; reserved for future error toasts).
  - `RouteDetailIntent { BookmarkToggled, SpotClicked(spotId) }`. `SpotClicked` is reducer-side a no-op (navigation is the Composable's callback responsibility — no need to round-trip through effects for a value the Composable already has).
- `SavedScreen` is rewritten:
  - Signature drops `selectedLanguage: String`, `savedRouteIds: Set<String>`. New signature: `(viewModel: SavedViewModel, onNavigateToHome/Memories: () -> Unit, onMoreClick: (String) -> Unit)`.
  - Reads from `SavedViewModel.state: StateFlow<SavedState>` (cards + `isLoading` + `error`).
  - Empty-state row's hard-coded `"目前還沒有收藏的路徑"` / `"No saved routes yet"` moves to `strings.xml` as `saved_empty_message` + `values-zh/` mirror.
  - The headline `"我的旅遊計畫"` / `"My travel plan"` already has a `strings.xml` entry (`saved_headline` + `saved_headline_zh`) — this change drops the literal usage and switches to `stringResource(R.string.saved_headline)` for `values-zh/` resolution.
- `SavedViewModel(contentRepo, prefsRepo, resources, localeProvider)` exposes:
  - `state: StateFlow<SavedState>` built from `combine(contentRepo.routes(), prefsRepo.bookmarkedRouteIds) { routes, bookmarks -> routes.filter { it.id in bookmarks }.map { it.toCard(...) } }`.
  - No `effects` Flow AND no `Intent` reducer — every click on this screen is a route navigation handled by a Composable callback. Adding `SavedIntent.CardClicked` as a no-op was considered and dropped (YAGNI; introduce when an interaction needs business-rule branching).
- `MyAppNavigation` is rewritten for both destinations:
  - Delete the lifted `var savedRouteIds by remember { mutableStateOf(...) }` state (introduced by change #2 only as a transitional read of `bookmarkedRouteIds` Flow). With both surfaces now reading the Flow directly via their own ViewModels, the MyAppNavigation-level collected state is no longer needed for bookmark.
  - `RouteDetailDestination` entry: construct `RouteDetailViewModel` via a factory passing the `routeId`; remove `isSaved`, `onToggleSave`, `selectedLanguage` parameters from the `RouteDetailScreen` call.
  - `SavedDestination` entry: construct `SavedViewModel` via a factory; remove `savedRouteIds` and `selectedLanguage` parameters.
- New strings:
  - `<string name="route_detail_save_place_saved">Saved</string>` in `values/strings.xml`.
  - `<string name="route_detail_save_place_saved">已收藏</string>` in `values-zh/strings.xml`.
  - `<string name="saved_empty_message">No saved routes yet</string>` in `values/strings.xml`.
  - `<string name="saved_empty_message">目前還沒有收藏的路徑</string>` in `values-zh/strings.xml`.
  - `saved_headline` already exists in `values/strings.xml`; ensure the Chinese mirror `<string name="saved_headline">我的旅遊計畫</string>` exists in `values-zh/strings.xml` (added by change #2's bulk mirror task — verify; if missing, add).
- **Not in scope**:
  - Unsave-from-Saved-screen affordance (long-press / swipe-to-delete). Toggle entry point stays on `RouteDetailScreen` only.
  - Bookmark indicator on `RouteCard` (HomeScreen card list does not show a heart icon). Adding it is a one-line UX change that we defer to whichever change next touches the shared `RouteCard` Composable.
  - `MyAppNavigation` to Nav3 typed routes.
  - Per-route bookmark order / grouping in SavedScreen — flat list in `index.json.routes` ASCII order.
  - Bookmark sync across devices.

## Capabilities

### New Capabilities
- `route-bookmarking`: User bookmark state for routes — the toggle UI on RouteDetailScreen, the listing UI on SavedScreen, the two ViewModels that wrap `UserPreferencesRepository.bookmarkedRouteIds`, and the string-resource hygiene fixes that remove the existing hard-coded Kotlin literals.

### Modified Capabilities
<!-- None — `content-pipeline` and `language-toggle` are read as foundations, not modified. -->

## Impact

- **New files**:
  - `frontend/mobile/app/src/main/java/com/mcis/memoir/ui/route/RouteDetailViewModel.kt` (with `RouteDetailState`, `RouteDetailIntent`, `RouteDetailEffect`, `RouteDetailViewModelFactory`)
  - `frontend/mobile/app/src/main/java/com/mcis/memoir/ui/saved/SavedViewModel.kt` (with `SavedState`, `SavedViewModelFactory` — no `SavedIntent` because the screen has no reducer-worthy actions in this change)
  - `frontend/mobile/app/src/test/java/com/mcis/memoir/ui/route/RouteDetailViewModelTest.kt` (JUnit5 + MockK + Turbine)
  - `frontend/mobile/app/src/test/java/com/mcis/memoir/ui/saved/SavedViewModelTest.kt`
- **Modified files**:
  - `frontend/mobile/app/src/main/java/com/mcis/memoir/RouteDetailScreen.kt` — drop `selectedLanguage` / `isSaved` / `onToggleSave` parameters; drop hard-coded `"取消收藏"` and `"Saved"` literals; drop `MockData.routes.find` read; drop `if (isChinese) … _zh else …` calls; render from `RouteDetailState`; fire `RouteDetailIntent.BookmarkToggled` on save tap.
  - `frontend/mobile/app/src/main/java/com/mcis/memoir/SavedScreen.kt` — drop `selectedLanguage` / `savedRouteIds` parameters; drop hard-coded headline + empty-message literals; drop `MockData.routes.filter` read; render from `SavedState`.
  - `frontend/mobile/app/src/main/java/com/mcis/memoir/MyAppNavigation.kt` — rewrite `RouteDetailDestination` and `SavedDestination` entries; delete the `savedRouteIds` Compose state if it was added by change #2's MyAppNavigation rewrite (it was added as a transitional read; both downstream consumers now own their own VMs, so the lifted state is dead).
  - `frontend/mobile/app/src/main/res/values/strings.xml` — add `route_detail_save_place_saved`, `saved_empty_message`.
  - `frontend/mobile/app/src/main/res/values-zh/strings.xml` — add the two new mirror entries; verify `saved_headline` mirror exists (change #2 was supposed to add it during the bulk mirror task).
- **Deleted code (inside modified files)**:
  - `RouteDetailScreen.kt:206-210` `if (isChinese) { if (isSaved) "取消收藏" else stringResource(R.string.route_detail_save_place_zh) } else { if (isSaved) "Saved" else stringResource(R.string.route_detail_save_place) }` block — replaced by `state.isSaved`-driven `stringResource(if (state.isSaved) R.string.route_detail_save_place_saved else R.string.route_detail_save_place)`.
  - `SavedScreen.kt:66` and `:92` hard-coded literals → `stringResource(R.string.saved_headline)` / `stringResource(R.string.saved_empty_message)`.
- **Dependencies added**: none. JUnit5 + MockK + Turbine + DataStore + serialization are all already on the catalog from prior changes.
- **Risk acknowledgements**:
  - `home-discovery` change #3 currently still threads `selectedLanguage` and `savedRouteIds` through `MyAppNavigation`'s `RouteDetailDestination` / `SavedDestination` entries (those weren't migrated then, deferred to this change). This change MUST not be merged until `home-discovery` is merged, OR the two changes are squashed in one PR. The implementing PR should explicitly call out the merge order.
  - The `isNavigating` debounce at `RouteDetailScreen.kt:62` survives the rewrite — preserve the behavior (a single back-click guard) inside the rewritten Composable.
  - Toggling bookmark on a route id that no longer exists in `ContentRepository` (theoretically impossible since the toggle UI only renders when the route loads) — the reducer still tolerates it; the next `combine` cycle just leaves `state.cards`/`state.route` unchanged.
- **Not changed**:
  - `data.PreferenceManager` — already deleted by change #2.
  - `tainan-route-content-pipeline` / `language-toggle` / `home-discovery` artifacts.
  - Any other screen.
