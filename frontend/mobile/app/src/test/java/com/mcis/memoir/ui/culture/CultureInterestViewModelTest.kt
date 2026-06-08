package com.mcis.memoir.ui.culture

import app.cash.turbine.test
import com.mcis.memoir.data.prefs.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CultureInterestViewModelTest {
    private val mainDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun toggleAddsTag() = runTest {
        val prefs = FakePrefs(emptySet())
        val vm = CultureInterestViewModel(prefs)

        vm.selected.test {
            assertEquals(emptySet<String>(), awaitItem())
            vm.toggle("temples")
            advanceUntilIdle()

            assertEquals(listOf(setOf("temples")), prefs.savedInterests)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun toggleRemovesExistingTag() = runTest {
        val prefs = FakePrefs(setOf("temples"))
        val vm = CultureInterestViewModel(prefs)

        vm.selected.test {
            assertEquals(setOf("temples"), awaitItem())
            vm.toggle("temples")
            advanceUntilIdle()

            assertEquals(listOf(emptySet<String>()), prefs.savedInterests)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun skipClearsInterests() = runTest {
        val prefs = FakePrefs(setOf("temples", "crafts"))
        val vm = CultureInterestViewModel(prefs)

        vm.skip()
        advanceUntilIdle()

        assertEquals(listOf(emptySet<String>()), prefs.savedInterests)
    }

    private class FakePrefs(initialInterests: Set<String>) : UserPreferencesRepository {
        private val interests = MutableStateFlow(initialInterests)
        val savedInterests = mutableListOf<Set<String>>()

        override val language: Flow<String> = MutableStateFlow("en")
        override val selectedInterests: Flow<Set<String>> = interests
        override val onboardingDone: Flow<Boolean> = MutableStateFlow(false)
        override val bookmarkedRouteIds: Flow<Set<String>> = MutableStateFlow(emptySet())

        override suspend fun setLanguage(tag: String) = Unit

        override suspend fun setInterests(set: Set<String>) {
            savedInterests += set
            interests.value = set
        }

        override suspend fun markOnboardingDone() = Unit

        override suspend fun setBookmarkedRouteIds(set: Set<String>) = Unit

        override suspend fun persistedLanguageTag(): String? = null
    }
}
