## Purpose

Define the Android language-toggle behavior, including DataStore-backed language persistence, runtime locale application, localized resource coverage, and migration away from the legacy `PreferenceManager` path.

## Requirements

### Requirement: User language preference is persisted via DataStore

`com.mcis.memoir.data.prefs.UserPreferencesRepository` SHALL persist the user-selected language as a BCP-47 tag string (e.g. `"en"`, `"zh"`) in a Jetpack DataStore Preferences instance named `"user_prefs"`, under the preference key `"selected_language"`. The key name `"selected_language"` is intentionally identical to the legacy `PreferenceManager.KEY_LANGUAGE`, so the umbrella's nominal `"language"` key (umbrella 禮7.1) is deviated from on purpose for SharedPreferences-migration continuity. Existing values written by the legacy `SharedPreferences` file of the same name MUST be inherited on first read via `androidx.datastore.migrations.SharedPreferencesMigration`.

The repository SHALL additionally expose `suspend fun persistedLanguageTag(): String?` returning the raw DataStore value (null when no entry exists), so `LocaleController.reconcileAtStartup` can distinguish "never persisted" from "persisted with the default-fallback value".

#### Scenario: Fresh install reads system locale as default
- **WHEN** the app is launched for the first time on a device whose `Locale.getDefault().language` is `"zh"`, and no DataStore value has been written
- **THEN** `UserPreferencesRepository.language.first()` returns `"zh"`

#### Scenario: Upgrade from a prior SharedPreferences-backed build inherits the saved value
- **WHEN** the user previously persisted `selected_language=zh` via the legacy `PreferenceManager`, the new APK is installed without clearing app data, and `UserPreferencesRepository.language.first()` is first read after upgrade
- **THEN** the read returns `"zh"`, AND a subsequent inspection shows the value has been copied into the DataStore file, AND the legacy `getSharedPreferences("user_prefs", MODE_PRIVATE)` file no longer holds the key

#### Scenario: Setting and re-reading the language tag
- **WHEN** a caller invokes `prefs.setLanguage("en")` and then collects `prefs.language.first()`
- **THEN** the emitted value is `"en"`

### Requirement: Confirming the language selection applies the locale runtime-wide

When the user taps the `Next` button on `LanguageSelectionScreen`, the system SHALL:
1. Persist the chosen tag via `UserPreferencesRepository.setLanguage(tag)`.
2. Apply the locale via `AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))`.
3. Emit a `LanguageEffect.NavigateNext` from the ViewModel after both operations complete.

Assertions on the AppCompat call (step 2) are observable via two paths: (a) directly, when the test harness extracts a `LocaleSource` seam from `LocaleController`, or (b) indirectly, by asserting that the DataStore write happened (step 1) and that `NavigateNext` was emitted (step 3), both of which are byproducts of the same `LocaleController.setLocale(...)` invocation. The implementing change MUST pick path (a) OR document in the implementation change why (b) is sufficient.

#### Scenario: User confirms a language change from English to Chinese
- **WHEN** the screen state is `LanguageState(selected = "zh", applying = false)` and the ViewModel receives `LanguageIntent.Confirm`
- **THEN** `UserPreferencesRepository.setLanguage("zh")` is invoked exactly once, AND the effects Flow emits `LanguageEffect.NavigateNext` exactly once (the corresponding `AppCompatDelegate.setApplicationLocales` invocation occurs inside `LocaleController.setLocale`; its assertion is implementation-detail, see preamble above)

#### Scenario: Persistence failure surfaces an error effect
- **WHEN** `prefs.setLanguage(tag)` throws (e.g. simulated DataStore I/O failure) during `LanguageIntent.Confirm` handling
- **THEN** the effects Flow emits `LanguageEffect.ShowError(...)` and no `NavigateNext` is emitted

### Requirement: Chrome text resolves via Android resources under the active locale

`res/values-zh/strings.xml` SHALL exist and contain Chinese translations for every non-`_zh`-suffixed `<string>` declared in `res/values/strings.xml`. After `AppCompatDelegate.setApplicationLocales` is invoked with `"zh"`, `stringResource(R.string.X)` MUST yield the Chinese value for any X declared in both files.

