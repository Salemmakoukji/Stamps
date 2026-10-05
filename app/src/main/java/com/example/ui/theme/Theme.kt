package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkBackground,
    primaryContainer = PostalNavy,
    onPrimaryContainer = Color.White,
    secondary = DarkSecondary,
    onSecondary = DarkBackground,
    secondaryContainer = TerracottaStamp,
    onSecondaryContainer = Color.White,
    tertiary = GoldenSun,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFF1F5F9),
    outline = Color(0xFF475569)
)

private val LightColorScheme = lightColorScheme(
    primary = PostalNavy,
    onPrimary = Color.White,
    primaryContainer = PostalNavyLight.copy(alpha = 0.15f),
    onPrimaryContainer = PostalNavy,
    secondary = TerracottaStamp,
    onSecondary = Color.White,
    secondaryContainer = TerracottaStamp.copy(alpha = 0.15f),
    onSecondaryContainer = TerracottaStamp,
    tertiary = OliveStamp,
    background = VintageParchment,
    surface = VintageCard,
    surfaceVariant = Color(0xFFF0EAE1),
    onBackground = Color(0xFF1E293B),
    onSurface = Color(0xFF1E293B),
    outline = VintageBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true, // Material 3 Dynamic Color enabled
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
