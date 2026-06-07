package com.mcis.memoir.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private var _PodcastIcon: ImageVector? = null

val UntitledIcons.PodcastIcon: ImageVector
    get() {
        if (_PodcastIcon != null) return _PodcastIcon!!
        _PodcastIcon = ImageVector.Builder(
            name = "PodcastIcon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Signal center shifted slightly for better balance
            val cx = 12f
            val cy = 10.5f

            // 1. Enlarged Center Dot
            path(fill = SolidColor(Color(0xFF000000))) {
                moveTo(cx, cy + 1.2f)
                curveTo(cx + 0.66f, cy + 1.2f, cx + 1.2f, cy + 0.66f, cx + 1.2f, cy)
                reflectiveCurveTo(cx + 0.66f, cy - 1.2f, cx, cy - 1.2f)
                reflectiveCurveTo(cx - 1.2f, cy - 0.66f, cx - 1.2f, cy)
                reflectiveCurveTo(cx - 0.66f, cy + 1.2f, cx, cy + 1.2f)
                close()
            }
            
            // 2. Enlarged Small Arc (Inner Signal)
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                // Radius 5.5 around (12, 10.5)
                // Start from approx 45 degrees below horizontal to create the "cut" look
                moveTo(8.11f, 14.39f)
                curveTo(7.1f, 13.38f, 6.5f, 12f, 6.5f, 10.5f)
                curveTo(6.5f, 7.46f, 8.96f, 5f, 12f, 5f)
                curveTo(15.04f, 5f, 17.5f, 7.46f, 17.5f, 10.5f)
                curveTo(17.5f, 12f, 16.9f, 13.38f, 15.89f, 14.39f)
            }

            // 3. Enlarged Big Arc (Outer Signal)
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                // Radius 9.5 around (12, 10.5)
                moveTo(5.28f, 17.22f)
                curveTo(3.55f, 15.49f, 2.5f, 13.12f, 2.5f, 10.5f)
                curveTo(2.5f, 5.25f, 6.75f, 1f, 12f, 1f)
                curveTo(17.25f, 1f, 21.5f, 5.25f, 21.5f, 10.5f)
                curveTo(21.5f, 13.12f, 20.45f, 15.49f, 18.72f, 17.22f)
            }

            // 4. Hollow Stretched Circle (The Pill - now relatively smaller)
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Centered at x=12, y from 16 to 23. Narrower width 3.5.
                moveTo(12f, 16f)
                curveTo(11.03f, 16f, 10.25f, 16.78f, 10.25f, 17.75f)
                verticalLineTo(21.25f)
                curveTo(10.25f, 22.22f, 11.03f, 23f, 12f, 23f)
                curveTo(12.97f, 23f, 13.75f, 22.22f, 13.75f, 21.25f)
                verticalLineTo(17.75f)
                curveTo(13.75f, 16.78f, 12.97f, 16f, 12f, 16f)
                close()
            }
        }.build()
        return _PodcastIcon!!
    }
