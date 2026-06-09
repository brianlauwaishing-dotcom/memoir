## Purpose

Define the home-screen discovery contract: how routes are searched, tag-filtered, and interest-ranked; how the `HomeViewModel` composes content/interests/query/tags into pre-resolved, locale-aware route cards; and the tag catalog, navigation effects, empty state, and test stack that support it.

## Requirements

### Requirement: TagCatalog is the single source of truth for filter tag ids and labels

A Kotlin object `com.mcis.memoir.ui.home.TagCatalog` SHALL declare the canonical list of filter tag ids and their `@StringRes` labels. `HomeScreen`'s filter chip row, `CultureInterestScreen`'s option list, and the route JSON tag-validation routine MUST consume this list — no other source of tag ids is permitted.

#### Scenario: HomeScreen renders chips from TagCatalog
- **WHEN** `HomeScreen` is composed under any locale
- **THEN** the filter chip row renders one chip per entry in `TagCatalog.all` in the declared order, plus a leading `"all"` chip; each chip's label is `stringResource(tag.labelRes)`

#### Scenario: CultureInterestScreen renders options from TagCatalog
- **WHEN** `CultureInterestScreen` is composed
- **THEN** the option list renders one row per entry in `TagCatalog.all` in the declared order (no `"all"` row, no inline `CultureInterest` data class); each row's label is `stringResource(tag.labelRes)`

#### Scenario: Tag id set is non-empty and free of duplicates
- **WHEN** the test `TagCatalogTest` inspects `TagCatalog.all`
- **THEN** the list is non-empty, AND `TagCatalog.all.map { it.id }.toSet().size == TagCatalog.all.size`

### Requirement: HomeViewModel composes content + interests + query + active tags via Flow combine

`com.mcis.memoir.ui.home.HomeViewModel` SHALL expose `state: StateFlow<HomeState>` and `effects: Flow<HomeEffect>`, and SHALL produce `HomeState.cards` by `combine`ing `ContentRepository.routes()`, `UserPreferencesRepository.selectedInterests`, an internal `queryFlow`, and an internal `activeTagsFlow`. State updates triggered by any of these four sources MUST propagate to a new `state` emission.

#### Scenario: User-interest change re-emits state with new ranking
- **WHEN** the ViewModel is collecting `state` and `prefsRepo.selectedInterests` emits a new set
- **THEN** within the next coroutine tick a new `HomeState` is emitted whose `cards` order reflects the new interest set

#### Scenario: Search query change re-emits filtered state
- **WHEN** the ViewModel receives `HomeIntent.SearchChanged("temple")`
- **THEN** a new `HomeState` is emitted whose `cards` list contains only routes whose locale-resolved title `contains("temple", ignoreCase = true)`

#### Scenario: Active tag toggle re-emits filtered state
- **WHEN** the ViewModel receives `HomeIntent.FilterTagToggled("temples")` with prior `activeTags == emptySet()`
- **THEN** a new `HomeState` is emitted whose `activeTags == setOf("temples")` and whose `cards` contains only routes with `"temples" in route.tags`

### Requirement: HomeViewModel exposes one-shot navigation effects

`HomeViewModel.effects` SHALL be a `Channel`-backed `Flow<HomeEffect>` (never a `StateFlow`) so navigation effects do not replay on Activity recreation.

#### Scenario: Card click emits a NavigateToRoute effect
- **WHEN** the ViewModel receives `HomeIntent.CardClicked("sounds_of_temple")`
- **THEN** the effects Flow emits exactly one `HomeEffect.NavigateToRoute("sounds_of_temple")`, AND `state` is unchanged

#### Scenario: Effect channel does not replay collected items
- **WHEN** a test collects one `HomeEffect.NavigateToRoute(...)`, cancels the collector, and starts a new collector
- **THEN** the new collector does NOT receive the previously emitted effect

### Requirement: Tag filtering rules

`HomeViewModel.buildState` SHALL filter routes by tags as follows:
1. If `activeTags` is empty OR contains the sentinel id `"all"`, no filter is applied — every route survives the tag step.
2. Otherwise, a route survives if `(route.tags.toSet() intersect activeTags).isNotEmpty()`.

