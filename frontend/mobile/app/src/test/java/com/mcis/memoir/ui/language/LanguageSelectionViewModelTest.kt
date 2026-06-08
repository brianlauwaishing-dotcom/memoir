package com.mcis.memoir.ui.language

import androidx.core.os.LocaleListCompat
import com.mcis.memoir.data.prefs.UserPreferencesRepository
import com.mcis.memoir.i18n.LocaleController
import com.mcis.memoir.i18n.LocaleSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LanguageSelectionViewModelTest {
    @After
    fun tearDown() {
        Dispatchers.resetMain()
        LocaleController.resetLocaleSourceForTest()
    }

    @Test
    fun initPreselectsPersistedLanguageAfterHydration() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = LanguageSelectionViewModel(FakeUserPreferencesRepository("en"))

        assertEquals(LanguageState(selected = null), viewModel.state.value)
        advanceUntilIdle()
        assertEquals(LanguageState(selected = "en"), viewModel.state.value)
    }

    @Test
    fun selectUpdatesStateWithoutEffect() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val viewModel = LanguageSelectionViewModel(FakeUserPreferencesRepository("en"))
        advanceUntilIdle()

        viewModel.onIntent(LanguageIntent.Select("zh"))

        assertEquals(LanguageState(selected = "zh"), viewModel.state.value)
        assertNull(withTimeoutOrNull(50) { viewModel.effects.first() })
    }

    @Test
    fun confirmPersistsSelectedLanguageOnceAndNavigates() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        LocaleController.replaceLocaleSourceForTest(FakeLocaleSource())
        val prefs = FakeUserPreferencesRepository("en")
        val viewModel = LanguageSelectionViewModel(prefs)
        advanceUntilIdle()

        viewModel.onIntent(LanguageIntent.Select("zh"))
        viewModel.onIntent(LanguageIntent.Confirm)
        advanceUntilIdle()

        assertEquals(listOf("zh"), prefs.setLanguageCalls)
        assertEquals(LanguageEffect.NavigateNext, viewModel.effects.first())
    }

    @Test
    fun confirmFailureEmitsErrorEffect() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        LocaleController.replaceLocaleSourceForTest(FakeLocaleSource())
        val prefs = FakeUserPreferencesRepository("zh").apply {
            failure = IllegalStateException("write failed")
        }
        val viewModel = LanguageSelectionViewModel(prefs)
        advanceUntilIdle()

        viewModel.onIntent(LanguageIntent.Confirm)
        advanceUntilIdle()

        val effect = viewModel.effects.first()
        assertTrue(effect is LanguageEffect.ShowError)
        assertEquals("write failed", (effect as LanguageEffect.ShowError).msg)
    }

    @Test
    fun doubleConfirmWhileApplyingEmitsAtMostOneNavigation() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        LocaleController.replaceLocaleSourceForTest(FakeLocaleSource())
        val gate = CompletableDeferred<Unit>()
        val prefs = FakeUserPreferencesRepository("zh").apply {
            setLanguageGate = gate
        }
        val viewModel = LanguageSelectionViewModel(prefs)
        advanceUntilIdle()

        viewModel.onIntent(LanguageIntent.Confirm)
        viewModel.onIntent(LanguageIntent.Confirm)
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(listOf("zh"), prefs.setLanguageCalls)
        assertEquals(LanguageEffect.NavigateNext, viewModel.effects.first())
        assertNull(withTimeoutOrNull(50) { viewModel.effects.first() })
    }

    @Test
    fun confirmBeforeHydrationIsNoOp() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        LocaleController.replaceLocaleSourceForTest(FakeLocaleSource())
        val prefs = FakeUserPreferencesRepository("en")
        val viewModel = LanguageSelectionViewModel(prefs)

        viewModel.onIntent(LanguageIntent.Confirm)
        advanceUntilIdle()

        assertTrue(prefs.setLanguageCalls.isEmpty())
        assertNull(withTimeoutOrNull(50) { viewModel.effects.first() })
        assertEquals(LanguageState(selected = "en"), viewModel.state.value)
    }

    private class FakeLocaleSource : LocaleSource {
        override fun appLocales(): LocaleListCompat =
            LocaleListCompat.getEmptyLocaleList()

        override fun setAppLocales(tags: LocaleListCompat) = Unit
    }

    private class FakeUserPreferencesRepository(
        initialLanguage: String?
    ) : UserPreferencesRepository {
        private val rawLanguage = MutableStateFlow(initialLanguage)
        val setLanguageCalls = mutableListOf<String>()
        var failure: Throwable? = null
        var setLanguageGate: CompletableDeferred<Unit>? = null

        override val language: Flow<String> =
            rawLanguage.map { it ?: "en" }
        override val selectedInterests: Flow<Set<String>> = MutableStateFlow(emptySet())
        override val onboardingDone: Flow<Boolean> = MutableStateFlow(false)
        override val bookmarkedRouteIds: Flow<Set<String>> = MutableStateFlow(emptySet())

        override suspend fun setLanguage(tag: String) {
            failure?.let { throw it }
            setLanguageCalls += tag
            setLanguageGate?.await()
            rawLanguage.value = tag
        }

        override suspend fun setInterests(set: Set<String>) = Unit
        override suspend fun markOnboardingDone() = Unit
        override suspend fun setBookmarkedRouteIds(set: Set<String>) = Unit
        override suspend fun persistedLanguageTag(): String? = rawLanguage.value
    }
}
