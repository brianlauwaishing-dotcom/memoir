## ADDED Requirements

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

`data/tainan-route/_assets.json` SHALL bind a `heroImage` drawable name to every route id present in `index.json.routes`, and a `{ heroImage, photographyTipImages[], artifactImages[] }` block to every spot id present in `index.json.spots`. The generator MUST exit non-zero if any binding is missing, naming the offending ids.

#### Scenario: Generator detects an unbound spot id
- **WHEN** a designer adds a new spot to `tainan_routes.csv`, the generator creates `spots/new_spot.json` and appends `new_spot` to `index.json.spots`, but `_assets.json` has no entry under that key
- **THEN** the generator prints `missing _assets.json binding: spots.new_spot` to stderr and exits with a non-zero status

#### Scenario: All ids are bound
- **WHEN** every entry in `index.json.routes` and `index.json.spots` has a corresponding key in `_assets.json` whose drawable names resolve via `Resources.getIdentifier`
- **THEN** the generator exits zero and `ContentValidationTest` passes

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
