## 1. Content-pipeline tag emission (must land first in this change's diff)

- [x] 1.1 Pick the generator variant — either add a `tags` column to `data/tainan_routes.csv` OR create `data/tainan-route/_tags.json` (keyed by route id) — and document the choice in `data/scripts/README.md`
- [x] 1.2 Update `data/scripts/generate_content.py` to: read the chosen tag source; emit `"tags": ["...", "..."]` in each `routes/<id>.json` after the existing fields; fail non-zero with a precise message if any route ends up with an empty tag list or any tag id outside `TagCatalog.knownIds` (the generator hard-codes the known ids list mirroring `TagCatalog.kt:7-15`, with a comment pointing to `TagCatalog.kt` so the two stay in sync — manual cross-edit obligation captured in step 1.6)
- [x] 1.3 Add `tags` fixture cases under `data/scripts/test/fixtures/`: missing column / missing entry → exit non-zero; empty value → exit non-zero; unknown tag → exit non-zero; valid `temples|folk-belief` → expected JSON
- [x] 1.4 Run `python data/scripts/generate_content.py`; confirm exits 0 against current CSV/_tags.json; commit regenerated `data/tainan-route/routes/*.json`
- [x] 1.5 Re-run the generator; confirm `git diff --exit-code data/tainan-route/` reports zero changes (determinism guard)
- [x] 1.6 Cross-link: add comment block at top of `TagCatalog.kt` "// Keep tag id list in sync with `data/scripts/generate_content.py` — both must agree on the set used to validate route JSON" and the mirror comment in `generate_content.py`

## 2. Gradle catalog + test stack activation

- [x] 2.1 In `frontend/mobile/gradle/libs.versions.toml` `[versions]` add: `junitJupiter`, `mockk`, `turbine`, `androidJunit5` (look up latest stable on Maven Central / Context7 at implementation time; this change does NOT pin specific numbers)
- [x] 2.2 In `[libraries]` add: `junit-jupiter` → `org.junit.jupiter:junit-jupiter`, `mockk` → `io.mockk:mockk`, `turbine` → `app.cash.turbine:turbine`
- [x] 2.3 In `[plugins]` add: `android-junit5 = { id = "de.mannodermaus.android-junit5", version.ref = "androidJunit5" }`
- [x] 2.4 In `frontend/mobile/app/build.gradle.kts` add `alias(libs.plugins.android.junit5)` to `plugins { }`
- [x] 2.5 In `dependencies { }` add `testImplementation(libs.junit.jupiter)`, `testImplementation(libs.mockk)`, `testImplementation(libs.turbine)`. Existing `testImplementation(libs.junit)` (JUnit4) STAYS — the `android-junit5` plugin's vintage-engine include runs both
- [x] 2.6 Run `cd frontend/mobile && ./gradlew :app:testDebugUnitTest` and confirm change #1's JUnit4 tests still pass (the vintage bridge works) before writing any new JUnit5 test

## 3. TagCatalog

