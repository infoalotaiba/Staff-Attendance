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

private val DarkColorScheme =
  darkColorScheme(
    primary = Color(0xFF38BDF8),
    onPrimary = Color(0xFF001D36),
    primaryContainer = Color(0xFF004B87),
    onPrimaryContainer = Color(0xFFD3E4FF),
    secondary = Color(0xFF94A3B8),
    onSecondary = Color(0xFF0F172A),
    background = Color(0xFF0F172A),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFF94A3B8),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF64748B),
    outlineVariant = Color(0xFF334155)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Color(0xFF005FB0), // Main blue color from high density design specs
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E4FF), // Present background from design
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF41484D),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDEE3EB),
    onSecondaryContainer = Color(0xFF41484D),
    background = Color(0xFFF7F9FC), // Modern High Density Light Slate Background
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6), // Late background from design
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFFE2E8F0)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
