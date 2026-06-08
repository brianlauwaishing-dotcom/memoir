package com.mcis.memoir.data.content.model

import kotlinx.serialization.Serializable

@Serializable
data class Spot(
    val id: String,
    val title: LocalizedText,
    val heroImage: String,
    val duration: LocalizedText,
    val whyItMatters: LocalizedText,
    val historicalContext: LocalizedText,
    val architecturalFeatures: LocalizedText,
    val modernUse: LocalizedText,
    val facts: LocalizedFacts,
    val photographyTips: List<PhotographyTip> = emptyList(),
    val artifacts: List<Artifact> = emptyList()
)
