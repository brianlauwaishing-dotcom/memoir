## Purpose

Define the source, generation, validation, packaging, and runtime access contract for Tainan route content.
## Requirements
### Requirement: Designer-editable CSV is the canonical content source

The `data/tainan_routes.csv` file at the repo root SHALL be the single canonical source for Tainan-route content. Generated JSON files under `data/tainan-route/` MUST NOT be hand-edited; the only hand-edited side input is `data/tainan-route/_assets.json`, which binds spot/route ids to drawable resource names.

#### Scenario: Designer adds a new spot row to the CSV
- **WHEN** a designer appends a new row under an existing `Route X` section header in `data/tainan_routes.csv` and runs `python data/scripts/generate_content.py`
- **THEN** a new `data/tainan-route/spots/<slugified-en-name>.json` is created, the new spot id is appended to `data/tainan-route/index.json` `spots` list in id-sorted order, and the corresponding route's `journey` array gains an entry for the new spot

#### Scenario: Designer hand-edits a generated JSON file
- **WHEN** a designer modifies `data/tainan-route/spots/grand_mazu.json` directly and later runs the generator
- **THEN** the generator overwrites the hand-edits with the CSV-derived content, and `data/tainan-route/README.md` documents this overwrite behavior so the designer is warned in advance

### Requirement: Generator is deterministic across machines and re-runs

`python data/scripts/generate_content.py` SHALL produce byte-identical output across runs on the same input, across Windows / macOS / Linux, and across local laptops / GitHub Actions runners. Determinism is enforced by: sorted-keys JSON, id-sorted route/spot iteration, LF line endings, UTF-8 (no BOM), trailing newline, and no embedded timestamps or generator version stamps.

#### Scenario: Running the generator twice on unchanged input
- **WHEN** `python data/scripts/generate_content.py` is invoked twice in succession with no CSV change between runs
- **THEN** `git status` reports zero changes under `data/tainan-route/` after the second run

#### Scenario: Running the generator on Linux vs Windows
- **WHEN** the same `data/tainan_routes.csv` is processed on a Windows worktree and on an Ubuntu CI runner, AND `.gitattributes` already enforces `text eol=lf` for `data/tainan_routes.csv` and `data/tainan-route/**/*.json` (precondition: the `.gitattributes` rule MUST land in the same commit as the first generated JSON so Windows `core.autocrlf` does not mutate checked-out files before the first determinism check)
- **THEN** both runs produce identical `data/tainan-route/**/*.json` byte-for-byte (validated via `git diff --exit-code`)

### Requirement: CI enforces CSV ↔ JSON sync

A GitHub Actions job step SHALL regenerate JSON from CSV and fail the build if the regenerated files differ from the committed files.

#### Scenario: Developer commits a CSV edit without regenerating JSON
- **WHEN** a pull request modifies `data/tainan_routes.csv` but does not update the corresponding `data/tainan-route/**/*.json` files
- **THEN** CI runs `python data/scripts/generate_content.py && git diff --exit-code data/tainan-route/`, the diff is non-empty, the job exits non-zero, and the PR is blocked

#### Scenario: Developer regenerates and commits JSON alongside CSV edit
- **WHEN** a pull request modifies `data/tainan_routes.csv` AND commits the regenerated `data/tainan-route/**/*.json`
- **THEN** the CI sync-check step exits zero

### Requirement: JSON assets are bundled into the APK at a stable path

The Android build SHALL bundle `data/tainan-route/**` from the repo root into the APK such that they are reachable at runtime via `AssetManager.open("tainan-route/<relative-path>")`. The bundling MUST use Gradle `sourceSets["main"].assets.srcDirs(...)` so no copy task is required.

#### Scenario: Asset is reachable from app code
- **WHEN** an instrumented test calls `context.assets.open("tainan-route/index.json")`
- **THEN** an `InputStream` is returned containing the same bytes as `data/tainan-route/index.json` at the repo root

