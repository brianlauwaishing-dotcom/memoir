package com.mcis.memoir.data.prefs

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

internal object UserPrefsKeys {
    val LANGUAGE = stringPreferencesKey("selected_language")
    val INTERESTS = stringSetPreferencesKey("user_interests")
    val ONBOARDING_DONE = booleanPreferencesKey("onboarding_completed")
    val BOOKMARKED_ROUTES = stringSetPreferencesKey("saved_route_ids")
    val CAPTURED_ARTIFACTS = stringSetPreferencesKey("captured_artifact_keys")
}
