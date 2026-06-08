package com.mcis.memoir.ui.home

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TagCatalogTest {
    @Test
    fun tagSetIsNonEmptyAndHasNoDuplicateIds() {
        assertTrue(TagCatalog.all.isNotEmpty())
        assertEquals(TagCatalog.all.size, TagCatalog.ids.size)
    }

    @Test
    fun everyTagReferencesAStringResource() {
        TagCatalog.all.forEach { tag ->
            assertTrue(tag.labelRes != 0, "labelRes for ${tag.id} must resolve at compile time")
        }
    }
}
