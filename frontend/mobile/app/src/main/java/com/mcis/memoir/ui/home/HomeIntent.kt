package com.mcis.memoir.ui.home

sealed interface HomeIntent {
    data class SearchChanged(val q: String) : HomeIntent
    data class FilterTagToggled(val tagId: String) : HomeIntent
    data class CardClicked(val routeId: String) : HomeIntent
}
