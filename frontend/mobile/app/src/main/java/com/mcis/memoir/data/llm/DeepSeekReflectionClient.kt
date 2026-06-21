package com.mcis.memoir.data.llm

import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.exception.AuthenticationException
import com.aallam.openai.api.exception.OpenAIAPIException
import com.aallam.openai.api.exception.OpenAIHttpException
import com.aallam.openai.api.exception.OpenAIServerException
import com.aallam.openai.api.exception.OpenAITimeoutException
import com.aallam.openai.api.exception.RateLimitException
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import java.io.IOException

class DeepSeekReflectionClient(private val openAI: OpenAI) : ReflectionClient {
    override suspend fun generate(input: JourneyReflectionInput): ReflectionResult {
        val messages = PromptBuilder.build(input)
        return runCatching {
            val req = ChatCompletionRequest(
                model = ModelId("deepseek-chat"),
                messages = messages,
                temperature = 0.8,
                maxTokens = 500
            )
            openAI.chatCompletion(req).choices.first().message.content.orEmpty()
        }.fold(
            onSuccess = { text ->
                if (text.isBlank()) ReflectionResult.Failure(ReflectionError.Unexpected)
                else ReflectionResult.Success(text)
            },
            onFailure = { e -> ReflectionResult.Failure(mapError(e), e) }
        )
    }

    private fun mapError(e: Throwable): ReflectionError = when (e) {
        is AuthenticationException -> ReflectionError.InvalidApiKey       // SDK's 401 mapping
        is RateLimitException -> ReflectionError.RateLimited              // SDK's 429 mapping
        is OpenAIServerException -> ReflectionError.ServiceUnavailable    // SDK's 5xx mapping
        is OpenAITimeoutException -> ReflectionError.Network              // SDK's timeout mapping
        is OpenAIAPIException -> ReflectionError.ServiceUnavailable       // other API errors (4xx not 401/429)
        is OpenAIHttpException -> ReflectionError.Network                 // non-API HTTP transport errors
        is IOException -> ReflectionError.Network                         // raw Ktor IO failures that escape the SDK
        else -> ReflectionError.Unexpected
    }
}
