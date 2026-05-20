package com.memoir.common.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun memoirOpenApi(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("Memoir Backend API")
                .description("Memoir — Heritage Exploration Mission platform")
                .version("0.0.1-SNAPSHOT"),
        )
}
