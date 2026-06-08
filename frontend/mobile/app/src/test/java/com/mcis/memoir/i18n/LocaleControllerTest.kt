package com.mcis.memoir.i18n

import androidx.core.os.LocaleListCompat
import com.mcis.memoir.data.prefs.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocaleControllerTest {
    @After
    fun tearDown() {
        Dispatchers.resetMain()
        LocaleController.resetLocaleSourceForTest()
    }

    @Test
    fun setLocalePersistsThenAppliesAppCompatLocale() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val prefs = FakeUserPreferencesRepository("en")
        val localeSource = FakeLocaleSource()
        LocaleController.replaceLocaleSourceForTest(localeSource)

        LocaleController.setLocale("zh", prefs)

        assertEquals(listOf("zh"), prefs.setLanguageCalls)
        assertEquals(listOf("zh"), localeSource.setCalls)
    }

    @Test
    fun reconcileWritesDataStoreWhenAppCompatHasLocale() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val prefs = FakeUserPreferencesRepository("en")
        val localeSource = FakeLocaleSource("zh")
        LocaleController.replaceLocaleSourceForTest(localeSource)

        LocaleController.reconcileAtStartup(prefs, TestScope(UnconfinedTestDispatcher(testScheduler)))
        advanceUntilIdle()

        assertEquals(listOf("zh"), prefs.setLanguageCalls)
        assertTrue(localeSource.setCalls.isEmpty())
    }

    @Test
    fun reconcileAppliesStoredTagWhenAppCompatIsEmpty() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val prefs = FakeUserPreferencesRepository("zh")
        val localeSource = FakeLocaleSource()
        LocaleController.replaceLocaleSourceForTest(localeSource)

        LocaleController.reconcileAtStartup(prefs, TestScope(UnconfinedTestDispatcher(testScheduler)))
        advanceUntilIdle()

        assertTrue(prefs.setLanguageCalls.isEmpty())
        assertEquals(listOf("zh"), localeSource.setCalls)
    }

    @Test
    fun reconcileDoesNothingWhenNeitherSourceHasLocale() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val prefs = FakeUserPreferencesRepository(null)
        val localeSource = FakeLocaleSource()
        LocaleController.replaceLocaleSourceForTest(localeSource)

        LocaleController.reconcileAtStartup(prefs, TestScope(UnconfinedTestDispatcher(testScheduler)))
        advanceUntilIdle()

        assertTrue(prefs.setLanguageCalls.isEmpty())
        assertTrue(localeSource.setCalls.isEmpty())
    }

    private class FakeLocaleSource(initialTags: String = "") : LocaleSource {
        private var locales = LocaleListCompat.forLanguageTags(initialTags)
        val setCalls = mutableListOf<String>()

        override fun appLocales(): LocaleListCompat = locales

        override fun setAppLocales(tags: LocaleListCompat) {
            setCalls += tags.toLanguageTags()
            locales = tags
        }
    }

    private class FakeUserPreferencesRepository(
        initialLanguage: String?
    ) : UserPreferencesRepository {
        private val rawLanguage = MutableStateFlow(initialLanguage)
        val setLanguageCalls = mutableListOf<String>()

        override val language: Flow<String> =
            rawLanguage.map { it ?: "en" }
        override val selectedInterests: Flow<Set<String>> = MutableStateFlow(emptySet())
        override val onboardingDone: Flow<Boolean> = MutableStateFlow(false)
        override val bookmarkedRouteIds: Flow<Set<String>> = MutableStateFlow(emptySet())

        override suspend fun setLanguage(tag: String) {
            setLanguageCalls += tag
            rawLanguage.value = tag
        }

        override suspend fun setInterests(set: Set<String>) = Unit
        override suspend fun markOnboardingDone() = Unit
        override suspend fun setBookmarkedRouteIds(set: Set<String>) = Unit
        override suspend fun persistedLanguageTag(): String? = rawLanguage.value
    }
}
