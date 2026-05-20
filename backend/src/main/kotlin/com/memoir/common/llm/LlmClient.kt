package com.memoir.common.llm

interface LlmClient {

    fun complete(prompt: String, system: String? = null): String
}
