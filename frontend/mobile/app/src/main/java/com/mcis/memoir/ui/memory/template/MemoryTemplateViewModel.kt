package com.mcis.memoir.ui.memory.template

import android.content.res.Resources
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mcis.memoir.data.memory.MemoryRepository
import java.util.Locale
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MemoryTemplateViewModel(
    private val repo: MemoryRepository,
    private val resources: Resources,
    private val localeProvider: () -> Locale
) : ViewModel() {
    private val _state = MutableStateFlow(MemoryTemplateState())
    val state: StateFlow<MemoryTemplateState> = _state.asStateFlow()

    private val _effects = Channel<MemoryTemplateEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        localeProvider()
        _state.value = MemoryTemplateState(
            isLoading = false,
            templates = TemplateCatalog.all.map {
                TemplateCard(
                    id = it.id,
                    title = resources.getString(it.titleRes),
                    description = resources.getString(it.descriptionRes),
                    imageRes = it.imageRes,
                    maskRes = it.maskRes
                )
            }
        )
    }

    fun onIntent(intent: MemoryTemplateIntent) {
        when (intent) {
            is MemoryTemplateIntent.TemplateClicked -> viewModelScope.launch {
                val template = TemplateCatalog.byId(intent.templateId)
                    ?: error("Unknown template: ${intent.templateId}")
                val title = resources.getString(template.titleRes)
                runCatching { repo.startDraft(intent.templateId, title) }
                    .onSuccess { memoryId -> _effects.send(MemoryTemplateEffect.NavigateToPhotoSelection(memoryId)) }
                    .onFailure { e -> _state.update { it.copy(error = e.message) } }
            }
        }
    }
}
