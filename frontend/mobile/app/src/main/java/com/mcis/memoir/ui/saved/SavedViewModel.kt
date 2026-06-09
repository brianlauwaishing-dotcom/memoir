package com.mcis.memoir.ui.saved

import android.content.res.Resources
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mcis.memoir.data.content.ContentRepository
import com.mcis.memoir.data.prefs.UserPreferencesRepository
import com.mcis.memoir.ui.home.toCard
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SavedViewModel(
    private val contentRepo: ContentRepository,
    private val prefsRepo: UserPreferencesRepository,
    private val resources: Resources,
    private val localeProvider: () -> Locale
) : ViewModel() {
    private val _state = MutableStateFlow(SavedState(isLoading = true))
    val state: StateFlow<SavedState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                contentRepo.routes(),
                prefsRepo.bookmarkedRouteIds
            ) { routes, bookmarks ->
                SavedState(
                    isLoading = false,
                    cards = routes
                        .filter { it.id in bookmarks }
                        .map { it.toCard(localeProvider(), resources) },
                    error = null
                )
            }
                .catch { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { newState -> _state.value = newState }
        }
    }
}
