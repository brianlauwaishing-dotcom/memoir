package com.memoir.common.llm

import com.memoir.common.web.ApiException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.body

@Component
class DeepSeekClient(
    private val deepSeekRestClient: RestClient,
    private val props: DeepSeekProperties,
) : LlmClient {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun complete(prompt: String, system: String?): String {
        require(props.apiKey.isNotBlank()) { "DEEPSEEK_API_KEY is not set" }

        val messages = buildList {
            if (!system.isNullOrBlank()) add(Message("system", system))
            add(Message("user", prompt))
        }
        val req = ChatRequest(model = props.model, messages = messages)

        val resp = try {
            deepSeekRestClient.post()
                .uri("/v1/chat/completions")
                .body(req)
                .retrieve()
                .body<ChatResponse>()
        } catch (e: RestClientResponseException) {
            log.warn("DeepSeek call failed: status={} body={}", e.statusCode, e.responseBodyAsString)
            throw ApiException(
                HttpStatus.BAD_GATEWAY,
                "LLM_UPSTREAM_ERROR",
                "DeepSeek call failed: ${e.statusCode}",
                e,
            )
        } ?: throw ApiException(HttpStatus.BAD_GATEWAY, "LLM_EMPTY_RESPONSE", "DeepSeek returned empty body")

        return resp.choices.firstOrNull()?.message?.content
            ?: throw ApiException(HttpStatus.BAD_GATEWAY, "LLM_NO_CHOICES", "DeepSeek returned no choices")
    }

    private data class ChatRequest(
        val model: String,
        val messages: List<Message>,
        val stream: Boolean = false,
    )

    private data class Message(val role: String, val content: String)

    private data class ChatResponse(val choices: List<Choice>) {
        data class Choice(val message: Message)
    }
}
