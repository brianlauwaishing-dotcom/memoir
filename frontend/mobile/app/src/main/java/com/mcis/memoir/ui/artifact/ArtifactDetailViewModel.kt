package com.mcis.memoir.ui.artifact

import android.content.res.Resources
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mcis.memoir.data.content.ContentRepository
import com.mcis.memoir.data.prefs.UserPreferencesRepository
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ArtifactDetailViewModel(
    private val spotId: String,
    private val artifactId: Int,
    private val contentRepo: ContentRepository,
    private val prefsRepo: UserPreferencesRepository,
    private val resources: Resources,
    private val localeProvider: () -> Locale
) : ViewModel() {
    private val _state = MutableStateFlow(ArtifactDetailState())
    val state: StateFlow<ArtifactDetailState> = _state.asStateFlow()
    private val bookmarkMutex = Mutex()

    init {
        viewModelScope.launch {
            val spot = contentRepo.spot(spotId)
            val artifact = spot?.artifacts?.firstOrNull { it.id == artifactId }
            if (spot == null || artifact == null) {
                _state.value = ArtifactDetailState(isLoading = false, error = "artifact_not_found")
                return@launch
            }

            val locale = localeProvider()
            val baseState = ArtifactDetailState(
                isLoading = false,
                label = artifact.title[locale],
                description = artifact.description[locale],
                imageDrawableRes = drawable(artifact.image)
            )
            prefsRepo.bookmarkedSpotIds.collectLatest { bookmarkedSpotIds ->
                _state.value = baseState.copy(isBookmarked = spot.id in bookmarkedSpotIds)
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
