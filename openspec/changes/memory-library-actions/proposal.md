## Why

`memory-library-actions` is change #9 — the last entry in the mobile-direct wiring umbrella (`docs/superpowers/specs/2026-06-07-mobile-direct-app-wiring-design.md` §2 line 9). `MemoriesScreen` is the only surface that still reads `MockData.memories` (`MemoriesScreen.kt:51`), so memories the user actually creates through the change #7 wizard never appear in the library. Its 3-dots menu renders four actions (Edit / Delete / Duplicate / Share) but only Delete is wired to UI — and even that just dismisses the dialog without removing anything (`MemoriesScreen.kt:220-223`); Edit, Duplicate, and Share are inert `MenuItem`s with the default no-op `onClick`. Completed cards also carry a dangling detail arrow whose handler is an empty `clickable { /* Detail */ }` (`MemoriesScreen.kt:490-507`), and the whole screen is still on the legacy `selectedLanguage: String` + `_zh`-suffix lookup that every prior change has been retiring. This change closes the umbrella by making the library show real Room data and making every affordance on it do something (or be removed).

## What Changes

- Replace `MockData.memories` reads with live Room data: `MemoriesScreen` consumes `MemoryRepository.observeByStatus("IN_PROGRESS")` and `observeByStatus("COMPLETED")` (both already exposed by change #7) through a new `MemoriesViewModel`.
- Wire the 3-dots menu actions on both card types:
  - **Edit** — re-enters the change #7 wizard keyed by `memoryId`. An `IN_PROGRESS` draft resumes at `MemoryPhotoSelectionDestination(memoryId)`; a `COMPLETED` memory opens at `MemoryEditDestination(memoryId)`. The wizard's existing `cancelDraftIfInProgress` `onCleared()` guard already no-ops on `COMPLETED`, so re-editing a finished memory can never delete it.
  - **Delete** — wires the existing `DeleteConfirmationDialog` to a new `MemoryRepository.deleteMemory(memoryId)` that removes the row + photo directory regardless of status (the change #7 `cancelDraftIfInProgress` only deletes `IN_PROGRESS`).
  - **Duplicate** — a new `MemoryRepository.duplicateMemory(memoryId)` that deep-copies the row and its photo files into a fresh `memoryId`, preserving `status`.
  - **Share** — shares the memory's photos via `Intent.ACTION_SEND` / `ACTION_SEND_MULTIPLE` through a new `FileProvider` (none exists today), since app-private `filesDir/memories/<id>/` files can't be handed to other apps without a content URI.
- **Draft resume**: the `IN_PROGRESS` card body ("Continue Editing", `MemoriesScreen.kt:346`) becomes a tappable resume into `MemoryPhotoSelectionDestination(memoryId)`.
- **Remove the dangling arrow**: delete the `CompletedMemoryCard` arrow `Box` (`MemoriesScreen.kt:490-507`) — it has no destination in a mobile-direct app with no memory-detail screen.
- **Drop dead social stats**: the completed card's likes/comments row (`MemoriesScreen.kt:445-473`) has no data source in the no-backend, single-device architecture (umbrella §1.3 non-goals: no auth/accounts/cloud). It is removed rather than rendered as a permanent fake `0 / 0`. (See design D6 — flagged for reviewer confirmation.)
- Migrate `MemoriesScreen` and its card composables off `selectedLanguage: String` + `R.string.X_zh` lookups to the established VM + AppCompat-locale pattern (`values-zh/strings.xml` already mirrors every `memories_*` key).
- Add the `MemoriesScreen` Nav3 entry wiring: `MyAppNavigation` constructs the `MemoriesViewModel` and routes its effects to the wizard destinations and the share chooser.

## Capabilities

### New Capabilities
- `memory-library-actions`: The Memories library surface backed by real Room data — the `MemoriesViewModel` that maps `Memory` domain rows into `IN_PROGRESS` / `COMPLETED` card lists, the four 3-dots actions (Edit-resume, Delete-any-status, Duplicate-with-photo-copy, Share-via-FileProvider), draft resume from the card body, the `FileProvider` + `file_paths.xml`, and the two new `MemoryRepository` methods (`deleteMemory`, `duplicateMemory`). Owns the contract that the library reflects Room state live and that every affordance on the screen either acts or is removed.

### Modified Capabilities
<!-- None. `memory-creation-flow` (#7) supplies MemoryRepository / MemoryDatabase / MemoirApplication.memoryRepo / the FilePhoto composable / the memoryId-keyed wizard destinations, and `language-toggle` (#2) supplies the AppCompat locale — both are foundations consumed unchanged. The two new repository methods are net-new requirements owned by memory-library-actions, not changes to #7's persistence requirements. -->

## Impact

- **Depends on**: change #7 `memory-creation-flow` (the entire `data/memory/` Room layer, `MemoirApplication.memoryRepo`, the `FilePhoto` Composable, the `MemoryPhotoSelectionDestination` / `MemoryEditDestination` wizard entries) and change #2 `language-toggle` (AppCompat locale). MUST land after both.
- **New files**:
  - `ui/memory/library/MemoriesViewModel.kt`, `MemoriesState.kt`, `MemoriesIntent.kt`, `MemoriesEffect.kt`, `MemoriesViewModelFactory.kt`, `MemoryCard.kt` (DTO).
  - `res/xml/file_paths.xml` (FileProvider config).
  - Tests: `MemoriesViewModelTest.kt`, `MemoryRepositoryActionsTest.kt` (delete-any-status + duplicate-with-photo-copy paths).
- **Modified files**:
  - `MemoriesScreen.kt` — VM-driven; `selectedLanguage`/`_zh` removed; card composables read `MemoryCard` DTOs and `FilePhoto` covers; arrow `Box` and likes/comments row deleted; menu/dialog wired to intents.
  - `data/memory/MemoryRepository.kt` (+ `RoomMemoryRepository.kt`) — add `deleteMemory(memoryId)` and `duplicateMemory(memoryId): String`.
  - `MyAppNavigation.kt` — `Memories` entry builds the `MemoriesViewModel`; routes effects to wizard destinations and the FileProvider share chooser.
  - `AndroidManifest.xml` — add the `<provider>` for `androidx.core.content.FileProvider`.
- **Dependencies added**: none new (`androidx.core` already present provides `FileProvider`).
- **Not changed**: `MockData.memories` / `MockData.templates` / `MockData.routes` / `MockData.spots` stay intact and compiling (multi-change coexistence rule); the change #7 wizard screens and their `cancelDraftIfInProgress` lifecycle are reused unmodified.
- **Risk**: dropping the likes/comments row is a visible deviation from the mock design (D6); FileProvider share of raw photos (not a rendered composite) is the honest MVP given the editor canvas stays stubbed (umbrella §2 scope).
