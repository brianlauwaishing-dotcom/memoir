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

class SpotDetailViewModel(
    private val spotId: String,
    private val contentRepo: ContentRepository,
    private val resources: Resources,
    private val localeProvider: () -> Locale
) : ViewModel() {
    private val _state = MutableStateFlow(SpotDetailState())
    val state: StateFlow<SpotDetailState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val spot = contentRepo.spot(spotId)
            if (spot == null) {
                _state.value = SpotDetailState(isLoading = false, error = "spot_not_found")
                return@launch
            }

            val locale = localeProvider()
            val isChinese = locale.language == "zh"
            _state.value = SpotDetailState(
                isLoading = false,
                title = spot.title[locale],
                heroDrawableRes = drawable(spot.heroImage),
                duration = spot.duration[locale],
                whyItMatters = spot.whyItMatters[locale],
                historicalContext = spot.historicalContext[locale],
                architecturalFeatures = spot.architecturalFeatures[locale],
                modernUse = spot.modernUse[locale],
                feelings = if (isChinese) DEFAULT_FEELINGS_ZH else DEFAULT_FEELINGS_EN,
                discoveryItems = spot.artifacts.map { artifact ->
                    SpotDetailGalleryItem(
                        id = artifact.id,
                        label = artifact.title[locale],
                        imageDrawableRes = drawable(artifact.galleryImage ?: artifact.image)
                    )
                }
            )
        }
    }

    private fun drawable(name: String): Int =
        resources.getIdentifier(name, "drawable", "com.mcis.memoir")

    private companion object {
        // Feelings are a fixed UI affordance, not designer-authored content, so they
        // are not modeled on Spot. The same default chip set is shown for every spot.
        val DEFAULT_FEELINGS_EN = listOf("Awe", "Peaceful", "Inspired", "Curious", "Grateful", "Amazed")
        val DEFAULT_FEELINGS_ZH = listOf("敬畏", "平靜", "受啟發", "好奇", "感激", "驚嘆")
    }
}
