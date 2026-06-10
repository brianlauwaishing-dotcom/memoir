package com.mcis.memoir.ui.memory.photo

import android.net.Uri

sealed interface PhotoSelectionIntent {
    data object AddPhotosClicked : PhotoSelectionIntent
    data class PhotosPicked(val uris: List<Uri>) : PhotoSelectionIntent
    data class PhotoRemoved(val index: Int) : PhotoSelectionIntent
    data object NextClicked : PhotoSelectionIntent
}