#### Scenario: A newly added spot JSON is readable from the APK
- **WHEN** a fresh `data/tainan-route/spots/new_spot.json` file is added to the worktree and a new debug APK is assembled
- **THEN** an instrumented test running on that APK reads the new file via `context.assets.open("tainan-route/spots/new_spot.json")` and observes the expected bytes — without any change to `app/build.gradle.kts`

### Requirement: All user-facing content text is bilingual

Every user-facing string field in route and spot JSON SHALL be modeled as `{"en": "...", "zh": "..."}` and deserialize to a `LocalizedText` Kotlin type that exposes `operator fun get(locale: Locale): String` returning the zh string when `locale.language == "zh"` and the en string otherwise.

#### Scenario: Locale resolution for Traditional Chinese
- **WHEN** Kotlin code evaluates `route.title[Locale("zh", "TW")]` against a route whose JSON title is `{"en": "Sounds of Temple", "zh": "台南廟宇聲音路線"}`
- **THEN** the returned value is `"台南廟宇聲音路線"`

#### Scenario: Locale resolution for English fallback
- **WHEN** Kotlin code evaluates `spot.whyItMatters[Locale.ENGLISH]` against a spot whose JSON has both `en` and `zh` populated
- **THEN** the returned value is the `en` string

#### Scenario: A required bilingual field is missing the zh half
- **WHEN** the generator emits a spot JSON in which `title.zh` is the empty string because the CSV cell was blank
- **THEN** kotlinx-serialization deserializes the field as `LocalizedText(en = "...", zh = "")` without throwing, and `ContentValidationTest` does NOT fail (empty strings are a tolerated content state during designer authoring; surfacing them as a hard failure is a separate quality-bar concern owned by a future `home-discovery` content-completeness gate)

### Requirement: Drawable references in JSON resolve to non-zero resource ids

Every `heroImage`, `photographyTips[].image`, and `artifacts[].image` field in route/spot JSON SHALL contain a drawable resource name (no file extension) that resolves to a non-zero id via `Resources.getIdentifier(name, "drawable", context.packageName)`.

#### Scenario: Build-time test catches a typo'd drawable name
- **WHEN** `data/tainan-route/_assets.json` is edited to set a spot's `heroImage` to `"grand_mazu_temppppple"` (typo) and `:app:testDebugUnitTest` is run
- **THEN** `ContentValidationTest` fails with an assertion message naming the offending spot id and the unresolved drawable name

#### Scenario: All committed drawable references resolve
- **WHEN** `ContentValidationTest` runs against the committed `data/tainan-route/**/*.json`
- **THEN** every drawable name resolves to a non-zero id and the test passes

### Requirement: Index file enumerates all route and spot ids

`data/tainan-route/index.json` SHALL list every route id present under `routes/` and every spot id present under `spots/`, with no extras and no omissions, both lists in ASCII-sorted order.

#### Scenario: A spot file exists but is missing from index
- **WHEN** the generator omits a spot id from `index.json.spots` while `data/tainan-route/spots/<id>.json` exists
- **THEN** `ContentValidationTest` fails citing the orphan file

#### Scenario: Index lists a spot id that has no file
- **WHEN** `index.json.spots` contains an id with no corresponding file under `data/tainan-route/spots/`
- **THEN** `ContentAssetLoader.load()` throws a parse exception naming the missing file, and `ContentValidationTest` fails before runtime

### Requirement: Route journeys reference existing spots

Every entry in a route's `journey` array SHALL reference a `spotId` that exists in `data/tainan-route/index.json` `spots`.

#### Scenario: Route references a non-existent spot
- **WHEN** a route's `journey` contains `{"order": 1, "spotId": "ghost_spot"}` and `"ghost_spot"` is not in `index.json.spots`
- **THEN** `ContentValidationTest` fails with a message naming the offending route id and the dangling spot id

#### Scenario: Route journey contains duplicate order numbers
- **WHEN** a route's `journey` contains two entries with the same `order` value
- **THEN** `ContentValidationTest` fails citing the route id and the duplicate order

