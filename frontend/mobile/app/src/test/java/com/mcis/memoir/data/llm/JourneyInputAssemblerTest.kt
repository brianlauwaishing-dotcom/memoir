package com.mcis.memoir.data.llm

import com.mcis.memoir.data.content.ContentRepository
import com.mcis.memoir.data.content.model.JourneyStop
import com.mcis.memoir.data.content.model.LocalizedText
import com.mcis.memoir.data.content.model.Route
import com.mcis.memoir.data.memory.Memory
import com.mcis.memoir.data.memory.MemoryStatus
import io.mockk.coEvery
import io.mockk.mockk
import java.util.Locale
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class JourneyInputAssemblerTest {

    private fun text(value: String) = LocalizedText(en = value, zh = value)

    private fun route(id: String, spotIds: List<String>) = Route(
        id = id,
        title = text("Route"),
        category = text("Cat"),
        heroImage = "hero.jpg",
        description = text("Desc"),
        tags = emptyList(),
        journey = spotIds.mapIndexed { i, spotId ->
            JourneyStop(order = i, spotId = spotId, title = text("Stop"))
        }
    )

    private fun memory(
        routeId: String?,
        photoPaths: List<String>,
        templateId: String = "old_street"
    ) = Memory(
        id = "11111111-1111-4111-8111-111111111111",
        templateId = templateId,
        routeId = routeId,
        title = "Title",
        status = MemoryStatus.COMPLETED,
        createdAt = 1L,
        updatedAt = 1L,
        photoRelativePaths = photoPaths,
        spotNotes = emptyMap(),
        overallMood = "calm",
        userInsights = "insights",
        postTripFeedback = "feedback",
        generatedReflection = null,
        editorState = null
    )

    @Test
    fun routeLookupPopulatesSpotEntriesWithEvenPhotoSplit() = runTest {
        val repo = mockk<ContentRepository>()
        coEvery { repo.route("r1") } returns route("r1", listOf("s1", "s2", "s3"))
        val mem = memory(routeId = "r1", photoPaths = List(15) { "p$it" })

        val input = JourneyInputAssembler.build(mem, Locale.ENGLISH, repo)

        assertEquals("r1", input.routeId)
        assertEquals(3, input.spotEntries.size)
        assertEquals(listOf("s1", "s2", "s3"), input.spotEntries.map { it.spotId })
        assertTrue(input.spotEntries.all { it.photoCount == 5 })
        assertTrue(input.spotEntries.all { it.userNote == null })
        assertEquals("old_street", input.templateStyle)
    }

    @Test
    fun nullRouteIdProducesNonePlaceholderAndEmptySpots() = runTest {
        val repo = mockk<ContentRepository>()
        val mem = memory(routeId = null, photoPaths = listOf("p1", "p2"))

        val input = JourneyInputAssembler.build(mem, Locale.ENGLISH, repo)

        assertEquals("(none)", input.routeId)
        assertTrue(input.spotEntries.isEmpty())
    }

    @Test
    fun emptyPhotoPathsYieldZeroPhotoCountPerStop() = runTest {
        val repo = mockk<ContentRepository>()
        coEvery { repo.route("r1") } returns route("r1", listOf("s1", "s2"))
        val mem = memory(routeId = "r1", photoPaths = emptyList())

        val input = JourneyInputAssembler.build(mem, Locale.ENGLISH, repo)

        assertEquals(2, input.spotEntries.size)
        assertTrue(input.spotEntries.all { it.photoCount == 0 })
    }
}
