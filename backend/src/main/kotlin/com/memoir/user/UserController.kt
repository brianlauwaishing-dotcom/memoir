package com.memoir.user

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users")
@Tag(name = "user", description = "User profile & preferences (scaffold)")
class UserController {

    @GetMapping("/ping")
    fun ping(): Map<String, String> = mapOf("module" to "user", "status" to "scaffold")
}
