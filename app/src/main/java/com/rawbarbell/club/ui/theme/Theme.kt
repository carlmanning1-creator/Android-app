package com.rawbarbell.club.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val RawBarbellColorScheme = darkColorScheme(
    primary = TealAccent,
    onPrimary = WhiteText,
    secondary = YellowHighlight,
    onSecondary = PurpleDark,
    tertiary = PurpleLight,
    background = PurpleDark,
    onBackground = WhiteText,
    surface = CardBackground,
    onSurface = WhiteText,
)

@Composable
fun RawBarbellClubTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RawBarbellColorScheme,
        typography = Typography,
        content = content
    )
}
