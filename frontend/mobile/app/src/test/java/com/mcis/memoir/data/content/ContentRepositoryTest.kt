package com.mcis.memoir.data.content

import com.mcis.memoir.data.content.model.LocalizedFacts
import com.mcis.memoir.data.content.model.LocalizedText
import com.mcis.memoir.data.content.model.Route
import com.mcis.memoir.data.content.model.Spot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ContentRepositoryTest {
    @Test
    fun servesRoutesAndSpotsFromSingleCachedLoad() = runTest {
        val loader = FakeContentAssetLoader(snapshot())
        val repository = ContentRepository(loader, this)

        assertNotNull(repository.route("known"))
        assertNull(repository.route("unknown"))
        assertNotNull(repository.spot("known_spot"))
        assertEquals(listOf("known", "second"), repository.routes().first().map { it.id })
        assertEquals(1, loader.loadCount)
    }

    @Test
    fun loaderExceptionPropagatesThroughLookupsAndFlow() {
        val failure = IllegalStateException("broken content")

        assertThrows(IllegalStateException::class.java) {
            runTest {
                ContentRepository(FailingContentAssetLoader(failure), this).route("any")
            }
        }
        assertThrows(IllegalStateException::class.java) {
            runTest {
                ContentRepository(FailingContentAssetLoader(failure), this).routes().first()
            }
        }
    }

    private class FakeContentAssetLoader(
        private val snapshot: ContentSnapshot
    ) : ContentAssetLoader {
        var loadCount = 0

        override suspend fun load(): ContentSnapshot {
            loadCount++
            return snapshot
        }
    }

    private class FailingContentAssetLoader(
        private val failure: Throwable
    ) : ContentAssetLoader {
        override suspend fun load(): ContentSnapshot {
            throw failure
        }
    }

    private fun snapshot(): ContentSnapshot {
        val routes = linkedMapOf(
            "known" to route("known"),
            "second" to route("second")
        )
        val spots = linkedMapOf("known_spot" to spot("known_spot"))
        return ContentSnapshot(routes = routes, spots = spots)
    }

    private fun route(id: String): Route = Route(
        id = id,
        title = LocalizedText(id, id),
        category = LocalizedText("Category", "分類"),
        heroImage = "sounds_of_temple",
        description = LocalizedText("Description", "說明")
    )

    private fun spot(id: String): Spot = Spot(
        id = id,
        title = LocalizedText(id, id),
        heroImage = "grand_mazu_temple",
        duration = LocalizedText("10 min", "10 分"),
        whyItMatters = LocalizedText("Why", "原因"),
        historicalContext = LocalizedText("History", "歷史"),
        architecturalFeatures = LocalizedText("Architecture", "建築"),
        modernUse = LocalizedText("Modern", "現代"),
        facts = LocalizedFacts()
    )
}
