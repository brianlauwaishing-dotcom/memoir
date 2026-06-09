# artifact-discovery-flow Specification

## Purpose
TBD - created by archiving change artifact-discovery-flow. Update Purpose after archive.
## Requirements
### Requirement: SpotIntroViewModel renders spot + artifacts from ContentRepository

`com.mcis.memoir.ui.spot.SpotIntroViewModel` SHALL expose `state: StateFlow<SpotIntroState>` whose value is computed by looking up `ContentRepository.spot(spotId)` exactly once on init and resolving locale-dependent text via the injected `localeProvider()` and drawable names via `Resources.getIdentifier(name, "drawable", "com.mcis.memoir")`.

#### Scenario: Spot found, state carries pre-resolved fields
- **WHEN** the VM is constructed with `spotId = "grand_mazu"` against a `ContentRepository` whose `spot("grand_mazu")` returns a spot with 3 artifacts and `localeProvider = { Locale.ENGLISH }`
- **THEN** `state.first().isLoading == false`, `state.first().title` equals the English spot title, `state.first().discoveryItems.size == 3`, AND every `DiscoveryItemCard.label` equals the English artifact title

#### Scenario: Spot not found, error surfaces in state
- **WHEN** the VM is constructed with `spotId = "ghost"` and `ContentRepository.spot("ghost")` returns `null`
- **THEN** `state.first().isLoading == false`, `state.first().error == "spot_not_found"`, AND `state.first().discoveryItems.isEmpty()`

