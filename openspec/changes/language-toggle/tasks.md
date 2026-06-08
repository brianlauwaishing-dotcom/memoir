## 1. Gradle catalog + module wiring

- [ ] 1.1 In `frontend/mobile/gradle/libs.versions.toml` add `[versions] datastore = "<latest stable AndroidX DataStore at implementation time>"` (look up `androidx.datastore:datastore-preferences` on Maven Central / Context7 docs before pinning)
- [ ] 1.2 Add `[libraries] androidx-datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }`
- [ ] 1.3 In `frontend/mobile/app/build.gradle.kts` `dependencies { }`, add `implementation(libs.androidx.datastore.preferences)`
- [ ] 1.4 Run `cd frontend/mobile && ./gradlew :app:dependencies | rg datastore-preferences` to confirm the artifact resolved

## 2. UserPreferencesRepository (interface + DataStore impl)

- [ ] 2.1 Create `frontend/mobile/app/src/main/java/com/mcis/memoir/data/prefs/UserPreferencesRepository.kt` declaring `interface UserPreferencesRepository` with `language: Flow<String>` (never empty — falls back to `defaultLocaleTag()`), `selectedInterests: Flow<Set<String>>`, `onboardingDone: Flow<Boolean>`, `bookmarkedRouteIds: Flow<Set<String>>`, `suspend fun setLanguage(tag: String)`, `suspend fun setInterests(set: Set<String>)`, `suspend fun markOnboardingDone()`, `suspend fun setBookmarkedRouteIds(set: Set<String>)`, **and** `suspend fun persistedLanguageTag(): String?` — the last method returns the raw DataStore value (null when no entry) and exists solely so `LocaleController.reconcileAtStartup` can distinguish "never persisted" from "persisted with default value"
- [ ] 2.2 Create `data/prefs/UserPrefsKeys.kt` (internal object) with `stringPreferencesKey("selected_language")`, `stringSetPreferencesKey("user_interests")`, `booleanPreferencesKey("onboarding_completed")`, `stringSetPreferencesKey("saved_route_ids")` — names MUST match legacy `PreferenceManager` keys exactly so the SharedPreferences migration picks them up
- [ ] 2.3 Create `data/prefs/UserDataStore.kt` with the top-level Context extension `val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs", produceMigrations = { ctx -> listOf(SharedPreferencesMigration(ctx, "user_prefs")) })` — file name `"user_prefs"` matches the legacy SharedPreferences file name so the migration helper finds it
- [ ] 2.4 Create `data/prefs/DataStoreUserPreferencesRepository.kt` implementing the interface: each `Flow<T>` is `dataStore.data.map { it[Keys.X] ?: default }`; setters use `dataStore.edit { it[Keys.X] = value }`; `language` falls back to `defaultLocaleTag()` (helper: `if (Locale.getDefault().language == "zh") "zh" else "en"`); `override suspend fun persistedLanguageTag(): String? = dataStore.data.first()[Keys.LANGUAGE]` (returns the raw nullable value without applying the default)

## 3. LocaleController

- [ ] 3.1 Create `frontend/mobile/app/src/main/java/com/mcis/memoir/i18n/LocaleController.kt` as `object LocaleController`
- [ ] 3.2 Implement `suspend fun setLocale(tag: String, prefs: UserPreferencesRepository)`: first `prefs.setLanguage(tag)`, then on `Dispatchers.Main.immediate` call `AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))`
- [ ] 3.3 Implement `fun reconcileAtStartup(prefs: UserPreferencesRepository, scope: CoroutineScope)`: `scope.launch { ... }`; read `AppCompatDelegate.getApplicationLocales()` + `prefs.persistedLanguageTag()` (NOT `prefs.language.first()` — the latter would always be non-null because of `defaultLocaleTag()` fallback, making the "neither" branch unreachable); resolve the three cases per spec.md ("AppCompat wins → write DataStore", "DataStore wins → call setApplicationLocales", "neither → no-op, system locale flows through and Confirm persists later"); note `appCompat.isEmpty()` is a method call, not a property
- [ ] 3.4 Implement `@Composable fun currentLocale(): Locale = LocalConfiguration.current.locales[0]`

