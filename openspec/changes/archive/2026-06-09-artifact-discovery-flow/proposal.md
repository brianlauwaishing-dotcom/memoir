## Why

Umbrella §2 line 5 ("SpotIntro → ArtifactDiscovery → ArtifactDetail with real multi-spot data") covers three screens that share the same problem set: each reads `data.MockData.spots.find { it.id == spotId }` directly, each carries `selectedLanguage: String` with `_zh`-suffix `stringResource` calls, and each renders artifact text that the umbrella's `Artifact` schema (`§4.4`) cannot actually represent — `MockData.DiscoveryItem` has five distinct text fields (`labelEn/Zh`, `questionEn/Zh`, `moreInfoEn/Zh`) plus two images (`imageRes`, `galleryImageRes`), while the umbrella spec gives `Artifact` only `{id, title, description, image}`. The schema gap is why this change couldn't just land as a screen migration — the data model itself needs to grow.

`tainan-route-content-pipeline` (change #1) is the read path; `language-toggle` (change #2) is the locale surface; `home-discovery` (change #3) and `route-bookmarking` (change #4) established the MVI template + `internal Route.toCard` extension pattern. This change does the same migration for the three artifact-discovery screens.

## What Changes

- **MODIFY** the `Artifact` schema in `content-pipeline` to add two fields:
  - `question: LocalizedText` (REQUIRED) — the discovery prompt shown on `ArtifactDiscoveryScreen` (e.g. `"How many dragons are on the pillars?"` / `"龍柱上有幾條龍呢？"`). The screen highlights the artifact title inside the prompt in maroon.
  - `galleryImage: String?` (OPTIONAL, drawable name) — a secondary illustration (e.g. the black-and-white sketch used by SpotDetail). Nullable because not every artifact has one.
  - The existing `description: LocalizedText` is repurposed as the storytelling text shown on `ArtifactDetailScreen` (matches `MockData.DiscoveryItem.moreInfoEn/Zh`). The umbrella's §4.4 reference is recorded as deviated and a one-line note is added.
- **MODIFY** the content generator + `_assets.json` to emit and bind the new fields:
  - The CSV source gains two columns: `question_zh` and `question_en` (or a single delimiter-separated column, generator's call) — the existing `為甚麼要看?` columns appear to be candidates; if the designer's CSV already carries the prompt text, the generator harvests it.
  - `_assets.json` gains an optional `galleryImage` per spot-artifact pair (alongside the existing `artifactImages` list).
  - `ContentValidationTest` (and the new tag-validation test) asserts: every artifact has non-empty `question.en` AND `question.zh`; every non-null `galleryImage` resolves via `Resources.getIdentifier`.
- Rewrite `SpotIntroScreen` to read from `SpotIntroViewModel`:
  - State: `{ isLoading, spotId, title, heroDrawableRes, discoveryItems: List<DiscoveryItemCard>, isError }`.
  - `DiscoveryItemCard(id, label, imageDrawableRes)` is the rendering DTO.
  - Drop `selectedLanguage` parameter; drop `_zh`-suffix branches; drop hard-coded `"${spot.discoveryItems.size}/${spot.discoveryItems.size}"` literal — replaced with a state-derived `found/total` pair (both equal to the full artifact count in MVP, since there's no actual "discovery progress" tracking yet — record as Open Question).
- Rewrite `ArtifactDiscoveryScreen` to read from `ArtifactDiscoveryViewModel`:
  - State: `{ isLoading, spotId, artifactIndex, totalArtifacts, label, question, imageDrawableRes, isError }`.
  - Pre-compute the `question` highlight: the VM does `question.indexOf(label)` and returns `QuestionHighlight(prefix, label, suffix)` so the Composable doesn't perform string splits itself.
  - Intent: `MoreClicked`, `CameraClicked` are no-op reducers (navigation handled by Composable callbacks); included only if they meaningfully shape future state — preliminary decision is to OMIT them under YAGNI per route-bookmarking's precedent.
- Rewrite `ArtifactDetailScreen` to read from `ArtifactDetailViewModel`:
  - State: `{ isLoading, label, description, imageDrawableRes, isError }`.
  - Same MVI template; no Effects channel.
- All three VMs get factories alongside the home-discovery + route-bookmarking ones (Koin change consolidates later).
- Migrate `MyAppNavigation`'s `SpotIntroDestination`, `ArtifactDiscoveryDestination`, `ArtifactDetailDestination` entries: construct each VM via `viewModel(key = …, factory = …)`; drop `selectedLanguage` parameter threading.
- String hygiene: every `R.string.X_zh` lookup in the three screens is replaced with the canonical `R.string.X` form (resolves via `values-zh/strings.xml` from change #2). The legacy `_zh` strings remain in `values/strings.xml` — other screens still consume them.
- `BottomNavigationBar` derivation in `ArtifactDetailScreen` follows route-bookmarking's pattern: `val isChinese = LocaleController.currentLocale().language == "zh"`.
- **Not in scope**:
  - `SpotDetailScreen` (reached via SpotIntro's Info button but stays MockData-backed under `selectedLanguage` threading; cleanup deferred — orphan note in Open Questions).
  - `SpotExploreScreen` (declared in `MyAppNavigation.kt:104-106` but no `is SpotExploreDestination` entryProvider case — dead code; deferred).
  - Camera capture handling (CameraPreviewScreen's `onCaptureClick` is umbrella #6).
  - "Discovery progress" tracking — current UI shows `n/n` where n is the artifact count, not a true found-vs-total counter. No persistent "user has discovered artifact X" state today; out of scope.
  - Audio / "Look Closer" hints / Podcast.

## Capabilities

### New Capabilities
- `artifact-discovery-flow`: SpotIntro / ArtifactDiscovery / ArtifactDetail screen surfaces, their three ViewModels, the artifact-question highlight algorithm, and the SpotIntro → ArtifactDiscovery → ArtifactDetail navigation contract. Owns the artifact `question` highlight UX.

### Modified Capabilities
- `content-pipeline`: `Artifact` schema gains `question: LocalizedText` (required) and `galleryImage: String?` (optional). Generator + `_assets.json` + validation test follow.

## Impact

- **New files**:
  - `frontend/mobile/app/src/main/java/com/mcis/memoir/ui/spot/SpotIntroViewModel.kt` (+State, +Intent if any, +ViewModelFactory)
  - `frontend/mobile/app/src/main/java/com/mcis/memoir/ui/artifact/ArtifactDiscoveryViewModel.kt`
  - `frontend/mobile/app/src/main/java/com/mcis/memoir/ui/artifact/ArtifactDetailViewModel.kt`
  - `frontend/mobile/app/src/main/java/com/mcis/memoir/ui/artifact/QuestionHighlight.kt` (the `data class QuestionHighlight(val prefix: String, val label: String, val suffix: String)` + a pure function that computes it)
  - Tests: `SpotIntroViewModelTest.kt`, `ArtifactDiscoveryViewModelTest.kt`, `ArtifactDetailViewModelTest.kt`, `QuestionHighlightTest.kt`, `ArtifactSchemaValidationTest.kt` (the new content-pipeline assertion, owned by this change like `HomeContentTagValidationTest`)
- **Modified files**:
  - `frontend/mobile/app/src/main/java/com/mcis/memoir/SpotIntroScreen.kt`, `ArtifactDiscoveryScreen.kt`, `ArtifactDetailScreen.kt` — drop `selectedLanguage`, drop `_zh`-suffix branches, drop `MockData` reads, render from state, fire intents via VM.
  - `frontend/mobile/app/src/main/java/com/mcis/memoir/MyAppNavigation.kt` — rewrite three destination entries.
  - `data/scripts/generate_content.py` — emit `question` and `galleryImage` in each artifact entry; error on missing `question` text.
  - `data/tainan-route/_assets.json` — schema bumps to include optional `galleryImage` per artifact id.
  - `data/tainan-route/spots/*.json` — regenerated with the new fields.
- **Dependencies added**: none (test stack already on JUnit5+MockK+Turbine from change #3).
- **Risk acknowledgements**:
  - CSV source must already carry the question text (it appears to per the column names `為甚麼要看?（中）`/`為甚麼要看?（英）`); if not, designer fills them in as part of the implementing PR. Generator MUST exit non-zero if any artifact ends up with an empty question.
  - The Artifact schema change ripples to `Spot.artifacts` JSON. Existing committed spot JSONs are regenerated by this change; the CI sync check catches drift.
  - SpotDetailScreen still reads MockData + `selectedLanguage` after this change — visible inconsistency in the same screen-family. Acceptable per scope cap.
- **Not changed**:
  - SpotDetailScreen, SpotExploreScreen.
  - CameraPreviewScreen (next change owns it).
  - Other umbrella changes' artifacts.
  - `MockData.kt` — preserved as long as SpotDetail still reads it.
