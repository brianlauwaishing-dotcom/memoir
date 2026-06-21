package com.mcis.memoir.ui.memory.reflection

import com.mcis.memoir.data.llm.ReflectionError

sealed interface AiState {
    data object Idle : AiState
    data object Generating : AiState
    data class Ready(val text: String) : AiState
    data class Error(val kind: ReflectionError, val message: String) : AiState
}
