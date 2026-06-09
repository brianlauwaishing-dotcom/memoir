## Context

Final change in the umbrella. Unlike change #7 (which built the Room layer, the photo lifecycle, and the wizard), this change is mostly UI wiring plus two small repository methods. It consumes:
- `memory-creation-flow` change #7: the entire `data/memory/` package (`MemoryEntity`, `MemoryDao`, `MemoryDatabase`, `MemoryRepository` interface + `RoomMemoryRepository`, the `Memory` domain type), `MemoirApplication.memoryRepo`, the `FilePhoto` Composable (file-path → `ImageBitmap` decode), and the memoryId-keyed wizard destinations (`MemoryPhotoSelectionDestination`, `MemoryEditDestination`).
- `language-toggle` change #2: `LocaleController` / AppCompat application locale, `values-zh/strings.xml`.
- `home-discovery` change #3: the MVI state/intent/effect + Factory pattern and the JUnit5 + MockK + Turbine test stack.

**Current state**:
- `MemoriesScreen.kt:51` — `val allMemories = remember { MockData.memories }`, split by `MemoryStatus` into in-progress / completed lists.
- `MemoriesScreen.kt:43` — signature takes `selectedLanguage: String = "en"`; `isChinese = selectedLanguage == "zh"` drives every `R.string.X_zh` lookup throughout the file.
- `MemoriesScreen.kt:205-211` — `MemoryActionMenu` only passes an `onDeleteClick`; Edit / Duplicate / Share `MenuItem`s fall through to the default no-op `onClick = {}` (`MenuItem` at `:250`).
- `MemoriesScreen.kt:216-225` — `DeleteConfirmationDialog.onDelete` just sets `showDeleteDialog = false; activeMenuMemoryId = null`. No repository call; nothing is deleted.
- `MemoriesScreen.kt:490-507` — `CompletedMemoryCard` arrow `Box` with `clickable { /* Detail */ }`.
- `MemoriesScreen.kt:445-473` — likes/comments row driven by `memory.likes` / `memory.comments` (mock-only fields; the Room `Memory` model has neither).
- `InProgressMemoryCard` (`:355-381`) renders a progress bar from `memory.currentProgress` / `memory.totalProgress` — mock-only fields absent from the Room model.
- No `FileProvider` declared in `AndroidManifest.xml`; no `res/xml/file_paths.xml`.

**Constraints**:
- Cannot precede change #7 (needs the Room layer + `FilePhoto` + wizard destinations) or change #2 (needs AppCompat locale).
- Must keep `MockData` compiling (coexistence rule).
- The Room `Memory` model is the single source of truth — the screen must render from it, not from `MockData`'s richer-but-fake field set.
- Test stack JUnit5 + MockK + Turbine carries from change #3.

## Goals / Non-Goals

**Goals:**
1. `MemoriesScreen` renders live Room data via `MemoriesViewModel` (in-progress + completed sections).
2. Wire all four 3-dots actions: Edit (resume/reopen wizard), Delete (any status), Duplicate (deep copy), Share (FileProvider).
3. Draft resume from the in-progress card body ("Continue Editing").
4. Remove the dangling detail arrow.
5. Migrate the whole screen off `selectedLanguage` / `_zh`-suffix to VM + AppCompat locale.
6. Add `MemoryRepository.deleteMemory` and `duplicateMemory` with the same path-safety guards change #7 established.
7. Add a `FileProvider` so app-private memory photos can be shared.
8. Keep `MockData` compiling.

**Non-Goals:**
- A memory **detail** screen (none exists; the arrow is removed, not wired).
- A rendered composite "memory image" to share — the editor canvas is stubbed in change #7, so Share hands over the underlying photo files.
- Social engagement (likes/comments) — no backend, single device.
- Editing a memory's **title** inline (the wizard owns title; this screen only lists/acts).
- Per-memory thumbnails/caching beyond change #7's `FilePhoto` decode.
- Multi-select / batch actions on the library.
- Undo for delete (a confirmation dialog already exists; that is the safety net).

## Decisions

### D1. `MemoriesViewModel` combines two status Flows into card DTOs

