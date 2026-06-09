package com.mcis.memoir.ui.route

data class RouteDetailState(
    val isLoading: Boolean = false,
    val routeId: String? = null,
    val title: String = "",
    val description: String = "",
    val heroDrawableRes: Int = 0,
    val journey: List<JourneyRowState> = emptyList(),
    val isSaved: Boolean = false,
    val error: String? = null
)

data class JourneyRowState(
    val order: Int,
    val spotId: String,
    val label: String
)
