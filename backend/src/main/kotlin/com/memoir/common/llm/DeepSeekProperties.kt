package com.memoir.common.llm

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "llm.deepseek")
data class DeepSeekProperties(
    val baseUrl: String = "https://api.deepseek.com",
    val apiKey: String = "",
    val model: String = "deepseek-chat",
    val timeoutSeconds: Long = 60,
)