#### Scenario: Route journey order numbers have gaps
- **WHEN** a route's `journey` contains orders `[1, 2, 5]` with no entries for `3` or `4`
- **THEN** `ContentValidationTest` passes — gaps are tolerated so designers can renumber spots without forcing a full sequence rewrite

### Requirement: All ids are unique within their kind

No two route JSON files SHALL share the same `id` field value. No two spot JSON files SHALL share the same `id` field value. The `id` in each file MUST equal the file's basename (without `.json` extension).

#### Scenario: Two route files declare the same id
- **WHEN** `routes/route_a.json` and `routes/route_b.json` both contain `"id": "sounds_of_temple"`
- **THEN** `ContentAssetLoader.load()` throws an exception, and `ContentValidationTest` fails citing both file paths

#### Scenario: A file's id does not match its filename
- **WHEN** `spots/grand_mazu.json` contains `"id": "grand_mazuu"` (typo)
- **THEN** `ContentValidationTest` fails citing the mismatch between filename and id field

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

### Requirement: ContentRepository serves routes and spots by id

`com.mcis.memoir.data.content.ContentRepository` SHALL expose:
- `fun routes(): Flow<List<Route>>` — emits the full route list exactly once then completes
- `suspend fun route(id: String): Route?` — returns the route with the matching id or `null`
- `suspend fun spot(id: String): Spot?` — returns the spot with the matching id or `null`

#### Scenario: Looking up an existing route by id
- **WHEN** a test instantiates `ContentRepository` with a `ContentAssetLoader` that returns a `ContentSnapshot` containing route `"sounds_of_temple"` and calls `runBlocking { repo.route("sounds_of_temple") }`
- **THEN** the call returns a non-null `Route` whose `id` equals `"sounds_of_temple"`

#### Scenario: Looking up an unknown id
- **WHEN** the same repository is queried with `repo.route("does_not_exist")`
- **THEN** the call returns `null` (no exception)

#### Scenario: Collecting the routes Flow
- **WHEN** a test collects `repo.routes()` via `runTest { repo.routes().first() }`
- **THEN** exactly one list is emitted containing every route in the snapshot in the same order as `index.json.routes` (which is ASCII-sorted by id per the index requirement), then the Flow completes

### Requirement: Content is loaded once per process and cached

`ContentRepository` SHALL invoke `ContentAssetLoader.load()` at most once per process lifetime, regardless of how many lookups or `routes()` collections occur after construction. Loading runs on `Dispatchers.IO`.

#### Scenario: Multiple lookups trigger a single load
- **WHEN** a test calls `repo.route("a")`, `repo.spot("b")`, and `repo.routes().first()` in sequence against a `ContentRepository` instance constructed with a spy/counting `ContentAssetLoader`
- **THEN** `ContentAssetLoader.load()` is invoked exactly one time

#### Scenario: Loader exception surfaces to every consumer
- **WHEN** `ContentAssetLoader.load()` throws (e.g. a corrupt JSON file in assets) and two callers invoke `repo.route("any")` AND `repo.routes().first()` after construction
- **THEN** both callers receive the original thrown exception via the public API (`route(id)` rethrows; `routes()` collection rethrows inside the collector) — neither call silently returns `null` or an empty list

### Requirement: Existing MockData consumers continue to compile

Landing this change MUST NOT modify or delete `com.mcis.memoir.data.MockData`, `com.mcis.memoir.data.RouteData`, `com.mcis.memoir.data.SpotData`, `com.mcis.memoir.data.PreferenceManager`, or any screen file under `com.mcis.memoir.*`. The new `com.mcis.memoir.data.content.*` package SHALL exist alongside `com.mcis.memoir.data.MockData` without conflict.

#### Scenario: Building the app after this change lands
- **WHEN** `./gradlew :app:assembleDebug` is run against the worktree immediately after this change is merged
- **THEN** the build succeeds, every existing screen still renders MockData-backed content, and no `data.MockData` reference has been removed or renamed

