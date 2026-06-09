package com.mcis.memoir.ui.saved

import android.content.res.Resources
import app.cash.turbine.test
import com.mcis.memoir.data.content.ContentAssetLoader
import com.mcis.memoir.data.content.ContentRepository
import com.mcis.memoir.data.content.ContentSnapshot
import com.mcis.memoir.data.content.model.LocalizedText
import com.mcis.memoir.data.content.model.Route
import com.mcis.memoir.data.prefs.UserPreferencesRepository
import io.mockk.every
import io.mockk.mockk
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SavedViewModelTest {
    private val mainDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun bookmarkedRoutesBecomeCardsInSourceOrder() = runTest(mainDispatcher) {
        val bookmarks = MutableStateFlow(setOf("E", "B"))
        val vm = viewModel(FakePrefs(bookmarks))

        vm.state.test {
            assertTrue(awaitItem().isLoading)
            advanceUntilIdle()

            val loaded = awaitItem()
            assertEquals(listOf("B", "E"), loaded.cards.map { it.id })
            assertEquals(listOf("Route B", "Route E"), loaded.cards.map { it.title })

            bookmarks.value = setOf("A")
            advanceUntilIdle()
            assertEquals(listOf("A"), awaitItem().cards.map { it.id })

            bookmarks.value = emptySet()
            advanceUntilIdle()
            assertTrue(awaitItem().cards.isEmpty())
        }
    }

    private fun TestScope.viewModel(prefs: FakePrefs): SavedViewModel {
        val resources = mockk<Resources>()
        every { resources.getIdentifier(any(), "drawable", "com.mcis.memoir") } returns 7

        return SavedViewModel(
            contentRepo = ContentRepository(FakeContentAssetLoader(snapshot()), this),
            prefsRepo = prefs,
            resources = resources,
            localeProvider = { Locale.ENGLISH }
        )
    }

    private class FakePrefs(
        private val bookmarks: MutableStateFlow<Set<String>>
    ) : UserPreferencesRepository {
        override val language: Flow<String> = MutableStateFlow("en")
        override val selectedInterests: Flow<Set<String>> = MutableStateFlow(emptySet())
        override val onboardingDone: Flow<Boolean> = MutableStateFlow(false)
        override val bookmarkedRouteIds: Flow<Set<String>> = bookmarks

        override suspend fun setLanguage(tag: String) = Unit
        override suspend fun setInterests(set: Set<String>) = Unit
        override suspend fun markOnboardingDone() = Unit
        override suspend fun setBookmarkedRouteIds(set: Set<String>) = Unit
        override suspend fun persistedLanguageTag(): String? = null
    }

    private class FakeContentAssetLoader(
        private val snapshot: ContentSnapshot
    ) : ContentAssetLoader {
        override suspend fun load(): ContentSnapshot = snapshot
    }

    private fun snapshot(): ContentSnapshot = ContentSnapshot(
        routes = linkedMapOf(
            "A" to route("A"),
            "B" to route("B"),
            "C" to route("C"),
            "D" to route("D"),
            "E" to route("E")
        ),
        spots = emptyMap()
    )

    private fun route(id: String): Route = Route(
        id = id,
        title = LocalizedText("Route $id", "路線 $id"),
        category = LocalizedText("Category", "分類"),
        heroImage = "image_$id",
        description = LocalizedText("Description $id", "介紹 $id")
    )
}
