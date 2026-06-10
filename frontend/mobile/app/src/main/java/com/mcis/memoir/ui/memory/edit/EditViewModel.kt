package com.mcis.memoir.ui.memory.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mcis.memoir.data.memory.MemoryRepository
import com.mcis.memoir.ui.memory.template.TemplateCatalog
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditViewModel(
    private val memoryId: String,
    private val repo: MemoryRepository
) : ViewModel() {
    private val _state = MutableStateFlow(EditState(memoryId = memoryId))
    val state: StateFlow<EditState> = _state.asStateFlow()

    private val _effects = Channel<EditEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            repo.observe(memoryId)
                .catch { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
                .collect { memory ->
                    val template = memory?.let { TemplateCatalog.byId(it.templateId) }
                    _state.update {
                        it.copy(
                            isLoading = false,
                            memoryId = memory?.id,
                            templateId = memory?.templateId,
                            templateImageRes = template?.imageRes ?: 0,
                            templateMaskRes = template?.maskRes ?: 0,
                            templateSlots = template?.slots.orEmpty(),
                            photoPaths = memory?.photoRelativePaths.orEmpty(),
                            error = null
                        )
                    }
                }
        }
    }

    fun onIntent(intent: EditIntent) {
        when (intent) {
            EditIntent.SaveClicked -> viewModelScope.launch {
                _effects.send(EditEffect.NavigateToReflection(memoryId))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        repo.fireCancelDraftIfInProgress(memoryId)
    }
}
