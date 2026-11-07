package id.sch.mts241alikhlash.exam.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme =
    lightColorScheme(
        primary = BrandNavy,
        secondary = BrandGold,
        background = BrandBackground,
        surface = BrandSurface,
        surfaceVariant = BrandMuted,
        onPrimary = BrandSurface,
        onSecondary = TextPrimary,
        onBackground = TextPrimary,
        onSurface = TextPrimary,
        outline = BorderSubtle,
        outlineVariant = BorderInput,
        error = StatusError,
    )

@Composable
fun AssessmentTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = AppTypography,
        content = content,
    )
}

@Composable
fun ExamTheme(content: @Composable () -> Unit) {
    AssessmentTheme(content = content)
}
