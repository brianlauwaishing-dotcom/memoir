package com.mcis.memoir.ui.route

sealed interface RouteDetailEffect {
    data class ShowError(val msg: String) : RouteDetailEffect
}