## 4. LanguageSelectionViewModel (MVI)

- [ ] 4.1 Create `frontend/mobile/app/src/main/java/com/mcis/memoir/ui/language/LanguageSelectionViewModel.kt`
- [ ] 4.2 Declare `data class LanguageState(val selected: String? = null, val applying: Boolean = false)` — `selected` is nullable because the initial value `"en"` would be wrong for users whose system locale is Chinese (would flash English pre-selection until `init` hydrates from prefs); `null` represents "not yet hydrated" and the UI disables the Confirm button until hydration completes
- [ ] 4.3 Declare `sealed interface LanguageIntent { data class Select(val tag: String); data object Confirm }`
- [ ] 4.4 Declare `sealed interface LanguageEffect { data object NavigateNext; data class ShowError(val msg: String) }`
- [ ] 4.5 Implement `class LanguageSelectionViewModel(private val prefs: UserPreferencesRepository) : ViewModel()` with `private val _state = MutableStateFlow(LanguageState())`, `val state = _state.asStateFlow()`, `private val _effects = Channel<LanguageEffect>(Channel.BUFFERED)`, `val effects = _effects.receiveAsFlow()`
- [ ] 4.6 In `init { }` block, `viewModelScope.launch { _state.update { it.copy(selected = prefs.language.first()) } }` so the toggle pre-selects the current language
- [ ] 4.7 Implement `fun onIntent(intent)` reducer matching design D4: `Select(tag)` → `_state.update { it.copy(selected = tag) }`; `Confirm` → snapshot `_state.value` first, return early if `applying == true` OR `selected == null`, then `viewModelScope.launch { setApplying(true); runCatching { LocaleController.setLocale(snapshot.selected!!, prefs) }.onSuccess { _effects.send(NavigateNext) }.onFailure { _effects.send(ShowError(it.message ?: "failed")) }; setApplying(false) }`
- [ ] 4.8 The early-return in 4.7 handles both the double-Confirm case (`applying == true`) and the not-yet-hydrated case (`selected == null`); the screen separately disables the Next button under both conditions

## 5. Locale resources

- [ ] 5.1 Create `frontend/mobile/app/src/main/res/values-zh/strings.xml` containing `<string name="X">…</string>` for every non-`_zh`-suffixed string in `res/values/strings.xml`. Source the Chinese text from the corresponding `<string name="X_zh">` entry. Do not delete any entry from `res/values/strings.xml`
- [ ] 5.2 Create `frontend/mobile/app/src/main/res/xml/locales_config.xml` with `<locale-config xmlns:android="http://schemas.android.com/apk/res/android"><locale android:name="en" /><locale android:name="zh" /></locale-config>`
- [ ] 5.3 In `frontend/mobile/app/src/main/AndroidManifest.xml`, add `android:localeConfig="@xml/locales_config"` to the `<application>` element

## 6. Application + Activity wiring

