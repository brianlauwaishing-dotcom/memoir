package com.mcis.memoir.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private var _MapIcon: ImageVector? = null

val UntitledIcons.MapIcon: ImageVector
    get() {
        if (_MapIcon != null) return _MapIcon!!
        _MapIcon = ImageVector.Builder(
            name = "MapIcon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Pin head
                moveTo(21f, 10f)
                curveTo(21f, 17f, 12f, 23f, 12f, 23f)
                reflectiveCurveTo(3f, 17f, 3f, 10f)
                curveTo(3f, 5.03f, 7.03f, 1f, 12f, 1f)
                reflectiveCurveTo(21f, 5.03f, 21f, 10f)
                close()
                // Inner dot
                moveTo(12f, 13f)
                curveTo(13.66f, 13f, 15f, 11.66f, 15f, 10f)
                reflectiveCurveTo(13.66f, 7f, 12f, 7f)
                reflectiveCurveTo(9f, 8.34f, 9f, 10f)
                reflectiveCurveTo(10.34f, 13f, 12f, 13f)
                close()
            }
        }.build()
        return _MapIcon!!
    }
