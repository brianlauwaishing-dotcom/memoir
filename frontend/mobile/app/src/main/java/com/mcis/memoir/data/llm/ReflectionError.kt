package com.mcis.memoir.data.llm

sealed interface ReflectionError {
    data object Network : ReflectionError
    data object InvalidApiKey : ReflectionError
    data object RateLimited : ReflectionError
    data object ServiceUnavailable : ReflectionError
    data object Unexpected : ReflectionError
}
