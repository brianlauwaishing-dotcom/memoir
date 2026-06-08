## ADDED Requirements

### Requirement: MemoryRepository exposes updateGeneratedReflection

`com.mcis.memoir.data.memory.MemoryRepository` (interface introduced by change #7 `memory-creation-flow`) SHALL gain a `suspend fun updateGeneratedReflection(memoryId: String, text: String)` method. The `RoomMemoryRepository` impl SHALL set `MemoryEntity.generatedReflection = text` AND bump `updatedAt` AND persist via `dao.upsert(...)`. The method is callable independently of `complete(...)` so the AI text can be saved before status flips to COMPLETED.

#### Scenario: updateGeneratedReflection writes the column
- **WHEN** an instrumented test calls `updateGeneratedReflection(memoryId, "Caption text")` against an IN_PROGRESS draft and then reads the row back via `dao.getOnce(memoryId)`
- **THEN** the row's `generatedReflection == "Caption text"` AND `updatedAt` is greater than the prior `updatedAt`

#### Scenario: updateGeneratedReflection on missing row is a silent no-op
- **WHEN** `updateGeneratedReflection("00000000-0000-0000-0000-000000000000", "text")` is called for an id that doesn't exist
- **THEN** no DB row is created (no upsert against a missing primary key), AND no exception is thrown

## MODIFIED Requirements

### Requirement: Reflection save persists three text fields and bumps status to COMPLETED

`ReflectionViewModel.SaveClicked` SHALL invoke `MemoryRepository.updateReflection(memoryId, mood, insights, feedback)` followed by `MemoryRepository.complete(memoryId)`. Empty / blank `mood` and `feedback` SHALL be persisted as `null` (not the empty string); empty `insights` SHALL be persisted as the empty string (per umbrella §5.5 — insights is required, the others are optional). When `state.aiState` (the AI state machine field added by change #8 `ai-reflection-generation`) is `AiState.Ready(text)`, `SaveClicked` SHALL also invoke `MemoryRepository.updateGeneratedReflection(memoryId, text)` between `updateReflection` and `complete`. When `aiState` is any other case (Idle / Generating / Error), `updateGeneratedReflection` MUST NOT be invoked, and the row's `generatedReflection` column stays at its prior value.

#### Scenario: SaveClicked persists all three fields and completes the draft
- **WHEN** the screen state is `ReflectionState(overallMood = "excited", userInsights = "...", postTripFeedback = "next time...", isSaving = false)` and the VM receives `SaveClicked`
- **THEN** `updateReflection(memoryId, "excited", "...", "next time...")` is invoked exactly once, AND `complete(memoryId)` is invoked exactly once, AND the row's `status` becomes `"COMPLETED"`, AND the effects Flow emits `NavigateToMemoriesList`

#### Scenario: Empty mood and feedback are stored as null
- **WHEN** the state is `ReflectionState(overallMood = "", userInsights = "some text", postTripFeedback = "")` and `SaveClicked` is fired
- **THEN** `updateReflection(memoryId, null, "some text", null)` is invoked — empty strings become null because they semantically mean "user skipped this field"

#### Scenario: SaveClicked persists generatedReflection when AI ready
- **WHEN** `state.aiState = AiState.Ready("AI caption text")` and `SaveClicked` is fired
- **THEN** `updateGeneratedReflection(memoryId, "AI caption text")` is invoked exactly once (between `updateReflection` and `complete`), AND the saved row's `generatedReflection == "AI caption text"`

#### Scenario: SaveClicked does NOT persist generatedReflection when AI not ready
- **WHEN** `state.aiState` is `Idle` OR `Generating` OR `Error(...)` and `SaveClicked` is fired
- **THEN** `updateGeneratedReflection` is NOT invoked, AND the row's `generatedReflection` column stays at its prior value (typically null for a never-Polish'd draft)

#### Scenario: No LLM call from the Save path
- **WHEN** the user taps the "Save" button on `MemoryReflectionScreen`
- **THEN** Save itself does NOT call any HTTP client AND does NOT instantiate any LLM SDK type — the only network call this screen makes is in `PolishClicked` / `RegenerateClicked` reducers (NOT in `SaveClicked`). The earlier change-#7 scenario "No LLM call is made" originally constrained the entire screen because no AI plumbing existed; with change #8 landing, that constraint is narrowed to the Save reducer specifically
