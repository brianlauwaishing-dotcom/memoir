## Context

This change implements umbrella §6 (MVI shape for first non-trivial ViewModel) + §2's "home-discovery" line item (search, filter, interest-driven ranking). It consumes:
- `tainan-route-content-pipeline` change #1 (`ContentRepository.routes()`, `Route.tags`, `LocalizedText`).
- `language-toggle` change #2 (`UserPreferencesRepository.selectedInterests`, `LocaleController.currentLocale()`, `values-zh/strings.xml`).

**Current state**:
- `HomeScreen.kt:76` reads `MockData.routes` — hard-coded list of 5 routes.
- `HomeScreen.kt:100-136` filters in-Composable by category-chip + search.
- `HomeScreen.kt:104-122` matches category by string-equality of `route.categoryEn` (English literal) vs `context.getString(categoryResId)` — meaning the filter only works under the en locale and silently drops everything under zh.
- Of the 7 chips (`all + 6 interest ids`), only `temples` and `architecture` produce any results against current Mock data (which has `categoryEn ∈ {"Temples & Folk Beliefs", "Historic Architecture"}`); `old_streets`, `trade`, `colonial`, `crafts` yield empty result sets. This is an existing latent bug.
- `HomeScreen.kt:129-132` does prefix-match on per-word splits including Han-boundary splits.
- `HomeScreen.kt:189-200` does runtime resource-name-string suffixing to find `_zh` variants — fragile and slow.
- `CultureInterestScreen.kt:66-73` declares the same 6 interest ids as HomeScreen's chips, but in a separate inline list. Drift risk if either gets edited without the other.
- `MyAppNavigation` threads `selectedLanguage: String` and `initialInterests: Set<String>` into both screens.

**Constraints**:
- Cannot wait on Koin (DI change is deferred); ViewModels read repositories from `MemoirApplication` companion fields established by changes #1 and #2.
- The umbrella's MVI pattern (§6) is the binding template; this change is the first non-trivial application of it, so the shape choices here become precedent for `route-bookmarking`, `artifact-discovery-flow`, `memory-creation-flow`, etc.
- Tags live as opaque string ids in route JSON per umbrella §4.2 (`"tags": ["temples", "folk-belief"]`). The umbrella explicitly defers `tags.json` (display labels, colors) to a follow-up; this change is that follow-up — labels and rendering live in code (`TagCatalog` + `strings.xml`).

## Goals / Non-Goals

**Goals:**
1. Replace `MockData` reads with `ContentRepository` reads on `HomeScreen`.
2. Make all 6 culture interest chips actually filter when matching routes exist; chips that match nothing yield an empty card list (correct behavior, not silent fallback).
3. Make `CultureInterestScreen`'s saved interests rank routes on `HomeScreen` (umbrella's "affects ranking").
4. Establish the umbrella's MVI template as concretely implemented code that subsequent changes can copy.
5. Migrate `HomeScreen` and `CultureInterestScreen` off `selectedLanguage: String` plumbing.
6. Activate MockK + Turbine + JUnit5 as the test stack from this change forward (language-toggle D8 promised this turn).
7. Eliminate `HomeScreen.kt:189-200` `getIdentifier(name + "_zh")` brittleness.

**Non-Goals:**
- Reworking `MyAppNavigation` to Nav3 typed routes (deferred to a navigation-rewrite change).
- Bottom-nav refactor.
- Fuzzy / TF-IDF search.
- Pagination (route count is ~5; YAGNI).
- Locale-aware sort (ASCII tiebreak is fine for ~5 items).
- `tags.json` — the implementation chose code-resident `TagCatalog` instead.
- Removing `_zh`-suffix strings from `strings.xml`. They are still consumed by 14 other screens; per-screen cleanup as those screens get touched.

## Decisions

### D1. `TagCatalog` is a code object, not a JSON file

```kotlin
object TagCatalog {
    val all: List<Tag> = listOf(
        Tag(id = "temples",      labelRes = R.string.culture_temples),
        Tag(id = "old_streets",  labelRes = R.string.culture_old_streets),
        Tag(id = "architecture", labelRes = R.string.culture_architecture),
        Tag(id = "trade",        labelRes = R.string.culture_trade),
        Tag(id = "colonial",     labelRes = R.string.culture_colonial),
        Tag(id = "crafts",       labelRes = R.string.culture_crafts)
    )
    val ids: Set<String> = all.map { it.id }.toSet()
    fun byId(id: String): Tag? = all.firstOrNull { it.id == id }
}

data class Tag(val id: String, @StringRes val labelRes: Int)
```

