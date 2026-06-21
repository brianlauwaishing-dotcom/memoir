package com.mcis.memoir.ui.memory.reflection

import android.content.res.Resources
import app.cash.turbine.test
import com.mcis.memoir.data.content.ContentRepository
import com.mcis.memoir.data.llm.ReflectionClient
import com.mcis.memoir.data.llm.ReflectionError
import com.mcis.memoir.data.llm.ReflectionResult
import com.mcis.memoir.data.memory.MemoryRepository
import com.mcis.memoir.ui.memory.photo.memory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReflectionViewModelAiTest {
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
    fun polishTransitionsIdleGeneratingReady() = runTest(mainDispatcher) {
        val client = mockk<ReflectionClient>()
        coEvery { client.generate(any()) } coAnswers {
            delay(1000)
            ReflectionResult.Success("caption")
        }
        val vm = viewModel(client = client)
        advanceUntilIdle()

        vm.state.test {
            assertEquals(AiState.Idle, awaitItem().aiState)

            vm.onIntent(ReflectionIntent.PolishClicked)
            runCurrent()
            assertEquals(AiState.Generating, awaitItem().aiState)

            advanceUntilIdle()
            assertEquals(AiState.Ready("caption"), awaitItem().aiState)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun polishWhileGeneratingIsNoOp() = runTest(mainDispatcher) {
        val client = mockk<ReflectionClient>()
        coEvery { client.generate(any()) } coAnswers {
            delay(1000)
            ReflectionResult.Success("caption")
        }
        val vm = viewModel(client = client)
        advanceUntilIdle()

        vm.onIntent(ReflectionIntent.PolishClicked)
        runCurrent() // now Generating, suspended in delay
        vm.onIntent(ReflectionIntent.PolishClicked) // dropped by guard
        advanceUntilIdle()

        coVerify(exactly = 1) { client.generate(any()) }
    }

    @Test
    fun regenerateProducesFreshCall() = runTest(mainDispatcher) {
        val client = mockk<ReflectionClient>()
        coEvery { client.generate(any()) } returns ReflectionResult.Success("caption")
        val vm = viewModel(client = client)
        advanceUntilIdle()

        vm.onIntent(ReflectionIntent.PolishClicked)
        advanceUntilIdle()
        vm.onIntent(ReflectionIntent.RegenerateClicked)
        advanceUntilIdle()

        coVerify(exactly = 2) { client.generate(any()) }
    }

    @Test
    fun failureTransitionsToErrorThenRetrySucceeds() = runTest(mainDispatcher) {
        val client = mockk<ReflectionClient>()
        coEvery { client.generate(any()) } returnsMany listOf(
            ReflectionResult.Failure(ReflectionError.Network),
            ReflectionResult.Success("caption")
        )
        val vm = viewModel(client = client)
        advanceUntilIdle()

        vm.onIntent(ReflectionIntent.PolishClicked)
        advanceUntilIdle()
        val errorState = vm.state.value.aiState
        assertInstanceOf(AiState.Error::class.java, errorState)
        assertEquals(ReflectionError.Network, (errorState as AiState.Error).kind)

        vm.onIntent(ReflectionIntent.PolishClicked)
        advanceUntilIdle()
        assertEquals(AiState.Ready("caption"), vm.state.value.aiState)
    }

    @Test
    fun copyWhileReadyEmitsCopyEffect() = runTest(mainDispatcher) {
        val client = mockk<ReflectionClient>()
        coEvery { client.generate(any()) } returns ReflectionResult.Success("caption")
        val vm = viewModel(client = client)
        advanceUntilIdle()
        vm.onIntent(ReflectionIntent.PolishClicked)
        advanceUntilIdle()

        vm.effects.test {
            vm.onIntent(ReflectionIntent.CopyClicked)
            advanceUntilIdle()
            assertEquals(ReflectionEffect.CopyToClipboard("caption"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun copyWhileNotReadyEmitsNothing() = runTest(mainDispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.effects.test {
            vm.onIntent(ReflectionIntent.CopyClicked)
            advanceUntilIdle()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun dismissAiErrorReturnsToIdle() = runTest(mainDispatcher) {
        val client = mockk<ReflectionClient>()
        coEvery { client.generate(any()) } returns ReflectionResult.Failure(ReflectionError.Unexpected)
        val vm = viewModel(client = client)
        advanceUntilIdle()
        vm.onIntent(ReflectionIntent.PolishClicked)
        advanceUntilIdle()
        assertInstanceOf(AiState.Error::class.java, vm.state.value.aiState)

        vm.onIntent(ReflectionIntent.DismissAiError)
        advanceUntilIdle()
        assertEquals(AiState.Idle, vm.state.value.aiState)
    }

    @Test
    fun savePersistsGeneratedReflectionWhenReady() = runTest(mainDispatcher) {
        val repo = mockk<MemoryRepository>(relaxed = true)
        every { repo.observe(memoryId) } returns MutableStateFlow(memory(emptyList()))
        val client = mockk<ReflectionClient>()
        coEvery { client.generate(any()) } returns ReflectionResult.Success("caption")
        val vm = viewModel(repo = repo, client = client)
        advanceUntilIdle()

        vm.onIntent(ReflectionIntent.PolishClicked)
        advanceUntilIdle()
        vm.onIntent(ReflectionIntent.SaveClicked)
        advanceUntilIdle()

        coVerify(exactly = 1) { repo.updateGeneratedReflection(memoryId, "caption") }
        coVerify(exactly = 1) { repo.complete(memoryId) }
    }

    @Test
    fun saveDoesNotPersistGeneratedReflectionWhenIdle() = runTest(mainDispatcher) {
        val repo = mockk<MemoryRepository>(relaxed = true)
        every { repo.observe(memoryId) } returns MutableStateFlow(memory(emptyList()))
        val vm = viewModel(repo = repo)
        advanceUntilIdle()

        vm.onIntent(ReflectionIntent.SaveClicked)
        advanceUntilIdle()

        coVerify(exactly = 0) { repo.updateGeneratedReflection(any(), any()) }
        coVerify(exactly = 1) { repo.complete(memoryId) }
    }

    private fun viewModel(
        repo: MemoryRepository = mockk(relaxed = true) {
            every { observe(memoryId) } returns MutableStateFlow(memory(emptyList()))
        },
        client: ReflectionClient = mockk(relaxed = true)
    ): ReflectionViewModel = ReflectionViewModel(
        memoryId = memoryId,
        repo = repo,
        reflectionClient = client,
        contentRepo = mockk<ContentRepository>(relaxed = true),
        resources = mockk<Resources>(relaxed = true),
        localeProvider = { Locale.ENGLISH }
    )
}
