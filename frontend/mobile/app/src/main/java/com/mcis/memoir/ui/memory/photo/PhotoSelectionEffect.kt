package com.mcis.memoir.ui.memory.photo

sealed interface PhotoSelectionEffect {
    data class LaunchPicker(val maxItems: Int) : PhotoSelectionEffect
    data class NavigateToEdit(val memoryId: String) : PhotoSelectionEffect
}
