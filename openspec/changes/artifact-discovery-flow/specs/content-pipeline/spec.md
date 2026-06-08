## ADDED Requirements

### Requirement: Artifact schema carries question and optional galleryImage

Each entry in `Spot.artifacts` (JSON `spots/<id>.json`) SHALL declare:
- `id: Int` — stable artifact id within the spot
- `title: LocalizedText` — short label (e.g. "Dragon Pillar" / "龍柱"), used both for the discovery card and for highlighting inside the question
- `description: LocalizedText` — long storytelling text shown on `ArtifactDetailScreen`
- `question: LocalizedText` — discovery prompt shown on `ArtifactDiscoveryScreen` (e.g. "How many dragons are on the pillars?" / "龍柱上有幾條龍呢？")
- `image: String` — drawable resource name for the primary artifact image
- `galleryImage: String?` — OPTIONAL drawable resource name for a secondary illustration (e.g. a sketch)

The Kotlin model `com.mcis.memoir.data.content.model.Artifact` MUST mirror this schema. `question` is required; `galleryImage` is nullable with `default = null`. This deviates from umbrella §4.4's earlier minimal `Artifact` shape — the deviation is intentional and recorded in the umbrella spec.

#### Scenario: Artifact JSON omits the question field
- **WHEN** a `spots/<id>.json` contains an artifact entry with no `question` field
- **THEN** kotlinx-serialization deserialization throws `MissingFieldException`, AND the generator (which validates before writing) exits non-zero citing the spot id and artifact id

#### Scenario: Artifact JSON has empty question text
- **WHEN** a `spots/<id>.json` contains an artifact entry with `question == {"en": "", "zh": ""}`
- **THEN** the generator exits non-zero with `empty question for spots.<id>.artifacts[<n>]`, AND `ArtifactSchemaValidationTest` fails the build before any APK ships

#### Scenario: Artifact JSON omits the optional galleryImage
- **WHEN** a `spots/<id>.json` contains an artifact entry with no `galleryImage` field
- **THEN** deserialization succeeds with `galleryImage = null`, AND `ArtifactSchemaValidationTest` does NOT fail

#### Scenario: Non-null galleryImage drawable resolves
- **WHEN** an artifact has `galleryImage = "eg1"` and `R.drawable.eg1` exists in the app's drawable set
- **THEN** `ArtifactSchemaValidationTest` passes the drawable-resolution check

#### Scenario: Non-null galleryImage with bad drawable name fails the test
- **WHEN** an artifact has `galleryImage = "missing_drawable"` and no matching `R.drawable.missing_drawable` exists
- **THEN** `ArtifactSchemaValidationTest` fails citing the spot id, artifact id, and the unresolved drawable name

### Requirement: Content generator emits question and galleryImage with validation

`data/scripts/generate_content.py` SHALL read artifact question text from designated CSV columns (`為甚麼要看?（中）` / `為甚麼要看?（英）` or equivalents identified at implementation time) and write `question.en` + `question.zh` into each emitted artifact JSON entry. It SHALL read each artifact's optional `galleryImage` from `data/tainan-route/_assets.json` under a per-spot, per-artifact-id binding and emit it only when present.

#### Scenario: Generator errors on missing question text
- **WHEN** the CSV row for an artifact has blank `question_en` or `question_zh`
- **THEN** the generator exits non-zero with `empty question for spots.<spotId>.artifacts[<id>]` and writes no JSON to disk for that spot

#### Scenario: Generator emits artifact with both required and optional fields
- **WHEN** the CSV provides full bilingual question text AND `_assets.json` binds a `galleryImage` for the artifact
- **THEN** the resulting `spots/<id>.json` contains an artifact entry with `question: {en, zh}`, `image`, AND `galleryImage` — all present and non-null

#### Scenario: Generator omits galleryImage field when binding is absent
- **WHEN** `_assets.json` has no `galleryImage` key for an artifact id
- **THEN** the resulting JSON entry has no `galleryImage` field (relies on `explicitNulls = false` in the consumer's `Json` config to deserialize as null)

#### Scenario: Determinism preserved across the new fields
- **WHEN** the generator runs twice against unchanged CSV + `_assets.json`
- **THEN** `git diff --exit-code data/tainan-route/spots/` reports zero changes after the second run

<!-- The `_assets.json covers every route and spot` requirement is MODIFIED by this change; see the MODIFIED block below. -->

## MODIFIED Requirements

### Requirement: `_assets.json` covers every route and spot

`data/tainan-route/_assets.json` SHALL bind a `heroImage` drawable name to every route id present in `index.json.routes`, and a `{ heroImage, photographyTipImages[], artifacts: { "<artifactId>": { "image", "galleryImage"? } } }` block to every spot id present in `index.json.spots`. The per-spot `artifacts` section is a MAP keyed by stringified artifact id (replacing the earlier flat-list shape `artifactImages: [...]` defined in the original `tainan-route-content-pipeline` change); each entry's `image` is REQUIRED and `galleryImage` is OPTIONAL. The generator MUST exit non-zero if any binding is missing (including any artifact whose `image` is absent), naming the offending ids.

#### Scenario: Generator detects an unbound spot id
- **WHEN** a designer adds a new spot to `tainan_routes.csv`, the generator creates `spots/new_spot.json` and appends `new_spot` to `index.json.spots`, but `_assets.json` has no entry under that key
- **THEN** the generator prints `missing _assets.json binding: spots.new_spot` to stderr and exits with a non-zero status

#### Scenario: Generator detects an unbound artifact image
- **WHEN** a spot's `artifacts` map is missing an entry for one of the artifact ids that the CSV produces (or has an entry with no `image` field)
- **THEN** the generator prints `missing _assets.json binding: spots.<spotId>.artifacts.<id>.image` to stderr and exits with a non-zero status

#### Scenario: All ids are bound
- **WHEN** every entry in `index.json.routes` and `index.json.spots` has a corresponding key in `_assets.json` whose drawable names resolve via `Resources.getIdentifier`, AND every artifact id in every spot section has an `image` binding (with optional `galleryImage`)
- **THEN** the generator exits zero and `ContentValidationTest` passes

#### Scenario: _assets.json declares both image fields per artifact
- **WHEN** `_assets.json` contains `"spots": { "grand_mazu": { "artifacts": { "1": { "image": "dragon_pillar", "galleryImage": "eg1" } } } }`
- **THEN** the generator emits the spot JSON's artifact id 1 with `image == "dragon_pillar"` and `galleryImage == "eg1"`

#### Scenario: _assets.json omits the optional galleryImage
- **WHEN** an artifact entry in `_assets.json` has `image` but no `galleryImage` key
- **THEN** the generator succeeds and the emitted artifact JSON has no `galleryImage` field (consumer's `ignoreUnknownKeys = true` / `explicitNulls = false` config handles deserialization to `galleryImage = null`)
