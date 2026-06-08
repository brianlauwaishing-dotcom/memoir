package com.mcis.memoir.data.content

interface ContentAssetLoader {
    suspend fun load(): ContentSnapshot
}
