package com.mcis.memoir.ui.artifact

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class QuestionHighlightTest {
    @Test
    fun labelAtStart() {
        assertEquals(
            QuestionHighlight(prefix = "", label = "Dragon", suffix = ": how many?"),
            computeHighlight("Dragon: how many?", "Dragon")
        )
    }

    @Test
    fun labelAtEnd() {
        assertEquals(
            QuestionHighlight(prefix = "How many ", label = "Dragon", suffix = ""),
            computeHighlight("How many Dragon", "Dragon")
        )
    }

    @Test
    fun labelInMiddle() {
        assertEquals(
            QuestionHighlight(prefix = "How many ", label = "Dragon", suffix = " pillars?"),
            computeHighlight("How many Dragon pillars?", "Dragon")
        )
    }

    @Test
    fun labelNotFound() {
        assertEquals(
            QuestionHighlight(prefix = "How many?", label = "", suffix = ""),
            computeHighlight("How many?", "Dragon")
        )
    }

    @Test
    fun labelEmpty() {
        assertEquals(
            QuestionHighlight(prefix = "anything", label = "", suffix = ""),
            computeHighlight("anything", "")
        )
    }

    @Test
    fun labelAppearsTwiceHighlightsFirst() {
        assertEquals(
            QuestionHighlight(prefix = "", label = "Dragon", suffix = " and Dragon"),
            computeHighlight("Dragon and Dragon", "Dragon")
        )
    }

    @Test
    fun fullWidthChineseQuestion() {
        assertEquals(
            QuestionHighlight(prefix = "", label = "龍柱", suffix = "上有幾條龍呢？"),
            computeHighlight("龍柱上有幾條龍呢？", "龍柱")
        )
    }
}
