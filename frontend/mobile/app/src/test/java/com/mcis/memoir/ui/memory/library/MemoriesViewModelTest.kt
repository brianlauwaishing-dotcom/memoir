package com.mcis.memoir.ui.memory.library

import android.content.ContentResolver
import android.content.res.Resources
import android.net.Uri
import app.cash.turbine.test
import com.mcis.memoir.data.content.ContentAssetLoader
import com.mcis.memoir.data.content.ContentRepository
import com.mcis.memoir.data.content.ContentSnapshot
import com.mcis.memoir.data.content.model.LocalizedFacts
import com.mcis.memoir.data.content.model.LocalizedText
import com.mcis.memoir.data.content.model.Spot
import com.mcis.memoir.data.memory.Memory
import com.mcis.memoir.data.memory.MemoryRepository
import com.mcis.memoir.data.memory.MemoryStatus
import com.mcis.memoir.data.prefs.UserPreferencesRepository
import io.mockk.every
import io.mockk.mockk
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MemoriesViewModelTest {
    private val mainDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun stateSeparatesInProgressAndCompletedAndMapsCoverDateProgress() = runTest(mainDispatcher) {
        val inProgress = MutableStateFlow(
            listOf(
                memory("ip1", MemoryStatus.IN_PROGRESS, photos = emptyList(), insights = ""),
                memory(
                    "ip2",
                    MemoryStatus.IN_PROGRESS,
                    photos = listOf("memories/ip2/photo_0.jpg"),
                    insights = "Loved it"
                ),
                memory(
                    "spot1",
                    MemoryStatus.IN_PROGRESS,
                    photos = listOf("memories/spot1/photo_0.jpg"),
                    spotId = "a"
                )
            )
        )
        val completed = MutableStateFlow(
            listOf(
                memory("c1", MemoryStatus.COMPLETED, photos = listOf("memories/c1/photo_0.jpg")),
                memory("spot2", MemoryStatus.COMPLETED, photos = listOf("memories/spot2/photo_0.jpg"), spotId = "b")
            )
        )
        val vm = viewModel(inProgress = inProgress, completed = completed)

        vm.state.test {
            assertTrue(awaitItem().isLoading)
            advanceUntilIdle()
            val loaded = awaitItem()

            assertFalse(loaded.isLoading)
            assertEquals(listOf("ip1", "ip2"), loaded.inProgress.map { it.id })
            assertEquals(listOf("c1"), loaded.completed.map { it.id })

            assertEquals(null, loaded.inProgress[0].coverRelativePath)
            assertEquals("memories/ip2/photo_0.jpg", loaded.inProgress[1].coverRelativePath)
            assertEquals("memories/c1/photo_0.jpg", loaded.completed[0].coverRelativePath)

            assertEquals(DraftProgress(1, 3), loaded.inProgress[0].draftProgress)
            assertEquals(DraftProgress(3, 3), loaded.inProgress[1].draftProgress)
            assertTrue(loaded.completed[0].dateLabel.isNotBlank())
        }
    }

    @Test
    fun bookmarkedSpotsAreFilteredLocaleResolvedSourceOrderedAndSearchable() = runTest(mainDispatcher) {
        val bookmarks = MutableStateFlow(setOf("c", "a"))
        val vm = viewModel(bookmarkedSpotIds = bookmarks)

        vm.state.test {
            assertTrue(awaitItem().isLoading)
            advanceUntilIdle()
            val loaded = awaitItem()

            // Filtered to bookmarked, preserving content source order [a, b, c] -> [a, c]
            assertEquals(listOf("a", "c"), loaded.bookmarkedSpots.map { it.id })
            assertEquals(
                listOf("Grand Mazu Temple", "Grand Wumiao Temple"),
                loaded.bookmarkedSpots.map { it.title }
            )

            vm.onIntent(MemoriesIntent.BookmarkSearchChanged("wumiao"))
            advanceUntilIdle()
            val filtered = awaitItem()
            assertEquals(listOf("c"), filtered.bookmarkedSpots.map { it.id })
        }
    }

    @Test
    fun tabSelectionAndSearchUpdateStateWithoutEffects() = runTest(mainDispatcher) {
        val vm = viewModel()
        subscribeState(vm)

        vm.effects.test {
            vm.onIntent(MemoriesIntent.TabSelected(MemoriesTab.BOOKMARK))
            advanceUntilIdle()
            assertEquals(MemoriesTab.BOOKMARK, vm.state.value.selectedTab)

            vm.onIntent(MemoriesIntent.BookmarkSearchChanged("temple"))
            advanceUntilIdle()
            assertEquals("temple", vm.state.value.bookmarkSearchQuery)

            expectNoEvents()
        }
    }

    @Test
    fun bookmarkClickOpensSpotDraftForPreviewEdit() = runTest(mainDispatcher) {
        val repo = FakeMemoryRepository(
            inProgress = MutableStateFlow(emptyList()),
            completed = MutableStateFlow(emptyList())
        )
        val vm = viewModel(repo = repo)
        subscribeState(vm)

        vm.effects.test {
            vm.onIntent(MemoriesIntent.BookmarkSpotClicked("a"))
            advanceUntilIdle()

            assertEquals(
                MemoriesEffect.NavigateToWizard("spot-draft-a", WizardEntry.EDIT),
                awaitItem()
            )
            assertEquals(listOf("a"), repo.spotDraftRequests)
        }
    }

    @Test
    fun memoryActionsEmitOrCallExpectedCollaborators() = runTest(mainDispatcher) {
        val repo = FakeMemoryRepository(
            inProgress = MutableStateFlow(listOf(memory("ip1", MemoryStatus.IN_PROGRESS))),
            completed = MutableStateFlow(
                listOf(memory("c1", MemoryStatus.COMPLETED, photos = listOf("memories/c1/photo_0.jpg")))
            )
        )
        val vm = viewModel(repo = repo)
        subscribeState(vm)
        advanceUntilIdle()

        // Continue editing an in-progress draft -> photo selection
        vm.effects.test {
            vm.onIntent(MemoriesIntent.ContinueEditingClicked("ip1"))
            advanceUntilIdle()
            assertEquals(
                MemoriesEffect.NavigateToWizard("ip1", WizardEntry.PHOTO_SELECTION),
                awaitItem()
            )

            // Edit a completed memory -> edit entry
            vm.onIntent(MemoriesIntent.EditClicked("c1"))
            advanceUntilIdle()
            assertEquals(MemoriesEffect.NavigateToWizard("c1", WizardEntry.EDIT), awaitItem())

            // Share a memory with photos
            vm.onIntent(MemoriesIntent.ShareClicked("c1"))
            advanceUntilIdle()
            assertEquals(
                MemoriesEffect.ShareMemory(listOf("memories/c1/photo_0.jpg"), "Memory c1"),
                awaitItem()
            )

            // Share a photo-less memory emits nothing
            vm.onIntent(MemoriesIntent.ShareClicked("ip1"))
            advanceUntilIdle()
            expectNoEvents()
        }

        // Delete confirm calls repo exactly once
        vm.onIntent(MemoriesIntent.DeleteClicked("c1"))
        vm.onIntent(MemoriesIntent.DeleteConfirmed)
        advanceUntilIdle()
        assertEquals(listOf("c1"), repo.deletedIds)
        assertFalse(vm.state.value.showDeleteDialog)

        // Cancel deletes nothing
        vm.onIntent(MemoriesIntent.DeleteClicked("ip1"))
        vm.onIntent(MemoriesIntent.DeleteCancelled)
        advanceUntilIdle()
        assertEquals(listOf("c1"), repo.deletedIds)

        // Duplicate calls repo once
        vm.onIntent(MemoriesIntent.DuplicateClicked("c1"))
        advanceUntilIdle()
        assertEquals(listOf("c1"), repo.duplicatedIds)
    }

    private fun TestScope.subscribeState(vm: MemoriesViewModel) {
        backgroundScope.launch { vm.state.collect {} }
    }

    private fun TestScope.viewModel(
        repo: MemoryRepository = FakeMemoryRepository(
            inProgress = MutableStateFlow(emptyList()),
            completed = MutableStateFlow(emptyList())
        ),
        inProgress: MutableStateFlow<List<Memory>>? = null,
        completed: MutableStateFlow<List<Memory>>? = null,
        bookmarkedSpotIds: MutableStateFlow<Set<String>> = MutableStateFlow(setOf("a", "c"))
    ): MemoriesViewModel {
        val effectiveRepo = if (inProgress != null || completed != null) {
            FakeMemoryRepository(
                inProgress = inProgress ?: MutableStateFlow(emptyList()),
                completed = completed ?: MutableStateFlow(emptyList())
            )
        } else {
            repo
        }
        val resources = mockk<Resources>()
        every { resources.getIdentifier(any(), "drawable", "com.mcis.memoir") } returns 7

        return MemoriesViewModel(
            repo = effectiveRepo,
            contentRepo = ContentRepository(FakeContentAssetLoader(snapshot()), this),
            prefsRepo = FakePrefs(bookmarkedSpotIds),
            resources = resources,
            localeProvider = { Locale.ENGLISH }
        )
    }

    private fun memory(
        id: String,
        status: String,
        photos: List<String> = emptyList(),
        insights: String = "",
        spotId: String? = null
    ): Memory = Memory(
        id = id,
        templateId = "old_street",
        routeId = null,
        spotId = spotId,
        title = "Memory $id",
        status = status,
        createdAt = 1_000L,
        updatedAt = 1_700_000_000_000L,
        photoRelativePaths = photos,
        spotNotes = emptyMap(),
        overallMood = null,
        userInsights = insights,
        postTripFeedback = null,
        generatedReflection = null,
        editorState = null
    )

    private fun snapshot(): ContentSnapshot = ContentSnapshot(
        routes = emptyMap(),
        spots = linkedMapOf(
            "a" to spot("a", "Grand Mazu Temple", "大媽祖廟"),
            "b" to spot("b", "Old Street", "老街"),
            "c" to spot("c", "Grand Wumiao Temple", "大武廟")
        )
    )

    private fun spot(id: String, titleEn: String, titleZh: String): Spot = Spot(
        id = id,
        title = LocalizedText(titleEn, titleZh),
        heroImage = "${id}_hero",
        duration = LocalizedText("20 min", "20 分鐘"),
        whyItMatters = LocalizedText("Why", "重要性"),
        historicalContext = LocalizedText("History", "歷史"),
        architecturalFeatures = LocalizedText("Architecture", "建築"),
        modernUse = LocalizedText("Modern", "現代"),
        facts = LocalizedFacts()
    )

    private class FakeContentAssetLoader(
        private val snapshot: ContentSnapshot
    ) : ContentAssetLoader {
        override suspend fun load(): ContentSnapshot = snapshot
    }

    private class FakePrefs(
        private val spots: MutableStateFlow<Set<String>>
    ) : UserPreferencesRepository {
        override val language: Flow<String> = MutableStateFlow("en")
        override val selectedInterests: Flow<Set<String>> = MutableStateFlow(emptySet())
        override val onboardingDone: Flow<Boolean> = MutableStateFlow(false)
        override val bookmarkedRouteIds: Flow<Set<String>> = MutableStateFlow(emptySet())
        override val bookmarkedSpotIds: Flow<Set<String>> = spots

        override suspend fun setLanguage(tag: String) = Unit
        override suspend fun setInterests(set: Set<String>) = Unit
        override suspend fun markOnboardingDone() = Unit
        override suspend fun setBookmarkedRouteIds(set: Set<String>) = Unit
        override suspend fun setBookmarkedSpotIds(set: Set<String>) = Unit
        override suspend fun persistedLanguageTag(): String? = null
    }

    private class FakeMemoryRepository(
        val inProgress: MutableStateFlow<List<Memory>>,
        val completed: MutableStateFlow<List<Memory>>
    ) : MemoryRepository {
        val deletedIds = mutableListOf<String>()
        val duplicatedIds = mutableListOf<String>()
        val spotDraftRequests = mutableListOf<String>()

        override fun observeByStatus(status: String): Flow<List<Memory>> =
            if (status == MemoryStatus.IN_PROGRESS) inProgress else completed

        override suspend fun deleteMemory(memoryId: String) {
            deletedIds.add(memoryId)
        }

        override suspend fun duplicateMemory(memoryId: String): Result<String> {
            duplicatedIds.add(memoryId)
            return Result.success("$memoryId-copy")
        }

        override suspend fun startDraft(templateId: String, defaultTitle: String): String = ""
        override suspend fun getOrCreateSpotDraft(
            spotId: String,
            templateId: String,
            defaultTitle: String
        ): String {
            spotDraftRequests.add(spotId)
            return "spot-draft-$spotId"
        }
        override fun observe(id: String): Flow<Memory?> = flowOf(null)
        override fun observeAll(): Flow<List<Memory>> = flowOf(emptyList())
        override suspend fun addPhoto(
            memoryId: String,
            sourceUri: Uri,
            contentResolver: ContentResolver
        ): Result<String> = Result.success("")
        override suspend fun removePhoto(memoryId: String, index: Int): Result<Unit> = Result.success(Unit)
        override suspend fun updateReflection(
            memoryId: String,
            mood: String?,
            insights: String,
            feedback: String?
        ) = Unit
        override suspend fun updateGeneratedReflection(memoryId: String, text: String) = Unit
        override suspend fun complete(memoryId: String) = Unit
        override suspend fun cancelDraftIfInProgress(memoryId: String) = Unit
        override fun fireCancelDraftIfInProgress(memoryId: String) = Unit
        override suspend fun sweepOrphans() = Unit
    }
}
