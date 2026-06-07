package com.mcis.memoir.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages persistent user preferences using SharedPreferences.
 */
class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LANGUAGE = "selected_language"
        private const val KEY_INTERESTS = "user_interests"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_SAVED_ROUTES = "saved_route_ids"
    }

    var selectedLanguage: String
        get() = prefs.getString(KEY_LANGUAGE, "en") ?: "en"
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    var userInterests: Set<String>
        get() = prefs.getStringSet(KEY_INTERESTS, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_INTERESTS, value).apply()

    var onboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()

    var savedRouteIds: Set<String>
        get() = prefs.getStringSet(KEY_SAVED_ROUTES, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_SAVED_ROUTES, value).apply()
}
