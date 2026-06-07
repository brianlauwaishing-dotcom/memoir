package com.mcis.memoir.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private var _ShareIcon: ImageVector? = null

val UntitledIcons.ShareIcon: ImageVector
    get() {
        if (_ShareIcon != null) return _ShareIcon!!
        _ShareIcon = ImageVector.Builder(
            name = "ShareIcon",
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
                // Node 1: Top Right (Center: 18, 5)
                moveTo(21f, 5f)
                curveTo(21f, 6.66f, 19.66f, 8f, 18f, 8f)
                curveTo(16.34f, 8f, 15f, 6.66f, 15f, 5f)
                curveTo(15f, 3.34f, 16.34f, 2f, 18f, 2f)
                curveTo(19.66f, 2f, 21f, 3.34f, 21f, 5f)
                close()

                // Node 2: Center Left (Center: 6, 12)
                moveTo(9f, 12f)
                curveTo(9f, 13.66f, 7.66f, 15f, 6f, 15f)
                curveTo(4.34f, 15f, 3f, 13.66f, 3f, 12f)
                curveTo(3f, 10.34f, 4.34f, 9f, 6f, 9f)
                curveTo(7.66f, 9f, 9f, 10.34f, 9f, 12f)
                close()

                // Node 3: Bottom Right (Center: 18, 19)
                moveTo(21f, 19f)
                curveTo(21f, 20.66f, 19.66f, 22f, 18f, 22f)
                curveTo(16.34f, 22f, 15f, 20.66f, 15f, 19f)
                curveTo(15f, 17.34f, 16.34f, 16f, 18f, 16f)
                curveTo(19.66f, 16f, 21f, 17.34f, 21f, 19f)
                close()

                // Connecting Line 1: Center to Top
                moveTo(8.59f, 10.51f)
                lineTo(15.42f, 6.49f)

                // Connecting Line 2: Center to Bottom
                moveTo(8.59f, 13.49f)
                lineTo(15.42f, 17.51f)
            }
        }.build()
        return _ShareIcon!!
    }
