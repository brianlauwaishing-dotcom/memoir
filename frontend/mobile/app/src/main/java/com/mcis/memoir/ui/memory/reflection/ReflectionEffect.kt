package com.mcis.memoir.ui.memory.reflection

sealed interface ReflectionEffect {
    data object NavigateToMemoriesList : ReflectionEffect
    data class ShowError(val msg: String) : ReflectionEffect
}
