## 1. Manifest + BuildConfig + local.properties

- [ ] 1.1 Add `<uses-permission android:name="android.permission.INTERNET" />` to `frontend/mobile/app/src/main/AndroidManifest.xml` (after the existing CAMERA / WRITE_EXTERNAL_STORAGE entries)
- [ ] 1.2 In `frontend/mobile/app/build.gradle.kts`, add a top-of-file block (BEFORE the `android { }` block):
  ```kotlin
  val localProps = java.util.Properties().apply {
      val f = rootProject.file("local.properties")
      if (f.exists()) f.inputStream().use(::load)
  }
  ```
- [ ] 1.3 In `android { defaultConfig { ... } }`, add:
  ```kotlin
  buildConfigField(
      "String",
      "DEEPSEEK_API_KEY",
      "\"${localProps["DEEPSEEK_API_KEY"] ?: ""}\""
  )
  ```
- [ ] 1.4 In `android { buildFeatures { ... } }`, add `buildConfig = true` (the `compose = true` line stays)
- [ ] 1.5 Create `frontend/mobile/local.properties.example` with:
  ```
  # Memoir DeepSeek API key. Copy this file to `local.properties` and replace the value.
  # ROTATION: If the release APK leaks publicly, rotate this key at the DeepSeek dashboard
  # and update local builds. Per umbrella §5.3 this is acceptable risk for the academic-MVP demo.
  DEEPSEEK_API_KEY=replace-me-with-real-key
  ```
- [ ] 1.6 Verify `local.properties` is in `.gitignore` (should already be — Android Gradle template default); do NOT commit the real key

## 2. Gradle catalog — OpenAI client + Ktor CIO

- [ ] 2.1 In `frontend/mobile/gradle/libs.versions.toml` `[versions]` add `openai = "4.0.1"` and `ktor = "3.0.0"` (verify both versions against Maven Central at implementation time — `com.aallam.openai:openai-client` and `io.ktor:ktor-client-cio` must both support Kotlin 2.2.10)
- [ ] 2.2 In `[libraries]` add: `openai-client = { module = "com.aallam.openai:openai-client", version.ref = "openai" }`, `ktor-client-cio = { module = "io.ktor:ktor-client-cio", version.ref = "ktor" }`
- [ ] 2.3 In `frontend/mobile/app/build.gradle.kts` `dependencies { }` add: `implementation(libs.openai.client)`, `implementation(libs.ktor.client.cio)`
- [ ] 2.4 Run `cd frontend/mobile && ./gradlew :app:dependencies | rg openai-client` and verify the artifact resolves to v4.0.1

## 3. LLM data types

- [ ] 3.1 Create `frontend/mobile/app/src/main/java/com/mcis/memoir/data/llm/JourneyReflectionInput.kt`:
  ```kotlin
  data class JourneyReflectionInput(
      val locale: Locale,
      val routeId: String,
      val spotEntries: List<SpotEntry>,
      val overallMood: String?,
      val userInsights: String,
      val postTripFeedback: String?,
      val templateStyle: String
  )
  ```
- [ ] 3.2 Create `data/llm/SpotEntry.kt`:
  ```kotlin
  data class SpotEntry(val spotId: String, val userNote: String?, val photoCount: Int)
  ```
- [ ] 3.3 Create `data/llm/ReflectionResult.kt`:
  ```kotlin
  sealed interface ReflectionResult {
      data class Success(val text: String) : ReflectionResult
      data class Failure(val kind: ReflectionError, val cause: Throwable? = null) : ReflectionResult
  }
  ```
- [ ] 3.4 Create `data/llm/ReflectionError.kt`:
  ```kotlin
  sealed interface ReflectionError {
      data object Network : ReflectionError
      data object InvalidApiKey : ReflectionError
      data object RateLimited : ReflectionError
      data object ServiceUnavailable : ReflectionError
      data object Unexpected : ReflectionError
  }
  ```

## 4. PromptBuilder

