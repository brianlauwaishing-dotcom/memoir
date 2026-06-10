package com.mcis.memoir.ui.memory.template

sealed interface MemoryTemplateEffect {
    data class NavigateToPhotoSelection(val memoryId: String) : MemoryTemplateEffect
}
