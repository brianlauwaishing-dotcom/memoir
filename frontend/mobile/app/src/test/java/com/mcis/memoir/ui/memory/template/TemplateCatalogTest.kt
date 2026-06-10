package com.mcis.memoir.ui.memory.template

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TemplateCatalogTest {
    @Test
    fun catalogHasFourDistinctTemplatesWithNonZeroResources() {
        assertEquals(4, TemplateCatalog.all.size)
        assertEquals(4, TemplateCatalog.ids.size)
        TemplateCatalog.all.forEach { template ->
            assertTrue(template.titleRes != 0)
            assertTrue(template.descriptionRes != 0)
            assertTrue(template.imageRes != 0)
            assertTrue(template.maskRes != 0)
        }
    }
}