- [ ] 4.1 Create `data/llm/PromptBuilder.kt` as `object PromptBuilder` per design D3 — `fun build(input: JourneyReflectionInput): List<ChatMessage>` returns the system + user message pair
- [ ] 4.2 System content: language directive (`"Output language: English"` or `"Output language: Traditional Chinese (繁體中文)"`), tone hint mapped from `templateStyle` (4 known cases + warm/personal default), 2-4 sentence + under-200-character guidance, no preamble / no quotation marks
- [ ] 4.3 User content: `Route id: <routeId>`, `Visited N stops with M photos total.`, per-stop lines `Stop K: spotId=<id>, photos=<n>` (skip `userNote` when null), `Overall mood: <mood>` (omit line when blank/null), `User reflection: <insights>` (placeholder `(none)` when blank), `Post-trip thoughts: <feedback>` (omit when blank/null)
- [ ] 4.4 `import com.aallam.openai.api.chat.ChatMessage` + `ChatRole` from the SDK; verify exact import paths at implementation time

## 5. JourneyInputAssembler

- [ ] 5.1 Create `data/llm/JourneyInputAssembler.kt` as `object JourneyInputAssembler` per design D4
- [ ] 5.2 `suspend fun build(memory: Memory, locale: Locale, contentRepo: ContentRepository): JourneyReflectionInput`: if `memory.routeId == null`, return with `routeId = "(none)"` + `spotEntries = emptyList()`; otherwise call `contentRepo.route(memory.routeId)` and map `route.journey` to `SpotEntry(spotId = stop.spotId, userNote = null, photoCount = memory.photoRelativePaths.size / max(route.journey.size, 1))`
- [ ] 5.3 Carry through `overallMood`, `userInsights`, `postTripFeedback`, `templateStyle = memory.templateId`, `locale`

## 6. ReflectionClient + DeepSeekReflectionClient

- [ ] 6.1 Create `data/llm/ReflectionClient.kt`:
  ```kotlin
  interface ReflectionClient {
      suspend fun generate(input: JourneyReflectionInput): ReflectionResult
  }
  ```
- [ ] 6.2 Create `data/llm/DeepSeekReflectionClient.kt` per design D1: constructor `(private val openAI: OpenAI)`; implement `generate` by building the `ChatCompletionRequest` (model `"deepseek-chat"`, temperature 0.8, maxTokens 500, messages from `PromptBuilder.build(input)`), wrapping `openAI.chatCompletion(req)` in `runCatching`, mapping the response or exception to `ReflectionResult`
- [ ] 6.3 Implement `private fun mapError(e: Throwable): ReflectionError` per design D1 — use type-based dispatch (`is`-checks) against the SDK's exception hierarchy in package `com.aallam.openai.api.exception`: `AuthenticationException` → InvalidApiKey, `RateLimitException` → RateLimited, `OpenAIServerException` → ServiceUnavailable, `OpenAITimeoutException` → Network, `OpenAIAPIException` → ServiceUnavailable (catch-all for other 4xx), `OpenAIHttpException` → Network, raw `IOException` → Network, else → Unexpected. **Do NOT** look for a non-existent `OpenAIException` class or a generic `statusCode` property — the SDK uses typed subclasses. Verify exact class names against the installed v4.0.1 jar before coding
- [ ] 6.4 Blank-response handling: if the SDK returns a non-throw response whose `.choices.first().message.content.orEmpty()` is `isBlank()`, return `Failure(Unexpected, null)` (NOT Success with empty string)

## 7. MemoirApplication wiring

- [ ] 7.1 In `MemoirApplication.kt`, add to the `companion object`:
  ```kotlin
  lateinit var reflectionClient: ReflectionClient private set
  ```
  alongside the existing `content`, `prefs`, `memoryRepo` fields
