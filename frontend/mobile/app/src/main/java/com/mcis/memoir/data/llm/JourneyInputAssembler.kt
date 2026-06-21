package com.mcis.memoir.data.llm

import com.mcis.memoir.data.content.ContentRepository
import com.mcis.memoir.data.memory.Memory
import java.util.Locale
import kotlin.math.max

object JourneyInputAssembler {
    suspend fun build(
        memory: Memory,
        locale: Locale,
        contentRepo: ContentRepository
    ): JourneyReflectionInput {
        val routeId = memory.routeId ?: "(none)"
        val route = memory.routeId?.let { contentRepo.route(it) }
        val journey = route?.journey.orEmpty()
        val spotEntries = journey.map { stop ->
            SpotEntry(
                spotId = stop.spotId,
                userNote = null, // memory.spotNotes is reserved for a future change
                photoCount = memory.photoRelativePaths.size / max(journey.size, 1)
            )
        }
        return JourneyReflectionInput(
            locale = locale,
            routeId = routeId,
            spotEntries = spotEntries,
            overallMood = memory.overallMood,
            userInsights = memory.userInsights,
            postTripFeedback = memory.postTripFeedback,
            templateStyle = memory.templateId
        )
    }
}
