package com.mcis.memoir.ui.memory.library

sealed interface MemoriesEffect {
    data class NavigateToWizard(val memoryId: String, val entry: WizardEntry) : MemoriesEffect
    data object NavigateToCreate : MemoriesEffect
    data class ShareMemory(val relativePaths: List<String>, val title: String) : MemoriesEffect
}

enum class WizardEntry { PHOTO_SELECTION, EDIT }
