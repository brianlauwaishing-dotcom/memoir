package com.mcis.memoir.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private var _LandmarkIcon: ImageVector? = null

val UntitledIcons.LandmarkIcon: ImageVector
    get() {
        if (_LandmarkIcon != null) return _LandmarkIcon!!
        _LandmarkIcon = ImageVector.Builder(
            name = "LandmarkIcon",
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
                moveTo(2f, 22f)
                horizontalLineTo(22f)
                moveTo(4f, 10f)
                verticalLineTo(19f)
                moveTo(8f, 10f)
                verticalLineTo(19f)
                moveTo(16f, 10f)
                verticalLineTo(19f)
                moveTo(20f, 10f)
                verticalLineTo(19f)
                moveTo(12f, 2f)
                lineTo(2f, 10f)
                horizontalLineTo(22f)
                lineTo(12f, 2f)
                close()
            }
        }.build()
        return _LandmarkIcon!!
    }
