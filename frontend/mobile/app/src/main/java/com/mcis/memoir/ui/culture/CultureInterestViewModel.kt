package com.mcis.memoir.ui.culture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mcis.memoir.data.prefs.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CultureInterestViewModel(
    private val prefs: UserPreferencesRepository
) : ViewModel() {
    val selected: StateFlow<Set<String>> = prefs.selectedInterests
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun toggle(tagId: String) {
        viewModelScope.launch {
            val current = prefs.selectedInterests.first()
            val next = if (tagId in current) current - tagId else current + tagId
            prefs.setInterests(next)
        }
    }

    fun skip() {
        viewModelScope.launch {
            prefs.setInterests(emptySet())
        }
    }
}

class CultureInterestViewModelFactory(
    private val prefs: UserPreferencesRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CultureInterestViewModel::class.java)) {
            return CultureInterestViewModel(prefs) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