```kotlin
data class MemoryCard(
    val id: String,
    val title: String,                 // already locale-resolved at creation (change #7 D13)
    val coverRelativePath: String?,    // first of photoRelativePaths, or null
    val status: String,                // MemoryStatus.IN_PROGRESS / COMPLETED
    val dateLabel: String,             // formatted from updatedAt, COMPLETED cards
    val draftProgress: DraftProgress   // derived completeness for IN_PROGRESS cards
)

data class DraftProgress(val current: Int, val total: Int)   // e.g. 2 / 3

class MemoriesViewModel(
    private val repo: MemoryRepository,
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {
    val state: StateFlow<MemoriesState> =
        combine(
            repo.observeByStatus(MemoryStatus.IN_PROGRESS),
            repo.observeByStatus(MemoryStatus.COMPLETED)
        ) { inProg, done ->
            MemoriesState(
                inProgress = inProg.map { it.toCard() },
                completed  = done.map { it.toCard() },
                isLoading = false
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MemoriesState(isLoading = true))
    // menu / dialog UI state held in a second MutableStateFlow merged into MemoriesState
}
```

**Why two `observeByStatus` calls vs one `observeAll` partitioned in the VM:** change #7 already exposes `observeByStatus`; the DB does the ordering (`ORDER BY updatedAt DESC`) per status. Partitioning `observeAll()` in Kotlin would re-sort and re-filter on every emission for no benefit. `combine` re-emits when either side changes.

**Menu/dialog state** (`activeMenuMemoryId`, `showDeleteDialog`) moves out of the Composable's `remember` into `MemoriesState` so it is driven by intents and unit-testable. Alternative — leave it as local `remember` — keeps the VM thinner but makes the Delete-confirm flow (open menu → tap Delete → confirm → repo call) untestable without an instrumented Compose test. Centralizing in the VM matches the change #7 ReflectionViewModel precedent.

### D2. `draftProgress` is derived, not stored

The Room `Memory` model has no `currentProgress`/`totalProgress` (those are `MockData`-only). Rather than grow the schema, derive a 3-step completeness for in-progress drafts:

```kotlin
private fun Memory.draftProgress(): DraftProgress {
    var done = 1                                   // template chosen (always true for a draft)
    if (photoRelativePaths.isNotEmpty()) done++    // ≥1 photo added
    if (userInsights.isNotBlank()) done++          // reflection started
    return DraftProgress(current = done, total = 3)
}
```

This keeps the existing progress-bar UI meaningful with real data and zero schema change. Alternative — drop the progress bar entirely — loses a useful affordance; the derived metric is cheap and honest. The exact heuristic is an implementation detail the spec pins only loosely (a `1..3 / 3` range).

### D3. Edit / resume entry points reuse change #7 destinations unchanged

| Source | Action | Destination |
|---|---|---|
| In-progress card body ("Continue Editing") | tap | `MemoryPhotoSelectionDestination(memoryId)` |
| In-progress card → menu → Edit | tap | `MemoryPhotoSelectionDestination(memoryId)` |
| Completed card → menu → Edit | tap | `MemoryEditDestination(memoryId)` |

Resuming an in-progress draft at the **photo** step is the safest single target: a draft may have 0 photos (abandoned right after template select), and the photo screen is the wizard's natural memoryId-keyed entry. A completed memory already has photos, so Edit opens the **edit** step. Critically, **no change-#7 code is touched**: re-entering a `COMPLETED` memory is safe because the wizard VMs' `onCleared()` calls `cancelDraftIfInProgress`, which no-ops on any non-`IN_PROGRESS` row — backing out of a re-edit cannot delete a finished memory. A re-save through Reflection calls `complete()` again (idempotent).

**Alternative considered** — resume at the screen matching the draft's furthest-reached step (compute from `photoRelativePaths` / reflection fields). Rejected: requires the library to model the wizard's internal step machine; one fixed entry per status is simpler and the wizard's forward nav covers the rest.

### D4. Delete is a new repository method (status-agnostic), distinct from `cancelDraftIfInProgress`

Change #7's `cancelDraftIfInProgress` deletes only `IN_PROGRESS` rows (its job is wizard abandonment cleanup). A user deleting a `COMPLETED` memory from the library needs an unconditional delete:

```kotlin
suspend fun deleteMemory(memoryId: String) = withContext(ioDispatcher) {
    if (!memoryId.isValidUuid()) return@withContext            // same guard as change #7
    val dir = File(filesDir, "memories/$memoryId")
    if (dir.exists() && dir.startsWith(File(filesDir, "memories"))) dir.deleteRecursively()
    dao.delete(memoryId)
}
```

