## Why

`HomeScreen` today reads `data.MockData.routes` directly and filters in-Composable by matching `route.categoryEn` (an English literal) against the localized chrome-text version of each category chip. The result: with the live Mock data (3 distinct English category values), four of the six category chips (`old_streets`, `trade`, `colonial`, `crafts`) silently render zero results — an existing latent bug. It also keeps the screen permanently tied to `MockData` + the legacy `selectedLanguage: String` plumbing, so the `tainan-route-content-pipeline` foundation laid by change #1 has no real consumer yet, and the `language-toggle` infrastructure built by change #2 still goes through `_zh`-suffix lookups on this screen.

Adjacent build-quality footnote: `CultureInterestScreen.kt:180-189` currently references `R.string.home_category_temples_zh`, `home_category_old_streets_zh`, `home_category_architecture_zh`, `home_category_trade_zh`, `home_category_colonial_zh`, `home_category_crafts_zh` — none of which exist in `strings.xml` (only `home_category_all` / `home_category_all_zh` are defined). Whether the file currently compiles or not, this change's rewrite of `CultureInterestScreen` deletes that block entirely (replaced by `TagCatalog`-driven `stringResource(R.string.culture_X)`), so the dangling references go away as a side effect.

This change does three jobs that umbrella §2 lists under "home-discovery": (a) move search + chip filtering onto the new `ContentRepository`-served content, (b) make `CultureInterestScreen`'s saved interests actually *do something* on `HomeScreen` (the umbrella's "affects ranking" requirement), and (c) demonstrate the umbrella's MVI pattern on the first screen where `combine`-driven multi-source emission earns Turbine + MockK.

## What Changes

