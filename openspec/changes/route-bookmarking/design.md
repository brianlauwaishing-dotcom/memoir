## Context

Two-screen change implementing umbrella §2 "RouteDetail bookmark + SavedScreen renders real bookmarks". Consumes:
- `tainan-route-content-pipeline` change #1: `ContentRepository.route(id): Route?` and `ContentRepository.routes(): Flow<List<Route>>`.
- `language-toggle` change #2: `UserPreferencesRepository.bookmarkedRouteIds: Flow<Set<String>>` + `setBookmarkedRouteIds(...)`; `LocaleController.currentLocale()`; `values-zh/strings.xml`.
- `home-discovery` change #3: MVI template (State / Intent / Effect, `Channel`-backed effects, hand-rolled VM factory), `RouteCard` rendering DTO under `ui.home`.

**Current state**:
- `RouteDetailScreen.kt:59` reads `MockData.routes.find { it.id == routeId }` directly.
- `RouteDetailScreen.kt:206-210` chooses between four label variants by hand:
  ```kotlin
  if (isChinese) {
      if (isSaved) "取消收藏" else stringResource(R.string.route_detail_save_place_zh)
  } else {
      if (isSaved) "Saved" else stringResource(R.string.route_detail_save_place)
  }
  ```
  Two of the four variants are hard-coded Kotlin literals never declared in `strings.xml` — wrong i18n hygiene.
