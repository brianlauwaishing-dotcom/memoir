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
import io.mockk.every
import io.mockk.mockk
import java.util.Locale
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
class SpotDetailViewModelTest {
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
            assertEquals("Grand Mazu Temple", loaded.title)
            assertEquals(11, loaded.heroDrawableRes)
            assertEquals("20 min", loaded.duration)
            assertEquals("Why", loaded.whyItMatters)
            assertEquals("History", loaded.historicalContext)
            assertEquals("Architecture", loaded.architecturalFeatures)
            assertEquals("Modern", loaded.modernUse)
            assertEquals(2, loaded.discoveryItems.size)
            assertEquals(listOf("Dragon Pillar", "Mazu Statue"), loaded.discoveryItems.map { it.label })
            assertTrue(loaded.feelings.isNotEmpty())
        }
    }

    @Test
    fun galleryPrefersGalleryImageThenFallsBackToImage() = runTest(mainDispatcher) {
        val vm = viewModel()

        vm.state.test {
            assertTrue(awaitItem().isLoading)
            advanceUntilIdle()

            val loaded = awaitItem()
            // artifact 1 has galleryImage "dragon_pillar_gallery" -> 31
            // artifact 2 has no galleryImage, falls back to image "mazu_statue" -> 22
            assertEquals(listOf(31, 22), loaded.discoveryItems.map { it.imageDrawableRes })
        }
    }

    @Test
    fun localeDrivesContentAndFeelings() = runTest(mainDispatcher) {
        val vm = viewModel(locale = Locale.TRADITIONAL_CHINESE)

        vm.state.test {
            assertTrue(awaitItem().isLoading)
            advanceUntilIdle()

            val loaded = awaitItem()
            assertEquals("大媽祖廟", loaded.title)
            assertEquals("重要性", loaded.whyItMatters)
            assertEquals(listOf("龍柱", "媽祖像"), loaded.discoveryItems.map { it.label })
            assertTrue(loaded.feelings.contains("敬畏"))
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
            assertEquals(emptyList<SpotDetailGalleryItem>(), loaded.discoveryItems)
        }
    }

    private fun TestScope.viewModel(
        spotId: String = "grand_mazu",
        locale: Locale = Locale.ENGLISH
    ): SpotDetailViewModel = SpotDetailViewModel(
        spotId = spotId,
        contentRepo = ContentRepository(FakeContentAssetLoader(snapshot()), this),
        resources = resources(),
        localeProvider = { locale }
    )

    private class FakeContentAssetLoader(
        private val snapshot: ContentSnapshot
    ) : ContentAssetLoader {
        override suspend fun load(): ContentSnapshot = snapshot
    }

    private fun resources(): Resources {
        val resources = mockk<Resources>()
        every { resources.getIdentifier("grand_mazu_hero", "drawable", "com.mcis.memoir") } returns 11
        every { resources.getIdentifier("mazu_statue", "drawable", "com.mcis.memoir") } returns 22
        every { resources.getIdentifier("dragon_pillar_gallery", "drawable", "com.mcis.memoir") } returns 31
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
            artifact(1, "Dragon Pillar", "龍柱", image = "dragon_pillar", galleryImage = "dragon_pillar_gallery"),
            artifact(2, "Mazu Statue", "媽祖像", image = "mazu_statue", galleryImage = null)
        )
    )

    private fun artifact(
        id: Int,
        titleEn: String,
        titleZh: String,
        image: String,
        galleryImage: String?
    ): Artifact = Artifact(
        id = id,
        title = LocalizedText(titleEn, titleZh),
        description = LocalizedText("Story $id", "故事 $id"),
        question = LocalizedText("Question $id", "問題 $id"),
        image = image,
        galleryImage = galleryImage
    )
}
