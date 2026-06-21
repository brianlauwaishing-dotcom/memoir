package com.mcis.memoir.ui.spot

data class SpotDetailState(
    val isLoading: Boolean = true,
    val title: String = "",
    val heroDrawableRes: Int = 0,
    val duration: String = "",
    val whyItMatters: String = "",
    val historicalContext: String = "",
    val architecturalFeatures: String = "",
    val modernUse: String = "",
    val feelings: List<String> = emptyList(),
    val discoveryItems: List<SpotDetailGalleryItem> = emptyList(),
    val error: String? = null
)

data class SpotDetailGalleryItem(
    val id: Int,
    val label: String,
    val imageDrawableRes: Int
)
