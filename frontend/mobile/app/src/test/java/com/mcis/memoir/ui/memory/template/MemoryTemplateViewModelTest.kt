package com.mcis.memoir.ui.memory.template

import android.content.res.Resources
import app.cash.turbine.test
import com.mcis.memoir.R
import com.mcis.memoir.data.memory.MemoryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class MemoryTemplateViewModelTest {
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
    fun templateClickedStartsDraftAndEmitsNavigationEffect() = runTest(mainDispatcher) {
        val repo = mockk<MemoryRepository>()
        val memoryId = "11111111-1111-4111-8111-111111111111"
        coEvery { repo.startDraft("old_street", "string-${R.string.template_old_street}") } returns memoryId
        val vm = MemoryTemplateViewModel(repo, resources(), localeProvider = { Locale.ENGLISH })

        assertFalse(vm.state.value.isLoading)
        assertEquals(4, vm.state.value.templates.size)

        vm.effects.test {
            vm.onIntent(MemoryTemplateIntent.TemplateClicked("old_street"))
            advanceUntilIdle()

            assertEquals(MemoryTemplateEffect.NavigateToPhotoSelection(memoryId), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 1) { repo.startDraft("old_street", "string-${R.string.template_old_street}") }
    }

    private fun resources(): Resources {
        val resources = mockk<Resources>()
        every { resources.getString(any<Int>()) } answers { "string-${firstArg<Int>()}" }
        return resources
    }
}
