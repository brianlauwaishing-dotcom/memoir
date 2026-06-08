## Why

Umbrella §2 line 8: "DeepSeek call + retry / error UX (decoupled from #7 via interface)". Change #7 (`memory-creation-flow`) shipped the Reflection screen with three text fields (mood / insights / feedback) persisted to Room, but left the "Polish with AI" button as a `Log.w` stub and `MemoryEntity.generatedReflection` is never written. The DeepSeek API key, OpenAI-compatible client, and the prompt template all need to land so the screen's stub becomes a working flow: tap Polish → spinner → AI-generated share-ready caption appears below the input fields → user can Copy, Regenerate, or Save (which persists the AI text to Room).

This is the only change in the umbrella that opens an outbound network connection. Per umbrella §5.3 the API key is embedded in BuildConfig via `local.properties` — acceptable risk for the academic-MVP demo, with explicit rotation guidance if the APK leaks.

## What Changes

- Add `<uses-permission android:name="android.permission.INTERNET" />` to `AndroidManifest.xml`.
- Add the DeepSeek API key plumbing per umbrella §5.3:
  - `local.properties` (already gitignored) gets a `DEEPSEEK_API_KEY=sk-…` entry (designer / engineer's local secret — never committed). Existing repo already has `local.properties` per a Glob check; this change adds the canonical example via a NEW committed `local.properties.example` template file showing `DEEPSEEK_API_KEY=replace-me-with-real-key` so onboarding engineers know what to add.
  - `app/build.gradle.kts` reads `DEEPSEEK_API_KEY` from `local.properties` and writes it as a `buildConfigField("String", "DEEPSEEK_API_KEY", "\"${value ?: ""}\"")`. `buildFeatures.buildConfig = true` enabled.
- Add Gradle catalog entries for `com.aallam.openai:openai-client` (v4.0.1 per umbrella §5.1) and `io.ktor:ktor-client-cio` (v3.0.0 per umbrella §5.1) — these versions are placeholders to verify at implementation time. `kotlinx-serialization-json` and `kotlinx-coroutines-core` (added by changes #1 and #2) are already on the catalog.
- Introduce `com.mcis.memoir.data.llm.ReflectionClient` (interface) per umbrella §5.4-§5.6, with `DeepSeekReflectionClient` (impl) using the `com.aallam.openai:openai-client` `OpenAI` client configured with `OpenAIHost(baseUrl = "https://api.deepseek.com/v1/")` and `model = ModelId("deepseek-chat")`.
- Introduce `com.mcis.memoir.data.llm.PromptBuilder` (pure-Kotlin object) per umbrella §5.5, with `build(input: JourneyReflectionInput): List<ChatMessage>` returning a system + user message pair. The prompt asks DeepSeek to produce a 2–4-sentence share-ready caption (Instagram / Threads target) IN the language matching `input.locale.language` (en or zh), with tone calibrated by `input.templateStyle` (`old_street` → nostalgic, `city_walk` → light/curious, `taiwan_pop` → vibrant/playful, `heritage_arch` → reflective/historical).
- Introduce `data/llm/JourneyReflectionInput.kt` + `SpotEntry.kt` matching umbrella §5.5 verbatim.
- Introduce `data/llm/ReflectionResult.kt` + `ReflectionError.kt` matching umbrella §5.6 verbatim — sealed `Success(text)` / `Failure(kind, cause?)` with `ReflectionError.{Network, InvalidApiKey, RateLimited, ServiceUnavailable, Unexpected}`.
- Introduce `data/llm/JourneyInputAssembler.kt` — a small helper that builds a `JourneyReflectionInput` from a `Memory` + `Locale` + (optional) `Route`/`Spot` lookups via `ContentRepository`. The Reflection VM uses this to assemble the prompt input.
- Extend `MemoirApplication.kt`: construct the `OpenAI` client + `DeepSeekReflectionClient` as a `companion object lateinit var reflectionClient: ReflectionClient`. Reuses the staging-shortcut pattern from changes #1/#2/#7.
- Replace change #7's stub `ReflectionViewModel` with a richer state machine:
  - State adds `aiState: AiState` (sealed: `Idle | Generating | Ready(text: String) | Error(kind: ReflectionError, message: String)`)
  - New intents: `PolishClicked`, `RegenerateClicked`, `CopyClicked`, `DismissAiError`
  - New effects: `CopyToClipboard(text: String)` (Composable bridges to system `ClipboardManager`)
  - `SaveClicked` reducer now also persists `generatedReflection = state.aiState.textOrNull()` into Room — if user tapped Polish + got a caption AND tapped Save, the caption is preserved in the row.
  - `PolishClicked` / `RegenerateClicked` both invoke `ReflectionClient.generate(JourneyInputAssembler.build(memory, locale, content))`; both serialize via a `Mutex` so rapid taps don't fire two concurrent network calls.
  - Loading / error UI rendered in the Composable: spinner while `aiState is Generating`; error chip with retry icon + message when `aiState is Error`; the maroon Polish-with-AI button shows "Regenerate" copy and an undo-circle icon when `aiState is Ready`.
- Add `memory_reflection_generating`, `memory_reflection_copied` strings + the four `ReflectionError`-variant messages (`error_network`, `error_invalid_api_key`, `error_rate_limited`, `error_service_unavailable`, `error_unexpected`) to `values/strings.xml` and `values-zh/strings.xml`.
- Add a `.github/workflows/mobile-ci.yml` step (creates the file if absent — check if change #1 already created one) that pre-creates `local.properties` with `DEEPSEEK_API_KEY=dummy-ci-key` before running `:app:assembleDebug` and `:app:testDebugUnitTest`. Tests MUST NOT issue real network calls — the test stack mocks `ReflectionClient`, never the underlying `OpenAI` client, and never makes a real DeepSeek call.
- **Not in scope**:
  - Server-side proxy for API key — umbrella §5.3 explicitly defers this; this change ships with the embedded-key risk acknowledged.
  - Streaming output — umbrella §12 explicitly defers; one-shot completion with a "Generating…" spinner is the UX.
  - Retry-with-backoff — single user-driven retry via the Regenerate button is sufficient for MVP.
  - LLM rate-limit caching (idempotency) — each Polish/Regenerate tap is a fresh API call.
  - Editing the AI output inline (the generated text is read-only in the screen; users who want to edit copy it then paste into a chat / post elsewhere).
  - LLM-based language detection — the prompt instructs DeepSeek to output in the input locale's language; we trust the model.
  - Multi-model fallback (e.g. Gemini if DeepSeek fails) — out of scope; failure surfaces to user via error chip.
  - `MemoriesScreen` displaying the AI-generated caption — change #9 owns the Memory list rendering.
  - Per-spot text entry (the `spotNotes` Room column stays empty/`"{}"`); the prompt input populates `SpotEntry` only with `(spotId, userNote = null, photoCount)` derived from the photo count split heuristically — see design D4.

## Capabilities

### New Capabilities
- `ai-reflection-generation`: The DeepSeek-backed caption generation feature — the `ReflectionClient` interface + `DeepSeekReflectionClient` impl, the `PromptBuilder` template, the `JourneyInputAssembler`, the BuildConfig + `local.properties` key plumbing, the INTERNET permission, the AI state machine in `ReflectionViewModel`, the loading / error / ready / copy / regenerate UI on `MemoryReflectionScreen`, and the CI dummy-key step.

### Modified Capabilities
<!-- None — `content-pipeline`, `language-toggle`, `home-discovery`, `route-bookmarking`, `artifact-discovery-flow`, `artifact-photo-capture`, `memory-creation-flow` are foundations consumed unchanged. -->

## Impact

- **New files**:
  - `frontend/mobile/app/src/main/java/com/mcis/memoir/data/llm/ReflectionClient.kt` (interface)
  - `data/llm/DeepSeekReflectionClient.kt` (impl)
  - `data/llm/JourneyReflectionInput.kt`, `SpotEntry.kt`, `ReflectionResult.kt`, `ReflectionError.kt`
  - `data/llm/PromptBuilder.kt`
  - `data/llm/JourneyInputAssembler.kt`
  - `local.properties.example` (at repo root or `frontend/mobile/`)
  - `.github/workflows/mobile-ci.yml` (or extension to an existing workflow created by change #1)
  - Tests: `PromptBuilderTest.kt`, `JourneyInputAssemblerTest.kt`, `DeepSeekReflectionClientTest.kt` (mocks the `OpenAI` client SDK calls — see design D5 for the seam), `ReflectionViewModelAiTest.kt` (extends change #7's VM test with AI-state coverage)
- **Modified files**:
  - `frontend/mobile/app/src/main/AndroidManifest.xml` — add `<uses-permission android:name="android.permission.INTERNET" />`.
  - `frontend/mobile/app/build.gradle.kts` — add `buildFeatures.buildConfig = true`, read `DEEPSEEK_API_KEY` from `local.properties`, declare `buildConfigField`; add `implementation(libs.openai.client)` + `implementation(libs.ktor.client.cio)`.
  - `frontend/mobile/gradle/libs.versions.toml` — add `openai-kotlin`, `ktor` versions + library aliases.
  - `frontend/mobile/app/src/main/java/com/mcis/memoir/MemoirApplication.kt` — add `lateinit var reflectionClient: ReflectionClient` companion field; construct in `onCreate()`.
  - `frontend/mobile/app/src/main/java/com/mcis/memoir/MemoryReflectionScreen.kt` — render loading / error / ready states; wire Polish / Regenerate / Copy clicks; add new visual chrome (spinner, error chip, copy feedback toast/snackbar substitute via flash text).
  - `frontend/mobile/app/src/main/java/com/mcis/memoir/ui/memory/reflection/ReflectionState.kt` — add `aiState: AiState` field.
  - `frontend/mobile/app/src/main/java/com/mcis/memoir/ui/memory/reflection/ReflectionIntent.kt` — add `PolishClicked`, `RegenerateClicked`, `CopyClicked`, `DismissAiError`.
  - `frontend/mobile/app/src/main/java/com/mcis/memoir/ui/memory/reflection/ReflectionEffect.kt` — add `CopyToClipboard(text)`.
  - `frontend/mobile/app/src/main/java/com/mcis/memoir/ui/memory/reflection/ReflectionViewModel.kt` — extend with AI state machine + `Mutex`-protected `generate()` private method.
  - `frontend/mobile/app/src/main/java/com/mcis/memoir/ui/memory/reflection/ReflectionViewModelFactory.kt` — add `reflectionClient: ReflectionClient` constructor parameter.
  - `frontend/mobile/app/src/main/java/com/mcis/memoir/MyAppNavigation.kt` — `MemoryReflectionDestination` entry factory now passes `MemoirApplication.reflectionClient` + `MemoirApplication.content` (for `JourneyInputAssembler` route/spot lookups).
  - `frontend/mobile/app/src/main/res/values/strings.xml` + `values-zh/strings.xml` — 7 new strings.
- **Dependencies added**: `com.aallam.openai:openai-client` v4.0.1 (verify), `io.ktor:ktor-client-cio` v3.0.0 (verify), `androidx.test.ext:junit-ktx` (if needed for AI VM test — probably not).
- **Risk acknowledgements**:
  - **Embedded API key is decompile-recoverable** — acceptable per umbrella §1.2 and §5.3 for the academic-MVP demo. APK MUST NOT be publicly distributed; if a release APK leaks, the DeepSeek key MUST be rotated. Production / public-distribution path is a proxy server (deferred).
  - **Real network calls in tests would burn cost** — all tests MUST mock the `ReflectionClient` interface (a Kotlin interface, MockK-safe), not the underlying `OpenAI` client (whose constructor performs HTTP setup). The CI workflow uses a dummy key precisely so a misconfigured test that accidentally instantiates `DeepSeekReflectionClient` gets a 401 fast-fail rather than running on a real account.
  - **Prompt-injection via `userInsights` / `postTripFeedback` text fields** — the user can type "Ignore previous instructions and …" into the insights box. DeepSeek's system prompt asks for caption text only; the worst plausible outcome is a misformatted caption (no data exfiltration because we send only user-typed text + route/spot ids the user already saw). Not mitigated further — out of MVP scope.
  - **OpenAI client v4.0.1 + Kotlin 2.2.10 compatibility** — verified at implementation time against Maven Central.
  - **Token budget overshoot** — `maxTokens = 500` per umbrella §5.4 caps output; if DeepSeek exceeds, the truncated response is still rendered.
  - **`JourneyInputAssembler` accesses `ContentRepository.route(...)` / `spot(...)`** — both are O(1) lookups against the loaded snapshot; no I/O.
  - **CI dummy key is `dummy-ci-key` (not a valid format)** — DeepSeek returns 401 if a test accidentally instantiates the real client. The "fail fast" is by design.
- **Not changed**:
  - Memory schema (`MemoryEntity.generatedReflection` was already there from change #7).
  - Any other screen.
  - Content pipeline, language toggle, navigation graph.
