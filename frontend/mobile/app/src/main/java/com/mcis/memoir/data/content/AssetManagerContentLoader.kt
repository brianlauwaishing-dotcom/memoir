package com.mcis.memoir.data.content

import android.content.res.AssetManager
import com.mcis.memoir.data.content.model.Route
import com.mcis.memoir.data.content.model.Spot
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class AssetManagerContentLoader(
    private val assets: AssetManager,
    private val json: Json
) : ContentAssetLoader {
    override suspend fun load(): ContentSnapshot = withContext(Dispatchers.IO) {
        val index = decodeAsset<Index>("tainan-route/index.json")
        val routePairs = index.routes.map { id ->
            id to decodeAsset<Route>("tainan-route/routes/$id.json")
        }
        val spotPairs = index.spots.map { id ->
            id to decodeAsset<Spot>("tainan-route/spots/$id.json")
        }
        val routes = routePairs.toMap(LinkedHashMap())
        val spots = spotPairs.toMap(LinkedHashMap())
        validateIdsMatchFilenames(routes, spots)
        ContentSnapshot(routes = routes, spots = spots)
    }

    private inline fun <reified T> decodeAsset(path: String): T =
        json.decodeFromString(readAsset(path))

    private fun readAsset(path: String): String =
        try {
            assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (exception: IOException) {
            throw IllegalStateException("Missing content asset: $path", exception)
        }

    @Serializable
    private data class Index(
        val routes: List<String>,
        val spots: List<String>
    )
}

internal fun validateIdsMatchFilenames(
    routes: Map<String, Route>,
    spots: Map<String, Spot>
) {
    routes.forEach { (filename, route) ->
        check(route.id == filename) {
            "Route id mismatch: routes/$filename.json declares ${route.id}"
        }
    }
    spots.forEach { (filename, spot) ->
        check(spot.id == filename) {
            "Spot id mismatch: spots/$filename.json declares ${spot.id}"
        }
    }
}
