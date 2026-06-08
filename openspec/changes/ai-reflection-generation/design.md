## Context

The only outbound-network change in the umbrella. Consumes:
- `tainan-route-content-pipeline` change #1: `ContentRepository.route(id)` / `spot(id)` for assembling per-spot context into the prompt input.
- `language-toggle` change #2: `LocaleController.currentLocale()` for choosing the output language.
- `home-discovery` change #3: JUnit5 + MockK + Turbine test stack.
- `route-bookmarking` change #4: `Mutex`-protected reducer precedent (for serializing rapid Polish/Regenerate taps).
- `artifact-discovery-flow` change #5: nothing directly.
- `artifact-photo-capture` change #6: nothing directly (photo count comes from `MemoryEntity.photoLocalPaths`, not from captured Uris).
- `memory-creation-flow` change #7: `MemoryRepository`, `MemoryEntity.generatedReflection`, `ReflectionViewModel`, `JourneyReflectionInput.spotEntries`-shaped Memory data.

**Current state**:
- `MemoryReflectionScreen.kt:41-42` declares `var reflectionText` + `var aiPolishedText` as Compose state, but the legacy file (pre-change-#7 rewrite) has no AI plumbing. After change #7 lands, the screen is VM-driven with three text fields and a no-op Polish button (`Log.w("memory-creation-flow", "AI polish coming in change #8")`).
- No `INTERNET` permission in `AndroidManifest.xml`.
- No `openai-client` or `ktor-client-cio` in catalog.
- `BuildConfig` not yet enabled in `app/build.gradle.kts`.
- `local.properties` exists (per Glob check) but is gitignored; no committed example template.

**Constraints**:
- Cannot precede change #7 (depends on `ReflectionViewModel`, `MemoryRepository`, `MemoryEntity.generatedReflection`).
- Must run on Kotlin 2.2.10 + AGP 9.1.1 + JDK 11 target (per `app/build.gradle.kts:34-35`).
- The `OpenAI` client from `com.aallam.openai:openai-client` is a concrete `OpenAI(config)` constructor that performs HTTP client setup at instantiation — must NOT be instantiated in unit tests.
- Test stack is JUnit5 + MockK + Turbine; the `ReflectionClient` SEAM is the interface to mock, not the SDK.

## Goals / Non-Goals

**Goals:**
1. Tapping Polish-with-AI on `MemoryReflectionScreen` makes a real DeepSeek API call and renders the generated caption below the input fields.
2. Errors (network / 401 / 429 / 5xx / other) surface to the user as a localized error chip with retry affordance.
3. Save persists the generated caption into Room (`MemoryEntity.generatedReflection`).
4. Copy puts the caption on the system clipboard.
5. Regenerate makes a fresh API call (no caching).
6. The API key plumbing (BuildConfig + local.properties + CI dummy key) is in place.
7. Tests cover the prompt template + the VM state machine + the input assembler — all without instantiating the real OpenAI client.

**Non-Goals:**
- Streaming output (umbrella §12 defers).
- Server-side proxy for API key (umbrella §5.3 defers).
- Retry-with-backoff (one user-driven retry button is sufficient).
- Multi-model fallback (Gemini, Anthropic).
- Caching successful generations (each tap is a fresh call — user-controllable cost is the user's call).
- Inline editing of the AI output.
- Per-spot text entry into `spotNotes` (the prompt sends `userNote = null` for every spot — see D4).
- Telemetry / cost tracking.
- Prompt-injection sanitization.

## Decisions

### D1. `ReflectionClient` is an interface; `DeepSeekReflectionClient` is the impl

```kotlin
interface ReflectionClient {
    suspend fun generate(input: JourneyReflectionInput): ReflectionResult
}

class DeepSeekReflectionClient(private val openAI: OpenAI) : ReflectionClient {
    override suspend fun generate(input: JourneyReflectionInput): ReflectionResult {
        val messages = PromptBuilder.build(input)
        return runCatching {
            val req = ChatCompletionRequest(
                model = ModelId("deepseek-chat"),
                messages = messages,
                temperature = 0.8,
                maxTokens = 500
            )
            openAI.chatCompletion(req).choices.first().message.content.orEmpty()
        }.fold(
            onSuccess = { text ->
                if (text.isBlank()) ReflectionResult.Failure(ReflectionError.Unexpected)
                else ReflectionResult.Success(text)
            },
            onFailure = { e -> ReflectionResult.Failure(mapError(e), e) }
        )
    }

    private fun mapError(e: Throwable): ReflectionError = when (e) {
        is AuthenticationException     -> ReflectionError.InvalidApiKey       // SDK's 401 mapping
        is RateLimitException          -> ReflectionError.RateLimited          // SDK's 429 mapping
        is OpenAIServerException       -> ReflectionError.ServiceUnavailable   // SDK's 5xx mapping
        is OpenAITimeoutException      -> ReflectionError.Network              // SDK's timeout mapping
        is OpenAIAPIException          -> ReflectionError.ServiceUnavailable   // other API errors (4xx not 401/429)
        is OpenAIHttpException         -> ReflectionError.Network              // non-API HTTP transport errors (connect refused, etc.)
        is IOException                 -> ReflectionError.Network              // raw Ktor IO failures that escape the SDK
        else                           -> ReflectionError.Unexpected
    }
}
```

**Why interface + impl split (vs single class):**
- Unit tests mock the interface; never instantiate the real `OpenAI` client.
- Future Gemini / Anthropic implementations slot in without touching `ReflectionViewModel`.
- Same `interface + Impl` precedent as `ContentAssetLoader`, `UserPreferencesRepository`, `MemoryRepository`.

**SDK exception hierarchy** (`com.aallam.openai:openai-client` v4.0.1, package `com.aallam.openai.api.exception`):
- `OpenAIHttpException` is the umbrella transport-level exception
- `OpenAIAPIException` is the umbrella API-error exception with subclasses:
  - `AuthenticationException` (401)
  - `RateLimitException` (429)
  - `InvalidRequestException` (400)
  - `OpenAIServerException` (5xx)
  - `OpenAITimeoutException` (request timeout)

The `mapError` chain above uses `is`-checks on these subclasses (NOT a `statusCode` switch) because the SDK doesn't expose a generic `statusCode` property — type-based dispatch is the SDK's idiomatic way. The implementer MUST verify these exact class names against the installed v4.0.1 jar at implementation time; if the SDK has been renamed in a later patch, adjust accordingly.

### D2. `OpenAI` client construction in `MemoirApplication`

```kotlin
// MemoirApplication.onCreate()
val openAIConfig = OpenAIConfig(
    token = BuildConfig.DEEPSEEK_API_KEY,
    host = OpenAIHost(baseUrl = "https://api.deepseek.com/v1/"),
    timeout = Timeout(socket = 60.seconds)
)
val openAI = OpenAI(openAIConfig)
reflectionClient = DeepSeekReflectionClient(openAI)
```

`OpenAIConfig` parameter names per umbrella §5.4. Verify at implementation against the OpenAI client SDK (v4.0.1 expected). The `60.seconds` socket timeout matches umbrella §5.4.

**Why construct in `MemoirApplication` vs in the VM:**
- `OpenAI(config)` performs Ktor HTTP client setup; instantiating per-VM-construction would create one HTTP client per ReflectionViewModel instance. Application-scoped singleton matches change-#1/#2/#7 staging precedent.

**Empty `DEEPSEEK_API_KEY` BuildConfig field**: if `local.properties` lacks the entry (new dev environment), `BuildConfig.DEEPSEEK_API_KEY = ""`. The first `chatCompletion(...)` call will return a 401 → `ReflectionError.InvalidApiKey` → user sees "API key invalid — contact dev" message. No crash. The implementer's `local.properties.example` documents the variable.

### D3. `PromptBuilder` template

Hardcoded English system prompt that asks for output in the user's locale language:

```kotlin
object PromptBuilder {
    fun build(input: JourneyReflectionInput): List<ChatMessage> {
        val outputLang = if (input.locale.language == "zh") "Traditional Chinese (繁體中文)" else "English"
        val toneHint = when (input.templateStyle) {
            "old_street"     -> "nostalgic and warm, like a quiet memory of a small alley"
            "city_walk"      -> "light and curious, like a friendly travel diary entry"
            "taiwan_pop"     -> "vibrant and playful, full of energy"
            "heritage_arch"  -> "reflective and historically grounded"
            else              -> "warm and personal"
        }

        val system = ChatMessage(
            role = ChatRole.System,
            content = """
                You are a writing assistant for a Taiwan cultural-travel journaling app.
                The user has finished a tour and wants a 2-4 sentence caption suitable for sharing on Instagram or Threads.
                Output language: $outputLang. Tone: $toneHint.
                Output only the caption text — no preamble, no quotation marks, no hashtags unless they naturally fit.
                Keep it under 200 characters total.
            """.trimIndent()
        )

        val userParts = buildString {
            appendLine("Route id: ${input.routeId}")
            appendLine("Visited ${input.spotEntries.size} stops with ${input.spotEntries.sumOf { it.photoCount }} photos total.")
            input.spotEntries.forEachIndexed { i, entry ->
                appendLine("  Stop ${i + 1}: spotId=${entry.spotId}, photos=${entry.photoCount}${entry.userNote?.let { ", note=\"$it\"" }.orEmpty()}")
            }
            if (!input.overallMood.isNullOrBlank()) appendLine("Overall mood: ${input.overallMood}")
            appendLine("User reflection: ${input.userInsights.ifBlank { "(none)" }}")
            if (!input.postTripFeedback.isNullOrBlank()) appendLine("Post-trip thoughts: ${input.postTripFeedback}")
        }

        val user = ChatMessage(role = ChatRole.User, content = userParts)
        return listOf(system, user)
    }
}
```

**Why English system prompt vs Chinese:**
- DeepSeek-chat handles both directions fluently; English instruction text is more compact and avoids translation drift.
- The `outputLang` directive in the system prompt steers the model's response language deterministically (verified empirically with similar models).

**Why hardcoded template vs file-based prompt:**
- Single template; A/B testing isn't in scope.
- Compile-time string constants are testable as pure functions.

**Why `outputLang` literal mentions "Traditional Chinese (繁體中文)" not just "Chinese":**
- The dataset is Traditional Chinese; without the qualifier DeepSeek may emit Simplified.

**Why spotId raw IDs vs human names**:
- IDs are stable; resolving names per spot would require async lookups inside `PromptBuilder` (it's a pure object). The assembler (D4) resolves names BEFORE calling PromptBuilder if richer prompts become desirable — for MVP, raw ids are sufficient context.

### D4. `JourneyInputAssembler`

```kotlin
object JourneyInputAssembler {
    suspend fun build(
        memory: Memory,
        locale: Locale,
        contentRepo: ContentRepository
    ): JourneyReflectionInput {
        val routeId = memory.routeId ?: "(none)"
        val route = memory.routeId?.let { contentRepo.route(it) }
        val spotEntries = route?.journey.orEmpty().mapIndexed { i, stop ->
            val pic = memory.photoRelativePaths.size / max(route?.journey?.size ?: 1, 1)
            SpotEntry(
                spotId = stop.spotId,
                userNote = null,                       // memory.spotNotes is reserved for a future change
                photoCount = pic                        // evenly split for MVP — see Risks
            )
        }
        return JourneyReflectionInput(
            locale = locale,
            routeId = routeId,
            spotEntries = spotEntries,
            overallMood = memory.overallMood,
            userInsights = memory.userInsights,
            postTripFeedback = memory.postTripFeedback,
            templateStyle = memory.templateId
        )
    }
}
```

**Why even-split photo count:**
- Per-spot photo attribution isn't tracked today (memory has a flat `photoRelativePaths: List<String>`). Even-split is a defensible MVP heuristic.
- A future change could add `MemoryPhoto(memoryId, spotId, index, path)` rows; not in scope.

**Why `userNote = null`:**
- `Memory.spotNotes` was reserved by change #7 with no UI to populate it. Until a UI lands, the field stays empty `"{}"` JSON and the assembler sends `null` per spot.

**Why `routeId = "(none)"` for free-form memories:**
- Some memories may not be linked to a route (`memory.routeId == null`). The prompt's "Route id" line then says `(none)` and `spotEntries` is empty — the system prompt still asks for a caption, the model writes one from `userInsights` + `overallMood` alone. Tested for content emptiness (D6).

### D5. `ReflectionViewModel` AI state machine

```kotlin
sealed interface AiState {
    data object Idle : AiState
    data object Generating : AiState
    data class Ready(val text: String) : AiState
    data class Error(val kind: ReflectionError, val message: String) : AiState
}

data class ReflectionState(
    // existing fields from change #7:
    val isLoading: Boolean = true,
    val overallMood: String = "",
    val userInsights: String = "",
    val postTripFeedback: String = "",
    val isSaving: Boolean = false,
    val error: String? = null,
    // new:
    val aiState: AiState = AiState.Idle
)

sealed interface ReflectionIntent {
    // existing:
    data class MoodChanged(val text: String) : ReflectionIntent
    data class InsightsChanged(val text: String) : ReflectionIntent
    data class FeedbackChanged(val text: String) : ReflectionIntent
    data object SaveClicked : ReflectionIntent
    // new:
    data object PolishClicked : ReflectionIntent
    data object RegenerateClicked : ReflectionIntent
    data object CopyClicked : ReflectionIntent
    data object DismissAiError : ReflectionIntent
}

sealed interface ReflectionEffect {
    // existing:
    data object NavigateToMemoriesList : ReflectionEffect
    data class ShowError(val msg: String) : ReflectionEffect
    // new:
    data class CopyToClipboard(val text: String) : ReflectionEffect
}
```

The VM gains a `private val generateMutex = Mutex()` (per the `RouteDetailViewModel.toggleMutex` precedent) so concurrent Polish + Regenerate taps serialize.

```kotlin
private fun generate() {
    // Drop the request entirely if a generation is already in flight.
    // The Mutex below serializes correctness on the rare interleavings the
    // guard misses (e.g. concurrent intent dispatch); without this guard,
    // every queued tap would still produce a fresh network call after the
    // current one releases the mutex.
    if (_state.value.aiState is AiState.Generating) return
    viewModelScope.launch {
        generateMutex.withLock {
            // Re-check inside the lock: another coroutine that acquired the
            // mutex before us may have already finished a generation; we want
            // exactly one in-flight call across both guards.
            if (_state.value.aiState is AiState.Generating) return@withLock
            _state.update { it.copy(aiState = AiState.Generating) }
            val memory = repo.getOnce(memoryId) ?: run {
                _state.update { it.copy(aiState = AiState.Error(ReflectionError.Unexpected, resources.getString(R.string.error_unexpected))) }
                return@withLock
            }
            val input = JourneyInputAssembler.build(memory, localeProvider(), contentRepo)
            val result = reflectionClient.generate(input)
            when (result) {
                is ReflectionResult.Success -> _state.update { it.copy(aiState = AiState.Ready(result.text)) }
                is ReflectionResult.Failure -> _state.update {
                    it.copy(aiState = AiState.Error(result.kind, resources.getString(result.kind.messageRes())))
                }
            }
        }
    }
}

private fun ReflectionError.messageRes(): Int = when (this) {
    ReflectionError.Network            -> R.string.error_network
    ReflectionError.InvalidApiKey      -> R.string.error_invalid_api_key
    ReflectionError.RateLimited        -> R.string.error_rate_limited
    ReflectionError.ServiceUnavailable -> R.string.error_service_unavailable
    ReflectionError.Unexpected         -> R.string.error_unexpected
}
```

`PolishClicked` and `RegenerateClicked` both call `generate()` — they're semantically identical, just visually distinct (the button label flips based on `aiState`). Could fuse into one intent but the umbrella explicitly mentioned Regenerate (per `references/guideline.md:64`) — keeping both makes the design surface match the UX vocabulary.

`SaveClicked` reducer (from change #7) extends to write `generatedReflection`:

```kotlin
ReflectionIntent.SaveClicked -> viewModelScope.launch {
    _state.update { it.copy(isSaving = true) }
    val s = _state.value
    val aiText = (s.aiState as? AiState.Ready)?.text
    runCatching {
        repo.updateReflection(memoryId, s.overallMood.ifBlank { null }, s.userInsights, s.postTripFeedback.ifBlank { null })
        if (aiText != null) repo.updateGeneratedReflection(memoryId, aiText)
        repo.complete(memoryId)
    }.fold(
        onSuccess = { completionConfirmed = true; _effects.send(NavigateToMemoriesList) },
        onFailure = { e -> _state.update { it.copy(isSaving = false, error = e.message) } }
    )
}
```

`updateGeneratedReflection(memoryId, text)` is a new method on `MemoryRepository` — see D8.

### D6. UI rendering states

```
aiState == Idle:
  Polish-with-AI button (maroon, label R.string.memory_reflection_polish_ai)

aiState == Generating:
  Polish-with-AI button disabled with CircularProgressIndicator overlay
  (no "Generating…" text — the spinner alone suffices)

aiState == Ready(text):
  Generated text rendered in a card below the input fields
  Below the card: row of [Copy button] [Regenerate button (replaces Polish)]
  Polish button hidden; Save button enabled

aiState == Error(kind, message):
  Error chip rendered below the input fields with the localized message + [Retry] icon
  Tap [Retry] = PolishClicked = generate() again
  Tap chip close = DismissAiError = aiState back to Idle
```

`CopyClicked` reducer emits `ReflectionEffect.CopyToClipboard(text)`; the Composable bridges via:
```kotlin
LaunchedEffect(viewModel) {
    viewModel.effects.collect { e -> when (e) {
        is ReflectionEffect.CopyToClipboard -> {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("memoir-reflection", e.text))
            // brief inline feedback: set a `var copiedAt by remember { mutableStateOf(0L) }`
            // and show a "Copied!" toast/text for 1.5 seconds
        }
        // other cases...
    }}
}
```

### D7. Tests

`PromptBuilderTest.kt`:
- Output `messages.size == 2` (system + user)
- System prompt under `Locale.ENGLISH` contains "English"
- System prompt under `Locale("zh")` contains "Traditional Chinese"
- Tone hint matches templateStyle (4 cases + "other")
- User prompt contains routeId, spotEntries (id + photoCount), mood (when non-null), insights, feedback
- userInsights `.ifBlank { "(none)" }` placeholder works
- spotEntries empty list → user prompt has no per-stop lines but still includes the count line

`JourneyInputAssemblerTest.kt`:
- Route lookup populates `spotEntries` from `route.journey`
- `memory.routeId == null` → `routeId = "(none)"`, `spotEntries.isEmpty()`
- Photo count even-split across stops (15 photos / 3 stops = 5 per stop)
- `userNote = null` for every entry (no spotNotes UI yet)
- `templateStyle = memory.templateId`

`DeepSeekReflectionClientTest.kt`:
- Construct with `mockk<OpenAI>()` — `coEvery { openAI.chatCompletion(any()) } returns mockResponse` where `mockResponse.choices.first().message.content == "Test caption"`
- `generate(input)` returns `ReflectionResult.Success("Test caption")`
- `coEvery { openAI.chatCompletion(any()) } throws OpenAIException(statusCode = 401, ...)` → `ReflectionResult.Failure(InvalidApiKey, _)`
- Same for 429 → RateLimited, 500 → ServiceUnavailable
- `coEvery { openAI.chatCompletion(any()) } throws IOException(...)` → `ReflectionResult.Failure(Network, _)`
- Other exception type → `Unexpected`
- Blank response content → `Failure(Unexpected)`

`ReflectionViewModelAiTest.kt`:
- `PolishClicked` transitions `aiState: Idle → Generating → Ready(text)` (use Turbine to capture both intermediate states)
- `PolishClicked` while `Generating` is a no-op (Mutex serialization)
- `RegenerateClicked` produces a fresh API call (two `coVerify { reflectionClient.generate(any()) }` invocations after Polish + Regenerate)
- Failure path → `aiState: Generating → Error(kind, message)`; subsequent `PolishClicked` retries
- `CopyClicked` while `Ready(text)` emits `CopyToClipboard(text)` effect
- `CopyClicked` while NOT Ready is a no-op (no effect emitted)
- `DismissAiError` while Error → `aiState = Idle`
- `SaveClicked` while `aiState is Ready` → `repo.updateGeneratedReflection(memoryId, text)` invoked exactly once before `complete()`
- `SaveClicked` while `aiState is Idle` → `repo.updateGeneratedReflection` NOT invoked

### D8. `MemoryRepository` extension

```kotlin
suspend fun updateGeneratedReflection(memoryId: String, text: String)
```

Single column update — bumps `updatedAt`. Spec impact: this is an addition to the change-#7 interface. Per the openspec "ADDED Requirements" pattern (not MODIFIED, since we're adding a new method, not changing an existing one), this change's spec.md declares the new requirement under its own capability.

Cross-change impact: change #7's `MemoryRepository` interface gets one new method. The implementer adds it to the change-#7-merged code in this change's PR. This is a one-line method addition, NOT a re-edit of change #7's spec files — same convention as how change #4 read change #2's `bookmarkedRouteIds` without modifying change #2's spec.

## Risks / Trade-offs

- **Embedded API key**: documented in proposal; APK-distribution discipline mitigates. Rotation procedure is a manual step in `local.properties.example` comment.
- **OpenAI client SDK version drift**: pin to v4.0.1 explicitly; if v4.1 changes `OpenAIConfig` API names, the implementer adjusts D2's snippet.
- **`OpenAIException` class name not verified**: catch-all `else -> Unexpected` covers wrong assumptions; tests stub the exception path explicitly.
- **Photo-count even-split is fake attribution**: documented; future per-spot-photo schema could replace it.
- **`CopyToClipboard` shows inline feedback for 1.5s without a snackbar host**: minor; users learn the pattern.
- **Prompt-injection via user text**: out of scope per proposal.
- **`generate()` doesn't cancel a previous Polish if user navigates away**: `viewModelScope.launch` cancels on `onCleared()`, so this is handled — the network request may already have started but the result is discarded.
- **`CIO` engine choice for Ktor**: works on Android; `OkHttp` engine would integrate with the app's existing HTTP stack — but the app has no other HTTP today, so CIO is simpler. Verify CIO works on minSdk 24.

## Migration Plan

1. Add `INTERNET` permission to `AndroidManifest.xml`.
2. Add `openai-client`, `ktor-client-cio` to catalog; add deps; enable `buildConfigField`.
3. Add `local.properties.example` template.
4. Create `data/llm/` package with `JourneyReflectionInput`, `SpotEntry`, `ReflectionResult`, `ReflectionError`, `ReflectionClient`, `DeepSeekReflectionClient`, `PromptBuilder`, `JourneyInputAssembler`.
5. Wire `MemoirApplication.onCreate` to construct `OpenAI` + `DeepSeekReflectionClient`; expose as `lateinit var reflectionClient`.
6. Extend `MemoryRepository` interface + `RoomMemoryRepository` impl with `updateGeneratedReflection`.
7. Extend `ReflectionState` + `ReflectionIntent` + `ReflectionEffect`.
8. Extend `ReflectionViewModel` with AI state machine + `Mutex` + `generate()` private method.
9. Extend `ReflectionViewModelFactory` with `reflectionClient` + `contentRepo` parameters.
10. Update `MyAppNavigation.MemoryReflectionDestination` to pass the new dependencies to the factory.
11. Rewrite `MemoryReflectionScreen` UI to render the four `aiState` cases + Copy/Regenerate buttons + error chip.
12. Add 7 new strings (5 errors + generating + copied) to `values/strings.xml` + `values-zh/`.
13. Add CI workflow step writing `DEEPSEEK_API_KEY=dummy-ci-key` to `local.properties` before Gradle commands.
14. Write 4 test files (PromptBuilder, Assembler, DeepSeekClient, VM-AI extension).
15. Emulator smoke: end-to-end memory creation → Polish → wait for spinner → caption appears → tap Copy → "Copied!" feedback → paste-test in another app to verify clipboard → tap Save → Memory row's `generatedReflection` column contains the text (verified via `adb shell ... sqlite3`).

**Rollback**: revert. `MemoryReflectionScreen` reverts to change-#7's stub. `MemoryEntity.generatedReflection` stays null. No data loss.

## Open Questions

- **Should the user be able to edit the generated text inline before Save?** Not in this change. They Copy → paste elsewhere → edit there. Acceptable per scope.
- **Per-spot user notes** (`Memory.spotNotes`)— no UI today, no prompt enrichment. Defer to a future change.
- **Caching successful generations** to avoid re-billing on identical input — defer; user controls cost via tap behavior.
- **Model selection** (deepseek-chat vs deepseek-coder vs etc.) — only deepseek-chat is appropriate; no UI to switch.
- **`PromptBuilder.build`'s output language directive verification**: empirically reliable in our experience but worth a smoke test under each locale.
- **`maxTokens = 500` may produce mid-sentence cuts** if the model targets a long output despite the "under 200 characters" instruction. Mitigation: keep `maxTokens = 500` for safety margin; if cuts are observed empirically, lower to ~250.
