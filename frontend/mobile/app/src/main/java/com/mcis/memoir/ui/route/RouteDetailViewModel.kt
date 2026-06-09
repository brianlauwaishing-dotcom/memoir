package com.mcis.memoir.ui.route

import android.content.res.Resources
import androidx.lifecycle.ViewModel
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RouteDetailViewModel(
    private val routeId: String,
    private val contentRepo: ContentRepository,
    private val prefsRepo: UserPreferencesRepository,
    private val resources: Resources,
    private val localeProvider: () -> Locale
) : ViewModel() {
    private val _state = MutableStateFlow(RouteDetailState(isLoading = true))
    val state: StateFlow<RouteDetailState> = _state.asStateFlow()

    private val _effects = Channel<RouteDetailEffect>(Channel.BUFFERED)
    val effects: Flow<RouteDetailEffect> = _effects.receiveAsFlow()

    private val toggleMutex = Mutex()

    init {
        viewModelScope.launch {
            combine(
                flow { emit(contentRepo.route(routeId)) },
                prefsRepo.bookmarkedRouteIds
            ) { route, bookmarks ->
                buildState(route, bookmarks)
            }
                .catch { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { newState -> _state.value = newState }
        }
    }

    fun onIntent(intent: RouteDetailIntent) {
        when (intent) {
            RouteDetailIntent.BookmarkToggled -> viewModelScope.launch {
                toggleMutex.withLock {
                    val current = prefsRepo.bookmarkedRouteIds.first()
                    val next = if (routeId in current) current - routeId else current + routeId
                    prefsRepo.setBookmarkedRouteIds(next)
                }
            }
            is RouteDetailIntent.SpotClicked -> Unit
        }
    }

    private suspend fun buildState(route: Route?, bookmarks: Set<String>): RouteDetailState {
        if (route == null) {
            return RouteDetailState(isLoading = false, error = "route_not_found")
        }

        val locale = localeProvider()
        return RouteDetailState(
            isLoading = false,
            routeId = route.id,
            title = route.title[locale],
            description = route.description[locale],
            heroDrawableRes = resources.getIdentifier(route.heroImage, "drawable", "com.mcis.memoir"),
            journey = route.journey
                .sortedBy { it.order }
                .map { stop ->
                    JourneyRowState(
                        order = stop.order,
                        spotId = stop.spotId,
                        label = resolveSpotLabel(stop.spotId, locale)
                    )
                },
            isSaved = routeId in bookmarks,
            error = null
        )
    }

    private suspend fun resolveSpotLabel(spotId: String, locale: Locale): String =
        contentRepo.spot(spotId)?.title?.get(locale) ?: spotId
}
