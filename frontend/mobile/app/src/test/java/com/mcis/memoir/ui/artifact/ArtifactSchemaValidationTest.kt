package com.mcis.memoir.ui.artifact

import com.mcis.memoir.R
import com.mcis.memoir.data.content.ContentJson
import com.mcis.memoir.data.content.model.Spot
import java.io.File
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ArtifactSchemaValidationTest {
    @Test
    fun artifactQuestionsAreRequiredAndGalleryImagesResolve() {
        val root = contentRoot()
        val index = ContentJson.decodeFromString<Index>(root.resolve("index.json").readText())
        val drawableNames = R.drawable::class.java.fields.map { it.name }.toSet()

        index.spots.forEach { spotId ->
            val spot = ContentJson.decodeFromString<Spot>(root.resolve("spots/$spotId.json").readText())
            spot.artifacts.forEach { artifact ->
                assertTrue(
                    artifact.question.en.isNotBlank(),
                    "spots.$spotId.artifacts[${artifact.id}].question.en is blank"
                )
                assertTrue(
                    artifact.question.zh.isNotBlank(),
                    "spots.$spotId.artifacts[${artifact.id}].question.zh is blank"
                )
                artifact.galleryImage?.let { galleryImage ->
                    assertTrue(
                        galleryImage in drawableNames,
                        "spots.$spotId.artifacts[${artifact.id}].galleryImage references missing drawable: $galleryImage"
                    )
                }
            }
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

    @Serializable
    private data class Index(
        val routes: List<String>,
        val spots: List<String>
    )
}
