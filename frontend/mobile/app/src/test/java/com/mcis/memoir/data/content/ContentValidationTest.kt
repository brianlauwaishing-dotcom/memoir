package com.mcis.memoir.data.content

import com.mcis.memoir.R
import com.mcis.memoir.data.content.model.Route
import com.mcis.memoir.data.content.model.Spot
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentValidationTest {
    @Test
    fun indexEnumeratesEveryRouteAndSpotFile() {
        val root = contentRoot()
        val index = readIndex(root)

        assertEquals(index.routes.sorted(), index.routes)
        assertEquals(index.spots.sorted(), index.spots)
        assertEquals(index.routes.size, index.routes.toSet().size)
        assertEquals(index.spots.size, index.spots.toSet().size)
        assertEquals(index.routes, jsonIds(root.resolve("routes")))
        assertEquals(index.spots, jsonIds(root.resolve("spots")))
    }

    @Test
    fun fileIdsMatchBasenamesAndJourneysResolve() {
        val root = contentRoot()
        val index = readIndex(root)
        val spotIds = index.spots.toSet()
        val routes = index.routes.associateWith { id ->
            ContentJson.decodeFromString<Route>(root.resolve("routes/$id.json").readText())
        }
        val spots = index.spots.associateWith { id ->
            ContentJson.decodeFromString<Spot>(root.resolve("spots/$id.json").readText())
        }

        validateIdsMatchFilenames(routes, spots)
        routes.values.forEach { route ->
            route.journey.forEach { stop ->
                assertTrue("${route.id} references missing spot ${stop.spotId}", stop.spotId in spotIds)
            }
            val orders = route.journey.map { it.order }
            assertEquals("${route.id} has duplicate journey order values", orders.size, orders.toSet().size)
        }
    }

    @Test
    fun assetsReferenceExistingDrawableNames() {
        val assets = ContentJson.parseToJsonElement(contentRoot().resolve("_assets.json").readText()).jsonObject
        val drawableNames = R.drawable::class.java.fields.map { it.name }.toSet()

        collectDrawableNames(assets).forEach { name ->
            assertTrue("Missing drawable referenced by _assets.json: $name", name in drawableNames)
        }
    }

    private fun contentRoot(): File {
        var dir = File(checkNotNull(System.getProperty("user.dir"))).absoluteFile
        repeat(8) {
            val candidate = dir.resolve("data/tainan-route")
            if (candidate.resolve("index.json").isFile) {
                return candidate
            }
            dir = dir.parentFile ?: dir
        }
        error("Unable to locate data/tainan-route from ${System.getProperty("user.dir")}")
    }

    private fun readIndex(root: File): Index =
        ContentJson.decodeFromString(root.resolve("index.json").readText())

    private fun jsonIds(dir: File): List<String> =
        dir.listFiles { file -> file.extension == "json" }
            ?.map { it.nameWithoutExtension }
            ?.sorted()
            ?: emptyList()

    private fun collectDrawableNames(element: JsonElement): List<String> =
        when (element) {
            is JsonObject -> element.flatMap { (key, value) ->
                if (key == "heroImage") {
                    listOf((value as JsonPrimitive).content)
                } else {
                    collectDrawableNames(value)
                }
            }
            is JsonArray -> element.flatMap { collectDrawableNames(it) }
            is JsonPrimitive -> if (element.isString) listOf(element.content) else emptyList()
        }

    @Serializable
    private data class Index(
        val routes: List<String>,
        val spots: List<String>
    )
}
