package com.mcis.memoir.ui.memory.photo

import android.content.ContentResolver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mcis.memoir.data.memory.MemoryRepository

class PhotoSelectionViewModelFactory(
    private val memoryId: String,
    private val repo: MemoryRepository,
    private val contentResolver: ContentResolver
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PhotoSelectionViewModel::class.java)) {
            return PhotoSelectionViewModel(memoryId, repo, contentResolver) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
