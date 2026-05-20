package com.memoir.route

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/routes")
@Tag(name = "route", description = "Route planning (scaffold)")
class RouteController {

    @GetMapping("/ping")
    fun ping(): Map<String, String> = mapOf("module" to "route", "status" to "scaffold")
}
