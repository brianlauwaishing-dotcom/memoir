package com.mcis.memoir.data.llm

import com.aallam.openai.api.chat.ChatRole
import java.util.Locale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PromptBuilderTest {

    private fun input(
        locale: Locale = Locale.ENGLISH,
        routeId: String = "route-1",
        spots: List<SpotEntry> = listOf(SpotEntry("spot-a", null, 2), SpotEntry("spot-b", null, 3)),
        mood: String? = "calm",
        insights: String = "Learned a lot",
        feedback: String? = "Would return",
        templateStyle: String = "old_street"
    ) = JourneyReflectionInput(
        locale = locale,
        routeId = routeId,
        spotEntries = spots,
        overallMood = mood,
        userInsights = insights,
        postTripFeedback = feedback,
        templateStyle = templateStyle
    )

    @Test
    fun buildsSystemAndUserMessage() {
        val messages = PromptBuilder.build(input())
        assertEquals(2, messages.size)
        assertEquals(ChatRole.System, messages[0].role)
        assertEquals(ChatRole.User, messages[1].role)
    }

    @Test
    fun englishLocaleRequestsEnglishOutput() {
        val system = PromptBuilder.build(input(locale = Locale.ENGLISH))[0].content.orEmpty()
        assertTrue(system.contains("Output language: English"))
    }

    @Test
    fun chineseLocaleRequestsTraditionalChineseOutput() {
        val system = PromptBuilder.build(input(locale = Locale("zh")))[0].content.orEmpty()
        assertTrue(system.contains("Traditional Chinese"))
    }

    @Test
    fun toneHintsMatchTemplateStyle() {
        fun systemFor(style: String) =
            PromptBuilder.build(input(templateStyle = style))[0].content.orEmpty()

        assertTrue(systemFor("old_street").contains("nostalgic"))
        assertTrue(systemFor("city_walk").contains("light and curious"))
        assertTrue(systemFor("taiwan_pop").contains("vibrant and playful"))
        assertTrue(systemFor("heritage_arch").contains("reflective and historically grounded"))
        assertTrue(systemFor("something_else").contains("warm and personal"))
    }

    @Test
    fun userMessageContainsRouteSpotsMoodInsightsFeedback() {
        val user = PromptBuilder.build(input())[1].content.orEmpty()
        assertTrue(user.contains("Route id: route-1"))
        assertTrue(user.contains("Visited 2 stops with 5 photos total."))
        assertTrue(user.contains("spotId=spot-a, photos=2"))
        assertTrue(user.contains("spotId=spot-b, photos=3"))
        assertTrue(user.contains("Overall mood: calm"))
        assertTrue(user.contains("User reflection: Learned a lot"))
        assertTrue(user.contains("Post-trip thoughts: Would return"))
    }

    @Test
    fun blankInsightsRenderNonePlaceholderAndMoodFeedbackOmitted() {
        val user = PromptBuilder.build(
            input(mood = null, insights = "", feedback = null)
        )[1].content.orEmpty()
        assertTrue(user.contains("User reflection: (none)"))
        assertFalse(user.contains("Overall mood:"))
        assertFalse(user.contains("Post-trip thoughts:"))
    }

    @Test
    fun emptySpotEntriesStillIncludesCountLineButNoStopLines() {
        val user = PromptBuilder.build(input(spots = emptyList()))[1].content.orEmpty()
        assertTrue(user.contains("Visited 0 stops with 0 photos total."))
        assertFalse(user.contains("Stop 1:"))
    }
}
