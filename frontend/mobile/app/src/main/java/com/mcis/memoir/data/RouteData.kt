package com.mcis.memoir.data

import androidx.annotation.DrawableRes

data class RouteData(
    val id: String,
    val titleEn: String,
    val titleZh: String,
    val categoryEn: String,
    val categoryZh: String,
    @DrawableRes val imageRes: Int,
    val descriptionEn: String = "",
    val descriptionZh: String = "",
    val journeyItems: List<JourneyItem> = emptyList()
)

data class JourneyItem(
    val id: Int,
    val spotId: String,
    val labelEn: String,
    val labelZh: String
)

data class SpotData(
    val id: String,
    val titleEn: String,
    val titleZh: String,
    @DrawableRes val imageRes: Int,
    val durationEn: String = "30–45 mins",
    val durationZh: String = "30–45 分鐘",
    val whyItMattersEn: String = "",
    val whyItMattersZh: String = "",
    val historicalContextEn: String = "",
    val historicalContextZh: String = "",
    val architecturalFeaturesEn: String = "",
    val architecturalFeaturesZh: String = "",
    val modernUseEn: String = "",
    val modernUseZh: String = "",
    val feelingsEn: List<String> = listOf("Awe", "Peaceful", "Inspired", "Curious", "Grateful", "Amazed"),
    val feelingsZh: List<String> = listOf("敬畏", "平靜", "受啟發", "好奇", "感激", "驚嘆"),
    val factsEn: List<String> = emptyList(),
    val factsZh: List<String> = emptyList(),
    val photographyTips: List<PhotographyTip> = emptyList(),
    val discoveryItems: List<DiscoveryItem> = emptyList()
)

data class DiscoveryItem(
    val id: Int,
    val labelEn: String,
    val labelZh: String,
    @DrawableRes val imageRes: Int,
    @DrawableRes val galleryImageRes: Int? = null,
    val questionEn: String = "",
    val questionZh: String = "",
    val moreInfoEn: String = "",
    val moreInfoZh: String = ""
)

data class PhotographyTip(
    val id: Int,
    val descriptionEn: String,
    val descriptionZh: String,
    @DrawableRes val imageRes: Int? = null
)

data class MemoryData(
    val id: String,
    val titleEn: String,
    val titleZh: String,
    val status: MemoryStatus,
    val date: String, // "Updated on YYYY/MM/DD" or "2024.05.20"
    @DrawableRes val imageRes: Int,
    val currentProgress: Int = 0,
    val totalProgress: Int = 0,
    val likes: Int = 0,
    val comments: Int = 0
)

enum class MemoryStatus {
    IN_PROGRESS, COMPLETED
}

data class TemplateData(
    val id: String,
    val titleEn: String,
    val titleZh: String,
    val descriptionEn: String,
    val descriptionZh: String,
    @DrawableRes val imageRes: Int,
    @DrawableRes val maskRes: Int? = null,
    val slots: List<TemplateSlot> = emptyList()
)

data class TemplateSlot(
    val xPercent: Float, // 0.0 to 1.0
    val yPercent: Float,
    val widthPercent: Float,
    val heightPercent: Float,
    val rotation: Float = 0f
)
