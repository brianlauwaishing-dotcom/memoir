package com.mcis.memoir.ui.memory.template

import com.mcis.memoir.R
import com.mcis.memoir.data.memory.TemplateSlot

object TemplateCatalog {
    val all: List<Template> = listOf(
        Template(
            id = "old_street",
            titleRes = R.string.template_old_street,
            descriptionRes = R.string.template_old_street_desc,
            imageRes = R.drawable.memory_templete_1,
            maskRes = R.drawable.memory_templete_1_mask,
            slots = listOf(
                TemplateSlot(0.0138f, 0.1106f, 0.56f, 0.4426f, 0f),
                TemplateSlot(0.4687f, 0.0658f, 0.5313f, 0.2721f, 0f),
                TemplateSlot(0f, 0.4f, 0.3783f, 0.247f, 0f),
                TemplateSlot(0.5717f, 0.2967f, 0.4283f, 0.5239f, 0f),
                TemplateSlot(0f, 0.6274f, 0.543f, 0.3056f, 0f)
            )
        ),
        Template(
            id = "city_walk",
            titleRes = R.string.template_city_walk,
            descriptionRes = R.string.template_city_walk_desc,
            imageRes = R.drawable.memory_templete_2,
            maskRes = R.drawable.memory_templete_2_mask,
            slots = listOf(
                TemplateSlot(0.034f, 0f, 0.8459f, 0.4563f, 0f),
                TemplateSlot(0.0266f, 0.3853f, 0.4463f, 0.2494f, 0f),
                TemplateSlot(0.5292f, 0.4705f, 0.4357f, 0.1896f, 0f),
                TemplateSlot(0.0223f, 0.6387f, 0.3858f, 0.1962f, 0f),
                TemplateSlot(0.5122f, 0.6666f, 0.4145f, 0.2147f, 0f)
            )
        ),
        Template(
            id = "taiwan_pop",
            titleRes = R.string.template_taiwan_pop,
            descriptionRes = R.string.template_taiwan_pop_desc,
            imageRes = R.drawable.memory_templete_3,
            maskRes = R.drawable.memory_templete_3_mask,
            slots = listOf(
                TemplateSlot(0.0138f, 0.1106f, 0.56f, 0.4426f, 0f),
                TemplateSlot(0.4687f, 0.0658f, 0.5313f, 0.2721f, 0f),
                TemplateSlot(0f, 0.4f, 0.3783f, 0.247f, 0f),
                TemplateSlot(0.5717f, 0.2967f, 0.4283f, 0.5239f, 0f),
                TemplateSlot(0f, 0.6274f, 0.543f, 0.3056f, 0f)
            )
        ),
        Template(
            id = "heritage_arch",
            titleRes = R.string.template_heritage_arch,
            descriptionRes = R.string.template_heritage_arch_desc,
            imageRes = R.drawable.memory_templete_4,
            maskRes = R.drawable.memory_templete_4_mask,
            slots = listOf(
                TemplateSlot(0.5078f, 0.1042f, 0.3438f, 0.2578f, 0f),
                TemplateSlot(0.1113f, 0.2702f, 0.4014f, 0.2454f, 0f),
                TemplateSlot(0.5078f, 0.3932f, 0.3135f, 0.2103f, 0f),
                TemplateSlot(0.1611f, 0.5612f, 0.2676f, 0.1745f, 0f),
                TemplateSlot(0.4629f, 0.6185f, 0.4063f, 0.1816f, 0f)
            )
        )
    )

    val ids: Set<String> = all.map { it.id }.toSet()

    fun byId(id: String): Template? = all.firstOrNull { it.id == id }
}
