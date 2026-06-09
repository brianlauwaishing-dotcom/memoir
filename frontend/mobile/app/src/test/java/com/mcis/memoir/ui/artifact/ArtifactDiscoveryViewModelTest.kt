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
class ArtifactDiscoveryViewModelTest {
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
    fun computesHighlightAndDisplayPosition() = runTest(mainDispatcher) {
        val vm = viewModel(artifactId = 7)

        vm.state.test {
            assertTrue(awaitItem().isLoading)
            advanceUntilIdle()

            val loaded = awaitItem()
            assertFalse(loaded.isLoading)
            assertEquals("grand_mazu", loaded.spotId)
            assertEquals(7, loaded.artifactId)
            assertEquals(2, loaded.displayPosition)
            assertEquals(3, loaded.totalArtifacts)
            assertEquals("Mazu Statue", loaded.label)
            assertEquals(
                QuestionHighlight(prefix = "What is the ", label = "Mazu Statue", suffix = "?"),
                loaded.highlight
            )
            assertEquals(22, loaded.imageDrawableRes)
        }
    }

    @Test
    fun nonSequentialArtifactIdKeepsStableNavigationId() = runTest(mainDispatcher) {
        val vm = viewModel(artifactId = 11)

        vm.state.test {
            awaitItem()
            advanceUntilIdle()

            val loaded = awaitItem()
            assertEquals(11, loaded.artifactId)
            assertEquals(3, loaded.displayPosition)
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

    private fun TestScope.viewModel(
        artifactId: Int,
        locale: Locale = Locale.ENGLISH
    ): ArtifactDiscoveryViewModel = ArtifactDiscoveryViewModel(
        spotId = "grand_mazu",
        artifactId = artifactId,
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
            artifact(5, "Dragon Pillar", "龍柱", "dragon_pillar", "How many dragons?"),
            artifact(7, "Mazu Statue", "媽祖像", "mazu_statue", "What is the Mazu Statue?"),
            artifact(11, "Hanfan", "憨番", "hanfan", "What is he doing?")
        )
    )

    private fun artifact(id: Int, titleEn: String, titleZh: String, image: String, questionEn: String): Artifact = Artifact(
        id = id,
        title = LocalizedText(titleEn, titleZh),
        description = LocalizedText("Story $id", "故事 $id"),
        question = LocalizedText(questionEn, "$titleZh 上有什麼？"),
        image = image
    )
}
