package com.mcis.memoir.ui.memory.template

sealed interface MemoryTemplateIntent {
    data class TemplateClicked(val templateId: String) : MemoryTemplateIntent
}