- `RouteDetailScreen.kt:62` defines `var isNavigating by remember { mutableStateOf(false) }` as a back-click debounce. The current code never resets it on success — single-shot guard, OK.
- `SavedScreen.kt:47-49` reads `MockData.routes.filter { it.id in savedRouteIds }` directly.
- `SavedScreen.kt:66` and `:92` use hard-coded literal strings for the headline and the empty state (`saved_headline` IS defined in `strings.xml` but the screen ignores it).
- `MyAppNavigation.kt:30` reads `savedRouteIds` from `preferenceManager` (this code is what change #2 rewrites; after #2 lands, `savedRouteIds` is `prefsRepo.bookmarkedRouteIds.collectAsStateWithLifecycle(initialValue = emptySet())`). Either way, MyAppNavigation owns the bookmark Set at the navigator level and threads it into both screens.
- `home-discovery` change #3's tasks do NOT migrate `RouteDetailDestination` or `SavedDestination` entries — they explicitly leave `selectedLanguage = selectedLanguage` and `savedRouteIds = savedRouteIds` for this change.

**Constraints**:
- Cannot precede `home-discovery` (would create an MVI shape contradiction at code-review time). PR order: change #4 lands AFTER change #3.
- `UserPreferencesRepository` API is fixed by change #2; this change adds zero methods.
- Test stack JUnit5 + MockK + Turbine activates from `home-discovery`; this change uses the same stack.

## Goals / Non-Goals

**Goals:**
1. Move bookmark state ownership out of `MyAppNavigation` and into per-screen ViewModels backed by `UserPreferencesRepository.bookmarkedRouteIds`.
2. Replace `MockData` reads with `ContentRepository` reads on both screens.
3. Eliminate hard-coded Kotlin string literals on both screens; everything goes through `stringResource(R.string.X)` with corresponding `values-zh/` mirrors.
4. Migrate both screens off the `selectedLanguage: String` parameter + `_zh`-suffix lookup pattern.
5. Keep the back-click debounce behavior on RouteDetail intact (preserve single-tap guard).
6. Keep PR diff scoped to two screens + two new VMs + four new string entries + the MyAppNavigation entries — no other surface touched.

**Non-Goals:**
- Unsave from SavedScreen (long-press / swipe). Toggle entry is RouteDetail only.
- Bookmark indicator on the home / shared `RouteCard` (deferred).
- Bookmark order or grouping in SavedScreen.
- Bookmark-driven analytics events.
- Bookmark sync to a backend (umbrella §1.3 non-goal).
- Nav3 typed-route rewrite.

## Decisions

### D1. `RouteDetailViewModel` takes `routeId` as a constructor parameter

```kotlin
class RouteDetailViewModel(
    private val routeId: String,
    private val contentRepo: ContentRepository,
    private val prefsRepo: UserPreferencesRepository,
    private val resources: Resources,
    private val localeProvider: () -> Locale
) : ViewModel() {

    private val _state = MutableStateFlow(RouteDetailState(isLoading = true))
    val state: StateFlow<RouteDetailState> = _state.asStateFlow()

    private val _effects = Channel<RouteDetailEffect>(Channel.BUFFERED)
    val effects: Flow<RouteDetailEffect> = _effects.receiveAsFlow()

    // Serializes BookmarkToggled invocations. DataStore.edit { } itself is serialized,
    // but the read-modify-write at this layer (first() → setBookmarkedRouteIds) is NOT
    // atomic without this mutex; without it, two rapid toggles could read the same
    // initial value and produce a lost-update.
    private val toggleMutex = Mutex()

    init {
        viewModelScope.launch {
            combine(
                flow { emit(contentRepo.route(routeId)) },     // suspend lookup → emit-once Flow
                prefsRepo.bookmarkedRouteIds
            ) { route, bookmarks ->
                buildState(route, bookmarks)
            }
            .catch { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
            .collect { _state.value = it }
        }
    }

    fun onIntent(intent: RouteDetailIntent) {
        when (intent) {
            RouteDetailIntent.BookmarkToggled -> viewModelScope.launch {
                toggleMutex.withLock {
                    val current = prefsRepo.bookmarkedRouteIds.first()
                    val next = if (routeId in current) current - routeId else current + routeId
                    prefsRepo.setBookmarkedRouteIds(next)
                }
            }
            is RouteDetailIntent.SpotClicked -> Unit  // navigation handled in Composable callback
        }
    }

    // suspend because resolveSpotLabel calls a suspend ContentRepository function.
    // `combine`'s transform lambda is already suspend, so this is legal there.
    private suspend fun buildState(route: Route?, bookmarks: Set<String>): RouteDetailState {
        if (route == null) return RouteDetailState(isLoading = false, error = "route_not_found")
        val locale = localeProvider()
        return RouteDetailState(
            isLoading = false,
            routeId = route.id,
            title = route.title[locale],
            description = route.description[locale],
            heroDrawableRes = resources.getIdentifier(route.heroImage, "drawable", "com.mcis.memoir"),
            journey = route.journey.map { stop ->
                JourneyRowState(order = stop.order, spotId = stop.spotId, label = resolveSpotLabel(stop.spotId, locale))
            },
            isSaved = routeId in bookmarks,
            error = null
        )
    }

    private suspend fun resolveSpotLabel(spotId: String, locale: Locale): String =
        contentRepo.spot(spotId)?.title?.get(locale) ?: spotId
}
```

**`Mutex` scope**: per-`RouteDetailViewModel` instance, which is per-routeId thanks to `viewModel(key = key.routeId)`. Toggles on different routes via different VMs are not serialized at this layer, but DataStore's own write serialization handles that — and two RouteDetail screens are never on screen at once (Nav3 single back-stack).

**Why `flow { emit(contentRepo.route(routeId)) }` instead of `routes().map { ... find ... }`:**
- `route(routeId)` is a `suspend fun` that hits the `ContentSnapshot` map directly — O(1) instead of O(n) over the full routes list.
- Wrapping in `flow { emit(...) }` makes it composable with `combine`.

**Why read `bookmarks.first()` inside `BookmarkToggled` instead of the combined state:**
- Same staleness concern as `CultureInterestViewModel` in change #3: `state.value` could be a stale snapshot if the bookmarks Flow had a pending emission. `.first()` reads the freshest available value before computing the new set.

**Why `SpotClicked` is a no-op reducer:**
- The Composable already has `spotId` in hand from the timeline row; routing it through the VM via an effect would mean the VM holds navigation responsibility, which it doesn't need. Including the intent in the sealed interface ONLY documents the user action surface; the reducer is intentionally empty.

### D2. `SavedViewModel` is even smaller — pure derivation

```kotlin
class SavedViewModel(
    private val contentRepo: ContentRepository,
    private val prefsRepo: UserPreferencesRepository,
    private val resources: Resources,
    private val localeProvider: () -> Locale
) : ViewModel() {

    private val _state = MutableStateFlow(SavedState(isLoading = true))
    val state: StateFlow<SavedState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                contentRepo.routes(),
                prefsRepo.bookmarkedRouteIds
            ) { routes, bookmarks ->
                val savedCards = routes.filter { it.id in bookmarks }.map { it.toCard(localeProvider(), resources) }
                SavedState(isLoading = false, cards = savedCards, error = null)
            }
            .catch { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
            .collect { _state.value = it }
        }
    }
}
```

**No `Intent` reducer and no Effects channel** — earlier draft proposed `SavedIntent.CardClicked` as a no-op for MVI shape consistency, but per YAGNI the type adds public surface that is literally never invoked. Card clicks route through a Composable callback (`onMoreClick: (String) -> Unit`) directly to the navigator. Justification: SavedScreen has no business-rule decisions on click — every card click is a route navigation, period. HomeScreen uses Effects because future search-result analytics or "no results, try X" prompts plausibly add effect-driven branches; SavedScreen has no comparable plausible extension. If swipe-to-unsave or similar interactive features arrive, `SavedIntent` gets added then.

### D3. `RouteDetailState` shape

```kotlin
data class RouteDetailState(
    val isLoading: Boolean = false,
    val routeId: String? = null,
    val title: String = "",
    val description: String = "",
    val heroDrawableRes: Int = 0,
    val journey: List<JourneyRowState> = emptyList(),
    val isSaved: Boolean = false,
    val error: String? = null
)

data class JourneyRowState(
    val order: Int,
    val spotId: String,
    val label: String
)
```

**Why eagerly resolve spot labels in the VM:**
- The existing UI shows a label per journey row (`route.journeyItems[i].labelEn/Zh`). Under the new content-pipeline schema, journey carries only `spotId`; the label must be resolved via `contentRepo.spot(spotId).title[locale]`.
- Doing this per row at Composable time would mean 3+ Compose-scope suspend calls. Resolving once in `buildState` keeps the Composable dumb.
- Trade-off: `buildState` is now `suspend` because it calls `resolveSpotLabel`. `combine` already operates inside `viewModelScope.launch`, so this is fine — but the `combine` block must `coroutineScope { ... }` if doing parallel resolution. For ≤5 spots, sequential is faithful enough.

### D4. `RouteDetailIntent` and `RouteDetailEffect`

```kotlin
sealed interface RouteDetailIntent {
    data object BookmarkToggled : RouteDetailIntent
    data class SpotClicked(val spotId: String) : RouteDetailIntent
}

sealed interface RouteDetailEffect {
    data class ShowError(val msg: String) : RouteDetailEffect
}
```

`ShowError` is defined for future use (no consumer yet — same caveat as the dropped HomeScreen `ShowSnackbar`). Difference vs HomeScreen: error display IS plausible here (the route-not-found case), but in this change we render the error inline in the screen via `state.error` rather than emitting an effect. Keep the effect type for the future case where the toggle persistence fails and the screen wants to show a snackbar; cost is one declared type.

**Decision**: keep `RouteDetailEffect.ShowError` despite no consumer in this change. The YAGNI argument from home-discovery was about `ShowSnackbar` — a generic effect with no plausible imminent consumer. `ShowError` here has an imminent consumer (the failure case of `setBookmarkedRouteIds`) that we don't ship a snackbar host for, so the type exists but the failure case currently calls `Log.w` instead. Re-evaluate when the navigation rewrite adds a snackbar host.

### D5. SavedScreen empty-state vs loading-state precedence

```
state.isLoading == true  → CircularProgressIndicator (no empty message)
state.isLoading == false && state.cards.isEmpty()  → empty-state message
state.isLoading == false && state.cards.isNotEmpty()  → cards list
state.error != null  → error chrome (precedence: error > loading > empty > cards)
```

This matches the home-discovery convention. Empty-state message is `stringResource(R.string.saved_empty_message)` — the existing hard-coded literal moves into resources.

### D6. RouteDetailScreen back-click debounce preservation

Preserve the existing `isNavigating` guard:

```kotlin
@Composable
fun RouteDetailScreen(
    viewModel: RouteDetailViewModel,
    onBackClick: () -> Unit,
    onNavigateToSaved: () -> Unit,
    onNavigateToMemories: () -> Unit,
    onSpotClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var isNavigating by remember { mutableStateOf(false) }

    // ... back-button uses guard ...
    .clickable {
        if (!isNavigating) {
            isNavigating = true
            onBackClick()
        }
    }
}
```

The guard lives at the Composable level — not the ViewModel — because it's a UI-tactile concern about double-tap, not a state-machine concern.

### D7. String hygiene additions

Two new string keys land in this change. The naming convention matches existing screens:

`res/values/strings.xml`:
```xml
<string name="route_detail_save_place_saved">Saved</string>
<string name="saved_empty_message">No saved routes yet</string>
```

`res/values-zh/strings.xml`:
```xml
<string name="route_detail_save_place_saved">已收藏</string>
<string name="saved_empty_message">目前還沒有收藏的路徑</string>
```

Reason: the existing `route_detail_save_place` covers the "Save Place" prompt; the new key covers the post-tap state. The bilingual mirror moves into `values-zh/` (the canonical chrome-text channel established by change #2), NOT a `_zh`-suffix sibling — this change starts fresh with the new pattern instead of perpetuating the legacy one.

Verify that `saved_headline` has a `values-zh/` mirror. Change #2's tasks 5.1 instructs the implementer to mirror all non-`_zh`-suffixed strings; this should already be there. If somehow missing, this change adds it.

### D8. ViewModel factory pattern

Two more factories alongside the home-discovery ones. Identical structure to `HomeViewModelFactory`:

```kotlin
class RouteDetailViewModelFactory(
    private val routeId: String,
    private val content: ContentRepository,
    private val prefs: UserPreferencesRepository,
    private val resources: Resources,
    private val localeProvider: () -> Locale
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return RouteDetailViewModel(routeId, content, prefs, resources, localeProvider) as T
    }
}

class SavedViewModelFactory(...) : ViewModelProvider.Factory { ... }
```

The `key = ...` form of Nav3 `entryProvider` exposes the `routeId` from the destination type; the factory closes over it.

The Koin migration will replace all four factories (Home, CultureInterest, RouteDetail, Saved) in one sweep.

### D9. MyAppNavigation rewrite — surgical and orthogonal to home-discovery's

Both entries land in this change:

```kotlin
is RouteDetailDestination -> NavEntry(key) {
    val ctx = LocalContext.current
    val currentLocale = LocaleController.currentLocale()
    val vm: RouteDetailViewModel = viewModel(
        key = key.routeId,                                     // distinct VM per routeId
        factory = RouteDetailViewModelFactory(
            routeId = key.routeId,
            content = MemoirApplication.content,
            prefs = MemoirApplication.prefs,
            resources = ctx.resources,
            localeProvider = { currentLocale }
        )
    )
    RouteDetailScreen(
        viewModel = vm,
        onBackClick = { backStack.removeLastOrNull() },
        onNavigateToSaved = { backStack.add(SavedDestination) },
        onNavigateToMemories = { backStack.add(MemoriesDestination) },
        onSpotClick = { spotId -> backStack.add(SpotIntroDestination(spotId)) }
    )
}

is SavedDestination -> NavEntry(key) {
    val ctx = LocalContext.current
    val currentLocale = LocaleController.currentLocale()
    val vm: SavedViewModel = viewModel(
        factory = SavedViewModelFactory(
            content = MemoirApplication.content,
            prefs = MemoirApplication.prefs,
            resources = ctx.resources,
            localeProvider = { currentLocale }
        )
    )
    SavedScreen(
        viewModel = vm,
        onNavigateToHome = { backStack.clear(); backStack.add(HomeDestination) },
        onNavigateToMemories = { backStack.add(MemoriesDestination) },
        onMoreClick = { routeId -> backStack.add(RouteDetailDestination(routeId)) }
    )
}
```

**Why `viewModel(key = key.routeId, ...)` for RouteDetail:**
- Without `key`, Compose's `viewModel { }` reuses the same VM instance across different route ids on the back stack. Each route detail needs its own VM with its own `routeId` constructor arg.

**Delete `var savedRouteIds by remember { mutableStateOf(...) }`:** the lifted state introduced by change #2 (which migrated the SharedPreferences read to a DataStore Flow read) was useful only while the consumers (RouteDetail + Saved) still took it as a parameter. With both consumers now reading the Flow directly via their own VMs, the navigator-level collected state has zero readers and is removed.

## Risks / Trade-offs

- **Merge order with `home-discovery`**: this change rewrites the same `MyAppNavigation` entries that change #3 left in legacy shape. PR-order: this MUST land AFTER #3 OR the two squash into one PR. The implementing PR's description calls this out.
- **`viewModel(key = key.routeId)` cross-route reuse**: passing the routeId as the key ensures uniqueness, but it also means re-navigating to the SAME route reuses the same VM (so `init` doesn't re-run, and the user sees the same data). Correct behavior — `combine` will already reflect any bookmark Flow changes.
- **`buildState` is `suspend` due to per-row spot label resolution**: the `combine` block remains correct because `combine`'s transform lambda is `suspend`. No special handling required. If perf shows up as an issue (it won't at ~5 spots per route), parallelize with `awaitAll`.
- **`RouteDetailIntent.SpotClicked` as a no-op reducer is a code smell**: an intent that does nothing in the reducer is signal that the navigator could call the Composable callback directly without going through `onIntent`. Trade-off: keeping it preserves the umbrella's "every UI action is an intent" template; dropping it saves four lines. Decision: keep it, the consistency is worth more than the brevity.
- **`isNavigating` debounce in Composable scope**: survives configuration change because `remember` is not `rememberSaveable`. On rotation the guard resets — acceptable (back-click after rotation is a legitimate fresh action).
- **`SavedViewModel` re-emits on every bookmark toggle**: each toggle on RouteDetail causes SavedViewModel's `combine` to recompute and re-emit. For ~5 routes this is cheap; for a future with hundreds it would warrant Flow-side `distinctUntilChanged`. Defer.

## Migration Plan

1. Add string entries to `values/strings.xml` and `values-zh/strings.xml`.
2. Verify `saved_headline` mirror in `values-zh/` (should exist from change #2; add if absent).
3. Implement `RouteDetailState`, `RouteDetailIntent`, `RouteDetailEffect`, `RouteDetailViewModel`, `RouteDetailViewModelFactory`, `JourneyRowState`.
4. Implement `SavedState`, `SavedViewModel`, `SavedViewModelFactory` (no `SavedIntent` — see D2).
5. Rewrite `RouteDetailScreen` Composable: drop `selectedLanguage` / `isSaved` / `onToggleSave` parameters; render from state; fire `BookmarkToggled` on save tap; preserve `isNavigating` back-click guard.
6. Rewrite `SavedScreen` Composable: drop `selectedLanguage` / `savedRouteIds` parameters; render from state; use `stringResource(R.string.saved_headline)` and `stringResource(R.string.saved_empty_message)`.
7. Rewrite `MyAppNavigation`'s `RouteDetailDestination` and `SavedDestination` entries; delete the `savedRouteIds` lifted state.
8. Write `RouteDetailViewModelTest`: covers the `route(id)`-found path, `route(id)`-null path, bookmark toggle from-empty-to-singleton, toggle from-singleton-to-empty, `SpotClicked` is a no-op state-wise.
9. Write `SavedViewModelTest`: covers empty bookmarks → empty cards, populated bookmarks → cards filtered correctly, bookmark Flow change → new emission.
10. Run `:app:testDebugUnitTest` — all green.
11. Emulator smoke: from Home tap a route → RouteDetail loads → tap Save (button label changes from "Save Place" to "Saved" / "已收藏") → back to Home → bottom-nav Saved → that route appears in the list. Toggle locale to Chinese; relaunch; same flow renders Chinese chrome text.

**Rollback**: revert the change commit. Both screens regress to their `MockData`-backed forms; bookmark state regresses to MyAppNavigation-level lifted state (per change #2's transitional shape). No data loss (DataStore keeps the bookmark set).

## Open Questions

- **Should toggling bookmark show a snackbar confirmation?** UX would benefit ("Added to Saved" / "Removed from Saved"). Adding it requires wiring `RouteDetailEffect.ShowSnackbar` through to a host. Deferred — open for the navigation-rewrite change to enable.
- **`SavedScreen` order — bookmark-time descending or route id ASCII?** Currently the `combine` outputs `index.json.routes` ASCII order. Bookmark-time ordering would require persisting the timestamp per bookmark (set → list-of-pairs). Defer.
- **Per-route bookmark visibility on RouteCard in home / saved list?** Adding a heart icon on the shared `RouteCard` is a one-line UX change; the data is in `prefsRepo.bookmarkedRouteIds`. Defer; cost-benefit unclear without UX input.
- **Bookmark cap?** No limit today. Designer feedback would set it; defer.
