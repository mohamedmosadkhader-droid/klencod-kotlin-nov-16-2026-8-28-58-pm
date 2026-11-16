package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val BareeqDarkColorScheme = darkColorScheme(
    primary = LuxuryGold,
    onPrimary = ObsidianBlack,
    primaryContainer = DeepBronzeGold,
    onPrimaryContainer = PearlIvory,
    secondary = MetallicSilver,
    onSecondary = ObsidianBlack,
    secondaryContainer = ElevatedCharcoal,
    onSecondaryContainer = ChampagneGold,
    tertiary = ChampagneGold,
    onTertiary = ObsidianBlack,
    background = ObsidianBlack,
    onBackground = PearlIvory,
    surface = DeepCarbon,
    onSurface = PearlIvory,
    surfaceVariant = CardSurface,
    onSurfaceVariant = MetallicSilver,
    outline = SubtleBorder,
    error = CrimsonAlert,
    onError = PearlIvory
)

val BareeqShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun BareeqTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = BareeqDarkColorScheme,
            typography = BareeqTypography,
            shapes = BareeqShapes,
            content = content
        )
    }
}
