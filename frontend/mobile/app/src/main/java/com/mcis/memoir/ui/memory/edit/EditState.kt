package com.mcis.memoir.ui.memory.edit

import com.mcis.memoir.data.memory.TemplateSlot

data class EditState(
    val isLoading: Boolean = true,
    val memoryId: String? = null,
    val templateId: String? = null,
    val templateImageRes: Int = 0,
    val templateMaskRes: Int = 0,
    val templateSlots: List<TemplateSlot> = emptyList(),
    val photoPaths: List<String> = emptyList(),
    val error: String? = null
)