#### Scenario: Locale switch under VM reuse is not observed
- **WHEN** the VM is constructed with `localeProvider = { Locale.ENGLISH }` and the test later changes the locale value the provider returns
- **THEN** the already-collected `state` is unchanged (locale is captured once on init; Activity recreation rebuilds the VM with a fresh locale per change #2's reconciliation model)

### Requirement: ArtifactDiscoveryViewModel pre-computes the question highlight

`ArtifactDiscoveryViewModel` SHALL compute a `QuestionHighlight(prefix, label, suffix)` triple from the locale-resolved `question` text and the locale-resolved artifact `title` via the `computeHighlight(question, label)` pure function. The Composable MUST NOT call `String.split` or `String.indexOf` itself — the VM owns the parse.

#### Scenario: Label appears inside question, highlight splits it three ways
- **WHEN** the VM is constructed for an artifact whose label is `"龍柱"` and question is `"龍柱上有幾條龍呢？"` under `Locale("zh")`
- **THEN** `state.first().highlight == QuestionHighlight(prefix = "", label = "龍柱", suffix = "上有幾條龍呢？")`

#### Scenario: Label absent from question, highlight is empty
- **WHEN** the question is `"How many?"` and the label is `"Dragon"` (label not in question)
- **THEN** `state.first().highlight == QuestionHighlight(prefix = "How many?", label = "", suffix = "")` — the screen renders the full question with no highlight

#### Scenario: artifactId / displayPosition / totalArtifacts reflect spot.artifacts
- **WHEN** the VM is constructed with `spotId = "grand_mazu"` (3 artifacts) and `artifactId = 2`
- **THEN** `state.first().artifactId == 2` (stable id, matches navigation argument) AND `state.first().displayPosition == 2` (1-based slot in `spot.artifacts`) AND `state.first().totalArtifacts == 3`

#### Scenario: artifactId and displayPosition diverge when ids are non-sequential
- **WHEN** a hypothetical spot has artifacts with ids `[5, 7, 11]` and the VM is constructed with `artifactId = 11`
- **THEN** `state.first().artifactId == 11` AND `state.first().displayPosition == 3` (it's the 3rd artifact in the list) — the More button's `onClick` MUST pass `artifactId` (11) for navigation, NOT `displayPosition` (3)

#### Scenario: Artifact not found, error surfaces in state
- **WHEN** the VM is constructed with `artifactId = 99` and no matching artifact exists on the spot
- **THEN** `state.first().isLoading == false` AND `state.first().error == "artifact_not_found"`

### Requirement: ArtifactDetailViewModel renders storytelling text

`ArtifactDetailViewModel` SHALL expose `state: StateFlow<ArtifactDetailState>` whose `description` field equals the locale-resolved `Artifact.description` text and whose `label` equals the locale-resolved `Artifact.title`.

#### Scenario: Description resolves to the locale's storytelling text
- **WHEN** the VM is constructed under `Locale.ENGLISH` against an artifact whose `description` is `LocalizedText("Long English story...", "長中文故事...")`
- **THEN** `state.first().description == "Long English story..."`

#### Scenario: Drawable resolves to the artifact image
- **WHEN** the VM is constructed against an artifact whose `image == "dragon_pillar"` and `R.drawable.dragon_pillar` exists
- **THEN** `state.first().imageDrawableRes != 0`

### Requirement: QuestionHighlight is a pure-function utility

`com.mcis.memoir.ui.artifact.QuestionHighlight` SHALL be a `data class(prefix: String, label: String, suffix: String)`. The function `computeHighlight(question: String, label: String): QuestionHighlight` SHALL be pure (no dependencies on Android framework / coroutines / Compose), idempotent, and testable as plain Kotlin.

#### Scenario: Label found at start
- **WHEN** `computeHighlight("Dragon: how many?", "Dragon")` is invoked
- **THEN** the result is `QuestionHighlight(prefix = "", label = "Dragon", suffix = ": how many?")`

#### Scenario: Label found at end
- **WHEN** `computeHighlight("How many Dragon", "Dragon")` is invoked
- **THEN** the result is `QuestionHighlight(prefix = "How many ", label = "Dragon", suffix = "")`

#### Scenario: Label appears multiple times — only first occurrence highlighted
- **WHEN** `computeHighlight("Dragon and Dragon", "Dragon")` is invoked
- **THEN** the result is `QuestionHighlight(prefix = "", label = "Dragon", suffix = " and Dragon")`

#### Scenario: Empty label yields no highlight
- **WHEN** `computeHighlight("anything", "")` is invoked
- **THEN** the result is `QuestionHighlight(prefix = "anything", label = "", suffix = "")` — the screen renders the question with no highlight

### Requirement: Three artifact-discovery screens render from state without legacy parameters

`SpotIntroScreen`, `ArtifactDiscoveryScreen`, and `ArtifactDetailScreen` SHALL NOT declare `selectedLanguage: String` parameters. They SHALL NOT call `stringResource(R.string.X_zh)` or use the runtime `getResourceEntryName` + `_zh`-suffix trick. All chrome text MUST be accessed via `stringResource(R.string.X)`.

#### Scenario: SpotIntroScreen signature is migrated
- **WHEN** the source of `SpotIntroScreen.kt` is inspected after this change lands
- **THEN** the function signature has no `selectedLanguage` and no `spotId` parameter, AND `grep -nE 'R\.string\.\w+_zh' SpotIntroScreen.kt` returns zero matches, AND the file does not import `com.mcis.memoir.data.MockData`

#### Scenario: ArtifactDiscoveryScreen signature is migrated
- **WHEN** the source of `ArtifactDiscoveryScreen.kt` is inspected after this change lands
- **THEN** the function signature has no `selectedLanguage`, `spotId`, or `artifactId` parameter, AND `grep -nE 'R\.string\.\w+_zh' ArtifactDiscoveryScreen.kt` returns zero matches, AND the file does not import `com.mcis.memoir.data.MockData`

#### Scenario: ArtifactDetailScreen signature is migrated
- **WHEN** the source of `ArtifactDetailScreen.kt` is inspected after this change lands
- **THEN** the function signature has no `selectedLanguage`, `spotId`, or `artifactId` parameter, AND `grep -nE 'R\.string\.\w+_zh' ArtifactDetailScreen.kt` returns zero matches, AND the file does not import `com.mcis.memoir.data.MockData`

### Requirement: ArtifactDiscoveryScreen renders the highlight in maroon

`ArtifactDiscoveryScreen` SHALL render the question text as an `AnnotatedString` built from `state.highlight.prefix` + `state.highlight.label` styled with `SpanStyle(color = maroon)` + `state.highlight.suffix`. When `state.highlight.label.isEmpty()`, only the prefix is rendered (no highlighted span).

#### Scenario: Highlighted span uses maroon color
- **WHEN** a Compose test renders `ArtifactDiscoveryScreen` against a state whose `highlight.label == "龍柱"` and inspects the rendered `AnnotatedString`'s span styles
- **THEN** exactly one span exists for the range corresponding to `"龍柱"`, AND its `SpanStyle.color` equals the project's maroon color (`Color(0xFFBF1B20)` as documented in the existing `ArtifactDiscoveryScreen.kt:166`)

#### Scenario: Empty highlight renders plain text
- **WHEN** the screen renders a state whose `highlight.label.isEmpty()`
- **THEN** the rendered question text contains zero `SpanStyle` color overrides

### Requirement: MyAppNavigation constructs the three VMs via factories

`MyAppNavigation` SHALL construct `SpotIntroViewModel`, `ArtifactDiscoveryViewModel`, and `ArtifactDetailViewModel` via `viewModel(key = …, factory = …)` blocks in their respective destination entries. The `selectedLanguage = selectedLanguage` parameter threading MUST be removed from those three entries. `spotId` (and `artifactId` where applicable) MUST be sourced from the destination key, not from the navigator-level state.

#### Scenario: MyAppNavigation has no selectedLanguage thread to artifact screens
- **WHEN** the source of `MyAppNavigation.kt` is inspected after this change lands
- **THEN** the `SpotIntroDestination`, `ArtifactDiscoveryDestination`, and `ArtifactDetailDestination` entry blocks contain no `selectedLanguage = selectedLanguage` assignment, AND each constructs its respective ViewModel via a Factory

#### Scenario: viewModel key includes navigation arguments
- **WHEN** the `SpotIntroDestination` entry constructs the VM
- **THEN** the `viewModel(key = key.spotId, factory = ...)` call uses the spotId as the key, so revisiting the same destination key reuses the same VM instance

<!-- Cross-capability deltas live in ../content-pipeline/spec.md per openspec convention (one spec file per capability). -->

