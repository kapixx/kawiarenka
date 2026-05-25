package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = SweetPeach,
    secondary = SoftGinger,
    tertiary = CoralPink,
    background = DarkCoffeeChoco,
    surface = DeepEspresso,
    onPrimary = Color.White,
    onSecondary = DeepEspresso,
    onTertiary = Color.White,
    onBackground = SweetCream,
    onSurface = SweetCream,
    surfaceVariant = SoftPlum,
    onSurfaceVariant = SweetCream
)

private val LightColorScheme = lightColorScheme(
    primary = SweetPeach,
    secondary = CookieBrown,
    tertiary = CoralPink,
    background = WarmLatte,
    surface = SweetCream,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = DeepEspresso,
    onSurface = DeepEspresso,
    surfaceVariant = CardboardGray,
    onSurfaceVariant = DeepEspresso
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Standardize on our beautiful cozy handcrafted custom colors
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
