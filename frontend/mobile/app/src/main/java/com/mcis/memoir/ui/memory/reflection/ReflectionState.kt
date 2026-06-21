package com.mcis.memoir.ui.memory.reflection

data class ReflectionState(
    val isLoading: Boolean = true,
    val overallMood: String = "",
    val userInsights: String = "",
    val postTripFeedback: String = "",
    val isSaving: Boolean = false,
    val error: String? = null,
    val aiState: AiState = AiState.Idle
)
