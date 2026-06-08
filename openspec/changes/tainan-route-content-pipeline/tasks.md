## 1. Content authoring source

- [x] 1.1 Track `data/tainan_routes.csv` in git (`git add data/tainan_routes.csv`) and confirm encoding is UTF-8 (no BOM) before commit
- [x] 1.2 Add repo-root `.gitattributes` entries: `data/tainan_routes.csv text eol=lf` and `data/tainan-route/**/*.json text eol=lf` so Windows `core.autocrlf` does not mutate checked-out files
- [x] 1.3 Create `data/tainan-route/README.md` stating that JSON files are generated from `tainan_routes.csv` + `_assets.json` and MUST NOT be hand-edited; document `python data/scripts/generate_content.py` as the regeneration command

## 2. CSV ??JSON generator

- [x] 2.1 Create `data/scripts/generate_content.py` skeleton: stdlib-only (`csv`, `json`, `re`, `sys`, `pathlib`), `if sys.version_info < (3, 11): sys.exit("Python 3.11+ required")`
- [x] 2.2 Implement section header detection: regex `^Route\s+[A-Z]\b(.*)$` against `row[0]`, then `re.split(r"\s*/\s*", tail, maxsplit=1)`; exit non-zero if split yields < 2 parts
- [x] 2.3 Implement EN-name slugifier (lowercase, replace non-alphanumeric with `_`, collapse repeats, strip leading/trailing `_`) for deriving stable route ids and spot ids
- [x] 2.4 Open the CSV as `open(path, encoding="utf-8-sig", newline="")` (required by `csv` module for multi-line cells); skip the top-level banner row; parse the multi-line column-name header into a stable mapping ??schema field names; exit non-zero with precise message if expected columns are missing or renamed
- [x] 2.5 Implement Key Info splitter: split on either ASCII `|` (U+007C) or full-width `嚚 (U+FF5C), strip whitespace, drop empty entries ??`facts: List<String>`
- [x] 2.6 Implement Photography Tips splitter: regex `r"[??Ｔ?手?色?兩]\s*"` for primary path, blank-line fallback when markers are absent
- [x] 2.7 Implement `_assets.json` consumer: read the file, fail non-zero if any route id in `index.json.routes` or spot id in `index.json.spots` has no binding (print `missing _assets.json binding: <kind>.<id>` to stderr)
- [x] 2.8 Implement deterministic JSON writer: `json.dumps(obj, ensure_ascii=False, indent=2, sort_keys=True)`, `open(path, "w", encoding="utf-8", newline="\n")`, trailing newline at EOF
- [x] 2.9 Implement `index.json` emission: route + spot ids in ASCII-sorted order; no extras, no omissions
- [x] 2.10 Add `data/scripts/README.md` documenting invocation (`python data/scripts/generate_content.py` from repo root), `python` vs `python3` on Windows/Unix, and the `_assets.json` editing protocol
- [x] 2.11 Add stdlib-only `data/scripts/pyproject.toml` (declares Python 3.11 target, no dependencies)
- [x] 2.12 Add fixtures under `data/scripts/test/fixtures/`: multi-line quoted cell, both `|`/`嚚 Key Info flavors, missing `/` separator (expected exit ??0), missing `? markers (fallback)
- [x] 2.13 Add `data/scripts/test/__init__.py` (empty file to make the directory a package) and `data/scripts/test/test_generate_content.py` (unittest, stdlib) exercising the fixtures; verify `python -m unittest discover -s data/scripts/test -t data/scripts` exits 0 and reports a non-zero test count (`-s` start dir + `-t` top-level dir avoids the silent-zero-tests footgun on Windows)

## 3. First generated commit

- [x] 3.1 Create `data/tainan-route/_assets.json` populated from `MockData.kt` drawable references: every route id ??`heroImage` (matches `RouteData.imageRes`), every spot id ??`{ heroImage, photographyTipImages, artifactImages }`
- [x] 3.2 Run `python data/scripts/generate_content.py` from repo root; verify exits 0
- [x] 3.3 Inspect generated `data/tainan-route/index.json`, `routes/*.json`, `spots/*.json` for sanity (spot ids match MockData ids; journey arrays reference existing spots; titles bilingual)
- [x] 3.4 Re-run the generator; confirm `git diff --exit-code data/tainan-route/` reports zero changes (determinism gate)
- [x] 3.5 Commit generated files together with `_assets.json` and `.gitattributes`

## 4. Gradle catalog + module wiring

- [x] 4.1 Add to `frontend/mobile/gradle/libs.versions.toml` `[versions]`: `kotlinxSerialization` and `kotlinxCoroutines`. Pin to the latest stable line that supports Kotlin 2.2.10 ??at implementation time, look up `org.jetbrains.kotlinx:kotlinx-serialization-json` and `org.jetbrains.kotlinx:kotlinx-coroutines-core` on Maven Central / Context7 docs; do NOT copy the placeholder version from this task (the catalog currently pins neither library, so there is no "existing stack" to match)
- [x] 4.2 Add to `[libraries]`: `kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinxSerialization" }`, `kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "kotlinxCoroutines" }`, and `kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "kotlinxCoroutines" }`
- [x] 4.3 Add to `[plugins]`: `kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }`
- [x] 4.4 In `frontend/mobile/app/build.gradle.kts`, add `alias(libs.plugins.kotlin.serialization)` to the `plugins { }` block
- [x] 4.5 In the `android { }` block, add `sourceSets["main"].assets.srcDirs("src/main/assets", rootProject.file("../../data"))`
- [x] 4.6 In `dependencies { }`, add `implementation(libs.kotlinx.serialization.json)`, `implementation(libs.kotlinx.coroutines.core)`, and `testImplementation(libs.kotlinx.coroutines.test)`
- [x] 4.7 Create `frontend/mobile/app/src/main/res/raw/keep.xml` with `xmlns:tools="http://schemas.android.com/tools"` and `tools:keep="@drawable/*"` so future R8/shrinkResources runs do not strip dynamically-referenced drawables
- [x] 4.8 Run `./gradlew :app:assembleDebug` from `frontend/mobile/`; confirm build succeeds and no MockData consumer regression

## 5. Kotlin content models

- [x] 5.1 Create `frontend/mobile/app/src/main/java/com/mcis/memoir/data/content/model/LocalizedText.kt`: `@Serializable data class LocalizedText(val en: String, val zh: String)` with `operator fun get(locale: Locale): String = if (locale.language == "zh") zh else en`
- [x] 5.2 Create `LocalizedFacts.kt`: `@Serializable data class LocalizedFacts(val en: List<String> = emptyList(), val zh: List<String> = emptyList())`
- [x] 5.3 Create `Route.kt` with `Route` and `JourneyStop` per umbrella 禮4.4 (id, title, category, heroImage, description, tags default empty, journey)
- [x] 5.4 Create `Spot.kt` per umbrella 禮4.4 (id, title, heroImage, duration, whyItMatters, historicalContext, architecturalFeatures, modernUse, facts, photographyTips default empty, artifacts default empty)
- [x] 5.5 Create `PhotographyTip.kt`: `@Serializable data class PhotographyTip(val id: Int, val description: LocalizedText, val image: String)`
- [x] 5.6 Create `Artifact.kt`: `@Serializable data class Artifact(val id: Int, val title: LocalizedText, val description: LocalizedText, val image: String)`

## 6. Loader + repository

- [x] 6.1 Create `data/content/ContentSnapshot.kt`: `data class ContentSnapshot(val routes: Map<String, Route>, val spots: Map<String, Spot>)`
- [x] 6.2 Create `data/content/ContentJson.kt`: `internal val ContentJson: Json = Json { ignoreUnknownKeys = true; explicitNulls = false; prettyPrint = false }`
- [x] 6.3 Declare `data/content/ContentAssetLoader.kt` as an **interface**: `interface ContentAssetLoader { suspend fun load(): ContentSnapshot }`. This avoids `open class` boilerplate and lets the test write a trivial `class FakeContentAssetLoader(...) : ContentAssetLoader { ... }` (per spec.md 禮"Content is loaded once per process") without changing the production signature.
- [x] 6.4 Create `data/content/AssetManagerContentLoader.kt` (the production implementation of the interface above): constructor `(private val assets: AssetManager, private val json: Json)`. `override suspend fun load(): ContentSnapshot = withContext(Dispatchers.IO) { ??}` ??reads `tainan-route/index.json` (private nested `@Serializable data class Index(val routes: List<String>, val spots: List<String>)`), then each `routes/<id>.json` and `spots/<id>.json`, validates `file.id == filename basename`, returns a `ContentSnapshot`; throws `IllegalStateException` on missing files or id mismatch
- [x] 6.5 Create `data/content/ContentRepository.kt`: constructor `(loader: ContentAssetLoader, scope: CoroutineScope)`, `private val snapshot: Deferred<ContentSnapshot> = scope.async(Dispatchers.IO) { loader.load() }` (eager start per umbrella 禮4.5), expose `fun routes(): Flow<List<Route>>` (emits `snapshot.await().routes.values.toList()` once then completes ??order is `index.json.routes` ASCII-sorted order because `AssetManagerContentLoader` reads ids in that order and `associateBy` preserves insertion order), `suspend fun route(id: String): Route?`, `suspend fun spot(id: String): Spot?`

## 7. Application wiring

- [x] 7.1 Create `frontend/mobile/app/src/main/java/com/mcis/memoir/MemoirApplication.kt`: `class MemoirApplication : Application()`. The current project has no `Application` subclass (only `MainActivity`) ??this task creates one from scratch
- [x] 7.2 In `frontend/mobile/app/src/main/AndroidManifest.xml`, set `android:name=".MemoirApplication"` on the `<application>` element (currently absent at `AndroidManifest.xml:7-16`) so the class is actually instantiated at process start
- [x] 7.3 In `MemoirApplication.kt`, add a `companion object { lateinit var content: ContentRepository private set }` ??staging shortcut, removed by the Koin change
- [x] 7.4 In `MemoirApplication.onCreate()`, initialize: `val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate); content = ContentRepository(AssetManagerContentLoader(assets, ContentJson), scope)`
- [x] 7.5 Verify no MockData consumer is modified ??the new singleton is unused by existing screens in this change

## 8. Tests

- [x] 8.1 Create `frontend/mobile/app/src/test/java/com/mcis/memoir/data/content/ContentAssetLoaderTest.kt`: parse a known-good in-memory JSON tree via `ContentJson.decodeFromString<Route>(...)` / `<Spot>(...)`, assert structure; assert malformed JSON throws `SerializationException`; assert id-vs-filename mismatch surfacing from `AssetManagerContentLoader` throws `IllegalStateException` (use a tiny stub `AssetManager` via `mockito` is NOT in scope ??instead test the post-parse validation as a pure function extracted from the loader, e.g. `internal fun validateIdsMatchFilenames(routes: Map<String, Route>, spots: Map<String, Spot>)`)
- [x] 8.2 Create `ContentValidationTest.kt` ??JVM-only, no Robolectric, no `Resources` (consistent with design D11). The test reads JSON files from the repo-root `data/tainan-route/` path (resolvable from the JVM test working directory via `File("../../../data/tainan-route/")` relative to `app/`, or via a `testOptions { unitTests.isIncludeAndroidResources = false }`-safe `System.getProperty("user.dir")` walk) and asserts: (a) every `index.json` id has a file under `routes/` or `spots/`; (b) no duplicate ids; (c) every route/spot file's `"id"` field equals its filename basename; (d) every `journey[].spotId` exists in `index.json.spots`; (e) no `journey` has duplicate `order` values; (f) every drawable name in `_assets.json` is present in the set of `static int` field names of the generated `com.mcis.memoir.R$drawable` class (obtained via `R.drawable::class.java.fields` ??this works in JVM unit tests because AGP's `processDebugUnitTestResources` puts the R class on the test classpath, no `Resources` instance needed)
- [x] 8.3 Create `ContentRepositoryTest.kt`: use a hand-written `class FakeContentAssetLoader(private val snapshot: ContentSnapshot) : ContentAssetLoader { var loadCount = 0; override suspend fun load(): ContentSnapshot { loadCount++; return snapshot } }` (no MockK; works because `ContentAssetLoader` is an interface per task 6.3); run inside `runTest { ... }` with `kotlinx-coroutines-test`; assert: `route("known")` returns non-null, `route("unknown")` returns null, `spot("known")` returns non-null, `routes().first()` emits the full list in `index.json` order, three lookups trigger exactly one `loadCount` increment, exception from loader propagates via both `route(id)` (rethrows) and `routes().first()` (rethrows inside collector)
- [x] 8.4 Run `./gradlew :app:testDebugUnitTest`; confirm all three test classes pass

## 9. CI sync enforcement

- [x] 9.1 Create `.github/workflows/content-sync.yml` (or add a step to an existing mobile workflow): job triggers on `pull_request` and `push` to `main`; `runs-on: ubuntu-latest`
- [x] 9.2 First job step: `actions/checkout@v4` (without it, subsequent `python data/scripts/...` and `git diff` have no repo to act on)
- [x] 9.3 Next step: `actions/setup-python@v5` with `python-version: '3.11'`
- [x] 9.4 Run `python data/scripts/generate_content.py` from repo root (the default `working-directory` is fine ??`actions/checkout@v4` checks out into `$GITHUB_WORKSPACE`, which becomes the default working dir)
- [x] 9.5 Run `git diff --exit-code data/tainan-route/`; non-empty diff fails the job with a clear message ("CSV ??JSON generator output drifted; rerun `python data/scripts/generate_content.py` and commit the result")
- [x] 9.6 Also run `python -m unittest discover -s data/scripts/test -t data/scripts` so the generator's own fixture suite is part of CI

## 10. Verification gate

- [x] 10.1 `cd frontend/mobile && ./gradlew :app:assembleDebug` succeeds
- [x] 10.2 `cd frontend/mobile && ./gradlew :app:testDebugUnitTest` passes
- [x] 10.3 `openspec verify tainan-route-content-pipeline --strict` reports zero issues
- [ ] 10.4 Manual smoke check on emulator: existing MockData-backed screens (HomeScreen, RouteDetailScreen, SpotIntroScreen) render identically to before ??this change is additive only
- [ ] 10.5 In a follow-up PR description / Koin-change brief, record the staging-shortcut removal obligation: "Koin change MUST delete `MemoirApplication.Companion.content` and migrate every reading site to `koinInject<ContentRepository>()`."
