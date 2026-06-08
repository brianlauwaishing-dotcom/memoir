package com.mcis.memoir.data.content

import com.mcis.memoir.data.content.model.LocalizedFacts
import com.mcis.memoir.data.content.model.LocalizedText
import com.mcis.memoir.data.content.model.Route
import com.mcis.memoir.data.content.model.Spot
import java.util.Locale
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ContentAssetLoaderTest {
    @Test
    fun parsesKnownGoodRouteAndSpotJson() {
        val route = ContentJson.decodeFromString<Route>(
            """
            {
              "id": "known_route",
              "title": {"en": "Known Route", "zh": "路線"},
              "category": {"en": "Category", "zh": "分類"},
              "heroImage": "sounds_of_temple",
              "description": {"en": "Route description", "zh": "說明"},
              "journey": [
                {"order": 1, "spotId": "known_spot", "title": {"en": "Known Spot", "zh": "景點"}}
              ],
              "tags": ["category"]
            }
            """.trimIndent()
        )
        val spot = ContentJson.decodeFromString<Spot>(
            """
            {
              "id": "known_spot",
              "title": {"en": "Known Spot", "zh": "景點"},
              "heroImage": "grand_mazu_temple",
              "duration": {"en": "10 min", "zh": "10 分"},
              "whyItMatters": {"en": "Why", "zh": "原因"},
              "historicalContext": {"en": "History", "zh": "歷史"},
              "architecturalFeatures": {"en": "Architecture", "zh": "建築"},
              "modernUse": {"en": "Modern", "zh": "現代"},
              "facts": {"en": ["one"], "zh": ["一"]},
              "photographyTips": [
                {"id": 1, "description": {"en": "Tip", "zh": "提示"}, "image": "sounds_of_temple"}
              ],
              "artifacts": [
                {"id": 1, "title": {"en": "Artifact", "zh": "文物"}, "description": {"en": "Look", "zh": "看"}, "image": "eg1"}
              ]
            }
            """.trimIndent()
        )

        assertEquals("Known Route", route.title[Locale.ENGLISH])
        assertEquals("路線", route.title[Locale.forLanguageTag("zh-TW")])
        assertEquals("known_spot", route.journey.single().spotId)
        assertEquals(listOf("one"), spot.facts.en)
        assertEquals("Tip", spot.photographyTips.single().description.en)
    }

    @Test
    fun malformedJsonThrowsSerializationException() {
        assertThrows(SerializationException::class.java) {
            ContentJson.decodeFromString<Route>("""{"id": }""")
        }
    }

    @Test
    fun idFilenameMismatchThrowsIllegalStateException() {
        val routes = mapOf(
            "route_file" to Route(
                id = "different_route",
                title = LocalizedText("Route", "路線"),
                category = LocalizedText("Category", "分類"),
                heroImage = "sounds_of_temple",
                description = LocalizedText("Description", "說明")
            )
        )
        val spots = mapOf(
            "spot_file" to Spot(
                id = "spot_file",
                title = LocalizedText("Spot", "景點"),
                heroImage = "grand_mazu_temple",
                duration = LocalizedText("10 min", "10 分"),
                whyItMatters = LocalizedText("Why", "原因"),
                historicalContext = LocalizedText("History", "歷史"),
                architecturalFeatures = LocalizedText("Architecture", "建築"),
                modernUse = LocalizedText("Modern", "現代"),
                facts = LocalizedFacts()
            )
        )

        assertThrows(IllegalStateException::class.java) {
            validateIdsMatchFilenames(routes, spots)
        }
    }
}
