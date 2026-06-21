package com.mcis.memoir.data.prefs

import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface UserPreferencesRepository {
    val language: Flow<String>
    val selectedInterests: Flow<Set<String>>
    val onboardingDone: Flow<Boolean>
    val bookmarkedRouteIds: Flow<Set<String>>
    val bookmarkedSpotIds: Flow<Set<String>>
        get() = flowOf(emptySet())
    val capturedArtifactKeys: Flow<Set<String>>
        get() = flowOf(emptySet())

    suspend fun setLanguage(tag: String)
    suspend fun setInterests(set: Set<String>)
    suspend fun markOnboardingDone()
    suspend fun setBookmarkedRouteIds(set: Set<String>)
    suspend fun setBookmarkedSpotIds(set: Set<String>) = Unit
    suspend fun setCapturedArtifactKeys(set: Set<String>) = Unit
    suspend fun persistedLanguageTag(): String?
}

fun defaultLocaleTag(): String =
    if (Locale.getDefault().language == "zh") "zh" else "en"

fun artifactCaptureKey(spotId: String, artifactId: Int): String = "$spotId:$artifactId"
