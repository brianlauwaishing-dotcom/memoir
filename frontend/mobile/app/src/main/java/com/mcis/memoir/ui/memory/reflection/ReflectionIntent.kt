package com.mcis.memoir.ui.memory.reflection

sealed interface ReflectionIntent {
    data class MoodChanged(val text: String) : ReflectionIntent
    data class InsightsChanged(val text: String) : ReflectionIntent
    data class FeedbackChanged(val text: String) : ReflectionIntent
    data object SaveClicked : ReflectionIntent
}
