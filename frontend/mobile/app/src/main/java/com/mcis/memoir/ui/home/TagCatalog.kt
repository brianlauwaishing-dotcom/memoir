package com.mcis.memoir.ui.home

import com.mcis.memoir.R

// Keep tag id list in sync with data/scripts/generate_content.py.
// Both must agree on the set used to validate route JSON.
object TagCatalog {
    val all: List<Tag> = listOf(
        Tag("temples", R.string.culture_temples),
        Tag("old_streets", R.string.culture_old_streets),
        Tag("architecture", R.string.culture_architecture),
        Tag("trade", R.string.culture_trade),
        Tag("colonial", R.string.culture_colonial),
        Tag("crafts", R.string.culture_crafts)
    )

    val ids: Set<String> = all.map { it.id }.toSet()

    fun byId(id: String): Tag? = all.firstOrNull { it.id == id }
}