`HomeScreen` chip row and `CultureInterestScreen` option list both consume `TagCatalog.all`. The `"all"` sentinel chip is rendered explicitly by `HomeScreen` as a UI affordance and is NOT a member of `TagCatalog` (it represents "no filter", not a tag).

**Why over alternatives:**
- *JSON `tags.json`*: would need a generator pass, a Kotlin loader, and an `R.string.X` resolution layer; net negative when the list is 6 entries and ~static.
- *Sealed class*: more ceremony; reflection-style id mapping awkward.
- *Database*: dramatic over-engineering for static UI metadata.

**Trade-off:** adding a tag requires recompile + content-pipeline regeneration (route JSON must list valid tag ids). Acceptable because tags shift on the timescale of months, not days.

### D2. Tag-id schema in route JSON

Route JSON gains:
```json
{
  "id": "sounds_of_temple",
  ...
  "tags": ["temples", "folk-belief"]
}
```

**Tag id rules:**
- Each tag id is a kebab-case ASCII string.
- The generator validates every `tags[]` entry against `TagCatalog.ids` — unknown ids fail CI with a precise error.
- Every route MUST declare at least one tag from `TagCatalog.ids`; the generator and `ContentValidationTest` enforce this so a route can never disappear from results under a non-`all` chip selection.
- The `route.category` field (umbrella §4.2 schema) is retained as `LocalizedText` for display; it is no longer used for filtering.

**Why a flat list, not a category-vs-tags split:**
- The current `route.categoryEn` mostly overlaps with one of the chip labels.
- Two-level (category + tags) adds schema complexity for no demonstrated UI benefit at this scale.

### D3. `HomeViewModel` implements the umbrella's `combine` template verbatim

```kotlin
sealed interface HomeIntent {
    data class SearchChanged(val q: String) : HomeIntent
    data class FilterTagToggled(val tagId: String) : HomeIntent
    data class CardClicked(val routeId: String) : HomeIntent
}

sealed interface HomeEffect {
    data class NavigateToRoute(val routeId: String) : HomeEffect
}

class HomeViewModel(
    private val contentRepo: ContentRepository,
    private val prefsRepo: UserPreferencesRepository,
    private val resources: Resources,          // for Locale-resolved title + drawable lookup
    private val localeProvider: () -> Locale    // injected so tests don't need a Compose context
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState(isLoading = true))
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val _effects = Channel<HomeEffect>(Channel.BUFFERED)
    val effects: Flow<HomeEffect> = _effects.receiveAsFlow()

    private val queryFlow = MutableStateFlow("")
    private val activeTagsFlow = MutableStateFlow<Set<String>>(emptySet())

    init {
        viewModelScope.launch {
            combine(
                contentRepo.routes(),
                prefsRepo.selectedInterests,
                queryFlow,
                activeTagsFlow
            ) { routes, interests, query, tags ->
                buildState(routes, interests, query, tags)
            }
            .catch { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
            .collect { newState -> _state.value = newState }
        }
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.SearchChanged    -> queryFlow.value = intent.q
            is HomeIntent.FilterTagToggled -> activeTagsFlow.update { it.toggle(intent.tagId) }
            is HomeIntent.CardClicked      -> emitEffect(HomeEffect.NavigateToRoute(intent.routeId))
        }
    }

    private fun emitEffect(e: HomeEffect) { viewModelScope.launch { _effects.send(e) } }

    private fun Set<String>.toggle(id: String): Set<String> =
        if (id in this) this - id else this + id
}
```

**Note on `contentRepo.routes()` Flow lifecycle in `combine`**: per change #1's spec, `routes()` emits exactly once then completes. `kotlinx.coroutines.flow.combine` retains the last-emitted value from each source even after that source completes, and continues to re-emit the combined value when any other source changes. So `combine(routes, interests, query, tags)` stays open and keeps producing new states from interest / query / tag changes long after `routes()` has completed — no special handling needed.

**Why `Refresh` and `ShowSnackbar` are absent vs umbrella §6**: the umbrella template lists them as part of the canonical shape. They are dropped here under YAGNI: `Refresh` against an emit-once-and-complete `routes()` is a no-op, and `ShowSnackbar` has no consumer until a top-level `SnackbarHostState` is wired (deferred to the navigation rewrite). Future screens add them when first needed.

**Why `BookmarkToggled` is absent vs umbrella §6**: bookmark toggle UI lives on `RouteDetailScreen`, not `HomeScreen`. The umbrella's listing of `BookmarkToggled` under `HomeIntent` (§6.1) is an example of the pattern, not a contract this change must honor. `route-bookmarking` (change #4) owns that intent on the appropriate screen.

