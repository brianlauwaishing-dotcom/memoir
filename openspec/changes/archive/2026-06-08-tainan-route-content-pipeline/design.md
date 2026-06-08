## Context

This change implements §4 of the umbrella architecture spec (`docs/superpowers/specs/2026-06-07-mobile-direct-app-wiring-design.md`). It is the foundation for the other 8 changes in the umbrella's build order — until route + spot content lives behind `ContentRepository`, no follow-up change can replace its `MockData` consumer cleanly.

**Current state**:
- Designer source: `data/tainan_routes.csv` (25 KB, UTF-8, Excel-exported, RFC 4180 with multi-line quoted cells, contains section header rows like `Route A 從海港守護到城市信仰` interleaved with data rows).
- Engineer source: `frontend/mobile/app/src/main/java/com/mcis/memoir/data/MockData.kt` (`object MockData { val routes; val spots; val memories; val templates }`). Routes/spots have parallel `…En`/`…Zh` fields and `imageRes = R.drawable.X` references.
- Build config: AGP 9.1.1, Kotlin 2.2.10, Compose BOM 2024.09.00, `gradle/libs.versions.toml` does NOT yet include `kotlin-serialization`, `kotlinx-serialization-json`, or any `assets.srcDirs` override.
- `frontend/mobile/app/build.gradle.kts` lines 6–40 currently has no `sourceSets` block.
- 5 routes × ~3 spots each = ~15 spots; deeply-detailed spot text for the lead spot (`grand_mazu`), placeholder text for the rest. Designer is actively iterating on CSV.

**Constraints**:
- Windows worktree: no symlinks (`mklink` requires admin or developer mode).
- AGP gradle root is `frontend/mobile/`, so the path from `app/build.gradle.kts` up to repo-root `data/` is `../../data`.
- Designer cannot rebuild the app to preview content changes — CSV → JSON generator must run in <1s on a laptop and be invokable without Android tooling.
- `MockData` consumers (HomeScreen, RouteDetailScreen, SpotIntroScreen, SpotDetailScreen, ArtifactDiscoveryScreen, ArtifactDetailScreen, SavedScreen) must keep compiling; this change adds the new layer alongside.

## Goals / Non-Goals

**Goals:**
1. `data/tainan_routes.csv` becomes a tracked, canonical source; a deterministic generator converts it to `data/tainan-route/index.json` + `routes/*.json` + `spots/*.json`.
2. Generated JSON ships inside the APK as `assets/tainan-route/**` via a Gradle `sourceSets` override — no copy task, no symlink.
3. `ContentRepository` exposes `routes(): Flow<List<Route>>`, `suspend fun route(id)`, `suspend fun spot(id)`, fed by `ContentAssetLoader` reading from `AssetManager` once per process.
4. A `ContentValidationTest` proves every drawable name, every `journey[].spotId`, and every JSON file referenced by `index.json` is reachable, so a broken designer edit fails CI before it reaches a build.
5. CI enforces CSV ↔ JSON sync via `git diff --exit-code data/tainan-route/` after re-running the generator.
6. Existing `MockData`-backed screens keep compiling. No consumer migration in this change.

**Non-Goals:**
- Rewriting any screen or ViewModel.
- Wiring Koin / DataStore / Room / Nav3 / LLM.
- Defining `tags.json` or `templates.json` (umbrella does not list them under §4 and follow-up changes own them).
- Localizing JSON file names. Files are keyed by stable English ids only.
- Hot-reload of content at runtime. `ContentRepository`'s `Deferred` cache is process-lifetime.
- Authoring tools / a CMS. Designer edits CSV in Excel/Sheets, exports CSV, commits.

## Decisions

### D1. Asset bundling via `sourceSets["main"].assets.srcDirs`

```kotlin
android {
    sourceSets["main"].assets.srcDirs(
        "src/main/assets",
        rootProject.file("../../data")
    )
}
```

**Why over alternatives:**
- *Copy task pre-`mergeAssets`*: works but is an extra Gradle plugin point; AGP already supports multiple asset roots natively.
- *Symlink `app/src/main/assets/tainan-route` → `data/tainan-route`*: cleanest in repo, breaks on Windows without admin/dev mode.
- *Bundle JSON inside `res/raw`*: forces all-lowercase, alphanumeric, underscore-only file names and bypasses `AssetManager` path semantics — designer-friendly file naming wins here.

