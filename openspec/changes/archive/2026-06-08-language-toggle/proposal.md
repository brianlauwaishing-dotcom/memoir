## Why

Today the app stores `selected_language` in a SharedPreferences-backed `data.PreferenceManager` (main-thread synchronous I/O) and propagates it as a `selectedLanguage: String` parameter into every Composable, which then chooses between `R.string.X` and `R.string.X_zh` by hand. This forks the chrome-text rendering path away from Android's standard locale system, has no way to react to system-level locale change, and cannot drive the `LocalizedText[locale]` accessor that `tainan-route-content-pipeline` (change #1) introduced for content text. The language toggle on `LanguageSelectionScreen` also doesn't *do* anything beyond updating an in-memory String — the app keeps rendering the same locale until the user kills and reopens it, and even then only because Compose remembers a fresh `mutableStateOf(preferenceManager.selectedLanguage)`.

This change replaces both layers with the umbrella-blessed wiring: DataStore-backed `UserPreferencesRepository`, `AppCompatDelegate.setApplicationLocales` for runtime locale switching, a `values-zh/strings.xml` mirror so `stringResource(R.string.X)` Just Works, and a `currentLocale()` helper for content-text consumers.

## What Changes

- Add `androidx.datastore:datastore-preferences` to the Gradle catalog.
- Introduce `com.mcis.memoir.data.prefs.UserPreferencesRepository` exposing `language: Flow<String>`, `selectedInterests: Flow<Set<String>>`, `onboardingDone: Flow<Boolean>`, `bookmarkedRouteIds: Flow<Set<String>>` (the last one folded in so `route-bookmarking` doesn't need a second DataStore wrapper), with `suspend` setters and a one-shot `SharedPreferencesMigration` that adopts the existing `user_prefs` SharedPreferences keys (`selected_language`, `user_interests`, `onboarding_completed`, `saved_route_ids`).
- Delete `com.mcis.memoir.data.PreferenceManager` (its only call site is `MyAppNavigation`, which this change migrates).
- Create `res/values-zh/strings.xml` containing the **canonical** translations of every non-`_zh`-suffixed string in `res/values/strings.xml` (e.g. `next_button`, `welcome_subtitle`, `home_subtitle`). `stringResource(R.string.next_button)` then yields `"Next"` under `en` and `"下一步"` under `zh` automatically.
- **Do not** delete the legacy `_zh`-suffixed strings (`next_button_zh`, `welcome_subtitle_zh`, …) — they are still consumed by every screen except `LanguageSelectionScreen`. Each follow-up per-screen change (`home-discovery`, `route-bookmarking`, `artifact-discovery-flow`, etc.) drops the `_zh` suffix usage and the `selectedLanguage: String` parameter as it touches its own screen, and the final `_zh`-suffix removal happens in `memory-library-actions` (the last screen change in umbrella build order).
- Add `res/xml/locales_config.xml` declaring `en` and `zh` as supported locales, and reference it from `AndroidManifest.xml` via `android:localeConfig`.
- Add `com.mcis.memoir.i18n.LocaleController`: a thin object exposing `suspend fun setLocale(tag: String)` that persists the choice to DataStore AND calls `AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))`. Also exposes `@Composable fun currentLocale(): Locale = LocalConfiguration.current.locales[0]` for content-text consumers.
- Add `com.mcis.memoir.ui.language.LanguageSelectionViewModel` (MVI per umbrella §6) with `State { selected: String?, applying: Boolean }` (selected is null until `init` hydrates from prefs), `Intent { Select(tag), Confirm }`, `Effect { NavigateNext, ShowError(msg) }`. The screen's `Next` button fires `Confirm`, which calls `LocaleController.setLocale(...)` then emits the nav effect. (Navigation destination naming is the navigator's concern, not the effect's — the effect just says "advance".)
- Migrate `LanguageSelectionScreen` to read from the ViewModel via `state.collectAsStateWithLifecycle()` + `viewModel.effects.collect { ... }`. Remove its `initialLanguage` / `onLanguageSelect` / `onNextClick` parameters from the call site; `MyAppNavigation` now hands it nothing — locale is the source of truth.
- Migrate `MyAppNavigation` to read `language` / `userInterests` / `onboardingCompleted` / `savedRouteIds` from `UserPreferencesRepository` Flows via `collectAsStateWithLifecycle` (rather than constructing `PreferenceManager(context)`). Every other screen keeps its existing `selectedLanguage: String` parameter wired through this state for now — clean-up is each follow-up change's job.
- Add a startup reconciliation step in `MemoirApplication.onCreate` (the application class created by change #1): if `AppCompatDelegate.getApplicationLocales()` is empty AND `UserPreferencesRepository.language` has a persisted value, apply that tag via `setApplicationLocales`; if both are present and differ, the AppCompat value wins (it survives reboots at the OS layer) and DataStore is updated to match.
- **Not in scope**: deleting any `_zh`-suffixed string, migrating non-`LanguageSelection` screens off `selectedLanguage: String` parameters, Koin DI (Application still owns the singletons explicitly per the change #1 staging shortcut), system-level locale change observers beyond the AppCompat→DataStore reconciliation, RTL support.

## Capabilities

### New Capabilities
- `language-toggle`: User-facing language selection persistence and runtime application — covers DataStore preferences storage, the `values-zh/` chrome-text layer, `AppCompatDelegate.setApplicationLocales` wiring, the `LocaleController` + `currentLocale()` API surface, and the MVI-shaped `LanguageSelectionViewModel`. Owns the contract that every other screen reads chrome text via `stringResource(R.string.X)` and content text via `LocalizedText[currentLocale()]`.

### Modified Capabilities
<!-- None — `content-pipeline` (change #1) is the only existing capability and it is untouched here. -->

## Impact

- **New files**:
  - `frontend/mobile/app/src/main/java/com/mcis/memoir/data/prefs/UserPreferencesRepository.kt`
  - `frontend/mobile/app/src/main/java/com/mcis/memoir/i18n/LocaleController.kt`
  - `frontend/mobile/app/src/main/java/com/mcis/memoir/ui/language/LanguageSelectionViewModel.kt`
  - `frontend/mobile/app/src/main/res/values-zh/strings.xml`
  - `frontend/mobile/app/src/main/res/xml/locales_config.xml`
  - Tests: `UserPreferencesRepositoryTest.kt`, `LocaleControllerTest.kt`, `LanguageSelectionViewModelTest.kt`.
- **Modified files**:
  - `frontend/mobile/app/src/main/AndroidManifest.xml` — add `android:localeConfig="@xml/locales_config"` to `<application>`.
  - `frontend/mobile/app/src/main/java/com/mcis/memoir/MyAppNavigation.kt` — replace `PreferenceManager(context)` with collected Flows from `UserPreferencesRepository`; rewire `LanguageSelectionDestination` entry to construct the ViewModel and drop the parameter-passing `initialLanguage`/`onLanguageSelect`/`onNextClick`.
  - `frontend/mobile/app/src/main/java/com/mcis/memoir/LanguageSelectionScreen.kt` — switch to ViewModel + `collectAsStateWithLifecycle` per umbrella §6.3; the `selectedLanguage` is now derived from `LocalConfiguration.current.locales[0]` for preview consistency.
  - `frontend/mobile/app/src/main/java/com/mcis/memoir/MemoirApplication.kt` — add startup reconciliation block, construct `UserPreferencesRepository` + `LocaleController` singletons via the same `companion object` staging shortcut introduced in change #1.
  - `frontend/mobile/gradle/libs.versions.toml` — add `androidx-datastore-preferences` (version + library alias).
  - `frontend/mobile/app/build.gradle.kts` — `implementation(libs.androidx.datastore.preferences)`.
- **Deleted files**:
  - `frontend/mobile/app/src/main/java/com/mcis/memoir/data/PreferenceManager.kt` — replaced; no remaining references after `MyAppNavigation` migration.
- **Dependencies added**: `androidx.datastore:datastore-preferences` (latest stable as of implementation).
- **Risk acknowledgements**:
  - `SharedPreferencesMigration` runs once at first DataStore read; if it fails mid-migration the second read would re-attempt against an already-renamed `user_prefs.xml`. Guard with the standard AndroidX `SharedPreferencesMigration` from `androidx.datastore.migrations`, which handles this idempotently.
  - `AppCompatDelegate.setApplicationLocales` triggers Activity re-creation. `MainActivity` currently has no `onSaveInstanceState`-backed state that would be lost (the only stateful piece is `MyAppNavigation`'s `backStack`, which Compose's `rememberSaveable` does not currently apply to). Locale change during onboarding therefore drops the user back to `WelcomeDestination`. Acceptable for MVP — locale is typically set once during onboarding and not toggled mid-session.
  - Non-`LanguageSelection` screens keep using `_zh`-suffix strings selected via a `selectedLanguage` parameter for now. They render correctly because both branches still exist in `values/strings.xml`. Each follow-up change owns its own migration.
- **Not changed**:
  - Any screen file other than `LanguageSelectionScreen.kt` and `MyAppNavigation.kt`.
  - `content-pipeline` (change #1) artifacts.
  - LLM / Room / Nav3 rewrite / camera / memory wizard.
