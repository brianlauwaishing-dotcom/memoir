package com.mcis.memoir.ui.route

sealed interface RouteDetailIntent {
    data object BookmarkToggled : RouteDetailIntent
    data class SpotClicked(val spotId: String) : RouteDetailIntent
}
