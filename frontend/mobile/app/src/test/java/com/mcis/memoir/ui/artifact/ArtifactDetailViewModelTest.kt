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

    private fun TestScope.viewModel(
        artifactId: Int = 1,
        locale: Locale = Locale.ENGLISH
    ): ArtifactDetailViewModel = ArtifactDetailViewModel(
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
