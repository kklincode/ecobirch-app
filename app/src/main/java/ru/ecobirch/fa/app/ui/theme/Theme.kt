package ru.ecobirch.fa.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

object EcoColors {
    // Primary botanical palette
    val Emerald600 = Color(0xFF059669)
    val Emerald700 = Color(0xFF047857)
    val Emerald800 = Color(0xFF065F46)
    val Emerald900 = Color(0xFF064E3B)
    val Emerald50 = Color(0xFFECFDF5)
    val Emerald100 = Color(0xFFD1FAE5)

    // Modern warm botanical neutrals (softer, richer than sterile cool slate)
    val CanvasBg = Color(0xFFF3F6F4)
    val SurfaceTonal = Color(0xFFE9F0EC)
    val SurfaceTonalAlt = Color(0xFFE1EAE4)
    val Hairline = Color(0xFFDCE5DF)

    val Slate50 = CanvasBg
    val Slate100 = Color(0xFFEAF0EC)
    val Slate200 = Color(0xFFDCE4DF)
    val Slate400 = Color(0xFF8B9992)
    val Slate500 = Color(0xFF56655E)
    val Slate700 = Color(0xFF2F3D37)
    val Slate900 = Color(0xFF101D17)
    val Slate950 = Color(0xFF07110D)

    // Zakharov 5-point scale colors
    val Score1 = Color(0xFF10B981) // < 0.040
    val Score2 = Color(0xFFEAB308) // 0.040 - 0.044
    val Score3 = Color(0xFFF59E0B) // 0.045 - 0.049
    val Score4 = Color(0xFFF43F5E) // 0.050 - 0.054
    val Score5 = Color(0xFF991B1B) // > 0.054

    // Soft tinted backgrounds for Score Hero cards
    val Score1Bg = Color(0xFFE6FAF3)
    val Score2Bg = Color(0xFFFEF9E3)
    val Score3Bg = Color(0xFFFEF3E2)
    val Score4Bg = Color(0xFFFFEBEF)
    val Score5Bg = Color(0xFFFCE8E8)

    fun scoreColor(score: Int?): Color = when (score) {
        1 -> Score1
        2 -> Score2
        3 -> Score3
        4 -> Score4
        5 -> Score5
        else -> Slate400
    }

    fun scoreSoftBg(score: Int?): Color = when (score) {
        1 -> Score1Bg
        2 -> Score2Bg
        3 -> Score3Bg
        4 -> Score4Bg
        5 -> Score5Bg
        else -> SurfaceTonal
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
    secondaryContainer = EcoColors.SurfaceTonal,
    onSecondaryContainer = EcoColors.Slate900,
    background = EcoColors.CanvasBg,
    onBackground = EcoColors.Slate900,
    surface = Color.White,
    onSurface = EcoColors.Slate900,
    surfaceVariant = EcoColors.SurfaceTonal,
    onSurfaceVariant = EcoColors.Slate700,
    outline = EcoColors.Hairline,
    outlineVariant = EcoColors.Slate200,
    error = EcoColors.Score4
)

private val AppTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Black,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.8).sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.2).sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.5.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun BetulaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
