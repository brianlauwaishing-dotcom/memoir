package com.memoir.common.llm

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.time.Duration

@Configuration
@EnableConfigurationProperties(DeepSeekProperties::class)
class LlmConfig {

    @Bean
    fun deepSeekRestClient(props: DeepSeekProperties): RestClient {
        val factory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(Duration.ofSeconds(10))
            setReadTimeout(Duration.ofSeconds(props.timeoutSeconds))
        }
        return RestClient.builder()
            .baseUrl(props.baseUrl)
            .defaultHeader("Authorization", "Bearer ${props.apiKey}")
            .defaultHeader("Content-Type", "application/json")
            .requestFactory(factory)
            .build()
    }
}