Reuses change #7's `isValidUuid()` regex + `startsWith` containment guard verbatim — no new path-safety surface. It deletes the directory then the row regardless of status. The existing `DeleteConfirmationDialog` is the user-facing safety; no soft-delete/undo.

### D5. Duplicate deep-copies row + photo files into a fresh memoryId

```kotlin
suspend fun duplicateMemory(memoryId: String): Result<String> = withContext(ioDispatcher) {
    runCatching {
        require(memoryId.isValidUuid()) { "memoryId must be a UUID" }
        val src = dao.getOnce(memoryId) ?: error("memory not found: $memoryId")
        val newId = UUID.randomUUID().toString()
        val srcPaths: List<String> = json.decodeFromString(src.photoLocalPaths)
        val newPaths = srcPaths.mapIndexed { i, rel ->
            val newRel = "memories/$newId/photo_$i.jpg"
            val dst = File(filesDir, newRel).apply { parentFile?.mkdirs() }
            File(filesDir, rel).copyTo(dst, overwrite = true)
            newRel
        }
        val now = System.currentTimeMillis()
        dao.upsert(src.copy(
            id = newId,
            createdAt = now,
            updatedAt = now,
            photoLocalPaths = json.encodeToString(newPaths)
        ))
        newId
    }
}
```

`status` is preserved (`src.copy` keeps it) — duplicating a completed memory yields a completed memory; duplicating a draft yields a draft. Title is copied verbatim (no `" (copy)"` suffix — avoids a new bilingual string and the awkward locale question of which language to suffix in). Returns `Result<String>` so a copy failure (e.g. a missing source file) surfaces as an error toast instead of a crash. The observed Flow makes the new card appear automatically.

### D6. Drop the completed card's likes/comments row (deviation from mock — reviewer confirm)

`memory.likes` / `memory.comments` exist only on `MockData.MemoryData`; the Room `Memory` model has no such fields and there is no backend to source them (umbrella §1.3: no auth/accounts/cloud). Options:
- **(a, chosen)** Remove the likes/comments `Row` (`MemoriesScreen.kt:445-473`). The completed card shows cover + title + "Updated on …".
- (b) Keep the row bound to constant `0 / 0`. Rejected — a permanent fake count on a single-user offline app is misleading.
- (c) Repurpose the counts (e.g. photo count). Rejected — scope creep, and a photo count next to a "saved/comment" glyph is confusing.

This is the one place the rendered card visibly departs from the Figma mock, so it is called out explicitly for reviewer sign-off in the implementing PR. If the reviewer prefers (b), it's a one-line change.

### D7. Share via `FileProvider` + `ACTION_SEND[_MULTIPLE]`, launched from the Composable

App-private `filesDir/memories/<id>/*.jpg` cannot be passed to another app as a `file://` URI (FileUriExposedException on API 24+). Add a `FileProvider`:

`AndroidManifest.xml`:
```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data android:name="android.support.FILE_PROVIDER_PATHS"
               android:resource="@xml/file_paths" />
</provider>
```
`res/xml/file_paths.xml`:
```xml
<paths><files-path name="memories" path="memories/" /></paths>
```

The VM emits an effect carrying only data; the Composable (which has `Context`) resolves URIs and launches the chooser:
```kotlin
is MemoriesEffect.ShareMemory -> {
    val uris = e.relativePaths.map { rel ->
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(context.filesDir, rel))
    }
    val intent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"; putExtra(Intent.EXTRA_STREAM, uris[0])
            putExtra(Intent.EXTRA_TEXT, e.title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/jpeg"; putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            putExtra(Intent.EXTRA_TEXT, e.title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    context.startActivity(Intent.createChooser(intent, null))
}
```

**Why share photos and not a composite:** the change #7 editor renders photos over a template mask only on-screen; there is no exported bitmap. Sharing the source photos + title is the honest MVP. A future change can render the canvas to a bitmap and share that. If a memory has **zero** photos (a bare draft), Share emits a no-op effect / disabled state — `ACTION_SEND` with no stream is pointless. **Why resolve URIs in the Composable:** `FileProvider.getUriForFile` and `startActivity` need `Context`; keeping them out of the VM preserves JVM-only unit testability (the VM test asserts the `ShareMemory(relativePaths, title)` effect, not the Intent).

