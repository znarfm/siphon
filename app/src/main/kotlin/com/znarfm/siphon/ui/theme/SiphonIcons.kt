package com.znarfm.siphon.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object SiphonIcons {
    private var _add: ImageVector? = null
    val Add: ImageVector
        get() {
            if (_add != null) return _add!!
            _add = ImageVector.Builder(name = "Add", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(18.0f, 13.0f)
                    horizontalLineToRelative(-5.0f)
                    verticalLineToRelative(5.0f)
                    curveToRelative(0.0f, 0.55f, -0.45f, 1.0f, -1.0f, 1.0f)
                    reflectiveCurveToRelative(-1.0f, -0.45f, -1.0f, -1.0f)
                    verticalLineToRelative(-5.0f)
                    horizontalLineTo(6.0f)
                    curveToRelative(-0.55f, 0.0f, -1.0f, -0.45f, -1.0f, -1.0f)
                    reflectiveCurveToRelative(0.45f, -1.0f, 1.0f, -1.0f)
                    horizontalLineToRelative(5.0f)
                    verticalLineTo(6.0f)
                    curveToRelative(0.0f, -0.55f, 0.45f, -1.0f, 1.0f, -1.0f)
                    reflectiveCurveToRelative(1.0f, 0.45f, 1.0f, 1.0f)
                    verticalLineToRelative(5.0f)
                    horizontalLineToRelative(5.0f)
                    curveToRelative(0.55f, 0.0f, 1.0f, 0.45f, 1.0f, 1.0f)
                    reflectiveCurveToRelative(-0.45f, 1.0f, -1.0f, 1.0f)
                    close()
                }
            }.build()
            return _add!!
        }

    private var _arrowBack: ImageVector? = null
    val ArrowBack: ImageVector
        get() {
            if (_arrowBack != null) return _arrowBack!!
            _arrowBack = ImageVector.Builder(name = "ArrowBack", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f, autoMirror = true).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(19.0f, 11.0f)
                    horizontalLineTo(7.83f)
                    lineToRelative(4.88f, -4.88f)
                    curveToRelative(0.39f, -0.39f, 0.39f, -1.03f, 0.0f, -1.42f)
                    arcToRelative(0.996f, 0.996f, 0.0f, false, false, -1.41f, 0.0f)
                    lineToRelative(-6.59f, 6.59f)
                    arcToRelative(0.996f, 0.996f, 0.0f, false, false, 0.0f, 1.41f)
                    lineToRelative(6.59f, 6.59f)
                    arcToRelative(0.996f, 0.996f, 0.0f, true, false, 1.41f, -1.41f)
                    lineTo(7.83f, 13.0f)
                    horizontalLineTo(19.0f)
                    curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                    reflectiveCurveToRelative(-0.45f, -1.0f, -1.0f, -1.0f)
                    close()
                }
            }.build()
            return _arrowBack!!
        }

    private var _arrowDropDown: ImageVector? = null
    val ArrowDropDown: ImageVector
        get() {
            if (_arrowDropDown != null) return _arrowDropDown!!
            _arrowDropDown = ImageVector.Builder(name = "ArrowDropDown", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveToRelative(8.71f, 11.71f)
                    lineToRelative(2.59f, 2.59f)
                    curveToRelative(0.39f, 0.39f, 1.02f, 0.39f, 1.41f, 0.0f)
                    lineToRelative(2.59f, -2.59f)
                    curveToRelative(0.63f, -0.63f, 0.18f, -1.71f, -0.71f, -1.71f)
                    horizontalLineTo(9.41f)
                    curveToRelative(-0.89f, 0.0f, -1.33f, 1.08f, -0.7f, 1.71f)
                    close()
                }
            }.build()
            return _arrowDropDown!!
        }

    private var _arrowForward: ImageVector? = null
    val ArrowForward: ImageVector
        get() {
            if (_arrowForward != null) return _arrowForward!!
            _arrowForward = ImageVector.Builder(name = "ArrowForward", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f, autoMirror = true).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(5.0f, 13.0f)
                    horizontalLineToRelative(11.17f)
                    lineToRelative(-4.88f, 4.88f)
                    curveToRelative(-0.39f, 0.39f, -0.39f, 1.03f, 0.0f, 1.42f)
                    curveToRelative(0.39f, 0.39f, 1.02f, 0.39f, 1.41f, 0.0f)
                    lineToRelative(6.59f, -6.59f)
                    arcToRelative(0.996f, 0.996f, 0.0f, false, false, 0.0f, -1.41f)
                    lineToRelative(-6.58f, -6.6f)
                    arcToRelative(0.996f, 0.996f, 0.0f, true, false, -1.41f, 1.41f)
                    lineTo(16.17f, 11.0f)
                    horizontalLineTo(5.0f)
                    curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                    reflectiveCurveToRelative(0.45f, 1.0f, 1.0f, 1.0f)
                    close()
                }
            }.build()
            return _arrowForward!!
        }

    private var _audioFile: ImageVector? = null
    val AudioFile: ImageVector
        get() {
            if (_audioFile != null) return _audioFile!!
            _audioFile = ImageVector.Builder(name = "AudioFile", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveToRelative(19.41f, 7.41f)
                    lineToRelative(-4.83f, -4.83f)
                    curveToRelative(-0.37f, -0.37f, -0.88f, -0.58f, -1.41f, -0.58f)
                    horizontalLineTo(6.0f)
                    curveToRelative(-1.1f, 0.0f, -1.99f, 0.9f, -1.99f, 2.0f)
                    lineTo(4.0f, 20.0f)
                    curveToRelative(0.0f, 1.1f, 0.89f, 2.0f, 1.99f, 2.0f)
                    horizontalLineTo(18.0f)
                    curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                    verticalLineTo(8.83f)
                    curveToRelative(0.0f, -0.53f, -0.21f, -1.04f, -0.59f, -1.42f)
                    close()
                    moveTo(15.0f, 13.0f)
                    horizontalLineToRelative(-2.0f)
                    verticalLineToRelative(3.61f)
                    curveToRelative(0.0f, 1.28f, -1.0f, 2.41f, -2.28f, 2.39f)
                    arcToRelative(2.259f, 2.259f, 0.0f, false, true, -2.13f, -2.91f)
                    curveToRelative(0.21f, -0.72f, 0.8f, -1.31f, 1.53f, -1.51f)
                    curveToRelative(0.7f, -0.19f, 1.36f, -0.05f, 1.88f, 0.29f)
                    verticalLineTo(12.0f)
                    curveToRelative(0.0f, -0.55f, 0.45f, -1.0f, 1.0f, -1.0f)
                    horizontalLineToRelative(2.0f)
                    curveToRelative(0.55f, 0.0f, 1.0f, 0.45f, 1.0f, 1.0f)
                    reflectiveCurveToRelative(-0.45f, 1.0f, -1.0f, 1.0f)
                    close()
                    moveToRelative(-1.0f, -4.0f)
                    curveToRelative(-0.55f, 0.0f, -1.0f, -0.45f, -1.0f, -1.0f)
                    verticalLineTo(3.5f)
                    lineTo(18.5f, 9.0f)
                    horizontalLineTo(14.0f)
                    close()
                }
            }.build()
            return _audioFile!!
        }

    private var _bolt: ImageVector? = null
    val Bolt: ImageVector
        get() {
            if (_bolt != null) return _bolt!!
            _bolt = ImageVector.Builder(name = "Bolt", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(10.67f, 21.0f)
                    curveToRelative(-0.35f, 0.0f, -0.62f, -0.31f, -0.57f, -0.66f)
                    lineTo(11.0f, 14.0f)
                    horizontalLineTo(7.5f)
                    curveToRelative(-0.88f, 0.0f, -0.33f, -0.75f, -0.31f, -0.78f)
                    curveToRelative(1.26f, -2.23f, 3.15f, -5.53f, 5.65f, -9.93f)
                    arcToRelative(0.577f, 0.577f, 0.0f, false, true, 1.07f, 0.37f)
                    lineToRelative(-0.9f, 6.34f)
                    horizontalLineToRelative(3.51f)
                    curveToRelative(0.4f, 0.0f, 0.62f, 0.19f, 0.4f, 0.66f)
                    curveToRelative(-3.29f, 5.74f, -5.2f, 9.09f, -5.75f, 10.05f)
                    curveToRelative(-0.1f, 0.18f, -0.29f, 0.29f, -0.5f, 0.29f)
                    close()
                }
            }.build()
            return _bolt!!
        }

    private var _bookmarkBorder: ImageVector? = null
    val BookmarkBorder: ImageVector
        get() {
            if (_bookmarkBorder != null) return _bookmarkBorder!!
            _bookmarkBorder = ImageVector.Builder(name = "BookmarkBorder", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(17.0f, 3.0f)
                    horizontalLineTo(7.0f)
                    curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                    verticalLineToRelative(16.0f)
                    lineToRelative(7.0f, -3.0f)
                    lineToRelative(7.0f, 3.0f)
                    verticalLineTo(5.0f)
                    curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                    close()
                    moveToRelative(0.0f, 15.0f)
                    lineToRelative(-5.0f, -2.18f)
                    lineTo(7.0f, 18.0f)
                    verticalLineTo(6.0f)
                    curveToRelative(0.0f, -0.55f, 0.45f, -1.0f, 1.0f, -1.0f)
                    horizontalLineToRelative(8.0f)
                    curveToRelative(0.55f, 0.0f, 1.0f, 0.45f, 1.0f, 1.0f)
                    verticalLineToRelative(12.0f)
                    close()
                }
            }.build()
            return _bookmarkBorder!!
        }

    private var _bookmarks: ImageVector? = null
    val Bookmarks: ImageVector
        get() {
            if (_bookmarks != null) return _bookmarks!!
            _bookmarks = ImageVector.Builder(name = "Bookmarks", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveToRelative(19.0f, 18.0f)
                    lineToRelative(2.0f, 1.0f)
                    verticalLineTo(3.0f)
                    curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                    horizontalLineTo(8.99f)
                    curveTo(7.89f, 1.0f, 7.0f, 1.9f, 7.0f, 3.0f)
                    horizontalLineToRelative(10.0f)
                    curveToRelative(1.1f, 0.0f, 2.0f, 0.9f, 2.0f, 2.0f)
                    verticalLineToRelative(13.0f)
                    close()
                    moveTo(15.0f, 5.0f)
                    horizontalLineTo(5.0f)
                    curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                    verticalLineToRelative(16.0f)
                    lineToRelative(7.0f, -3.0f)
                    lineToRelative(7.0f, 3.0f)
                    verticalLineTo(7.0f)
                    curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                    close()
                }
            }.build()
            return _bookmarks!!
        }

    private var _cancel: ImageVector? = null
    val Cancel: ImageVector
        get() {
            if (_cancel != null) return _cancel!!
            _cancel = ImageVector.Builder(name = "Cancel", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(12.0f, 2.0f)
                    curveTo(6.47f, 2.0f, 2.0f, 6.47f, 2.0f, 12.0f)
                    reflectiveCurveToRelative(4.47f, 10.0f, 10.0f, 10.0f)
                    reflectiveCurveToRelative(10.0f, -4.47f, 10.0f, -10.0f)
                    reflectiveCurveTo(17.53f, 2.0f, 12.0f, 2.0f)
                    close()
                    moveToRelative(4.3f, 14.3f)
                    arcToRelative(0.996f, 0.996f, 0.0f, false, true, -1.41f, 0.0f)
                    lineTo(12.0f, 13.41f)
                    lineTo(9.11f, 16.3f)
                    arcToRelative(0.996f, 0.996f, 0.0f, true, true, -1.41f, -1.41f)
                    lineTo(10.59f, 12.0f)
                    lineTo(7.7f, 9.11f)
                    arcTo(0.996f, 0.996f, 0.0f, true, true, 9.11f, 7.7f)
                    lineTo(12.0f, 10.59f)
                    lineToRelative(2.89f, -2.89f)
                    arcToRelative(0.996f, 0.996f, 0.0f, true, true, 1.41f, 1.41f)
                    lineTo(13.41f, 12.0f)
                    lineToRelative(2.89f, 2.89f)
                    curveToRelative(0.38f, 0.38f, 0.38f, 1.02f, 0.0f, 1.41f)
                    close()
                }
            }.build()
            return _cancel!!
        }

    private var _check: ImageVector? = null
    val Check: ImageVector
        get() {
            if (_check != null) return _check!!
            _check = ImageVector.Builder(name = "Check", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(9.0f, 16.17f)
                    lineTo(5.53f, 12.7f)
                    arcToRelative(0.996f, 0.996f, 0.0f, true, false, -1.41f, 1.41f)
                    lineToRelative(4.18f, 4.18f)
                    curveToRelative(0.39f, 0.39f, 1.02f, 0.39f, 1.41f, 0.0f)
                    lineTo(20.29f, 7.71f)
                    arcToRelative(0.996f, 0.996f, 0.0f, true, false, -1.41f, -1.41f)
                    lineTo(9.0f, 16.17f)
                    close()
                }
            }.build()
            return _check!!
        }

    private var _checkCircle: ImageVector? = null
    val CheckCircle: ImageVector
        get() {
            if (_checkCircle != null) return _checkCircle!!
            _checkCircle = ImageVector.Builder(name = "CheckCircle", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(12.0f, 2.0f)
                    curveTo(6.48f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f)
                    reflectiveCurveToRelative(4.48f, 10.0f, 10.0f, 10.0f)
                    reflectiveCurveToRelative(10.0f, -4.48f, 10.0f, -10.0f)
                    reflectiveCurveTo(17.52f, 2.0f, 12.0f, 2.0f)
                    close()
                    moveTo(9.29f, 16.29f)
                    lineTo(5.7f, 12.7f)
                    arcToRelative(0.996f, 0.996f, 0.0f, true, true, 1.41f, -1.41f)
                    lineTo(10.0f, 14.17f)
                    lineToRelative(6.88f, -6.88f)
                    arcToRelative(0.996f, 0.996f, 0.0f, true, true, 1.41f, 1.41f)
                    lineToRelative(-7.59f, 7.59f)
                    arcToRelative(0.996f, 0.996f, 0.0f, false, true, -1.41f, 0.0f)
                    close()
                }
            }.build()
            return _checkCircle!!
        }

    private var _clearAll: ImageVector? = null
    val ClearAll: ImageVector
        get() {
            if (_clearAll != null) return _clearAll!!
            _clearAll = ImageVector.Builder(name = "ClearAll", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(6.0f, 13.0f)
                    horizontalLineToRelative(12.0f)
                    curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                    reflectiveCurveToRelative(-0.45f, -1.0f, -1.0f, -1.0f)
                    horizontalLineTo(6.0f)
                    curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                    reflectiveCurveToRelative(0.45f, 1.0f, 1.0f, 1.0f)
                    close()
                    moveToRelative(-2.0f, 4.0f)
                    horizontalLineToRelative(12.0f)
                    curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                    reflectiveCurveToRelative(-0.45f, -1.0f, -1.0f, -1.0f)
                    horizontalLineTo(4.0f)
                    curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                    reflectiveCurveToRelative(0.45f, 1.0f, 1.0f, 1.0f)
                    close()
                    moveToRelative(3.0f, -9.0f)
                    curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                    horizontalLineToRelative(12.0f)
                    curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                    reflectiveCurveToRelative(-0.45f, -1.0f, -1.0f, -1.0f)
                    horizontalLineTo(8.0f)
                    curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                    close()
                }
            }.build()
            return _clearAll!!
        }

    private var _close: ImageVector? = null
    val Close: ImageVector
        get() {
            if (_close != null) return _close!!
            _close = ImageVector.Builder(name = "Close", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(18.3f, 5.71f)
                    arcToRelative(0.996f, 0.996f, 0.0f, false, false, -1.41f, 0.0f)
                    lineTo(12.0f, 10.59f)
                    lineTo(7.11f, 5.7f)
                    arcTo(0.996f, 0.996f, 0.0f, true, false, 5.7f, 7.11f)
                    lineTo(10.59f, 12.0f)
                    lineTo(5.7f, 16.89f)
                    arcToRelative(0.996f, 0.996f, 0.0f, true, false, 1.41f, 1.41f)
                    lineTo(12.0f, 13.41f)
                    lineToRelative(4.89f, 4.89f)
                    arcToRelative(0.996f, 0.996f, 0.0f, true, false, 1.41f, -1.41f)
                    lineTo(13.41f, 12.0f)
                    lineToRelative(4.89f, -4.89f)
                    curveToRelative(0.38f, -0.38f, 0.38f, -1.02f, 0.0f, -1.4f)
                    close()
                }
            }.build()
            return _close!!
        }

    private var _contentCopy: ImageVector? = null
    val ContentCopy: ImageVector
        get() {
            if (_contentCopy != null) return _contentCopy!!
            _contentCopy = ImageVector.Builder(name = "ContentCopy", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(15.0f, 20.0f)
                    horizontalLineTo(5.0f)
                    verticalLineTo(7.0f)
                    curveToRelative(0.0f, -0.55f, -0.45f, -1.0f, -1.0f, -1.0f)
                    reflectiveCurveToRelative(-1.0f, 0.45f, -1.0f, 1.0f)
                    verticalLineToRelative(13.0f)
                    curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                    horizontalLineToRelative(10.0f)
                    curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                    reflectiveCurveToRelative(-0.45f, -1.0f, -1.0f, -1.0f)
                    close()
                    moveToRelative(5.0f, -4.0f)
                    verticalLineTo(4.0f)
                    curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                    horizontalLineTo(9.0f)
                    curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                    verticalLineToRelative(12.0f)
                    curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                    horizontalLineToRelative(9.0f)
                    curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                    close()
                    moveToRelative(-2.0f, 0.0f)
                    horizontalLineTo(9.0f)
                    verticalLineTo(4.0f)
                    horizontalLineToRelative(9.0f)
                    verticalLineToRelative(12.0f)
                    close()
                }
            }.build()
            return _contentCopy!!
        }

    private var _contentPaste: ImageVector? = null
    val ContentPaste: ImageVector
        get() {
            if (_contentPaste != null) return _contentPaste!!
            _contentPaste = ImageVector.Builder(name = "ContentPaste", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(19.0f, 2.0f)
                    horizontalLineToRelative(-4.18f)
                    curveTo(14.4f, 0.84f, 13.3f, 0.0f, 12.0f, 0.0f)
                    reflectiveCurveTo(9.6f, 0.84f, 9.18f, 2.0f)
                    horizontalLineTo(5.0f)
                    curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                    verticalLineToRelative(16.0f)
                    curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                    horizontalLineToRelative(14.0f)
                    curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                    verticalLineTo(4.0f)
                    curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                    close()
                    moveToRelative(-7.0f, 0.0f)
                    curveToRelative(0.55f, 0.0f, 1.0f, 0.45f, 1.0f, 1.0f)
                    reflectiveCurveToRelative(-0.45f, 1.0f, -1.0f, 1.0f)
                    reflectiveCurveToRelative(-1.0f, -0.45f, -1.0f, -1.0f)
                    reflectiveCurveToRelative(0.45f, -1.0f, 1.0f, -1.0f)
                    close()
                    moveToRelative(6.0f, 18.0f)
                    horizontalLineTo(6.0f)
                    curveToRelative(-0.55f, 0.0f, -1.0f, -0.45f, -1.0f, -1.0f)
                    verticalLineTo(5.0f)
                    curveToRelative(0.0f, -0.55f, 0.45f, -1.0f, 1.0f, -1.0f)
                    horizontalLineToRelative(1.0f)
                    verticalLineToRelative(1.0f)
                    curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                    horizontalLineToRelative(6.0f)
                    curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                    verticalLineTo(4.0f)
                    horizontalLineToRelative(1.0f)
                    curveToRelative(0.55f, 0.0f, 1.0f, 0.45f, 1.0f, 1.0f)
                    verticalLineToRelative(14.0f)
                    curveToRelative(0.0f, 0.55f, -0.45f, 1.0f, -1.0f, 1.0f)
                    close()
                }
            }.build()
            return _contentPaste!!
        }

    private var _darkMode: ImageVector? = null
    val DarkMode: ImageVector
        get() {
            if (_darkMode != null) return _darkMode!!
            _darkMode = ImageVector.Builder(name = "DarkMode", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(11.01f, 3.05f)
                    curveTo(6.51f, 3.54f, 3.0f, 7.36f, 3.0f, 12.0f)
                    arcToRelative(9.0f, 9.0f, 0.0f, false, false, 9.0f, 9.0f)
                    curveToRelative(4.63f, 0.0f, 8.45f, -3.5f, 8.95f, -8.0f)
                    curveToRelative(0.09f, -0.79f, -0.78f, -1.42f, -1.54f, -0.95f)
                    arcTo(5.403f, 5.403f, 0.0f, false, true, 11.1f, 7.5f)
                    curveToRelative(0.0f, -1.06f, 0.31f, -2.06f, 0.84f, -2.89f)
                    curveToRelative(0.45f, -0.67f, -0.04f, -1.63f, -0.93f, -1.56f)
                    close()
                }
            }.build()
            return _darkMode!!
        }

    private var _delete: ImageVector? = null
    val Delete: ImageVector
        get() {
            if (_delete != null) return _delete!!
            _delete = ImageVector.Builder(name = "Delete", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(6.0f, 19.0f)
                    curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                    horizontalLineToRelative(8.0f)
                    curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                    verticalLineTo(9.0f)
                    curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                    horizontalLineTo(8.0f)
                    curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                    verticalLineToRelative(10.0f)
                    close()
                    moveTo(18.0f, 4.0f)
                    horizontalLineToRelative(-2.5f)
                    lineToRelative(-0.71f, -0.71f)
                    curveToRelative(-0.18f, -0.18f, -0.44f, -0.29f, -0.7f, -0.29f)
                    horizontalLineTo(9.91f)
                    curveToRelative(-0.26f, 0.0f, -0.52f, 0.11f, -0.7f, 0.29f)
                    lineTo(8.5f, 4.0f)
                    horizontalLineTo(6.0f)
                    curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                    reflectiveCurveToRelative(0.45f, 1.0f, 1.0f, 1.0f)
                    horizontalLineToRelative(12.0f)
                    curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                    reflectiveCurveToRelative(-0.45f, -1.0f, -1.0f, -1.0f)
                    close()
                }
            }.build()
            return _delete!!
        }

    private var _deleteOutline: ImageVector? = null
    val DeleteOutline: ImageVector
        get() {
            if (_deleteOutline != null) return _deleteOutline!!
            _deleteOutline = ImageVector.Builder(name = "DeleteOutline", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(6.0f, 19.0f)
                    curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                    horizontalLineToRelative(8.0f)
                    curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                    verticalLineTo(9.0f)
                    curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                    horizontalLineTo(8.0f)
                    curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                    verticalLineToRelative(10.0f)
                    close()
                    moveTo(9.0f, 9.0f)
                    horizontalLineToRelative(6.0f)
                    curveToRelative(0.55f, 0.0f, 1.0f, 0.45f, 1.0f, 1.0f)
                    verticalLineToRelative(8.0f)
                    curveToRelative(0.0f, 0.55f, -0.45f, 1.0f, -1.0f, 1.0f)
                    horizontalLineTo(9.0f)
                    curveToRelative(-0.55f, 0.0f, -1.0f, -0.45f, -1.0f, -1.0f)
                    verticalLineToRelative(-8.0f)
                    curveToRelative(0.0f, -0.55f, 0.45f, -1.0f, 1.0f, -1.0f)
                    close()
                    moveToRelative(6.5f, -5.0f)
                    lineToRelative(-0.71f, -0.71f)
                    curveToRelative(-0.18f, -0.18f, -0.44f, -0.29f, -0.7f, -0.29f)
                    horizontalLineTo(9.91f)
                    curveToRelative(-0.26f, 0.0f, -0.52f, 0.11f, -0.7f, 0.29f)
                    lineTo(8.5f, 4.0f)
                    horizontalLineTo(6.0f)
                    curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                    reflectiveCurveToRelative(0.45f, 1.0f, 1.0f, 1.0f)
                    horizontalLineToRelative(12.0f)
                    curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                    reflectiveCurveToRelative(-0.45f, -1.0f, -1.0f, -1.0f)
                    horizontalLineToRelative(-2.5f)
                    close()
                }
            }.build()
            return _deleteOutline!!
        }

    private var _deleteSweep: ImageVector? = null
    val DeleteSweep: ImageVector
        get() {
            if (_deleteSweep != null) return _deleteSweep!!
            _deleteSweep = ImageVector.Builder(name = "DeleteSweep", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(16.0f, 16.0f)
                    horizontalLineToRelative(2.0f)
                    curveToRelative(0.55f, 0.0f, 1.0f, 0.45f, 1.0f, 1.0f)
                    reflectiveCurveToRelative(-0.45f, 1.0f, -1.0f, 1.0f)
                    horizontalLineToRelative(-2.0f)
                    curveToRelative(-0.55f, 0.0f, -1.0f, -0.45f, -1.0f, -1.0f)
                    reflectiveCurveToRelative(0.45f, -1.0f, 1.0f, -1.0f)
                    close()
                    moveToRelative(0.0f, -8.0f)
                    horizontalLineToRelative(5.0f)
                    curveToRelative(0.55f, 0.0f, 1.0f, 0.45f, 1.0f, 1.0f)
                    reflectiveCurveToRelative(-0.45f, 1.0f, -1.0f, 1.0f)
                    horizontalLineToRelative(-5.0f)
                    curveToRelative(-0.55f, 0.0f, -1.0f, -0.45f, -1.0f, -1.0f)
                    reflectiveCurveToRelative(0.45f, -1.0f, 1.0f, -1.0f)
                    close()
                    moveToRelative(0.0f, 4.0f)
                    horizontalLineToRelative(4.0f)
                    curveToRelative(0.55f, 0.0f, 1.0f, 0.45f, 1.0f, 1.0f)
                    reflectiveCurveToRelative(-0.45f, 1.0f, -1.0f, 1.0f)
                    horizontalLineToRelative(-4.0f)
                    curveToRelative(-0.55f, 0.0f, -1.0f, -0.45f, -1.0f, -1.0f)
                    reflectiveCurveToRelative(0.45f, -1.0f, 1.0f, -1.0f)
                    close()
                    moveTo(3.0f, 18.0f)
                    curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                    horizontalLineToRelative(6.0f)
                    curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                    verticalLineTo(8.0f)
                    horizontalLineTo(3.0f)
                    verticalLineToRelative(10.0f)
                    close()
                    moveTo(13.0f, 5.0f)
                    horizontalLineToRelative(-2.0f)
                    lineToRelative(-0.71f, -0.71f)
                    curveToRelative(-0.18f, -0.18f, -0.44f, -0.29f, -0.7f, -0.29f)
                    horizontalLineTo(6.41f)
                    curveToRelative(-0.26f, 0.0f, -0.52f, 0.11f, -0.7f, 0.29f)
                    lineTo(5.0f, 5.0f)
                    horizontalLineTo(3.0f)
                    curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                    reflectiveCurveToRelative(0.45f, 1.0f, 1.0f, 1.0f)
                    horizontalLineToRelative(10.0f)
                    curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                    reflectiveCurveToRelative(-0.45f, -1.0f, -1.0f, -1.0f)
                    close()
                }
            }.build()
            return _deleteSweep!!
        }

    private var _deselect: ImageVector? = null
    val Deselect: ImageVector
        get() {
            if (_deselect != null) return _deselect!!
            _deselect = ImageVector.Builder(name = "Deselect", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(3.0f, 13.0f)
                    horizontalLineToRelative(2.0f)
                    verticalLineToRelative(-2.0f)
                    horizontalLineTo(3.0f)
                    verticalLineToRelative(2.0f)
                    close()
                    moveToRelative(4.0f, 8.0f)
                    horizontalLineToRelative(2.0f)
                    verticalLineToRelative(-2.0f)
                    horizontalLineTo(7.0f)
                    verticalLineToRelative(2.0f)
                    close()
                    moveToRelative(6.0f, -18.0f)
                    horizontalLineToRelative(-2.0f)
                    verticalLineToRelative(2.0f)
                    horizontalLineToRelative(2.0f)
                    verticalLineTo(3.0f)
                    close()
                    moveToRelative(6.0f, 0.0f)
                    verticalLineToRelative(2.0f)
                    horizontalLineToRelative(2.0f)
                    curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                    close()
                    moveTo(5.0f, 21.0f)
                    verticalLineToRelative(-2.0f)
                    horizontalLineTo(3.0f)
                    curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                    close()
                    moveToRelative(-2.0f, -4.0f)
                    horizontalLineToRelative(2.0f)
                    verticalLineToRelative(-2.0f)
                    horizontalLineTo(3.0f)
                    verticalLineToRelative(2.0f)
                    close()
                    moveToRelative(8.0f, 4.0f)
                    horizontalLineToRelative(2.0f)
                    verticalLineToRelative(-2.0f)
                    horizontalLineToRelative(-2.0f)
                    verticalLineToRelative(2.0f)
                    close()
                    moveToRelative(8.0f, -8.0f)
                    horizontalLineToRelative(2.0f)
                    verticalLineToRelative(-2.0f)
                    horizontalLineToRelative(-2.0f)
                    verticalLineToRelative(2.0f)
                    close()
                    moveToRelative(0.0f, -4.0f)
                    horizontalLineToRelative(2.0f)
                    verticalLineTo(7.0f)
                    horizontalLineToRelative(-2.0f)
                    verticalLineToRelative(2.0f)
                    close()
                    moveToRelative(-4.0f, -4.0f)
                    horizontalLineToRelative(2.0f)
                    verticalLineTo(3.0f)
                    horizontalLineToRelative(-2.0f)
                    verticalLineToRelative(2.0f)
                    close()
                    moveTo(7.83f, 5.0f)
                    lineTo(7.0f, 4.17f)
                    verticalLineTo(3.0f)
                    horizontalLineToRelative(2.0f)
                    verticalLineToRelative(2.0f)
                    horizontalLineTo(7.83f)
                    close()
                    moveToRelative(12.0f, 12.0f)
                    lineToRelative(-0.83f, -0.83f)
                    verticalLineTo(15.0f)
                    horizontalLineToRelative(2.0f)
                    verticalLineToRelative(2.0f)
                    horizontalLineToRelative(-1.17f)
                    close()
                    moveTo(9.0f, 15.0f)
                    verticalLineToRelative(-3.17f)
                    lineTo(12.17f, 15.0f)
                    horizontalLineTo(9.0f)
                    close()
                    moveTo(2.1f, 3.51f)
                    arcToRelative(0.996f, 0.996f, 0.0f, false, false, 0.0f, 1.41f)
                    lineTo(4.17f, 7.0f)
                    horizontalLineTo(3.0f)
                    verticalLineToRelative(2.0f)
                    horizontalLineToRelative(2.0f)
                    verticalLineTo(7.83f)
                    lineToRelative(2.0f, 2.0f)
                    verticalLineTo(16.0f)
                    curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                    horizontalLineToRelative(6.17f)
                    lineToRelative(2.0f, 2.0f)
                    horizontalLineTo(15.0f)
                    verticalLineToRelative(2.0f)
                    horizontalLineToRelative(2.0f)
                    verticalLineToRelative(-1.17f)
                    lineToRelative(2.07f, 2.07f)
                    arcToRelative(0.996f, 0.996f, 0.0f, true, false, 1.41f, -1.41f)
                    lineTo(3.51f, 3.51f)
                    arcToRelative(0.996f, 0.996f, 0.0f, false, false, -1.41f, 0.0f)
                    close()
                    moveTo(17.0f, 8.0f)
                    curveToRelative(0.0f, -0.55f, -0.45f, -1.0f, -1.0f, -1.0f)
                    horizontalLineTo(9.83f)
                    lineToRelative(2.0f, 2.0f)
                    horizontalLineTo(15.0f)
                    verticalLineToRelative(3.17f)
                    lineToRelative(2.0f, 2.0f)
                    verticalLineTo(8.0f)
                    close()
                }
            }.build()
            return _deselect!!
        }

    private var _desktopWindows: ImageVector? = null
    val DesktopWindows: ImageVector
        get() {
            if (_desktopWindows != null) return _desktopWindows!!
            _desktopWindows = ImageVector.Builder(name = "DesktopWindows", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(21.0f, 2.0f)
                    horizontalLineTo(3.0f)
                    curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                    verticalLineToRelative(12.0f)
                    curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                    horizontalLineToRelative(7.0f)
                    verticalLineToRelative(2.0f)
                    horizontalLineTo(9.0f)
                    curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                    reflectiveCurveToRelative(0.45f, 1.0f, 1.0f, 1.0f)
                    horizontalLineToRelative(6.0f)
                    curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                    reflectiveCurveToRelative(-0.45f, -1.0f, -1.0f, -1.0f)
                    horizontalLineToRelative(-1.0f)
                    verticalLineToRelative(-2.0f)
                    horizontalLineToRelative(7.0f)
                    curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                    verticalLineTo(4.0f)
                    curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                    close()
                    moveToRelative(-1.0f, 14.0f)
                    horizontalLineTo(4.0f)
                    curveToRelative(-0.55f, 0.0f, -1.0f, -0.45f, -1.0f, -1.0f)
                    verticalLineTo(5.0f)
                    curveToRelative(0.0f, -0.55f, 0.45f, -1.0f, 1.0f, -1.0f)
                    horizontalLineToRelative(16.0f)
                    curveToRelative(0.55f, 0.0f, 1.0f, 0.45f, 1.0f, 1.0f)
                    verticalLineToRelative(10.0f)
                    curveToRelative(0.0f, 0.55f, -0.45f, 1.0f, -1.0f, 1.0f)
                    close()
                }
            }.build()
            return _desktopWindows!!
        }

    private var _download: ImageVector? = null
    val Download: ImageVector
        get() {
            if (_download != null) return _download!!
            _download = ImageVector.Builder(name = "Download", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(16.59f, 9.0f)
                    horizontalLineTo(15.0f)
                    verticalLineTo(4.0f)
                    curveToRelative(0.0f, -0.55f, -0.45f, -1.0f, -1.0f, -1.0f)
                    horizontalLineToRelative(-4.0f)
                    curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                    verticalLineToRelative(5.0f)
                    horizontalLineTo(7.41f)
                    curveToRelative(-0.89f, 0.0f, -1.34f, 1.08f, -0.71f, 1.71f)
                    lineToRelative(4.59f, 4.59f)
                    curveToRelative(0.39f, 0.39f, 1.02f, 0.39f, 1.41f, 0.0f)
                    lineToRelative(4.59f, -4.59f)
                    curveToRelative(0.63f, -0.63f, 0.19f, -1.71f, -0.7f, -1.71f)
                    close()
                    moveTo(5.0f, 19.0f)
                    curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                    horizontalLineToRelative(12.0f)
                    curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                    reflectiveCurveToRelative(-0.45f, -1.0f, -1.0f, -1.0f)
                    horizontalLineTo(6.0f)
                    curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                    close()
                }
            }.build()
            return _download!!
        }

    private var _edit: ImageVector? = null
    val Edit: ImageVector
        get() {
            if (_edit != null) return _edit!!
            _edit = ImageVector.Builder(name = "Edit", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(3.0f, 17.46f)
                    verticalLineToRelative(3.04f)
                    curveToRelative(0.0f, 0.28f, 0.22f, 0.5f, 0.5f, 0.5f)
                    horizontalLineToRelative(3.04f)
                    curveToRelative(0.13f, 0.0f, 0.26f, -0.05f, 0.35f, -0.15f)
                    lineTo(17.81f, 9.94f)
                    lineToRelative(-3.75f, -3.75f)
                    lineTo(3.15f, 17.1f)
                    curveToRelative(-0.1f, 0.1f, -0.15f, 0.22f, -0.15f, 0.36f)
                    close()
                    moveTo(20.71f, 7.04f)
                    arcToRelative(0.996f, 0.996f, 0.0f, false, false, 0.0f, -1.41f)
                    lineToRelative(-2.34f, -2.34f)
                    arcToRelative(0.996f, 0.996f, 0.0f, false, false, -1.41f, 0.0f)
                    lineToRelative(-1.83f, 1.83f)
                    lineToRelative(3.75f, 3.75f)
                    lineToRelative(1.83f, -1.83f)
                    close()
                }
            }.build()
            return _edit!!
        }

    private var _errorOutline: ImageVector? = null
    val ErrorOutline: ImageVector
        get() {
            if (_errorOutline != null) return _errorOutline!!
            _errorOutline = ImageVector.Builder(name = "ErrorOutline", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(12.0f, 7.0f)
                    curveToRelative(0.55f, 0.0f, 1.0f, 0.45f, 1.0f, 1.0f)
                    verticalLineToRelative(4.0f)
                    curveToRelative(0.0f, 0.55f, -0.45f, 1.0f, -1.0f, 1.0f)
                    reflectiveCurveToRelative(-1.0f, -0.45f, -1.0f, -1.0f)
                    verticalLineTo(8.0f)
                    curveToRelative(0.0f, -0.55f, 0.45f, -1.0f, 1.0f, -1.0f)
                    close()
                    moveToRelative(-0.01f, -5.0f)
                    curveTo(6.47f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f)
                    reflectiveCurveToRelative(4.47f, 10.0f, 9.99f, 10.0f)
                    curveTo(17.52f, 22.0f, 22.0f, 17.52f, 22.0f, 12.0f)
                    reflectiveCurveTo(17.52f, 2.0f, 11.99f, 2.0f)
                    close()
                    moveTo(12.0f, 20.0f)
                    curveToRelative(-4.42f, 0.0f, -8.0f, -3.58f, -8.0f, -8.0f)
                    reflectiveCurveToRelative(3.58f, -8.0f, 8.0f, -8.0f)
                    reflectiveCurveToRelative(8.0f, 3.58f, 8.0f, 8.0f)
                    reflectiveCurveToRelative(-3.58f, 8.0f, -8.0f, 8.0f)
                    close()
                    moveToRelative(1.0f, -3.0f)
                    horizontalLineToRelative(-2.0f)
                    verticalLineToRelative(-2.0f)
                    horizontalLineToRelative(2.0f)
                    verticalLineToRelative(2.0f)
                    close()
                }
            }.build()
            return _errorOutline!!
        }

    private var _fileOpen: ImageVector? = null
    val FileOpen: ImageVector
        get() {
            if (_fileOpen != null) return _fileOpen!!
            _fileOpen = ImageVector.Builder(name = "FileOpen", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(13.17f, 2.0f)
                    horizontalLineTo(6.0f)
                    curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                    verticalLineToRelative(16.0f)
                    curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                    horizontalLineToRelative(9.0f)
                    verticalLineToRelative(-6.0f)
                    curveToRelative(0.0f, -1.1f, 0.9f, -2.0f, 2.0f, -2.0f)
                    horizontalLineToRelative(3.0f)
                    verticalLineTo(8.83f)
                    curveToRelative(0.0f, -0.53f, -0.21f, -1.04f, -0.59f, -1.41f)
                    lineToRelative(-4.83f, -4.83f)
                    curveToRelative(-0.37f, -0.38f, -0.88f, -0.59f, -1.41f, -0.59f)
                    close()
                    moveTo(13.0f, 8.0f)
                    verticalLineTo(3.5f)
                    lineTo(18.5f, 9.0f)
                    horizontalLineTo(14.0f)
                    curveToRelative(-0.55f, 0.0f, -1.0f, -0.45f, -1.0f, -1.0f)
                    close()
                    moveToRelative(9.66f, 9.0f)
                    curveToRelative(0.0f, 0.55f, -0.45f, 1.0f, -1.0f, 1.0f)
                    horizontalLineToRelative(-1.24f)
                    lineToRelative(2.24f, 2.24f)
                    arcToRelative(0.996f, 0.996f, 0.0f, true, true, -1.41f, 1.41f)
                    lineTo(19.0f, 19.41f)
                    verticalLineToRelative(1.24f)
                    curveToRelative(0.0f, 0.55f, -0.45f, 1.0f, -1.0f, 1.0f)
                    reflectiveCurveToRelative(-1.0f, -0.45f, -1.0f, -1.0f)
                    verticalLineTo(17.0f)
                    curveToRelative(0.0f, -0.55f, 0.45f, -1.0f, 1.0f, -1.0f)
                    horizontalLineToRelative(3.66f)
                    curveToRelative(0.55f, 0.0f, 1.0f, 0.45f, 1.0f, 1.0f)
                    close()
                }
            }.build()
            return _fileOpen!!
        }

    private var _findInPage: ImageVector? = null
    val FindInPage: ImageVector
        get() {
            if (_findInPage != null) return _findInPage!!
            _findInPage = ImageVector.Builder(name = "FindInPage", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(20.0f, 19.59f)
                    verticalLineTo(8.83f)
                    curveToRelative(0.0f, -0.53f, -0.21f, -1.04f, -0.59f, -1.41f)
                    lineToRelative(-4.83f, -4.83f)
                    curveToRelative(-0.37f, -0.38f, -0.88f, -0.59f, -1.41f, -0.59f)
                    horizontalLineTo(6.0f)
                    curveToRelative(-1.1f, 0.0f, -1.99f, 0.9f, -1.99f, 2.0f)
                    lineTo(4.0f, 20.0f)
                    curveToRelative(0.0f, 1.1f, 0.89f, 2.0f, 1.99f, 2.0f)
                    horizontalLineTo(18.0f)
                    curveToRelative(0.45f, 0.0f, 0.85f, -0.15f, 1.19f, -0.4f)
                    lineToRelative(-4.43f, -4.43f)
                    curveToRelative(-0.86f, 0.56f, -1.89f, 0.88f, -3.0f, 0.82f)
                    curveToRelative(-2.37f, -0.11f, -4.4f, -1.96f, -4.72f, -4.31f)
                    arcToRelative(5.013f, 5.013f, 0.0f, false, true, 5.83f, -5.61f)
                    curveToRelative(1.95f, 0.33f, 3.57f, 1.85f, 4.0f, 3.78f)
                    curveToRelative(0.33f, 1.46f, 0.01f, 2.82f, -0.7f, 3.9f)
                    lineTo(20.0f, 19.59f)
                    close()
                    moveTo(9.0f, 13.0f)
                    curveToRelative(0.0f, 1.66f, 1.34f, 3.0f, 3.0f, 3.0f)
                    reflectiveCurveToRelative(3.0f, -1.34f, 3.0f, -3.0f)
                    reflectiveCurveToRelative(-1.34f, -3.0f, -3.0f, -3.0f)
                    reflectiveCurveToRelative(-3.0f, 1.34f, -3.0f, 3.0f)
                    close()
                }
            }.build()
            return _findInPage!!
        }

    private var _folder: ImageVector? = null
    val Folder: ImageVector
        get() {
            if (_folder != null) return _folder!!
            _folder = ImageVector.Builder(name = "Folder", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(10.59f, 4.59f)
                    curveTo(10.21f, 4.21f, 9.7f, 4.0f, 9.17f, 4.0f)
                    horizontalLineTo(4.0f)
                    curveToRelative(-1.1f, 0.0f, -1.99f, 0.9f, -1.99f, 2.0f)
                    lineTo(2.0f, 18.0f)
                    curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                    horizontalLineToRelative(16.0f)
                    curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                    verticalLineTo(8.0f)
                    curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                    horizontalLineToRelative(-8.0f)
                    lineToRelative(-1.41f, -1.41f)
                    close()
                }
            }.build()
            return _folder!!
        }

    private var _folderOpen: ImageVector? = null
    val FolderOpen: ImageVector
        get() {
            if (_folderOpen != null) return _folderOpen!!
            _folderOpen = ImageVector.Builder(name = "FolderOpen", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(20.0f, 6.0f)
                    horizontalLineToRelative(-8.0f)
                    lineToRelative(-1.41f, -1.41f)
                    curveTo(10.21f, 4.21f, 9.7f, 4.0f, 9.17f, 4.0f)
                    horizontalLineTo(4.0f)
                    curveToRelative(-1.1f, 0.0f, -1.99f, 0.9f, -1.99f, 2.0f)
                    lineTo(2.0f, 18.0f)
                    curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                    horizontalLineToRelative(16.0f)
                    curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                    verticalLineTo(8.0f)
                    curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                    close()
                    moveToRelative(-1.0f, 12.0f)
                    horizontalLineTo(5.0f)
                    curveToRelative(-0.55f, 0.0f, -1.0f, -0.45f, -1.0f, -1.0f)
                    verticalLineTo(9.0f)
                    curveToRelative(0.0f, -0.55f, 0.45f, -1.0f, 1.0f, -1.0f)
                    horizontalLineToRelative(14.0f)
                    curveToRelative(0.55f, 0.0f, 1.0f, 0.45f, 1.0f, 1.0f)
                    verticalLineToRelative(8.0f)
                    curveToRelative(0.0f, 0.55f, -0.45f, 1.0f, -1.0f, 1.0f)
                    close()
                }
            }.build()
            return _folderOpen!!
        }

    private var _history: ImageVector? = null
    val History: ImageVector
        get() {
            if (_history != null) return _history!!
            _history = ImageVector.Builder(name = "History", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(13.26f, 3.0f)
                    curveTo(8.17f, 2.86f, 4.0f, 6.95f, 4.0f, 12.0f)
                    horizontalLineTo(2.21f)
                    curveToRelative(-0.45f, 0.0f, -0.67f, 0.54f, -0.35f, 0.85f)
                    lineToRelative(2.79f, 2.8f)
                    curveToRelative(0.2f, 0.2f, 0.51f, 0.2f, 0.71f, 0.0f)
                    lineToRelative(2.79f, -2.8f)
                    arcToRelative(0.5f, 0.5f, 0.0f, false, false, -0.36f, -0.85f)
                    horizontalLineTo(6.0f)
                    curveToRelative(0.0f, -3.9f, 3.18f, -7.05f, 7.1f, -7.0f)
                    curveToRelative(3.72f, 0.05f, 6.85f, 3.18f, 6.9f, 6.9f)
                    curveToRelative(0.05f, 3.91f, -3.1f, 7.1f, -7.0f, 7.1f)
                    curveToRelative(-1.61f, 0.0f, -3.1f, -0.55f, -4.28f, -1.48f)
                    arcToRelative(0.994f, 0.994f, 0.0f, false, false, -1.32f, 0.08f)
                    curveToRelative(-0.42f, 0.42f, -0.39f, 1.13f, 0.08f, 1.49f)
                    arcTo(8.858f, 8.858f, 0.0f, false, false, 13.0f, 21.0f)
                    curveToRelative(5.05f, 0.0f, 9.14f, -4.17f, 9.0f, -9.26f)
                    curveToRelative(-0.13f, -4.69f, -4.05f, -8.61f, -8.74f, -8.74f)
                    close()
                    moveToRelative(-0.51f, 5.0f)
                    curveToRelative(-0.41f, 0.0f, -0.75f, 0.34f, -0.75f, 0.75f)
                    verticalLineToRelative(3.68f)
                    curveToRelative(0.0f, 0.35f, 0.19f, 0.68f, 0.49f, 0.86f)
                    lineToRelative(3.12f, 1.85f)
                    curveToRelative(0.36f, 0.21f, 0.82f, 0.09f, 1.03f, -0.26f)
                    curveToRelative(0.21f, -0.36f, 0.09f, -0.82f, -0.26f, -1.03f)
                    lineToRelative(-2.88f, -1.71f)
                    verticalLineToRelative(-3.4f)
                    curveToRelative(0.0f, -0.4f, -0.34f, -0.74f, -0.75f, -0.74f)
                    close()
                }
            }.build()
            return _history!!
        }

    private var _hourglassTop: ImageVector? = null
    val HourglassTop: ImageVector
        get() {
            if (_hourglassTop != null) return _hourglassTop!!
            _hourglassTop = ImageVector.Builder(name = "HourglassTop", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(8.0f, 2.0f)
                    curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                    lineToRelative(0.01f, 3.18f)
                    curveToRelative(0.0f, 0.53f, 0.21f, 1.03f, 0.58f, 1.41f)
                    lineTo(10.0f, 12.0f)
                    lineToRelative(-3.41f, 3.43f)
                    curveToRelative(-0.37f, 0.37f, -0.58f, 0.88f, -0.58f, 1.41f)
                    lineTo(6.0f, 20.0f)
                    curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                    horizontalLineToRelative(8.0f)
                    curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                    verticalLineToRelative(-3.16f)
                    curveToRelative(0.0f, -0.53f, -0.21f, -1.04f, -0.58f, -1.41f)
                    lineTo(14.0f, 12.0f)
                    lineToRelative(3.41f, -3.4f)
                    curveToRelative(0.38f, -0.38f, 0.59f, -0.89f, 0.59f, -1.42f)
                    verticalLineTo(4.0f)
                    curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                    horizontalLineTo(8.0f)
                    close()
                    moveToRelative(8.0f, 14.91f)
                    verticalLineTo(19.0f)
                    curveToRelative(0.0f, 0.55f, -0.45f, 1.0f, -1.0f, 1.0f)
                    horizontalLineTo(9.0f)
                    curveToRelative(-0.55f, 0.0f, -1.0f, -0.45f, -1.0f, -1.0f)
                    verticalLineToRelative(-2.09f)
                    curveToRelative(0.0f, -0.27f, 0.11f, -0.52f, 0.29f, -0.71f)
                    lineTo(12.0f, 12.5f)
                    lineToRelative(3.71f, 3.71f)
                    curveToRelative(0.18f, 0.18f, 0.29f, 0.44f, 0.29f, 0.7f)
                    close()
                }
            }.build()
            return _hourglassTop!!
        }

    private var _hub: ImageVector? = null
    val Hub: ImageVector
        get() {
            if (_hub != null) return _hub!!
            _hub = ImageVector.Builder(name = "Hub", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(8.4f, 18.2f)
                    curveToRelative(0.38f, 0.5f, 0.6f, 1.12f, 0.6f, 1.8f)
                    curveToRelative(0.0f, 1.66f, -1.34f, 3.0f, -3.0f, 3.0f)
                    reflectiveCurveToRelative(-3.0f, -1.34f, -3.0f, -3.0f)
                    reflectiveCurveToRelative(1.34f, -3.0f, 3.0f, -3.0f)
                    curveToRelative(0.44f, 0.0f, 0.85f, 0.09f, 1.23f, 0.26f)
                    lineToRelative(1.41f, -1.77f)
                    arcToRelative(4.504f, 4.504f, 0.0f, false, true, -1.09f, -3.69f)
                    lineToRelative(-2.03f, -0.68f)
                    arcTo(2.997f, 2.997f, 0.0f, false, true, 0.0f, 9.5f)
                    curveToRelative(0.0f, -1.66f, 1.34f, -3.0f, 3.0f, -3.0f)
                    reflectiveCurveToRelative(3.0f, 1.34f, 3.0f, 3.0f)
                    curveToRelative(0.0f, 0.07f, 0.0f, 0.14f, -0.01f, 0.21f)
                    lineToRelative(2.03f, 0.68f)
                    arcToRelative(4.468f, 4.468f, 0.0f, false, true, 3.22f, -2.32f)
                    verticalLineTo(5.91f)
                    arcTo(3.018f, 3.018f, 0.0f, false, true, 9.0f, 3.0f)
                    curveToRelative(0.0f, -1.66f, 1.34f, -3.0f, 3.0f, -3.0f)
                    reflectiveCurveToRelative(3.0f, 1.34f, 3.0f, 3.0f)
                    curveToRelative(0.0f, 1.4f, -0.96f, 2.57f, -2.25f, 2.91f)
                    verticalLineToRelative(2.16f)
                    curveToRelative(1.4f, 0.23f, 2.58f, 1.11f, 3.22f, 2.32f)
                    lineTo(18.0f, 9.71f)
                    verticalLineTo(9.5f)
                    curveToRelative(0.0f, -1.66f, 1.34f, -3.0f, 3.0f, -3.0f)
                    reflectiveCurveToRelative(3.0f, 1.34f, 3.0f, 3.0f)
                    reflectiveCurveToRelative(-1.34f, 3.0f, -3.0f, 3.0f)
                    curveToRelative(-1.06f, 0.0f, -1.98f, -0.55f, -2.52f, -1.37f)
                    lineToRelative(-2.03f, 0.68f)
                    arcToRelative(4.49f, 4.49f, 0.0f, false, true, -1.09f, 3.69f)
                    lineToRelative(1.41f, 1.77f)
                    curveToRelative(0.38f, -0.18f, 0.79f, -0.27f, 1.23f, -0.27f)
                    curveToRelative(1.66f, 0.0f, 3.0f, 1.34f, 3.0f, 3.0f)
                    reflectiveCurveToRelative(-1.34f, 3.0f, -3.0f, 3.0f)
                    reflectiveCurveToRelative(-3.0f, -1.34f, -3.0f, -3.0f)
                    curveToRelative(0.0f, -0.68f, 0.22f, -1.3f, 0.6f, -1.8f)
                    lineToRelative(-1.41f, -1.77f)
                    curveToRelative(-1.35f, 0.75f, -3.01f, 0.76f, -4.37f, 0.0f)
                    lineTo(8.4f, 18.2f)
                    close()
                }
            }.build()
            return _hub!!
        }

    private var _info: ImageVector? = null
    val Info: ImageVector
        get() {
            if (_info != null) return _info!!
            _info = ImageVector.Builder(name = "Info", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(12.0f, 2.0f)
                    curveTo(6.48f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f)
                    reflectiveCurveToRelative(4.48f, 10.0f, 10.0f, 10.0f)
                    reflectiveCurveToRelative(10.0f, -4.48f, 10.0f, -10.0f)
                    reflectiveCurveTo(17.52f, 2.0f, 12.0f, 2.0f)
                    close()
                    moveToRelative(0.0f, 15.0f)
                    curveToRelative(-0.55f, 0.0f, -1.0f, -0.45f, -1.0f, -1.0f)
                    verticalLineToRelative(-4.0f)
                    curveToRelative(0.0f, -0.55f, 0.45f, -1.0f, 1.0f, -1.0f)
                    reflectiveCurveToRelative(1.0f, 0.45f, 1.0f, 1.0f)
                    verticalLineToRelative(4.0f)
                    curveToRelative(0.0f, 0.55f, -0.45f, 1.0f, -1.0f, 1.0f)
                    close()
                    moveToRelative(1.0f, -8.0f)
                    horizontalLineToRelative(-2.0f)
                    verticalLineTo(7.0f)
                    horizontalLineToRelative(2.0f)
                    verticalLineToRelative(2.0f)
                    close()
                }
            }.build()
            return _info!!
        }

    private var _insertDriveFile: ImageVector? = null
    val InsertDriveFile: ImageVector
        get() {
            if (_insertDriveFile != null) return _insertDriveFile!!
            _insertDriveFile = ImageVector.Builder(name = "InsertDriveFile", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(6.0f, 2.0f)
                    curveToRelative(-1.1f, 0.0f, -1.99f, 0.9f, -1.99f, 2.0f)
                    lineTo(4.0f, 20.0f)
                    curveToRelative(0.0f, 1.1f, 0.89f, 2.0f, 1.99f, 2.0f)
                    horizontalLineTo(18.0f)
                    curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                    verticalLineTo(8.83f)
                    curveToRelative(0.0f, -0.53f, -0.21f, -1.04f, -0.59f, -1.41f)
                    lineToRelative(-4.83f, -4.83f)
                    curveToRelative(-0.37f, -0.38f, -0.88f, -0.59f, -1.41f, -0.59f)
                    horizontalLineTo(6.0f)
                    close()
                    moveToRelative(7.0f, 6.0f)
                    verticalLineTo(3.5f)
                    lineTo(18.5f, 9.0f)
                    horizontalLineTo(14.0f)
                    curveToRelative(-0.55f, 0.0f, -1.0f, -0.45f, -1.0f, -1.0f)
                    close()
                }
            }.build()
            return _insertDriveFile!!
        }

    private var _installMobile: ImageVector? = null
    val InstallMobile: ImageVector
        get() {
            if (_installMobile != null) return _installMobile!!
            _installMobile = ImageVector.Builder(name = "InstallMobile", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(18.71f, 13.29f)
                    lineTo(22.3f, 9.7f)
                    arcToRelative(0.996f, 0.996f, 0.0f, true, false, -1.41f, -1.41f)
                    lineTo(19.0f, 10.17f)
                    verticalLineTo(4.0f)
                    curveToRelative(0.0f, -0.55f, -0.45f, -1.0f, -1.0f, -1.0f)
                    reflectiveCurveToRelative(-1.0f, 0.45f, -1.0f, 1.0f)
                    verticalLineToRelative(6.17f)
                    lineToRelative(-1.89f, -1.88f)
                    arcTo(0.996f, 0.996f, 0.0f, true, false, 13.7f, 9.7f)
                    lineToRelative(3.59f, 3.59f)
                    curveToRelative(0.4f, 0.39f, 1.03f, 0.39f, 1.42f, 0.0f)
                    close()
                }
                path(fill = SolidColor(Color.Black)) {
                    moveTo(17.0f, 18.0f)
                    horizontalLineTo(7.0f)
                    verticalLineTo(6.0f)
                    horizontalLineToRelative(7.0f)
                    verticalLineTo(1.0f)
                    horizontalLineTo(7.0f)
                    curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                    verticalLineToRelative(18.0f)
                    curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                    horizontalLineToRelative(10.0f)
                    curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                    verticalLineToRelative(-5.0f)
                    horizontalLineToRelative(-2.0f)
                    verticalLineToRelative(2.0f)
                    close()
                }
            }.build()
            return _installMobile!!
        }

    private var _keyboardArrowDown: ImageVector? = null
    val KeyboardArrowDown: ImageVector
        get() {
            if (_keyboardArrowDown != null) return _keyboardArrowDown!!
            _keyboardArrowDown = ImageVector.Builder(name = "KeyboardArrowDown", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(8.12f, 9.29f)
                    lineTo(12.0f, 13.17f)
                    lineToRelative(3.88f, -3.88f)
                    arcToRelative(0.996f, 0.996f, 0.0f, true, true, 1.41f, 1.41f)
                    lineToRelative(-4.59f, 4.59f)
                    arcToRelative(0.996f, 0.996f, 0.0f, false, true, -1.41f, 0.0f)
                    lineTo(6.7f, 10.7f)
                    arcToRelative(0.996f, 0.996f, 0.0f, false, true, 0.0f, -1.41f)
                    curveToRelative(0.39f, -0.38f, 1.03f, -0.39f, 1.42f, 0.0f)
                    close()
                }
            }.build()
            return _keyboardArrowDown!!
        }

    private var _keyboardArrowRight: ImageVector? = null
    val KeyboardArrowRight: ImageVector
        get() {
            if (_keyboardArrowRight != null) return _keyboardArrowRight!!
            _keyboardArrowRight = ImageVector.Builder(name = "KeyboardArrowRight", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f, autoMirror = true).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(9.29f, 15.88f)
                    lineTo(13.17f, 12.0f)
                    lineTo(9.29f, 8.12f)
                    arcToRelative(0.996f, 0.996f, 0.0f, true, true, 1.41f, -1.41f)
                    lineToRelative(4.59f, 4.59f)
                    curveToRelative(0.39f, 0.39f, 0.39f, 1.02f, 0.0f, 1.41f)
                    lineTo(10.7f, 17.3f)
                    arcToRelative(0.996f, 0.996f, 0.0f, false, true, -1.41f, 0.0f)
                    curveToRelative(-0.38f, -0.39f, -0.39f, -1.03f, 0.0f, -1.42f)
                    close()
                }
            }.build()
            return _keyboardArrowRight!!
        }

    private var _keyboardArrowUp: ImageVector? = null
    val KeyboardArrowUp: ImageVector
        get() {
            if (_keyboardArrowUp != null) return _keyboardArrowUp!!
            _keyboardArrowUp = ImageVector.Builder(name = "KeyboardArrowUp", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(8.12f, 14.71f)
                    lineTo(12.0f, 10.83f)
                    lineToRelative(3.88f, 3.88f)
                    arcToRelative(0.996f, 0.996f, 0.0f, true, false, 1.41f, -1.41f)
                    lineTo(12.7f, 8.71f)
                    arcToRelative(0.996f, 0.996f, 0.0f, false, false, -1.41f, 0.0f)
                    lineTo(6.7f, 13.3f)
                    arcToRelative(0.996f, 0.996f, 0.0f, false, false, 0.0f, 1.41f)
                    curveToRelative(0.39f, 0.38f, 1.03f, 0.39f, 1.42f, 0.0f)
                    close()
                }
            }.build()
            return _keyboardArrowUp!!
        }

    private var _language: ImageVector? = null
    val Language: ImageVector
        get() {
            if (_language != null) return _language!!
            _language = ImageVector.Builder(name = "Language", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(11.99f, 2.0f)
                    curveTo(6.47f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f)
                    reflectiveCurveToRelative(4.47f, 10.0f, 9.99f, 10.0f)
                    curveTo(17.52f, 22.0f, 22.0f, 17.52f, 22.0f, 12.0f)
                    reflectiveCurveTo(17.52f, 2.0f, 11.99f, 2.0f)
                    close()
                    moveToRelative(6.93f, 6.0f)
                    horizontalLineToRelative(-2.95f)
                    arcToRelative(15.65f, 15.65f, 0.0f, false, false, -1.38f, -3.56f)
                    arcTo(8.03f, 8.03f, 0.0f, false, true, 18.92f, 8.0f)
                    close()
                    moveTo(12.0f, 4.04f)
                    curveToRelative(0.83f, 1.2f, 1.48f, 2.53f, 1.91f, 3.96f)
                    horizontalLineToRelative(-3.82f)
                    curveToRelative(0.43f, -1.43f, 1.08f, -2.76f, 1.91f, -3.96f)
                    close()
                    moveTo(4.26f, 14.0f)
                    curveTo(4.1f, 13.36f, 4.0f, 12.69f, 4.0f, 12.0f)
                    reflectiveCurveToRelative(0.1f, -1.36f, 0.26f, -2.0f)
                    horizontalLineToRelative(3.38f)
                    curveToRelative(-0.08f, 0.66f, -0.14f, 1.32f, -0.14f, 2.0f)
                    reflectiveCurveToRelative(0.06f, 1.34f, 0.14f, 2.0f)
                    horizontalLineTo(4.26f)
                    close()
                    moveToRelative(0.82f, 2.0f)
                    horizontalLineToRelative(2.95f)
                    curveToRelative(0.32f, 1.25f, 0.78f, 2.45f, 1.38f, 3.56f)
                    arcTo(7.987f, 7.987f, 0.0f, false, true, 5.08f, 16.0f)
                    close()
                    moveToRelative(2.95f, -8.0f)
                    horizontalLineTo(5.08f)
                    arcToRelative(7.987f, 7.987f, 0.0f, false, true, 4.33f, -3.56f)
                    arcTo(15.65f, 15.65f, 0.0f, false, false, 8.03f, 8.0f)
                    close()
                    moveTo(12.0f, 19.96f)
                    curveToRelative(-0.83f, -1.2f, -1.48f, -2.53f, -1.91f, -3.96f)
                    horizontalLineToRelative(3.82f)
                    curveToRelative(-0.43f, 1.43f, -1.08f, 2.76f, -1.91f, 3.96f)
                    close()
                    moveTo(14.34f, 14.0f)
                    horizontalLineTo(9.66f)
                    curveToRelative(-0.09f, -0.66f, -0.16f, -1.32f, -0.16f, -2.0f)
                    reflectiveCurveToRelative(0.07f, -1.35f, 0.16f, -2.0f)
                    horizontalLineToRelative(4.68f)
                    curveToRelative(0.09f, 0.65f, 0.16f, 1.32f, 0.16f, 2.0f)
                    reflectiveCurveToRelative(-0.07f, 1.34f, -0.16f, 2.0f)
                    close()
                    moveToRelative(0.25f, 5.56f)
                    curveToRelative(0.6f, -1.11f, 1.06f, -2.31f, 1.38f, -3.56f)
                    horizontalLineToRelative(2.95f)
                    arcToRelative(8.03f, 8.03f, 0.0f, false, true, -4.33f, 3.56f)
                    close()
                    moveTo(16.36f, 14.0f)
                    curveToRelative(0.08f, -0.66f, 0.14f, -1.32f, 0.14f, -2.0f)
                    reflectiveCurveToRelative(-0.06f, -1.34f, -0.14f, -2.0f)
                    horizontalLineToRelative(3.38f)
                    curveToRelative(0.16f, 0.64f, 0.26f, 1.31f, 0.26f, 2.0f)
                    reflectiveCurveToRelative(-0.1f, 1.36f, -0.26f, 2.0f)
                    horizontalLineToRelative(-3.38f)
                    close()
                }
            }.build()
            return _language!!
        }

    private var _link: ImageVector? = null
    val Link: ImageVector
        get() {
            if (_link != null) return _link!!
            _link = ImageVector.Builder(name = "Link", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(17.0f, 7.0f)
                    horizontalLineToRelative(-3.0f)
                    curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                    reflectiveCurveToRelative(0.45f, 1.0f, 1.0f, 1.0f)
                    horizontalLineToRelative(3.0f)
                    curveToRelative(1.65f, 0.0f, 3.0f, 1.35f, 3.0f, 3.0f)
                    reflectiveCurveToRelative(-1.35f, 3.0f, -3.0f, 3.0f)
                    horizontalLineToRelative(-3.0f)
                    curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                    reflectiveCurveToRelative(0.45f, 1.0f, 1.0f, 1.0f)
                    horizontalLineToRelative(3.0f)
                    curveToRelative(2.76f, 0.0f, 5.0f, -2.24f, 5.0f, -5.0f)
                    reflectiveCurveToRelative(-2.24f, -5.0f, -5.0f, -5.0f)
                    close()
                    moveToRelative(-9.0f, 5.0f)
                    curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                    horizontalLineToRelative(6.0f)
                    curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                    reflectiveCurveToRelative(-0.45f, -1.0f, -1.0f, -1.0f)
                    horizontalLineTo(9.0f)
                    curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                    close()
                    moveToRelative(2.0f, 3.0f)
                    horizontalLineTo(7.0f)
                    curveToRelative(-1.65f, 0.0f, -3.0f, -1.35f, -3.0f, -3.0f)
                    reflectiveCurveToRelative(1.35f, -3.0f, 3.0f, -3.0f)
                    horizontalLineToRelative(3.0f)
                    curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                    reflectiveCurveToRelative(-0.45f, -1.0f, -1.0f, -1.0f)
                    horizontalLineTo(7.0f)
                    curveToRelative(-2.76f, 0.0f, -5.0f, 2.24f, -5.0f, 5.0f)
                    reflectiveCurveToRelative(2.24f, 5.0f, 5.0f, 5.0f)
                    horizontalLineToRelative(3.0f)
                    curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                    reflectiveCurveToRelative(-0.45f, -1.0f, -1.0f, -1.0f)
                    close()
                }
            }.build()
            return _link!!
        }

    private var _lock: ImageVector? = null
    val Lock: ImageVector
        get() {
            if (_lock != null) return _lock!!
            _lock = ImageVector.Builder(name = "Lock", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(18.0f, 8.0f)
                    horizontalLineToRelative(-1.0f)
                    verticalLineTo(6.0f)
                    curveToRelative(0.0f, -2.76f, -2.24f, -5.0f, -5.0f, -5.0f)
                    reflectiveCurveTo(7.0f, 3.24f, 7.0f, 6.0f)
                    verticalLineToRelative(2.0f)
                    horizontalLineTo(6.0f)
                    curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                    verticalLineToRelative(10.0f)
                    curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                    horizontalLineToRelative(12.0f)
                    curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                    verticalLineTo(10.0f)
                    curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                    close()
                    moveToRelative(-6.0f, 9.0f)
                    curveToRelative(-1.1f, 0.0f, -2.0f, -0.9f, -2.0f, -2.0f)
                    reflectiveCurveToRelative(0.9f, -2.0f, 2.0f, -2.0f)
                    reflectiveCurveToRelative(2.0f, 0.9f, 2.0f, 2.0f)
                    reflectiveCurveToRelative(-0.9f, 2.0f, -2.0f, 2.0f)
                    close()
                    moveTo(9.0f, 8.0f)
                    verticalLineTo(6.0f)
                    curveToRelative(0.0f, -1.66f, 1.34f, -3.0f, 3.0f, -3.0f)
                    reflectiveCurveToRelative(3.0f, 1.34f, 3.0f, 3.0f)
                    verticalLineToRelative(2.0f)
                    horizontalLineTo(9.0f)
                    close()
                }
            }.build()
            return _lock!!
        }

    private var _moreVert: ImageVector? = null
    val MoreVert: ImageVector
        get() {
            if (_moreVert != null) return _moreVert!!
            _moreVert = ImageVector.Builder(name = "MoreVert", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(12.0f, 8.0f)
                    curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                    reflectiveCurveToRelative(-0.9f, -2.0f, -2.0f, -2.0f)
                    reflectiveCurveToRelative(-2.0f, 0.9f, -2.0f, 2.0f)
                    reflectiveCurveToRelative(0.9f, 2.0f, 2.0f, 2.0f)
                    close()
                    moveToRelative(0.0f, 2.0f)
                    curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                    reflectiveCurveToRelative(0.9f, 2.0f, 2.0f, 2.0f)
                    reflectiveCurveToRelative(2.0f, -0.9f, 2.0f, -2.0f)
                    reflectiveCurveToRelative(-0.9f, -2.0f, -2.0f, -2.0f)
                    close()
                    moveToRelative(0.0f, 6.0f)
                    curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                    reflectiveCurveToRelative(0.9f, 2.0f, 2.0f, 2.0f)
                    reflectiveCurveToRelative(2.0f, -0.9f, 2.0f, -2.0f)
                    reflectiveCurveToRelative(-0.9f, -2.0f, -2.0f, -2.0f)
                    close()
                }
            }.build()
            return _moreVert!!
        }

    private var _openInNew: ImageVector? = null
    val OpenInNew: ImageVector
        get() {
            if (_openInNew != null) return _openInNew!!
            _openInNew = ImageVector.Builder(name = "OpenInNew", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(18.0f, 19.0f)
                    horizontalLineTo(6.0f)
                    curveToRelative(-0.55f, 0.0f, -1.0f, -0.45f, -1.0f, -1.0f)
                    verticalLineTo(6.0f)
                    curveToRelative(0.0f, -0.55f, 0.45f, -1.0f, 1.0f, -1.0f)
                    horizontalLineToRelative(5.0f)
                    curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                    reflectiveCurveToRelative(-0.45f, -1.0f, -1.0f, -1.0f)
                    horizontalLineTo(5.0f)
                    arcToRelative(2.0f, 2.0f, 0.0f, false, false, -2.0f, 2.0f)
                    verticalLineToRelative(14.0f)
                    curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                    horizontalLineToRelative(14.0f)
                    curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                    verticalLineToRelative(-6.0f)
                    curveToRelative(0.0f, -0.55f, -0.45f, -1.0f, -1.0f, -1.0f)
                    reflectiveCurveToRelative(-1.0f, 0.45f, -1.0f, 1.0f)
                    verticalLineToRelative(5.0f)
                    curveToRelative(0.0f, 0.55f, -0.45f, 1.0f, -1.0f, 1.0f)
                    close()
                    moveTo(14.0f, 4.0f)
                    curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                    horizontalLineToRelative(2.59f)
                    lineToRelative(-9.13f, 9.13f)
                    arcToRelative(0.996f, 0.996f, 0.0f, true, false, 1.41f, 1.41f)
                    lineTo(19.0f, 6.41f)
                    verticalLineTo(9.0f)
                    curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                    reflectiveCurveToRelative(1.0f, -0.45f, 1.0f, -1.0f)
                    verticalLineTo(4.0f)
                    curveToRelative(0.0f, -0.55f, -0.45f, -1.0f, -1.0f, -1.0f)
                    horizontalLineToRelative(-5.0f)
                    curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                    close()
                }
            }.build()
            return _openInNew!!
        }

    private var _palette: ImageVector? = null
    val Palette: ImageVector
        get() {
            if (_palette != null) return _palette!!
            _palette = ImageVector.Builder(name = "Palette", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(12.0f, 2.0f)
                    curveTo(6.49f, 2.0f, 2.0f, 6.49f, 2.0f, 12.0f)
                    reflectiveCurveToRelative(4.49f, 10.0f, 10.0f, 10.0f)
                    arcToRelative(2.5f, 2.5f, 0.0f, false, false, 2.5f, -2.5f)
                    curveToRelative(0.0f, -0.61f, -0.23f, -1.2f, -0.64f, -1.67f)
                    arcToRelative(0.528f, 0.528f, 0.0f, false, true, -0.13f, -0.33f)
                    curveToRelative(0.0f, -0.28f, 0.22f, -0.5f, 0.5f, -0.5f)
                    horizontalLineTo(16.0f)
                    curveToRelative(3.31f, 0.0f, 6.0f, -2.69f, 6.0f, -6.0f)
                    curveToRelative(0.0f, -4.96f, -4.49f, -9.0f, -10.0f, -9.0f)
                    close()
                    moveToRelative(5.5f, 11.0f)
                    curveToRelative(-0.83f, 0.0f, -1.5f, -0.67f, -1.5f, -1.5f)
                    reflectiveCurveToRelative(0.67f, -1.5f, 1.5f, -1.5f)
                    reflectiveCurveToRelative(1.5f, 0.67f, 1.5f, 1.5f)
                    reflectiveCurveToRelative(-0.67f, 1.5f, -1.5f, 1.5f)
                    close()
                    moveToRelative(-3.0f, -4.0f)
                    curveToRelative(-0.83f, 0.0f, -1.5f, -0.67f, -1.5f, -1.5f)
                    reflectiveCurveTo(13.67f, 6.0f, 14.5f, 6.0f)
                    reflectiveCurveToRelative(1.5f, 0.67f, 1.5f, 1.5f)
                    reflectiveCurveTo(15.33f, 9.0f, 14.5f, 9.0f)
                    close()
                    moveTo(5.0f, 11.5f)
                    curveToRelative(0.0f, -0.83f, 0.67f, -1.5f, 1.5f, -1.5f)
                    reflectiveCurveToRelative(1.5f, 0.67f, 1.5f, 1.5f)
                    reflectiveCurveTo(7.33f, 13.0f, 6.5f, 13.0f)
                    reflectiveCurveTo(5.0f, 12.33f, 5.0f, 11.5f)
                    close()
                    moveToRelative(6.0f, -4.0f)
                    curveToRelative(0.0f, 0.83f, -0.67f, 1.5f, -1.5f, 1.5f)
                    reflectiveCurveTo(8.0f, 8.33f, 8.0f, 7.5f)
                    reflectiveCurveTo(8.67f, 6.0f, 9.5f, 6.0f)
                    reflectiveCurveToRelative(1.5f, 0.67f, 1.5f, 1.5f)
                    close()
                }
            }.build()
            return _palette!!
        }

    private var _pause: ImageVector? = null
    val Pause: ImageVector
        get() {
            if (_pause != null) return _pause!!
            _pause = ImageVector.Builder(name = "Pause", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(8.0f, 19.0f)
                    curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                    verticalLineTo(7.0f)
                    curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                    reflectiveCurveToRelative(-2.0f, 0.9f, -2.0f, 2.0f)
                    verticalLineToRelative(10.0f)
                    curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                    close()
                    moveToRelative(6.0f, -12.0f)
                    verticalLineToRelative(10.0f)
                    curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                    reflectiveCurveToRelative(2.0f, -0.9f, 2.0f, -2.0f)
                    verticalLineTo(7.0f)
                    curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                    reflectiveCurveToRelative(-2.0f, 0.9f, -2.0f, 2.0f)
                    close()
                }
            }.build()
            return _pause!!
        }

    private var _person: ImageVector? = null
    val Person: ImageVector
        get() {
            if (_person != null) return _person!!
            _person = ImageVector.Builder(name = "Person", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(12.0f, 12.0f)
                    curveToRelative(2.21f, 0.0f, 4.0f, -1.79f, 4.0f, -4.0f)
                    reflectiveCurveToRelative(-1.79f, -4.0f, -4.0f, -4.0f)
                    reflectiveCurveToRelative(-4.0f, 1.79f, -4.0f, 4.0f)
                    reflectiveCurveToRelative(1.79f, 4.0f, 4.0f, 4.0f)
                    close()
                    moveToRelative(0.0f, 2.0f)
                    curveToRelative(-2.67f, 0.0f, -8.0f, 1.34f, -8.0f, 4.0f)
                    verticalLineToRelative(1.0f)
                    curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                    horizontalLineToRelative(14.0f)
                    curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                    verticalLineToRelative(-1.0f)
                    curveToRelative(0.0f, -2.66f, -5.33f, -4.0f, -8.0f, -4.0f)
                    close()
                }
            }.build()
            return _person!!
        }

    private var _playArrow: ImageVector? = null
    val PlayArrow: ImageVector
        get() {
            if (_playArrow != null) return _playArrow!!
            _playArrow = ImageVector.Builder(name = "PlayArrow", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(8.0f, 6.82f)
                    verticalLineToRelative(10.36f)
                    curveToRelative(0.0f, 0.79f, 0.87f, 1.27f, 1.54f, 0.84f)
                    lineToRelative(8.14f, -5.18f)
                    arcToRelative(1.0f, 1.0f, 0.0f, false, false, 0.0f, -1.69f)
                    lineTo(9.54f, 5.98f)
                    arcTo(0.998f, 0.998f, 0.0f, false, false, 8.0f, 6.82f)
                    close()
                }
            }.build()
            return _playArrow!!
        }

    private var _privacyTip: ImageVector? = null
    val PrivacyTip: ImageVector
        get() {
            if (_privacyTip != null) return _privacyTip!!
            _privacyTip = ImageVector.Builder(name = "PrivacyTip", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(4.19f, 4.47f)
                    curveTo(3.47f, 4.79f, 3.0f, 5.51f, 3.0f, 6.3f)
                    verticalLineTo(11.0f)
                    curveToRelative(0.0f, 5.55f, 3.84f, 10.74f, 9.0f, 12.0f)
                    curveToRelative(5.16f, -1.26f, 9.0f, -6.45f, 9.0f, -12.0f)
                    verticalLineTo(6.3f)
                    curveToRelative(0.0f, -0.79f, -0.47f, -1.51f, -1.19f, -1.83f)
                    lineToRelative(-7.0f, -3.11f)
                    curveToRelative(-0.52f, -0.23f, -1.11f, -0.23f, -1.62f, 0.0f)
                    lineToRelative(-7.0f, 3.11f)
                    close()
                    moveTo(12.0f, 7.0f)
                    curveToRelative(0.55f, 0.0f, 1.0f, 0.45f, 1.0f, 1.0f)
                    reflectiveCurveToRelative(-0.45f, 1.0f, -1.0f, 1.0f)
                    reflectiveCurveToRelative(-1.0f, -0.45f, -1.0f, -1.0f)
                    reflectiveCurveToRelative(0.45f, -1.0f, 1.0f, -1.0f)
                    close()
                    moveToRelative(0.0f, 4.0f)
                    curveToRelative(0.55f, 0.0f, 1.0f, 0.45f, 1.0f, 1.0f)
                    verticalLineToRelative(4.0f)
                    curveToRelative(0.0f, 0.55f, -0.45f, 1.0f, -1.0f, 1.0f)
                    reflectiveCurveToRelative(-1.0f, -0.45f, -1.0f, -1.0f)
                    verticalLineToRelative(-4.0f)
                    curveToRelative(0.0f, -0.55f, 0.45f, -1.0f, 1.0f, -1.0f)
                    close()
                }
            }.build()
            return _privacyTip!!
        }

    private var _public: ImageVector? = null
    val Public: ImageVector
        get() {
            if (_public != null) return _public!!
            _public = ImageVector.Builder(name = "Public", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(12.0f, 2.0f)
                    curveTo(6.48f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f)
                    reflectiveCurveToRelative(4.48f, 10.0f, 10.0f, 10.0f)
                    reflectiveCurveToRelative(10.0f, -4.48f, 10.0f, -10.0f)
                    reflectiveCurveTo(17.52f, 2.0f, 12.0f, 2.0f)
                    close()
                    moveToRelative(-1.0f, 17.93f)
                    curveToRelative(-3.95f, -0.49f, -7.0f, -3.85f, -7.0f, -7.93f)
                    curveToRelative(0.0f, -0.62f, 0.08f, -1.21f, 0.21f, -1.79f)
                    lineTo(9.0f, 15.0f)
                    verticalLineToRelative(1.0f)
                    curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                    verticalLineToRelative(1.93f)
                    close()
                    moveToRelative(6.9f, -2.54f)
                    curveToRelative(-0.26f, -0.81f, -1.0f, -1.39f, -1.9f, -1.39f)
                    horizontalLineToRelative(-1.0f)
                    verticalLineToRelative(-3.0f)
                    curveToRelative(0.0f, -0.55f, -0.45f, -1.0f, -1.0f, -1.0f)
                    horizontalLineTo(8.0f)
                    verticalLineToRelative(-2.0f)
                    horizontalLineToRelative(2.0f)
                    curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                    verticalLineTo(7.0f)
                    horizontalLineToRelative(2.0f)
                    curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                    verticalLineToRelative(-0.41f)
                    curveToRelative(2.93f, 1.19f, 5.0f, 4.06f, 5.0f, 7.41f)
                    curveToRelative(0.0f, 2.08f, -0.8f, 3.97f, -2.1f, 5.39f)
                    close()
                }
            }.build()
            return _public!!
        }

    private var _refresh: ImageVector? = null
    val Refresh: ImageVector
        get() {
            if (_refresh != null) return _refresh!!
            _refresh = ImageVector.Builder(name = "Refresh", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(17.65f, 6.35f)
                    arcToRelative(7.95f, 7.95f, 0.0f, false, false, -6.48f, -2.31f)
                    curveToRelative(-3.67f, 0.37f, -6.69f, 3.35f, -7.1f, 7.02f)
                    curveTo(3.52f, 15.91f, 7.27f, 20.0f, 12.0f, 20.0f)
                    arcToRelative(7.98f, 7.98f, 0.0f, false, false, 7.21f, -4.56f)
                    curveToRelative(0.32f, -0.67f, -0.16f, -1.44f, -0.9f, -1.44f)
                    curveToRelative(-0.37f, 0.0f, -0.72f, 0.2f, -0.88f, 0.53f)
                    arcToRelative(5.994f, 5.994f, 0.0f, false, true, -6.8f, 3.31f)
                    curveToRelative(-2.22f, -0.49f, -4.01f, -2.3f, -4.48f, -4.52f)
                    arcTo(6.002f, 6.002f, 0.0f, false, true, 12.0f, 6.0f)
                    curveToRelative(1.66f, 0.0f, 3.14f, 0.69f, 4.22f, 1.78f)
                    lineToRelative(-1.51f, 1.51f)
                    curveToRelative(-0.63f, 0.63f, -0.19f, 1.71f, 0.7f, 1.71f)
                    horizontalLineTo(19.0f)
                    curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                    verticalLineTo(6.41f)
                    curveToRelative(0.0f, -0.89f, -1.08f, -1.34f, -1.71f, -0.71f)
                    lineToRelative(-0.64f, 0.65f)
                    close()
                }
            }.build()
            return _refresh!!
        }

    private var _search: ImageVector? = null
    val Search: ImageVector
        get() {
            if (_search != null) return _search!!
            _search = ImageVector.Builder(name = "Search", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(15.5f, 14.0f)
                    horizontalLineToRelative(-0.79f)
                    lineToRelative(-0.28f, -0.27f)
                    arcToRelative(6.5f, 6.5f, 0.0f, false, false, 1.48f, -5.34f)
                    curveToRelative(-0.47f, -2.78f, -2.79f, -5.0f, -5.59f, -5.34f)
                    arcToRelative(6.505f, 6.505f, 0.0f, false, false, -7.27f, 7.27f)
                    curveToRelative(0.34f, 2.8f, 2.56f, 5.12f, 5.34f, 5.59f)
                    arcToRelative(6.5f, 6.5f, 0.0f, false, false, 5.34f, -1.48f)
                    lineToRelative(0.27f, 0.28f)
                    verticalLineToRelative(0.79f)
                    lineToRelative(4.25f, 4.25f)
                    curveToRelative(0.41f, 0.41f, 1.08f, 0.41f, 1.49f, 0.0f)
                    curveToRelative(0.41f, -0.41f, 0.41f, -1.08f, 0.0f, -1.49f)
                    lineTo(15.5f, 14.0f)
                    close()
                    moveToRelative(-6.0f, 0.0f)
                    curveTo(7.01f, 14.0f, 5.0f, 11.99f, 5.0f, 9.5f)
                    reflectiveCurveTo(7.01f, 5.0f, 9.5f, 5.0f)
                    reflectiveCurveTo(14.0f, 7.01f, 14.0f, 9.5f)
                    reflectiveCurveTo(11.99f, 14.0f, 9.5f, 14.0f)
                    close()
                }
            }.build()
            return _search!!
        }

    private var _searchOff: ImageVector? = null
    val SearchOff: ImageVector
        get() {
            if (_searchOff != null) return _searchOff!!
            _searchOff = ImageVector.Builder(name = "SearchOff", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(15.5f, 14.0f)
                    horizontalLineToRelative(-0.79f)
                    lineToRelative(-0.28f, -0.27f)
                    arcToRelative(6.5f, 6.5f, 0.0f, false, false, 1.48f, -5.34f)
                    curveToRelative(-0.47f, -2.78f, -2.79f, -4.99f, -5.58f, -5.34f)
                    arcTo(6.492f, 6.492f, 0.0f, false, false, 3.03f, 9.0f)
                    horizontalLineToRelative(2.02f)
                    curveToRelative(0.24f, -2.12f, 1.92f, -3.8f, 4.06f, -3.98f)
                    curveTo(11.65f, 4.8f, 14.0f, 6.95f, 14.0f, 9.5f)
                    curveToRelative(0.0f, 2.49f, -2.01f, 4.5f, -4.5f, 4.5f)
                    curveToRelative(-0.17f, 0.0f, -0.33f, -0.03f, -0.5f, -0.05f)
                    verticalLineToRelative(2.02f)
                    lineToRelative(0.01f, 0.01f)
                    curveToRelative(1.8f, 0.13f, 3.47f, -0.47f, 4.72f, -1.55f)
                    lineToRelative(0.27f, 0.28f)
                    verticalLineToRelative(0.79f)
                    lineToRelative(4.25f, 4.25f)
                    curveToRelative(0.41f, 0.41f, 1.08f, 0.41f, 1.49f, 0.0f)
                    curveToRelative(0.41f, -0.41f, 0.41f, -1.08f, 0.0f, -1.49f)
                    lineTo(15.5f, 14.0f)
                    close()
                }
                path(fill = SolidColor(Color.Black)) {
                    moveTo(6.12f, 11.17f)
                    lineTo(4.0f, 13.29f)
                    lineToRelative(-2.12f, -2.12f)
                    curveToRelative(-0.2f, -0.2f, -0.51f, -0.2f, -0.71f, 0.0f)
                    curveToRelative(-0.2f, 0.2f, -0.2f, 0.51f, 0.0f, 0.71f)
                    lineTo(3.29f, 14.0f)
                    lineToRelative(-2.12f, 2.12f)
                    curveToRelative(-0.2f, 0.2f, -0.2f, 0.51f, 0.0f, 0.71f)
                    curveToRelative(0.2f, 0.2f, 0.51f, 0.2f, 0.71f, 0.0f)
                    lineTo(4.0f, 14.71f)
                    lineToRelative(2.12f, 2.12f)
                    curveToRelative(0.2f, 0.2f, 0.51f, 0.2f, 0.71f, 0.0f)
                    curveToRelative(0.2f, -0.2f, 0.2f, -0.51f, 0.0f, -0.71f)
                    lineTo(4.71f, 14.0f)
                    lineToRelative(2.12f, -2.12f)
                    curveToRelative(0.2f, -0.2f, 0.2f, -0.51f, 0.0f, -0.71f)
                    arcToRelative(0.513f, 0.513f, 0.0f, false, false, -0.71f, 0.0f)
                    close()
                }
            }.build()
            return _searchOff!!
        }

    private var _selectAll: ImageVector? = null
    val SelectAll: ImageVector
        get() {
            if (_selectAll != null) return _selectAll!!
            _selectAll = ImageVector.Builder(name = "SelectAll", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(3.0f, 5.0f)
                    horizontalLineToRelative(2.0f)
                    verticalLineTo(3.0f)
                    curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                    close()
                    moveToRelative(0.0f, 8.0f)
                    horizontalLineToRelative(2.0f)
                    verticalLineToRelative(-2.0f)
                    horizontalLineTo(3.0f)
                    verticalLineToRelative(2.0f)
                    close()
                    moveToRelative(4.0f, 8.0f)
                    horizontalLineToRelative(2.0f)
                    verticalLineToRelative(-2.0f)
                    horizontalLineTo(7.0f)
                    verticalLineToRelative(2.0f)
                    close()
                    moveTo(3.0f, 9.0f)
                    horizontalLineToRelative(2.0f)
                    verticalLineTo(7.0f)
                    horizontalLineTo(3.0f)
                    verticalLineToRelative(2.0f)
                    close()
                    moveToRelative(10.0f, -6.0f)
                    horizontalLineToRelative(-2.0f)
                    verticalLineToRelative(2.0f)
                    horizontalLineToRelative(2.0f)
                    verticalLineTo(3.0f)
                    close()
                    moveToRelative(6.0f, 0.0f)
                    verticalLineToRelative(2.0f)
                    horizontalLineToRelative(2.0f)
                    curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                    close()
                    moveTo(5.0f, 21.0f)
                    verticalLineToRelative(-2.0f)
                    horizontalLineTo(3.0f)
                    curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                    close()
                    moveToRelative(-2.0f, -4.0f)
                    horizontalLineToRelative(2.0f)
                    verticalLineToRelative(-2.0f)
                    horizontalLineTo(3.0f)
                    verticalLineToRelative(2.0f)
                    close()
                    moveTo(9.0f, 3.0f)
                    horizontalLineTo(7.0f)
                    verticalLineToRelative(2.0f)
                    horizontalLineToRelative(2.0f)
                    verticalLineTo(3.0f)
                    close()
                    moveToRelative(2.0f, 18.0f)
                    horizontalLineToRelative(2.0f)
                    verticalLineToRelative(-2.0f)
                    horizontalLineToRelative(-2.0f)
                    verticalLineToRelative(2.0f)
                    close()
                    moveToRelative(8.0f, -8.0f)
                    horizontalLineToRelative(2.0f)
                    verticalLineToRelative(-2.0f)
                    horizontalLineToRelative(-2.0f)
                    verticalLineToRelative(2.0f)
                    close()
                    moveToRelative(0.0f, 8.0f)
                    curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                    horizontalLineToRelative(-2.0f)
                    verticalLineToRelative(2.0f)
                    close()
                    moveToRelative(0.0f, -12.0f)
                    horizontalLineToRelative(2.0f)
                    verticalLineTo(7.0f)
                    horizontalLineToRelative(-2.0f)
                    verticalLineToRelative(2.0f)
                    close()
                    moveToRelative(0.0f, 8.0f)
                    horizontalLineToRelative(2.0f)
                    verticalLineToRelative(-2.0f)
                    horizontalLineToRelative(-2.0f)
                    verticalLineToRelative(2.0f)
                    close()
                    moveToRelative(-4.0f, 4.0f)
                    horizontalLineToRelative(2.0f)
                    verticalLineToRelative(-2.0f)
                    horizontalLineToRelative(-2.0f)
                    verticalLineToRelative(2.0f)
                    close()
                    moveToRelative(0.0f, -16.0f)
                    horizontalLineToRelative(2.0f)
                    verticalLineTo(3.0f)
                    horizontalLineToRelative(-2.0f)
                    verticalLineToRelative(2.0f)
                    close()
                    moveTo(8.0f, 17.0f)
                    horizontalLineToRelative(8.0f)
                    curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                    verticalLineTo(8.0f)
                    curveToRelative(0.0f, -0.55f, -0.45f, -1.0f, -1.0f, -1.0f)
                    horizontalLineTo(8.0f)
                    curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                    verticalLineToRelative(8.0f)
                    curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                    close()
                    moveToRelative(1.0f, -8.0f)
                    horizontalLineToRelative(6.0f)
                    verticalLineToRelative(6.0f)
                    horizontalLineTo(9.0f)
                    verticalLineTo(9.0f)
                    close()
                }
            }.build()
            return _selectAll!!
        }

    private var _settings: ImageVector? = null
    val Settings: ImageVector
        get() {
            if (_settings != null) return _settings!!
            _settings = ImageVector.Builder(name = "Settings", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(19.5f, 12.0f)
                    curveToRelative(0.0f, -0.23f, -0.01f, -0.45f, -0.03f, -0.68f)
                    lineToRelative(1.86f, -1.41f)
                    curveToRelative(0.4f, -0.3f, 0.51f, -0.86f, 0.26f, -1.3f)
                    lineToRelative(-1.87f, -3.23f)
                    arcToRelative(0.987f, 0.987f, 0.0f, false, false, -1.25f, -0.42f)
                    lineToRelative(-2.15f, 0.91f)
                    curveToRelative(-0.37f, -0.26f, -0.76f, -0.49f, -1.17f, -0.68f)
                    lineToRelative(-0.29f, -2.31f)
                    curveToRelative(-0.06f, -0.5f, -0.49f, -0.88f, -0.99f, -0.88f)
                    horizontalLineToRelative(-3.73f)
                    curveToRelative(-0.51f, 0.0f, -0.94f, 0.38f, -1.0f, 0.88f)
                    lineToRelative(-0.29f, 2.31f)
                    curveToRelative(-0.41f, 0.19f, -0.8f, 0.42f, -1.17f, 0.68f)
                    lineToRelative(-2.15f, -0.91f)
                    curveToRelative(-0.46f, -0.2f, -1.0f, -0.02f, -1.25f, 0.42f)
                    lineTo(2.41f, 8.62f)
                    curveToRelative(-0.25f, 0.44f, -0.14f, 0.99f, 0.26f, 1.3f)
                    lineToRelative(1.86f, 1.41f)
                    arcToRelative(7.343f, 7.343f, 0.0f, false, false, 0.0f, 1.35f)
                    lineToRelative(-1.86f, 1.41f)
                    curveToRelative(-0.4f, 0.3f, -0.51f, 0.86f, -0.26f, 1.3f)
                    lineToRelative(1.87f, 3.23f)
                    curveToRelative(0.25f, 0.44f, 0.79f, 0.62f, 1.25f, 0.42f)
                    lineToRelative(2.15f, -0.91f)
                    curveToRelative(0.37f, 0.26f, 0.76f, 0.49f, 1.17f, 0.68f)
                    lineToRelative(0.29f, 2.31f)
                    curveToRelative(0.06f, 0.5f, 0.49f, 0.88f, 0.99f, 0.88f)
                    horizontalLineToRelative(3.73f)
                    curveToRelative(0.5f, 0.0f, 0.93f, -0.38f, 0.99f, -0.88f)
                    lineToRelative(0.29f, -2.31f)
                    curveToRelative(0.41f, -0.19f, 0.8f, -0.42f, 1.17f, -0.68f)
                    lineToRelative(2.15f, 0.91f)
                    curveToRelative(0.46f, 0.2f, 1.0f, 0.02f, 1.25f, -0.42f)
                    lineToRelative(1.87f, -3.23f)
                    curveToRelative(0.25f, -0.44f, 0.14f, -0.99f, -0.26f, -1.3f)
                    lineToRelative(-1.86f, -1.41f)
                    curveToRelative(0.03f, -0.23f, 0.04f, -0.45f, 0.04f, -0.68f)
                    close()
                    moveToRelative(-7.46f, 3.5f)
                    curveToRelative(-1.93f, 0.0f, -3.5f, -1.57f, -3.5f, -3.5f)
                    reflectiveCurveToRelative(1.57f, -3.5f, 3.5f, -3.5f)
                    reflectiveCurveToRelative(3.5f, 1.57f, 3.5f, 3.5f)
                    reflectiveCurveToRelative(-1.57f, 3.5f, -3.5f, 3.5f)
                    close()
                }
            }.build()
            return _settings!!
        }

    private var _share: ImageVector? = null
    val Share: ImageVector
        get() {
            if (_share != null) return _share!!
            _share = ImageVector.Builder(name = "Share", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(18.0f, 16.08f)
                    curveToRelative(-0.76f, 0.0f, -1.44f, 0.3f, -1.96f, 0.77f)
                    lineTo(8.91f, 12.7f)
                    curveToRelative(0.05f, -0.23f, 0.09f, -0.46f, 0.09f, -0.7f)
                    reflectiveCurveToRelative(-0.04f, -0.47f, -0.09f, -0.7f)
                    lineToRelative(7.05f, -4.11f)
                    curveToRelative(0.54f, 0.5f, 1.25f, 0.81f, 2.04f, 0.81f)
                    curveToRelative(1.66f, 0.0f, 3.0f, -1.34f, 3.0f, -3.0f)
                    reflectiveCurveToRelative(-1.34f, -3.0f, -3.0f, -3.0f)
                    reflectiveCurveToRelative(-3.0f, 1.34f, -3.0f, 3.0f)
                    curveToRelative(0.0f, 0.24f, 0.04f, 0.47f, 0.09f, 0.7f)
                    lineTo(8.04f, 9.81f)
                    curveTo(7.5f, 9.31f, 6.79f, 9.0f, 6.0f, 9.0f)
                    curveToRelative(-1.66f, 0.0f, -3.0f, 1.34f, -3.0f, 3.0f)
                    reflectiveCurveToRelative(1.34f, 3.0f, 3.0f, 3.0f)
                    curveToRelative(0.79f, 0.0f, 1.5f, -0.31f, 2.04f, -0.81f)
                    lineToRelative(7.12f, 4.16f)
                    curveToRelative(-0.05f, 0.21f, -0.08f, 0.43f, -0.08f, 0.65f)
                    curveToRelative(0.0f, 1.61f, 1.31f, 2.92f, 2.92f, 2.92f)
                    reflectiveCurveToRelative(2.92f, -1.31f, 2.92f, -2.92f)
                    reflectiveCurveToRelative(-1.31f, -2.92f, -2.92f, -2.92f)
                    close()
                }
            }.build()
            return _share!!
        }

    private var _shield: ImageVector? = null
    val Shield: ImageVector
        get() {
            if (_shield != null) return _shield!!
            _shield = ImageVector.Builder(name = "Shield", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveToRelative(11.3f, 2.26f)
                    lineToRelative(-6.0f, 2.25f)
                    curveTo(4.52f, 4.81f, 4.0f, 5.55f, 4.0f, 6.39f)
                    verticalLineToRelative(4.7f)
                    curveToRelative(0.0f, 4.83f, 3.13f, 9.37f, 7.43f, 10.75f)
                    curveToRelative(0.37f, 0.12f, 0.77f, 0.12f, 1.14f, 0.0f)
                    curveToRelative(4.3f, -1.38f, 7.43f, -5.91f, 7.43f, -10.75f)
                    verticalLineToRelative(-4.7f)
                    arcToRelative(2.0f, 2.0f, 0.0f, false, false, -1.3f, -1.87f)
                    lineToRelative(-6.0f, -2.25f)
                    curveToRelative(-0.45f, -0.18f, -0.95f, -0.18f, -1.4f, -0.01f)
                    close()
                }
            }.build()
            return _shield!!
        }

    private var _speed: ImageVector? = null
    val Speed: ImageVector
        get() {
            if (_speed != null) return _speed!!
            _speed = ImageVector.Builder(name = "Speed", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(19.46f, 10.0f)
                    arcToRelative(1.0f, 1.0f, 0.0f, false, false, -0.07f, 1.0f)
                    arcToRelative(7.55f, 7.55f, 0.0f, false, true, 0.52f, 1.81f)
                    arcToRelative(8.0f, 8.0f, 0.0f, false, true, -0.69f, 4.73f)
                    arcToRelative(1.0f, 1.0f, 0.0f, false, true, -0.89f, 0.53f)
                    horizontalLineTo(5.68f)
                    arcToRelative(1.0f, 1.0f, 0.0f, false, true, -0.89f, -0.54f)
                    arcTo(8.0f, 8.0f, 0.0f, false, true, 13.0f, 6.06f)
                    arcToRelative(7.69f, 7.69f, 0.0f, false, true, 2.11f, 0.56f)
                    arcToRelative(1.0f, 1.0f, 0.0f, false, false, 1.0f, -0.07f)
                    arcToRelative(1.0f, 1.0f, 0.0f, false, false, -0.17f, -1.76f)
                    arcTo(10.0f, 10.0f, 0.0f, false, false, 3.35f, 19.0f)
                    arcToRelative(2.0f, 2.0f, 0.0f, false, false, 1.72f, 1.0f)
                    horizontalLineToRelative(13.85f)
                    arcToRelative(2.0f, 2.0f, 0.0f, false, false, 1.74f, -1.0f)
                    arcToRelative(10.0f, 10.0f, 0.0f, false, false, 0.55f, -8.89f)
                    arcToRelative(1.0f, 1.0f, 0.0f, false, false, -1.75f, -0.11f)
                    close()
                }
                path(fill = SolidColor(Color.Black)) {
                    moveTo(10.59f, 12.59f)
                    arcToRelative(2.0f, 2.0f, 0.0f, false, false, 2.83f, 2.83f)
                    lineToRelative(5.66f, -8.49f)
                    close()
                }
            }.build()
            return _speed!!
        }

    private var _star: ImageVector? = null
    val Star: ImageVector
        get() {
            if (_star != null) return _star!!
            _star = ImageVector.Builder(name = "Star", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveToRelative(12.0f, 17.27f)
                    lineToRelative(4.15f, 2.51f)
                    curveToRelative(0.76f, 0.46f, 1.69f, -0.22f, 1.49f, -1.08f)
                    lineToRelative(-1.1f, -4.72f)
                    lineToRelative(3.67f, -3.18f)
                    curveToRelative(0.67f, -0.58f, 0.31f, -1.68f, -0.57f, -1.75f)
                    lineToRelative(-4.83f, -0.41f)
                    lineToRelative(-1.89f, -4.46f)
                    curveToRelative(-0.34f, -0.81f, -1.5f, -0.81f, -1.84f, 0.0f)
                    lineTo(9.19f, 8.63f)
                    lineToRelative(-4.83f, 0.41f)
                    curveToRelative(-0.88f, 0.07f, -1.24f, 1.17f, -0.57f, 1.75f)
                    lineToRelative(3.67f, 3.18f)
                    lineToRelative(-1.1f, 4.72f)
                    curveToRelative(-0.2f, 0.86f, 0.73f, 1.54f, 1.49f, 1.08f)
                    lineToRelative(4.15f, -2.5f)
                    close()
                }
            }.build()
            return _star!!
        }

    private var _subtitles: ImageVector? = null
    val Subtitles: ImageVector
        get() {
            if (_subtitles != null) return _subtitles!!
            _subtitles = ImageVector.Builder(name = "Subtitles", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(20.0f, 4.0f)
                    horizontalLineTo(4.0f)
                    curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                    verticalLineToRelative(12.0f)
                    curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                    horizontalLineToRelative(16.0f)
                    curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                    verticalLineTo(6.0f)
                    curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                    close()
                    moveTo(5.0f, 12.0f)
                    horizontalLineToRelative(2.0f)
                    curveToRelative(0.55f, 0.0f, 1.0f, 0.45f, 1.0f, 1.0f)
                    reflectiveCurveToRelative(-0.45f, 1.0f, -1.0f, 1.0f)
                    horizontalLineTo(5.0f)
                    curveToRelative(-0.55f, 0.0f, -1.0f, -0.45f, -1.0f, -1.0f)
                    reflectiveCurveToRelative(0.45f, -1.0f, 1.0f, -1.0f)
                    close()
                    moveToRelative(8.0f, 6.0f)
                    horizontalLineTo(5.0f)
                    curveToRelative(-0.55f, 0.0f, -1.0f, -0.45f, -1.0f, -1.0f)
                    reflectiveCurveToRelative(0.45f, -1.0f, 1.0f, -1.0f)
                    horizontalLineToRelative(8.0f)
                    curveToRelative(0.55f, 0.0f, 1.0f, 0.45f, 1.0f, 1.0f)
                    reflectiveCurveToRelative(-0.45f, 1.0f, -1.0f, 1.0f)
                    close()
                    moveToRelative(6.0f, 0.0f)
                    horizontalLineToRelative(-2.0f)
                    curveToRelative(-0.55f, 0.0f, -1.0f, -0.45f, -1.0f, -1.0f)
                    reflectiveCurveToRelative(0.45f, -1.0f, 1.0f, -1.0f)
                    horizontalLineToRelative(2.0f)
                    curveToRelative(0.55f, 0.0f, 1.0f, 0.45f, 1.0f, 1.0f)
                    reflectiveCurveToRelative(-0.45f, 1.0f, -1.0f, 1.0f)
                    close()
                    moveToRelative(0.0f, -4.0f)
                    horizontalLineToRelative(-8.0f)
                    curveToRelative(-0.55f, 0.0f, -1.0f, -0.45f, -1.0f, -1.0f)
                    reflectiveCurveToRelative(0.45f, -1.0f, 1.0f, -1.0f)
                    horizontalLineToRelative(8.0f)
                    curveToRelative(0.55f, 0.0f, 1.0f, 0.45f, 1.0f, 1.0f)
                    reflectiveCurveToRelative(-0.45f, 1.0f, -1.0f, 1.0f)
                    close()
                }
            }.build()
            return _subtitles!!
        }

    private var _systemUpdate: ImageVector? = null
    val SystemUpdate: ImageVector
        get() {
            if (_systemUpdate != null) return _systemUpdate!!
            _systemUpdate = ImageVector.Builder(name = "SystemUpdate", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(17.0f, 1.01f)
                    lineTo(7.0f, 1.0f)
                    curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                    verticalLineToRelative(18.0f)
                    curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                    horizontalLineToRelative(10.0f)
                    curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                    verticalLineTo(3.0f)
                    curveToRelative(0.0f, -1.1f, -0.9f, -1.99f, -2.0f, -1.99f)
                    close()
                    moveTo(17.0f, 19.0f)
                    horizontalLineTo(7.0f)
                    verticalLineTo(5.0f)
                    horizontalLineToRelative(10.0f)
                    verticalLineToRelative(14.0f)
                    close()
                    moveToRelative(-2.21f, -6.0f)
                    horizontalLineTo(13.0f)
                    verticalLineTo(9.0f)
                    curveToRelative(0.0f, -0.55f, -0.45f, -1.0f, -1.0f, -1.0f)
                    reflectiveCurveToRelative(-1.0f, 0.45f, -1.0f, 1.0f)
                    verticalLineToRelative(4.0f)
                    horizontalLineTo(9.21f)
                    curveToRelative(-0.45f, 0.0f, -0.67f, 0.54f, -0.35f, 0.85f)
                    lineToRelative(2.79f, 2.79f)
                    curveToRelative(0.2f, 0.2f, 0.51f, 0.2f, 0.71f, 0.0f)
                    lineToRelative(2.79f, -2.79f)
                    arcToRelative(0.5f, 0.5f, 0.0f, false, false, -0.36f, -0.85f)
                    close()
                }
            }.build()
            return _systemUpdate!!
        }

    private var _tab: ImageVector? = null
    val Tab: ImageVector
        get() {
            if (_tab != null) return _tab!!
            _tab = ImageVector.Builder(name = "Tab", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(21.0f, 3.0f)
                    horizontalLineTo(3.0f)
                    curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                    verticalLineToRelative(14.0f)
                    curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                    horizontalLineToRelative(18.0f)
                    curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                    verticalLineTo(5.0f)
                    curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                    close()
                    moveToRelative(-1.0f, 16.0f)
                    horizontalLineTo(4.0f)
                    curveToRelative(-0.55f, 0.0f, -1.0f, -0.45f, -1.0f, -1.0f)
                    verticalLineTo(6.0f)
                    curveToRelative(0.0f, -0.55f, 0.45f, -1.0f, 1.0f, -1.0f)
                    horizontalLineToRelative(9.0f)
                    verticalLineToRelative(3.0f)
                    curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                    horizontalLineToRelative(7.0f)
                    verticalLineToRelative(9.0f)
                    curveToRelative(0.0f, 0.55f, -0.45f, 1.0f, -1.0f, 1.0f)
                    close()
                }
            }.build()
            return _tab!!
        }

    private var _tune: ImageVector? = null
    val Tune: ImageVector
        get() {
            if (_tune != null) return _tune!!
            _tune = ImageVector.Builder(name = "Tune", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(3.0f, 18.0f)
                    curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                    horizontalLineToRelative(5.0f)
                    verticalLineToRelative(-2.0f)
                    horizontalLineTo(4.0f)
                    curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                    close()
                    moveTo(3.0f, 6.0f)
                    curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                    horizontalLineToRelative(9.0f)
                    verticalLineTo(5.0f)
                    horizontalLineTo(4.0f)
                    curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                    close()
                    moveToRelative(10.0f, 14.0f)
                    verticalLineToRelative(-1.0f)
                    horizontalLineToRelative(7.0f)
                    curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                    reflectiveCurveToRelative(-0.45f, -1.0f, -1.0f, -1.0f)
                    horizontalLineToRelative(-7.0f)
                    verticalLineToRelative(-1.0f)
                    curveToRelative(0.0f, -0.55f, -0.45f, -1.0f, -1.0f, -1.0f)
                    reflectiveCurveToRelative(-1.0f, 0.45f, -1.0f, 1.0f)
                    verticalLineToRelative(4.0f)
                    curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                    reflectiveCurveToRelative(1.0f, -0.45f, 1.0f, -1.0f)
                    close()
                    moveTo(7.0f, 10.0f)
                    verticalLineToRelative(1.0f)
                    horizontalLineTo(4.0f)
                    curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                    reflectiveCurveToRelative(0.45f, 1.0f, 1.0f, 1.0f)
                    horizontalLineToRelative(3.0f)
                    verticalLineToRelative(1.0f)
                    curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                    reflectiveCurveToRelative(1.0f, -0.45f, 1.0f, -1.0f)
                    verticalLineToRelative(-4.0f)
                    curveToRelative(0.0f, -0.55f, -0.45f, -1.0f, -1.0f, -1.0f)
                    reflectiveCurveToRelative(-1.0f, 0.45f, -1.0f, 1.0f)
                    close()
                    moveToRelative(14.0f, 2.0f)
                    curveToRelative(0.0f, -0.55f, -0.45f, -1.0f, -1.0f, -1.0f)
                    horizontalLineToRelative(-9.0f)
                    verticalLineToRelative(2.0f)
                    horizontalLineToRelative(9.0f)
                    curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                    close()
                    moveToRelative(-5.0f, -3.0f)
                    curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                    verticalLineTo(7.0f)
                    horizontalLineToRelative(3.0f)
                    curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                    reflectiveCurveToRelative(-0.45f, -1.0f, -1.0f, -1.0f)
                    horizontalLineToRelative(-3.0f)
                    verticalLineTo(4.0f)
                    curveToRelative(0.0f, -0.55f, -0.45f, -1.0f, -1.0f, -1.0f)
                    reflectiveCurveToRelative(-1.0f, 0.45f, -1.0f, 1.0f)
                    verticalLineToRelative(4.0f)
                    curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                    close()
                }
            }.build()
            return _tune!!
        }

    private var _verifiedUser: ImageVector? = null
    val VerifiedUser: ImageVector
        get() {
            if (_verifiedUser != null) return _verifiedUser!!
            _verifiedUser = ImageVector.Builder(name = "VerifiedUser", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveToRelative(11.19f, 1.36f)
                    lineToRelative(-7.0f, 3.11f)
                    curveTo(3.47f, 4.79f, 3.0f, 5.51f, 3.0f, 6.3f)
                    verticalLineTo(11.0f)
                    curveToRelative(0.0f, 5.55f, 3.84f, 10.74f, 9.0f, 12.0f)
                    curveToRelative(5.16f, -1.26f, 9.0f, -6.45f, 9.0f, -12.0f)
                    verticalLineTo(6.3f)
                    curveToRelative(0.0f, -0.79f, -0.47f, -1.51f, -1.19f, -1.83f)
                    lineToRelative(-7.0f, -3.11f)
                    curveToRelative(-0.51f, -0.23f, -1.11f, -0.23f, -1.62f, 0.0f)
                    close()
                    moveToRelative(-1.9f, 14.93f)
                    lineTo(6.7f, 13.7f)
                    arcToRelative(0.996f, 0.996f, 0.0f, true, true, 1.41f, -1.41f)
                    lineTo(10.0f, 14.17f)
                    lineToRelative(5.88f, -5.88f)
                    arcToRelative(0.996f, 0.996f, 0.0f, true, true, 1.41f, 1.41f)
                    lineToRelative(-6.59f, 6.59f)
                    arcToRelative(0.996f, 0.996f, 0.0f, false, true, -1.41f, 0.0f)
                    close()
                }
            }.build()
            return _verifiedUser!!
        }

    private var _videoFile: ImageVector? = null
    val VideoFile: ImageVector
        get() {
            if (_videoFile != null) return _videoFile!!
            _videoFile = ImageVector.Builder(name = "VideoFile", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(13.17f, 2.0f)
                    horizontalLineTo(6.01f)
                    arcToRelative(2.0f, 2.0f, 0.0f, false, false, -2.0f, 2.0f)
                    lineTo(4.0f, 20.0f)
                    curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                    horizontalLineToRelative(12.0f)
                    curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                    verticalLineTo(8.83f)
                    curveToRelative(0.0f, -0.53f, -0.21f, -1.04f, -0.59f, -1.41f)
                    lineToRelative(-4.83f, -4.83f)
                    curveToRelative(-0.37f, -0.38f, -0.88f, -0.59f, -1.41f, -0.59f)
                    close()
                    moveTo(13.0f, 8.0f)
                    verticalLineTo(3.5f)
                    lineTo(18.5f, 9.0f)
                    horizontalLineTo(14.0f)
                    curveToRelative(-0.55f, 0.0f, -1.0f, -0.45f, -1.0f, -1.0f)
                    close()
                    moveToRelative(1.0f, 6.0f)
                    lineToRelative(1.27f, -0.67f)
                    curveToRelative(0.33f, -0.18f, 0.73f, 0.06f, 0.73f, 0.44f)
                    verticalLineToRelative(2.46f)
                    curveToRelative(0.0f, 0.38f, -0.4f, 0.62f, -0.73f, 0.44f)
                    lineTo(14.0f, 16.0f)
                    verticalLineToRelative(1.0f)
                    curveToRelative(0.0f, 0.55f, -0.45f, 1.0f, -1.0f, 1.0f)
                    horizontalLineTo(9.0f)
                    curveToRelative(-0.55f, 0.0f, -1.0f, -0.45f, -1.0f, -1.0f)
                    verticalLineToRelative(-4.0f)
                    curveToRelative(0.0f, -0.55f, 0.45f, -1.0f, 1.0f, -1.0f)
                    horizontalLineToRelative(4.0f)
                    curveToRelative(0.55f, 0.0f, 1.0f, 0.45f, 1.0f, 1.0f)
                    verticalLineToRelative(1.0f)
                    close()
                }
            }.build()
            return _videoFile!!
        }

    private var _visibilityOff: ImageVector? = null
    val VisibilityOff: ImageVector
        get() {
            if (_visibilityOff != null) return _visibilityOff!!
            _visibilityOff = ImageVector.Builder(name = "VisibilityOff", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(12.0f, 6.5f)
                    curveToRelative(2.76f, 0.0f, 5.0f, 2.24f, 5.0f, 5.0f)
                    curveToRelative(0.0f, 0.51f, -0.1f, 1.0f, -0.24f, 1.46f)
                    lineToRelative(3.06f, 3.06f)
                    curveToRelative(1.39f, -1.23f, 2.49f, -2.77f, 3.18f, -4.53f)
                    curveTo(21.27f, 7.11f, 17.0f, 4.0f, 12.0f, 4.0f)
                    curveToRelative(-1.27f, 0.0f, -2.49f, 0.2f, -3.64f, 0.57f)
                    lineToRelative(2.17f, 2.17f)
                    curveToRelative(0.47f, -0.14f, 0.96f, -0.24f, 1.47f, -0.24f)
                    close()
                    moveTo(2.71f, 3.16f)
                    arcToRelative(0.996f, 0.996f, 0.0f, false, false, 0.0f, 1.41f)
                    lineToRelative(1.97f, 1.97f)
                    arcTo(11.892f, 11.892f, 0.0f, false, false, 1.0f, 11.5f)
                    curveTo(2.73f, 15.89f, 7.0f, 19.0f, 12.0f, 19.0f)
                    curveToRelative(1.52f, 0.0f, 2.97f, -0.3f, 4.31f, -0.82f)
                    lineToRelative(2.72f, 2.72f)
                    arcToRelative(0.996f, 0.996f, 0.0f, true, false, 1.41f, -1.41f)
                    lineTo(4.13f, 3.16f)
                    curveToRelative(-0.39f, -0.39f, -1.03f, -0.39f, -1.42f, 0.0f)
                    close()
                    moveTo(12.0f, 16.5f)
                    curveToRelative(-2.76f, 0.0f, -5.0f, -2.24f, -5.0f, -5.0f)
                    curveToRelative(0.0f, -0.77f, 0.18f, -1.5f, 0.49f, -2.14f)
                    lineToRelative(1.57f, 1.57f)
                    curveToRelative(-0.03f, 0.18f, -0.06f, 0.37f, -0.06f, 0.57f)
                    curveToRelative(0.0f, 1.66f, 1.34f, 3.0f, 3.0f, 3.0f)
                    curveToRelative(0.2f, 0.0f, 0.38f, -0.03f, 0.57f, -0.07f)
                    lineTo(14.14f, 16.0f)
                    curveToRelative(-0.65f, 0.32f, -1.37f, 0.5f, -2.14f, 0.5f)
                    close()
                    moveToRelative(2.97f, -5.33f)
                    arcToRelative(2.97f, 2.97f, 0.0f, false, false, -2.64f, -2.64f)
                    lineToRelative(2.64f, 2.64f)
                    close()
                }
            }.build()
            return _visibilityOff!!
        }

    private var _warning: ImageVector? = null
    val Warning: ImageVector
        get() {
            if (_warning != null) return _warning!!
            _warning = ImageVector.Builder(name = "Warning", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(4.47f, 21.0f)
                    horizontalLineToRelative(15.06f)
                    curveToRelative(1.54f, 0.0f, 2.5f, -1.67f, 1.73f, -3.0f)
                    lineTo(13.73f, 4.99f)
                    curveToRelative(-0.77f, -1.33f, -2.69f, -1.33f, -3.46f, 0.0f)
                    lineTo(2.74f, 18.0f)
                    curveToRelative(-0.77f, 1.33f, 0.19f, 3.0f, 1.73f, 3.0f)
                    close()
                    moveTo(12.0f, 14.0f)
                    curveToRelative(-0.55f, 0.0f, -1.0f, -0.45f, -1.0f, -1.0f)
                    verticalLineToRelative(-2.0f)
                    curveToRelative(0.0f, -0.55f, 0.45f, -1.0f, 1.0f, -1.0f)
                    reflectiveCurveToRelative(1.0f, 0.45f, 1.0f, 1.0f)
                    verticalLineToRelative(2.0f)
                    curveToRelative(0.0f, 0.55f, -0.45f, 1.0f, -1.0f, 1.0f)
                    close()
                    moveToRelative(1.0f, 4.0f)
                    horizontalLineToRelative(-2.0f)
                    verticalLineToRelative(-2.0f)
                    horizontalLineToRelative(2.0f)
                    verticalLineToRelative(2.0f)
                    close()
                }
            }.build()
            return _warning!!
        }

    private var _wifi: ImageVector? = null
    val Wifi: ImageVector
        get() {
            if (_wifi != null) return _wifi!!
            _wifi = ImageVector.Builder(name = "Wifi", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(2.06f, 10.06f)
                    curveToRelative(0.51f, 0.51f, 1.32f, 0.56f, 1.87f, 0.1f)
                    curveToRelative(4.67f, -3.84f, 11.45f, -3.84f, 16.13f, -0.01f)
                    curveToRelative(0.56f, 0.46f, 1.38f, 0.42f, 1.89f, -0.09f)
                    curveToRelative(0.59f, -0.59f, 0.55f, -1.57f, -0.1f, -2.1f)
                    curveToRelative(-5.71f, -4.67f, -13.97f, -4.67f, -19.69f, 0.0f)
                    curveToRelative(-0.65f, 0.52f, -0.7f, 1.5f, -0.1f, 2.1f)
                    close()
                    moveToRelative(7.76f, 7.76f)
                    lineToRelative(1.47f, 1.47f)
                    curveToRelative(0.39f, 0.39f, 1.02f, 0.39f, 1.41f, 0.0f)
                    lineToRelative(1.47f, -1.47f)
                    curveToRelative(0.47f, -0.47f, 0.37f, -1.28f, -0.23f, -1.59f)
                    arcToRelative(4.28f, 4.28f, 0.0f, false, false, -3.91f, 0.0f)
                    curveToRelative(-0.57f, 0.31f, -0.68f, 1.12f, -0.21f, 1.59f)
                    close()
                    moveToRelative(-3.73f, -3.73f)
                    curveToRelative(0.49f, 0.49f, 1.26f, 0.54f, 1.83f, 0.13f)
                    arcToRelative(7.064f, 7.064f, 0.0f, false, true, 8.16f, 0.0f)
                    curveToRelative(0.57f, 0.4f, 1.34f, 0.36f, 1.83f, -0.13f)
                    lineToRelative(0.01f, -0.01f)
                    curveToRelative(0.6f, -0.6f, 0.56f, -1.62f, -0.13f, -2.11f)
                    curveToRelative(-3.44f, -2.49f, -8.13f, -2.49f, -11.58f, 0.0f)
                    curveToRelative(-0.69f, 0.5f, -0.73f, 1.51f, -0.12f, 2.12f)
                    close()
                }
            }.build()
            return _wifi!!
        }

    object Outlined {
        private var _outlined_bookmarks: ImageVector? = null
        val Bookmarks: ImageVector
            get() {
                if (_outlined_bookmarks != null) return _outlined_bookmarks!!
                _outlined_bookmarks = ImageVector.Builder(name = "outlined_Bookmarks", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                    path(fill = SolidColor(Color.Black)) {
                        moveTo(15.0f, 7.0f)
                        verticalLineToRelative(12.97f)
                        lineToRelative(-4.21f, -1.81f)
                        lineToRelative(-0.79f, -0.34f)
                        lineToRelative(-0.79f, 0.34f)
                        lineTo(5.0f, 19.97f)
                        verticalLineTo(7.0f)
                        horizontalLineToRelative(10.0f)
                        moveToRelative(4.0f, -6.0f)
                        horizontalLineTo(8.99f)
                        curveTo(7.89f, 1.0f, 7.0f, 1.9f, 7.0f, 3.0f)
                        horizontalLineToRelative(10.0f)
                        curveToRelative(1.1f, 0.0f, 2.0f, 0.9f, 2.0f, 2.0f)
                        verticalLineToRelative(13.0f)
                        lineToRelative(2.0f, 1.0f)
                        verticalLineTo(3.0f)
                        curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                        close()
                        moveToRelative(-4.0f, 4.0f)
                        horizontalLineTo(5.0f)
                        curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                        verticalLineToRelative(16.0f)
                        lineToRelative(7.0f, -3.0f)
                        lineToRelative(7.0f, 3.0f)
                        verticalLineTo(7.0f)
                        curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                        close()
                    }
                }.build()
                return _outlined_bookmarks!!
            }

        private var _outlined_delete: ImageVector? = null
        val Delete: ImageVector
            get() {
                if (_outlined_delete != null) return _outlined_delete!!
                _outlined_delete = ImageVector.Builder(name = "outlined_Delete", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                    path(fill = SolidColor(Color.Black)) {
                        moveTo(16.0f, 9.0f)
                        verticalLineToRelative(10.0f)
                        horizontalLineTo(8.0f)
                        verticalLineTo(9.0f)
                        horizontalLineToRelative(8.0f)
                        moveToRelative(-1.5f, -6.0f)
                        horizontalLineToRelative(-5.0f)
                        lineToRelative(-1.0f, 1.0f)
                        horizontalLineTo(5.0f)
                        verticalLineToRelative(2.0f)
                        horizontalLineToRelative(14.0f)
                        verticalLineTo(4.0f)
                        horizontalLineToRelative(-3.5f)
                        lineToRelative(-1.0f, -1.0f)
                        close()
                        moveTo(18.0f, 7.0f)
                        horizontalLineTo(6.0f)
                        verticalLineToRelative(12.0f)
                        curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                        horizontalLineToRelative(8.0f)
                        curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                        verticalLineTo(7.0f)
                        close()
                    }
                }.build()
                return _outlined_delete!!
            }

        private var _outlined_download: ImageVector? = null
        val Download: ImageVector
            get() {
                if (_outlined_download != null) return _outlined_download!!
                _outlined_download = ImageVector.Builder(name = "outlined_Download", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                    path(fill = SolidColor(Color.Black)) {
                        moveTo(19.0f, 9.0f)
                        horizontalLineToRelative(-4.0f)
                        verticalLineTo(3.0f)
                        horizontalLineTo(9.0f)
                        verticalLineToRelative(6.0f)
                        horizontalLineTo(5.0f)
                        lineToRelative(7.0f, 7.0f)
                        lineToRelative(7.0f, -7.0f)
                        close()
                        moveToRelative(-8.0f, 2.0f)
                        verticalLineTo(5.0f)
                        horizontalLineToRelative(2.0f)
                        verticalLineToRelative(6.0f)
                        horizontalLineToRelative(1.17f)
                        lineTo(12.0f, 13.17f)
                        lineTo(9.83f, 11.0f)
                        horizontalLineTo(11.0f)
                        close()
                        moveToRelative(-6.0f, 7.0f)
                        horizontalLineToRelative(14.0f)
                        verticalLineToRelative(2.0f)
                        horizontalLineTo(5.0f)
                        close()
                    }
                }.build()
                return _outlined_download!!
            }

        private var _outlined_privacyTip: ImageVector? = null
        val PrivacyTip: ImageVector
            get() {
                if (_outlined_privacyTip != null) return _outlined_privacyTip!!
                _outlined_privacyTip = ImageVector.Builder(name = "outlined_PrivacyTip", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                    path(fill = SolidColor(Color.Black)) {
                        moveToRelative(12.0f, 3.19f)
                        lineToRelative(7.0f, 3.11f)
                        verticalLineTo(11.0f)
                        curveToRelative(0.0f, 4.52f, -2.98f, 8.69f, -7.0f, 9.93f)
                        curveToRelative(-4.02f, -1.24f, -7.0f, -5.41f, -7.0f, -9.93f)
                        verticalLineTo(6.3f)
                        lineToRelative(7.0f, -3.11f)
                        moveTo(12.0f, 1.0f)
                        lineTo(3.0f, 5.0f)
                        verticalLineToRelative(6.0f)
                        curveToRelative(0.0f, 5.55f, 3.84f, 10.74f, 9.0f, 12.0f)
                        curveToRelative(5.16f, -1.26f, 9.0f, -6.45f, 9.0f, -12.0f)
                        verticalLineTo(5.0f)
                        lineToRelative(-9.0f, -4.0f)
                        close()
                        moveToRelative(-1.0f, 6.0f)
                        horizontalLineToRelative(2.0f)
                        verticalLineToRelative(2.0f)
                        horizontalLineToRelative(-2.0f)
                        verticalLineTo(7.0f)
                        close()
                        moveToRelative(0.0f, 4.0f)
                        horizontalLineToRelative(2.0f)
                        verticalLineToRelative(6.0f)
                        horizontalLineToRelative(-2.0f)
                        verticalLineToRelative(-6.0f)
                        close()
                    }
                }.build()
                return _outlined_privacyTip!!
            }

        private var _outlined_public: ImageVector? = null
        val Public: ImageVector
            get() {
                if (_outlined_public != null) return _outlined_public!!
                _outlined_public = ImageVector.Builder(name = "outlined_Public", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                    path(fill = SolidColor(Color.Black)) {
                        moveTo(12.0f, 2.0f)
                        curveTo(6.48f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f)
                        reflectiveCurveToRelative(4.48f, 10.0f, 10.0f, 10.0f)
                        reflectiveCurveToRelative(10.0f, -4.48f, 10.0f, -10.0f)
                        reflectiveCurveTo(17.52f, 2.0f, 12.0f, 2.0f)
                        close()
                        moveTo(4.0f, 12.0f)
                        curveToRelative(0.0f, -0.61f, 0.08f, -1.21f, 0.21f, -1.78f)
                        lineTo(8.99f, 15.0f)
                        verticalLineToRelative(1.0f)
                        curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                        verticalLineToRelative(1.93f)
                        curveTo(7.06f, 19.43f, 4.0f, 16.07f, 4.0f, 12.0f)
                        close()
                        moveToRelative(13.89f, 5.4f)
                        curveToRelative(-0.26f, -0.81f, -1.0f, -1.4f, -1.9f, -1.4f)
                        horizontalLineToRelative(-1.0f)
                        verticalLineToRelative(-3.0f)
                        curveToRelative(0.0f, -0.55f, -0.45f, -1.0f, -1.0f, -1.0f)
                        horizontalLineToRelative(-6.0f)
                        verticalLineToRelative(-2.0f)
                        horizontalLineToRelative(2.0f)
                        curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                        verticalLineTo(7.0f)
                        horizontalLineToRelative(2.0f)
                        curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                        verticalLineToRelative(-0.41f)
                        curveTo(17.92f, 5.77f, 20.0f, 8.65f, 20.0f, 12.0f)
                        curveToRelative(0.0f, 2.08f, -0.81f, 3.98f, -2.11f, 5.4f)
                        close()
                    }
                }.build()
                return _outlined_public!!
            }

        private var _outlined_settings: ImageVector? = null
        val Settings: ImageVector
            get() {
                if (_outlined_settings != null) return _outlined_settings!!
                _outlined_settings = ImageVector.Builder(name = "outlined_Settings", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                    path(fill = SolidColor(Color.Black)) {
                        moveTo(19.43f, 12.98f)
                        curveToRelative(0.04f, -0.32f, 0.07f, -0.64f, 0.07f, -0.98f)
                        curveToRelative(0.0f, -0.34f, -0.03f, -0.66f, -0.07f, -0.98f)
                        lineToRelative(2.11f, -1.65f)
                        curveToRelative(0.19f, -0.15f, 0.24f, -0.42f, 0.12f, -0.64f)
                        lineToRelative(-2.0f, -3.46f)
                        arcToRelative(0.5f, 0.5f, 0.0f, false, false, -0.61f, -0.22f)
                        lineToRelative(-2.49f, 1.0f)
                        curveToRelative(-0.52f, -0.4f, -1.08f, -0.73f, -1.69f, -0.98f)
                        lineToRelative(-0.38f, -2.65f)
                        arcTo(0.488f, 0.488f, 0.0f, false, false, 14.0f, 2.0f)
                        horizontalLineToRelative(-4.0f)
                        curveToRelative(-0.25f, 0.0f, -0.46f, 0.18f, -0.49f, 0.42f)
                        lineToRelative(-0.38f, 2.65f)
                        curveToRelative(-0.61f, 0.25f, -1.17f, 0.59f, -1.69f, 0.98f)
                        lineToRelative(-2.49f, -1.0f)
                        arcToRelative(0.566f, 0.566f, 0.0f, false, false, -0.18f, -0.03f)
                        curveToRelative(-0.17f, 0.0f, -0.34f, 0.09f, -0.43f, 0.25f)
                        lineToRelative(-2.0f, 3.46f)
                        curveToRelative(-0.13f, 0.22f, -0.07f, 0.49f, 0.12f, 0.64f)
                        lineToRelative(2.11f, 1.65f)
                        curveToRelative(-0.04f, 0.32f, -0.07f, 0.65f, -0.07f, 0.98f)
                        curveToRelative(0.0f, 0.33f, 0.03f, 0.66f, 0.07f, 0.98f)
                        lineToRelative(-2.11f, 1.65f)
                        curveToRelative(-0.19f, 0.15f, -0.24f, 0.42f, -0.12f, 0.64f)
                        lineToRelative(2.0f, 3.46f)
                        arcToRelative(0.5f, 0.5f, 0.0f, false, false, 0.61f, 0.22f)
                        lineToRelative(2.49f, -1.0f)
                        curveToRelative(0.52f, 0.4f, 1.08f, 0.73f, 1.69f, 0.98f)
                        lineToRelative(0.38f, 2.65f)
                        curveToRelative(0.03f, 0.24f, 0.24f, 0.42f, 0.49f, 0.42f)
                        horizontalLineToRelative(4.0f)
                        curveToRelative(0.25f, 0.0f, 0.46f, -0.18f, 0.49f, -0.42f)
                        lineToRelative(0.38f, -2.65f)
                        curveToRelative(0.61f, -0.25f, 1.17f, -0.59f, 1.69f, -0.98f)
                        lineToRelative(2.49f, 1.0f)
                        curveToRelative(0.06f, 0.02f, 0.12f, 0.03f, 0.18f, 0.03f)
                        curveToRelative(0.17f, 0.0f, 0.34f, -0.09f, 0.43f, -0.25f)
                        lineToRelative(2.0f, -3.46f)
                        curveToRelative(0.12f, -0.22f, 0.07f, -0.49f, -0.12f, -0.64f)
                        lineToRelative(-2.11f, -1.65f)
                        close()
                        moveToRelative(-1.98f, -1.71f)
                        curveToRelative(0.04f, 0.31f, 0.05f, 0.52f, 0.05f, 0.73f)
                        curveToRelative(0.0f, 0.21f, -0.02f, 0.43f, -0.05f, 0.73f)
                        lineToRelative(-0.14f, 1.13f)
                        lineToRelative(0.89f, 0.7f)
                        lineToRelative(1.08f, 0.84f)
                        lineToRelative(-0.7f, 1.21f)
                        lineToRelative(-1.27f, -0.51f)
                        lineToRelative(-1.04f, -0.42f)
                        lineToRelative(-0.9f, 0.68f)
                        curveToRelative(-0.43f, 0.32f, -0.84f, 0.56f, -1.25f, 0.73f)
                        lineToRelative(-1.06f, 0.43f)
                        lineToRelative(-0.16f, 1.13f)
                        lineToRelative(-0.2f, 1.35f)
                        horizontalLineToRelative(-1.4f)
                        lineToRelative(-0.19f, -1.35f)
                        lineToRelative(-0.16f, -1.13f)
                        lineToRelative(-1.06f, -0.43f)
                        curveToRelative(-0.43f, -0.18f, -0.83f, -0.41f, -1.23f, -0.71f)
                        lineToRelative(-0.91f, -0.7f)
                        lineToRelative(-1.06f, 0.43f)
                        lineToRelative(-1.27f, 0.51f)
                        lineToRelative(-0.7f, -1.21f)
                        lineToRelative(1.08f, -0.84f)
                        lineToRelative(0.89f, -0.7f)
                        lineToRelative(-0.14f, -1.13f)
                        curveToRelative(-0.03f, -0.31f, -0.05f, -0.54f, -0.05f, -0.74f)
                        reflectiveCurveToRelative(0.02f, -0.43f, 0.05f, -0.73f)
                        lineToRelative(0.14f, -1.13f)
                        lineToRelative(-0.89f, -0.7f)
                        lineToRelative(-1.08f, -0.84f)
                        lineToRelative(0.7f, -1.21f)
                        lineToRelative(1.27f, 0.51f)
                        lineToRelative(1.04f, 0.42f)
                        lineToRelative(0.9f, -0.68f)
                        curveToRelative(0.43f, -0.32f, 0.84f, -0.56f, 1.25f, -0.73f)
                        lineToRelative(1.06f, -0.43f)
                        lineToRelative(0.16f, -1.13f)
                        lineToRelative(0.2f, -1.35f)
                        horizontalLineToRelative(1.39f)
                        lineToRelative(0.19f, 1.35f)
                        lineToRelative(0.16f, 1.13f)
                        lineToRelative(1.06f, 0.43f)
                        curveToRelative(0.43f, 0.18f, 0.83f, 0.41f, 1.23f, 0.71f)
                        lineToRelative(0.91f, 0.7f)
                        lineToRelative(1.06f, -0.43f)
                        lineToRelative(1.27f, -0.51f)
                        lineToRelative(0.7f, 1.21f)
                        lineToRelative(-1.07f, 0.85f)
                        lineToRelative(-0.89f, 0.7f)
                        lineToRelative(0.14f, 1.13f)
                        close()
                        moveTo(12.0f, 8.0f)
                        curveToRelative(-2.21f, 0.0f, -4.0f, 1.79f, -4.0f, 4.0f)
                        reflectiveCurveToRelative(1.79f, 4.0f, 4.0f, 4.0f)
                        reflectiveCurveToRelative(4.0f, -1.79f, 4.0f, -4.0f)
                        reflectiveCurveToRelative(-1.79f, -4.0f, -4.0f, -4.0f)
                        close()
                        moveToRelative(0.0f, 6.0f)
                        curveToRelative(-1.1f, 0.0f, -2.0f, -0.9f, -2.0f, -2.0f)
                        reflectiveCurveToRelative(0.9f, -2.0f, 2.0f, -2.0f)
                        reflectiveCurveToRelative(2.0f, 0.9f, 2.0f, 2.0f)
                        reflectiveCurveToRelative(-0.9f, 2.0f, -2.0f, 2.0f)
                        close()
                    }
                }.build()
                return _outlined_settings!!
            }

        private var _outlined_shield: ImageVector? = null
        val Shield: ImageVector
            get() {
                if (_outlined_shield != null) return _outlined_shield!!
                _outlined_shield = ImageVector.Builder(name = "outlined_Shield", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                    path(fill = SolidColor(Color.Black)) {
                        moveTo(12.0f, 2.0f)
                        lineTo(4.0f, 5.0f)
                        verticalLineToRelative(6.09f)
                        curveToRelative(0.0f, 5.05f, 3.41f, 9.76f, 8.0f, 10.91f)
                        curveToRelative(4.59f, -1.15f, 8.0f, -5.86f, 8.0f, -10.91f)
                        verticalLineTo(5.0f)
                        lineToRelative(-8.0f, -3.0f)
                        close()
                        moveToRelative(6.0f, 9.09f)
                        curveToRelative(0.0f, 4.0f, -2.55f, 7.7f, -6.0f, 8.83f)
                        curveToRelative(-3.45f, -1.13f, -6.0f, -4.82f, -6.0f, -8.83f)
                        verticalLineToRelative(-4.7f)
                        lineToRelative(6.0f, -2.25f)
                        lineToRelative(6.0f, 2.25f)
                        verticalLineToRelative(4.7f)
                        close()
                    }
                }.build()
                return _outlined_shield!!
            }

        private var _outlined_starBorder: ImageVector? = null
        val StarBorder: ImageVector
            get() {
                if (_outlined_starBorder != null) return _outlined_starBorder!!
                _outlined_starBorder = ImageVector.Builder(name = "outlined_StarBorder", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                    path(fill = SolidColor(Color.Black)) {
                        moveToRelative(22.0f, 9.24f)
                        lineToRelative(-7.19f, -0.62f)
                        lineTo(12.0f, 2.0f)
                        lineTo(9.19f, 8.63f)
                        lineTo(2.0f, 9.24f)
                        lineToRelative(5.46f, 4.73f)
                        lineTo(5.82f, 21.0f)
                        lineTo(12.0f, 17.27f)
                        lineTo(18.18f, 21.0f)
                        lineToRelative(-1.63f, -7.03f)
                        lineTo(22.0f, 9.24f)
                        close()
                        moveTo(12.0f, 15.4f)
                        lineToRelative(-3.76f, 2.27f)
                        lineToRelative(1.0f, -4.28f)
                        lineToRelative(-3.32f, -2.88f)
                        lineToRelative(4.38f, -0.38f)
                        lineTo(12.0f, 6.1f)
                        lineToRelative(1.71f, 4.04f)
                        lineToRelative(4.38f, 0.38f)
                        lineToRelative(-3.32f, 2.88f)
                        lineToRelative(1.0f, 4.28f)
                        lineTo(12.0f, 15.4f)
                        close()
                    }
                }.build()
                return _outlined_starBorder!!
            }

        private var _outlined_tab: ImageVector? = null
        val Tab: ImageVector
            get() {
                if (_outlined_tab != null) return _outlined_tab!!
                _outlined_tab = ImageVector.Builder(name = "outlined_Tab", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
                    path(fill = SolidColor(Color.Black)) {
                        moveTo(21.0f, 3.0f)
                        horizontalLineTo(3.0f)
                        curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                        verticalLineToRelative(14.0f)
                        curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                        horizontalLineToRelative(18.0f)
                        curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                        verticalLineTo(5.0f)
                        curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                        close()
                        moveToRelative(0.0f, 16.0f)
                        horizontalLineTo(3.0f)
                        verticalLineTo(5.0f)
                        horizontalLineToRelative(10.0f)
                        verticalLineToRelative(4.0f)
                        horizontalLineToRelative(8.0f)
                        verticalLineToRelative(10.0f)
                        close()
                    }
                }.build()
                return _outlined_tab!!
            }

    }
}
