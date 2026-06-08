package com.mcis.memoir.data.content

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSerializationApi::class)
internal val ContentJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    prettyPrint = false
}