#### Scenario: "all" chip selected — every route survives
- **WHEN** `activeTags == setOf("all")` and the route list contains routes with disjoint tag sets
- **THEN** every route appears in `HomeState.cards`

#### Scenario: Multi-chip selection — union match
- **WHEN** `activeTags == setOf("temples", "old_streets")` and route A has `tags = ["temples"]`, route B has `tags = ["old_streets", "crafts"]`, route C has `tags = ["colonial"]`
- **THEN** routes A and B appear in `HomeState.cards`; route C does not

#### Scenario: Empty `activeTags` — no filter
- **WHEN** `activeTags.isEmpty()` and any route list is loaded
- **THEN** every route appears in `HomeState.cards`

### Requirement: Search filtering rules

`HomeViewModel.buildState` SHALL filter routes by search query as follows:
1. If `query.trim()` is empty, no search filter is applied.
2. Otherwise, a route survives if its locale-resolved title contains the trimmed query (case-insensitive).

#### Scenario: Empty query — no filter
- **WHEN** `query` is the empty string or whitespace only
- **THEN** every route surviving the tag step also survives the search step

#### Scenario: Case-insensitive substring match against English title
- **WHEN** active locale is English, query is `"TEMPLE"`, and a route's English title is `"Sounds of Temple Tainan"`
- **THEN** that route survives the search step

#### Scenario: Case-insensitive substring match against Chinese title
- **WHEN** active locale is Chinese, query is `"廟"`, and a route's Chinese title is `"台南廟宇聲音路線"`
- **THEN** that route survives the search step

#### Scenario: No match across locales
- **WHEN** active locale is Chinese, query is `"temple"`, and the route's Chinese title is `"台南廟宇聲音路線"`
- **THEN** that route does NOT survive (cross-locale search is out of scope)

### Requirement: Interest-driven ranking with stable tiebreak

`HomeViewModel.buildState` SHALL rank surviving routes by `(route.tags intersect userInterests).size` descending, with ties broken by the input order (which is `index.json.routes` ASCII-sorted order from `ContentRepository.routes()`). Routes with zero interest matches MUST still appear at the bottom — interests rank, they do not filter.

#### Scenario: User has selected one interest — matching route ranks first
- **WHEN** `userInterests == setOf("temples")`, route A has `tags = ["temples"]`, route B has `tags = ["architecture"]`, and both survive the prior filter steps
- **THEN** route A appears before route B in `HomeState.cards`

#### Scenario: Multiple interest matches outrank single match
- **WHEN** `userInterests == setOf("temples", "old_streets")`, route A has `tags = ["temples", "old_streets"]`, route B has `tags = ["temples"]`, route C has `tags = ["architecture"]`
- **THEN** the order in `HomeState.cards` is A, B, C

#### Scenario: User has no interests selected — natural order preserved
- **WHEN** `userInterests.isEmpty()` and three routes survive the filter steps
- **THEN** `HomeState.cards` is in the same order as `ContentRepository.routes()` emitted them (ASCII-sorted by id)

#### Scenario: Zero-interest-match routes still appear
- **WHEN** `userInterests == setOf("crafts")`, route A has `tags = ["crafts"]`, route B has `tags = ["temples"]`, no chip filter is active
- **THEN** both routes appear in `HomeState.cards`, with A before B

### Requirement: Route cards are pre-resolved per locale by the ViewModel

The ViewModel SHALL resolve each route's `LocalizedText` title / category / description against the current locale exactly once per state emission, and SHALL resolve `heroImage` drawable names via `Resources.getIdentifier(name, "drawable", packageName)` once per state emission. The resulting `RouteCard` data class carries plain `String` and `Int` (drawable res id) values; Composables receive `RouteCard` and do no locale or resource lookup themselves.

#### Scenario: HomeScreen renders RouteCards without performing resource lookups
- **WHEN** `HomeScreen` is composed against a `HomeState` whose `cards` list is populated
- **THEN** the Composable does not invoke `Resources.getIdentifier`, does not invoke `LocalizedText.get`, and does not read `LocalConfiguration.current` for content-text purposes (chrome text via `stringResource` is still permitted)

