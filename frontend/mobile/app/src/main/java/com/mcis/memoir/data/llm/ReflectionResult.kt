package com.mcis.memoir.data.llm

sealed interface ReflectionResult {
    data class Success(val text: String) : ReflectionResult
    data class Failure(val kind: ReflectionError, val cause: Throwable? = null) : ReflectionResult
}
