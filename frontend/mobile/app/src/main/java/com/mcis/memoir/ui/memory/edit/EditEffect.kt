package com.mcis.memoir.ui.memory.edit

sealed interface EditEffect {
    data class NavigateToReflection(val memoryId: String) : EditEffect
}
