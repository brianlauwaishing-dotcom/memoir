## ADDED Requirements

### Requirement: RouteDetailViewModel composes route content and bookmark state

`com.mcis.memoir.ui.route.RouteDetailViewModel` SHALL expose `state: StateFlow<RouteDetailState>` produced by `combine`ing a single-emission lookup of `ContentRepository.route(routeId)` with `UserPreferencesRepository.bookmarkedRouteIds`. The state MUST reflect the latest bookmark Flow emission within one coroutine tick of that emission.

#### Scenario: Route exists and is unbookmarked
- **WHEN** a test instantiates the VM with `routeId = "sounds_of_temple"`, the content repo returns a route for that id, and `prefsRepo.bookmarkedRouteIds` emits `emptySet()`
- **THEN** `state.first().isLoading == false`, `state.first().routeId == "sounds_of_temple"`, `state.first().isSaved == false`, AND `state.first().title` equals the locale-resolved route title

#### Scenario: Route exists and is bookmarked
- **WHEN** the content repo returns a route for `"sounds_of_temple"` and `bookmarkedRouteIds` emits `setOf("sounds_of_temple")`
- **THEN** `state.first().isSaved == true`

#### Scenario: Route id not found
- **WHEN** the VM is constructed with a `routeId` for which `ContentRepository.route(...)` returns `null`
- **THEN** `state.first().isLoading == false`, `state.first().error != null`, AND `state.first().routeId == null`

#### Scenario: Bookmark Flow emission flips isSaved
- **WHEN** the VM is collecting state, initial `bookmarks == emptySet()`, AND `bookmarkedRouteIds` then emits `setOf(routeId)`
- **THEN** the next `state` emission has `isSaved == true` and all other fields unchanged

### Requirement: RouteDetailViewModel toggles bookmark via UserPreferencesRepository

On `RouteDetailIntent.BookmarkToggled`, the VM SHALL read the freshest bookmark set via `prefsRepo.bookmarkedRouteIds.first()`, compute the toggled set, and persist it via `prefsRepo.setBookmarkedRouteIds(...)`. The VM MUST NOT directly mutate any in-memory state — the persistence Flow re-emission drives state update.

#### Scenario: Bookmark add
- **WHEN** the VM's `routeId == "sounds_of_temple"`, `bookmarkedRouteIds.first() == emptySet()`, and `onIntent(BookmarkToggled)` is invoked
- **THEN** `prefsRepo.setBookmarkedRouteIds(setOf("sounds_of_temple"))` is invoked exactly once

#### Scenario: Bookmark remove
- **WHEN** the VM's `routeId == "sounds_of_temple"`, `bookmarkedRouteIds.first() == setOf("sounds_of_temple", "sea_protection")`, and `onIntent(BookmarkToggled)` is invoked
- **THEN** `prefsRepo.setBookmarkedRouteIds(setOf("sea_protection"))` is invoked exactly once

#### Scenario: Concurrent toggles serialize via mutex, no lost update
- **WHEN** the test fires `BookmarkToggled` twice in rapid succession against an initially-unbookmarked route, with both coroutines suspended on `prefsRepo.bookmarkedRouteIds.first()` before either reaches `setBookmarkedRouteIds`, and lets both run to completion under `runTest`'s virtual time
- **THEN** `setBookmarkedRouteIds` is invoked exactly twice in sequential order (not in parallel), AND the final persisted set is `emptySet()` — toggle-on then toggle-off cleanly, with no lost-update where both invocations would have read `emptySet()` and both written `setOf(routeId)`. This MUST be enforced by a `Mutex` inside the ViewModel's `BookmarkToggled` handler

#### Scenario: Odd number of rapid toggles ends in bookmarked state
- **WHEN** the test fires `BookmarkToggled` three times in rapid succession against an initially-unbookmarked route, with mutex serialization in effect
- **THEN** `setBookmarkedRouteIds` is invoked exactly three times, AND the final persisted set is `setOf(routeId)`

### Requirement: RouteDetailViewModel pre-resolves locale-dependent fields

The VM SHALL resolve `LocalizedText` fields (title, description, journey labels) and drawable names against the injected `localeProvider()` once per state emission. The resulting `RouteDetailState` MUST carry plain `String` and `Int` (drawable res id) values so the Composable performs no locale or resource lookup itself.

#### Scenario: State carries pre-resolved title under English locale
- **WHEN** the VM is constructed with `localeProvider = { Locale.ENGLISH }` against a route whose JSON title is `{"en": "Sounds of Temple Tainan", "zh": "台南廟宇聲音路線"}`
- **THEN** `state.first().title == "Sounds of Temple Tainan"`

#### Scenario: State carries pre-resolved title under Chinese locale
- **WHEN** the VM is constructed with `localeProvider = { Locale("zh") }` against the same route
- **THEN** `state.first().title == "台南廟宇聲音路線"`

#### Scenario: Spot labels in journey are resolved
- **WHEN** the VM is constructed for a route whose journey contains `{order: 1, spotId: "grand_mazu"}`, and `ContentRepository.spot("grand_mazu")` returns a spot with locale-resolved title `"Grand Mazu Temple"`
- **THEN** `state.first().journey[0].label == "Grand Mazu Temple"`

### Requirement: SavedViewModel derives cards from bookmarked subset of routes

`com.mcis.memoir.ui.saved.SavedViewModel` SHALL expose `state: StateFlow<SavedState>` produced by `combine`ing `ContentRepository.routes()` with `UserPreferencesRepository.bookmarkedRouteIds`. `state.cards` MUST be the route list filtered to `it.id in bookmarks` and pre-resolved to `RouteCard` DTOs.

