package com.expensemanager.ui.theme

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

private val LightColorScheme = lightColorScheme(
    primary = TealPrimary,
    onPrimary = ColorPalette.OnPrimaryLight,
    primaryContainer = TealLight,
    onPrimaryContainer = TealDark,
    secondary = TealDark,
    onSecondary = ColorPalette.OnPrimaryLight,
    tertiary = IncomeGreen,
    onTertiary = ColorPalette.OnPrimaryLight,
    background = SurfaceLight,
    onBackground = ColorPalette.OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = ColorPalette.OnSurfaceLight,
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    error = ExpenseRose,
)

private val DarkColorScheme = darkColorScheme(
    primary = TealLight,
    onPrimary = TealDark,
    primaryContainer = TealDark,
    onPrimaryContainer = TealLight,
    secondary = TealLight,
    onSecondary = TealDark,
    tertiary = IncomeGreen,
    onTertiary = ColorPalette.OnPrimaryLight,
    background = SurfaceDark,
    onBackground = ColorPalette.OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = ColorPalette.OnSurfaceDark,
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    error = ExpenseRose,
)

private object ColorPalette {
    val OnPrimaryLight = androidx.compose.ui.graphics.Color(0xFFFFFFFF)
    val OnSurfaceLight = androidx.compose.ui.graphics.Color(0xFF0F172A)
    val OnSurfaceDark = androidx.compose.ui.graphics.Color(0xFFF1F5F9)
}

@Composable
fun SmartExpenseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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
        content = content,
    )
}
