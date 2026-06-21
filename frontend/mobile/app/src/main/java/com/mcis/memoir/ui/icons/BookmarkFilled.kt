package com.mcis.memoir.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private var _BookmarkFilled: ImageVector? = null

val UntitledIcons.BookmarkFilled: ImageVector
    get() {
        if (_BookmarkFilled != null) return _BookmarkFilled!!
        _BookmarkFilled = ImageVector.Builder(
            name = "BookmarkFilled",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF000000))
            ) {
                moveTo(7f, 3f)
                horizontalLineTo(17f)
                curveTo(18.1f, 3f, 19f, 3.9f, 19f, 5f)
                verticalLineTo(21f)
                curveTo(19f, 21.36f, 18.62f, 21.59f, 18.3f, 21.42f)
                lineTo(12f, 18f)
                lineTo(5.7f, 21.42f)
                curveTo(5.38f, 21.59f, 5f, 21.36f, 5f, 21f)
                verticalLineTo(5f)
                curveTo(5f, 3.9f, 5.9f, 3f, 7f, 3f)
                close()
            }
        }.build()
        return _BookmarkFilled!!
    }
