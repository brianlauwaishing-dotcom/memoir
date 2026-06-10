package com.mcis.memoir.ui.spot

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
import kotlinx.coroutines.launch

class SpotIntroViewModel(
    private val spotId: String,
    private val contentRepo: ContentRepository,
    private val prefsRepo: UserPreferencesRepository,
    private val resources: Resources,
    private val localeProvider: () -> Locale
) : ViewModel() {
    private val _state = MutableStateFlow(SpotIntroState())
    val state: StateFlow<SpotIntroState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val spot = contentRepo.spot(spotId)
            if (spot == null) {
                _state.value = SpotIntroState(isLoading = false, error = "spot_not_found")
                return@launch
            }

            val locale = localeProvider()
            val discoveryItems = spot.artifacts.map { artifact ->
                DiscoveryItemCard(
                    id = artifact.id,
                    label = artifact.title[locale],
                    imageDrawableRes = drawable(artifact.image)
                )
            }
            val baseState = SpotIntroState(
                isLoading = false,
                spotId = spot.id,
                title = spot.title[locale],
                heroDrawableRes = drawable(spot.heroImage),
                discoveryItems = discoveryItems
            )
            prefsRepo.capturedArtifactKeys.collectLatest { capturedKeys ->
                val capturedCount = spot.artifacts.count { artifact ->
                    artifactCaptureKey(spot.id, artifact.id) in capturedKeys
                }
                _state.value = baseState.copy(foundLabel = "$capturedCount/${spot.artifacts.size}")
            }
        }
    }

    private fun drawable(name: String): Int =
        resources.getIdentifier(name, "drawable", "com.mcis.memoir")
}