- [ ] 6.1 In `MemoirApplication.kt` (created by change #1), add `companion object { lateinit var prefs: UserPreferencesRepository private set; lateinit var content: ContentRepository private set }` (keep both fields)
- [ ] 6.2 In `MemoirApplication.onCreate()`, after `super.onCreate()`, construct `val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)`, then `prefs = DataStoreUserPreferencesRepository(applicationContext.userDataStore)` (type is the impl class, the field type is the interface), then `LocaleController.reconcileAtStartup(prefs, appScope)`, then existing `content = ContentRepository(...)` block stays
- [ ] 6.3 Verify the existing change-#1 `content` field is NOT touched in a breaking way; both singletons live side by side until the Koin change replaces them
- [ ] 6.4 In `MainActivity.kt`, change `class MainActivity : ComponentActivity()` to `class MainActivity : AppCompatActivity()` and update the import. Reason: `AppCompatDelegate.setApplicationLocales` is callable from any Context, but reliable Activity recreation on locale change requires the AppCompat delegate chain to be active in the foreground Activity. Compose stack works identically — `AppCompatActivity` extends `ComponentActivity` transitively; `setContent { ... }` continues to function. Verify the existing `setContent` block compiles without changes after the base class swap

## 7. Migrate MyAppNavigation off PreferenceManager

- [ ] 7.1 In `MyAppNavigation.kt`, replace `val preferenceManager = remember { PreferenceManager(context) }` with `val prefsRepo = remember { (context.applicationContext as MemoirApplication).prefs }`
- [ ] 7.2 Replace `var selectedLanguage by remember { mutableStateOf(preferenceManager.selectedLanguage) }` with `val selectedLanguage by prefsRepo.language.collectAsStateWithLifecycle(initialValue = "en")` (import `androidx.lifecycle.compose.collectAsStateWithLifecycle`). Add a catalog entry `androidx-lifecycle-runtime-compose = { module = "androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycleRuntimeKtx" }` (reuse the existing `lifecycleRuntimeKtx = "2.10.0"` version ref in `libs.versions.toml`, do NOT introduce a new version pin) and `implementation(libs.androidx.lifecycle.runtime.compose)` in `app/build.gradle.kts`
- [ ] 7.3 Replace `userInterests` / `savedRouteIds` mutableStateOf reads similarly with `collectAsStateWithLifecycle(initialValue = emptySet())`; `onboardingCompleted` with `collectAsStateWithLifecycle(initialValue = false)`. Note: `collectAsStateWithLifecycle` requires `initialValue` as a named arg when called on a plain `Flow<T>` (vs `StateFlow<T>`); positional arg may resolve to the wrong overload
- [ ] 7.4 In the entries that previously wrote via `preferenceManager.X = value` (e.g. `CultureInterestDestination.onStartExploringClick`, `RouteDetailDestination.onToggleSave`), wrap each write in `LaunchedEffect`-managed coroutine OR (simpler) capture a `coroutineScope = rememberCoroutineScope()` at the top and call `coroutineScope.launch { prefsRepo.setInterests(...) }` / `prefsRepo.setBookmarkedRouteIds(...)` / `prefsRepo.markOnboardingDone()`
- [ ] 7.5 Rewrite the `LanguageSelectionDestination` entry: remove `initialLanguage` / `onLanguageSelect` / `onNextClick` params; instead, construct `val vm = viewModel { LanguageSelectionViewModel(prefsRepo) }` and pass `onNavigateNext = { backStack.add(CultureInterestDestination) }` only as a navigation callback wired to the effect collector inside `LanguageSelectionScreen`
- [ ] 7.6 Delete the `selectedLanguage = languageCode; preferenceManager.selectedLanguage = languageCode` line from the old onLanguageSelect — language persistence is now the ViewModel's job

## 8. Migrate LanguageSelectionScreen to MVI shape

- [ ] 8.1 Change the signature to `@Composable fun LanguageSelectionScreen(onNavigateNext: () -> Unit, viewModel: LanguageSelectionViewModel = viewModel(), modifier: Modifier = Modifier)` — drop the legacy `initialLanguage` / `onLanguageSelect` / `onNextClick` parameters
- [ ] 8.2 Inside the Composable: `val state by viewModel.state.collectAsStateWithLifecycle()`; collect effects via `LaunchedEffect(viewModel) { viewModel.effects.collect { e -> when (e) { is LanguageEffect.NavigateNext -> onNavigateNext(); is LanguageEffect.ShowError -> Log.w(...) } } }`
- [ ] 8.3 Bind UI clicks: language option `onClick = { viewModel.onIntent(LanguageIntent.Select(tag)) }`; Next button `onClick = { viewModel.onIntent(LanguageIntent.Confirm) }`
- [ ] 8.4 Disable the Next button while `state.applying == true` to give visual feedback of the in-flight persistence + locale apply
- [ ] 8.5 Extract a stateless inner Composable `LanguageSelectionContent(state: LanguageState, onSelect: (String) -> Unit, onConfirm: () -> Unit, modifier: Modifier)` that takes raw state + lambdas. The public `LanguageSelectionScreen` becomes a thin wrapper that constructs a ViewModel and calls `LanguageSelectionContent`. `LanguageSelectionScreenPreview()` then renders `LanguageSelectionContent(state = LanguageState(selected = "en"), onSelect = {}, onConfirm = {})` with no ViewModel, no DataStore, no `ViewModelStoreOwner` requirement — the preview tooling crashes if you call `viewModel()` outside a real Activity

## 9. Delete legacy PreferenceManager

- [ ] 9.1 Confirm `grep -r PreferenceManager frontend/mobile/app/src/` returns zero matches after steps 7.x land (use Grep tool, not bash grep)
- [ ] 9.2 Delete `frontend/mobile/app/src/main/java/com/mcis/memoir/data/PreferenceManager.kt`
- [ ] 9.3 Re-run `:app:assembleDebug` to confirm no broken imports

## 10. Tests

- [ ] 10.1 Create `frontend/mobile/app/src/test/java/com/mcis/memoir/data/prefs/UserPreferencesRepositoryTest.kt`: use a JVM-only in-memory `DataStore<Preferences>` (`PreferenceDataStoreFactory.create` pointing at a `java.io.File.createTempFile`); assert each setter then getter round-trip; assert `language` default falls back to `defaultLocaleTag()` when no value persisted
- [ ] 10.2 Create `LocaleControllerTest.kt` JVM-only: hand-written `FakeUserPreferencesRepository : UserPreferencesRepository` backed by `MutableStateFlow`s; cover all three `reconcileAtStartup` branches by stubbing `AppCompatDelegate.getApplicationLocales()` (this is testable in unit tests because the AppCompat delegate exposes a getter; if the static call cannot be intercepted without Robolectric, extract a `LocaleSource` seam: `interface LocaleSource { fun appLocales(): LocaleListCompat; fun setAppLocales(tags: LocaleListCompat) }` with a production impl wrapping `AppCompatDelegate` — make this decision at implementation time; if the seam adds more code than it saves, defer the reconcile test to a follow-up Robolectric-enabled change and instead unit-test `setLocale` only)
- [ ] 10.3 Create `LanguageSelectionViewModelTest.kt`: `runTest`-based; instantiate `LanguageSelectionViewModel(FakeUserPreferencesRepository(language = MutableStateFlow("en")))`; assert (a) init pre-selects `"en"` after the first state emission (state transitions from `selected = null` to `selected = "en"`), (b) `Select("zh")` updates state to `selected = "zh"` without effect, (c) `Confirm` calls `prefs.setLanguage("zh")` exactly once and emits `NavigateNext`, (d) `Confirm` when fake `setLanguage` throws emits `ShowError`, (e) double `Confirm` while applying emits at most one `NavigateNext`, (f) `Confirm` when `selected == null` (e.g. before init hydration completes) is a no-op — no `setLanguage` call, no effect emitted
- [ ] 10.4 Run `cd frontend/mobile && ./gradlew :app:testDebugUnitTest`; all new tests pass

## 11. Verification gate

- [ ] 11.1 `cd frontend/mobile && ./gradlew :app:assembleDebug` succeeds
- [ ] 11.2 `cd frontend/mobile && ./gradlew :app:testDebugUnitTest` passes (including content-pipeline tests from change #1)
- [ ] 11.3 `openspec validate language-toggle --strict` reports zero issues
- [ ] 11.4 Emulator smoke test: launch → splash → welcome → language select Chinese → tap Next → CultureInterestScreen renders Chinese text (sourced from existing `_zh`-suffix strings, validating the DataStore-Flow propagation path)
- [ ] 11.5 Emulator smoke test: kill and relaunch the app → splash → welcome → language pre-selected to Chinese (validating DataStore persistence + reconciliation path)
- [ ] 11.6 Emulator smoke test: clear app data (`adb shell pm clear com.mcis.memoir`) on a device with system locale set to Chinese → fresh launch shows welcome screen in Chinese (validating system-locale fallback)
- [ ] 11.7 Record Koin-change follow-up obligation: "Koin change MUST delete `MemoirApplication.Companion.prefs` and inject `UserPreferencesRepository` directly into `LanguageSelectionViewModel` + `MyAppNavigation` instead of pulling from the Application singleton"
