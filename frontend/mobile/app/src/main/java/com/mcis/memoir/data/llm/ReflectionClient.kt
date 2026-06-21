package com.mcis.memoir.data.llm

interface ReflectionClient {
    suspend fun generate(input: JourneyReflectionInput): ReflectionResult
}
