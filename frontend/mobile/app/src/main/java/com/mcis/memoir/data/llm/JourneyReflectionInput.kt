package com.mcis.memoir.data.llm

import java.util.Locale

data class JourneyReflectionInput(
    val locale: Locale,
    val routeId: String,
    val spotEntries: List<SpotEntry>,
    val overallMood: String?,
    val userInsights: String,
    val postTripFeedback: String?,
    val templateStyle: String
)
