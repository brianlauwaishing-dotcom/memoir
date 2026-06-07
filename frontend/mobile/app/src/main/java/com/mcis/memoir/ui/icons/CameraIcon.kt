package com.mcis.memoir.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private var _CameraIcon: ImageVector? = null

val UntitledIcons.CameraIcon: ImageVector
    get() {
        if (_CameraIcon != null) return _CameraIcon!!
        _CameraIcon = ImageVector.Builder(
            name = "CameraIcon",
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
                // Camera body
                moveTo(20f, 6f)
                horizontalLineTo(17f)
                lineTo(15.5f, 4f)
                horizontalLineTo(8.5f)
                lineTo(7f, 6f)
                horizontalLineTo(4f)
                curveTo(2.9f, 6f, 2f, 6.9f, 2f, 8f)
                verticalLineTo(18f)
                curveTo(2f, 19.1f, 2.9f, 20f, 4f, 20f)
                horizontalLineTo(20f)
                curveTo(21.1f, 20f, 22f, 19.1f, 22f, 18f)
                verticalLineTo(8f)
                curveTo(22f, 6.9f, 21.1f, 6f, 20f, 6f)
                close()
                // Lens
                moveTo(12f, 17f)
                curveTo(9.79f, 17f, 8f, 15.21f, 8f, 13f)
                reflectiveCurveTo(9.79f, 9f, 12f, 9f)
                reflectiveCurveTo(16f, 10.79f, 16f, 13f)
                reflectiveCurveTo(14.21f, 17f, 12f, 17f)
                close()
            }
        }.build()
        return _CameraIcon!!
    }
