package com.mcis.memoir.ui.home

import com.mcis.memoir.data.content.model.LocalizedText
import com.mcis.memoir.data.content.model.Route
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RankingTest {
    @Test
    fun emptyInterestsPreserveInputOrder() {
        val routes = listOf(route("b", "temples"), route("a", "architecture"), route("c", "trade"))

        assertEquals(listOf("b", "a", "c"), routes.sortedWith(rankComparator(emptySet())).map { it.id })
    }

    @Test
    fun matchingInterestRanksBeforeNonMatchingRoute() {
        val routes = listOf(route("architecture", "architecture"), route("temple", "temples"))

        assertEquals(
            listOf("temple", "architecture"),
            routes.sortedWith(rankComparator(setOf("temples"))).map { it.id }
        )
    }

    @Test
    fun multiMatchOutranksSingleMatchAndZeroMatchRemainsLast() {
        val routes = listOf(
            route("zero", "architecture"),
            route("single", "temples"),
            route("multi", "temples", "old_streets")
        )

        assertEquals(
            listOf("multi", "single", "zero"),
            routes.sortedWith(rankComparator(setOf("temples", "old_streets"))).map { it.id }
        )
    }

    private fun route(id: String, vararg tags: String): Route = Route(
        id = id,
        title = LocalizedText(id, id),
        category = LocalizedText("Category", "Category"),
        heroImage = id,
        description = LocalizedText("Description", "Description"),
        tags = tags.toList()
    )
}
