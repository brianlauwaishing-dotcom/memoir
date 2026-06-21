package com.mcis.memoir.ui.artifact

import android.content.res.Resources
import app.cash.turbine.test
import com.mcis.memoir.data.content.ContentAssetLoader
import com.mcis.memoir.data.content.ContentRepository
import com.mcis.memoir.data.content.ContentSnapshot
import com.mcis.memoir.data.content.model.Artifact
import com.mcis.memoir.data.content.model.LocalizedFacts
import com.mcis.memoir.data.content.model.LocalizedText
import com.mcis.memoir.data.content.model.Spot
import com.mcis.memoir.data.prefs.UserPreferencesRepository
import io.mockk.every
import io.mockk.mockk
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class ArtifactDetailViewModelTest {
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
    fun stateUsesLocaleResolvedDescription() = runTest(mainDispatcher) {
        val vm = viewModel()

        vm.state.test {
            assertTrue(awaitItem().isLoading)
            advanceUntilIdle()

            val loaded = awaitItem()
            assertFalse(loaded.isLoading)
            assertEquals("Dragon Pillar", loaded.label)
            assertEquals("Long English story", loaded.description)
            assertEquals(21, loaded.imageDrawableRes)
        }
    }

    @Test
    fun missingArtifactProducesError() = runTest(mainDispatcher) {
        val vm = viewModel(artifactId = 99)

        vm.state.test {
            assertTrue(awaitItem().isLoading)
            advanceUntilIdle()

            val loaded = awaitItem()
            assertFalse(loaded.isLoading)
            assertEquals("artifact_not_found", loaded.error)
        }
    }

    @Test
    fun stateReflectsSpotBookmarkMembership() = runTest(mainDispatcher) {
        val vm = viewModel(bookmarkedSpotIds = setOf("grand_mazu"))

        vm.state.test {
            assertTrue(awaitItem().isLoading)
            advanceUntilIdle()

            val loaded = awaitItem()
            assertTrue(loaded.isBookmarked)
        }
    }

    @Test
    fun bookmarkClickRemovesCurrentSpotFromMemoriesBookmarks() = runTest(mainDispatcher) {
        val prefs = FakePrefs(bookmarkedSpotIds = setOf("grand_mazu", "other_spot"))
        val vm = viewModel(prefs = prefs)

        vm.state.test {
            awaitItem()
            advanceUntilIdle()
            awaitItem()

            vm.onBookmarkClick()
            advanceUntilIdle()

            assertFalse(awaitItem().isBookmarked)
            assertEquals(setOf("other_spot"), prefs.bookmarkedSpotIds.value)
        }
    }

    private fun TestScope.viewModel(
        artifactId: Int = 1,
        locale: Locale = Locale.ENGLISH,
        bookmarkedSpotIds: Set<String> = emptySet(),
        prefs: FakePrefs = FakePrefs(bookmarkedSpotIds = bookmarkedSpotIds)
    ): ArtifactDetailViewModel = ArtifactDetailViewModel(
        spotId = "grand_mazu",
        artifactId = artifactId,
        contentRepo = ContentRepository(FakeContentAssetLoader(snapshot()), this),
        prefsRepo = prefs,
        resources = resources(),
        localeProvider = { locale }
    )

    private class FakePrefs(
        bookmarkedSpotIds: Set<String> = emptySet()
    ) : UserPreferencesRepository {
        override val language: Flow<String> = MutableStateFlow("en")
        override val selectedInterests: Flow<Set<String>> = MutableStateFlow(emptySet())
        override val onboardingDone: Flow<Boolean> = MutableStateFlow(true)
        override val bookmarkedRouteIds: Flow<Set<String>> = MutableStateFlow(emptySet())
        override val bookmarkedSpotIds: MutableStateFlow<Set<String>> = MutableStateFlow(bookmarkedSpotIds)

        override suspend fun setLanguage(tag: String) = Unit
        override suspend fun setInterests(set: Set<String>) = Unit
        override suspend fun markOnboardingDone() = Unit
        override suspend fun setBookmarkedRouteIds(set: Set<String>) = Unit
        override suspend fun setBookmarkedSpotIds(set: Set<String>) {
            bookmarkedSpotIds.value = set
        }
        override suspend fun persistedLanguageTag(): String? = null
    }

    private class FakeContentAssetLoader(
        private val snapshot: ContentSnapshot
    ) : ContentAssetLoader {
        override suspend fun load(): ContentSnapshot = snapshot
    }

    private fun resources(): Resources {
        val resources = mockk<Resources>()
        every { resources.getIdentifier("dragon_pillar", "drawable", "com.mcis.memoir") } returns 21
        return resources
    }

    private fun snapshot(): ContentSnapshot = ContentSnapshot(
        routes = emptyMap(),
        spots = mapOf("grand_mazu" to spot())
    )

    private fun spot(): Spot = Spot(
        id = "grand_mazu",
        title = LocalizedText("Grand Mazu Temple", "大媽祖廟"),
        heroImage = "grand_mazu_hero",
        duration = LocalizedText("20 min", "20 分鐘"),
        whyItMatters = LocalizedText("Why", "重要性"),
        historicalContext = LocalizedText("History", "歷史"),
        architecturalFeatures = LocalizedText("Architecture", "建築"),
        modernUse = LocalizedText("Modern", "現代"),
        facts = LocalizedFacts(),
        artifacts = listOf(
            Artifact(
                id = 1,
                title = LocalizedText("Dragon Pillar", "龍柱"),
                description = LocalizedText("Long English story", "中文故事"),
                question = LocalizedText("How many dragons?", "龍柱上有幾條龍？"),
                image = "dragon_pillar"
            )
        )
    )
}
