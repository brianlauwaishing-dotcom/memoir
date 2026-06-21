package com.mcis.memoir.ui.memory.edit

import app.cash.turbine.test
import com.mcis.memoir.data.memory.MemoryRepository
import com.mcis.memoir.ui.memory.photo.clearForTest
import com.mcis.memoir.ui.memory.photo.memory
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
        every { repo.observe(memoryId) } returns MutableStateFlow(memory(listOf("p0", "p1")))
        val vm = EditViewModel(memoryId, repo)
        advanceUntilIdle()

        assertFalse(vm.state.value.isLoading)
        assertEquals("old_street", vm.state.value.templateId)
        assertEquals(5, vm.state.value.templateSlots.size)
        assertEquals(listOf("p0", "p1"), vm.state.value.photoPaths)

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
}