`rootProject.file("../../data")` resolves to repo-root `/data/` because `frontend/mobile/settings.gradle.kts` sets the Gradle rootProject to `frontend/mobile/`. After build, `data/tainan-route/index.json` is reachable at `assets/tainan-route/index.json` via `AssetManager.open("tainan-route/index.json")`.

### D2. CSV → JSON generator: Python script under `data/scripts/`

Generator location: `data/scripts/generate_content.py`.

**Why over alternatives:**
- *Gradle task in `buildSrc/`*: tighter integration but designer needs JDK + Gradle just to preview a JSON diff. Python is already on team laptops.
- *Kotlin script (`.kts`)*: same JDK dependency; slower cold start; harder for the designer to read.
- *Pure-shell `awk`/`jq`*: brittle on multi-line quoted CSV cells.

Generator contract:
- Runtime: Python ≥ 3.11. Generator's first action: `if sys.version_info < (3, 11): sys.exit("Python 3.11+ required")`. CI uses `actions/setup-python@v5` pinned to 3.11; `data/scripts/README.md` documents `python3` (Unix/Mac) vs `python` (Windows venv) invocation. A minimal `data/scripts/pyproject.toml` lists no runtime deps (stdlib-only) so the generator runs without `pip install`.
- Input: `data/tainan_routes.csv` opened as `open(path, encoding="utf-8-sig", newline="")` — `newline=""` is required by the `csv` module to correctly assemble multi-line quoted cells (per Python docs). `utf-8-sig` strips an optional BOM. `csv.reader(f)` then handles RFC 4180 quoted multi-line cells natively (no extra `quoting=` kwarg needed; `QUOTE_*` constants are writer-side).
- Side input: `data/tainan-route/_assets.json` — committed, hand-curated map of `spotId → { heroImage, photographyTipImages, artifactImages }` and `routeId → heroImage`. The CSV contains no drawable references; this side file is the only place drawable names are bound to ids. Initial population of `_assets.json` is mechanically derived from `MockData.kt` so designer + engineer agree on a baseline; from then on, `_assets.json` is the source of truth and `MockData.kt` is read-only and slated for deletion by follow-up changes (see Open Questions).
- Output:
  - `data/tainan-route/index.json` — `{ "routes": [..ids..], "spots": [..ids..] }`, lists in id-sorted order.
  - `data/tainan-route/routes/<routeId>.json` — one file per route, route id derived from CSV section header.
  - `data/tainan-route/spots/<spotId>.json` — one file per spot, spot id derived from a stable slugifier on the EN spot name (matches existing `MockData` ids).
- Determinism rules (apply to the JSON writer only, not the CSV reader):
  - JSON serialized with `json.dumps(obj, ensure_ascii=False, indent=2, sort_keys=True)`.
  - Trailing newline at EOF.
  - Output files opened as `open(path, "w", encoding="utf-8", newline="\n")` so Windows `os.linesep` does not creep in.
  - Iteration order over routes/spots = sorted by id ASCII order.
  - No timestamps, no generator-version stamp inside the JSON (would defeat the CI diff check).
- Invocation: `python data/scripts/generate_content.py` from repo root, no arguments. Exits non-zero on parse error, prints unmapped CSV columns, lists spot ids missing from `_assets.json`.

CSV parsing notes baked into the generator:
- Section header rows (e.g., `Route A  從海港守護到城市信仰  /  From Sea Protection to City Beliefs,,,,...`) detected by regex `^Route\s+[A-Z]\b(.*)$` against `row[0]`; the captured tail is then split on `/` with arbitrary surrounding whitespace (`re.split(r"\s*/\s*", tail, maxsplit=1)`). If the split yields fewer than 2 parts, the generator exits non-zero — the designer is expected to keep the `中文 / English` convention. Generator uses the EN half to derive `routeId` via slugifier.
- The first two rows of the CSV are a top-level category banner + the header row (multi-line cell). Generator skips the banner row, then reads the header to bind columns to schema field names.
- "Key Info" column is delimiter-separated → `facts: List<String>`. Split on **either** ASCII `|` (U+007C) **or** full-width `｜` (U+FF5C) — designers use both. Strip whitespace, drop empty. A future column-level escape (e.g. `\|`) is out of scope.
- "Photography Tips" column is `①②③`-prefixed multi-line → split on the circled digit markers (regex `r"[①②③④⑤⑥⑦⑧⑨⑩]\s*"`), strip leading marker + spaces, fall back to splitting on blank lines if the markers are missing.
- Generator fixtures live at `data/scripts/test/fixtures/` and cover: multi-line quoted cells, both `|` flavors in Key Info, missing `/` separator in section header (expected exit code != 0), missing `①` markers (fallback path).

