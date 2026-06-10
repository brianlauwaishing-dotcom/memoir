package com.mcis.memoir.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class DataStoreUserPreferencesRepository(
    private val dataStore: DataStore<Preferences>
) : UserPreferencesRepository {
    override val language: Flow<String> =
        dataStore.data.map { preferences ->
            preferences[UserPrefsKeys.LANGUAGE] ?: defaultLocaleTag()
        }

    override val selectedInterests: Flow<Set<String>> =
        dataStore.data.map { preferences ->
            preferences[UserPrefsKeys.INTERESTS].orEmpty()
        }

    override val onboardingDone: Flow<Boolean> =
        dataStore.data.map { preferences ->
            preferences[UserPrefsKeys.ONBOARDING_DONE] ?: false
        }

    override val bookmarkedRouteIds: Flow<Set<String>> =
        dataStore.data.map { preferences ->
            preferences[UserPrefsKeys.BOOKMARKED_ROUTES].orEmpty()
        }

    override val capturedArtifactKeys: Flow<Set<String>> =
        dataStore.data.map { preferences ->
            preferences[UserPrefsKeys.CAPTURED_ARTIFACTS].orEmpty()
        }

    override suspend fun setLanguage(tag: String) {
        dataStore.edit { preferences ->
            preferences[UserPrefsKeys.LANGUAGE] = tag
        }
    }

    override suspend fun setInterests(set: Set<String>) {
        dataStore.edit { preferences ->
            preferences[UserPrefsKeys.INTERESTS] = set
        }
    }

    override suspend fun markOnboardingDone() {
        dataStore.edit { preferences ->
            preferences[UserPrefsKeys.ONBOARDING_DONE] = true
        }
    }

    override suspend fun setBookmarkedRouteIds(set: Set<String>) {
        dataStore.edit { preferences ->
            preferences[UserPrefsKeys.BOOKMARKED_ROUTES] = set
        }
    }

    override suspend fun setCapturedArtifactKeys(set: Set<String>) {
        dataStore.edit { preferences ->
            preferences[UserPrefsKeys.CAPTURED_ARTIFACTS] = set
        }
    }

    override suspend fun persistedLanguageTag(): String? =
        dataStore.data.first()[UserPrefsKeys.LANGUAGE]
}