**Why over alternatives:**
- *Reading `LocalConfiguration` inside the ViewModel*: not testable. Injected `locale: () -> Locale` is.
- *Hot-source `selectedInterests` re-collection on tag toggle*: handled correctly by `combine`'s upstream change propagation.
- *Lift query / activeTags into the public State*: kept inside ViewModel as the source-of-truth via dedicated `MutableStateFlow`s; UI reads them through the composed `HomeState`. Avoids state desync.

**`HomeIntent.Refresh` as a no-op vs removed**: kept as a member because the umbrella's template lists it; in practice it's never fired by UI in this screen. Documented so the next change doesn't add a refresh button thinking it does something.

### D4. Ranking is a stable sort by interest-intersection count

```kotlin
private fun buildState(
    routes: List<Route>,
    interests: Set<String>,
    query: String,
    activeTags: Set<String>
): HomeState {
    val filtered = routes
        .filter { matchesTags(it, activeTags) && matchesSearch(it, query) }
        .sortedWith(rankComparator(interests))
    return HomeState(
        cards = filtered.map { it.toCard(locale(), resources) },
        isLoading = false,
        query = query,
        activeTags = activeTags,
        userInterests = interests,
        error = null
    )
}

private fun matchesTags(route: Route, activeTags: Set<String>): Boolean {
    if (activeTags.isEmpty() || "all" in activeTags) return true
    return (route.tags.toSet() intersect activeTags).isNotEmpty()
}

private fun matchesSearch(route: Route, query: String): Boolean {
    val q = query.trim()
    if (q.isEmpty()) return true
    return route.title[locale()].contains(q, ignoreCase = true)
}

private fun rankComparator(interests: Set<String>): Comparator<Route> {
    if (interests.isEmpty()) return Comparator { _, _ -> 0 }   // stable: preserve input order
    return compareByDescending { route -> (route.tags.toSet() intersect interests).size }
}
```

`List.sortedWith` is documented stable; ties preserve the input order — which is `index.json.routes` ASCII order coming out of `ContentRepository.routes()` (locked by `content-pipeline` spec).

**Why over alternatives:**
- *Weighted score (tag importance)*: no signal source; YAGNI.
- *Multiplicative search × interest score*: more complex; harder to debug.
- *Promotion at index 0 only*: loses the "many matches > few matches" gradient.

### D5. `RouteCard` is a ViewModel-resolved DTO

```kotlin
data class RouteCard(
    val id: String,
    val title: String,
    val category: String,
    val heroDrawableRes: Int,    // 0 if name unresolved; UI shows placeholder
    val description: String
)

// internal (not private) so route-bookmarking change #4 can reuse it from ui.saved
internal fun Route.toCard(locale: Locale, resources: Resources): RouteCard = RouteCard(
    id = id,
    title = title[locale],
    category = category[locale],
    heroDrawableRes = resources.getIdentifier(heroImage, "drawable", /* pkg */ "com.mcis.memoir"),
    description = description[locale]
)
```

`heroDrawableRes = 0` is the documented `getIdentifier` miss return; the card UI renders a neutral placeholder Box in that case. The package literal `"com.mcis.memoir"` matches `build.gradle.kts:7` namespace; an alternative is `resources.getResourcePackageName(R.string.app_name)`, but the literal is fine and explicit.

**Why resolve in ViewModel, not in Compose:**
- Locale resolution belongs to state preparation. Composables that take String in / String out are dumber and test more cleanly with `createComposeRule()`.
- One resolution per state-emission cycle, not once per recomposition.

### D6. `CultureInterestScreen` gets a tiny ViewModel too

```kotlin
class CultureInterestViewModel(private val prefs: UserPreferencesRepository) : ViewModel() {
    val selected: StateFlow<Set<String>> = prefs.selectedInterests
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun toggle(tagId: String) {
        viewModelScope.launch {
            val current = selected.value
            val next = if (tagId in current) current - tagId else current + tagId
            prefs.setInterests(next)
        }
    }

    fun skip() {
        viewModelScope.launch { prefs.setInterests(emptySet()) }
    }
}
```

