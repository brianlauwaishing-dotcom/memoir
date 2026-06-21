package com.mcis.memoir.data.memory

data class Memory(
    val id: String,
    val templateId: String,
    val routeId: String?,
    val spotId: String?,
    val title: String,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
    val photoRelativePaths: List<String>,
    val spotNotes: Map<String, String>,
    val overallMood: String?,
    val userInsights: String,
    val postTripFeedback: String?,
    val generatedReflection: String?,
    val editorState: String?
)

data class TemplateSlot(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val rotation: Float = 0f
)
