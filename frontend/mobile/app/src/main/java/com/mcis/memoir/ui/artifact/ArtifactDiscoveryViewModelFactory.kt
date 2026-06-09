package com.mcis.memoir.ui.artifact

import android.content.res.Resources
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mcis.memoir.data.content.ContentRepository
import java.util.Locale

class ArtifactDiscoveryViewModelFactory(
    private val spotId: String,
    private val artifactId: Int,
    private val content: ContentRepository,
    private val resources: Resources,
    private val localeProvider: () -> Locale
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ArtifactDiscoveryViewModel::class.java)) {
            return ArtifactDiscoveryViewModel(spotId, artifactId, content, resources, localeProvider) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
