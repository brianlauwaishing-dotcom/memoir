package com.mcis.memoir.ui.home

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.StandardTestDispatcher
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
class HomeViewModelTest {
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
    fun stateCombinesContentSearchTagsAndInterests() = runTest(mainDispatcher) {
        val interests = MutableStateFlow(emptySet<String>())
        val vm = viewModel(interests)

        vm.state.test {
            assertTrue(awaitItem().isLoading)
            advanceUntilIdle()
            assertEquals(listOf("alpha", "beta", "gamma"), awaitItem().cards.map { it.id })

            vm.onIntent(HomeIntent.SearchChanged("temple"))
            advanceUntilIdle()
            assertEquals(listOf("beta", "gamma"), awaitItem().cards.map { it.id })

            vm.onIntent(HomeIntent.FilterTagToggled("temples"))
            advanceUntilIdle()
            val tagged = awaitItem()
            assertEquals(setOf("temples"), tagged.activeTags)
            assertEquals(listOf("beta", "gamma"), tagged.cards.map { it.id })

            interests.value = setOf("old_streets")
            advanceUntilIdle()
            assertEquals(listOf("gamma", "beta"), awaitItem().cards.map { it.id })
        }
    }

    @Test
    fun cardClickEmitsOneShotNavigationEffect() = runTest(mainDispatcher) {
        val vm = viewModel(MutableStateFlow(emptySet()))

        vm.effects.test {
            vm.onIntent(HomeIntent.CardClicked("beta"))
            advanceUntilIdle()

            assertEquals(HomeEffect.NavigateToRoute("beta"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun TestScope.viewModel(interests: MutableStateFlow<Set<String>>): HomeViewModel {
        val prefs = mockk<UserPreferencesRepository>()
        every { prefs.selectedInterests } returns interests

        val resources = mockk<Resources>()
        every { resources.getIdentifier(any(), "drawable", "com.mcis.memoir") } returns 1

        return HomeViewModel(
            contentRepo = ContentRepository(FakeContentAssetLoader(snapshot()), this),
            prefsRepo = prefs,
            resources = resources,
            localeProvider = { Locale.ENGLISH }
        )
    }

    private class FakeContentAssetLoader(
        private val snapshot: ContentSnapshot
    ) : ContentAssetLoader {
        override suspend fun load(): ContentSnapshot = snapshot
    }

    private fun snapshot(): ContentSnapshot = ContentSnapshot(
        routes = linkedMapOf(
            "alpha" to route("alpha", "Architecture Walk", "architecture"),
            "beta" to route("beta", "Temple Sound", "temples"),
            "gamma" to route("gamma", "Old Temple", "temples", "old_streets")
        ),
        spots = emptyMap()
    )

    private fun route(id: String, title: String, vararg tags: String): Route = Route(
        id = id,
        title = LocalizedText(title, title),
        category = LocalizedText("Category", "Category"),
        heroImage = id,
        description = LocalizedText("Description", "Description"),
        tags = tags.toList()
    )
}
