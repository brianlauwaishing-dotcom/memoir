package com.mcis.memoir.ui.memory.library

data class MemoryCard(
    val id: String,
    val title: String,
    val coverRelativePath: String?,
    val status: String,
    val dateLabel: String,
    val draftProgress: DraftProgress
)

data class DraftProgress(val current: Int, val total: Int)
