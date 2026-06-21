package com.mcis.memoir.data.llm

import com.aallam.openai.api.chat.ChatChoice
import com.aallam.openai.api.chat.ChatCompletion
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.exception.AuthenticationException
import com.aallam.openai.api.exception.OpenAIServerException
import com.aallam.openai.api.exception.RateLimitException
import com.aallam.openai.client.OpenAI
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import java.util.Locale
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DeepSeekReflectionClientTest {

    private val openAI = mockk<OpenAI>()
    private val client = DeepSeekReflectionClient(openAI)

    private val input = JourneyReflectionInput(
        locale = Locale.ENGLISH,
        routeId = "r1",
        spotEntries = emptyList(),
        overallMood = null,
        userInsights = "",
        postTripFeedback = null,
        templateStyle = "old_street"
    )

    private fun responseWithContent(text: String): ChatCompletion {
        val completion = mockk<ChatCompletion>()
        val choice = mockk<ChatChoice>()
        every { completion.choices } returns listOf(choice)
        every { choice.message } returns ChatMessage(role = ChatRole.Assistant, content = text)
        return completion
    }

    @Test
    fun successReturnsCaption() = runTest {
        coEvery { openAI.chatCompletion(any()) } returns responseWithContent("Test caption")
        val result = client.generate(input)
        assertEquals(ReflectionResult.Success("Test caption"), result)
    }

    @Test
    fun blankContentMapsToUnexpectedFailure() = runTest {
        coEvery { openAI.chatCompletion(any()) } returns responseWithContent("   ")
        val result = client.generate(input)
        assertInstanceOf(ReflectionResult.Failure::class.java, result)
        assertEquals(ReflectionError.Unexpected, (result as ReflectionResult.Failure).kind)
    }

    @Test
    fun authenticationExceptionMapsToInvalidApiKey() = runTest {
        coEvery { openAI.chatCompletion(any()) } throws mockk<AuthenticationException>(relaxed = true)
        assertFailureKind(ReflectionError.InvalidApiKey)
    }

    @Test
    fun rateLimitExceptionMapsToRateLimited() = runTest {
        coEvery { openAI.chatCompletion(any()) } throws mockk<RateLimitException>(relaxed = true)
        assertFailureKind(ReflectionError.RateLimited)
    }

    @Test
    fun serverExceptionMapsToServiceUnavailable() = runTest {
        coEvery { openAI.chatCompletion(any()) } throws mockk<OpenAIServerException>(relaxed = true)
        assertFailureKind(ReflectionError.ServiceUnavailable)
    }

    @Test
    fun ioExceptionMapsToNetwork() = runTest {
        coEvery { openAI.chatCompletion(any()) } throws IOException("offline")
        assertFailureKind(ReflectionError.Network)
    }

    @Test
    fun otherExceptionMapsToUnexpected() = runTest {
        coEvery { openAI.chatCompletion(any()) } throws RuntimeException("boom")
        assertFailureKind(ReflectionError.Unexpected)
    }

    private suspend fun assertFailureKind(expected: ReflectionError) {
        val result = client.generate(input)
        assertInstanceOf(ReflectionResult.Failure::class.java, result)
        val failure = result as ReflectionResult.Failure
        assertEquals(expected, failure.kind)
        assertTrue(failure.cause != null)
    }
}
