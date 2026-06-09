package com.mcis.memoir.ui.route

import android.content.res.Resources
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mcis.memoir.data.content.ContentRepository
import com.mcis.memoir.data.prefs.UserPreferencesRepository
import java.util.Locale

class RouteDetailViewModelFactory(
    private val routeId: String,
    private val content: ContentRepository,
    private val prefs: UserPreferencesRepository,
    private val resources: Resources,
    private val localeProvider: () -> Locale
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RouteDetailViewModel::class.java)) {
            return RouteDetailViewModel(routeId, content, prefs, resources, localeProvider) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
