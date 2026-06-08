package com.mcis.memoir.ui.home

import android.content.res.Resources
import com.mcis.memoir.data.content.model.Route
import java.util.Locale

data class RouteCard(
    val id: String,
    val title: String,
    val category: String,
    val heroDrawableRes: Int,
    val description: String
)

internal fun Route.toCard(locale: Locale, resources: Resources): RouteCard = RouteCard(
    id = id,
    title = title[locale],
    category = category[locale],
    heroDrawableRes = resources.getIdentifier(heroImage, "drawable", "com.mcis.memoir"),
    description = description[locale]
)
