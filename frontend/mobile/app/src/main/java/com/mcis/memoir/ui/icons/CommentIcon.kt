package com.mcis.memoir.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private var _CommentIcon: ImageVector? = null

val UntitledIcons.CommentIcon: ImageVector
    get() {
        if (_CommentIcon != null) return _CommentIcon!!
        _CommentIcon = ImageVector.Builder(
            name = "CommentIcon",
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
                // A smooth, continuous speech bubble path
                moveTo(21f, 10f)
                curveTo(21f, 14.42f, 17.42f, 18f, 13f, 18f)
                curveTo(11.5f, 18f, 10.1f, 17.58f, 8.9f, 16.85f)
                lineTo(4f, 19f) // The tip of the tail
                lineTo(5.15f, 14.1f)
                curveTo(4.42f, 12.9f, 4f, 11.5f, 4f, 10f)
                curveTo(4f, 5.58f, 7.8f, 2f, 12.5f, 2f)
                curveTo(17.2f, 2f, 21f, 5.58f, 21f, 10f)
                close()
            }
        }.build()
        return _CommentIcon!!
    }
