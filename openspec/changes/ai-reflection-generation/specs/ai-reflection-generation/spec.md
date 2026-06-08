## ADDED Requirements

### Requirement: ReflectionClient is an interface backed by DeepSeekReflectionClient

`com.mcis.memoir.data.llm.ReflectionClient` SHALL be an interface with `suspend fun generate(input: JourneyReflectionInput): ReflectionResult`. `com.mcis.memoir.data.llm.DeepSeekReflectionClient` SHALL implement it by invoking the `com.aallam.openai:openai-client` SDK's `OpenAI.chatCompletion(...)` with `ModelId("deepseek-chat")`, `temperature = 0.8`, `maxTokens = 500`, and a base URL of `https://api.deepseek.com/v1/`.

#### Scenario: Successful API response yields ReflectionResult.Success
- **WHEN** `mockk<OpenAI>()` is configured so `coEvery { openAI.chatCompletion(any()) } returns chatResp(message = "Sunny day in Tainan")` and `DeepSeekReflectionClient(openAI).generate(anyInput)` is invoked
- **THEN** the call returns `ReflectionResult.Success("Sunny day in Tainan")`

#### Scenario: Blank API response yields ReflectionResult.Failure(Unexpected)
- **WHEN** the mocked OpenAI returns an empty content string
- **THEN** the call returns `ReflectionResult.Failure(kind = ReflectionError.Unexpected, cause = null)`

