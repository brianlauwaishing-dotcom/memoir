package com.memoir.common.llm

import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/llm")
@Tag(name = "llm", description = "LLM completion gateway (DeepSeek)")
class LlmController(
    private val llm: LlmClient,
) {

    @PostMapping("/complete")
    fun complete(@Valid @RequestBody req: CompleteRequest): CompleteResponse =
        CompleteResponse(llm.complete(req.prompt, req.system))

    data class CompleteRequest(
        @field:NotBlank val prompt: String,
        val system: String? = null,
    )

    data class CompleteResponse(val text: String)
}
