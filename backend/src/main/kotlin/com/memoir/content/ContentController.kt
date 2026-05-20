package com.memoir.content

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/content")
@Tag(name = "content", description = "Spots / stories / contexts / missions (scaffold)")
class ContentController {

    @GetMapping("/ping")
    fun ping(): Map<String, String> = mapOf("module" to "content", "status" to "scaffold")
}
