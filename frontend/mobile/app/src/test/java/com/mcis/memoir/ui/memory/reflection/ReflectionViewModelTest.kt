package com.mcis.memoir.ui.memory.reflection

import android.content.res.Resources
import app.cash.turbine.test
import com.mcis.memoir.data.memory.MemoryRepository
import com.mcis.memoir.ui.memory.photo.clearForTest
import com.mcis.memoir.ui.memory.photo.memory
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReflectionViewModelTest {
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
    fun textChangeIntentsUpdateStateWithoutEffects() = runTest(mainDispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.effects.test {
            vm.onIntent(ReflectionIntent.MoodChanged("calm"))
            vm.onIntent(ReflectionIntent.InsightsChanged("learned"))
            vm.onIntent(ReflectionIntent.FeedbackChanged("return"))
            advanceUntilIdle()

            assertEquals("calm", vm.state.value.overallMood)
            assertEquals("learned", vm.state.value.userInsights)
            assertEquals("return", vm.state.value.postTripFeedback)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun savePersistsReflectionCompletesAndSkipsCleanupAfterSuccess() = runTest(mainDispatcher) {
        val repo = mockk<MemoryRepository>(relaxed = true)
        every { repo.observe(memoryId) } returns MutableStateFlow(memory(emptyList()))
        coEvery { repo.updateReflection(any(), any(), any(), any()) } returns Unit
        coEvery { repo.complete(memoryId) } returns Unit
        val vm = reflectionVm(repo)
        advanceUntilIdle()

        vm.onIntent(ReflectionIntent.MoodChanged(""))
        vm.onIntent(ReflectionIntent.InsightsChanged("some text"))
        vm.onIntent(ReflectionIntent.FeedbackChanged(""))

        vm.effects.test {
            vm.onIntent(ReflectionIntent.SaveClicked)
            advanceUntilIdle()

            assertEquals(ReflectionEffect.NavigateToMemoriesList, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { repo.updateReflection(memoryId, null, "some text", null) }
        coVerify(exactly = 1) { repo.complete(memoryId) }
        vm.clearForTest()
        verify(exactly = 0) { repo.fireCancelDraftIfInProgress(memoryId) }
    }

    @Test
    fun saveFailureEmitsShowError() = runTest(mainDispatcher) {
        val repo = mockk<MemoryRepository>(relaxed = true)
        every { repo.observe(memoryId) } returns MutableStateFlow(memory(emptyList()))
        coEvery { repo.updateReflection(any(), any(), any(), any()) } throws IllegalStateException("nope")
        val vm = reflectionVm(repo)
        advanceUntilIdle()

        vm.effects.test {
            vm.onIntent(ReflectionIntent.SaveClicked)
            advanceUntilIdle()

            assertEquals(ReflectionEffect.ShowError("nope"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun viewModel(): ReflectionViewModel {
        val repo = mockk<MemoryRepository>(relaxed = true)
        every { repo.observe(memoryId) } returns MutableStateFlow(memory(emptyList()))
        return reflectionVm(repo)
    }

    private fun reflectionVm(repo: MemoryRepository): ReflectionViewModel =
        ReflectionViewModel(
            memoryId = memoryId,
            repo = repo,
            reflectionClient = mockk(relaxed = true),
            contentRepo = mockk(relaxed = true),
            resources = mockk<Resources>(relaxed = true),
            localeProvider = { Locale.ENGLISH }
        )
}