- Replace `data.MockData.routes` reads with `ContentRepository.routes()` Flow + per-route lookups via `route(id)`.
- Replace English-string category matching with a structured tag model: each `Route` JSON now declares `"tags": ["temples", "folk-belief", ...]` strings drawn from a fixed list, and home filtering / ranking is set-intersection over those ids. The 6 user-facing tag ids (`temples`, `old_streets`, `architecture`, `trade`, `colonial`, `crafts`) are kept verbatim from `CultureInterestScreen`'s existing list.
- Introduce a `TagCatalog` Kotlin object that owns the canonical list of `TagId` ⇒ `@StringRes labelRes` pairs. `HomeScreen`'s chip row, `CultureInterestScreen`'s option list, and any future filter UI all import from this single catalog.
- Update the `_assets.json` consumer / generator in `tainan-route-content-pipeline` to also write a `tags` array per route (sourced from a CSV column or from a `_tags.json` side input — implementation chooses). The generator MUST emit at least one valid tag per route so no route disappears from results when a user has selected interests.
- Migrate `HomeScreen` to MVI: `HomeViewModel(contentRepo, prefsRepo, resources, localeProvider)` exposes `state: StateFlow<HomeState>` and `effects: Flow<HomeEffect>`. `HomeState` carries `cards: List<RouteCard>`, `isLoading`, `query`, `activeTags`, `userInterests`, `error`. `HomeIntent` covers `SearchChanged(q)`, `FilterTagToggled(tagId)`, `CardClicked(routeId)`. `HomeEffect` covers `NavigateToRoute(routeId)`. (The umbrella §6 template lists `Refresh` and `ShowSnackbar` — both are deferred here under YAGNI; `Refresh` would be a no-op against change #1's emit-once-and-complete routes Flow, and `ShowSnackbar` has no consumer because no snackbar host is wired until the navigation rewrite. Future screens add them when first needed.)
- Implement the umbrella's prescribed combine: `combine(contentRepo.routes(), prefsRepo.selectedInterests, queryFlow, activeTagsFlow) { routes, interests, query, tags -> rank(filter(routes, query, tags), interests) }` produces `HomeState.cards`.
- Define `RouteCard` (rendering DTO) under `ui.home`: `data class RouteCard(val id: String, val title: String, val category: String, val heroDrawableRes: Int, val description: String)`. The ViewModel resolves `LocalizedText[currentLocale]` once when building cards, and resolves drawable names via `Resources.getIdentifier`.
- **Filtering rules** (locked here):
  - When `activeTags` is empty OR contains the `"all"` sentinel id, no filter is applied.
  - When `activeTags` is non-empty, a route appears if `route.tags intersect activeTags` is non-empty.
  - Search: case-insensitive `title.contains(query.trim(), ignoreCase = true)` against the locale-resolved title only. The existing per-character Han boundary split is dropped — simpler semantics, fewer edge cases, accepting that an English query like `"sound"` against a Chinese title like `"台南廟宇聲音路線"` won't match (the inverse via Chinese query against Chinese title still works).
- **Ranking rules** (locked here):
  - Score each surviving route as `(route.tags intersect userInterests).size`.
  - Stable sort by score descending, then by `index.json.routes` ASCII order as the tiebreaker. Zero-score routes still appear at the bottom — interests only rank, they never filter.
  - When `userInterests` is empty, every route scores 0 and the natural index order is preserved.
- Migrate `HomeScreen` and `CultureInterestScreen` off `selectedLanguage: String` parameter + `_zh`-suffix `stringResource` lookups. Both screens now use `stringResource(R.string.X)` directly (resolves to `values-zh/strings.xml` under `zh` locale, established by change #2) and `LocaleController.currentLocale()` where they need `LocalizedText[...]` against content from change #1.
- Delete the inline `CategoryItem` declaration in `HomeScreen.kt:55`, the `CultureInterest` data class in `CultureInterestScreen.kt:40`, and the brittle resource-name-string-suffixing block at `HomeScreen.kt:189-200` — all superseded by `TagCatalog`.
- Add an empty-state row when `state.cards.isEmpty()` and `state.query.isNotBlank()`: a localized "No routes match" message. When the cards list is empty for any other reason (no content loaded yet, error state), use `state.isLoading` / `state.error` to render a spinner or error chrome.
- Rewrite `MyAppNavigation`'s `HomeDestination` entry: pass no `selectedLanguage` / `initialInterests` parameters; instead obtain `HomeViewModel` via `viewModel { HomeViewModel(MemoirApplication.content, MemoirApplication.prefs) }` and route `HomeEffect.NavigateToRoute` to `backStack.add(RouteDetailDestination(routeId))`.
- Add Gradle catalog entries for `mockk`, `turbine`, `kotlinx-coroutines-test` (already added by change #2, reused here), and JUnit5 (`junit-jupiter`, `android-junit5` plugin) so this change can write the umbrella's prescribed VM tests with multi-emit Flow timing assertions. JUnit4 stays for now where existing tests rely on it; new tests use JUnit5.
- **Not in scope**: navigation rewrite to Nav3 typed routes (`HomeDestination` stays as the existing `data object` for now), bottom-nav cleanup, Room, LLM, camera, memory wizard, locale-aware sorting beyond ASCII tiebreak, search ranking (TF-IDF, fuzzy), pagination (route count ≤ 20).

## Capabilities

### New Capabilities
- `home-discovery`: HomeScreen's route discovery surface — search, chip filtering, interest-driven ranking, MVI ViewModel, `TagCatalog`, and the route-card rendering pipeline. Owns the contract that culture interests selected during onboarding influence the order of routes the user sees first.

### Modified Capabilities
- `content-pipeline`: routes JSON now carries a non-empty `tags` array, and the generator validates that every route has at least one tag from `TagCatalog.knownIds`. The mock-data fallback drawable column gains nothing new — only the tag list is added.

## Impact

- **New files**:
  - `frontend/mobile/app/src/main/java/com/mcis/memoir/ui/home/HomeViewModel.kt` (with `HomeState`, `HomeIntent`, `HomeEffect`, `RouteCard`)
  - `frontend/mobile/app/src/main/java/com/mcis/memoir/ui/home/TagCatalog.kt`
  - `frontend/mobile/app/src/test/java/com/mcis/memoir/ui/home/HomeViewModelTest.kt` (JUnit5 + MockK + Turbine)
  - `frontend/mobile/app/src/test/java/com/mcis/memoir/ui/home/RankingTest.kt` (pure unit; deterministic sort behavior)
  - `frontend/mobile/app/src/test/java/com/mcis/memoir/ui/home/TagCatalogTest.kt`
- **Modified files**:
  - `frontend/mobile/app/src/main/java/com/mcis/memoir/HomeScreen.kt` — rewrite signature, drop `selectedLanguage` / `initialInterests` parameters, drop the in-Composable filter logic, drop `CategoryItem`, drop `categoryEn`-string comparison, render from `HomeState`, fire `HomeIntent` on every interaction.
  - `frontend/mobile/app/src/main/java/com/mcis/memoir/CultureInterestScreen.kt` — drop `selectedLanguage` parameter, drop `_zh` suffix lookups, drop the inline `CultureInterest` data class, source the interest list from `TagCatalog`, persist on toggle via `prefsRepo.setInterests(...)` (the ViewModel still resides here as a small `CultureInterestViewModel` — its job is just persist-on-toggle, no `combine` needed).
  - `frontend/mobile/app/src/main/java/com/mcis/memoir/MyAppNavigation.kt` — rewrite `HomeDestination` and `CultureInterestDestination` entries to construct ViewModels; remove `userInterests`, `initialInterests` threading.
  - `frontend/mobile/gradle/libs.versions.toml` — add catalog entries for `mockk`, `turbine`, `junit-jupiter`, `android-junit5` (plugin).
  - `frontend/mobile/app/build.gradle.kts` — apply `alias(libs.plugins.android.junit5)`, add `testImplementation(libs.junit.jupiter)`, `testImplementation(libs.mockk)`, `testImplementation(libs.turbine)`.
  - Tasks under `tainan-route-content-pipeline/tasks.md` are NOT re-edited here (avoid cross-change merge contention). The new tag-validation assertions live in a NEW test file owned by this change (`HomeContentTagValidationTest.kt`), not by amending change #1's `ContentValidationTest.kt`. The content-pipeline delta in this change's `specs/content-pipeline/spec.md` carries the requirement; the test that fulfills it lives in this change's test directory.
- **Modified content** (data files): `data/tainan_routes.csv` may need a new `tags` column OR `data/tainan-route/_tags.json` is added with `{ routeId: ["tag", ...] }`; pick at implementation time. `data/tainan-route/routes/*.json` regenerate with a non-empty `tags` array each.
- **Deleted code (inside modified files)**: `HomeScreen.kt:55` (`CategoryItem`), `CultureInterestScreen.kt:40` (`CultureInterest`), plus the brittle `getResourceEntryName` + `getIdentifier(name + "_zh")` block at `HomeScreen.kt:189-200`.
- **Dependencies added**: MockK + Turbine + JUnit5 — the umbrella's standard test stack activates from this change forward.
- **Risk acknowledgements**:
  - Adding tags to existing route JSON forces a content-pipeline regeneration step, which will surface in the CI sync check. Designer must update CSV (or `_tags.json`) before this change can merge.
  - The "interests rank, not filter" rule means a user with no matching tags sees zero impact — communicate this in the UX writing (the empty-state copy is search-only; no "no interest matches" copy).
  - JUnit5 + JUnit4 coexist in `:app:testDebugUnitTest` via the `android-junit5` Gradle plugin; this is the supported configuration but pipeline operators must know that adding a JUnit5 runner doesn't migrate existing JUnit4 content-pipeline tests automatically.
- **Not changed**:
  - `tainan-route-content-pipeline/proposal.md`, `design.md`, `specs/` — the modification is a new requirement bolted on via this change's specs `MODIFIED Requirements` block.
  - Any non-Home/CultureInterest screen.
  - `language-toggle` artifacts.
