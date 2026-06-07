package com.mcis.memoir.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private var _MoreIcon: ImageVector? = null

val UntitledIcons.MoreIcon: ImageVector
    get() {
        if (_MoreIcon != null) return _MoreIcon!!
        _MoreIcon = ImageVector.Builder(
            name = "MoreIcon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color(0xFF000000))) {
                moveTo(12f, 8f)
                curveTo(13.1f, 8f, 14f, 7.1f, 14f, 6f)
                reflectiveCurveTo(13.1f, 4f, 12f, 4f)
                reflectiveCurveTo(10f, 4.9f, 10f, 6f)
                reflectiveCurveTo(10.9f, 8f, 12f, 8f)
                close()
                
                moveTo(12f, 14f)
                curveTo(13.1f, 14f, 14f, 13.1f, 14f, 12f)
                reflectiveCurveTo(13.1f, 10f, 12f, 10f)
                reflectiveCurveTo(10f, 10.9f, 10f, 12f)
                reflectiveCurveTo(10.9f, 14f, 12f, 14f)
                close()
                
                moveTo(12f, 20f)
                curveTo(13.1f, 20f, 14f, 19.1f, 14f, 18f)
                reflectiveCurveTo(13.1f, 16f, 12f, 16f)
                reflectiveCurveTo(10f, 16.9f, 10f, 18f)
                reflectiveCurveTo(10.9f, 20f, 12f, 20f)
                close()
            }
        }.build()
        return _MoreIcon!!
    }
