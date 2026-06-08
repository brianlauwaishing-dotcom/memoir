package com.mcis.memoir.data.content

import com.mcis.memoir.data.content.model.Route
import com.mcis.memoir.data.content.model.Spot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ContentRepository(
    loader: ContentAssetLoader,
    scope: CoroutineScope
) {
    private val snapshot: Deferred<ContentSnapshot> =
        scope.async(Dispatchers.IO) { loader.load() }

    fun routes(): Flow<List<Route>> = flow {
        emit(snapshot.await().routes.values.toList())
    }

    suspend fun route(id: String): Route? = snapshot.await().routes[id]

    suspend fun spot(id: String): Spot? = snapshot.await().spots[id]
}
