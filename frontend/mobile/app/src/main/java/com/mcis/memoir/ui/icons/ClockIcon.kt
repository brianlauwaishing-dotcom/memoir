package com.mcis.memoir.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private var _ClockIcon: ImageVector? = null

val UntitledIcons.ClockIcon: ImageVector
    get() {
        if (_ClockIcon != null) return _ClockIcon!!
        _ClockIcon = ImageVector.Builder(
            name = "ClockIcon",
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
                // Circle
                moveTo(12f, 2f)
                curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
                reflectiveCurveTo(6.48f, 22f, 12f, 22f)
                reflectiveCurveTo(22f, 17.52f, 22f, 12f)
                reflectiveCurveTo(17.52f, 2f, 12f, 2f)
                close()
                // Hands
                moveTo(12f, 7f)
                verticalLineTo(12f)
                lineTo(15f, 15f)
            }
        }.build()
        return _ClockIcon!!
    }
