package com.mcis.memoir.ui.memory.template

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.mcis.memoir.data.memory.TemplateSlot

data class Template(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    @DrawableRes val imageRes: Int,
    @DrawableRes val maskRes: Int,
    val slots: List<TemplateSlot>
)
