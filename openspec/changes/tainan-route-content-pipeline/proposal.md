## Why

The Android app currently hard-codes route + spot content in `data.MockData`, forcing recompiles for any copy change and forking the designer's source-of-truth (`data/tainan_routes.csv`). With the mobile-direct architecture (no backend), the app must load designer-editable bilingual content from a reproducible, versioned, on-device source before any of the other 8 follow-up changes can replace their hard-coded strings. This change establishes that foundation: a deterministic CSV → JSON authoring pipeline, an assets-bundled JSON layout, kotlinx-serialization models, and a `ContentRepository` — without breaking the existing `MockData`-backed screens (consumers migrate in later changes).

## What Changes

- Adopt `data/tainan-route/` at the repo root as the bundled content directory (per umbrella §4.1) with this exact layout:
  ```
  data/tainan-route/
  ├── index.json
  ├── routes/<routeId>.json
  └── spots/<spotId>.json
  ```
  No separate `artifacts/`, `tags.json`, or `templates.json` in this change — artifacts are embedded in each spot JSON; route tags are inline string ids; templates are out of scope here.
- Adopt a **CSV-as-source** authoring pipeline (umbrella §4.0): commit both `data/tainan_routes.csv` AND the generated `data/tainan-route/**/*.json`, with a deterministic generator (Gradle task or script) and a CI check that fails if regenerating the JSON from the CSV produces a diff.
- Define the JSON schema with bilingual `LocalizedText {en, zh}` wrappers on every user-facing string (umbrella §4.2); image fields hold **drawable resource names** (no extension), resolved at runtime via `Resources.getIdentifier(name, "drawable", pkg)` — images themselves stay in `app/src/main/res/drawable/`.
- Add `kotlinx-serialization-json` + the `kotlin("plugin.serialization")` Gradle plugin; add catalog entries to `gradle/libs.versions.toml`.
- Wire `frontend/mobile/app/build.gradle.kts`:
  ```kotlin
  android {
      sourceSets["main"].assets.srcDirs(
          "src/main/assets",
          rootProject.file("../../data")
      )
  }
  ```
  so `data/tainan-route/` ships as `assets/tainan-route/` inside the APK with no copy step.
- Introduce `@Serializable` Kotlin models in `com.mcis.memoir.data.content.model` (`LocalizedText`, `LocalizedFacts`, `Route`, `JourneyStop`, `Spot`, `PhotographyTip`, `Artifact`) mirroring §4.4.
- Introduce `data.content.ContentAssetLoader` (reads `index.json`, then routes/spots, returns a `ContentSnapshot`) and `data.content.ContentRepository` (`Deferred<ContentSnapshot>` cached for the process lifetime; exposes `routes(): Flow<List<Route>>`, `suspend fun route(id)`, `suspend fun spot(id)` per §4.5).
- Ship a content validation test suite (umbrella §10 "Unit — Content validation"): JSON parses, `index.json` lists every file under `routes/`+`spots/`, no duplicate ids, every `journey[].spotId` resolves to a spot, every `heroImage` / `photographyTips[].image` / `artifacts[].image` resolves to a non-zero drawable id.
- Keep `data.MockData` and all current `RouteData` / `SpotData` consumers compiling — this change introduces the new content layer in parallel. Migration of HomeScreen / RouteDetailScreen / etc. to `ContentRepository` is the job of follow-up changes per the umbrella's build order.
- **Out of scope for this change** (deferred to other umbrella items): Koin DI wiring (`ContentRepository` is held as a `lateinit var content: ContentRepository` field on a `companion object` of `MemoirApplication`, initialized in `onCreate` — explicit staging shortcut that the Koin change will replace with `koinInject<ContentRepository>()`), `MockData` deletion, screen migration, navigation rewrite, DataStore, Room, LLM client, and i18n locale switching.

## Capabilities

### New Capabilities
- `content-pipeline`: Bundled, reproducible, bilingual Tainan-route content for the Android app — the CSV authoring source, the deterministic CSV → JSON generator, the on-disk JSON schema, the build-time `sourceSets` wiring, the `@Serializable` Kotlin models, the asset loader, and the in-app `ContentRepository` that serves routes and spots to every feature.

### Modified Capabilities
<!-- None — this is the first capability in the repo. -->