- [ ] 7.2 In `onCreate()`, after the existing `memoryRepo = ...` line, construct:
  ```kotlin
  val openAIConfig = OpenAIConfig(
      token = BuildConfig.DEEPSEEK_API_KEY,
      host = OpenAIHost(baseUrl = "https://api.deepseek.com/v1/"),
      timeout = Timeout(socket = 60.seconds)
  )
  val openAI = OpenAI(openAIConfig)
  reflectionClient = DeepSeekReflectionClient(openAI)
  ```
  — verify `OpenAIConfig` / `OpenAIHost` / `Timeout` constructor signatures against SDK v4.0.1 at implementation time
- [ ] 7.3 Add imports: `com.aallam.openai.client.OpenAI`, `com.aallam.openai.client.OpenAIConfig`, `com.aallam.openai.client.OpenAIHost`, `com.aallam.openai.api.http.Timeout`, `kotlin.time.Duration.Companion.seconds`

## 8. MemoryRepository extension

- [ ] 8.1 Add `suspend fun updateGeneratedReflection(memoryId: String, text: String)` to the `MemoryRepository` interface (change #7's file)
- [ ] 8.2 Implement in `RoomMemoryRepository`: `withContext(ioDispatcher) { val row = dao.getOnce(memoryId) ?: return@withContext; dao.upsert(row.copy(generatedReflection = text, updatedAt = System.currentTimeMillis())) }`
- [ ] 8.3 Note: this is an ADDITIVE interface change to change #7's contract — no MODIFIED spec block is needed; change #7's existing requirements are preserved. The interface change is one new method, NOT a re-edit of change #7's spec files

## 9. ReflectionState / Intent / Effect / VM extension

- [ ] 9.1 In `ui/memory/reflection/AiState.kt` (NEW file): `sealed interface AiState { data object Idle; data object Generating; data class Ready(val text: String); data class Error(val kind: ReflectionError, val message: String) }` with each member declared `: AiState`
- [ ] 9.2 In `ReflectionState.kt`, add `val aiState: AiState = AiState.Idle` field (existing 6 fields stay)
- [ ] 9.3 In `ReflectionIntent.kt`, add: `data object PolishClicked : ReflectionIntent`, `data object RegenerateClicked : ReflectionIntent`, `data object CopyClicked : ReflectionIntent`, `data object DismissAiError : ReflectionIntent`
- [ ] 9.4 In `ReflectionEffect.kt`, add: `data class CopyToClipboard(val text: String) : ReflectionEffect`
- [ ] 9.5 Extend `ReflectionViewModel` constructor with `private val reflectionClient: ReflectionClient` AND `private val contentRepo: ContentRepository` AND `private val resources: Resources` AND `private val localeProvider: () -> Locale` parameters
- [ ] 9.6 Add `private val generateMutex = Mutex()` instance field
- [ ] 9.7 Implement `private fun generate()` per design D5 with the TWO-LEVEL guard: (a) at the top of the function, `if (_state.value.aiState is AiState.Generating) return` — drops rapid double-taps cheaply before launching any coroutine; (b) inside `generateMutex.withLock { }`, re-check the same condition to catch rare interleavings; only then transition `aiState` to `Generating` and proceed. The Mutex alone does NOT dedupe — it serializes — so without the guard a queued tap would still spawn a fresh `reflectionClient.generate(...)` call after the current one releases the lock
- [ ] 9.8 Extend `onIntent(intent)` reducer:
  - `PolishClicked` → call `generate()` (no-op if already Generating, per 9.7's guard)
  - `RegenerateClicked` → call `generate()` (same behavior; the early-return guard fires identically — distinct intent kept for UX vocabulary only)
  - `CopyClicked` → if `_state.value.aiState is AiState.Ready`, emit `CopyToClipboard((aiState as AiState.Ready).text)`
  - `DismissAiError` → if `_state.value.aiState is AiState.Error`, `_state.update { it.copy(aiState = AiState.Idle) }`
- [ ] 9.9 Extend `SaveClicked` reducer to also call `repo.updateGeneratedReflection(memoryId, (aiState as? AiState.Ready)?.text ?: return@runCatching null)` before `complete(memoryId)` — only when `aiState is Ready`; otherwise skip the call so `generatedReflection` stays null
- [ ] 9.10 Extend `ReflectionViewModelFactory` with the new constructor parameters

## 10. MemoryReflectionScreen UI

- [ ] 10.1 In `MemoryReflectionScreen.kt`, collect `state.aiState` from `viewModel.state.collectAsStateWithLifecycle()` (already done by change #7)
- [ ] 10.2 Render the AI state UI per design D6:
  - When `aiState is Idle`: render the maroon "Polish with AI" button at the existing position; clicking fires `viewModel.onIntent(PolishClicked)`
  - When `aiState is Generating`: render the same button but disabled with a `CircularProgressIndicator` overlay
  - When `aiState is Ready(text)`: render a `Card { Text(text) }` below the input fields; render a row below with [Copy button] (`viewModel.onIntent(CopyClicked)`) + [Regenerate button] (`viewModel.onIntent(RegenerateClicked)`); hide the original Polish button
  - When `aiState is Error(kind, message)`: render an error chip below the input fields with `message` text + retry icon (`viewModel.onIntent(PolishClicked)`) + close icon (`viewModel.onIntent(DismissAiError)`)
- [ ] 10.3 Wire `CopyToClipboard` effect: `LaunchedEffect(viewModel) { viewModel.effects.collect { e -> when (e) { is ReflectionEffect.CopyToClipboard -> { val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager; clipboard.setPrimaryClip(ClipData.newPlainText("memoir-reflection", e.text)); copiedAt = System.currentTimeMillis() }; ... } } }` + a `var copiedAt by remember { mutableLongStateOf(0L) }` + a brief inline "Copied!" Text rendered when `System.currentTimeMillis() - copiedAt < 1500`
- [ ] 10.4 Verify the existing change-#7 effect handlers (`NavigateToMemoriesList`, `ShowError`) stay wired

## 11. MyAppNavigation entry update

- [ ] 11.1 In `MemoryReflectionDestination` entry, update the factory construction to pass `reflectionClient = MemoirApplication.reflectionClient` AND `contentRepo = MemoirApplication.content` AND `resources = ctx.resources` AND `localeProvider = { currentLocale }` (hoist `currentLocale = LocaleController.currentLocale()` per the `home-discovery` / `route-bookmarking` precedent)

## 12. String resources

- [ ] 12.1 Add to `res/values/strings.xml`:
  ```xml
  <string name="memory_reflection_generating">Generating…</string>
  <string name="memory_reflection_copied">Copied!</string>
  <string name="error_network">Network error. Check your connection.</string>
  <string name="error_invalid_api_key">API key invalid — contact dev.</string>
  <string name="error_rate_limited">Too many requests. Wait 30 seconds.</string>
  <string name="error_service_unavailable">Service unavailable. Try again later.</string>
  <string name="error_unexpected">Something went wrong. Try again.</string>
  ```
- [ ] 12.2 Add to `res/values-zh/strings.xml`:
  ```xml
  <string name="memory_reflection_generating">生成中…</string>
  <string name="memory_reflection_copied">已複製！</string>
  <string name="error_network">網路錯誤，請檢查連線</string>
  <string name="error_invalid_api_key">API 金鑰無效，請聯絡開發者</string>
  <string name="error_rate_limited">請求過於頻繁，請等候 30 秒</string>
  <string name="error_service_unavailable">服務暫時無法使用，請稍後再試</string>
  <string name="error_unexpected">發生未預期錯誤，請再試一次</string>
  ```

## 13. CI workflow

- [ ] 13.1 If `.github/workflows/mobile-ci.yml` does NOT exist (change #1 may or may not have created it), CREATE it with `runs-on: ubuntu-latest`, `actions/checkout@v4`, `actions/setup-java@v4` with `java-version: '11'`, then the steps in 13.2-13.5
- [ ] 13.2 Add a step BEFORE Gradle commands: `run: echo "DEEPSEEK_API_KEY=dummy-ci-key" >> frontend/mobile/local.properties` — pre-creates the file so the BuildConfig read in 1.3 doesn't see an absent key
- [ ] 13.3 Add `run: cd frontend/mobile && ./gradlew :app:assembleDebug`
- [ ] 13.4 Add `run: cd frontend/mobile && ./gradlew :app:testDebugUnitTest`
- [ ] 13.5 Add `run: cd frontend/mobile && ./gradlew :app:lintDebug`
- [ ] 13.6 NEVER add a step that writes a real key to CI — the dummy key is by design (any test accidentally hitting DeepSeek returns 401 fast)

## 14. Tests

- [ ] 14.1 Create `frontend/mobile/app/src/test/java/com/mcis/memoir/data/llm/PromptBuilderTest.kt` (JUnit5): cover the 5 spec scenarios per design D7 (English locale, zh locale, 4 templateStyle tone hints, spotEntries content, blank insights placeholder)
- [ ] 14.2 Create `data/llm/JourneyInputAssemblerTest.kt`: cover the 3 spec scenarios (route + photoCount split, null routeId, empty photoRelativePaths) — use a hand-rolled fake `ContentRepository` returning known route/spot data
- [ ] 14.3 Create `data/llm/DeepSeekReflectionClientTest.kt` (JUnit5 + MockK): `val openAI = mockk<OpenAI>()`; cover 6 spec scenarios (success, blank → Unexpected, 401, 429, 5xx, IOException, other exception) — see design D7 for exact mock setup. **Never** instantiate the real `OpenAI(config)` in tests
- [ ] 14.4 Create `ui/memory/reflection/ReflectionViewModelAiTest.kt` (JUnit5 + MockK + Turbine): mock `ReflectionClient`, `MemoryRepository`, `ContentRepository`, `Resources`; cover the AI-state-machine scenarios per design D7 (Polish → Generating → Ready, concurrent serialize, Polish-while-Generating no-op, failure → Error, Save persists generatedReflection only when Ready, Copy emits only when Ready, Dismiss returns to Idle)
- [ ] 14.5 Run `cd frontend/mobile && ./gradlew :app:testDebugUnitTest`; assert all new + existing tests pass

## 15. Verification gate

- [ ] 15.1 `cd frontend/mobile && ./gradlew :app:assembleDebug` succeeds (with real DEEPSEEK_API_KEY in local.properties)
- [ ] 15.2 `cd frontend/mobile && ./gradlew :app:testDebugUnitTest` passes (full suite from changes #1-#8)
- [ ] 15.3 `openspec validate ai-reflection-generation --strict` reports zero issues
- [ ] 15.4 Emulator smoke (en, with real key): complete a memory wizard → MemoryReflection → fill 3 fields → tap Polish → spinner appears for ~2-5s → English caption renders → tap Copy → "Copied!" feedback for 1.5s → paste into another app to verify → tap Save → returns to Memories
- [ ] 15.5 Emulator smoke (zh): toggle locale → repeat → caption renders in Traditional Chinese
- [ ] 15.6 Emulator smoke (no network): toggle airplane mode → tap Polish → error chip appears with `network error` message + retry icon → toggle off airplane → tap retry → caption renders
- [ ] 15.7 Emulator smoke (bad key): set `DEEPSEEK_API_KEY=sk-invalid` in local.properties → rebuild → tap Polish → error chip says `API key invalid`
- [ ] 15.8 Verify Room: `adb shell run-as com.mcis.memoir sqlite3 databases/memoir.db "SELECT generatedReflection FROM memories LIMIT 1"` returns the AI text after a Save
- [ ] 15.9 Record Koin-change follow-up obligation: "Koin change MUST delete `MemoirApplication.Companion.reflectionClient` and replace `viewModel(factory = ReflectionViewModelFactory(reflectionClient = MemoirApplication.reflectionClient, ...))` with `koinViewModel { parametersOf(memoryId) }` plus `single<ReflectionClient> { DeepSeekReflectionClient(get<OpenAI>()) }` + `single<OpenAI> { OpenAI(OpenAIConfig(BuildConfig.DEEPSEEK_API_KEY, ...)) }` bindings"
