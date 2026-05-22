package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val ImmersiveColorScheme =
  darkColorScheme(
    primary = ImmersivePrimary,
    onPrimary = ImmersiveOnPrimary,
    background = ImmersiveBackground,
    onBackground = ImmersiveOnBackground,
    surface = ImmersiveSurface,
    onSurface = ImmersiveOnBackground,
    surfaceVariant = ImmersiveSurfaceVariant,
    onSurfaceVariant = ImmersiveOnSurfaceVariant,
    outline = ImmersiveOutline
  )

@Composable
fun WallpaperEngineTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = ImmersiveColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