#### Scenario: Composable reads a chrome string under English locale
- **WHEN** the application locale is `"en"` and a Composable calls `stringResource(R.string.next_button)`
- **THEN** the returned value is the English value from `res/values/strings.xml`

#### Scenario: Composable reads a chrome string under Chinese locale
- **WHEN** the application locale is `"zh"` and a Composable calls `stringResource(R.string.next_button)`
- **THEN** the returned value is the Chinese value from `res/values-zh/strings.xml`

#### Scenario: Legacy `_zh`-suffixed strings are preserved unchanged
- **WHEN** this change lands, the existing `res/values/strings.xml` is inspected
- **THEN** every `<string name="X_zh">` entry from the prior build is still present, AND no `<string name="X">` entry was removed (legacy screens must still compile and render correctly)

### Requirement: Locale config declares supported app languages

`res/xml/locales_config.xml` SHALL declare exactly `en` and `zh` as supported locales. `AndroidManifest.xml` `<application>` SHALL reference it via `android:localeConfig="@xml/locales_config"`.

#### Scenario: Locale config file is committed
- **WHEN** the repo is inspected after this change lands
- **THEN** `frontend/mobile/app/src/main/res/xml/locales_config.xml` exists, contains `<locale android:name="en" />` and `<locale android:name="zh" />`, and is referenced from `AndroidManifest.xml` `<application android:localeConfig="@xml/locales_config" ...>`

### Requirement: MainActivity extends AppCompatActivity

`MainActivity` SHALL extend `androidx.appcompat.app.AppCompatActivity` (not `androidx.activity.ComponentActivity`) so that `AppCompatDelegate.setApplicationLocales` reliably triggers Activity recreation with the new locale applied to `Configuration`.

#### Scenario: MainActivity class hierarchy
- **WHEN** the repo is inspected after this change lands
- **THEN** `MainActivity.kt` declares `class MainActivity : AppCompatActivity()` and the corresponding import is `androidx.appcompat.app.AppCompatActivity`

### Requirement: Startup reconciliation keeps AppCompat and DataStore in sync

At process start, `LocaleController.reconcileAtStartup(prefs, scope)` SHALL be invoked from `MemoirApplication.onCreate` and reconcile the AppCompat application-locales state with the DataStore-persisted tag.

#### Scenario: AppCompat has an application locale, DataStore differs
- **WHEN** the OS reports `AppCompatDelegate.getApplicationLocales().toLanguageTags() == "zh"` AND `prefs.language.first() == "en"` at startup
- **THEN** `reconcileAtStartup` writes `"zh"` to DataStore (AppCompat wins because it survives reboots at the OS layer), AND `AppCompatDelegate.setApplicationLocales` is NOT called again

#### Scenario: DataStore has a stored tag, AppCompat is empty
- **WHEN** `AppCompatDelegate.getApplicationLocales().isEmpty()` returns `true` AND `prefs.persistedLanguageTag() == "zh"` at startup
- **THEN** `reconcileAtStartup` calls `AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("zh"))` exactly once, AND DataStore is NOT written again

#### Scenario: Neither source has a value
- **WHEN** `AppCompatDelegate.getApplicationLocales().isEmpty()` returns `true` AND `prefs.persistedLanguageTag()` returns `null` (no DataStore entry yet) at startup
- **THEN** `reconcileAtStartup` performs no AppCompat write and no DataStore write; the system locale flows through `Locale.getDefault()` (surfaced via `prefs.language`'s default fallback) and is applied by the first user `Confirm` from `LanguageSelectionScreen`

### Requirement: `currentLocale()` Composable returns the active locale

`LocaleController.currentLocale()` SHALL be a `@Composable` function that returns `LocalConfiguration.current.locales[0]`. Content-text consumers MUST use this helper so they react to AppCompat-driven configuration changes.

#### Scenario: Composable observes a locale change
- **WHEN** a Compose test renders a Composable that captures `LocaleController.currentLocale()` into local state, the test triggers `AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("zh"))`, and the recomposition completes
- **THEN** the captured locale's `language` value transitions from `"en"` to `"zh"` (Activity recreation re-emits the new `LocalConfiguration`)

### Requirement: `LanguageSelectionScreen` is driven by an MVI ViewModel

`LanguageSelectionScreen` SHALL be powered by `LanguageSelectionViewModel` using the umbrella's State / Intent / Effect triad. UI interacts with the ViewModel solely through `onIntent(intent)`; rendering reads `state` via `collectAsStateWithLifecycle`; one-shot side effects are consumed from a `Channel`-backed `effects: Flow<LanguageEffect>`. `LanguageState.selected` is nullable to model the pre-hydration window: the constructor emits `selected = null`, and the `init` block rewrites it to `prefs.language.first()` on first scope launch.

#### Scenario: Initial state before hydration completes
- **WHEN** the ViewModel is constructed and its `state` Flow's first value is read before `init` has completed (e.g. in a test that doesn't advance the dispatcher)
- **THEN** the first emitted state is `LanguageState(selected = null, applying = false)`, AND the UI MUST render the Next button as disabled while `selected` is null

