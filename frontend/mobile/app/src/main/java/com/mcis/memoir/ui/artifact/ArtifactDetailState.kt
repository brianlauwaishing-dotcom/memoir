package com.mcis.memoir.ui.artifact

data class ArtifactDetailState(
    val isLoading: Boolean = true,
    val label: String = "",
    val description: String = "",
    val imageDrawableRes: Int = 0,
    val error: String? = null
)
