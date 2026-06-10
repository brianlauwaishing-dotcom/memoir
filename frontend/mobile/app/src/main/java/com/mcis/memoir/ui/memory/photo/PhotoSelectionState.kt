package com.mcis.memoir.ui.memory.photo

data class PhotoSelectionState(
    val isLoading: Boolean = true,
    val memoryId: String? = null,
    val photoPaths: List<String> = emptyList(),
    val maxPhotos: Int = 5,
    val error: String? = null
)
