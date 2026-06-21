package com.mcis.memoir.ui.memory.library

import android.content.res.Resources
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mcis.memoir.data.content.ContentRepository
import com.mcis.memoir.data.content.model.Spot
import com.mcis.memoir.data.memory.Memory
import com.mcis.memoir.data.memory.MemoryRepository
import com.mcis.memoir.data.memory.MemoryStatus
import com.mcis.memoir.data.prefs.UserPreferencesRepository
import com.mcis.memoir.ui.memory.template.TemplateCatalog
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MemoriesViewModel(
    private val repo: MemoryRepository,
    private val contentRepo: ContentRepository,
    private val prefsRepo: UserPreferencesRepository,
    private val resources: Resources,
    private val localeProvider: () -> Locale
) : ViewModel() {

    private data class UiState(
        val selectedTab: MemoriesTab = MemoriesTab.ROUTE,
        val bookmarkSearchQuery: String = "",
        val activeMenuMemoryId: String? = null,
        val showDeleteDialog: Boolean = false
    )

    private val _ui = MutableStateFlow(UiState())

    private val _effects = Channel<MemoriesEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    // Latest memory rows keyed by id, kept so Share can read photo paths without a second repo read.
    @Volatile
    private var latestMemories: Map<String, Memory> = emptyMap()
    private var latestBookmarkedSpots: Map<String, BookmarkSpotCard> = emptyMap()

    val state: StateFlow<MemoriesState> = combine(
        repo.observeByStatus(MemoryStatus.IN_PROGRESS),
        repo.observeByStatus(MemoryStatus.COMPLETED),
        contentRepo.spots(),
        prefsRepo.bookmarkedSpotIds,
        _ui
    ) { inProgress, completed, spots, bookmarkedIds, ui ->
        latestMemories = (inProgress + completed).associateBy { it.id }
        val locale = localeProvider()
        val bookmarkedSpots = spots.toBookmarkCards(bookmarkedIds, locale, ui.bookmarkSearchQuery)
        latestBookmarkedSpots = bookmarkedSpots.associateBy { it.id }
        MemoriesState(
            selectedTab = ui.selectedTab,
            inProgress = inProgress.filter { it.spotId == null }.map { it.toCard(locale) },
            completed = completed.filter { it.spotId == null }.map { it.toCard(locale) },
            bookmarkedSpots = bookmarkedSpots,
            bookmarkSearchQuery = ui.bookmarkSearchQuery,
            isLoading = false,
            activeMenuMemoryId = ui.activeMenuMemoryId,
            showDeleteDialog = ui.showDeleteDialog
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MemoriesState()
    )

    fun onIntent(intent: MemoriesIntent) {
        when (intent) {
            is MemoriesIntent.TabSelected ->
                _ui.update { it.copy(selectedTab = intent.tab) }
            is MemoriesIntent.BookmarkSearchChanged ->
                _ui.update { it.copy(bookmarkSearchQuery = intent.query) }
            is MemoriesIntent.BookmarkSpotClicked -> viewModelScope.launch {
                val title = latestBookmarkedSpots[intent.spotId]?.title ?: intent.spotId
                val memoryId = repo.getOrCreateSpotDraft(
                    spotId = intent.spotId,
                    templateId = TemplateCatalog.all.first().id,
                    defaultTitle = title
                )
                _effects.send(MemoriesEffect.NavigateToWizard(memoryId, WizardEntry.EDIT))
            }
            is MemoriesIntent.MoreClicked ->
                _ui.update { it.copy(activeMenuMemoryId = intent.memoryId) }
            MemoriesIntent.MenuDismissed ->
                _ui.update { it.copy(activeMenuMemoryId = null) }
            is MemoriesIntent.ContinueEditingClicked ->
                emit(MemoriesEffect.NavigateToWizard(intent.memoryId, WizardEntry.PHOTO_SELECTION))
            is MemoriesIntent.EditClicked -> {
                val entry = if (latestMemories[intent.memoryId]?.status == MemoryStatus.COMPLETED) {
                    WizardEntry.EDIT
                } else {
                    WizardEntry.PHOTO_SELECTION
                }
                _ui.update { it.copy(activeMenuMemoryId = null) }
                emit(MemoriesEffect.NavigateToWizard(intent.memoryId, entry))
            }
            is MemoriesIntent.DeleteClicked ->
                _ui.update { it.copy(activeMenuMemoryId = intent.memoryId, showDeleteDialog = true) }
            MemoriesIntent.DeleteConfirmed -> {
                val target = _ui.value.activeMenuMemoryId
                if (target != null) {
                    viewModelScope.launch { repo.deleteMemory(target) }
                }
                _ui.update { it.copy(activeMenuMemoryId = null, showDeleteDialog = false) }
            }
            MemoriesIntent.DeleteCancelled ->
                _ui.update { it.copy(activeMenuMemoryId = null, showDeleteDialog = false) }
            is MemoriesIntent.DuplicateClicked -> {
                viewModelScope.launch { repo.duplicateMemory(intent.memoryId) }
                _ui.update { it.copy(activeMenuMemoryId = null) }
            }
            is MemoriesIntent.ShareClicked -> {
                val memory = latestMemories[intent.memoryId]
                _ui.update { it.copy(activeMenuMemoryId = null) }
                if (memory != null && memory.photoRelativePaths.isNotEmpty()) {
                    emit(MemoriesEffect.ShareMemory(memory.photoRelativePaths, memory.title))
                }
            }
            MemoriesIntent.CreateMemoryClicked -> emit(MemoriesEffect.NavigateToCreate)
        }
    }

    private fun emit(effect: MemoriesEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }

    private fun Memory.toCard(locale: Locale): MemoryCard = MemoryCard(
        id = id,
        title = title,
        coverRelativePath = photoRelativePaths.firstOrNull(),
        status = status,
        dateLabel = DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(Date(updatedAt)),
        draftProgress = draftProgress()
    )

    private fun Memory.draftProgress(): DraftProgress {
        var current = 1 // draft exists
        if (photoRelativePaths.isNotEmpty()) current++
        if (userInsights.isNotBlank()) current++
        return DraftProgress(current = current, total = 3)
    }

    private fun List<Spot>.toBookmarkCards(
        bookmarkedIds: Set<String>,
        locale: Locale,
        query: String
    ): List<BookmarkSpotCard> =
        filter { it.id in bookmarkedIds }
            .map { spot ->
                BookmarkSpotCard(
                    id = spot.id,
                    title = spot.title[locale],
                    heroDrawableRes = resources.getIdentifier(spot.heroImage, "drawable", "com.mcis.memoir")
                )
            }
            .filter { query.isBlank() || it.title.contains(query, ignoreCase = true) }
}
