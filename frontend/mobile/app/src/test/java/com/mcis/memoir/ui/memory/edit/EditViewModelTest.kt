package com.mcis.memoir.ui.memory.edit

import app.cash.turbine.test
import com.mcis.memoir.data.content.ContentAssetLoader
import com.mcis.memoir.data.content.ContentRepository
import com.mcis.memoir.data.content.ContentSnapshot
import com.mcis.memoir.data.content.model.LocalizedFacts
import com.mcis.memoir.data.content.model.LocalizedText
import com.mcis.memoir.data.content.model.Spot
import com.mcis.memoir.data.memory.MemoryRepository
import com.mcis.memoir.ui.memory.photo.clearForTest
import com.mcis.memoir.ui.memory.photo.memory
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditViewModelTest {
    private val mainDispatcher = StandardTestDispatcher()
    private val memoryId = "11111111-1111-4111-8111-111111111111"

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initPopulatesTemplateSlotsAndPhotosAndSaveNavigates() = runTest(mainDispatcher) {
        val repo = mockk<MemoryRepository>(relaxed = true)
        every { repo.observe(memoryId) } returns MutableStateFlow(
            memory(listOf("p0", "p1"), spotId = "grand_mazu")
        )
        val vm = EditViewModel(
            memoryId = memoryId,
            repo = repo,
            contentRepo = ContentRepository(FakeContentAssetLoader(snapshot()), this),
            localeProvider = { Locale.ENGLISH }
        )
        advanceUntilIdle()

        assertFalse(vm.state.value.isLoading)
        assertEquals("old_street", vm.state.value.templateId)
        assertEquals(5, vm.state.value.templateSlots.size)
        assertEquals(listOf("p0", "p1"), vm.state.value.photoPaths)
        assertEquals(true, vm.state.value.isSpotDraft)
        assertEquals("Grand Mazu Temple", vm.state.value.spotTitle)
        assertEquals("A layered temple story.", vm.state.value.spotDescription)

        vm.effects.test {
            vm.onIntent(EditIntent.SaveClicked)
            advanceUntilIdle()

            assertEquals(EditEffect.NavigateToReflection(memoryId), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        // Clearing the ViewModel (e.g. on forward navigation to Reflection) must NOT delete the
        // draft; ReflectionViewModel finalizes it via repo.complete().
        vm.clearForTest()
        verify(exactly = 0) { repo.fireCancelDraftIfInProgress(memoryId) }
    }

    private class FakeContentAssetLoader(
        private val snapshot: ContentSnapshot
    ) : ContentAssetLoader {
        override suspend fun load(): ContentSnapshot = snapshot
    }

    private fun snapshot(): ContentSnapshot = ContentSnapshot(
        routes = emptyMap(),
        spots = mapOf(
            "grand_mazu" to Spot(
                id = "grand_mazu",
                title = LocalizedText("Grand Mazu Temple", "大天后宮"),
                heroImage = "grand_mazu",
                duration = LocalizedText("20 min", "20 分鐘"),
                whyItMatters = LocalizedText("A layered temple story.", "寺廟故事"),
                historicalContext = LocalizedText("History", "歷史"),
                architecturalFeatures = LocalizedText("Architecture", "建築"),
                modernUse = LocalizedText("Modern", "現代"),
                facts = LocalizedFacts()
            )
        )
    )
}