## Impact

- **New files (repo root content)**:
  - `data/tainan-route/index.json`
  - `data/tainan-route/routes/<routeId>.json` (one per route already in `MockData` / CSV)
  - `data/tainan-route/spots/<spotId>.json` (one per spot)
  - `data/tainan_routes.csv` — promote the currently untracked designer CSV into the repo (the canonical source).
- **New files (generator)**: a script or Gradle task under `frontend/mobile/` (e.g. `buildSrc/` or `scripts/generate_content.{py|kts}`) that converts the CSV to JSON deterministically; the exact location and command is documented in this change's `design.md` and committed alongside.
- **New files (mobile)**:
  - `frontend/mobile/app/src/main/java/com/mcis/memoir/data/content/ContentAssetLoader.kt`
  - `frontend/mobile/app/src/main/java/com/mcis/memoir/data/content/ContentRepository.kt`
  - `frontend/mobile/app/src/main/java/com/mcis/memoir/data/content/ContentSnapshot.kt`
  - `frontend/mobile/app/src/main/java/com/mcis/memoir/data/content/model/LocalizedText.kt`
  - `frontend/mobile/app/src/main/java/com/mcis/memoir/data/content/model/LocalizedFacts.kt`
  - `frontend/mobile/app/src/main/java/com/mcis/memoir/data/content/model/Route.kt` (with `JourneyStop`)
  - `frontend/mobile/app/src/main/java/com/mcis/memoir/data/content/model/Spot.kt`
  - `frontend/mobile/app/src/main/java/com/mcis/memoir/data/content/model/PhotographyTip.kt`
  - `frontend/mobile/app/src/main/java/com/mcis/memoir/data/content/model/Artifact.kt`
- **New tests**:
  - `frontend/mobile/app/src/test/java/com/mcis/memoir/data/content/ContentAssetLoaderTest.kt` (parse + flatten)
  - `frontend/mobile/app/src/test/java/com/mcis/memoir/data/content/ContentValidationTest.kt` (index completeness, no duplicate ids, spotId references resolve, drawable names resolve)
  - `frontend/mobile/app/src/test/java/com/mcis/memoir/data/content/ContentRepositoryTest.kt` (Flow emits once, lookups, parse-failure path) — plain JUnit4 + `kotlinx-coroutines-test` `runTest`; uses a hand-written fake `ContentAssetLoader` (no MockK), exercises the single-emit Flow with `first()` (no Turbine). Adding MockK/Turbine is deferred to a future change that actually needs multi-emit / mock-class testing.
- **Modified files**:
  - `frontend/mobile/app/build.gradle.kts` (apply `kotlin("plugin.serialization")`, add `kotlinx-serialization-json` dep, add `assets.srcDirs(...)` for `rootProject.file("../../data")`).
  - `frontend/mobile/gradle/libs.versions.toml` (`kotlinx-serialization-json` version + lib alias, `kotlin-serialization` plugin alias).
  - `.github/workflows/*.yml` — a new job (or addition to existing job) that runs the CSV → JSON generator and `git diff --exit-code data/tainan-route/` to enforce in-sync.
- **Dependencies added**:
  - `org.jetbrains.kotlinx:kotlinx-serialization-json` (version per `libs.versions.toml`).
  - Kotlin Serialization Gradle plugin (`org.jetbrains.kotlin.plugin.serialization`).
- **Build-time impact**:
  - APK size grows by the JSON payload only; image drawables are unchanged.
  - No new runtime permissions (assets read via `AssetManager`).
- **Risk acknowledgements**:
  - The CSV becomes the canonical edit surface — any direct edits to generated JSON will be wiped by the next generator run. The generator script + CI guardrail enforce this.
  - Drawable-name validation runs only in tests; production failure mode for a missing drawable is a `0` resource id at runtime — UI must show a placeholder. (Follow-up screen-migration changes own placeholder UX; this change is responsible for the test that prevents committing a broken reference.)
- **Not changed in this proposal** (called out so reviewers don't expect them):
  - `data.MockData` / `data.RouteData` / `data.PreferenceManager` — kept as-is.
  - Any screen file under `com.mcis.memoir.*` — no UI consumers migrated yet.
  - Koin / DataStore / Room / Nav3 / LLM — covered by later umbrella changes.
