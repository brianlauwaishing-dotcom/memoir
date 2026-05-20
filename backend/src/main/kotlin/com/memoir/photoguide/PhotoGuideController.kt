package com.memoir.photoguide

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/photo-guides")
@Tag(name = "photoguide", description = "Photo composition guidance — FR-24 (scaffold)")
class PhotoGuideController {

    @GetMapping("/ping")
    fun ping(): Map<String, String> = mapOf("module" to "photoguide", "status" to "scaffold")
}
