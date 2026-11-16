package ru.ecobirch.fa.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object EcoColors {
    val Emerald600 = Color(0xFF059669)
    val Emerald700 = Color(0xFF047857)
    val Emerald900 = Color(0xFF064E3B)
    val Emerald50 = Color(0xFFECFDF5)
    val Emerald100 = Color(0xFFD1FAE5)

    val Slate50 = Color(0xFFF8FAFC)
    val Slate100 = Color(0xFFF1F5F9)
    val Slate200 = Color(0xFFE2E8F0)
    val Slate400 = Color(0xFF94A3B8)
    val Slate500 = Color(0xFF64748B)
    val Slate700 = Color(0xFF334155)
    val Slate900 = Color(0xFF0F172A)
    val Slate950 = Color(0xFF020617)

    // Zakharov 5-point scale colors
    val Score1 = Color(0xFF10B981) // < 0.040
    val Score2 = Color(0xFFEAB308) // 0.040 - 0.044
    val Score3 = Color(0xFFF59E0B) // 0.045 - 0.049
    val Score4 = Color(0xFFF43F5E) // 0.050 - 0.054
    val Score5 = Color(0xFF991B1B) // > 0.054

    fun scoreColor(score: Int?): Color = when (score) {
        1 -> Score1
        2 -> Score2
        3 -> Score3
        4 -> Score4
        5 -> Score5
        else -> Slate400
    }

    fun scoreTextColor(score: Int?): Color = when (score) {
        2 -> Slate900
        1, 3, 4, 5 -> Color.White
        else -> Slate700
    }
}

private val LightColors = lightColorScheme(
    primary = EcoColors.Emerald600,
    onPrimary = Color.White,
    primaryContainer = EcoColors.Emerald100,
    onPrimaryContainer = EcoColors.Emerald900,
    secondary = EcoColors.Slate700,
    onSecondary = Color.White,
    background = EcoColors.Slate50,
    onBackground = EcoColors.Slate900,
    surface = Color.White,
    onSurface = EcoColors.Slate900,
    surfaceVariant = EcoColors.Slate100,
    onSurfaceVariant = EcoColors.Slate700,
    outline = EcoColors.Slate200,
    error = EcoColors.Score4
)

private val AppTypography = Typography(
    headlineSmall = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.3).sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp
    )
)

@Composable
fun BetulaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        content = content
    )
}
