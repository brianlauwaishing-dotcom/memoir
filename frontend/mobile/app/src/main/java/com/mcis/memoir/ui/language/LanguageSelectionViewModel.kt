package com.mcis.memoir.ui.language

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mcis.memoir.data.prefs.UserPreferencesRepository
import com.mcis.memoir.i18n.LocaleController
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LanguageState(
    val selected: String? = null,
    val applying: Boolean = false
)

sealed interface LanguageIntent {
    data class Select(val tag: String) : LanguageIntent
    data object Confirm : LanguageIntent
}

sealed interface LanguageEffect {
    data object NavigateNext : LanguageEffect
    data class ShowError(val msg: String) : LanguageEffect
}

class LanguageSelectionViewModel(
    private val prefs: UserPreferencesRepository
) : ViewModel() {
    private val _state = MutableStateFlow(LanguageState())
    val state = _state.asStateFlow()

    private val _effects = Channel<LanguageEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            _state.update { it.copy(selected = prefs.language.first()) }
        }
    }

    fun onIntent(intent: LanguageIntent) {
        when (intent) {
            is LanguageIntent.Select -> {
                _state.update { it.copy(selected = intent.tag) }
            }
            LanguageIntent.Confirm -> {
                val snapshot = _state.value
                if (snapshot.applying || snapshot.selected == null) return

                _state.update { it.copy(applying = true) }
                viewModelScope.launch {
                    runCatching {
                        LocaleController.setLocale(snapshot.selected, prefs)
                    }.onSuccess {
                        _effects.send(LanguageEffect.NavigateNext)
                    }.onFailure { error ->
                        _effects.send(LanguageEffect.ShowError(error.message ?: "failed"))
                    }
                    _state.update { it.copy(applying = false) }
                }
            }
        }
    }
}