No `Effect` channel — navigation callbacks (Start Exploring / Skip) are wired by `MyAppNavigation` directly. The ViewModel exists only to remove `selectedLanguage` and `initialInterests` plumbing and to persist on every toggle (so the Start Exploring button doesn't need to bulk-write).

**Trade-off**: `CultureInterestScreen` and `LanguageSelectionScreen` use different MVI flavors (full triad vs StateFlow-only). Both are documented; future screen authors pick the shape that matches their interaction surface.

### D7. Drop the existing per-word + Han-boundary search semantics

The existing `HomeScreen.kt:129-132` splits the title on whitespace + Han character boundaries, then checks `word.startsWith(query)`. This is over-engineered: it tries to be a "starts with any morpheme" matcher. Replace with a single `title.contains(query, ignoreCase = true)`.

**Why:**
- Han-boundary splitting was added to handle "search `廟` matches `大天后宮`" — which `contains` handles too.
- Fewer cases to mentally model; matches the umbrella's `query` spec without ambiguity.
- The lost behavior — "prefix-only" matching — was inconsistently applied (Han boundary splitting made it effectively contains anyway) and never documented as a UX requirement.

### D8. JUnit5 + MockK + Turbine activate here

Per language-toggle's D8 deferral note, this is the change where the test stack upgrade earns its keep. `HomeViewModelTest` exercises multi-source `combine` timing (interests Flow emits while a query is being typed), which Turbine's `awaitItem()` models cleanly. MockK relaxes the verbosity of "stub one Flow, capture one suspend call".

```kotlin
// libs.versions.toml additions
[versions]
junitJupiter = "5.10.2"
mockk = "1.13.10"
turbine = "1.1.0"
androidJunit5 = "1.10.0.0"

[libraries]
junit-jupiter = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junitJupiter" }
mockk = { module = "io.mockk:mockk", version.ref = "mockk" }
turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }

[plugins]
android-junit5 = { id = "de.mannodermaus.android-junit5", version.ref = "androidJunit5" }
```

(Exact versions verified at implementation time via Maven Central; numbers above are illustrative.)

`app/build.gradle.kts` applies the plugin and adds `testImplementation`s for the three libraries. Existing JUnit4 tests (`ContentAssetLoaderTest`, etc.) continue to run unmodified via `android-junit5`'s vintage-engine include.

### D9. `MyAppNavigation` wires the ViewModels but stays as-is for Nav3

`MyAppNavigation` is not migrated to Nav3 typed routes in this change (deferred). It still uses the existing `data object HomeDestination` / `data class CultureInterestDestination` keys. The `HomeDestination` entry block changes from:

```kotlin
is HomeDestination -> NavEntry(key) {
    HomeScreen(
        selectedLanguage = selectedLanguage,
        initialInterests = userInterests,
        onNavigateToHome = { … },
        onMoreClick = { routeId -> backStack.add(RouteDetailDestination(routeId)) }
    )
}
```

to:

```kotlin
is HomeDestination -> NavEntry(key) {
    val vm: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(
            content = MemoirApplication.content,
            prefs   = MemoirApplication.prefs,
            resources = LocalContext.current.resources,
            localeProvider = { LocaleController.currentLocale() }
        )
    )
    HomeScreen(
        viewModel = vm,
        onNavigateToSaved = { backStack.add(SavedDestination) },
        onNavigateToMemories = { backStack.add(MemoriesDestination) }
    )
    LaunchedEffect(vm) {
        vm.effects.collect { e ->
            when (e) {
                is HomeEffect.NavigateToRoute -> backStack.add(RouteDetailDestination(e.routeId))
                is HomeEffect.ShowSnackbar    -> Unit // no snackbar host wired in this change
            }
        }
    }
}
```

`HomeViewModelFactory` is a thin `ViewModelProvider.Factory` — the simplest hand-rolled DI for ViewModels with non-default constructor parameters. The Koin change replaces both the factory and the `MemoirApplication.X` lookups.

`onNavigateToHome` is removed from `HomeScreen` (you can't navigate-to-home from Home). The home-nav button still works via the `BottomNavigationBar` callback the screen owns.

### D10. Drop-target removal log

The following code is removed in this change. Each is named so the reviewer can grep:
- `data class CategoryItem` in `HomeScreen.kt:55`
- `data class CultureInterest` in `CultureInterestScreen.kt:40`
- `val categoryList = remember { listOf(...) }` block in `HomeScreen.kt:87-97`
- The `selectedCategoryIds` initial-state logic at `HomeScreen.kt:82-84` (replaced by ViewModel `activeTagsFlow`)
- The category-matching block at `HomeScreen.kt:104-122`
- The search per-word/Han-boundary block at `HomeScreen.kt:129-132`
- The `getResourceEntryName` + `_zh` suffix lookup at `HomeScreen.kt:189-200`
- All `if (isChinese) X_zh else X` calls inside HomeScreen and CultureInterestScreen (replaced by direct `stringResource(R.string.X)`)

## Risks / Trade-offs

- **Content-pipeline regeneration drift**: this change requires the route JSON `tags` field to be populated. Without coordinating with the content-pipeline owner, the CI sync check will fail. Mitigation: the implementing PR includes both the generator update AND the regenerated JSON in the same commit.
- **Empty results from chip selection**: with current Mock data only 2 chips meaningfully match. Once real designer-tagged content arrives all 6 work. Document the empty card list as expected UX, not a bug.
- **MockK + JVM-only**: cannot mock Android framework types (`Resources`, `Context`). Tests inject `Resources` and `locale: () -> Locale` so MockK only stubs Kotlin / interface types — well within its sweet spot. Robolectric remains out of scope.
- **JUnit4 ↔ JUnit5 dual stack**: the `android-junit5` plugin handles this, but adding both runners doubles classpath surface. Acceptable; the alternative is rewriting change #1's tests, which is out of scope.
- **`HomeViewModelFactory` vs Koin**: factory adds a small code smell that gets paid down by the Koin change. The umbrella migration staging anticipates this; no risk beyond Koin-change scope.
- **`MyAppNavigation` Flow collection inside `NavEntry`**: `LaunchedEffect(vm) { vm.effects.collect { … } }` works when the entry recomposes; when the NavDisplay back-stack pops Home, the LaunchedEffect cancels and the channel drains — correct behavior. Verified against Compose's Effect lifecycle docs.

## Migration Plan

1. Update `tainan-route-content-pipeline` generator + side input (or CSV) to emit `tags: [...]` per route. Run `python data/scripts/generate_content.py`; commit regenerated `data/tainan-route/routes/*.json` and the `tags`-aware `_assets.json` (or `_tags.json`) — same PR.
2. Add the spec.md MODIFIED Requirement to `content-pipeline` via this change's `specs/content-pipeline/spec.md` delta (see specs/ folder).
3. Add MockK + Turbine + JUnit5 + plugin to catalog; apply plugin; add `testImplementation`s.
4. Create `TagCatalog.kt`.
5. Add `Route.tags: List<String>` to the existing model class — note this requires updating `content-pipeline` spec.md (MODIFIED Requirement, see specs/).
6. Implement `HomeViewModel` + `HomeState` + `HomeIntent` + `HomeEffect` + `RouteCard` + `HomeViewModelFactory`.
7. Implement `CultureInterestViewModel` + factory.
8. Rewrite `HomeScreen` Composable signature and body.
9. Rewrite `CultureInterestScreen` Composable signature and body.
10. Rewrite `MyAppNavigation`'s `HomeDestination` and `CultureInterestDestination` entries.
11. Write `HomeViewModelTest` (Turbine + MockK + JUnit5), `RankingTest` (pure JUnit5), `TagCatalogTest` (pure JUnit5).
12. Run `:app:testDebugUnitTest`; assert both JUnit4 and JUnit5 results land.
13. Emulator smoke: select interests on CultureInterest → Start Exploring → Home shows matching routes first. Toggle Chinese in LanguageSelection → relaunch → Home renders Chinese titles and chip labels.

**Rollback**: revert the change commit. `HomeScreen` returns to its `MockData`-backed form. The added route-JSON `tags` field is benign (ignored by old code via `kotlinx-serialization`'s `ignoreUnknownKeys = true` already set in change #1's `ContentJson` config).

## Open Questions

- **CSV column vs `_tags.json` side input**: the implementer picks based on whether the designer naturally edits tags in spreadsheet rows (column wins) or in a separate text file (side-input wins). The decision affects only the generator; nothing downstream changes.
- **Should the `"all"` sentinel chip be `Tag(id="all", labelRes=R.string.home_category_all)` in TagCatalog for symmetry?** No — `"all"` is a UI affordance ("no filter"), not a tag a route could carry. Keeping it out of `TagCatalog.ids` makes the route-tag validation rule unambiguous (`route.tags ⊆ TagCatalog.ids` would fail if "all" leaked in).
- **Snackbar host**: `HomeEffect.ShowSnackbar` is defined but not consumed. Wiring a snackbar requires a host at the `MyAppNavigation` root level. Defer to the navigation rewrite change; this change keeps the effect type so the umbrella's MVI template is honored.
- **Search across both locales simultaneously?** A user typing English on a Chinese phone gets zero matches against a Chinese title. Worth considering but adds confusing UX (English query "temple" matching a Chinese title visually conveys nothing). Defer.