- [x] 3.1 Create `frontend/mobile/app/src/main/java/com/mcis/memoir/ui/home/Tag.kt`: `data class Tag(val id: String, @StringRes val labelRes: Int)`
- [x] 3.2 Create `frontend/mobile/app/src/main/java/com/mcis/memoir/ui/home/TagCatalog.kt` (separate file): `object TagCatalog { val all = listOf(Tag("temples", R.string.culture_temples), Tag("old_streets", R.string.culture_old_streets), Tag("architecture", R.string.culture_architecture), Tag("trade", R.string.culture_trade), Tag("colonial", R.string.culture_colonial), Tag("crafts", R.string.culture_crafts)); val ids = all.map { it.id }.toSet(); fun byId(id: String) = all.firstOrNull { it.id == id } }`
- [x] 3.3 Create `frontend/mobile/app/src/test/java/com/mcis/memoir/ui/home/TagCatalogTest.kt` (JUnit5): assert `TagCatalog.all.isNotEmpty()`, assert `TagCatalog.ids.size == TagCatalog.all.size` (no duplicate ids), assert every `labelRes` resolves at compile time (just referencing it is enough — won't compile if missing)

## 4. HomeViewModel + supporting types

- [x] 4.1 Create `frontend/mobile/app/src/main/java/com/mcis/memoir/ui/home/HomeState.kt`: `data class HomeState(val cards: List<RouteCard> = emptyList(), val isLoading: Boolean = false, val query: String = "", val activeTags: Set<String> = emptySet(), val userInterests: Set<String> = emptySet(), val error: String? = null)`
- [x] 4.2 Create `ui/home/HomeIntent.kt`:
  ```kotlin
  sealed interface HomeIntent {
      data class SearchChanged(val q: String) : HomeIntent
      data class FilterTagToggled(val tagId: String) : HomeIntent
      data class CardClicked(val routeId: String) : HomeIntent
  }
  ```
  Each member MUST repeat the `: HomeIntent` supertype — Kotlin sealed-interface members inside the braces do not auto-inherit. `Refresh` is intentionally absent (no-op against change #1's emit-once `routes()` Flow)
- [x] 4.3 Create `ui/home/HomeEffect.kt`:
  ```kotlin
  sealed interface HomeEffect {
      data class NavigateToRoute(val routeId: String) : HomeEffect
  }
  ```
  `ShowSnackbar` is intentionally absent (no snackbar host wired in this change)
- [x] 4.4 Create `ui/home/RouteCard.kt`: `data class RouteCard(val id: String, val title: String, val category: String, val heroDrawableRes: Int, val description: String)`. In the same file, declare `internal fun Route.toCard(locale: Locale, resources: Resources): RouteCard = RouteCard(id = id, title = title[locale], category = category[locale], heroDrawableRes = resources.getIdentifier(heroImage, "drawable", "com.mcis.memoir"), description = description[locale])` — visibility MUST be `internal` (not `private`) so the route-bookmarking change #4 can call it from `ui.saved`
- [x] 4.5 Create `ui/home/HomeViewModel.kt` with constructor `(private val contentRepo: ContentRepository, private val prefsRepo: UserPreferencesRepository, private val resources: Resources, private val localeProvider: () -> Locale) : ViewModel()`
- [x] 4.6 Add internal `_state: MutableStateFlow<HomeState>` (initial `HomeState(isLoading = true)`), `_effects: Channel<HomeEffect>(Channel.BUFFERED)`, `queryFlow: MutableStateFlow<String>("")`, `activeTagsFlow: MutableStateFlow<Set<String>>(emptySet())`
- [x] 4.7 In `init { }`, `viewModelScope.launch { combine(contentRepo.routes(), prefsRepo.selectedInterests, queryFlow, activeTagsFlow) { r, i, q, t -> buildState(r, i, q, t) }.catch { e -> _state.update { it.copy(isLoading = false, error = e.message) } }.collect { _state.value = it } }`
- [x] 4.8 Implement `private fun buildState(routes: List<Route>, interests: Set<String>, query: String, activeTags: Set<String>): HomeState` with the filter + rank algorithm: filter by `matchesTags(route, activeTags) && matchesSearch(route, query)`, then `sortedWith(rankComparator(interests))`, then `map { it.toCard(localeProvider(), resources) }`
- [x] 4.9 Implement `matchesTags`, `matchesSearch`, `rankComparator`, `Route.toCard(locale, resources)` per design D4 / D5; `Comparator` MUST use stable ordering — `List.sortedWith` is documented stable in the Kotlin stdlib
- [x] 4.10 Implement `onIntent(intent: HomeIntent)` per design D3: `SearchChanged` writes to `queryFlow`; `FilterTagToggled` toggles `activeTagsFlow`; `CardClicked` sends `NavigateToRoute(...)` to `_effects`
- [x] 4.11 Create `HomeViewModelFactory(content, prefs, resources, localeProvider): ViewModelProvider.Factory` for non-default constructor parameter injection (replaced by Koin in a later change)

## 5. CultureInterestViewModel

- [x] 5.1 Create `frontend/mobile/app/src/main/java/com/mcis/memoir/ui/culture/CultureInterestViewModel.kt`: constructor `(private val prefs: UserPreferencesRepository)`; `val selected: StateFlow<Set<String>> = prefs.selectedInterests.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())`. `toggle(tagId)` MUST read the fresh value via `prefs.selectedInterests.first()` (NOT `selected.value`) because `WhileSubscribed(5000)` can hold a stale snapshot when toggle is fired during the 5-second grace period after the screen loses subscribers: `fun toggle(tagId: String) { viewModelScope.launch { val cur = prefs.selectedInterests.first(); prefs.setInterests(if (tagId in cur) cur - tagId else cur + tagId) } }`. `fun skip() { viewModelScope.launch { prefs.setInterests(emptySet()) } }`
- [x] 5.2 Create `CultureInterestViewModelFactory(prefs): ViewModelProvider.Factory`

## 6. Rewrite HomeScreen.kt

- [x] 6.1 Replace function signature: `@Composable fun HomeScreen(viewModel: HomeViewModel, onNavigateToSaved: () -> Unit, onNavigateToMemories: () -> Unit, modifier: Modifier = Modifier)` — drop `selectedLanguage` and `initialInterests`; drop `onNavigateToHome` (you cannot navigate to Home from Home); keep `onMoreClick` semantics via the effect collector in the navigator (see task 8.2)
- [x] 6.2 Inside the function: `val state by viewModel.state.collectAsStateWithLifecycle(initialValue = HomeState(isLoading = true))`
- [x] 6.3 Delete the `CategoryItem` data class declaration (currently `HomeScreen.kt:55`), the `categoryList = remember { ... }` block (lines ~87-97), the filter-in-Composable block (lines ~100-136), the `getResourceEntryName` + `_zh`-suffix lookup block (lines ~189-200), and every `if (isChinese) X_zh else X` usage
- [x] 6.4 Header text: `stringResource(R.string.home_subtitle)` and `stringResource(R.string.home_headline)` only — no `_zh` branch
- [x] 6.5 Search bar: `SearchBar(query = state.query, onQueryChange = { viewModel.onIntent(HomeIntent.SearchChanged(it)) }, placeholder = stringResource(R.string.home_search_placeholder))`
- [x] 6.6 Chip row: render an `"all"` chip explicitly + one `CategoryChip` per `TagCatalog.all` entry. `isSelected = if (tag.id == "all") state.activeTags.isEmpty() || "all" in state.activeTags else tag.id in state.activeTags`. `onClick` for the `"all"` chip clears `activeTags`; `onClick` for any other chip emits `FilterTagToggled(tag.id)` (which the ViewModel handles by removing `"all"` if present and toggling the chosen tag)
- [x] 6.7 Card list: render `state.cards.forEach { card -> RouteCardItem(card = card, onClick = { viewModel.onIntent(HomeIntent.CardClicked(card.id)) }) }`. The existing `ui.components.RouteCard` Composable accepts a `RouteData` and stays as-is for now (legacy screens still use it); create a NEW Composable named `RouteCardItem` in `ui/home/RouteCardItem.kt` with signature `@Composable fun RouteCardItem(card: RouteCard, onClick: () -> Unit, modifier: Modifier = Modifier)` that renders the same visual layout as `ui.components.RouteCard` but reads from the pre-resolved `ui.home.RouteCard` DTO (no `isChinese` parameter, no internal locale lookup). Naming rule: `RouteCardItem` is the canonical name from this change forward; route-bookmarking change #4's `SavedScreen` reuses it
- [x] 6.8 Empty state: when `state.cards.isEmpty() && state.query.isNotBlank()`, render `Text(stringResource(R.string.home_no_results, state.query))` — add the string `<string name="home_no_results">No routes match "%1$s"</string>` to `values/strings.xml` AND `<string name="home_no_results">沒有符合「%1$s」的路徑</string>` to `values-zh/strings.xml`
- [x] 6.9 Loading state: when `state.isLoading == true`, render a `CircularProgressIndicator` and no empty-state text
- [x] 6.10 The `HomeScreenPreview()` should use a stateless `HomeContent(state, onIntent)` inner Composable, so the preview can pass a hand-built `HomeState(cards = listOf(RouteCard(id = "demo", title = "Demo Route", category = "Demo", heroDrawableRes = R.drawable.sounds_of_temple, description = "...")), isLoading = false)` and `onIntent = {}` without a real ViewModel

## 7. Rewrite CultureInterestScreen.kt

- [x] 7.1 Replace function signature: `@Composable fun CultureInterestScreen(viewModel: CultureInterestViewModel, onStartExploringClick: () -> Unit, onSkipClick: () -> Unit, modifier: Modifier = Modifier)` — drop `selectedLanguage` and `initialInterests`
- [x] 7.2 Delete the `CultureInterest` data class (currently `CultureInterestScreen.kt:40`), the `interests = remember { ... }` block (lines ~65-73), the local `selectedInterests` state, and the per-id `when (interest.id) { ... }` block that does manual `_zh` lookups (lines ~180-189)
- [x] 7.3 Source the option list from `TagCatalog.all`; render each row's label as `stringResource(tag.labelRes)` only (no `_zh` branch — resolved by `values-zh/`)
- [x] 7.4 `val selected by viewModel.selected.collectAsStateWithLifecycle(initialValue = emptySet())`
- [x] 7.5 Toggle handler: `viewModel.toggle(tag.id)`
- [x] 7.6 Skip handler: `viewModel.skip(); onSkipClick()`
- [x] 7.7 Subtitle / headline: `stringResource(R.string.culture_interest_subtitle)` and `stringResource(R.string.culture_interest_headline)` only — no `_zh` branch
- [x] 7.8 Skip button + Start Exploring button labels: `stringResource(R.string.skip_button)` and `stringResource(R.string.start_exploring_button)`
- [x] 7.9 Preview uses a stateless `CultureInterestContent(selected, onToggle, onSkip, onStart)` inner Composable

## 8. Rewrite MyAppNavigation entries

- [x] 8.1 Delete the `userInterests` / `initialInterests` threading from the file (lines ~28-29 and the consumers at HomeDestination / CultureInterestDestination entries)
- [x] 8.2 `HomeDestination` entry: `val ctx = LocalContext.current; val vm: HomeViewModel = viewModel(factory = HomeViewModelFactory(content = MemoirApplication.content, prefs = MemoirApplication.prefs, resources = ctx.resources, localeProvider = { LocaleController.currentLocale() })); HomeScreen(viewModel = vm, onNavigateToSaved = { backStack.add(SavedDestination) }, onNavigateToMemories = { backStack.add(MemoriesDestination) }); LaunchedEffect(vm) { vm.effects.collect { e -> when (e) { is HomeEffect.NavigateToRoute -> backStack.add(RouteDetailDestination(e.routeId)); is HomeEffect.ShowSnackbar -> Unit } } }`
- [x] 8.3 `LocaleController.currentLocale()` is `@Composable`, so it can NOT be invoked from inside the `localeProvider = { ... }` lambda that runs later in the VM's non-Composable scope. Hoist `val currentLocale = LocaleController.currentLocale()` ABOVE the factory construction and pass `localeProvider = { currentLocale }`. The captured snapshot is acceptable because change #2 D2 guarantees that `AppCompatDelegate.setApplicationLocales` triggers Activity recreation, which rebuilds `MyAppNavigation` (and therefore the VM via the factory) with a fresh `currentLocale` value. The VM never observes a locale change mid-life — that's a feature, not a limitation, since it sidesteps mid-flow state churn
- [x] 8.4 `CultureInterestDestination` entry: `val vm: CultureInterestViewModel = viewModel(factory = CultureInterestViewModelFactory(MemoirApplication.prefs)); CultureInterestScreen(viewModel = vm, onStartExploringClick = { coroutineScope.launch { MemoirApplication.prefs.markOnboardingDone() }; backStack.add(HomeDestination) }, onSkipClick = { backStack.add(HomeDestination) })`
- [x] 8.5 Verify the rest of `MyAppNavigation` still compiles — the other screens still pass `selectedLanguage = selectedLanguage` (sourced from change #2's `language` Flow), which is intentional: those screens are migrated by their own respective changes

## 9. Tests

- [x] 9.1 Create `frontend/mobile/app/src/test/java/com/mcis/memoir/ui/home/RankingTest.kt` (pure JUnit5): assert `rankComparator(emptySet())` preserves order; assert `rankComparator(setOf("temples"))` ranks `tags=["temples"]` before `tags=["architecture"]`; assert multi-match outranks single-match; assert zero-match still appears at the end
- [x] 9.2 Create `HomeViewModelTest.kt` (JUnit5 + MockK + Turbine): use `runTest`; build a fake `ContentRepository` (interface, hand-rolled fake — MockK NOT required for the loader because it's a small interface) and a `mockk<UserPreferencesRepository>()` (MockK earns its keep here because the interface has 8 members); use `Turbine.test { ... }` on `vm.state` and assert: (a) initial `isLoading = true`, (b) after first emission `cards` reflect ASCII order, (c) `SearchChanged("temple")` produces cards filtered to title-contains, (d) `FilterTagToggled("temples")` produces cards filtered by tag intersection, (e) interest Flow emitting new set re-emits state with new ranking, (f) `CardClicked` emits `NavigateToRoute` effect on the separate effects Turbine
- [x] 9.3 Create `CultureInterestViewModelTest.kt` (JUnit5 + Turbine): assert `toggle("temples")` calls `setInterests(setOf("temples"))`; assert `toggle("temples")` after `selected = setOf("temples")` calls `setInterests(emptySet())`; assert `skip()` calls `setInterests(emptySet())`
- [x] 9.4 Create `frontend/mobile/app/src/test/java/com/mcis/memoir/ui/home/HomeContentTagValidationTest.kt` (JUnit5) — owned by this change, NOT an edit to change #1's `ContentValidationTest.kt`. The test loads the bundled `data/tainan-route/routes/*.json` from the JVM test classpath (same approach as `ContentValidationTest` in change #1) and asserts: (a) every route's `tags` list is non-empty, (b) `tags.all { it in TagCatalog.ids }`. Living in this change's test directory preserves the "no cross-change file edits" rule from this proposal §Impact
- [x] 9.5 Run `cd frontend/mobile && ./gradlew :app:testDebugUnitTest`; assert all JUnit4 AND JUnit5 tests pass; assert task report shows both engines

## 10. Verification gate

- [x] 10.1 `cd frontend/mobile && ./gradlew :app:assembleDebug` succeeds
- [x] 10.2 `cd frontend/mobile && ./gradlew :app:testDebugUnitTest` passes (content-pipeline, language-toggle, home-discovery tests all green)
- [x] 10.3 `openspec validate home-discovery --strict` reports zero issues
- [ ] 10.4 Emulator smoke: complete onboarding (language → interests Crafts + Temples → Start Exploring) → Home shows routes with Temples-tagged routes first (Crafts has 0 matches against current data); chip-toggle `architecture` shows only architecture-tagged routes; type `"廟"` under zh locale → filtered correctly
- [x] 10.5 Emulator smoke: tap a card → navigates to RouteDetailScreen with the correct routeId
- [x] 10.6 Record Koin-change follow-up obligation: "Koin change MUST delete `HomeViewModelFactory` and `CultureInterestViewModelFactory` and replace `viewModel(factory = ...)` call sites with `koinViewModel()` injection"
