## 1. String resources

- [x] 1.1 Add to `frontend/mobile/app/src/main/res/values/strings.xml`:
  ```xml
  <string name="route_detail_save_place_saved">Saved</string>
  <string name="saved_empty_message">No saved routes yet</string>
  ```
- [x] 1.2 Add to `frontend/mobile/app/src/main/res/values-zh/strings.xml`:
  ```xml
  <string name="route_detail_save_place_saved">已收藏</string>
  <string name="saved_empty_message">目前還沒有收藏的路徑</string>
  ```
- [x] 1.3 Verify `<string name="saved_headline">我的旅遊計畫</string>` exists in `res/values-zh/strings.xml` (should have been added by change #2's bulk-mirror task 5.1); if missing, add it
- [x] 1.4 Do NOT delete any existing `_zh`-suffixed string from `res/values/strings.xml` — other screens still consume them

## 2. RouteDetailViewModel

- [x] 2.1 Create `frontend/mobile/app/src/main/java/com/mcis/memoir/ui/route/RouteDetailState.kt`:
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

  data class JourneyRowState(val order: Int, val spotId: String, val label: String)
  ```
- [x] 2.2 Create `ui/route/RouteDetailIntent.kt`:
  ```kotlin
  sealed interface RouteDetailIntent {
      data object BookmarkToggled : RouteDetailIntent
      data class SpotClicked(val spotId: String) : RouteDetailIntent
  }
  ```
- [x] 2.3 Create `ui/route/RouteDetailEffect.kt`:
  ```kotlin
  sealed interface RouteDetailEffect {
      data class ShowError(val msg: String) : RouteDetailEffect
  }
  ```
  Note: kept for future use; no consumer in this change. Persistence failures currently land in `Log.w` not in this effect
- [x] 2.4 Create `ui/route/RouteDetailViewModel.kt`: constructor `(private val routeId: String, private val contentRepo: ContentRepository, private val prefsRepo: UserPreferencesRepository, private val resources: Resources, private val localeProvider: () -> Locale) : ViewModel()`
- [x] 2.5 Internal state holders: `private val _state = MutableStateFlow(RouteDetailState(isLoading = true))`, `val state: StateFlow<RouteDetailState> = _state.asStateFlow()`, `private val _effects = Channel<RouteDetailEffect>(Channel.BUFFERED)`, `val effects: Flow<RouteDetailEffect> = _effects.receiveAsFlow()`
- [x] 2.6 In `init { }`, `viewModelScope.launch { combine(flow { emit(contentRepo.route(routeId)) }, prefsRepo.bookmarkedRouteIds) { route, bookmarks -> buildState(route, bookmarks) }.catch { e -> _state.update { it.copy(isLoading = false, error = e.message) } }.collect { _state.value = it } }`
- [x] 2.7 Implement `private suspend fun buildState(route: Route?, bookmarks: Set<String>): RouteDetailState` — note `suspend` is required because the function calls `resolveSpotLabel` which is itself `suspend` (`combine`'s transform lambda is already a suspend context). Returns `RouteDetailState(isLoading = false, error = "route_not_found")` if `route == null`; otherwise resolves locale-dependent fields (title, description, journey labels via `contentRepo.spot(stop.spotId)?.title?.get(localeProvider()) ?: stop.spotId`), drawable name via `resources.getIdentifier(route.heroImage, "drawable", "com.mcis.memoir")`, and `isSaved = routeId in bookmarks`
- [x] 2.8 Declare `private val toggleMutex = Mutex()` at the VM scope (import `kotlinx.coroutines.sync.Mutex` + `withLock`). Implement `fun onIntent(intent: RouteDetailIntent)`: `BookmarkToggled` launches inside `viewModelScope`, then wraps the read-modify-write in `toggleMutex.withLock { val current = prefsRepo.bookmarkedRouteIds.first(); val next = if (routeId in current) current - routeId else current + routeId; prefsRepo.setBookmarkedRouteIds(next) }` — the mutex serializes concurrent toggles on this VM instance to prevent lost-update; `SpotClicked` is a no-op reducer (navigation handled by Composable callback)
- [x] 2.9 Create `ui/route/RouteDetailViewModelFactory.kt`: `class RouteDetailViewModelFactory(private val routeId: String, private val content: ContentRepository, private val prefs: UserPreferencesRepository, private val resources: Resources, private val localeProvider: () -> Locale) : ViewModelProvider.Factory` with `create<T>(modelClass) = RouteDetailViewModel(routeId, content, prefs, resources, localeProvider) as T`

## 3. SavedViewModel

- [x] 3.1 Create `frontend/mobile/app/src/main/java/com/mcis/memoir/ui/saved/SavedState.kt`:
  ```kotlin
  data class SavedState(
      val isLoading: Boolean = false,
      val cards: List<RouteCard> = emptyList(),
      val error: String? = null
  )
  ```
  Reuses the `com.mcis.memoir.ui.home.RouteCard` DTO from change #3
- [x] 3.2 Do NOT create a `SavedIntent` file. Earlier draft proposed `SavedIntent.CardClicked` as a no-op for "MVI shape consistency"; YAGNI — the Composable invokes the navigator callback directly. If a future SavedScreen interaction needs a real reducer (e.g. swipe-to-unsave), introduce the type then
- [x] 3.3 Create `ui/saved/SavedViewModel.kt`: constructor `(private val contentRepo: ContentRepository, private val prefsRepo: UserPreferencesRepository, private val resources: Resources, private val localeProvider: () -> Locale) : ViewModel()`
- [x] 3.4 Internal state holders: `private val _state = MutableStateFlow(SavedState(isLoading = true))`, `val state: StateFlow<SavedState> = _state.asStateFlow()`. No effects channel (no one-shot side effects this screen owns)
- [x] 3.5 In `init { }`, `viewModelScope.launch { combine(contentRepo.routes(), prefsRepo.bookmarkedRouteIds) { routes, bookmarks -> SavedState(isLoading = false, cards = routes.filter { it.id in bookmarks }.map { it.toCard(localeProvider(), resources) }) }.catch { e -> _state.update { it.copy(isLoading = false, error = e.message) } }.collect { _state.value = it } }`
- [x] 3.6 No `onIntent` method on `SavedViewModel` — the screen has no actions that need a reducer in this change
- [x] 3.7 Create `ui/saved/SavedViewModelFactory.kt`: identical pattern to `RouteDetailViewModelFactory` but without `routeId`

## 4. Rewrite RouteDetailScreen.kt

- [x] 4.1 Replace signature with `@Composable fun RouteDetailScreen(viewModel: RouteDetailViewModel, onBackClick: () -> Unit, onNavigateToSaved: () -> Unit, onNavigateToMemories: () -> Unit, onSpotClick: (String) -> Unit, modifier: Modifier = Modifier)` — drop `selectedLanguage`, `routeId`, `isSaved`, `onToggleSave`, `onNavigateToHome`
- [x] 4.2 `val state by viewModel.state.collectAsStateWithLifecycle()`
- [x] 4.3 Preserve the `var isNavigating by remember { mutableStateOf(false) }` back-click debounce — keep the exact existing guard around `onBackClick()`
- [x] 4.4 Loading state: when `state.isLoading == true`, render a `CircularProgressIndicator`
- [x] 4.5 Error state: when `state.error != null`, render `stringResource(R.string.route_not_found)` (which already exists in both `values/` and `values/strings.xml` as `route_not_found` + `route_not_found_zh`; verify the `values-zh/` mirror exists per change #2 task 5.1)
- [x] 4.6 Hero image: `Image(painter = painterResource(state.heroDrawableRes), ...)`; if `state.heroDrawableRes == 0`, render a neutral placeholder `Box(Modifier.fillMaxWidth().height(259.dp).background(Color.LightGray))`
- [x] 4.7 Title: `Text(text = state.title, ...)`
- [x] 4.8 Description: `Text(text = state.description, ...)`
- [x] 4.9 "Your Journey" heading: `stringResource(R.string.route_detail_your_journey)` — drop the `_zh` branch
- [x] 4.10 Timeline items: `state.journey.forEachIndexed { index, row -> TimelineItem(number = row.order, label = row.label, hasLine = index < state.journey.size - 1, onClick = { onSpotClick(row.spotId); viewModel.onIntent(RouteDetailIntent.SpotClicked(row.spotId)) }) }` — the intent fires alongside the navigator callback for MVI shape consistency even though the reducer is a no-op
- [x] 4.11 Save button label: `Text(text = stringResource(if (state.isSaved) R.string.route_detail_save_place_saved else R.string.route_detail_save_place), ...)`
- [x] 4.12 Save button color: `if (state.isSaved) Color(0xFF5C5C5C) else DesignTokens.colorMaroon` (preserve existing color logic)
- [x] 4.13 Save button click handler: `onClick = { viewModel.onIntent(RouteDetailIntent.BookmarkToggled) }`
- [x] 4.14 BottomNavigationBar: the existing `BottomNavigationBar` signature in `ui/components/CommonUI.kt` requires `isChinese: Boolean` (verified). This change does NOT touch the component itself (cleanup deferred); derive the value locally from the active locale: `val isChinese = LocaleController.currentLocale().language == "zh"` and pass it through. The full migration of `BottomNavigationBar` off the `isChinese` parameter is a follow-up obligation owned by `memory-library-actions` (the last screen-touching change in umbrella build order)
- [x] 4.15 Delete the `selectedLanguage`-derived `isChinese` local val; delete every `if (isChinese) … else …` pair; delete the `MockData.routes.find` lookup; delete `data.MockData` and `data.RouteData` imports if unused after the rewrite
- [x] 4.16 `RouteDetailScreenPreview()` instantiates a stateless `RouteDetailContent(state, onIntent, onBackClick, onNavigateToSaved, onNavigateToMemories, onSpotClick)` inner Composable with a hand-built `RouteDetailState(isLoading = false, routeId = "demo", title = "Demo Route", description = "A demo description", heroDrawableRes = R.drawable.sounds_of_temple, journey = listOf(JourneyRowState(1, "demo_spot", "Demo Spot")), isSaved = false)` so the preview renders without a real ViewModel

## 5. Rewrite SavedScreen.kt

- [x] 5.1 Replace signature with `@Composable fun SavedScreen(viewModel: SavedViewModel, onNavigateToHome: () -> Unit, onNavigateToMemories: () -> Unit, onMoreClick: (String) -> Unit, modifier: Modifier = Modifier)` — drop `selectedLanguage`, `savedRouteIds`, `onNavigateToSaved` (you cannot navigate to Saved from Saved)
- [x] 5.2 `val state by viewModel.state.collectAsStateWithLifecycle()`
- [x] 5.3 Headline: `Text(text = stringResource(R.string.saved_headline), ...)` — drop the hard-coded `"我的旅遊計畫"`/`"My travel plan"` literal
- [x] 5.4 Loading state: when `state.isLoading == true`, render a `CircularProgressIndicator`
- [x] 5.5 Empty state: when `state.cards.isEmpty() && state.isLoading == false`, render `Text(text = stringResource(R.string.saved_empty_message), ...)` — drop the hard-coded `"目前還沒有收藏的路徑"`/`"No saved routes yet"` literal
- [x] 5.6 Populated state: `state.cards.forEach { card -> RouteCardItem(card = card, onClick = { onMoreClick(card.id) }) }` — uses `com.mcis.memoir.ui.home.RouteCardItem` introduced by change #3 (pinned name)
- [x] 5.7 BottomNavigationBar: derive `val isChinese = LocaleController.currentLocale().language == "zh"` locally and pass through (same handling as RouteDetailScreen task 4.14; `memory-library-actions` owns the long-term cleanup of the component itself)
- [x] 5.8 Delete the `selectedLanguage`-derived `isChinese` local val; delete `MockData.routes.filter` read; delete `data.MockData` import if unused
- [x] 5.9 `SavedScreenPreview()` uses a stateless `SavedContent(state, onMoreClick, onNavigateToHome, onNavigateToMemories)` inner Composable with a hand-built `SavedState(isLoading = false, cards = listOf(RouteCard(id = "demo", title = "Demo", category = "Demo", heroDrawableRes = R.drawable.sounds_of_temple, description = "...")))` for preview

## 6. Rewrite MyAppNavigation entries

- [x] 6.1 If `var savedRouteIds by remember { mutableStateOf(...) }` (or `val savedRouteIds by prefsRepo.bookmarkedRouteIds.collectAsStateWithLifecycle(...)`) exists at the top of `MyAppNavigation()`, delete it — neither consumer screen needs it any more
- [x] 6.2 `RouteDetailDestination` entry block: per design D9, construct `RouteDetailViewModel` via `viewModel(key = key.routeId, factory = RouteDetailViewModelFactory(routeId = key.routeId, content = MemoirApplication.content, prefs = MemoirApplication.prefs, resources = ctx.resources, localeProvider = { currentLocale }))` where `currentLocale` is hoisted from `LocaleController.currentLocale()` before the factory construction; `RouteDetailScreen(viewModel = vm, onBackClick = { backStack.removeLastOrNull() }, onNavigateToSaved = { backStack.add(SavedDestination) }, onNavigateToMemories = { backStack.add(MemoriesDestination) }, onSpotClick = { spotId -> backStack.add(SpotIntroDestination(spotId)) })`. Do NOT add a `LaunchedEffect(vm) { vm.effects.collect { ... } }` block — `RouteDetailEffect.ShowError` has no consumer in this change; the effects channel intentionally remains uncollected until a snackbar host arrives in the navigation rewrite
- [x] 6.3 `SavedDestination` entry block: construct `SavedViewModel` via `viewModel(factory = SavedViewModelFactory(...))`; `SavedScreen(viewModel = vm, onNavigateToHome = { backStack.clear(); backStack.add(HomeDestination) }, onNavigateToMemories = { backStack.add(MemoriesDestination) }, onMoreClick = { routeId -> backStack.add(RouteDetailDestination(routeId)) })`
- [x] 6.4 Verify no other destination entry still references the deleted `savedRouteIds` Compose state
- [x] 6.5 Verify `MyAppNavigation` compiles after the rewrite — `home-discovery` change #3 already touched this file; this change adds two more entries to the migrated set

## 7. Tests

- [x] 7.1 Create `frontend/mobile/app/src/test/java/com/mcis/memoir/ui/route/RouteDetailViewModelTest.kt` (JUnit5 + MockK + Turbine): build a fake `ContentRepository` (interface, hand-rolled) returning a known route + spot map; `mockk<UserPreferencesRepository>()` with `every { bookmarkedRouteIds } returns MutableStateFlow(emptySet())`; `coEvery { setBookmarkedRouteIds(any()) } just Runs`; assert (a) initial state is `isLoading = true`, (b) post-init state has `isSaved = false` and pre-resolved title, (c) `BookmarkToggled` invokes `setBookmarkedRouteIds(setOf(routeId))`, (d) flipping the bookmarks flow's value re-emits state with `isSaved = true`, (e) route-not-found yields `state.error == "route_not_found"`, (f) `SpotClicked` is a state no-op
- [x] 7.2 Create `ui/saved/SavedViewModelTest.kt`: fake `ContentRepository` with `routes() = flow { emit(listOf(A, B, C, D, E)) }`; mock prefs `bookmarkedRouteIds = MutableStateFlow(setOf("B", "E"))`; assert (a) state has `cards.map { it.id } == ["B", "E"]` (filter preserves source order), (b) flipping bookmarks to `setOf("A")` re-emits state with `cards.map { it.id } == ["A"]`, (c) empty bookmarks → `cards.isEmpty()`, (d) `CardClicked` is a reducer no-op
- [x] 7.3 Run `cd frontend/mobile && ./gradlew :app:testDebugUnitTest`; assert all tests pass (including change #1's JUnit4, change #2's JUnit4, change #3's JUnit5, this change's JUnit5)

## 8. Verification gate

- [x] 8.1 `cd frontend/mobile && ./gradlew :app:assembleDebug` succeeds
- [x] 8.2 `cd frontend/mobile && ./gradlew :app:testDebugUnitTest` passes (full suite)
- [x] 8.3 `openspec validate route-bookmarking --strict` reports zero issues
- [x] 8.4 Emulator smoke (en): home → tap a route → RouteDetail loads → tap Save (button label flips "Save Place" → "Saved", color flips maroon → gray) → tap Back → bottom-nav Saved → that route appears in the list → tap the card → returns to RouteDetail with state.isSaved already true
- [x] 8.5 Emulator smoke (zh): toggle locale → relaunch → home → tap a route → labels render in Chinese → tap "收藏地點" → label flips to "已收藏"
- [x] 8.6 Emulator smoke (empty): clear all bookmarks (toggle off each one from RouteDetail) → bottom-nav Saved → empty-state message renders ("No saved routes yet" / "目前還沒有收藏的路徑")
- [x] 8.7 Record Koin-change follow-up obligation: "Koin change MUST delete `RouteDetailViewModelFactory` and `SavedViewModelFactory` and replace `viewModel(factory = ...)` call sites with `koinViewModel { parametersOf(routeId) }` (for RouteDetail) / `koinViewModel()` (for Saved)"
