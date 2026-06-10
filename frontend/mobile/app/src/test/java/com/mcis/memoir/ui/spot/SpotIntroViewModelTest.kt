package com.mcis.memoir.ui.spot

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
class SpotIntroViewModelTest {
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
    fun stateHydratesFromContentRepository() = runTest(mainDispatcher) {
        val vm = viewModel()

        vm.state.test {
            assertTrue(awaitItem().isLoading)
            advanceUntilIdle()

            val loaded = awaitItem()
            assertFalse(loaded.isLoading)
            assertEquals("grand_mazu", loaded.spotId)
            assertEquals("Grand Mazu Temple", loaded.title)
            assertEquals(11, loaded.heroDrawableRes)
            assertEquals("0/3", loaded.foundLabel)
            assertEquals(3, loaded.discoveryItems.size)
            assertEquals(listOf("Dragon Pillar", "Mazu Statue", "Hanfan"), loaded.discoveryItems.map { it.label })
            assertEquals(listOf(21, 22, 23), loaded.discoveryItems.map { it.imageDrawableRes })
        }
    }

    @Test
    fun foundLabelCountsCapturedArtifactsForThisSpot() = runTest(mainDispatcher) {
        val vm = viewModel(
            capturedArtifactKeys = setOf("grand_mazu:1", "grand_mazu:3", "other_spot:2")
        )

        vm.state.test {
            assertTrue(awaitItem().isLoading)
            advanceUntilIdle()

            val loaded = awaitItem()
            assertEquals("2/3", loaded.foundLabel)
        }
    }

    @Test
    fun missingSpotProducesError() = runTest(mainDispatcher) {
        val vm = viewModel(spotId = "ghost")

        vm.state.test {
            assertTrue(awaitItem().isLoading)
            advanceUntilIdle()

            val loaded = awaitItem()
            assertFalse(loaded.isLoading)
            assertEquals("spot_not_found", loaded.error)
            assertEquals(emptyList<DiscoveryItemCard>(), loaded.discoveryItems)
        }
    }

    private fun TestScope.viewModel(
        spotId: String = "grand_mazu",
        locale: Locale = Locale.ENGLISH,
        capturedArtifactKeys: Set<String> = emptySet()
    ): SpotIntroViewModel = SpotIntroViewModel(
        spotId = spotId,
        contentRepo = ContentRepository(FakeContentAssetLoader(snapshot()), this),
        prefsRepo = FakePrefs(capturedArtifactKeys),
        resources = resources(),
        localeProvider = { locale }
    )

    private class FakePrefs(
        capturedKeys: Set<String>
    ) : UserPreferencesRepository {
        override val language: Flow<String> = MutableStateFlow("en")
        override val selectedInterests: Flow<Set<String>> = MutableStateFlow(emptySet())
        override val onboardingDone: Flow<Boolean> = MutableStateFlow(true)
        override val bookmarkedRouteIds: Flow<Set<String>> = MutableStateFlow(emptySet())
        override val capturedArtifactKeys: Flow<Set<String>> = MutableStateFlow(capturedKeys)

        override suspend fun setLanguage(tag: String) = Unit
        override suspend fun setInterests(set: Set<String>) = Unit
        override suspend fun markOnboardingDone() = Unit
        override suspend fun setBookmarkedRouteIds(set: Set<String>) = Unit
        override suspend fun setCapturedArtifactKeys(set: Set<String>) = Unit
        override suspend fun persistedLanguageTag(): String? = null
    }

    private class FakeContentAssetLoader(
        private val snapshot: ContentSnapshot
    ) : ContentAssetLoader {
        override suspend fun load(): ContentSnapshot = snapshot
    }

    private fun resources(): Resources {
        val resources = mockk<Resources>()
        every { resources.getIdentifier("grand_mazu_hero", "drawable", "com.mcis.memoir") } returns 11
        every { resources.getIdentifier("dragon_pillar", "drawable", "com.mcis.memoir") } returns 21
        every { resources.getIdentifier("mazu_statue", "drawable", "com.mcis.memoir") } returns 22
        every { resources.getIdentifier("hanfan", "drawable", "com.mcis.memoir") } returns 23
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
            artifact(1, "Dragon Pillar", "龍柱", "dragon_pillar"),
            artifact(2, "Mazu Statue", "媽祖像", "mazu_statue"),
            artifact(3, "Hanfan", "憨番", "hanfan")
        )
    )

    private fun artifact(id: Int, titleEn: String, titleZh: String, image: String): Artifact = Artifact(
        id = id,
        title = LocalizedText(titleEn, titleZh),
        description = LocalizedText("Story $id", "故事 $id"),
        question = LocalizedText("Question $id", "問題 $id"),
        image = image
    )
}
