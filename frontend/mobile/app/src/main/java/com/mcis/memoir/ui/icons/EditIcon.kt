package com.mcis.memoir.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private var _EditIcon: ImageVector? = null

val UntitledIcons.EditIcon: ImageVector
    get() {
        if (_EditIcon != null) return _EditIcon!!
        _EditIcon = ImageVector.Builder(
            name = "EditIcon",
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
                moveTo(11f, 4f)
                horizontalLineTo(4f)
                curveTo(2.895f, 4f, 2f, 4.895f, 2f, 6f)
                verticalLineTo(20f)
                curveTo(2f, 21.105f, 2.895f, 22f, 4f, 22f)
                horizontalLineTo(18f)
                curveTo(19.105f, 22f, 20f, 21.105f, 20f, 20f)
                verticalLineTo(13f)
                
                moveTo(18.5f, 2.5f)
                curveTo(19.328f, 1.672f, 20.672f, 1.672f, 21.5f, 2.5f)
                curveTo(22.328f, 3.328f, 22.328f, 4.672f, 21.5f, 5.5f)
                lineTo(12f, 15f)
                lineTo(8f, 16f)
                lineTo(9f, 12f)
                lineTo(18.5f, 2.5f)
                close()
            }
        }.build()
        return _EditIcon!!
    }
