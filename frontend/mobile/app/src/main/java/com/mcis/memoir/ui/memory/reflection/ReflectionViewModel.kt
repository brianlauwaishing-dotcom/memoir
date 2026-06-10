package com.mcis.memoir.ui.memory.reflection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mcis.memoir.data.memory.MemoryRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReflectionViewModel(
    private val memoryId: String,
    private val repo: MemoryRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ReflectionState())
    val state: StateFlow<ReflectionState> = _state.asStateFlow()

    private val _effects = Channel<ReflectionEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var completionConfirmed = false

    init {
        viewModelScope.launch {
            repo.observe(memoryId)
                .catch { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
                .collect { memory ->
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
        }
    }

    private fun save() {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            val current = _state.value
            runCatching {
                repo.updateReflection(
                    memoryId = memoryId,
                    mood = current.overallMood.ifBlank { null },
                    insights = current.userInsights,
                    feedback = current.postTripFeedback.ifBlank { null }
                )
                repo.complete(memoryId)
            }.fold(
                onSuccess = {
                    completionConfirmed = true
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

    override fun onCleared() {
        super.onCleared()
        if (!completionConfirmed) {
            repo.fireCancelDraftIfInProgress(memoryId)
        }
    }
}
