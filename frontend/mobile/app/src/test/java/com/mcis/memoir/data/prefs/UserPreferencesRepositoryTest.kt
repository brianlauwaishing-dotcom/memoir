package com.mcis.memoir.data.prefs

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class UserPreferencesRepositoryTest {
    @Test
    fun settersRoundTripThroughDataStore() = runTest {
        repository(this).apply {
            setLanguage("zh")

            assertEquals("zh", language.first())
            assertEquals("zh", persistedLanguageTag())
        }
        repository(this).apply {
            setInterests(setOf("temples", "crafts"))

            assertEquals(setOf("temples", "crafts"), selectedInterests.first())
        }
        repository(this).apply {
            markOnboardingDone()

            assertEquals(true, onboardingDone.first())
        }
        repository(this).apply {
            setBookmarkedRouteIds(setOf("route-a", "route-b"))

            assertEquals(setOf("route-a", "route-b"), bookmarkedRouteIds.first())
        }
        repository(this).apply {
            setCapturedArtifactKeys(setOf("spot-a:1", "spot-b:4"))

            assertEquals(setOf("spot-a:1", "spot-b:4"), capturedArtifactKeys.first())
        }
    }

    @Test
    fun languageFallsBackToSystemChineseWhenNothingPersisted() = runTest {
        val previous = Locale.getDefault()
        Locale.setDefault(Locale.CHINESE)
        try {
            val repository = repository(this)

            assertEquals(defaultLocaleTag(), repository.language.first())
            assertEquals("zh", repository.language.first())
            assertEquals(null, repository.persistedLanguageTag())
        } finally {
            Locale.setDefault(previous)
        }
    }

    private fun repository(scope: TestScope): DataStoreUserPreferencesRepository {
        val file = File.createTempFile("user-prefs", ".preferences_pb")
        file.delete()
        file.deleteOnExit()
        val dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { file }
        )
        return DataStoreUserPreferencesRepository(dataStore)
    }
}
