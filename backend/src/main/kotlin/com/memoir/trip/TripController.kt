package com.memoir.trip

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/trips")
@Tag(name = "trip", description = "Trip & itinerary (scaffold)")
class TripController {

    @GetMapping("/ping")
    fun ping(): Map<String, String> = mapOf("module" to "trip", "status" to "scaffold")
}