### D3. Images stay in `res/drawable/`, referenced by drawable name

JSON `heroImage` / `photographyTips[].image` / `artifacts[].image` are **drawable resource names without extension** (e.g., `"grand_mazu_temple"`, not `"grand_mazu_temple.png"` or `"R.drawable.grand_mazu_temple"`). Loader resolves at use site via `Resources.getIdentifier(name, "drawable", context.packageName)`.

**Why:**
- Reuses the existing PNG drawable pipeline (density buckets, AGP optimization, R-class).
- Moving images into `assets/` would lose density-aware loading and require Coil/Glide to do the right thing manually.
- Validation test catches typos at test time.

**Cost:** `Resources.getIdentifier` is documented as "discouraged" by Android for hot paths. We hit it at most once per spot/route during cold start; tolerable.

**R8 / resource shrinking caveat:** dynamic drawable lookup by name is invisible to R8, so once a future change enables `isMinifyEnabled = true` + `shrinkResources = true` for release, the referenced drawables would be stripped. The `release` `buildTypes` block in `app/build.gradle.kts` currently sets `isMinifyEnabled = false`, so this is not a today-problem. This change adds `app/src/main/res/raw/keep.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources xmlns:tools="http://schemas.android.com/tools"
    tools:keep="@drawable/*" />
```

so that any future enabling of shrinking does not silently break content rendering. Cheap insurance; doesn't grow the APK while minify is off, and the file lives in `res/raw/` (the AGP-documented location) with the correct `tools` namespace declaration.

### D4. `ContentRepository` as a plain class with a `Deferred<ContentSnapshot>` cache

`ContentAssetLoader` is declared as an **interface** (`interface ContentAssetLoader { suspend fun load(): ContentSnapshot }`) so tests can substitute a hand-written fake without `open class` boilerplate or a mocking library. The production implementation is `AssetManagerContentLoader(assets, ContentJson)`.

```kotlin
class ContentRepository(
    private val loader: ContentAssetLoader,
    private val scope: CoroutineScope
) {
    // Eager async to match umbrella §4.5 reference implementation — load starts as
    // soon as ContentRepository is constructed (Application.onCreate), runs on
    // Dispatchers.IO, and consumers await() the cached result.
    private val snapshot: Deferred<ContentSnapshot> =
        scope.async(Dispatchers.IO) { loader.load() }

    fun routes(): Flow<List<Route>> = flow { emit(snapshot.await().routes.values.toList()) }
    suspend fun route(id: String): Route? = snapshot.await().routes[id]
    suspend fun spot(id: String): Spot?   = snapshot.await().spots[id]
}
```

