package com.mcis.memoir.ui.artifact

import android.content.res.Resources
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mcis.memoir.data.content.ContentRepository
import com.mcis.memoir.data.prefs.UserPreferencesRepository
import com.mcis.memoir.data.prefs.artifactCaptureKey
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ArtifactDiscoveryViewModel(
    private val spotId: String,
    private val artifactId: Int,
    private val contentRepo: ContentRepository,
    private val prefsRepo: UserPreferencesRepository,
    private val resources: Resources,
    private val localeProvider: () -> Locale
) : ViewModel() {
    private val _state = MutableStateFlow(ArtifactDiscoveryState())
    val state: StateFlow<ArtifactDiscoveryState> = _state.asStateFlow()
    private val bookmarkMutex = Mutex()

    init {
        viewModelScope.launch {
            val spot = contentRepo.spot(spotId)
            val artifact = spot?.artifacts?.firstOrNull { it.id == artifactId }
            if (spot == null || artifact == null) {
                _state.value = ArtifactDiscoveryState(isLoading = false, error = "artifact_not_found")
                return@launch
            }

            val locale = localeProvider()
            val label = artifact.title[locale]
            val question = artifact.question[locale]
            val baseState = ArtifactDiscoveryState(
                isLoading = false,
                spotId = spot.id,
                artifactId = artifact.id,
                displayPosition = spot.artifacts.indexOfFirst { it.id == artifact.id } + 1,
                totalArtifacts = spot.artifacts.size,
                label = label,
                highlight = computeHighlight(question, label),
                imageDrawableRes = drawable(artifact.image)
            )
            combine(
                prefsRepo.capturedArtifactKeys,
                prefsRepo.bookmarkedSpotIds
            ) { capturedKeys, bookmarkedSpotIds ->
                val capturedCount = spot.artifacts.count { artifact ->
                    artifactCaptureKey(spot.id, artifact.id) in capturedKeys
                }
                baseState.copy(
                    capturedArtifactsCount = capturedCount,
                    isBookmarked = spot.id in bookmarkedSpotIds
                )
            }.collectLatest { nextState ->
                _state.value = nextState
            }
        }
    }

    fun onBookmarkClick() {
        viewModelScope.launch {
            bookmarkMutex.withLock {
                val current = prefsRepo.bookmarkedSpotIds.first()
                val next = if (spotId in current) current - spotId else current + spotId
                prefsRepo.setBookmarkedSpotIds(next)
            }
        }
    }

    private fun drawable(name: String): Int =
        resources.getIdentifier(name, "drawable", "com.mcis.memoir")
}
