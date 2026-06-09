package com.mcis.memoir.ui.spot

data class SpotIntroState(
    val isLoading: Boolean = true,
    val spotId: String? = null,
    val title: String = "",
    val heroDrawableRes: Int = 0,
    val foundLabel: String = "",
    val discoveryItems: List<DiscoveryItemCard> = emptyList(),
    val error: String? = null
)

data class DiscoveryItemCard(
    val id: Int,
    val label: String,
    val imageDrawableRes: Int
)
