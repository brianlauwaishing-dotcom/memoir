package com.mcis.memoir.data.content.model

import kotlinx.serialization.Serializable

@Serializable
data class LocalizedFacts(
    val en: List<String> = emptyList(),
    val zh: List<String> = emptyList()
)
