package com.mcis.memoir.ui.memory.photo

import android.content.ContentResolver
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

class PhotoSelectionViewModel(
    private val memoryId: String,
    private val repo: MemoryRepository,
    private val contentResolver: ContentResolver
) : ViewModel() {
    private val _state = MutableStateFlow(PhotoSelectionState(memoryId = memoryId))
    val state: StateFlow<PhotoSelectionState> = _state.asStateFlow()

    private val _effects = Channel<PhotoSelectionEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            repo.observe(memoryId)
                .catch { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
                .collect { memory ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            memoryId = memory?.id,
                            photoPaths = memory?.photoRelativePaths.orEmpty(),
                            error = null
                        )
                    }
                }
        }
    }

    fun onIntent(intent: PhotoSelectionIntent) {
        when (intent) {
            PhotoSelectionIntent.AddPhotosClicked -> viewModelScope.launch {
                val remaining = (_state.value.maxPhotos - _state.value.photoPaths.size).coerceAtLeast(0)
                if (remaining > 0) {
                    _effects.send(PhotoSelectionEffect.LaunchPicker(remaining))
                }
            }
            is PhotoSelectionIntent.PhotosPicked -> viewModelScope.launch {
                val remaining = (_state.value.maxPhotos - _state.value.photoPaths.size).coerceAtLeast(0)
                intent.uris.take(remaining).forEach { uri ->
                    repo.addPhoto(memoryId, uri, contentResolver)
                        .onFailure { e -> _state.update { it.copy(error = e.message) } }
                }
            }
            is PhotoSelectionIntent.PhotoRemoved -> viewModelScope.launch {
                repo.removePhoto(memoryId, intent.index)
                    .onFailure { e -> _state.update { it.copy(error = e.message) } }
            }
            PhotoSelectionIntent.NextClicked -> viewModelScope.launch {
                if (_state.value.photoPaths.isNotEmpty()) {
                    _effects.send(PhotoSelectionEffect.NavigateToEdit(memoryId))
                }
            }
        }
    }

    // Note: the in-progress draft is intentionally NOT cancelled when this ViewModel is cleared.
    // Navigation3 clears a step's ViewModel on forward navigation (Photo -> Edit) as well as on
    // back-out, so cancelling here would delete the draft the next wizard step still needs.
    // Drafts persist as resumable IN_PROGRESS memories ("Continue editing") and stale ones are
    // removed by MemoryRepository.sweepOrphans() on app start.
}
