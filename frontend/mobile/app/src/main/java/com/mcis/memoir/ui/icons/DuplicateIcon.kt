package com.mcis.memoir.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private var _DuplicateIcon: ImageVector? = null

val UntitledIcons.DuplicateIcon: ImageVector
    get() {
        if (_DuplicateIcon != null) return _DuplicateIcon!!
        _DuplicateIcon = ImageVector.Builder(
            name = "DuplicateIcon",
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
                // Background card
                moveTo(9f, 9f)
                verticalLineTo(5f)
                curveTo(9f, 3.895f, 9.895f, 3f, 11f, 3f)
                horizontalLineTo(19f)
                curveTo(20.105f, 3f, 21f, 3.895f, 21f, 5f)
                verticalLineTo(13f)
                curveTo(21f, 14.105f, 20.105f, 15f, 19f, 15f)
                horizontalLineTo(15f)
                
                // Foreground card
                moveTo(5f, 9f)
                horizontalLineTo(13f)
                curveTo(14.105f, 9f, 15f, 9.895f, 15f, 11f)
                verticalLineTo(19f)
                curveTo(15f, 20.105f, 14.105f, 21f, 13f, 21f)
                horizontalLineTo(5f)
                curveTo(3.895f, 21f, 3f, 20.105f, 3f, 19f)
                verticalLineTo(11f)
                curveTo(3f, 9.895f, 3.895f, 9f, 5f, 9f)
                close()
            }
        }.build()
        return _DuplicateIcon!!
    }
