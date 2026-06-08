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

### Requirement: `_assets.json` schema for artifacts is keyed by artifact id

The `_assets.json` per-spot artifact section SHALL be structured as `{ "artifacts": { "<artifactId>": { "image": "<drawable>", "galleryImage": "<drawable>?" } } }` — a map keyed by stringified artifact id, with each entry containing `image` (required) and `galleryImage` (optional). This replaces any earlier flat-list shape (`"artifactImages": ["...", "..."]`).

#### Scenario: _assets.json declares both image fields per artifact
- **WHEN** `_assets.json` contains `"spots": { "grand_mazu": { "artifacts": { "1": { "image": "dragon_pillar", "galleryImage": "eg1" } } } }`
- **THEN** the generator emits the spot JSON's artifact id 1 with `image == "dragon_pillar"` and `galleryImage == "eg1"`

#### Scenario: Generator errors on missing image binding
- **WHEN** a spot's artifact id is missing from `_assets.json` artifacts map (no `image` binding)
- **THEN** the generator exits non-zero with `missing _assets.json binding: spots.<spotId>.artifacts.<id>.image`