### D8. Screen migration mirrors prior changes

`MemoriesScreen(selectedLanguage: String, …)` → `MemoriesScreen(viewModel: MemoriesViewModel, onNavigate…)`. `isChinese` and every `stringResource(R.string.X_zh)` are deleted; chrome text uses `stringResource(R.string.X)` resolved by AppCompat locale (`values-zh/strings.xml` already has all 11 `memories_*` keys + `cancel_button` / `delete_button`). Card composables take `MemoryCard` DTOs instead of `MemoryData`; covers render through change #7's `FilePhoto(relativePath, filesDir)` instead of `painterResource(memory.imageRes)`. The `MockData`/`MemoryData`/`MemoryStatus`-from-`data` imports are dropped (the screen now imports `MemoryStatus` constants from `data/memory`).

## Risks / Trade-offs

- **Likes/comments removal is a visible mock deviation (D6)** → called out for explicit reviewer sign-off in the PR; trivially revertible to a static `0/0` if rejected.
- **Re-editing a COMPLETED memory and backing out** → safe by construction: `cancelDraftIfInProgress` no-ops on non-`IN_PROGRESS`; verified by a change #7 spec scenario already. A re-save calls `complete()` idempotently.
- **`duplicateMemory` partial copy** → if a source photo file is missing mid-copy, `copyTo` throws and the whole op returns `Result.failure`; the partially-created `memories/<newId>/` dir may linger but is swept only if the new row were `IN_PROGRESS` and stale. Mitigation: on failure, best-effort `deleteRecursively()` the new dir before returning. (Implementation detail in tasks.)
- **FileProvider authority typo** → `${applicationId}.fileprovider` in the manifest must match `${context.packageName}.fileprovider` at the call site; a mismatch throws at share time. Covered by a manual emulator share smoke (tasks) — not unit-testable.
- **Sharing raw photos, not the styled memory** → expectation gap if a user assumes "Share" exports the decorated layout. Acceptable per umbrella scope (editor stubbed); documented.
- **`combine` re-emission churn** → `WhileSubscribed(5_000)` + Room's diffed Flow keep this cheap at MVP list sizes.
- **Empty-state** → with real data the library can be empty (no `MockData` seed). The screen must render an empty state (or just the create button) rather than blank sections; handled in the screen rewrite.

## Migration Plan

1. Add `deleteMemory` + `duplicateMemory` to the `MemoryRepository` interface and `RoomMemoryRepository` impl.
2. Add `FileProvider` `<provider>` to `AndroidManifest.xml` + `res/xml/file_paths.xml`.
3. Create `ui/memory/library/`: `MemoryCard`, `MemoriesState`, `MemoriesIntent`, `MemoriesEffect`, `MemoriesViewModel`, `MemoriesViewModelFactory`.
4. Rewrite `MemoriesScreen.kt` VM-driven; delete arrow + likes/comments; wire menu/dialog to intents; covers via `FilePhoto`.
5. Wire the `Memories` entry in `MyAppNavigation`; route effects (wizard nav + share chooser).
6. Tests: `MemoryRepositoryActionsTest` (delete-any-status, duplicate-with-photo-copy), `MemoriesViewModelTest` (combine mapping, menu/dialog intents, Share effect).
7. Emulator smoke: create a memory in the wizard → see it in the library → edit/duplicate/delete/share → resume a draft.

**Rollback**: revert the change commit. `MemoriesScreen` returns to reading `MockData.memories`; the two new repository methods and the FileProvider are orphaned but harmless. Room DB on device is untouched by the rollback.

## Open Questions

- **Keep or drop likes/comments (D6)?** Recommend drop; reviewer confirms in the PR.
- **Title `" (copy)"` suffix on duplicate?** Recommend no suffix for now (avoids a bilingual-suffix locale question); revisit if duplicates become hard to tell apart.
- **Empty-state copy** — does the library need its own "No memories yet" string, or is the existing "Create Memory" button enough? Recommend the button alone for MVP; add a string only if the bare screen tests poorly.
- **Share zero-photo draft** — disable Share in the menu when `coverRelativePath == null`, or show a toast? Recommend disabling the menu item (cleaner than a toast).
