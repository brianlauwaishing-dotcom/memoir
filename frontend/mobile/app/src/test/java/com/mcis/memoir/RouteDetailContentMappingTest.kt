package com.mcis.memoir

import android.content.res.Resources
import com.mcis.memoir.data.content.model.JourneyStop
import com.mcis.memoir.data.content.model.LocalizedText
import com.mcis.memoir.data.content.model.Route
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RouteDetailContentMappingTest {
    @Test
    fun contentRouteKeepsRouteIdAndJourneyForDetailScreen() {
        val resources = mockk<Resources>()
        every { resources.getIdentifier("faith_hidden", "drawable", "com.mcis.memoir") } returns 42

        val route = Route(
            id = "faith_hidden_in_alleyways",
            title = LocalizedText("Faith Hidden in Alleyways", "Faith Hidden ZH"),
            category = LocalizedText("Temples & Folk Beliefs", "Temple Beliefs ZH"),
            heroImage = "faith_hidden",
            description = LocalizedText("Small shrines with big stories.", "Small shrines ZH"),
            tags = listOf("temples"),
            journey = listOf(
                JourneyStop(2, "kaiji_jade_emperor_temple", LocalizedText("Kaiji Jade Emperor Temple", "Kaiji ZH")),
                JourneyStop(1, "zonggong_temple", LocalizedText("Zonggong Temple", "Zonggong ZH"))
            )
        )

        val detailRoute = route.toRouteData(resources)

        assertEquals("faith_hidden_in_alleyways", detailRoute.id)
        assertEquals("Faith Hidden in Alleyways", detailRoute.titleEn)
        assertEquals("Faith Hidden ZH", detailRoute.titleZh)
        assertEquals(42, detailRoute.imageRes)
        assertEquals(listOf("zonggong_temple", "kaiji_jade_emperor_temple"), detailRoute.journeyItems.map { it.spotId })
    }
}
