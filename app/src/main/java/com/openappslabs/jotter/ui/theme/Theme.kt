/*
 * Copyright (c) 2026 Open Apps Labs
 *
 * This file is part of Jotter
 *
 * Jotter is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * Jotter is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Jotter.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package com.openappslabs.jotter.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun JotterTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    isTrueBlackEnabled: Boolean = false,
    isDynamicColor: Boolean = true,
    isHapticEnabled: Boolean = true,
    accentColor: String = "",
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val colorScheme = remember(isDarkTheme, isTrueBlackEnabled, isDynamicColor, accentColor) {
        val accent = parseAccentColor(accentColor)
        val usesDynamic = isDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        val base = when {
            usesDynamic -> {
                if (isDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            isDarkTheme -> darkColorScheme()
            else -> lightColorScheme()
        }

        val themed = if (!usesDynamic && accent != null) {
            accentColorScheme(accent, isDarkTheme)
        } else base

        if (isDarkTheme && isTrueBlackEnabled) {
            themed.copy(
                background = Color.Black,
                surface = Color.Black
            )
        } else themed
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !isDarkTheme
            insetsController.isAppearanceLightNavigationBars = !isDarkTheme
        }
    }

    CompositionLocalProvider(LocalHapticEnabled provides isHapticEnabled) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            motionScheme = remember { MotionScheme.expressive() },
            content = content
        )
    }
}

fun parseAccentColor(hex: String): Color? {
    if (hex.isBlank()) return null
    return try {
        val clean = hex.removePrefix("#")
        val value = clean.toLong(16)
        when (clean.length) {
            6 -> Color(0xFF000000L or value)
            8 -> Color(value)
            else -> null
        }
    } catch (e: Exception) {
        null
    }
}

private fun hsvColor(hue: Float, sat: Float, value: Float): Color {
    val h = ((hue % 360f) + 360f) % 360f
    return Color.hsv(h, sat.coerceIn(0f, 1f), value.coerceIn(0f, 1f))
}

private fun contrastOn(color: Color): Color {
    return if (color.luminance() > 0.5f) Color(0xFF1B1B1F) else Color.White
}

private fun accentColorScheme(accent: Color, isDarkTheme: Boolean): ColorScheme {
    val base = if (isDarkTheme) darkColorScheme() else lightColorScheme()

    val r = accent.red
    val g = accent.green
    val b = accent.blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min

    val hue = if (delta == 0f) {
        0f
    } else when (max) {
        r -> 60f * (((g - b) / delta) % 6f)
        g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }
    val sat = if (max == 0f) 0f else delta / max

    val primaryContainer = lerp(
        accent,
        if (isDarkTheme) Color.Black else Color.White,
        if (isDarkTheme) 0.55f else 0.78f
    )
    val onPrimaryContainer = if (isDarkTheme) {
        lerp(accent, Color.White, 0.85f)
    } else {
        lerp(accent, Color.Black, 0.35f)
    }

    val secondary = hsvColor(hue + 150f, sat * 0.55f, if (isDarkTheme) 0.85f else 0.5f)
    val secondaryContainer = lerp(
        secondary,
        if (isDarkTheme) Color.Black else Color.White,
        if (isDarkTheme) 0.5f else 0.8f
    )
    val onSecondaryContainer = if (isDarkTheme) {
        lerp(secondary, Color.White, 0.85f)
    } else {
        lerp(secondary, Color.Black, 0.35f)
    }

    val tertiary = hsvColor(hue - 120f, sat * 0.7f, if (isDarkTheme) 0.85f else 0.5f)
    val tertiaryContainer = lerp(
        tertiary,
        if (isDarkTheme) Color.Black else Color.White,
        if (isDarkTheme) 0.5f else 0.8f
    )
    val onTertiaryContainer = if (isDarkTheme) {
        lerp(tertiary, Color.White, 0.85f)
    } else {
        lerp(tertiary, Color.Black, 0.35f)
    }
    return base.copy(
        primary = accent,
        onPrimary = contrastOn(accent),
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = contrastOn(secondary),
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = contrastOn(tertiary),
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        surfaceTint = accent
    )
}