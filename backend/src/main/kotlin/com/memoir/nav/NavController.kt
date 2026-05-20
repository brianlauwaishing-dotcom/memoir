package com.memoir.nav

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/nav")
@Tag(name = "nav", description = "Navigation (scaffold)")
class NavController {

    @GetMapping("/ping")
    fun ping(): Map<String, String> = mapOf("module" to "nav", "status" to "scaffold")
}
