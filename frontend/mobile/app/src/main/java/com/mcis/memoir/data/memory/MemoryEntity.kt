package com.mcis.memoir.data.memory

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey val id: String,
    val templateId: String,
    val routeId: String? = null,
    val spotId: String? = null,
    val title: String,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
    val photoLocalPaths: String,
    val spotNotes: String,
    val overallMood: String? = null,
    val userInsights: String = "",
    val postTripFeedback: String? = null,
    val generatedReflection: String? = null,
    val editorState: String? = null
)
