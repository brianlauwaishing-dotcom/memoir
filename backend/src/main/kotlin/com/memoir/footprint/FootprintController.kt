package com.memoir.footprint

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/footprints")
@Tag(name = "footprint", description = "Footprint / UGC / MemoryAnswer — FR-10, FR-26 (scaffold)")
class FootprintController {

    @GetMapping("/ping")
    fun ping(): Map<String, String> = mapOf("module" to "footprint", "status" to "scaffold")
}
