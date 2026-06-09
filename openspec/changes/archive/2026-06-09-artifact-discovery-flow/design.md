## Context

Three-screen migration plus a content-pipeline schema growth. Consumes:
- `tainan-route-content-pipeline` change #1: `ContentRepository.spot(id): Spot?`, `Spot.artifacts: List<Artifact>` (with `id`, `title`, `description`, `image`).
- `language-toggle` change #2: `LocaleController.currentLocale()`, `values-zh/strings.xml`.
- `home-discovery` change #3: MVI template (State / Intent / Effect / `Channel`-backed effects), `internal Route.toCard` pattern, `RouteCardItem` Composable naming convention.
- `route-bookmarking` change #4: per-VM `Mutex` precedent (not needed here — no writes), staged factory pattern, `BottomNavigationBar` `isChinese` derivation.

**Current state**:
- `SpotIntroScreen.kt:50` reads `MockData.spots.find { it.id == spotId }`.
- `SpotIntroScreen.kt:133` renders the hard-coded `"${spot.discoveryItems.size}/${spot.discoveryItems.size}"` — a fake "found/total" counter that is always `n/n`.
- `ArtifactDiscoveryScreen.kt:51` reads `spot.discoveryItems.find { it.id == artifactId }`.
- `ArtifactDiscoveryScreen.kt:160-173` does `question.split(label)` inline in the Composable to compute the red-highlight `AnnotatedString` — pure logic that belongs in the VM.
- `ArtifactDetailScreen.kt:51` same lookup.
- All three screens use `if (isChinese) X_zh else X` for every `stringResource` call (≥ 6 sites across the three files).
- `MockData.DiscoveryItem` (the runtime type) has fields the umbrella `Artifact` schema (`§4.4`) does not: `questionEn/Zh`, `moreInfoEn/Zh`, `galleryImageRes`. The current `Artifact { id, title, description, image }` cannot represent the existing UX without a schema growth.
- `MyAppNavigation` still threads `selectedLanguage = selectedLanguage` into all three destinations; bookmarkable / saved state is unrelated to this change.

**Constraints**:
- Cannot precede change #1 (depends on `ContentRepository`).
- Cannot precede change #2 (depends on `LocaleController` + `values-zh/`).
- Should follow change #3 + #4 to inherit the MVI / factory precedent.
- Test stack JUnit5 + MockK + Turbine activated in change #3.

## Goals / Non-Goals

**Goals:**
1. Migrate the three umbrella-listed screens (SpotIntro, ArtifactDiscovery, ArtifactDetail) off MockData onto `ContentRepository`.
2. Grow the `Artifact` schema to carry `question` and `galleryImage`, so the existing UX has a place to live.
3. Move the question-highlight string-split logic into the VM as a pure-function-tested `QuestionHighlight` value.
4. Drop `selectedLanguage` parameter + `_zh`-suffix usage from all three screens.
5. Keep `SpotDetailScreen`, `SpotExploreScreen`, `CameraPreviewScreen`, `MockData.kt` untouched.