#### Scenario: AuthenticationException maps to InvalidApiKey
- **WHEN** the mocked OpenAI throws `com.aallam.openai.api.exception.AuthenticationException(...)` (the SDK's typed 401 subclass)
- **THEN** the call returns `ReflectionResult.Failure(kind = ReflectionError.InvalidApiKey, cause = <the AuthenticationException>)`

#### Scenario: RateLimitException maps to RateLimited
- **WHEN** the mocked OpenAI throws `com.aallam.openai.api.exception.RateLimitException(...)` (the SDK's typed 429 subclass)
- **THEN** the call returns `ReflectionResult.Failure(kind = ReflectionError.RateLimited, ...)`

#### Scenario: OpenAIServerException maps to ServiceUnavailable
- **WHEN** the mocked OpenAI throws `com.aallam.openai.api.exception.OpenAIServerException(...)` (the SDK's typed 5xx subclass)
- **THEN** the call returns `ReflectionResult.Failure(kind = ReflectionError.ServiceUnavailable, ...)`

#### Scenario: OpenAITimeoutException maps to Network
- **WHEN** the mocked OpenAI throws `com.aallam.openai.api.exception.OpenAITimeoutException(...)`
- **THEN** the call returns `ReflectionResult.Failure(kind = ReflectionError.Network, ...)`

#### Scenario: OpenAIHttpException maps to Network
- **WHEN** the mocked OpenAI throws `com.aallam.openai.api.exception.OpenAIHttpException(...)` (transport-level error, e.g. connection refused)
- **THEN** the call returns `ReflectionResult.Failure(kind = ReflectionError.Network, ...)`

#### Scenario: Raw IOException maps to Network
- **WHEN** the mocked OpenAI throws a raw `java.io.IOException("connection refused")` (escaping the SDK's normal wrapping)
- **THEN** the call returns `ReflectionResult.Failure(kind = ReflectionError.Network, ...)`

#### Scenario: Other exception types map to Unexpected
- **WHEN** the mocked OpenAI throws `IllegalStateException("???")`
- **THEN** the call returns `ReflectionResult.Failure(kind = ReflectionError.Unexpected, ...)`

### Requirement: PromptBuilder produces a 2-message conversation with locale-aware output directive

`com.mcis.memoir.data.llm.PromptBuilder.build(input)` SHALL return exactly two `ChatMessage` entries: a `ChatRole.System` message containing the language directive AND tone hint, followed by a `ChatRole.User` message containing the journey context (routeId, per-spot entries, mood, insights, feedback).

#### Scenario: English output for English locale
- **WHEN** `PromptBuilder.build(input.copy(locale = Locale.ENGLISH))` is invoked
- **THEN** `result.size == 2` AND `result[0].role == ChatRole.System` AND `result[0].content` contains the substring `"English"`

#### Scenario: Traditional Chinese output for zh locale
- **WHEN** `PromptBuilder.build(input.copy(locale = Locale("zh")))` is invoked
- **THEN** `result[0].content` contains the substring `"Traditional Chinese"` (with `繁體中文` parenthetical reinforcement)

#### Scenario: Template style maps to tone hint
- **WHEN** four invocations are made with `templateStyle in setOf("old_street", "city_walk", "taiwan_pop", "heritage_arch")`
- **THEN** each invocation's system content contains a distinct tone descriptor (`nostalgic`, `light`, `vibrant`, `reflective` respectively)

#### Scenario: User content carries spotEntries
- **WHEN** `PromptBuilder.build(input)` is invoked with `spotEntries = listOf(SpotEntry("grand_mazu", null, 3), SpotEntry("anping_kaitai", null, 2))`
- **THEN** `result[1].content` contains both `"grand_mazu"` and `"anping_kaitai"` AND the per-stop photo counts `"photos=3"` and `"photos=2"`

#### Scenario: Blank userInsights renders as "(none)"
- **WHEN** `PromptBuilder.build(input.copy(userInsights = ""))` is invoked
- **THEN** `result[1].content` contains `"User reflection: (none)"` — explicit placeholder so the model doesn't see a dangling empty line

### Requirement: JourneyInputAssembler builds JourneyReflectionInput from Memory + ContentRepository

`com.mcis.memoir.data.llm.JourneyInputAssembler.build(memory, locale, contentRepo)` SHALL return a `JourneyReflectionInput` with `templateStyle = memory.templateId`, `userInsights = memory.userInsights`, `overallMood = memory.overallMood`, `postTripFeedback = memory.postTripFeedback`, `locale = locale`, and `routeId = memory.routeId ?: "(none)"`. `spotEntries` SHALL be derived from `contentRepo.route(memory.routeId)?.journey` (empty list if the route has no journey OR memory has no routeId), with each entry's `photoCount = memory.photoRelativePaths.size / max(route.journey.size, 1)` (integer division, denominator clamped to ≥ 1 to defend against a zero-stop edge case) and `userNote = null`. Remainder photos from the integer division are silently lost — see lossy-distribution scenario; per MVP scope this is acceptable.

#### Scenario: Route exists and photoCount is evenly split
- **WHEN** `memory.routeId = "sounds_of_temple"`, the route has 3 journey stops, and `memory.photoRelativePaths` has 6 entries
- **THEN** `spotEntries.size == 3` AND every entry's `photoCount == 2` AND every entry's `userNote == null`

#### Scenario: routeId is null — free-form memory
- **WHEN** `memory.routeId == null`
- **THEN** the returned input has `routeId == "(none)"` AND `spotEntries.isEmpty()`

#### Scenario: photoRelativePaths is empty
- **WHEN** the route has 3 stops but `memory.photoRelativePaths.isEmpty()`
- **THEN** every entry's `photoCount == 0`

#### Scenario: photoCount integer division silently drops remainder photos
- **WHEN** the route has 3 stops and `memory.photoRelativePaths` has 5 entries (integer division: 5/3 = 1, remainder 2 dropped)
- **THEN** every entry's `photoCount == 1` (NOT distributed as `[2, 2, 1]` or any other remainder-aware scheme) — accepted as MVP heuristic; future work could add a `MemoryPhoto(memoryId, spotId, ...)` per-spot attribution schema

#### Scenario: fewer photos than stops — every entry counts zero
- **WHEN** the route has 3 stops and `memory.photoRelativePaths` has 1 entry
- **THEN** every entry's `photoCount == 0` (1/3 = 0 in integer division — the one photo is dropped from the prompt's per-stop counts; the user prompt's `"sumOf { it.photoCount }"` line will show 0, NOT 1 — a documented inconsistency between the per-stop sum and the actual photo count)

### Requirement: ReflectionViewModel state machine includes Idle/Generating/Ready/Error AI states

`ReflectionViewModel.state.aiState` SHALL be a `sealed interface AiState` with cases `Idle`, `Generating`, `Ready(text: String)`, `Error(kind: ReflectionError, message: String)`. `PolishClicked` and `RegenerateClicked` SHALL transition `aiState` from any prior state through `Generating` to either `Ready(text)` or `Error(kind, message)`. Concurrent Polish/Regenerate invocations MUST be serialized via a `Mutex` so only one in-flight `ReflectionClient.generate(...)` call exists per ViewModel instance.

#### Scenario: PolishClicked transitions through Generating to Ready
- **WHEN** the VM is in `aiState = Idle`, a `runTest` Turbine collector watches `state`, and the mocked `ReflectionClient.generate(...)` returns `Success("Generated caption")`
- **THEN** the collector observes a `Generating` state THEN a `Ready("Generated caption")` state, in that order

#### Scenario: Rapid double-tap of Polish fires exactly one API call
- **WHEN** `PolishClicked` is fired twice in rapid succession against a `coEvery { reflectionClient.generate(any()) } coAnswers { delay(100); Success("text") }` slow mock, with the second tap dispatching while the first is still in flight
- **THEN** `coVerify(exactly = 1) { reflectionClient.generate(any()) }` — the early-return guard at the top of `generate()` (which checks `aiState is Generating`) drops the second tap entirely; the mutex below the guard is belt-and-suspenders against rare interleavings where the guard misses

#### Scenario: PolishClicked while aiState is Generating is a no-op
- **WHEN** the VM has explicitly transitioned to `aiState = Generating` and `PolishClicked` is fired again
- **THEN** no additional `reflectionClient.generate(...)` call is made AND no `aiState` transition is observed — the early-return guard fires immediately

#### Scenario: Sequential Polish + Regenerate fires two distinct API calls
- **WHEN** `PolishClicked` completes successfully (`aiState = Ready(text)`), then `RegenerateClicked` is fired
- **THEN** `coVerify(exactly = 2) { reflectionClient.generate(any()) }` — distinct user-initiated generations DO produce separate API calls; only IN-FLIGHT taps are absorbed

#### Scenario: API failure transitions to Error
- **WHEN** the mocked client returns `Failure(InvalidApiKey, cause)` and PolishClicked is fired from Idle
- **THEN** the final `aiState` is `Error(kind = InvalidApiKey, message = <resource string for error_invalid_api_key>)`

### Requirement: SaveClicked persists generated reflection when aiState is Ready

`ReflectionIntent.SaveClicked` reducer SHALL invoke `MemoryRepository.updateGeneratedReflection(memoryId, text)` exactly once before `complete(memoryId)` IF and ONLY IF `state.aiState is AiState.Ready`. When `aiState` is anything else, `updateGeneratedReflection` MUST NOT be invoked and the Memory row's `generatedReflection` column stays at its current value (typically null).

#### Scenario: Save while aiState is Ready persists the caption
- **WHEN** `state.aiState = AiState.Ready("Caption text")` and `SaveClicked` is fired
- **THEN** `coVerify(exactly = 1) { repo.updateGeneratedReflection(memoryId, "Caption text") }` AND `coVerify(exactly = 1) { repo.complete(memoryId) }`

#### Scenario: Save while aiState is Idle does not touch generatedReflection
- **WHEN** `state.aiState = AiState.Idle` and `SaveClicked` is fired
- **THEN** `coVerify(exactly = 0) { repo.updateGeneratedReflection(any(), any()) }` AND `coVerify(exactly = 1) { repo.complete(memoryId) }`

#### Scenario: Save while aiState is Error does not touch generatedReflection
- **WHEN** `state.aiState = AiState.Error(...)` and `SaveClicked` is fired
- **THEN** `coVerify(exactly = 0) { repo.updateGeneratedReflection(any(), any()) }`

### Requirement: CopyClicked emits CopyToClipboard effect only when aiState is Ready

`ReflectionIntent.CopyClicked` reducer SHALL emit `ReflectionEffect.CopyToClipboard(text)` IF and ONLY IF `state.aiState is AiState.Ready(text)`. When `aiState` is anything else, the intent is a no-op.

#### Scenario: Copy while Ready emits the clipboard effect
- **WHEN** `state.aiState = AiState.Ready("Hello")` and `CopyClicked` is fired
- **THEN** the effects Flow emits exactly one `CopyToClipboard("Hello")`

#### Scenario: Copy while not Ready is a no-op
- **WHEN** `state.aiState = AiState.Generating` and `CopyClicked` is fired
- **THEN** the effects Flow emits zero `CopyToClipboard` items

### Requirement: DismissAiError resets aiState to Idle

`ReflectionIntent.DismissAiError` SHALL transition `aiState` from `Error(...)` to `Idle`. From any other prior state, the intent is a no-op.

#### Scenario: Dismiss from Error returns to Idle
- **WHEN** `state.aiState = AiState.Error(Network, "...")` and `DismissAiError` is fired
- **THEN** the next emitted state has `aiState == AiState.Idle`

#### Scenario: Dismiss from Idle is a no-op
- **WHEN** `state.aiState = AiState.Idle` and `DismissAiError` is fired
- **THEN** no state transition occurs

### Requirement: MemoryReflectionScreen renders four distinct UI states

`MemoryReflectionScreen` SHALL render distinct UI for each of the four `aiState` cases:
- `Idle`: a "Polish with AI" button (maroon, label from `R.string.memory_reflection_polish_ai`) visible.
- `Generating`: the button disabled with a `CircularProgressIndicator` overlay.
- `Ready(text)`: a card displaying `text` below the input fields, plus a row with [Copy button] and [Regenerate button].
- `Error(kind, message)`: an error chip rendered below the input fields containing `message` and a retry icon; chip is dismissable.

The Composable SHALL bridge `ReflectionEffect.CopyToClipboard(text)` to the system clipboard via `context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager` + `ClipData.newPlainText("memoir-reflection", text)`.

#### Scenario: Screen renders Polish button in Idle state
- **WHEN** the Composable is rendered with `state.aiState == AiState.Idle`
- **THEN** a Composable matching the Polish-with-AI button is present in the rendered tree, AND no spinner / card / error chip is rendered

#### Scenario: Screen renders spinner overlay in Generating state
- **WHEN** the Composable is rendered with `state.aiState == AiState.Generating`
- **THEN** a `CircularProgressIndicator` is present, AND the Polish button is disabled

#### Scenario: Screen renders Copy and Regenerate buttons in Ready state
- **WHEN** the Composable is rendered with `state.aiState == AiState.Ready("text")`
- **THEN** the text card is rendered with content `"text"`, AND both [Copy] and [Regenerate] buttons are present

### Requirement: INTERNET permission and DEEPSEEK_API_KEY BuildConfig field land

`AndroidManifest.xml` SHALL declare `<uses-permission android:name="android.permission.INTERNET" />`. `app/build.gradle.kts` SHALL enable `buildFeatures.buildConfig = true` AND declare a `buildConfigField("String", "DEEPSEEK_API_KEY", "\"${localProps[\"DEEPSEEK_API_KEY\"] ?: \"\"}\"")` that reads from `local.properties`. A new `local.properties.example` file SHALL be committed at the project root to document the variable.

#### Scenario: Manifest has INTERNET permission
- **WHEN** the repo is inspected after this change lands
- **THEN** `AndroidManifest.xml` contains exactly one `<uses-permission android:name="android.permission.INTERNET" />` entry

#### Scenario: BuildConfig field is declared
- **WHEN** `app/build.gradle.kts` is inspected after this change lands
- **THEN** the file contains `buildConfigField("String", "DEEPSEEK_API_KEY", …)` AND `buildFeatures { buildConfig = true }` AND a `local.properties`-reading block above the `android { … }` block

#### Scenario: local.properties.example documents the variable
- **WHEN** the repo is inspected after this change lands
- **THEN** `local.properties.example` exists at the documented location AND contains `DEEPSEEK_API_KEY=replace-me-with-real-key` AND a comment explaining the rotation policy

### Requirement: CI workflow pre-creates local.properties with a dummy key

The mobile CI workflow SHALL write `DEEPSEEK_API_KEY=dummy-ci-key` to `frontend/mobile/local.properties` BEFORE running `:app:assembleDebug` or `:app:testDebugUnitTest`. This ensures the BuildConfig field has a (non-functional) value at compile time AND that any test accidentally hitting the real client fails fast with a 401 rather than succeeding on a real account.

#### Scenario: CI step is present
- **WHEN** `.github/workflows/mobile-ci.yml` is inspected after this change lands
- **THEN** at least one job step writes `DEEPSEEK_API_KEY=dummy-ci-key` to `frontend/mobile/local.properties` BEFORE the Gradle invocation

#### Scenario: Unit tests never use the real OpenAI client
- **WHEN** the test sources are inspected after this change lands
- **THEN** no test file constructs `OpenAI(...)` or `DeepSeekReflectionClient(realOpenAi)` against a real config — every VM test mocks the `ReflectionClient` interface, and every client test mocks `OpenAI` via `mockk<OpenAI>()`
