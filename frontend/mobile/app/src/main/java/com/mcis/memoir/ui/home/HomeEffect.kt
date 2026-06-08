package com.mcis.memoir.ui.home

sealed interface HomeEffect {
    data class NavigateToRoute(val routeId: String) : HomeEffect
}