**Why over alternatives:**
- *Eager blocking load at `Application.onCreate`*: blocks main thread; rejected.
- *`lazy { runBlocking { ... } }`*: trades async correctness for laziness; not coroutine-friendly.
- *`MutableStateFlow<ContentSnapshot?>`*: encourages refresh semantics, but content is immutable for process lifetime — `Deferred` better models "load once, await many".
- *`CoroutineStart.LAZY`*: tempting (don't load before Splash needs it), but diverges from umbrella §4.5 and saves only ~50–100 ms of work that we'd want done before HomeScreen anyway. If cold-start telemetry later shows the load competing with first-frame, switch to LAZY and document the divergence in a follow-up change.

**Instantiation in this change (staging shortcut, explicitly temporary):** `MemoirApplication` gains a `companion object { lateinit var content: ContentRepository }`, initialized in `onCreate`. This is a deliberate global the Koin change (umbrella build order item 0→1) will rip out: the Koin change replaces both the `companion object` field AND every `MemoirApplication.content` call site with `koinInject<ContentRepository>()`. The shortcut is justified because:
1. We cannot wait on the Koin change — it isn't scoped yet and `tainan-route-content-pipeline` is the foundation everything else builds on.
2. Adding a half-baked DI container "just for one binding" is more churn than a `lateinit` field.
3. The removal is mechanical and covered by the future Koin change's own tests.

This staging shortcut is recorded in this change's `tasks.md` so the Koin change owner sees it as a follow-up obligation.

### D5. `index.json` as the source of file listing, not `AssetManager.list`

`AssetManager.list("tainan-route/routes")` works on Android but:
- The generator can validate completeness server-side (every listed id has a JSON file; every JSON file is listed).
- The validation test can assert one canonical list.
- It maps cleanly to a future migration where one mega `content.json` replaces the per-id files.

### D6. `Artifact[]` and `PhotographyTip[]` embedded inside `Spot` JSON

Per umbrella §4.2 schema. Trade-off: spot JSONs can be 2–5 KB. Acceptable; designer edits each spot in one CSV row.

### D7. Tags in routes are opaque string ids

`Route.tags: List<String> = emptyList()` for now. A `tags.json` declaring tag display labels + colors is a `home-discovery` concern; this change does not need it. The generator pulls tags from the route's "category (EN)" + future explicit tag columns; for the current CSV the tag list is the lowercased, slugified category.

### D8. `MockData` coexistence: do not delete, do not modify

Existing screens keep importing `data.MockData`, `data.RouteData`, `data.SpotData`. The new content layer lives under `data.content.*` with no overlap. Follow-up changes (`home-discovery`, `route-bookmarking`, etc.) migrate their respective consumer and, once every `MockData` reference is gone, delete `MockData.kt`. This change does not even rewrite a single import.

### D9. Library catalog additions

`gradle/libs.versions.toml` gains:
```toml
[versions]
kotlinxSerialization = "1.7.3"   # confirm latest matching Kotlin 2.2.10 at implementation time

[libraries]
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinxSerialization" }

[plugins]
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

`app/build.gradle.kts` applies `alias(libs.plugins.kotlin.serialization)` and adds `implementation(libs.kotlinx.serialization.json)`. No Koin / DataStore / Room / coroutine-test catalog additions — those belong to their respective umbrella changes.

### D10. JSON parser configuration

```kotlin
val ContentJson: Json = Json {
    ignoreUnknownKeys = true   // designer-added experimental fields don't crash older builds
    explicitNulls = false      // omit nulls from output; tolerant on input
    prettyPrint = false        // production parse path; generator produces pretty JSON separately
}
```

### D11. Test stack: keep current JUnit4 for this change, no Robolectric

`libs.versions.toml` already pins `junit = "4.13.2"`. Adding JUnit5 + MockK + Turbine + Robolectric is the umbrella's longer-term plan but **out of scope** for this change. Concrete justification per test:
- `ContentAssetLoaderTest` — parses JSON fixtures via `ContentJson.decodeFromString<Route>(...)` / `<Spot>(...)`. Pure JUnit4. The id-vs-filename validation is extracted from `AssetManagerContentLoader` as `internal fun validateIdsMatchFilenames(...)` so it can be unit-tested without instantiating an `AssetManager`.
- `ContentValidationTest` — reads JSON files directly from the repo-root `data/tainan-route/` path on the JVM test classpath (no `AssetManager`, no `Resources`, no Robolectric), walks references, asserts ids unique + filename↔id equality + journey spot-refs + journey duplicate-order. For the drawable-name resolution requirement, the test reads the **`R.drawable` class via Kotlin reflection** (`R.drawable::class.java.fields.map { it.name }.toSet()`) and asserts every drawable name in `_assets.json` is in that set. AGP's `processDebugUnitTestResources` puts the generated `R` class on the JVM test classpath; this works without `Resources` or Robolectric and is significantly faster than a Robolectric run.
- `ContentRepositoryTest` — `routes(): Flow` emits once then completes; `runTest { repo.routes().first() }` covers it. `ContentAssetLoader` is declared as an `interface`, so a hand-written fake (`class FakeContentAssetLoader(val snap: ContentSnapshot) : ContentAssetLoader { var loadCount = 0; override suspend fun load() = snap.also { loadCount++ } }`) replaces MockK in five lines. The single-emit Flow shape makes Turbine unnecessary.

If MockK / Turbine / Robolectric become genuinely needed for a later test, we add the minimal catalog entries then; this change ships with `kotlinx-coroutines-test` only.

## Risks / Trade-offs

- **Generator + CI diff drift on Windows vs Linux line endings** → Generator forces LF. `.gitattributes` for `data/tainan-route/**.json` set to `text eol=lf`.
- **Designer edits generated JSON directly, then re-runs generator and loses edits** → Generator's `_assets.json` is the only place to hand-edit drawable bindings; documented in `data/scripts/README.md` (created by this change). An in-file `"_note": "Generated from tainan_routes.csv — do not edit by hand"` marker was considered and rejected — not for determinism reasons (`sort_keys=True` would just place it first), but because it pollutes every one of ~20 spot/route JSONs that humans will skim during review. A single `data/tainan-route/README.md` warning is cleaner.
- **Embedded API key risk** → Out of scope; covered in umbrella §5.3 and `ai-reflection-generation`.
- **CSV column drift** → Generator validates header row matches an expected list of column names; missing/renamed columns exit non-zero with a precise error.
- **Drawable typo lands without test** → `ContentValidationTest` is part of the apply gate. If a referenced drawable does not resolve, the test fails on CI's `:app:testDebugUnitTest`.
- **`Resources.getIdentifier` deprecation rumors** → Documented as discouraged for hot paths, not deprecated; we hit it ≤ 30 times during cold start.
- **JSON parse cost on cold start** → Estimated 5–10 KB per route, 2–5 KB per spot, ~50 KB total parsed once. `Dispatchers.IO` keeps it off the main thread.
- **CSV's multi-line photo-tip cell with `①②③` markers** → Generator robustness verified by snapshot fixtures (`data/scripts/test/fixtures/*.csv`) committed alongside the script.

## Migration Plan

This change is additive; nothing to migrate. Rollout per change-internal task ordering:

1. Promote `data/tainan_routes.csv` and add `.gitattributes` (`text eol=lf` for CSV + JSON).
2. Add `data/scripts/generate_content.py` + `_assets.json` + a `data/tainan-route/README.md` noting "JSON is generated".
3. Run the generator; commit `data/tainan-route/{index,routes/*,spots/*}.json`.
4. Update `gradle/libs.versions.toml` + apply plugin + add `assets.srcDirs(...)` in `app/build.gradle.kts`.
5. Add Kotlin models + `ContentJson` + `ContentAssetLoader` + `ContentRepository` + `ContentSnapshot`.
6. Wire `MemoirApplication` to construct + hold the singleton.
7. Add `ContentValidationTest` + `ContentAssetLoaderTest` + `ContentRepositoryTest`.
8. Add CI job step: `python data/scripts/generate_content.py && git diff --exit-code data/tainan-route/`.

**Rollback**: revert the change commit; `MockData`-backed screens continue to work because nothing was modified.

## Open Questions

- **Generator language confirmation**: Python 3.11+ is assumed on team laptops + GitHub Actions ubuntu-latest. If the team prefers a Kotlin/Gradle generator after this design, swap D2 — the contract is unchanged.
- **Are there spots in the CSV that are not yet in `MockData`?** Likely yes (CSV has all 5 routes' worth of spots; `MockData` only fleshes out `grand_mazu`). The generator must succeed for under-populated spots — schema defaults (empty `LocalizedFacts`, empty `photographyTips`, empty `artifacts`) cover this.
- **Drawable for spots with no committed PNG yet** → Convention: fall back to the route's `heroImage`. `_assets.json` makes this explicit per-spot rather than a generator heuristic.
- **Section row recognition**: should the generator also handle a future "Route F" added by the designer? Yes — pattern is `Route [A-Z] …`, generator does not hard-code route ids. Route id is slugified from the EN half of the header.
- **`_assets.json` ↔ `MockData.kt` drift during the migration window**: while `MockData.kt` still ships (until follow-up changes finish migrating screens), the same drawable mapping exists in two places — `MockData.imageRes = R.drawable.X` and `_assets.json: { "heroImage": "X" }`. If a designer adds a new drawable mid-migration, they'll naively edit `_assets.json` while `MockData.kt` lags. Mitigation options to pick one of in the first follow-up change:
  1. Add a one-shot test that asserts `MockData.routes[i].imageRes` resolves to the same drawable as `_assets.json[routeId].heroImage`, failing fast on drift.
  2. Accept the drift on the grounds that `MockData` is unread by any new screen (only old screens read it), and old screens are being deleted anyway.
  3. Have the generator emit a `MockData.kt`-shaped Kotlin file as a secondary output, making `_assets.json` the single source.
  This change picks option 2 for simplicity (no extra test, MockData is dead code from the moment any consumer migrates), and trusts the follow-up change for each screen to delete the corresponding `MockData` entry as it migrates. Documented in `data/tainan-route/README.md`.