#### Scenario: An unresolved drawable name yields drawable res id 0
- **WHEN** a route's `heroImage` string does not resolve via `Resources.getIdentifier`
- **THEN** the corresponding `RouteCard.heroDrawableRes` is `0`, AND the `HomeScreen` card renders a neutral placeholder Box

### Requirement: HomeScreen surfaces an empty state for non-matching searches

`HomeScreen` SHALL render a localized "No routes match" message when `state.cards.isEmpty()` AND `state.query.isNotBlank()`. When the cards list is empty for any other reason (initial load, error), the message MUST NOT show.

#### Scenario: Empty state appears only for non-matching search
- **WHEN** `state == HomeState(cards = emptyList(), query = "zzz", isLoading = false, error = null)`
- **THEN** `HomeScreen` renders a `<string name="home_no_results">No routes match "%1$s"</string>` message with `"zzz"` substituted

#### Scenario: Loading state shows a spinner, not the empty-state message
- **WHEN** `state.cards.isEmpty()`, `state.isLoading = true`, `state.query.isBlank()`
- **THEN** `HomeScreen` renders a loading indicator and no empty-state message

### Requirement: HomeScreen and CultureInterestScreen do not consume `selectedLanguage` parameters or `_zh`-suffix string resources

The composables `HomeScreen` and `CultureInterestScreen` SHALL NOT declare a `selectedLanguage: String` parameter. They SHALL NOT call `stringResource(R.string.X_zh)` or use the runtime `getResourceEntryName` + `_zh` suffix trick. All chrome text MUST be accessed via `stringResource(R.string.X)`; the active locale resolves the resource via `res/values-zh/strings.xml` established by `language-toggle`.

#### Scenario: HomeScreen signature does not include selectedLanguage
- **WHEN** the source of `HomeScreen.kt` is inspected after this change lands
- **THEN** the function signature has no `selectedLanguage` parameter, AND `grep -E "(R\.string\.\w+_zh|getResourceEntryName)" HomeScreen.kt` returns zero matches

#### Scenario: CultureInterestScreen signature does not include selectedLanguage
- **WHEN** the source of `CultureInterestScreen.kt` is inspected after this change lands
- **THEN** the function signature has no `selectedLanguage` parameter, AND `grep -E "R\.string\.\w+_zh" CultureInterestScreen.kt` returns zero matches

### Requirement: CultureInterestScreen persists interests on each toggle

`CultureInterestViewModel.toggle(tagId)` SHALL invoke `UserPreferencesRepository.setInterests(...)` with the updated set on each call. `CultureInterestViewModel.skip()` SHALL invoke `setInterests(emptySet())`.

#### Scenario: Toggling on a tag persists the addition immediately
- **WHEN** the screen state is `selected = emptySet()` and the ViewModel receives `toggle("temples")`
- **THEN** `prefs.setInterests(setOf("temples"))` is invoked exactly once

#### Scenario: Toggling off a tag persists the removal immediately
- **WHEN** the screen state is `selected = setOf("temples", "old_streets")` and the ViewModel receives `toggle("temples")`
- **THEN** `prefs.setInterests(setOf("old_streets"))` is invoked exactly once

#### Scenario: Skip clears all interests
- **WHEN** the ViewModel receives `skip()`
- **THEN** `prefs.setInterests(emptySet())` is invoked exactly once

### Requirement: Test stack — JUnit5 + MockK + Turbine — activates from this change

`frontend/mobile/gradle/libs.versions.toml` SHALL declare versioned aliases for `junit-jupiter`, `mockk`, `turbine`, and the `android-junit5` Gradle plugin. `frontend/mobile/app/build.gradle.kts` SHALL apply the `android-junit5` plugin and declare `testImplementation`s for the three libraries. Existing JUnit4 tests authored in change #1 MUST continue to pass via the plugin's vintage-engine bridge.

#### Scenario: Both JUnit4 and JUnit5 tests run in the same task
- **WHEN** `cd frontend/mobile && ./gradlew :app:testDebugUnitTest` is run after this change lands
- **THEN** the task runs `ContentAssetLoaderTest` (JUnit4 from change #1) AND `HomeViewModelTest` (JUnit5 from this change) and reports passes for both
