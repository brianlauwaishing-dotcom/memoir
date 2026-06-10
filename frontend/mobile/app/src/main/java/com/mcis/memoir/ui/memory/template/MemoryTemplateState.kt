package com.mcis.memoir.ui.memory.template

import androidx.annotation.DrawableRes

data class TemplateCard(
    val id: String,
    val title: String,
    val description: String,
    @DrawableRes val imageRes: Int,
    @DrawableRes val maskRes: Int
)

data class MemoryTemplateState(
    val templates: List<TemplateCard> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)
