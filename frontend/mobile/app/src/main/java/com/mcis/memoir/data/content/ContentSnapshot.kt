package com.mcis.memoir.data.content

import com.mcis.memoir.data.content.model.Route
import com.mcis.memoir.data.content.model.Spot

data class ContentSnapshot(
    val routes: Map<String, Route>,
    val spots: Map<String, Spot>
)
