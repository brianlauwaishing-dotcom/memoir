package com.mcis.memoir.ui.home

import android.content.res.Resources
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mcis.memoir.data.content.ContentRepository
import com.mcis.memoir.data.content.model.Route
import com.mcis.memoir.data.prefs.UserPreferencesRepository
import java.util.Locale
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val contentRepo: ContentRepository,
    private val prefsRepo: UserPreferencesRepository,
    private val resources: Resources,
    private val localeProvider: () -> Locale
) : ViewModel() {
    private val _state = MutableStateFlow(HomeState(isLoading = true))
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val _effects = Channel<HomeEffect>(Channel.BUFFERED)
    val effects: Flow<HomeEffect> = _effects.receiveAsFlow()

    private val queryFlow = MutableStateFlow("")
    private val activeTagsFlow = MutableStateFlow<Set<String>>(emptySet())

    init {
        viewModelScope.launch {
            combine(
                contentRepo.routes(),
                prefsRepo.selectedInterests,
                queryFlow,
                activeTagsFlow
            ) { routes, interests, query, activeTags ->
                buildHomeState(routes, interests, query, activeTags, localeProvider(), resources)
            }
                .catch { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { newState -> _state.value = newState }
        }
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.SearchChanged -> queryFlow.value = intent.q
            is HomeIntent.FilterTagToggled -> activeTagsFlow.update { it.toggled(intent.tagId) }
            is HomeIntent.CardClicked -> viewModelScope.launch {
                _effects.send(HomeEffect.NavigateToRoute(intent.routeId))
            }
        }
    }

    private fun Set<String>.toggled(tagId: String): Set<String> {
        if (tagId == "all") return emptySet()
        val current = this - "all"
        return if (tagId in current) current - tagId else current + tagId
    }
}

internal fun buildHomeState(
    routes: List<Route>,
    interests: Set<String>,
    query: String,
    activeTags: Set<String>,
    locale: Locale,
    resources: Resources
): HomeState {
    val cards = routes
        .filter { route -> matchesTags(route, activeTags) && matchesSearch(route, query, locale) }
        .sortedWith(rankComparator(interests))
        .map { route -> route.toCard(locale, resources) }

    return HomeState(
        cards = cards,
        isLoading = false,
        query = query,
        activeTags = activeTags,
        userInterests = interests,
        error = null
    )
}

internal fun matchesTags(route: Route, activeTags: Set<String>): Boolean {
    if (activeTags.isEmpty() || "all" in activeTags) return true
    return route.tags.any { it in activeTags }
}

internal fun matchesSearch(route: Route, query: String, locale: Locale): Boolean {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return true
    return route.title[locale].contains(trimmed, ignoreCase = true)
}

internal fun rankComparator(interests: Set<String>): Comparator<Route> {
    if (interests.isEmpty()) return Comparator { _, _ -> 0 }
    return compareByDescending { route: Route -> route.tags.count { it in interests } }
}

class HomeViewModelFactory(
    private val content: ContentRepository,
    private val prefs: UserPreferencesRepository,
    private val resources: Resources,
    private val localeProvider: () -> Locale
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(content, prefs, resources, localeProvider) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
