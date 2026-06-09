## ADDED Requirements

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
