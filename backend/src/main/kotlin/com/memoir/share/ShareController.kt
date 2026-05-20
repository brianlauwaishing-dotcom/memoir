package com.memoir.share

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/shares")
@Tag(name = "share", description = "Share & curated layouts — FR-14, FR-27 (scaffold)")
class ShareController {

    @GetMapping("/ping")
    fun ping(): Map<String, String> = mapOf("module" to "share", "status" to "scaffold")
}
