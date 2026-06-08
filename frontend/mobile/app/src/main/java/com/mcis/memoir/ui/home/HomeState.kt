package com.mcis.memoir.ui.home

data class HomeState(
    val cards: List<RouteCard> = emptyList(),
    val isLoading: Boolean = false,
    val query: String = "",
    val activeTags: Set<String> = emptySet(),
    val userInterests: Set<String> = emptySet(),
    val error: String? = null
)
