package com.mcis.memoir.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private var _CopyIcon: ImageVector? = null

val UntitledIcons.CopyIcon: ImageVector
    get() {
        if (_CopyIcon != null) return _CopyIcon!!
        _CopyIcon = ImageVector.Builder(
            name = "CopyIcon",
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
                moveTo(16f, 4f)
                horizontalLineTo(6f)
                curveTo(4.9f, 4f, 4f, 4.9f, 4f, 6f)
                verticalLineTo(16f)
                
                moveTo(20f, 8f)
                horizontalLineTo(10f)
                curveTo(8.9f, 8f, 8f, 8.9f, 8f, 10f)
                verticalLineTo(20f)
                curveTo(8f, 21.1f, 8.9f, 22f, 10f, 22f)
                horizontalLineTo(20f)
                curveTo(21.1f, 22f, 22f, 21.1f, 22f, 20f)
                verticalLineTo(10f)
                curveTo(22f, 8.9f, 21.1f, 8f, 20f, 8f)
                close()
            }
        }.build()
        return _CopyIcon!!
    }
