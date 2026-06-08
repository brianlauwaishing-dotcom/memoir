package com.mcis.memoir.data.content.model

import kotlinx.serialization.Serializable

@Serializable
data class Route(
    val id: String,
    val title: LocalizedText,
    val category: LocalizedText,
    val heroImage: String,
    val description: LocalizedText,
    val tags: List<String> = emptyList(),
    val journey: List<JourneyStop> = emptyList()
)

@Serializable
data class JourneyStop(
    val order: Int,
    val spotId: String,
    val title: LocalizedText
)
