package com.mcis.memoir.ui.memory.library

import android.content.res.Resources
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mcis.memoir.data.content.ContentRepository
import com.mcis.memoir.data.memory.MemoryRepository
import com.mcis.memoir.data.prefs.UserPreferencesRepository
import java.util.Locale

class MemoriesViewModelFactory(
    private val repo: MemoryRepository,
    private val contentRepo: ContentRepository,
    private val prefsRepo: UserPreferencesRepository,
    private val resources: Resources,
    private val localeProvider: () -> Locale
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MemoriesViewModel::class.java)) {
            return MemoriesViewModel(repo, contentRepo, prefsRepo, resources, localeProvider) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