**Non-Goals:**
- Migrating `SpotDetailScreen` or wiring `SpotExploreScreen`.
- Camera capture wiring (umbrella #6).
- A real "found vs total" artifact-discovery progress counter (would need DataStore persistence per-(spotId, artifactId); deferred).
- Animated highlight transitions on the question text.
- Locale-aware artifact rotation order.

## Decisions

### D1. `Artifact` schema grows to `{ id, title, description, question, image, galleryImage? }`

```kotlin
@Serializable data class Artifact(
    val id: Int,
    val title: LocalizedText,
    val description: LocalizedText,         // storytelling text (shown on ArtifactDetailScreen)
    val question: LocalizedText,            // discovery prompt (shown on ArtifactDiscoveryScreen)
    val image: String,                       // main artifact image (drawable name)
    val galleryImage: String? = null         // optional secondary illustration (drawable name)
)
```

**Why this shape vs alternatives:**
- *Rename `description` → `reveal` and put `question` where `description` was*: cleaner naming, but more umbrella churn and more spec.md edits for marginal benefit.
- *Bundle `question` + `description` into a single text field*: loses the structural separation needed by `QuestionHighlight` and forces the Composable to text-parse content.
- *Add a separate `ArtifactDiscovery` type distinct from `ArtifactDetail`*: doubles the model count for a single conceptual thing — over-engineering.

The umbrella deviation is one paragraph in §4.4. The generator + `_assets.json` + validation test for `question` non-emptiness are the load-bearing work.

### D2. `QuestionHighlight` is a pure data class + pure function

```kotlin
data class QuestionHighlight(
    val prefix: String,    // text before the highlighted label
    val label: String,      // the highlighted label itself
    val suffix: String      // text after the label
)

fun computeHighlight(question: String, label: String): QuestionHighlight {
    val idx = question.indexOf(label)
    if (idx < 0) return QuestionHighlight(prefix = question, label = "", suffix = "")
    return QuestionHighlight(
        prefix = question.substring(0, idx),
        label = label,
        suffix = question.substring(idx + label.length)
    )
}
```

The Composable renders an `AnnotatedString` from the three parts. Tests cover: label-found, label-absent (no highlight), label-at-start, label-at-end, label-appears-twice (only first occurrence is highlighted), empty-label (no highlight).

**Why pure function instead of method on VM:**
- Trivially unit-testable without coroutines / Flow / MockK.
- Reusable if a future screen wants the same highlight semantics on a different prompt.
- Stateless — no reason for it to live on the VM.

### D3. Per-screen ViewModels with `(spotId, artifactId)` or `(spotId)` constructor args

```kotlin
class SpotIntroViewModel(
    private val spotId: String,
    private val contentRepo: ContentRepository,
    private val resources: Resources,
    private val localeProvider: () -> Locale
) : ViewModel() { … }

class ArtifactDiscoveryViewModel(
    private val spotId: String,
    private val artifactId: Int,
    private val contentRepo: ContentRepository,
    private val resources: Resources,
    private val localeProvider: () -> Locale
) : ViewModel() { … }

class ArtifactDetailViewModel(
    private val spotId: String,
    private val artifactId: Int,
    private val contentRepo: ContentRepository,
    private val resources: Resources,
    private val localeProvider: () -> Locale
) : ViewModel() { … }
```

Each VM loads its own state via a single `viewModelScope.launch { … }` (no `combine` — only one async source: the spot lookup; no Flow-of-flows). The reducer-less screens (Discovery + Detail have no actions beyond navigation) skip the `onIntent` ceremony entirely, matching `SavedViewModel`'s YAGNI precedent. SpotIntro likewise: clicking a discovery item is a navigator callback, not an intent.

**Why three separate VMs vs one shared `SpotViewModel`:**
- A shared VM would need a `currentArtifactIndex: Int` state field to know which artifact ArtifactDiscovery is showing; that's the navigator's responsibility (the destination carries `artifactId`).
- Cross-screen lifecycle is messier — when does the shared VM die? At screen scope, `ViewModelStoreOwner` is per-NavEntry by default.
- Three small VMs with constructor-injected ids are simpler.

### D4. State shapes

```kotlin
data class SpotIntroState(
    val isLoading: Boolean = true,
    val spotId: String? = null,
    val title: String = "",
    val heroDrawableRes: Int = 0,
    val foundLabel: String = "",          // e.g. "3/3"
    val discoveryItems: List<DiscoveryItemCard> = emptyList(),
    val error: String? = null
)

data class DiscoveryItemCard(
    val id: Int,
    val label: String,
    val imageDrawableRes: Int
)

data class ArtifactDiscoveryState(
    val isLoading: Boolean = true,
    val spotId: String? = null,
    val artifactId: Int = 0,                 // stable id used for navigation (e.g. ArtifactDetailDestination)
    val displayPosition: Int = 0,            // 1-based slot for "1/3" UI; today equals artifactId per MockData, separated so a future renumbering doesn't break either UI label or navigation
    val totalArtifacts: Int = 0,             // denominator of the "1/3" UI
    val label: String = "",                   // for header
    val highlight: QuestionHighlight = QuestionHighlight("", "", ""),
    val imageDrawableRes: Int = 0,
    val error: String? = null
)

data class ArtifactDetailState(
    val isLoading: Boolean = true,
    val label: String = "",
    val description: String = "",
    val imageDrawableRes: Int = 0,
    val error: String? = null
)
```

`foundLabel` is a pre-formatted string `"$found/$total"`. Both numbers are the total artifact count in MVP (no actual progress tracking yet — see Open Questions); the field exists so a future "real progress" change can swap the value without touching the VM-to-Composable contract.

### D5. ViewModel implementations

`SpotIntroViewModel.init`:
```kotlin
viewModelScope.launch {
    val spot = contentRepo.spot(spotId)
    _state.value = if (spot == null) {
        SpotIntroState(isLoading = false, error = "spot_not_found")
    } else {
        val locale = localeProvider()
        SpotIntroState(
            isLoading = false,
            spotId = spot.id,
            title = spot.title[locale],
            heroDrawableRes = resources.getIdentifier(spot.heroImage, "drawable", "com.mcis.memoir"),
            foundLabel = "${spot.artifacts.size}/${spot.artifacts.size}",
            discoveryItems = spot.artifacts.map { artifact ->
                DiscoveryItemCard(
                    id = artifact.id,
                    label = artifact.title[locale],
                    imageDrawableRes = resources.getIdentifier(artifact.image, "drawable", "com.mcis.memoir")
                )
            }
        )
    }
}
```

`ArtifactDiscoveryViewModel.init`:
```kotlin
viewModelScope.launch {
    val spot = contentRepo.spot(spotId)
    val artifact = spot?.artifacts?.firstOrNull { it.id == artifactId }
    _state.value = if (spot == null || artifact == null) {
        ArtifactDiscoveryState(isLoading = false, error = "artifact_not_found")
    } else {
        val locale = localeProvider()
        val label = artifact.title[locale]
        val question = artifact.question[locale]
        val position = spot.artifacts.indexOfFirst { it.id == artifact.id } + 1
        ArtifactDiscoveryState(
            isLoading = false,
            spotId = spot.id,
            artifactId = artifact.id,
            displayPosition = position,
            totalArtifacts = spot.artifacts.size,
            label = label,
            highlight = computeHighlight(question, label),
            imageDrawableRes = resources.getIdentifier(artifact.image, "drawable", "com.mcis.memoir")
        )
    }
}
```

`ArtifactDetailViewModel.init`:
```kotlin
viewModelScope.launch {
    val spot = contentRepo.spot(spotId)
    val artifact = spot?.artifacts?.firstOrNull { it.id == artifactId }
    _state.value = if (spot == null || artifact == null) {
        ArtifactDetailState(isLoading = false, error = "artifact_not_found")
    } else {
        val locale = localeProvider()
        ArtifactDetailState(
            isLoading = false,
            label = artifact.title[locale],
            description = artifact.description[locale],
            imageDrawableRes = resources.getIdentifier(artifact.image, "drawable", "com.mcis.memoir")
        )
    }
}
```

None of the three VMs declare an `Intent` reducer or an `Effects` channel — every user action on these screens is a pure navigation callback handled by `MyAppNavigation`. This matches `SavedViewModel`'s YAGNI shape.

### D6. Composable signatures (after rewrite)

```kotlin
@Composable fun SpotIntroScreen(
    viewModel: SpotIntroViewModel,
    onBackClick: () -> Unit,
    onInfoClick: (String) -> Unit,
    onDiscoveryItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier
)

@Composable fun ArtifactDiscoveryScreen(
    viewModel: ArtifactDiscoveryViewModel,
    onBackClick: () -> Unit,
    onInfoClick: (String) -> Unit,
    onMoreClick: (String, Int) -> Unit,
    onCameraClick: () -> Unit,
    modifier: Modifier = Modifier
)

@Composable fun ArtifactDetailScreen(
    viewModel: ArtifactDetailViewModel,
    onBackClick: () -> Unit,
    onInfoClick: (String) -> Unit,
    onCameraClick: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToSaved: () -> Unit,
    onNavigateToMemories: () -> Unit,
    modifier: Modifier = Modifier
)
```

All drop `selectedLanguage`. `SpotIntroScreen` drops `spotId`. `ArtifactDiscoveryScreen` drops `spotId, artifactId` (in the VM). `ArtifactDetailScreen` drops `spotId, artifactId`. Composable previews use stateless inner `*Content(state, ...)` Composables to render without a real ViewModel — same pattern as route-bookmarking 4.16 / 5.9.

### D7. Generator + `_assets.json` schema growth

`_assets.json` schema bump:
```json
{
  "spots": {
    "grand_mazu": {
      "heroImage": "grand_mazu_temple",
      "photographyTipImages": ["sounds_of_temple", "sea_protection", "faith_hidden"],
      "artifacts": {
        "1": { "image": "dragon_pillar", "galleryImage": "eg1" },
        "2": { "image": "hanfan", "galleryImage": "eg2" },
        "3": { "image": "mazu_statue", "galleryImage": "eg3" }
      }
    }
  }
}
```

Previously `artifactImages` was a flat list; now it's keyed by artifact id with two image slots per entry. `galleryImage` is optional; when absent the JSON output simply omits the field (kotlinx-serialization's `explicitNulls = false` handles that — set in change #1's `ContentJson`).

Generator changes:
- Reads `question_zh` + `question_en` from the CSV (the designer's existing `為甚麼要看?（中）` + `為甚麼要看?（英）` columns are the source — verify column names at implementation time and adjust the column-name binding).
- Errors non-zero if any artifact ends up with `question.en.isBlank()` or `question.zh.isBlank()`.
- Reads `galleryImage` from `_assets.json` per (spotId, artifactId); emits it into the artifact JSON only when present.
- Determinism rules unchanged.

CI sync check unchanged in mechanism; the new fields are part of the regenerated output and `git diff --exit-code` catches drift.

### D8. New string keys

None. The three screens already use existing `R.string.*` ids (e.g. `discovery_mode`, `discovery_found`, `discovery_look_closer`, `more_button`, `spot_explore_take_photo`, `discovery_you_discovered`, `back_button`, `spot_explore_info_content_description`, `spot_not_found`). Change #2's `values-zh/` mirror covers all of them. This change adds zero new strings — confirms the bilingual coverage assumption against change #2 task 5.1.

### D9. Generator-side mirror obligation

Per the `home-discovery` precedent, the generator's hard-coded "expected non-empty fields per artifact" list (now including `question.en` and `question.zh`) must stay in sync with the Kotlin `Artifact` model. Cross-link comment in `data/scripts/generate_content.py` mirrors the same cross-link comment block added by change #3 for `TagCatalog`.

## Risks / Trade-offs

- **Schema deviation from umbrella §4.4**: requires a one-line note in the umbrella spec ("recorded deviation"). Adding fields is additive — old `Spot.artifacts[].description` consumers (none after this change lands) would not break.
- **Designer CSV may not yet have populated `question_en`/`question_zh` columns**: implementing PR includes a content authoring pass to verify; generator's error gate prevents a broken commit.
- **`galleryImage` is committed as a hand-curated mapping in `_assets.json`**: same drift risk as the route `heroImage` mapping addressed by change #1. Trust the designer + the validation test.
- **`MockData.DiscoveryItem.galleryImageRes` is consumed by `SpotDetailScreen.kt` (`R.drawable.eg1/eg2/eg3` in MockData)** which this change does NOT migrate. Verify the gallery images still exist as drawable references after the schema migration; they do, because `_assets.json` still binds those drawable names, and `MockData` itself is untouched.
- **`computeHighlight` returns empty highlight on label-absent**: the existing Composable falls back to "just render the question with no highlight". The pure function preserves that behavior; tested.
- **`SpotIntroState.foundLabel` is `"n/n"` (no real progress)**: confusing UX claim. Acceptable for MVP per Open Questions.
- **Two writes to the same key in `_assets.json` schema** (`artifactImages` list before, `artifacts: { id: { image, galleryImage } }` map now): this is a breaking change to `_assets.json` itself. The generator MUST be updated in lockstep; CI sync check is the safety net.

## Migration Plan

1. Update umbrella `2026-06-07-mobile-direct-app-wiring-design.md:252` `Artifact` data class snippet to add `question` and `galleryImage` (the umbrella is the canonical reference; this change records the deviation).
2. Update content-pipeline generator: read `question_en`/`question_zh` from CSV; emit `question` and (optional) `galleryImage` in artifact JSON; error gate on empty question.
3. Migrate `_assets.json` from flat `artifactImages: [...]` to `artifacts: { id: { image, galleryImage? } }` structure.
4. Regenerate `data/tainan-route/spots/*.json`; commit alongside the generator + `_assets.json` changes.
5. Update the Kotlin `Artifact` model to add `question` (non-null) and `galleryImage` (nullable, default null).
6. Create `QuestionHighlight.kt` (data class + `computeHighlight` fun) and its unit test.
7. Implement `SpotIntroViewModel`, `ArtifactDiscoveryViewModel`, `ArtifactDetailViewModel` + factories.
8. Rewrite the three Composables.
9. Rewrite the three `MyAppNavigation` entries.
10. Write `SpotIntroViewModelTest`, `ArtifactDiscoveryViewModelTest`, `ArtifactDetailViewModelTest`, `QuestionHighlightTest`, `ArtifactSchemaValidationTest`.
11. Run `:app:testDebugUnitTest` — all green.
12. Emulator smoke: home → tap a route → tap a spot → SpotIntro renders with hero + artifact list under en, then under zh after locale toggle → tap an item → ArtifactDiscovery renders with question + red-highlighted label → tap More → ArtifactDetail renders with storytelling text → tap Camera → CameraPreview opens (unmodified by this change).

**Rollback**: revert the change commit. Screens regress to their MockData-backed forms. The `Artifact` JSON additions are tolerated by `ignoreUnknownKeys = true` in `ContentJson` — no schema rollback needed at the asset level.

## Open Questions

- **Real "found vs total" progress tracking?** Would need DataStore persistence per (spotId, artifactId). Deferred — current UI's `n/n` is a placeholder. Don't add a "marked discovered" semantic here without UX guidance.
- **CSV column name for `question`**: assumed to be `為甚麼要看?（中）`/`為甚麼要看?（英）` based on inspection. If the column header actually differs, generator's column-binding fails loudly; implementing PR fixes.
- **Should label-absent questions log a warning?** `computeHighlight` returns empty highlight silently. A `Log.w` line at VM init might help designers spot copy errors. Marginal value; defer.
- **`SpotDetailScreen` and `SpotExploreScreen` orphan status**: SpotDetail still reads MockData and uses `selectedLanguage`; SpotExplore has no entryProvider case. Both are deferred — owner TBD. Worth flagging to the umbrella for follow-up scoping. **Not** this change's responsibility.
