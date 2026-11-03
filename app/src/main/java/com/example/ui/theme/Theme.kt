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
    primary = NeonCyan,
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004F58),
    onPrimaryContainer = Color(0xFF97F0FF),

    secondary = ElectricViolet,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF381E72),
    onSecondaryContainer = Color(0xFFE8DDFF),

    tertiary = VibrantPink,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF5C001F),
    onTertiaryContainer = Color(0xFFFFD9DF),

    background = BackgroundDark,
    onBackground = TextPrimary,

    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = SurfaceElevatedDark,

    outline = BorderDark,
    outlineVariant = Color(0xFF1E293B)
)

private val LightColorScheme = lightColorScheme(
    primary = DeepIndigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF312E81),

    secondary = ElectricViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF4C1D95),

    tertiary = VibrantPink,
    onTertiary = Color.White,

    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),

    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),

    outline = Color(0xFFCBD5E1)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to sleek dark music theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
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
