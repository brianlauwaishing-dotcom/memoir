package com.mcis.memoir.data.content.model

import kotlinx.serialization.Serializable

@Serializable
data class Artifact(
    val id: Int,
    val title: LocalizedText,
    val description: LocalizedText,
    val question: LocalizedText,
    val image: String,
    val galleryImage: String? = null
)
