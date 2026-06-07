package com.mcis.memoir.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private var _FiltersIcon: ImageVector? = null
val UntitledIcons.FiltersIcon: ImageVector
    get() {
        if (_FiltersIcon != null) return _FiltersIcon!!
        _FiltersIcon = ImageVector.Builder(
            name = "FiltersIcon",
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
                moveTo(3f, 6f)
                horizontalLineTo(21f)
                moveTo(3f, 12f)
                horizontalLineTo(21f)
                moveTo(3f, 18f)
                horizontalLineTo(21f)
                // Filter knobs
                moveTo(7f, 4f)
                verticalLineTo(8f)
                moveTo(17f, 10f)
                verticalLineTo(14f)
                moveTo(11f, 16f)
                verticalLineTo(20f)
            }
        }.build()
        return _FiltersIcon!!
    }

private var _StampsIcon: ImageVector? = null
val UntitledIcons.StampsIcon: ImageVector
    get() {
        if (_StampsIcon != null) return _StampsIcon!!
        _StampsIcon = ImageVector.Builder(
            name = "StampsIcon",
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
                moveTo(12f, 2f)
                curveTo(12f, 2f, 10f, 2f, 9f, 4f)
                curveTo(8f, 6f, 8f, 8f, 8f, 8f)
                horizontalLineTo(16f)
                curveTo(16f, 8f, 16f, 6f, 15f, 4f)
                curveTo(14f, 2f, 12f, 2f, 12f, 2f)
                moveTo(6f, 8f)
                horizontalLineTo(18f)
                verticalLineTo(18f)
                curveTo(18f, 19.1f, 17.1f, 20f, 16f, 20f)
                horizontalLineTo(8f)
                curveTo(6.9f, 20f, 6f, 19.1f, 6f, 18f)
                verticalLineTo(8f)
                moveTo(6f, 21f)
                horizontalLineTo(18f)
            }
        }.build()
        return _StampsIcon!!
    }

private var _MoveIcon: ImageVector? = null
val UntitledIcons.MoveIcon: ImageVector
    get() {
        if (_MoveIcon != null) return _MoveIcon!!
        _MoveIcon = ImageVector.Builder(
            name = "MoveIcon",
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
                moveTo(12f, 2f)
                verticalLineTo(22f)
                moveTo(2f, 12f)
                horizontalLineTo(22f)
                // Arrows
                moveTo(12f, 2f)
                lineTo(9f, 5f)
                moveTo(12f, 2f)
                lineTo(15f, 5f)
                moveTo(12f, 22f)
                lineTo(9f, 19f)
                moveTo(12f, 22f)
                lineTo(15f, 19f)
                moveTo(2f, 12f)
                lineTo(5f, 9f)
                moveTo(2f, 12f)
                lineTo(5f, 15f)
                moveTo(22f, 12f)
                lineTo(19f, 9f)
                moveTo(22f, 12f)
                lineTo(19f, 15f)
            }
        }.build()
        return _MoveIcon!!
    }

private var _StickersIcon: ImageVector? = null
val UntitledIcons.StickersIcon: ImageVector
    get() {
        if (_StickersIcon != null) return _StickersIcon!!
        _StickersIcon = ImageVector.Builder(
            name = "StickersIcon",
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
                moveTo(12f, 2f)
                curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
                reflectiveCurveTo(6.48f, 22f, 12f, 22f)
                curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
                moveTo(12f, 2f)
                curveTo(12f, 2f, 18f, 2f, 22f, 8f)
                verticalLineTo(12f)
                moveTo(15f, 5f)
                lineTo(21f, 11f)
            }
        }.build()
        return _StickersIcon!!
    }

private var _FontsIcon: ImageVector? = null
val UntitledIcons.FontsIcon: ImageVector
    get() {
        if (_FontsIcon != null) return _FontsIcon!!
        _FontsIcon = ImageVector.Builder(
            name = "FontsIcon",
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
                moveTo(5f, 20f)
                lineTo(12f, 4f)
                lineTo(19f, 20f)
                moveTo(8f, 14f)
                horizontalLineTo(16f)
            }
        }.build()
        return _FontsIcon!!
    }
