package com.mcis.memoir.ui.saved

import com.mcis.memoir.ui.home.RouteCard

data class SavedState(
    val isLoading: Boolean = false,
    val cards: List<RouteCard> = emptyList(),
    val error: String? = null
)
