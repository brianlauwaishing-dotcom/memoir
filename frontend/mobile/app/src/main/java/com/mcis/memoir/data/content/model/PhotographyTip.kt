package com.mcis.memoir.data.content.model

import kotlinx.serialization.Serializable

@Serializable
data class PhotographyTip(
    val id: Int,
    val description: LocalizedText,
    val image: String
)
