package ru.uust.schedule.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * The UUST mark from the official brand guide (purple, #4D19CC).
 * Kept as a vector so it stays sharp at every window size.
 */
val UustLogoMark: ImageVector by lazy {
    ImageVector.Builder(
        name = "UustLogoMark",
        defaultWidth = 180.23.dp,
        defaultHeight = 164.65.dp,
        viewportWidth = 180.23f,
        viewportHeight = 164.65f,
    ).apply {
        path(fill = SolidColor(Color(0xFF4D19CC))) {
            moveTo(3.918f, 0.000f)
            lineTo(2.070f, 0.168f)
            lineTo(0.598f, 1.309f)
            lineTo(0.000f, 3.207f)
            lineTo(0.000f, 99.848f)
            lineTo(0.258f, 101.137f)
            lineTo(0.969f, 102.188f)
            lineTo(2.027f, 102.895f)
            lineTo(3.316f, 103.156f)
            lineTo(80.000f, 103.156f)
            lineTo(56.785f, 127.723f)
            lineTo(55.805f, 128.980f)
            lineTo(55.098f, 130.379f)
            lineTo(54.656f, 131.887f)
            lineTo(54.496f, 133.469f)
            lineTo(54.496f, 161.379f)
            lineTo(54.938f, 163.020f)
            lineTo(56.055f, 164.168f)
            lineTo(57.586f, 164.648f)
            lineTo(59.242f, 164.297f)
            lineTo(176.582f, 104.996f)
            lineTo(179.250f, 102.555f)
            lineTo(180.230f, 99.090f)
            lineTo(180.230f, 3.258f)
            lineTo(179.629f, 1.355f)
            lineTo(178.160f, 0.219f)
            lineTo(176.312f, 0.047f)
            lineTo(174.562f, 1.039f)
            lineTo(90.496f, 91.812f)
            lineTo(5.656f, 0.977f)
            close()
        }
    }.build()
}
