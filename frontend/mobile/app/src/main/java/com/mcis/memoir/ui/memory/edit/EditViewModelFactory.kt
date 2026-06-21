package com.mcis.memoir.ui.memory.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mcis.memoir.data.content.ContentRepository
import com.mcis.memoir.data.memory.MemoryRepository
import java.util.Locale

class EditViewModelFactory(
    private val memoryId: String,
    private val repo: MemoryRepository,
    private val contentRepo: ContentRepository,
    private val localeProvider: () -> Locale
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditViewModel::class.java)) {
            return EditViewModel(memoryId, repo, contentRepo, localeProvider) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
