package com.mcis.memoir.ui.memory.reflection

import android.content.res.Resources
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mcis.memoir.R
import com.mcis.memoir.data.content.ContentRepository
import com.mcis.memoir.data.llm.JourneyInputAssembler
import com.mcis.memoir.data.llm.ReflectionClient
import com.mcis.memoir.data.llm.ReflectionError
import com.mcis.memoir.data.llm.ReflectionResult
import com.mcis.memoir.data.memory.Memory
import com.mcis.memoir.data.memory.MemoryRepository
import java.util.Locale
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ReflectionViewModel(
    private val memoryId: String,
    private val repo: MemoryRepository,
    private val reflectionClient: ReflectionClient,
    private val contentRepo: ContentRepository,
    private val resources: Resources,
    private val localeProvider: () -> Locale
) : ViewModel() {
    private val _state = MutableStateFlow(ReflectionState())
    val state: StateFlow<ReflectionState> = _state.asStateFlow()

    private val _effects = Channel<ReflectionEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private val generateMutex = Mutex()

    @Volatile
    private var currentMemory: Memory? = null

    init {
        viewModelScope.launch {
            repo.observe(memoryId)
                .catch { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
                .collect { memory ->
                    currentMemory = memory
                    if (memory != null) {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                overallMood = memory.overallMood.orEmpty(),
                                userInsights = memory.userInsights,
                                postTripFeedback = memory.postTripFeedback.orEmpty(),
                                error = null
                            )
                        }
                    } else {
                        _state.update { it.copy(isLoading = false) }
                    }
                }
        }
    }

    fun onIntent(intent: ReflectionIntent) {
        when (intent) {
            is ReflectionIntent.MoodChanged -> _state.update { it.copy(overallMood = intent.text) }
            is ReflectionIntent.InsightsChanged -> _state.update { it.copy(userInsights = intent.text) }
            is ReflectionIntent.FeedbackChanged -> _state.update { it.copy(postTripFeedback = intent.text) }
            ReflectionIntent.SaveClicked -> save()
            ReflectionIntent.PolishClicked -> generate()
            ReflectionIntent.RegenerateClicked -> generate()
            ReflectionIntent.CopyClicked -> {
                val ai = _state.value.aiState
                if (ai is AiState.Ready) {
                    viewModelScope.launch { _effects.send(ReflectionEffect.CopyToClipboard(ai.text)) }
                }
            }
            ReflectionIntent.DismissAiError -> {
                if (_state.value.aiState is AiState.Error) {
                    _state.update { it.copy(aiState = AiState.Idle) }
                }
            }
        }
    }

    private fun generate() {
        // Drop the request entirely if a generation is already in flight (cheap guard
        // before launching any coroutine). The Mutex below only serializes — without
        // this guard a queued tap would still fire a fresh network call.
        if (_state.value.aiState is AiState.Generating) return
        viewModelScope.launch {
            generateMutex.withLock {
                // Re-check inside the lock to catch rare interleavings.
                if (_state.value.aiState is AiState.Generating) return@withLock
                _state.update { it.copy(aiState = AiState.Generating) }
                val memory = currentMemory ?: run {
                    _state.update {
                        it.copy(
                            aiState = AiState.Error(
                                ReflectionError.Unexpected,
                                resources.getString(R.string.error_unexpected)
                            )
                        )
                    }
                    return@withLock
                }
                val input = JourneyInputAssembler.build(memory, localeProvider(), contentRepo)
                when (val result = reflectionClient.generate(input)) {
                    is ReflectionResult.Success ->
                        _state.update { it.copy(aiState = AiState.Ready(result.text)) }
                    is ReflectionResult.Failure ->
                        _state.update {
                            it.copy(
                                aiState = AiState.Error(
                                    result.kind,
                                    resources.getString(result.kind.messageRes())
                                )
                            )
                        }
                }
            }
        }
    }

    private fun save() {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            val current = _state.value
            val aiText = (current.aiState as? AiState.Ready)?.text
            runCatching {
                repo.updateReflection(
                    memoryId = memoryId,
                    mood = current.overallMood.ifBlank { null },
                    insights = current.userInsights,
                    feedback = current.postTripFeedback.ifBlank { null }
                )
                if (aiText != null) repo.updateGeneratedReflection(memoryId, aiText)
                repo.complete(memoryId)
            }.fold(
                onSuccess = {
                    _effects.send(ReflectionEffect.NavigateToMemoriesList)
                },
                onFailure = { e ->
                    val message = e.message ?: "Could not save reflection"
                    _state.update { it.copy(isSaving = false, error = message) }
                    _effects.send(ReflectionEffect.ShowError(message))
                }
            )
        }
    }

    private fun ReflectionError.messageRes(): Int = when (this) {
        ReflectionError.Network -> R.string.error_network
        ReflectionError.InvalidApiKey -> R.string.error_invalid_api_key
        ReflectionError.RateLimited -> R.string.error_rate_limited
        ReflectionError.ServiceUnavailable -> R.string.error_service_unavailable
        ReflectionError.Unexpected -> R.string.error_unexpected
    }
}
