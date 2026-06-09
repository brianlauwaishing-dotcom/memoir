package com.mcis.memoir.ui.spot

import android.content.res.Resources
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mcis.memoir.data.content.ContentRepository
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SpotIntroViewModel(
    private val spotId: String,
    private val contentRepo: ContentRepository,
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
            _state.value = SpotIntroState(
                isLoading = false,
                spotId = spot.id,
                title = spot.title[locale],
                heroDrawableRes = drawable(spot.heroImage),
                foundLabel = "${spot.artifacts.size}/${spot.artifacts.size}",
                discoveryItems = spot.artifacts.map { artifact ->
                    DiscoveryItemCard(
                        id = artifact.id,
                        label = artifact.title[locale],
                        imageDrawableRes = drawable(artifact.image)
                    )
                }
            )
        }
    }

    private fun drawable(name: String): Int =
        resources.getIdentifier(name, "drawable", "com.mcis.memoir")
}
