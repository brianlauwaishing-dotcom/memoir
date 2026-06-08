package com.mcis.memoir.ui.home

import com.mcis.memoir.data.content.ContentJson
import com.mcis.memoir.data.content.model.Route
import java.io.File
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HomeContentTagValidationTest {
    @Test
    fun committedRoutesDeclareKnownNonEmptyTags() {
        val routesDir = contentRoot().resolve("routes")

        routesDir.listFiles { file -> file.extension == "json" }
            .orEmpty()
            .forEach { file ->
                val route = ContentJson.decodeFromString<Route>(file.readText())

                assertTrue(
                    route.tags.isNotEmpty(),
                    "${route.id} must declare at least one tag from TagCatalog.ids"
                )
                val unknown = route.tags.filterNot { it in TagCatalog.ids }
                assertTrue(
                    unknown.isEmpty(),
                    "${route.id} declares unknown tag ids outside TagCatalog.ids: ${unknown.joinToString()}"
                )
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
}
