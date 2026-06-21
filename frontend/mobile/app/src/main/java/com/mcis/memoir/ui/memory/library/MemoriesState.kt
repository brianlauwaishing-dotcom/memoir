package com.mcis.memoir.ui.memory.library

data class MemoriesState(
    val selectedTab: MemoriesTab = MemoriesTab.ROUTE,
    val inProgress: List<MemoryCard> = emptyList(),
    val completed: List<MemoryCard> = emptyList(),
    val bookmarkedSpots: List<BookmarkSpotCard> = emptyList(),
    val bookmarkSearchQuery: String = "",
    val isLoading: Boolean = true,
    val activeMenuMemoryId: String? = null,
    val showDeleteDialog: Boolean = false
)

enum class MemoriesTab { ROUTE, BOOKMARK }
