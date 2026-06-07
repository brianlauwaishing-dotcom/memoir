package com.mcis.memoir.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private var _CloseIcon: ImageVector? = null

val UntitledIcons.CloseIcon: ImageVector
    get() {
        if (_CloseIcon != null) return _CloseIcon!!
        _CloseIcon = ImageVector.Builder(
            name = "CloseIcon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFFFFFFFF)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(18f, 6f)
                lineTo(6f, 18f)
                moveTo(6f, 6f)
                lineTo(18f, 18f)
            }
        }.build()
        return _CloseIcon!!
    }
