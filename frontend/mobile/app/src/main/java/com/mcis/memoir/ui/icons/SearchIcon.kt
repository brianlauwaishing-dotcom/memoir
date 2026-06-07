package com.mcis.memoir.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private var _SearchIcon: ImageVector? = null

val UntitledIcons.SearchIcon: ImageVector
    get() {
        if (_SearchIcon != null) return _SearchIcon!!
        _SearchIcon = ImageVector.Builder(
            name = "SearchIcon",
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
                moveTo(11f, 19f)
                curveTo(15.4183f, 19f, 19f, 15.4183f, 19f, 11f)
                curveTo(19f, 6.5817f, 15.4183f, 3f, 11f, 3f)
                curveTo(6.5817f, 3f, 3f, 6.5817f, 3f, 11f)
                curveTo(3f, 15.4183f, 6.5817f, 19f, 11f, 19f)
                close()
                moveTo(21f, 21f)
                lineTo(16.65f, 16.65f)
            }
        }.build()
        return _SearchIcon!!
    }
