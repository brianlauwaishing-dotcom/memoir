package com.mcis.memoir.data.content.model

import java.util.Locale
import kotlinx.serialization.Serializable

@Serializable
data class LocalizedText(
    val en: String,
    val zh: String
) {
    operator fun get(locale: Locale): String = if (locale.language == "zh") zh else en
}
