package com.mcis.memoir.data.llm

import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole

object PromptBuilder {
    fun build(input: JourneyReflectionInput): List<ChatMessage> {
        val outputLang =
            if (input.locale.language == "zh") "Traditional Chinese (繁體中文)" else "English"
        val toneHint = when (input.templateStyle) {
            "old_street" -> "nostalgic and warm, like a quiet memory of a small alley"
            "city_walk" -> "light and curious, like a friendly travel diary entry"
            "taiwan_pop" -> "vibrant and playful, full of energy"
            "heritage_arch" -> "reflective and historically grounded"
            else -> "warm and personal"
        }

        val system = ChatMessage(
            role = ChatRole.System,
            content = """
                You are a Instagram influencer and content creator for a Taiwan cultural-travel journaling app.
                The user has finished a tour and wants a 2-4 sentence caption suitable for sharing on Instagram or Threads.
                Output language: $outputLang. Tone: $toneHint.
                Keep it under 200 characters total.
            """.trimIndent()
        )

        val userParts = buildString {
            appendLine("Route id: ${input.routeId}")
            appendLine(
                "Visited ${input.spotEntries.size} stops with " +
                    "${input.spotEntries.sumOf { it.photoCount }} photos total."
            )
            input.spotEntries.forEachIndexed { i, entry ->
                appendLine(
                    "  Stop ${i + 1}: spotId=${entry.spotId}, photos=${entry.photoCount}" +
                        (entry.userNote?.let { ", note=\"$it\"" }.orEmpty())
                )
            }
            if (!input.overallMood.isNullOrBlank()) appendLine("Overall mood: ${input.overallMood}")
            appendLine("User reflection: ${input.userInsights.ifBlank { "(none)" }}")
            if (!input.postTripFeedback.isNullOrBlank()) {
                appendLine("Post-trip thoughts: ${input.postTripFeedback}")
            }
        }

        val user = ChatMessage(role = ChatRole.User, content = userParts)
        return listOf(system, user)
    }
}
