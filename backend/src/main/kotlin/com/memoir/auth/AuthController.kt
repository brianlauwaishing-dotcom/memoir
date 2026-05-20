package com.memoir.auth

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
@Tag(name = "auth", description = "Authentication & user identity (scaffold)")
class AuthController {

    @GetMapping("/ping")
    fun ping(): Map<String, String> = mapOf("module" to "auth", "status" to "scaffold")
}
