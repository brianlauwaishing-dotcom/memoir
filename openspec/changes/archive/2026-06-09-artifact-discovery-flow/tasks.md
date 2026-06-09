## 1. Umbrella + content-pipeline schema bump

- [x] 1.1 Edit `docs/superpowers/specs/2026-06-07-mobile-direct-app-wiring-design.md` §4.4 `Artifact` data class snippet to add `question: LocalizedText` (required) and `galleryImage: String? = null` (optional). Add a one-line "Recorded deviation" note below the snippet explaining that change `artifact-discovery-flow` introduced the two fields to support the existing UX
- [x] 1.2 Update `frontend/mobile/app/src/main/java/com/mcis/memoir/data/content/model/Artifact.kt`: add `val question: LocalizedText` (no default — required) and `val galleryImage: String? = null` to the constructor. Verify kotlinx-serialization still deserializes the existing committed spot JSONs (which will be regenerated in step 2.x with the new fields — interim build between schema bump and regeneration would fail to deserialize; sequence the steps so the regenerate happens in the same commit as the model update)

## 2. Generator + `_assets.json` migration

- [x] 2.1 Identify the CSV column names for the discovery question (likely `為甚麼要看?（中）` and `為甚麼要看?（英）` per CSV inspection). If the column names differ in practice, update the generator's column-binding map at `data/scripts/generate_content.py` task 2.4 wire-up
- [x] 2.2 In `data/scripts/generate_content.py`, add a `question_zh` / `question_en` column reader for the artifact rows. Each artifact entry being emitted gets `"question": {"en": <text>, "zh": <text>}` written in
- [x] 2.3 Validate non-emptiness: if either `question.en` or `question.zh` is blank, exit non-zero with `empty question for spots.<spotId>.artifacts[<id>]` and write no JSON
- [x] 2.4 Migrate `data/tainan-route/_assets.json` from the flat `"artifactImages": [...]` shape (per change #1's structure) to a keyed map: `"artifacts": { "<artifactId>": { "image": "<drawable>", "galleryImage": "<drawable>?" } }`. Populate `image` for every artifact from the prior `artifactImages` list (preserve order). Populate `galleryImage` from `MockData.DiscoveryItem.galleryImageRes` for the three artifacts of `grand_mazu` (eg1 / eg2 / eg3); leave absent for artifacts that had no gallery image in MockData
- [x] 2.5 In the generator, read each artifact's `image` (required) and optional `galleryImage` from the new map shape; emit `image` into every artifact JSON entry and `galleryImage` only when present. Update the generator's hard-coded `_assets.json` schema validator (the existing missing-binding check) to match the new shape: error message `missing _assets.json binding: spots.<spotId>.artifacts.<id>.image` when the `image` key is missing under an artifact id
- [x] 2.6 Run `python data/scripts/generate_content.py` from repo root; verify exits 0
- [x] 2.7 Inspect the regenerated `data/tainan-route/spots/grand_mazu.json` (the fully-populated example spot) to confirm each artifact entry contains `question.en` + `question.zh` + `image` and (for ids 1/2/3) `galleryImage`
- [x] 2.8 Re-run the generator; confirm `git diff --exit-code data/tainan-route/` reports zero changes (determinism gate)
- [x] 2.9 Update generator fixture `data/scripts/test/fixtures/` and `test_generate_content.py` to cover: empty `question_en` → exit non-zero; valid question + missing `galleryImage` → emit artifact without that field; valid question + valid `galleryImage` → emit both fields
- [x] 2.10 Add a mirror comment in `data/scripts/generate_content.py` near the schema validator: `# Keep this artifact-shape validation in sync with Artifact.kt — both must agree on required fields (id, title, description, question, image)` (mirrors the cross-link convention established by `home-discovery` for `TagCatalog`)

## 3. QuestionHighlight

- [x] 3.1 Create `frontend/mobile/app/src/main/java/com/mcis/memoir/ui/artifact/QuestionHighlight.kt`:
  ```kotlin
  data class QuestionHighlight(val prefix: String, val label: String, val suffix: String)

  fun computeHighlight(question: String, label: String): QuestionHighlight {
      if (label.isEmpty()) return QuestionHighlight(prefix = question, label = "", suffix = "")
      val idx = question.indexOf(label)
      if (idx < 0) return QuestionHighlight(prefix = question, label = "", suffix = "")
      return QuestionHighlight(
          prefix = question.substring(0, idx),
          label = label,
          suffix = question.substring(idx + label.length)
      )
  }
  ```
- [x] 3.2 Create `frontend/mobile/app/src/test/java/com/mcis/memoir/ui/artifact/QuestionHighlightTest.kt` (JUnit5): cover label-at-start, label-at-end, label-in-middle, label-not-found, label-empty, label-appears-twice (only first highlighted), full-width Chinese label/question

## 4. SpotIntroViewModel

- [x] 4.1 Create `ui/spot/SpotIntroState.kt`: `data class SpotIntroState(val isLoading: Boolean = true, val spotId: String? = null, val title: String = "", val heroDrawableRes: Int = 0, val foundLabel: String = "", val discoveryItems: List<DiscoveryItemCard> = emptyList(), val error: String? = null)` and `data class DiscoveryItemCard(val id: Int, val label: String, val imageDrawableRes: Int)`
- [x] 4.2 Create `ui/spot/SpotIntroViewModel.kt`: constructor `(private val spotId: String, private val contentRepo: ContentRepository, private val resources: Resources, private val localeProvider: () -> Locale) : ViewModel()`
- [x] 4.3 Internal: `private val _state = MutableStateFlow(SpotIntroState())`, `val state: StateFlow<SpotIntroState> = _state.asStateFlow()`. No effects channel, no `onIntent` reducer (every screen action is a navigator callback)
- [x] 4.4 In `init { }`, `viewModelScope.launch { ... }`: call `contentRepo.spot(spotId)`; if null, emit `SpotIntroState(isLoading = false, error = "spot_not_found")`; otherwise compute `locale = localeProvider()` and emit a populated state per design D5
- [x] 4.5 Create `ui/spot/SpotIntroViewModelFactory.kt`: `class SpotIntroViewModelFactory(val spotId: String, val content: ContentRepository, val resources: Resources, val localeProvider: () -> Locale) : ViewModelProvider.Factory` with `create<T> = SpotIntroViewModel(spotId, content, resources, localeProvider) as T`

## 5. ArtifactDiscoveryViewModel

- [x] 5.1 Create `ui/artifact/ArtifactDiscoveryState.kt`: `data class ArtifactDiscoveryState(val isLoading: Boolean = true, val spotId: String? = null, val artifactId: Int = 0, val displayPosition: Int = 0, val totalArtifacts: Int = 0, val label: String = "", val highlight: QuestionHighlight = QuestionHighlight("", "", ""), val imageDrawableRes: Int = 0, val error: String? = null)`. `artifactId` is the stable id used for navigation; `displayPosition` is the 1-based slot for the "1/3" UI — they coincide today (per MockData) but are tracked separately so future id renumbering doesn't break either UI label or navigation
- [x] 5.2 Create `ui/artifact/ArtifactDiscoveryViewModel.kt`: constructor `(private val spotId: String, private val artifactId: Int, private val contentRepo: ContentRepository, private val resources: Resources, private val localeProvider: () -> Locale) : ViewModel()`
- [x] 5.3 In `init { }`: look up `spot = contentRepo.spot(spotId)`, then `artifact = spot?.artifacts?.firstOrNull { it.id == artifactId }`; if either is null, emit `ArtifactDiscoveryState(isLoading = false, error = "artifact_not_found")`; otherwise resolve locale text, compute `val displayPosition = spot.artifacts.indexOfFirst { it.id == artifact.id } + 1`, and call `computeHighlight(artifact.question[locale], artifact.title[locale])` to build `state.highlight`; populate `state.artifactId = artifact.id` and `state.displayPosition = displayPosition`
- [x] 5.4 No `onIntent` reducer, no effects channel — both `MoreClicked` and `CameraClicked` are navigator callbacks; YAGNI per route-bookmarking's `SavedViewModel` precedent
- [x] 5.5 Create `ui/artifact/ArtifactDiscoveryViewModelFactory.kt`: parallel to SpotIntro's factory, with `spotId` and `artifactId` as constructor args

## 6. ArtifactDetailViewModel

- [x] 6.1 Create `ui/artifact/ArtifactDetailState.kt`: `data class ArtifactDetailState(val isLoading: Boolean = true, val label: String = "", val description: String = "", val imageDrawableRes: Int = 0, val error: String? = null)`
- [x] 6.2 Create `ui/artifact/ArtifactDetailViewModel.kt`: constructor `(private val spotId: String, private val artifactId: Int, private val contentRepo: ContentRepository, private val resources: Resources, private val localeProvider: () -> Locale) : ViewModel()`
- [x] 6.3 In `init { }`: same lookup pattern as ArtifactDiscovery; emit a state carrying locale-resolved `label` (from `artifact.title`) and `description` (from `artifact.description`) and the resolved drawable for `artifact.image`
- [x] 6.4 No `onIntent` reducer, no effects channel
- [x] 6.5 Create `ui/artifact/ArtifactDetailViewModelFactory.kt`

## 7. Rewrite SpotIntroScreen.kt

- [x] 7.1 Replace signature: `@Composable fun SpotIntroScreen(viewModel: SpotIntroViewModel, onBackClick: () -> Unit, onInfoClick: (String) -> Unit, onDiscoveryItemClick: (Int) -> Unit, modifier: Modifier = Modifier)` — drop `selectedLanguage` and `spotId`
- [x] 7.2 `val state by viewModel.state.collectAsStateWithLifecycle(initialValue = SpotIntroState())`
- [x] 7.3 Drop the `MockData.spots.find` lookup; drop the `isChinese`-derived branching; drop all `R.string.X_zh` lookups
- [x] 7.4 Hero image: `Image(painter = painterResource(state.heroDrawableRes), ...)`; if `state.heroDrawableRes == 0`, render a `Box(Modifier.fillMaxSize().background(Color.DarkGray))` placeholder
- [x] 7.5 Title row: `Text(text = state.title, ...)`
- [x] 7.6 "Discovery Mode" / "Found" headers: `stringResource(R.string.discovery_mode)` / `stringResource(R.string.discovery_found)` only
- [x] 7.7 "Found" count: `Text(text = state.foundLabel, ...)` — replaces the prior hard-coded `"${spot.discoveryItems.size}/${spot.discoveryItems.size}"`
- [x] 7.8 Discovery items list: `LazyColumn { items(state.discoveryItems) { item -> DiscoveryItemRow(item = item, onClick = { onDiscoveryItemClick(item.id) }) } }` — rename the existing `@Composable fun DiscoveryItemCard(...)` at `SpotIntroScreen.kt:185` to `@Composable fun DiscoveryItemRow(item: DiscoveryItemCard, onClick: () -> Unit)` so the Composable (now named after a layout role: row) does not clash with the new `data class DiscoveryItemCard` DTO. The Composable takes the DTO and renders the label + imageRes already resolved (no `isChinese` parameter)
- [x] 7.9 Error state: when `state.error != null`, render `Text(stringResource(R.string.spot_not_found))`
- [x] 7.10 Loading state: when `state.isLoading`, render a centered `CircularProgressIndicator`
- [x] 7.11 `SpotIntroScreenPreview()` uses a stateless `SpotIntroContent(state, onBack, onInfo, onItemClick)` inner Composable with a hand-built `SpotIntroState(isLoading = false, spotId = "demo", title = "Demo Spot", heroDrawableRes = R.drawable.grand_mazu_temple, foundLabel = "3/3", discoveryItems = listOf(DiscoveryItemCard(1, "Dragon Pillar", R.drawable.dragon_pillar)))`

## 8. Rewrite ArtifactDiscoveryScreen.kt

- [x] 8.1 Replace signature: `@Composable fun ArtifactDiscoveryScreen(viewModel: ArtifactDiscoveryViewModel, onBackClick: () -> Unit, onInfoClick: (String) -> Unit, onMoreClick: (String, Int) -> Unit, onCameraClick: () -> Unit, modifier: Modifier = Modifier)` — drop `selectedLanguage`, `spotId`, `artifactId`
- [x] 8.2 `val state by viewModel.state.collectAsStateWithLifecycle(initialValue = ArtifactDiscoveryState())`
- [x] 8.3 Drop the `MockData` lookup; drop the inline `question.split(label)` highlight computation (now in VM via `state.highlight`)
- [x] 8.4 Header: `stringResource(R.string.discovery_mode)`, `stringResource(R.string.back_button)`, `stringResource(R.string.spot_explore_info_content_description)`
- [x] 8.5 "Look Closer" label + position `"${state.displayPosition}/${state.totalArtifacts}"`: `stringResource(R.string.discovery_look_closer)` + the formatted position string
- [x] 8.6 Question rendering: build `AnnotatedString` from `state.highlight.prefix` + a styled span `(SpanStyle(color = Color(0xFFBF1B20)))` over `state.highlight.label` + `state.highlight.suffix`. If `state.highlight.label.isEmpty()`, only render the prefix (no span)
- [x] 8.7 "More" button: `Text(stringResource(R.string.more_button), ...)`; `onClick = { state.spotId?.let { sid -> onMoreClick(sid, state.artifactId) } }` — MUST pass `artifactId` (stable id) NOT `displayPosition`; guard against state-not-yet-loaded
- [x] 8.8 Camera button: `onClick = { onCameraClick() }`; contentDescription `stringResource(R.string.spot_explore_take_photo)`
- [x] 8.9 Info button: `onClick = { state.spotId?.let(onInfoClick) }`
- [x] 8.10 Image: `Image(painter = painterResource(state.imageDrawableRes), ...)`; placeholder if zero
- [x] 8.11 Error / loading states (parallel to SpotIntroScreen tasks 7.9 / 7.10)
- [x] 8.12 `ArtifactDiscoveryScreenPreview()` uses a stateless `ArtifactDiscoveryContent(state, onBack, onInfo, onMore, onCamera)` with a hand-built state

## 9. Rewrite ArtifactDetailScreen.kt

- [x] 9.1 Replace signature: `@Composable fun ArtifactDetailScreen(viewModel: ArtifactDetailViewModel, onBackClick: () -> Unit, onInfoClick: () -> Unit, onCameraClick: () -> Unit, onNavigateToHome: () -> Unit, onNavigateToSaved: () -> Unit, onNavigateToMemories: () -> Unit, modifier: Modifier = Modifier)` — drop `selectedLanguage`, `spotId`, `artifactId`. `onInfoClick` is parameterless because the navigator closes over `key.spotId` and bridges (see task 9.8); the Composable does not need to know its own spot id
- [x] 9.2 `val state by viewModel.state.collectAsStateWithLifecycle(initialValue = ArtifactDetailState())`
- [x] 9.3 Drop `MockData` lookup; drop all `_zh`-suffix branches
- [x] 9.4 Header: `stringResource(R.string.discovery_you_discovered)`, back, info — all canonical
- [x] 9.5 Artifact title: `Text(text = state.label, ...)`
- [x] 9.6 Storytelling text: `Text(text = state.description, ...)`
- [x] 9.7 Image: `Image(painter = painterResource(state.imageDrawableRes), ...)`; placeholder if zero
- [x] 9.8 Info button: `onClick = { onInfoClick() }`. The navigator (task 10.3) closes over `key.spotId` and calls `backStack.add(SpotDetailDestination(key.spotId))` inside the lambda, so the Composable itself stays spot-id-agnostic
- [x] 9.9 Camera button: `onCameraClick()`; contentDescription `stringResource(R.string.spot_explore_take_photo)`
- [x] 9.10 BottomNavigationBar: derive `val isChinese = LocaleController.currentLocale().language == "zh"` per route-bookmarking precedent
- [x] 9.11 Error / loading states
- [x] 9.12 `ArtifactDetailScreenPreview()` uses a stateless inner Composable

## 10. Rewrite MyAppNavigation entries

- [x] 10.1 `SpotIntroDestination` entry: hoist `val ctx = LocalContext.current; val currentLocale = LocaleController.currentLocale()`; construct `val vm = viewModel(key = key.spotId, factory = SpotIntroViewModelFactory(spotId = key.spotId, content = MemoirApplication.content, resources = ctx.resources, localeProvider = { currentLocale }))`; call `SpotIntroScreen(viewModel = vm, onBackClick = { backStack.removeLastOrNull() }, onInfoClick = { spotId -> backStack.add(SpotDetailDestination(spotId)) }, onDiscoveryItemClick = { artifactId -> backStack.add(ArtifactDiscoveryDestination(key.spotId, artifactId)) })`. Remove `selectedLanguage = selectedLanguage` from this entry
- [x] 10.2 `ArtifactDiscoveryDestination` entry: same factory pattern with both `spotId = key.spotId` and `artifactId = key.artifactId` constructor args; `viewModel(key = "${key.spotId}/${key.artifactId}", factory = ...)` so distinct ids get distinct VM instances; remove `selectedLanguage = selectedLanguage`
- [x] 10.3 `ArtifactDetailDestination` entry: same factory pattern; the Composable's `onInfoClick: () -> Unit` (per task 9.8 option b) is wired here as `{ backStack.add(SpotDetailDestination(key.spotId)) }`; remove `selectedLanguage = selectedLanguage`
- [x] 10.4 Verify other destination entries that still pass `selectedLanguage = selectedLanguage` to non-this-change screens continue to compile (SpotDetail, Memories, etc. still expect the parameter)

## 11. Tests

- [x] 11.1 Create `frontend/mobile/app/src/test/java/com/mcis/memoir/ui/spot/SpotIntroViewModelTest.kt` (JUnit5 + Turbine): fake `ContentRepository.spot(id)` returning known spot; assert state hydrates with correct title / discoveryItems count / foundLabel; assert `spot(id) == null` yields `error = "spot_not_found"`
- [x] 11.2 Create `ui/artifact/ArtifactDiscoveryViewModelTest.kt`: assert highlight is correctly computed for label-in-question; assert artifactIndex / totalArtifacts derived correctly; assert null artifact yields `error = "artifact_not_found"`
- [x] 11.3 Create `ui/artifact/ArtifactDetailViewModelTest.kt`: assert description renders from locale-resolved `Artifact.description`; assert error case
- [x] 11.4 `QuestionHighlightTest.kt` already covered in task 3.2
- [x] 11.5 Create `ui/artifact/ArtifactSchemaValidationTest.kt` (JVM-only, no Robolectric): load the committed `data/tainan-route/spots/*.json` from the test classpath; for each spot, for each artifact, assert `question.en.isNotBlank() && question.zh.isNotBlank()`; for non-null `galleryImage`, assert it's a key in `R.drawable::class.java.fields.map { it.name }.toSet()` (using the Kotlin reflection trick established by `home-discovery` for drawable validation)
- [x] 11.6 Run `cd frontend/mobile && ./gradlew :app:testDebugUnitTest`; assert full suite passes (changes #1 JUnit4, #2 JUnit4, #3 JUnit5, #4 JUnit5, this change JUnit5)

## 12. Verification gate

- [x] 12.1 `cd frontend/mobile && ./gradlew :app:assembleDebug` succeeds
- [x] 12.2 `cd frontend/mobile && ./gradlew :app:testDebugUnitTest` passes (full suite)
- [x] 12.3 `openspec validate artifact-discovery-flow --strict` reports zero issues
- [ ] 12.4 Emulator smoke (en): home → tap a route → tap a spot → SpotIntro renders hero + discovery items list → tap an item → ArtifactDiscovery renders with question text and the artifact label highlighted in maroon → tap More → ArtifactDetail renders storytelling text → tap Camera → CameraPreview opens
- [ ] 12.5 Emulator smoke (zh): toggle locale → relaunch → repeat the above flow; all chrome text and content text render in Chinese
- [ ] 12.6 Emulator smoke (error): synthesize a navigation to a non-existent spot id (via a test build flag or temporary code) → SpotIntro renders the error state
- [ ] 12.7 CI: confirm the new `ArtifactSchemaValidationTest` passes; confirm the `git diff --exit-code data/tainan-route/` check still passes after the schema-bumped JSON is committed
- [x] 12.8 Record Koin-change follow-up obligation: "Koin change MUST delete `SpotIntroViewModelFactory`, `ArtifactDiscoveryViewModelFactory`, `ArtifactDetailViewModelFactory` and replace their `viewModel(factory = ...)` call sites with `koinViewModel { parametersOf(spotId, artifactId) }`"
