package com.mcis.memoir.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private var _BookmarkIcon: ImageVector? = null

val UntitledIcons.BookmarkIcon: ImageVector
    get() {
        if (_BookmarkIcon != null) return _BookmarkIcon!!
        _BookmarkIcon = ImageVector.Builder(
            name = "BookmarkIcon",
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
                moveTo(7f, 4f)
                horizontalLineTo(17f)
                curveTo(17.83f, 4f, 18.5f, 4.67f, 18.5f, 5.5f)
                verticalLineTo(20.5f)
                lineTo(12f, 17f)
                lineTo(5.5f, 20.5f)
                verticalLineTo(5.5f)
                curveTo(5.5f, 4.67f, 6.17f, 4f, 7f, 4f)
                close()
            }
        }.build()
        return _BookmarkIcon!!
    }
