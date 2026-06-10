package com.mcis.memoir.ui.artifact

data class ArtifactDiscoveryState(
    val isLoading: Boolean = true,
    val spotId: String? = null,
    val artifactId: Int = 0,
    val displayPosition: Int = 0,
    val totalArtifacts: Int = 0,
    val capturedArtifactsCount: Int = 0,
    val label: String = "",
    val highlight: QuestionHighlight = QuestionHighlight("", "", ""),
    val imageDrawableRes: Int = 0,
    val error: String? = null
)
