package com.memoir.mission

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/missions")
@Tag(name = "mission", description = "Heritage mission trigger & progress — FR-08, FR-25 (scaffold)")
class MissionController {

    @GetMapping("/ping")
    fun ping(): Map<String, String> = mapOf("module" to "mission", "status" to "scaffold")
}