#### Scenario: Importing both old and new layers in the same file
- **WHEN** a Kotlin file imports both `com.mcis.memoir.data.MockData` and `com.mcis.memoir.data.content.ContentRepository`
- **THEN** compilation succeeds with no package collision or duplicate-class error

### Requirement: Routes declare filter tags from the canonical tag id set

Every route JSON under `data/tainan-route/routes/` SHALL declare a top-level `"tags"` field as a non-empty array of strings. Each entry MUST be a member of the canonical tag id set. The canonical set is defined once in Kotlin source as `com.mcis.memoir.ui.home.TagCatalog.ids`, and mirrored as a hard-coded constant inside `data/scripts/generate_content.py` (with mutual cross-reference comments in both files). The Python generator MUST exit non-zero if any route violates the rule against its own copy of the set; a new JVM unit test `HomeContentTagValidationTest` (owned by this change, NOT an edit to change #1's `ContentValidationTest.kt`) MUST fail if any committed route JSON violates the rule against `TagCatalog.ids`; together these two checks catch both editor-side and code-side drift.

#### Scenario: Route declares an empty tags array
- **WHEN** a route JSON has `"tags": []`
- **THEN** the generator exits non-zero AND `ContentValidationTest` fails, both citing the route id and the rule "every route must declare at least one tag from TagCatalog.ids"

#### Scenario: Route omits the tags field entirely
- **WHEN** a route JSON has no `"tags"` field
- **THEN** kotlinx-serialization deserialization uses the default empty list (per `Route.tags: List<String> = emptyList()` in change #1's model), AND `ContentValidationTest` fails citing the missing-or-empty `tags` rule

#### Scenario: Route declares an unknown tag
- **WHEN** a route JSON has `"tags": ["temples", "moon-cult"]` and `"moon-cult"` is not in the canonical set
- **THEN** the generator exits non-zero (caught by the generator's hard-coded mirror set) AND `HomeContentTagValidationTest` fails (caught by `TagCatalog.ids`), both citing the route id and the unknown tag id

#### Scenario: All committed routes have valid tags
- **WHEN** `HomeContentTagValidationTest` runs against the committed `data/tainan-route/routes/*.json`
- **THEN** every route's `tags` array is non-empty AND every tag id appears in `TagCatalog.ids` AND the test passes

#### Scenario: Generator mirror drifts from TagCatalog
- **WHEN** the generator's hard-coded tag id mirror set differs from `TagCatalog.ids` (e.g. one side adds `"food"` and the other doesn't)
- **THEN** at least one of these failures occurs on the next CI run: (a) the generator accepts a route id the JVM test then rejects, OR (b) the JVM test accepts a route id the generator then rejects on regeneration — either way the drift is caught before merge

### Requirement: Generator side input declares route tags

The content generator SHALL source each route's tag list from either (a) a new `tags` column in `data/tainan_routes.csv` (pipe-separated) OR (b) a committed `data/tainan-route/_tags.json` side input keyed by route id. The implementation MAY pick either path; the chosen path MUST be documented in `data/scripts/README.md`.

#### Scenario: CSV-column variant
- **WHEN** the generator chooses the CSV-column path, the CSV contains a `tags` column with `"temples|folk-belief"` for one route, and the generator runs
- **THEN** the corresponding `routes/<id>.json` contains `"tags": ["temples", "folk-belief"]` (split on `|` or `｜`, trimmed, deduped, in source order)

#### Scenario: Side-input variant
- **WHEN** the generator chooses the `_tags.json` path, the file contains `{"sounds_of_temple": ["temples"]}` for one route, and the generator runs
- **THEN** the corresponding `routes/sounds_of_temple.json` contains `"tags": ["temples"]`

#### Scenario: Determinism preserved
- **WHEN** the generator runs twice against unchanged inputs (CSV or side-input + `_assets.json`)
- **THEN** `git diff --exit-code data/tainan-route/` reports zero changes after the second run (the tag-emission step does not introduce non-determinism)

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

