package com.mcis.memoir.ui.route

import android.content.res.Resources
import app.cash.turbine.test
import com.mcis.memoir.data.content.ContentAssetLoader
import com.mcis.memoir.data.content.ContentRepository
import com.mcis.memoir.data.content.ContentSnapshot
import com.mcis.memoir.data.content.model.JourneyStop
import com.mcis.memoir.data.content.model.LocalizedFacts
import com.mcis.memoir.data.content.model.LocalizedText
import com.mcis.memoir.data.content.model.Route
import com.mcis.memoir.data.content.model.Spot
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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RouteDetailViewModelTest {
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
    fun stateResolvesContentAndBookmarkMembership() = runTest(mainDispatcher) {
        val bookmarks = MutableStateFlow(emptySet<String>())
        val prefs = FakePrefs(bookmarks)
        val vm = viewModel(prefs)

        vm.state.test {
            assertTrue(awaitItem().isLoading)
            advanceUntilIdle()

            val loaded = awaitItem()
            assertFalse(loaded.isLoading)
            assertEquals("sounds_of_temple", loaded.routeId)
            assertEquals("Sounds of Temple Tainan", loaded.title)
            assertEquals("Temple route description", loaded.description)
            assertEquals(42, loaded.heroDrawableRes)
            assertEquals(listOf("grand_mazu", "hayashi"), loaded.journey.map { it.spotId })
            assertEquals(listOf("Grand Mazu Temple", "Hayashi Department Store"), loaded.journey.map { it.label })
            assertFalse(loaded.isSaved)

            bookmarks.value = setOf("sounds_of_temple")
            advanceUntilIdle()
            assertTrue(awaitItem().isSaved)
        }
    }

    @Test
    fun stateUsesChineseLocaleWhenRequested() = runTest(mainDispatcher) {
        val vm = viewModel(FakePrefs(MutableStateFlow(emptySet())), locale = Locale("zh"))

        vm.state.test {
            assertTrue(awaitItem().isLoading)
            advanceUntilIdle()

            val loaded = awaitItem()
            assertEquals("台南寺廟之聲", loaded.title)
            assertEquals("大天后宮", loaded.journey.first().label)
        }
    }

    @Test
    fun missingRouteProducesRouteNotFoundError() = runTest(mainDispatcher) {
        val vm = viewModel(FakePrefs(MutableStateFlow(emptySet())), routeId = "missing")

        vm.state.test {
            assertTrue(awaitItem().isLoading)
            advanceUntilIdle()

            val loaded = awaitItem()
            assertFalse(loaded.isLoading)
            assertEquals("route_not_found", loaded.error)
            assertEquals(null, loaded.routeId)
        }
    }

    @Test
    fun bookmarkTogglePersistsToggledSetAndStateFollowsFlow() = runTest(mainDispatcher) {
        val bookmarks = MutableStateFlow(emptySet<String>())
        val prefs = FakePrefs(bookmarks)
        val vm = viewModel(prefs)

        advanceUntilIdle()
        vm.onIntent(RouteDetailIntent.BookmarkToggled)
        advanceUntilIdle()

        assertEquals(listOf(setOf("sounds_of_temple")), prefs.savedBookmarks)
        assertTrue(vm.state.value.isSaved)

        vm.onIntent(RouteDetailIntent.BookmarkToggled)
        advanceUntilIdle()

        assertEquals(listOf(setOf("sounds_of_temple"), emptySet<String>()), prefs.savedBookmarks)
        assertFalse(vm.state.value.isSaved)
    }

    @Test
    fun rapidBookmarkTogglesSerializeWithoutLostUpdate() = runTest(mainDispatcher) {
        val prefs = FakePrefs(MutableStateFlow(emptySet()))
        val vm = viewModel(prefs)

        advanceUntilIdle()
        repeat(3) {
            vm.onIntent(RouteDetailIntent.BookmarkToggled)
        }
        advanceUntilIdle()

        assertEquals(
            listOf(
                setOf("sounds_of_temple"),
                emptySet(),
                setOf("sounds_of_temple")
            ),
            prefs.savedBookmarks
        )
        assertEquals(setOf("sounds_of_temple"), prefs.bookmarks.value)
    }

    @Test
    fun spotClickedIsStateNoOp() = runTest(mainDispatcher) {
        val vm = viewModel(FakePrefs(MutableStateFlow(emptySet())))
        advanceUntilIdle()
        val before = vm.state.value

        vm.onIntent(RouteDetailIntent.SpotClicked("grand_mazu"))
        advanceUntilIdle()

        assertEquals(before, vm.state.value)
    }

    private fun TestScope.viewModel(
        prefs: FakePrefs,
        routeId: String = "sounds_of_temple",
        locale: Locale = Locale.ENGLISH
    ): RouteDetailViewModel {
        val resources = mockk<Resources>()
        every { resources.getIdentifier("sounds_of_temple", "drawable", "com.mcis.memoir") } returns 42

        return RouteDetailViewModel(
            routeId = routeId,
            contentRepo = ContentRepository(FakeContentAssetLoader(snapshot()), this),
            prefsRepo = prefs,
            resources = resources,
            localeProvider = { locale }
        )
    }

    private class FakePrefs(
        val bookmarks: MutableStateFlow<Set<String>>
    ) : UserPreferencesRepository {
        val savedBookmarks = mutableListOf<Set<String>>()

        override val language: Flow<String> = MutableStateFlow("en")
        override val selectedInterests: Flow<Set<String>> = MutableStateFlow(emptySet())
        override val onboardingDone: Flow<Boolean> = MutableStateFlow(false)
        override val bookmarkedRouteIds: Flow<Set<String>> = bookmarks

        override suspend fun setLanguage(tag: String) = Unit
        override suspend fun setInterests(set: Set<String>) = Unit
        override suspend fun markOnboardingDone() = Unit

        override suspend fun setBookmarkedRouteIds(set: Set<String>) {
            savedBookmarks += set
            bookmarks.value = set
        }

        override suspend fun persistedLanguageTag(): String? = null
    }

    private class FakeContentAssetLoader(
        private val snapshot: ContentSnapshot
    ) : ContentAssetLoader {
        override suspend fun load(): ContentSnapshot = snapshot
    }

    private fun snapshot(): ContentSnapshot = ContentSnapshot(
        routes = linkedMapOf("sounds_of_temple" to route("sounds_of_temple")),
        spots = linkedMapOf(
            "grand_mazu" to spot("grand_mazu", "Grand Mazu Temple", "大天后宮"),
            "hayashi" to spot("hayashi", "Hayashi Department Store", "林百貨")
        )
    )

    private fun route(id: String): Route = Route(
        id = id,
        title = LocalizedText("Sounds of Temple Tainan", "台南寺廟之聲"),
        category = LocalizedText("Temples", "寺廟"),
        heroImage = "sounds_of_temple",
        description = LocalizedText("Temple route description", "寺廟路線介紹"),
        tags = listOf("temples"),
        journey = listOf(
            JourneyStop(2, "hayashi", LocalizedText("Unused", "Unused")),
            JourneyStop(1, "grand_mazu", LocalizedText("Unused", "Unused"))
        )
    )

    private fun spot(id: String, en: String, zh: String): Spot = Spot(
        id = id,
        title = LocalizedText(en, zh),
        heroImage = id,
        duration = LocalizedText("10 min", "10 分鐘"),
        whyItMatters = LocalizedText("Why", "重要性"),
        historicalContext = LocalizedText("History", "歷史"),
        architecturalFeatures = LocalizedText("Architecture", "建築"),
        modernUse = LocalizedText("Modern", "現代用途"),
        facts = LocalizedFacts()
    )
}
