package com.mcis.memoir.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private var _RegenerateIcon: ImageVector? = null

val UntitledIcons.RegenerateIcon: ImageVector
    get() {
        if (_RegenerateIcon != null) return _RegenerateIcon!!
        _RegenerateIcon = ImageVector.Builder(
            name = "RegenerateIcon",
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
                moveTo(1f, 4f)
                verticalLineTo(10f)
                horizontalLineTo(7f)
                
                moveTo(23f, 20f)
                verticalLineTo(14f)
                horizontalLineTo(17f)
                
                moveTo(20.49f, 9f)
                curveTo(19.98f, 7.5f, 19.1f, 6.19f, 17.94f, 5.14f)
                curveTo(15.22f, 2.67f, 11.23f, 2.14f, 7.89f, 3.55f)
                lineTo(1f, 10f)
                
                moveTo(3.51f, 15f)
                curveTo(4.02f, 16.5f, 4.9f, 17.81f, 6.06f, 18.86f)
                curveTo(8.78f, 21.33f, 12.77f, 21.86f, 16.11f, 20.45f)
                lineTo(23f, 14f)
            }
        }.build()
        return _RegenerateIcon!!
    }
