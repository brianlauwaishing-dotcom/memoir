package com.mcis.memoir.ui.memory.reflection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mcis.memoir.data.memory.MemoryRepository

class ReflectionViewModelFactory(
    private val memoryId: String,
    private val repo: MemoryRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReflectionViewModel::class.java)) {
            return ReflectionViewModel(memoryId, repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
