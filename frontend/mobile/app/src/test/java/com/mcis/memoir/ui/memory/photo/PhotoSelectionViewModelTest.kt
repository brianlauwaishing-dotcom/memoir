package com.mcis.memoir.ui.memory.photo

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import app.cash.turbine.test
import com.mcis.memoir.data.memory.Memory
import com.mcis.memoir.data.memory.MemoryRepository
import com.mcis.memoir.data.memory.MemoryStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PhotoSelectionViewModelTest {
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
    fun addPhotosClickedEmitsRemainingSlotCount() = runTest(mainDispatcher) {
        val vm = viewModel(paths = emptyList())
        advanceUntilIdle()

        vm.effects.test {
            vm.onIntent(PhotoSelectionIntent.AddPhotosClicked)
            advanceUntilIdle()

            assertEquals(PhotoSelectionEffect.LaunchPicker(maxItems = 5), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun photosPickedClipsToAvailableSlotsAndRemoveCallsRepository() = runTest(mainDispatcher) {
        val repo = mockk<MemoryRepository>(relaxed = true)
        every { repo.observe(memoryId) } returns MutableStateFlow(memory(paths = listOf("a", "b", "c")))
        coEvery { repo.addPhoto(eq(memoryId), any(), any()) } returns Result.success("ok")
        coEvery { repo.removePhoto(memoryId, 1) } returns Result.success(Unit)
        val vm = PhotoSelectionViewModel(memoryId, repo, mockk<ContentResolver>())
        advanceUntilIdle()

        val uris = (0 until 4).map { mockk<Uri>() }
        vm.onIntent(PhotoSelectionIntent.PhotosPicked(uris))
        advanceUntilIdle()
        vm.onIntent(PhotoSelectionIntent.PhotoRemoved(1))
        advanceUntilIdle()

        coVerify(exactly = 2) { repo.addPhoto(eq(memoryId), any(), any()) }
        coVerify(exactly = 1) { repo.removePhoto(memoryId, 1) }
    }

    @Test
    fun nextClickedDoesNotNavigateWithoutPhotosAndOnClearedCleansUp() = runTest(mainDispatcher) {
        val repo = mockk<MemoryRepository>(relaxed = true)
        every { repo.observe(memoryId) } returns MutableStateFlow(memory(paths = emptyList()))
        val vm = PhotoSelectionViewModel(memoryId, repo, mockk<ContentResolver>())
        advanceUntilIdle()

        vm.effects.test {
            vm.onIntent(PhotoSelectionIntent.NextClicked)
            advanceUntilIdle()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }

        vm.clearForTest()
        verify(exactly = 1) { repo.fireCancelDraftIfInProgress(memoryId) }
    }

    private fun viewModel(paths: List<String>): PhotoSelectionViewModel {
        val repo = mockk<MemoryRepository>(relaxed = true)
        every { repo.observe(memoryId) } returns MutableStateFlow(memory(paths))
        return PhotoSelectionViewModel(memoryId, repo, mockk<ContentResolver>())
    }
}

fun memory(paths: List<String>): Memory = Memory(
    id = "11111111-1111-4111-8111-111111111111",
    templateId = "old_street",
    routeId = null,
    title = "Old Street Journal",
    status = MemoryStatus.IN_PROGRESS,
    createdAt = 1L,
    updatedAt = 1L,
    photoRelativePaths = paths,
    spotNotes = emptyMap(),
    overallMood = null,
    userInsights = "",
    postTripFeedback = null,
    generatedReflection = null,
    editorState = null
)

fun ViewModel.clearForTest() {
    val method = javaClass.getDeclaredMethod("onCleared")
    method.isAccessible = true
    method.invoke(this)
}
