package com.mcis.memoir.ui.memory.library

sealed interface MemoriesIntent {
    data class TabSelected(val tab: MemoriesTab) : MemoriesIntent
    data class BookmarkSearchChanged(val query: String) : MemoriesIntent
    data class BookmarkSpotClicked(val spotId: String) : MemoriesIntent
    data class MoreClicked(val memoryId: String) : MemoriesIntent
    data object MenuDismissed : MemoriesIntent
    data class ContinueEditingClicked(val memoryId: String) : MemoriesIntent
    data class EditClicked(val memoryId: String) : MemoriesIntent
    data class DeleteClicked(val memoryId: String) : MemoriesIntent
    data object DeleteConfirmed : MemoriesIntent
    data object DeleteCancelled : MemoriesIntent
    data class DuplicateClicked(val memoryId: String) : MemoriesIntent
    data class ShareClicked(val memoryId: String) : MemoriesIntent
    data object CreateMemoryClicked : MemoriesIntent
}
