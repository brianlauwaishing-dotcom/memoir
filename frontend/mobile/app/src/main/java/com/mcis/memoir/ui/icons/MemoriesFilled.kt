package com.mcis.memoir.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private var _MemoriesFilled: ImageVector? = null

val UntitledIcons.MemoriesFilled: ImageVector
    get() {
        if (_MemoriesFilled != null) return _MemoriesFilled!!
        _MemoriesFilled = ImageVector.Builder(
            name = "MemoriesFilled",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF000000))
            ) {
                moveTo(18f, 2f)
                horizontalLineTo(6f)
                curveTo(4.9f, 2f, 4f, 2.9f, 4f, 4f)
                verticalLineTo(20f)
                curveTo(4f, 21.1f, 4.9f, 22f, 6f, 22f)
                horizontalLineTo(18f)
                curveTo(19.1f, 22f, 20f, 21.1f, 20f, 20f)
                verticalLineTo(4f)
                curveTo(20f, 2.9f, 19.1f, 2f, 18f, 2f)
                close()
                // Spine line
                moveTo(7f, 4f)
                horizontalLineTo(8f)
                verticalLineTo(20f)
                horizontalLineTo(7f)
                verticalLineTo(4f)
                close()
            }
        }.build()
        return _MemoriesFilled!!
    }