#### Scenario: Hydration sets the pre-selected language
- **WHEN** the ViewModel's `init` completes against `prefs.language.first() == "zh"`
- **THEN** state transitions to `LanguageState(selected = "zh", applying = false)`

#### Scenario: Tapping a language option updates state without triggering an effect
- **WHEN** the screen state is `LanguageState(selected = "en")` and the ViewModel receives `LanguageIntent.Select("zh")`
- **THEN** state becomes `LanguageState(selected = "zh", applying = false)`, AND no `LanguageEffect` is emitted

#### Scenario: Tapping Next while applying does not double-fire
- **WHEN** the screen state is `LanguageState(selected = "zh", applying = true)` and the ViewModel receives a second `LanguageIntent.Confirm`
- **THEN** the second Confirm is ignored, AND the effects Flow emits at most one `NavigateNext` per logical confirmation

#### Scenario: Confirm before hydration is a no-op
- **WHEN** the screen state is `LanguageState(selected = null, applying = false)` (hydration has not yet completed) and the ViewModel receives `LanguageIntent.Confirm`
- **THEN** no `setLanguage` call is made, no `LanguageEffect` is emitted, and `applying` remains `false`

### Requirement: `PreferenceManager` is removed and `MyAppNavigation` reads from `UserPreferencesRepository`

`com.mcis.memoir.data.PreferenceManager` SHALL be deleted. `com.mcis.memoir.MyAppNavigation` MUST NOT reference `PreferenceManager` or `getSharedPreferences("user_prefs", ...)`. Compose state in `MyAppNavigation` MUST be collected from `UserPreferencesRepository` Flows.

#### Scenario: Build after this change has no PreferenceManager class
- **WHEN** the repo is inspected after this change lands
- **THEN** `frontend/mobile/app/src/main/java/com/mcis/memoir/data/PreferenceManager.kt` does not exist, AND `grep -r PreferenceManager frontend/mobile/app/src/` returns zero matches

#### Scenario: Navigation reads language from DataStore Flow
- **WHEN** `MyAppNavigation()` is composed under a test harness with `UserPreferencesRepository.language` emitting `"zh"`
- **THEN** `selectedLanguage` collected state equals `"zh"`, AND `LanguageSelectionDestination` entry renders with the Chinese subtitle string from `values-zh/strings.xml`

### Requirement: Legacy screens continue to render bilingual chrome text

This change MUST NOT break any non-`LanguageSelection` screen. Screens that currently take `selectedLanguage: String` and render `stringResource(if (isChinese) R.string.X_zh else R.string.X)` continue to compile and render after this change lands; their migration off the `_zh`-suffix pattern is owned by follow-up changes.

#### Scenario: Building the app after this change
- **WHEN** `./gradlew :app:assembleDebug` is run against the worktree after this change is merged
- **THEN** the build succeeds with zero references to `data.PreferenceManager`, AND every legacy screen (`WelcomeScreen`, `HomeScreen`, `MemoriesScreen`, etc.) still receives a non-null `selectedLanguage` value sourced from `UserPreferencesRepository.language`

#### Scenario: A legacy screen renders the correct language after a toggle
- **WHEN** the user completes onboarding by selecting Chinese on `LanguageSelectionScreen` and proceeds to `CultureInterestScreen`
- **THEN** `CultureInterestScreen` (still using the `_zh`-suffix pattern) renders Chinese text because its `selectedLanguage` parameter equals `"zh"` (sourced from the same DataStore Flow that `LanguageSelectionViewModel` just wrote)
