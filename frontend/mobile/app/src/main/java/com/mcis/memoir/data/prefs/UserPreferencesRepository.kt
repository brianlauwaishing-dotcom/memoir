package com.mcis.memoir.data.prefs

import java.util.Locale
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val language: Flow<String>
    val selectedInterests: Flow<Set<String>>
    val onboardingDone: Flow<Boolean>
    val bookmarkedRouteIds: Flow<Set<String>>

    suspend fun setLanguage(tag: String)
    suspend fun setInterests(set: Set<String>)
    suspend fun markOnboardingDone()
    suspend fun setBookmarkedRouteIds(set: Set<String>)
    suspend fun persistedLanguageTag(): String?
}

fun defaultLocaleTag(): String =
    if (Locale.getDefault().language == "zh") "zh" else "en"