#### Scenario: No bookmarks → empty cards
- **WHEN** the VM is collecting state, `routes()` emits a 5-route list, and `bookmarkedRouteIds.first() == emptySet()`
- **THEN** `state.first().cards.isEmpty() == true`, `state.first().isLoading == false`

#### Scenario: Two bookmarks → two cards in route-list order
- **WHEN** `routes()` emits `[A, B, C, D, E]` (ASCII id order) and `bookmarkedRouteIds.first() == setOf("E", "B")`
- **THEN** `state.first().cards.map { it.id } == ["B", "E"]` (filter preserves source order; ASCII tiebreak from `index.json.routes`)

#### Scenario: Bookmark Flow re-emission updates cards
- **WHEN** state has been collected with `bookmarks == setOf("B")` and `bookmarkedRouteIds` then emits `setOf("B", "C")`
- **THEN** the next `state` emission has `cards.map { it.id } == ["B", "C"]`

### Requirement: RouteDetailScreen renders from state without legacy parameters

`RouteDetailScreen` Composable SHALL NOT declare `selectedLanguage: String`, `isSaved: Boolean`, or `onToggleSave: (String) -> Unit` parameters. The screen renders chrome text via `stringResource(R.string.X)` only; the bookmark button label uses `stringResource(R.string.route_detail_save_place_saved)` when `state.isSaved == true` and `stringResource(R.string.route_detail_save_place)` otherwise.

#### Scenario: Screen signature is migrated
- **WHEN** the source of `RouteDetailScreen.kt` is inspected after this change lands
- **THEN** the function signature has no `selectedLanguage` / `isSaved` / `onToggleSave` parameters, AND `grep -nE 'R\.string\.\w+_zh' RouteDetailScreen.kt` returns zero matches, AND the file contains no Kotlin string literals matching `"取消收藏"` or `"Saved"`

#### Scenario: Save-button label flips with bookmark state
- **WHEN** a Compose test renders `RouteDetailScreen` with a state whose `isSaved` flips from `false` to `true` between two snapshots
- **THEN** the bookmark button label transitions from `"Save Place"` (under en) to `"Saved"`; or from `"收藏地點"` (under zh, value mirrored in `values-zh/`) to `"已收藏"`

#### Scenario: Back-click debounce prevents double-fire
- **WHEN** a Compose test simulates two rapid clicks on the back button within a single recomposition window
- **THEN** the `onBackClick` callback is invoked exactly once

### Requirement: SavedScreen renders from state without legacy parameters

`SavedScreen` Composable SHALL NOT declare `selectedLanguage: String` or `savedRouteIds: Set<String>` parameters. The screen renders chrome text via `stringResource(R.string.saved_headline)` for the headline and `stringResource(R.string.saved_empty_message)` for the empty state — no hard-coded Kotlin literals.

#### Scenario: Screen signature is migrated
- **WHEN** the source of `SavedScreen.kt` is inspected after this change lands
- **THEN** the function signature has no `selectedLanguage` / `savedRouteIds` parameters, AND the file contains no Kotlin string literals matching `"我的旅遊計畫"`, `"My travel plan"`, `"目前還沒有收藏的路徑"`, or `"No saved routes yet"`

#### Scenario: Empty state shows the localized message
- **WHEN** the Compose test renders `SavedScreen` with `state.cards.isEmpty() && state.isLoading == false`
- **THEN** the screen renders `stringResource(R.string.saved_empty_message)` — `"No saved routes yet"` under en, `"目前還沒有收藏的路徑"` under zh

#### Scenario: Populated state renders cards
- **WHEN** the Compose test renders `SavedScreen` with `state.cards` containing two cards
- **THEN** two `RouteCardComposable` items render in the order present in `state.cards`

### Requirement: Bookmark state lives in DataStore, not in MyAppNavigation

`MyAppNavigation` SHALL NOT hold lifted bookmark Compose state (e.g. `var savedRouteIds by remember { mutableStateOf(...) }`). The single source of truth for bookmark membership is `UserPreferencesRepository.bookmarkedRouteIds`; the two consumer screens own their own VMs that read it directly.

#### Scenario: MyAppNavigation has no bookmark-state remember
- **WHEN** the source of `MyAppNavigation.kt` is inspected after this change lands
- **THEN** the file contains no `mutableStateOf` block reading from `prefsRepo.bookmarkedRouteIds` or storing `Set<String>` of route ids, AND the `RouteDetailDestination` entry block does not pass an `isSaved` or `savedRouteIds` parameter

### Requirement: New string resources for bookmark and saved-empty UX

The strings `route_detail_save_place_saved` and `saved_empty_message` SHALL exist in both `res/values/strings.xml` and `res/values-zh/strings.xml`. The `saved_headline` string SHALL have a `values-zh/` mirror entry.

#### Scenario: All new strings have bilingual coverage
- **WHEN** the repo is inspected after this change lands
- **THEN** `grep -l "route_detail_save_place_saved" res/values/strings.xml res/values-zh/strings.xml` returns both files, AND `grep -l "saved_empty_message" res/values/strings.xml res/values-zh/strings.xml` returns both files, AND `grep -l "saved_headline" res/values-zh/strings.xml` returns the file

#### Scenario: No regression of pre-existing strings
- **WHEN** the repo is inspected after this change lands
- **THEN** `route_detail_save_place`, `route_detail_save_place_zh`, `saved_headline`, `saved_headline_zh` all still exist in `res/values/strings.xml` (no deletions from this change)
